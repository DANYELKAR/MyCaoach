package com.spark.api.platform;

import com.spark.api.ai.AiClient;
import com.spark.api.ai.OutputValidator;
import com.spark.api.billing.BillingProvider;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

/**
 * Explicitly disabled integrations fail closed; they never return fixture output or entitlements.
 */
@Configuration
@Profile("supabase")
public class DisabledProviders {
  @Bean
  @ConditionalOnProperty(name = "spark.ai.enabled", havingValue = "false")
  AiClient disabledAi() {
    return new AiClient() {
      public Result generate(String prompt, List<Image> images, int count) {
        throw OutputValidator.unavailable();
      }

      public Map<String, Object> analyzeStyle(List<String> samples) {
        throw OutputValidator.unavailable();
      }
    };
  }

  @Bean
  @ConditionalOnProperty(name = "spark.billing.enabled", havingValue = "false")
  BillingProvider disabledBilling() {
    return user -> {
      throw new BusinessException(
          503, "BILLING_UNAVAILABLE", "Subscription status is temporarily unavailable.");
    };
  }
}
