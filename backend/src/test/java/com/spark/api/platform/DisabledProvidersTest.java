package com.spark.api.platform;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.api.ai.*;
import com.spark.api.billing.*;
import com.spark.api.domain.ProfileRepository;
import com.spark.api.profile.ProfileService;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class DisabledProvidersTest {
  private final ApplicationContextRunner context =
      new ApplicationContextRunner()
          .withPropertyValues("spring.profiles.active=supabase")
          .withUserConfiguration(
              DisabledProviders.class, GeminiClient.class, RevenueCatProvider.class)
          .withBean(ObjectMapper.class, ObjectMapper::new)
          .withBean(Clock.class, Clock::systemUTC);

  @Test
  void disabledIntegrationsRequireNoKeysAndNeverProduceOutputOrGrantPlus() {
    context
        .withPropertyValues("spark.ai.enabled=false", "spark.billing.enabled=false")
        .run(
            c -> {
              assertThat(c)
                  .hasNotFailed()
                  .hasSingleBean(AiClient.class)
                  .hasSingleBean(BillingProvider.class);
              assertUnavailable(
                  () -> c.getBean(AiClient.class).generate("synthetic", List.of(), 5),
                  "AI_UNAVAILABLE");
              assertUnavailable(
                  () -> c.getBean(AiClient.class).analyzeStyle(List.of("synthetic")),
                  "AI_UNAVAILABLE");
              assertUnavailable(
                  () -> c.getBean(BillingProvider.class).fetch(UUID.randomUUID()),
                  "BILLING_UNAVAILABLE");
            });
  }

  @Test
  void integrationsStillRequireRealConfigurationUnlessExplicitlyDisabled() {
    context
        .withPropertyValues("GEMINI_API_KEY=", "REVENUECAT_API_KEY=")
        .run(c -> assertThat(c).hasFailed());
    context
        .withPropertyValues("GEMINI_API_KEY=test-fixture", "REVENUECAT_API_KEY=test-fixture")
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              assertThat(c.getBean(AiClient.class)).isInstanceOf(GeminiClient.class);
              assertThat(c.getBean(BillingProvider.class)).isInstanceOf(RevenueCatProvider.class);
            });
  }

  @Test
  void disabledBillingRejectsWebhooksEvenWhenASecretRemainsConfigured() {
    var service =
        new BillingService(
            mock(BillingProvider.class),
            mock(ProfileService.class),
            mock(ProfileRepository.class),
            mock(JdbcTemplate.class),
            "old-secret",
            false);
    assertThatThrownBy(() -> service.webhook("old-secret", new ObjectMapper().createObjectNode()))
        .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status).isEqualTo(401));
  }

  private void assertUnavailable(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> {
              assertThat(e.status).isEqualTo(503);
              assertThat(e.code).isEqualTo(code);
            });
  }
}
