package com.spark.api.usage;

import static org.assertj.core.api.Assertions.*;

import com.spark.api.history.QualityMetricsService.Metrics;
import org.junit.jupiter.api.Test;

class QualityMetricsTest {
  @Test
  void ratesUseAppropriateDenominatorsAndHandleEmptyData() {
    var empty = new Metrics(0, 0, 0, 0, 0);
    assertThat(empty.likeRate()).isZero();
    assertThat(empty.sentRate()).isZero();
    assertThat(empty.repliedRate()).isZero();
    var m = new Metrics(10, 3, 4, 5, 2);
    assertThat(m.likeRate()).isEqualTo(0.75);
    assertThat(m.sentRate()).isEqualTo(0.5);
    assertThat(m.repliedRate()).isEqualTo(0.4);
  }
}
