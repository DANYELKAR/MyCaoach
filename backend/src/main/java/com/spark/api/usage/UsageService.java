package com.spark.api.usage;

import com.spark.api.domain.*;
import java.time.*;
import org.springframework.stereotype.Service;

@Service
public class UsageService {
  public record Window(Instant startsAt, Instant resetsAt) {}

  public record Usage(int remainingToday, int dailyLimit, Instant resetsAt) {}

  private final GenerationRepository generations;
  private final PlanService plans;
  private final Clock clock;

  public UsageService(GenerationRepository generations, PlanService plans, Clock clock) {
    this.generations = generations;
    this.plans = plans;
    this.clock = clock;
  }

  public static Window window(Instant now, String timezone) {
    var day = now.atZone(ZoneId.of(timezone)).toLocalDate();
    var zone = ZoneId.of(timezone);
    return new Window(
        day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant());
  }

  public Usage usage(Profile p) {
    var w = window(clock.instant(), p.timezone);
    int limit = plans.limit(p);
    long used =
        generations.countByUserIdAndStatusAndCreatedAtGreaterThanEqual(
            p.id, "SUCCEEDED", w.startsAt());
    return new Usage((int) Math.max(0, limit - used), limit, w.resetsAt());
  }

  public Instant historySince(Profile p) {
    return plans.isPlus(p) ? Instant.EPOCH : clock.instant().minus(Duration.ofDays(7));
  }
}
