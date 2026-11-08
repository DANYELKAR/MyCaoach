package com.spark.api.generation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.spark.api.ai.*;
import com.spark.api.platform.BusinessException;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("local")
@Testcontainers
class GenerationIntegrationTest {
  static final java.nio.file.Path STORAGE =
      java.nio.file.Path.of(
          System.getProperty("java.io.tmpdir"), "spark-generation-test-" + UUID.randomUUID());
  @Autowired GenerationTransactions transactions;

  @Container
  static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", DB::getJdbcUrl);
    r.add("spring.datasource.username", DB::getUsername);
    r.add("spring.datasource.password", DB::getPassword);
    r.add("LOCAL_STORAGE_PATH", () -> STORAGE.toString());
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired GenerationService service;
  @MockitoBean AiClient ai;
  UUID user;
  String token;
  private static final UUID TONE = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @BeforeEach
  void user() throws Exception {
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
    user = UUID.fromString(data.get("userId").asText());
    token = data.get("accessToken").asText();
    mvc.perform(
            patch("/v1/me")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"ageConfirmed\":true,\"aiConsent\":true}"))
        .andExpect(status().isOk());
    when(ai.generate(anyString(), anyList(), eq(5))).thenReturn(output());
  }

  static AiClient.Result output() {
    return new AiClient.Result(
        List.of("One?", "Two?", "Three?", "Four?", "Five?"), false, "mock", 4, 5);
  }

  GenerationInput input(String key) {
    return new GenerationInput(
        "OPENER",
        TONE,
        null,
        null,
        null,
        key,
        List.of(new AiClient.Image("image/png", new byte[] {1, 2, 3})));
  }

  @Test
  void replayConflictAndFailureCreditRules() {
    var first = service.generate(user, input("same"));
    var replay = service.generate(user, input("same"));
    assertThat(replay.generation().id()).isEqualTo(first.generation().id());
    assertThat(first.suggestions()).hasSize(5);
    assertThat(replay.usage().remainingToday()).isEqualTo(2);
    verify(ai, times(1)).generate(anyString(), anyList(), eq(5));
    assertThatThrownBy(
            () ->
                service.generate(
                    user,
                    new GenerationInput(
                        "OPENER", TONE, "different", null, null, "same", input("x").images())))
        .isInstanceOfSatisfying(
            BusinessException.class, b -> assertThat(b.code).isEqualTo("IDEMPOTENCY_CONFLICT"));
    when(ai.generate(anyString(), anyList(), eq(5)))
        .thenReturn(new AiClient.Result(List.of(), true, "mock", 0, 0));
    assertThatThrownBy(() -> service.generate(user, input("blocked")))
        .isInstanceOfSatisfying(
            BusinessException.class, b -> assertThat(b.code).isEqualTo("CONTENT_BLOCKED"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from generations where user_id=? and status='SUCCEEDED'",
                Integer.class,
                user))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from generations where user_id=? and status='BLOCKED'",
                Integer.class,
                user))
        .isEqualTo(1);
    when(ai.generate(anyString(), anyList(), eq(5))).thenThrow(OutputValidator.unavailable());
    assertThatThrownBy(() -> service.generate(user, input("failed")))
        .isInstanceOf(BusinessException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from generations where user_id=? and status='FAILED'",
                Integer.class,
                user))
        .isEqualTo(1);
  }

  @Test
  void concurrentRequestsCannotOverspend() throws Exception {
    when(ai.generate(anyString(), anyList(), eq(5)))
        .thenAnswer(
            i -> {
              Thread.sleep(60);
              return output();
            });
    try (var executor = Executors.newFixedThreadPool(8)) {
      List<Callable<Integer>> tasks = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        String key = "concurrent-" + i;
        tasks.add(
            () -> {
              try {
                service.generate(user, input(key));
                return 200;
              } catch (BusinessException e) {
                return e.status;
              }
            });
      }
      var results = executor.invokeAll(tasks);
      List<Integer> statuses = new ArrayList<>();
      for (var result : results) statuses.add(result.get());
      assertThat(statuses.stream().filter(s -> s == 200).count()).isEqualTo(3);
      assertThat(statuses.stream().filter(s -> s == 402).count()).isEqualTo(5);
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from generations where user_id=? and status='SUCCEEDED'",
                Integer.class,
                user))
        .isEqualTo(3);
  }

  @Test
  void multipartImageValidationAndConsentAndPlanChecks() throws Exception {
    mvc.perform(
            multipart("/v1/generations/opener")
                .file(
                    new MockMultipartFile(
                        "image", "image.jpg", "image/jpeg", "not a jpeg".getBytes()))
                .param("toneId", TONE.toString())
                .param("idempotencyKey", "bad-image")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    assertThatThrownBy(
            () ->
                service.generate(
                    user,
                    new GenerationInput("REPLY", TONE, null, "Hello", null, "reply", List.of())))
        .isInstanceOfSatisfying(
            BusinessException.class, b -> assertThat(b.code).isEqualTo("FORBIDDEN_PLAN"));
    jdbc.update("update profiles set ai_consent_at=null where id=?", user);
    assertThatThrownBy(() -> service.generate(user, input("consent")))
        .isInstanceOfSatisfying(
            BusinessException.class, b -> assertThat(b.code).isEqualTo("CONSENT_REQUIRED"));
    verifyNoInteractions(ai);
  }

  @Test
  void expiredAttemptCannotCommitOrFailANewerReservation() {
    var in = input("fenced");
    var old = transactions.reserve(user, in, "fingerprint");
    assertThatThrownBy(() -> transactions.reserve(user, in, "fingerprint"))
        .isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.code).isEqualTo("GENERATION_PENDING"));
    jdbc.update(
        "update generations set reservation_expires_at=now()-interval '1 second' where id=?",
        old.id());
    var current = transactions.reserve(user, in, "fingerprint");
    assertThat(current.attempt()).isNotEqualTo(old.attempt());
    assertThatThrownBy(
            () -> transactions.complete(user, old, output(), output().suggestions(), List.of()))
        .isInstanceOfSatisfying(
            BusinessException.class, e -> assertThat(e.code).isEqualTo("GENERATION_EXPIRED"));
    transactions.fail(user, old, false);
    assertThat(
            jdbc.queryForObject(
                "select status from generations where id=?", String.class, current.id()))
        .isEqualTo("PENDING");
    assertThat(
            transactions
                .complete(user, current, output(), output().suggestions(), List.of())
                .response()
                .usage()
                .remainingToday())
        .isEqualTo(2);
  }

  @Test
  void withdrawalOfImageRetentionDuringAiCallDeletesTheUploadedFile() throws Exception {
    jdbc.update("update profiles set store_images=true where id=?", user);
    when(ai.generate(anyString(), anyList(), eq(5)))
        .thenAnswer(
            invocation -> {
              jdbc.update("update profiles set store_images=false where id=?", user);
              return output();
            });
    var result = service.generate(user, input("retention-withdrawn"));
    assertThat(
            jdbc.queryForObject(
                "select cardinality(input_image_paths) from generations where id=?",
                Integer.class,
                result.generation().id()))
        .isZero();
    try (var paths = java.nio.file.Files.walk(STORAGE.resolve(user.toString()))) {
      assertThat(paths.filter(java.nio.file.Files::isRegularFile).count()).isZero();
    }
  }
}
