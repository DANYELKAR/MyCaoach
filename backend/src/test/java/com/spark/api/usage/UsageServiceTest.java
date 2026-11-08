package com.spark.api.usage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.spark.api.domain.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsageServiceTest {
  @Test
  void handlesDstShortAndLongDays() {
    var spring = UsageService.window(Instant.parse("2026-03-08T12:00:00Z"), "America/New_York");
    var fall = UsageService.window(Instant.parse("2026-11-01T12:00:00Z"), "America/New_York");
    assertThat(Duration.between(spring.startsAt(), spring.resetsAt()).toHours()).isEqualTo(23);
    assertThat(Duration.between(fall.startsAt(), fall.resetsAt()).toHours()).isEqualTo(25);
  }

  @Test
  void respectsLocalMidnightAndOnlySucceededCredits() {
    var now = Instant.parse("2026-11-07T22:00:00Z");
    var clock = Clock.fixed(now, ZoneOffset.UTC);
    var repo = mock(GenerationRepository.class);
    var plans = new PlanService(clock);
    var usage = new UsageService(repo, plans, clock);
    var p = new Profile();
    p.id = UUID.randomUUID();
    p.timezone = "Europe/Istanbul";
    when(repo.countByUserIdAndStatusAndCreatedAtGreaterThanEqual(
            p.id, "SUCCEEDED", Instant.parse("2026-11-07T21:00:00Z")))
        .thenReturn(2L);
    var result = usage.usage(p);
    assertThat(result.remainingToday()).isEqualTo(1);
    assertThat(result.resetsAt()).isEqualTo(Instant.parse("2026-11-08T21:00:00Z"));
  }

  @Test
  void planExpiryAndCapacityAreEnforced() {
    var clock = Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC);
    var plans = new PlanService(clock);
    var p = new Profile();
    assertThat(plans.limit(p)).isEqualTo(3);
    assertThatThrownBy(() -> plans.requirePlus(p)).hasMessageContaining("Plus");
    p.plan = "PLUS";
    p.planExpiresAt = clock.instant().plusSeconds(1);
    assertThat(plans.limit(p)).isEqualTo(100);
    p.planExpiresAt = clock.instant();
    assertThat(plans.limit(p)).isEqualTo(3);
    assertThatThrownBy(() -> plans.requireCapacity(p, 3, 3)).hasMessageContaining("limit");
  }
}
