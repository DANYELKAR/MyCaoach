package com.spark.api.platform;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {
  @Test
  void limitsEachUserIndependently() {
    var limits = new RateLimitService();
    var user = UUID.randomUUID();
    for (int i = 0; i < 20; i++) limits.check(user);
    assertThatThrownBy(() -> limits.check(user))
        .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.status).isEqualTo(429));
    assertThatCode(() -> limits.check(UUID.randomUUID())).doesNotThrowAnyException();
  }
}
