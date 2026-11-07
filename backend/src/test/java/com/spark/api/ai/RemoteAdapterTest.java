package com.spark.api.ai;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.spark.api.billing.RevenueCatProvider;
import com.spark.api.platform.BusinessException;
import com.spark.api.privacy.SupabaseAdminClient;
import com.sun.net.httpserver.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;

class RemoteAdapterTest {
  HttpServer server;
  String base;
  ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.start();
    base = "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  void respond(HttpExchange x, int status, Object value) throws java.io.IOException {
    byte[] bytes = json.writeValueAsBytes(value);
    x.getResponseHeaders().add("Content-Type", "application/json");
    x.sendResponseHeaders(status, bytes.length);
    x.getResponseBody().write(bytes);
    x.close();
  }

  @Test
  void geminiUsesSchemaSafetyImagesAndOnlyOneRetryOn5xx() throws Exception {
    AtomicInteger calls = new AtomicInteger();
    AtomicReference<JsonNode> body = new AtomicReference<>();
    server.createContext(
        "/models/test-flash:generateContent",
        x -> {
          body.set(json.readTree(x.getRequestBody()));
          assertThat(x.getRequestHeaders().getFirst("x-goog-api-key")).isEqualTo("test-key");
          if (calls.incrementAndGet() == 1) {
            respond(x, 503, Map.of());
            return;
          }
          var content =
              Map.of(
                  "suggestions",
                  List.of(
                      Map.of("text", "One?"),
                      Map.of("text", "Two?"),
                      Map.of("text", "Three?"),
                      Map.of("text", "Four?"),
                      Map.of("text", "Five?")),
                  "blocked",
                  false);
          respond(
              x,
              200,
              Map.of(
                  "candidates",
                  List.of(
                      Map.of(
                          "finishReason",
                          "STOP",
                          "content",
                          Map.of(
                              "parts", List.of(Map.of("text", json.writeValueAsString(content)))))),
                  "usageMetadata",
                  Map.of("promptTokenCount", 11, "candidatesTokenCount", 12)));
        });
    var result =
        new GeminiClient(json, "test-key", "test-flash", base)
            .generate(
                "respectful prompt",
                List.of(new AiClient.Image("image/png", new byte[] {1, 2})),
                5);
    assertThat(new OutputValidator().validate(result, 5)).hasSize(5);
    assertThat(calls.get()).isEqualTo(2);
    assertThat(result.tokensIn()).isEqualTo(11);
    assertThat(
            body.get()
                .path("generationConfig")
                .path("responseJsonSchema")
                .path("properties")
                .path("suggestions")
                .path("minItems")
                .asInt())
        .isEqualTo(5);
    assertThat(body.get().path("safetySettings")).hasSize(4);
    assertThat(
            body.get()
                .path("contents")
                .path(0)
                .path("parts")
                .path(1)
                .path("inlineData")
                .path("mimeType")
                .asText())
        .isEqualTo("image/png");
  }

  @Test
  void geminiDoesNotRetry4xxOrExposeRawErrors() {
    AtomicInteger calls = new AtomicInteger();
    server.createContext(
        "/models/test:generateContent",
        x -> {
          calls.incrementAndGet();
          respond(x, 429, Map.of("secret", "raw-provider-message"));
        });
    assertThatThrownBy(
            () -> new GeminiClient(json, "test-key", "test", base).generate("data", List.of(), 5))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> assertThat(e.getMessage()).doesNotContain("raw-provider"));
    assertThat(calls.get()).isEqualTo(1);
  }

  @Test
  void revenueCatVerifiesCanonicalEntitlementAndExpiry() {
    var user = UUID.randomUUID();
    server.createContext(
        "/subscribers/" + user,
        x -> {
          assertThat(x.getRequestHeaders().getFirst("Authorization"))
              .isEqualTo("Bearer server-key");
          respond(
              x,
              200,
              Map.of(
                  "subscriber",
                  Map.of(
                      "entitlements",
                      Map.of("plus", Map.of("expires_date", "2026-11-08T00:00:00Z")))));
        });
    var clock = Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC);
    var provider = new RevenueCatProvider(json, clock, "server-key", base);
    assertThat(provider.fetch(user).active()).isTrue();
    assertThat(
            new RevenueCatProvider(
                    json,
                    Clock.fixed(Instant.parse("2026-11-09T00:00:00Z"), ZoneOffset.UTC),
                    "server-key",
                    base)
                .fetch(user)
                .active())
        .isFalse();
  }

  @Test
  void supabaseRecursivelyListsAndDeletesOnlyUserObjectsThenAuth() {
    UUID user = UUID.randomUUID();
    List<String> operations = new ArrayList<>();
    server.createContext(
        "/storage/v1/object/list/inputs",
        x -> {
          var prefix = json.readTree(x.getRequestBody()).path("prefix").asText();
          assertThat(x.getRequestHeaders().getFirst("apikey")).isEqualTo("server-secret");
          if (prefix.equals(user.toString()))
            respond(x, 200, List.of(Map.of("name", "generation")));
          else
            respond(
                x,
                200,
                List.of(
                    Map.of(
                        "name",
                        "photo.png",
                        "id",
                        "object-id",
                        "created_at",
                        "2026-11-01T00:00:00Z")));
        });
    server.createContext(
        "/storage/v1/object/inputs",
        x -> {
          operations.add(json.readTree(x.getRequestBody()).path("prefixes").path(0).asText());
          respond(x, 200, Map.of());
        });
    server.createContext(
        "/auth/v1/admin/users/" + user,
        x -> {
          operations.add(x.getRequestMethod());
          respond(x, 200, Map.of());
        });
    var client = new SupabaseAdminClient(json, base, "server-secret");
    client.deleteUserObjects(user);
    client.revoke(user, null);
    client.deleteUser(user);
    assertThat(operations).containsExactly(user + "/generation/photo.png", "PUT", "DELETE");
  }

  @Test
  void supabaseSignsPrivateImageAccessForOnlySixtySeconds() {
    String path = UUID.randomUUID() + "/generation/photo.png";
    server.createContext(
        "/storage/v1/object/sign/inputs/" + path,
        x -> {
          assertThat(json.readTree(x.getRequestBody()).path("expiresIn").asInt()).isEqualTo(60);
          respond(
              x, 200, Map.of("signedURL", "/object/sign/inputs/" + path + "?token=test-signature"));
        });
    var access = new SupabaseAdminClient(json, base, "server-secret").access(path);
    assertThat(access.signedUrl())
        .isEqualTo(base + "/storage/v1/object/sign/inputs/" + path + "?token=test-signature");
    assertThat(access.bytes()).isNull();
  }
}
