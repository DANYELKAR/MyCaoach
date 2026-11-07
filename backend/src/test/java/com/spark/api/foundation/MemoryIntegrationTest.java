package com.spark.api.foundation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.spark.api.ai.AiClient;
import com.spark.api.generation.*;
import com.spark.api.platform.BusinessException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("local")
@Testcontainers
class MemoryIntegrationTest {
  static final java.nio.file.Path STORAGE =
      java.nio.file.Path.of(
          System.getProperty("java.io.tmpdir"), "spark-memory-test-" + UUID.randomUUID());

  @Container
  static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", DB::getJdbcUrl);
    r.add("spring.datasource.username", DB::getUsername);
    r.add("spring.datasource.password", DB::getPassword);
    r.add("LOCAL_STORAGE_PATH", () -> STORAGE.toString());
  }

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  com.spark.api.privacy.InputStorage storage;

  @Autowired com.spark.api.privacy.CleanupService cleanup;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired GenerationService generations;

  record User(UUID id, String token) {}

  User user() throws Exception {
    var result =
        mvc.perform(
                post("/local/auth/register")
                    .contentType("application/json")
                    .content(
                        "{\"email\":\""
                            + UUID.randomUUID()
                            + "@example.test\",\"password\":\"test-password-123\"}"))
            .andExpect(status().isOk())
            .andReturn();
    var data = json.readTree(result.getResponse().getContentAsString());
    var u =
        new User(UUID.fromString(data.get("userId").asText()), data.get("accessToken").asText());
    mvc.perform(
            patch("/v1/me")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json")
                .content("{\"ageConfirmed\":true,\"aiConsent\":true}"))
        .andExpect(status().isOk());
    return u;
  }

  JsonNode create(User u, String path, String body) throws Exception {
    return json.readTree(
        mvc.perform(
                post(path)
                    .header("Authorization", "Bearer " + u.token())
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  GenerationDtos.Result generate(User u, UUID contact, String key) {
    return generations.generate(
        u.id(),
        new GenerationInput(
            "OPENER",
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            null,
            null,
            contact,
            key,
            List.of(new AiClient.Image("image/png", new byte[] {1}))));
  }

  @Test
  void peopleFoldersSavedFeedbackAndOwnership() throws Exception {
    User a = user(), b = user();
    var c =
        create(
            a,
            "/v1/contacts",
            "{\"name\":\"Alex\",\"source\":\"HINGE\",\"interests\":[\"hiking\"]}");
    var contact = UUID.fromString(c.get("id").asText());
    mvc.perform(
            get("/v1/contacts").param("q", "lex").header("Authorization", "Bearer " + a.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Alex"));
    for (String method : List.of("GET", "PATCH", "DELETE"))
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                      org.springframework.http.HttpMethod.valueOf(method),
                      "/v1/contacts/" + contact)
                  .header("Authorization", "Bearer " + b.token())
                  .contentType("application/json")
                  .content("{\"name\":\"No\"}"))
          .andExpect(status().isNotFound());
    var g = generate(a, contact, "memory");
    String sid = g.suggestions().getFirst().id().toString();
    for (String path :
        List.of(
            "/v1/generations/" + g.generation().id(), "/v1/threads/" + g.generation().threadId()))
      mvc.perform(get(path).header("Authorization", "Bearer " + b.token()))
          .andExpect(status().isNotFound());
    mvc.perform(
            patch("/v1/suggestions/" + sid)
                .header("Authorization", "Bearer " + b.token())
                .contentType("application/json")
                .content("{\"feedback\":\"LIKE\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/v1/suggestions/" + sid + "/report")
                .header("Authorization", "Bearer " + b.token())
                .contentType("application/json")
                .content("{\"reason\":\"unsafe\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            patch("/v1/suggestions/" + sid)
                .header("Authorization", "Bearer " + a.token())
                .contentType("application/json")
                .content("{\"feedback\":\"LIKE\",\"sentStatus\":\"SENT\",\"outcome\":\"REPLIED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("REPLIED"));
    create(a, "/v1/suggestions/" + sid + "/report", "{\"reason\":\"unsafe\"}");
    var folder = create(a, "/v1/folders", "{\"name\":\"Favorites\"}");
    var saved =
        create(
            a,
            "/v1/saved",
            "{\"kind\":\"SUGGESTION\",\"suggestionId\":\""
                + sid
                + "\",\"folderId\":\""
                + folder.get("id").asText()
                + "\"}");
    mvc.perform(
            patch("/v1/saved/" + saved.get("id").asText())
                .header("Authorization", "Bearer " + b.token())
                .contentType("application/json")
                .content("{\"title\":\"steal\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/v1/saved")
                .header("Authorization", "Bearer " + b.token())
                .contentType("application/json")
                .content("{\"kind\":\"SUGGESTION\",\"suggestionId\":\"" + sid + "\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(
            delete("/v1/folders/" + folder.get("id").asText())
                .header("Authorization", "Bearer " + a.token()))
        .andExpect(status().isOk());
    mvc.perform(get("/v1/saved").header("Authorization", "Bearer " + a.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].folderId").isEmpty());
    mvc.perform(
            get("/v1/threads/" + g.generation().threadId())
                .header("Authorization", "Bearer " + a.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.generations[0].suggestions.length()").value(5));
  }

  @Test
  void freeLimitsAndHistoryDeletionCannotRefillCredits() throws Exception {
    User u = user();
    for (int i = 0; i < 3; i++) {
      create(u, "/v1/contacts", "{\"name\":\"Person " + i + "\"}");
      create(u, "/v1/folders", "{\"name\":\"Folder " + i + "\"}");
      generate(u, null, "limit-" + i);
    }
    for (String path : List.of("/v1/contacts", "/v1/folders"))
      mvc.perform(
              post(path)
                  .header("Authorization", "Bearer " + u.token())
                  .contentType("application/json")
                  .content("{\"name\":\"over limit\"}"))
          .andExpect(status().isForbidden());
    mvc.perform(delete("/v1/threads").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk());
    mvc.perform(get("/v1/threads").header("Authorization", "Bearer " + u.token()))
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/v1/me/usage").header("Authorization", "Bearer " + u.token()))
        .andExpect(jsonPath("$.remainingToday").value(0));
    assertThatThrownBy(() -> generate(u, null, "after-delete"))
        .isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.code).isEqualTo("OUT_OF_CREDITS"));
    assertThatThrownBy(() -> generate(u, null, "limit-0"))
        .isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.code).isEqualTo("HISTORY_DELETED"));
  }

  @Test
  void oldHistoryRequiresPlus() throws Exception {
    User u = user();
    var g = generate(u, null, "old");
    jdbc.update(
        "update generations set created_at=now()-interval '8 days' where id=?",
        g.generation().id());
    jdbc.update(
        "update threads set last_activity_at=now()-interval '8 days' where id=?",
        g.generation().threadId());
    mvc.perform(
            get("/v1/generations/" + g.generation().id())
                .header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isNotFound());
    jdbc.update(
        "update profiles set plan='PLUS', plan_expires_at=now()+interval '1 day' where id=?",
        u.id());
    mvc.perform(
            get("/v1/generations/" + g.generation().id())
                .header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk());
  }

  @Test
  void billingReplayOrderingAndCancellation() throws Exception {
    User u = user();
    long now = System.currentTimeMillis();
    long expires = now + 3600000;
    String purchase = event(u, "purchase", "INITIAL_PURCHASE", now, expires);
    mvc.perform(post("/v1/billing/webhook").contentType("application/json").content(purchase))
        .andExpect(status().isUnauthorized());
    webhook(purchase);
    webhook(purchase);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from billing_events where user_id=?", Integer.class, u.id()))
        .isEqualTo(1);
    webhook(event(u, "cancel", "CANCELLATION", now + 1, expires));
    mvc.perform(get("/v1/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(jsonPath("$.plan").value("PLUS"));
    webhook(event(u, "expire", "EXPIRATION", now + 2, now - 1));
    webhook(event(u, "late-renewal", "RENEWAL", now - 10, expires));
    mvc.perform(post("/v1/billing/sync").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plan").value("FREE"));
  }

  String event(User u, String id, String type, long time, long expires) {
    return "{\"event\":{\"id\":\""
        + u.id()
        + "-"
        + id
        + "\",\"app_user_id\":\""
        + u.id()
        + "\",\"type\":\""
        + type
        + "\",\"event_timestamp_ms\":"
        + time
        + ",\"expiration_at_ms\":"
        + expires
        + ",\"entitlement_ids\":[\"plus\"]}}";
  }

  void webhook(String body) throws Exception {
    mvc.perform(
            post("/v1/billing/webhook")
                .header("Authorization", "local-webhook-secret")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk());
  }

  @Test
  void plusUnlocksReplyStyleCustomTonesAndTemplates() throws Exception {
    User u = user(), other = user();
    mvc.perform(get("/v1/style-profile").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/local/billing/entitlement")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json")
                .content("{\"active\":true,\"expiresAt\":\"2099-01-01T00:00:00Z\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plan").value("PLUS"));
    var tone =
        create(
            u,
            "/v1/tones",
            "{\"name\":\"My tone\",\"styleInstruction\":\"Be concise and curious\",\"humorLevel\":70}");
    mvc.perform(
            patch("/v1/tones/" + tone.get("id").asText())
                .header("Authorization", "Bearer " + other.token())
                .contentType("application/json")
                .content("{\"name\":\"other\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            multipart("/v1/generations/reply")
                .param("text", "What is your favorite book?")
                .param("toneId", tone.get("id").asText())
                .param("idempotencyKey", "reply-plus")
                .header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestions.length()").value(4));
    mvc.perform(
            put("/v1/style-profile")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json")
                .content(
                    "{\"sampleMessages\":[\"Hello there!\",\"How was your day?\",\"That sounds fun\",\"Tell me more\",\"What did you think?\"]}"))
        .andExpect(status().isOk());
    create(u, "/v1/style-profile/analyze", "{}");
    mvc.perform(get("/v1/style-profile").header("Authorization", "Bearer " + u.token()))
        .andExpect(jsonPath("$.summary.formality").value("casual"));
    create(u, "/v1/saved", "{\"kind\":\"TEMPLATE\",\"text\":\"What made you smile today?\"}");
    mvc.perform(delete("/v1/style-profile").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from style_profiles where user_id=?", Integer.class, u.id()))
        .isZero();
  }

  @Test
  void accountDeletionRemovesAuthRowsAndFilesAndRejectsStaleToken() throws Exception {
    User u = user();
    mvc.perform(
            patch("/v1/me")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json")
                .content("{\"storeImages\":true}"))
        .andExpect(status().isOk());
    var c = create(u, "/v1/contacts", "{\"name\":\"Delete test\"}");
    var g = generate(u, UUID.fromString(c.get("id").asText()), "delete-account");
    create(u, "/v1/folders", "{\"name\":\"Remove me\"}");
    mvc.perform(
            get("/v1/generations/" + g.generation().id() + "/images/0")
                .header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"));
    mvc.perform(
            patch("/v1/me")
                .header("Authorization", "Bearer " + u.token())
                .contentType("application/json")
                .content("{\"timezone\":\"Europe/Istanbul\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TIMEZONE_LOCKED"));

    assertThat(
            java.nio.file.Files.walk(STORAGE.resolve(u.id().toString()))
                .filter(java.nio.file.Files::isRegularFile)
                .count())
        .isEqualTo(1);
    mvc.perform(delete("/v1/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk());
    for (String table :
        List.of(
            "contacts",
            "generations",
            "suggestions",
            "threads",
            "folders",
            "saved_items",
            "reports",
            "style_profiles",
            "billing_events"))
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from " + table + " where user_id=?", Integer.class, u.id()))
          .as(table)
          .isZero();
    assertThat(
            jdbc.queryForObject("select count(*) from profiles where id=?", Integer.class, u.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auth.users where id=?", Integer.class, u.id()))
        .isZero();
    assertThat(java.nio.file.Files.exists(STORAGE.resolve(u.id().toString()))).isFalse();
    mvc.perform(get("/v1/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isUnauthorized());
    mvc.perform(delete("/v1/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk());
  }

  @Test
  void cleanupRemovesOnlyOldUnreferencedObjects() throws Exception {
    User u = user();
    String old = u.id() + "/old/orphan.png", fresh = u.id() + "/fresh/orphan.png";
    storage.put(old, new byte[] {1}, "image/png");
    storage.put(fresh, new byte[] {1}, "image/png");
    java.nio.file.Files.setLastModifiedTime(
        STORAGE.resolve(old),
        java.nio.file.attribute.FileTime.from(
            java.time.Instant.now().minus(java.time.Duration.ofDays(2))));
    cleanup.clean();
    assertThat(java.nio.file.Files.exists(STORAGE.resolve(old))).isFalse();
    assertThat(java.nio.file.Files.exists(STORAGE.resolve(fresh))).isTrue();
  }

  @Test
  void interruptedAccountDeletionCanBeRetriedWithTheSameToken() throws Exception {
    User u = user();
    org.mockito.Mockito.doThrow(new IllegalStateException("temporary fixture storage failure"))
        .doCallRealMethod()
        .when(storage)
        .deleteUserObjects(u.id());
    mvc.perform(delete("/v1/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isInternalServerError());
    mvc.perform(get("/v1/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isUnauthorized());
    mvc.perform(delete("/v1/me").header("Authorization", "Bearer " + u.token()))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from auth.users where id=?", Integer.class, u.id()))
        .isZero();
  }
}
