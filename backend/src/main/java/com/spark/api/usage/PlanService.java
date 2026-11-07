package com.spark.api.usage;

import com.spark.api.domain.Profile;
import com.spark.api.platform.BusinessException;
import java.time.*;
import org.springframework.stereotype.Service;

@Service
public class PlanService {
  private final Clock clock;

  public PlanService(Clock clock) {
    this.clock = clock;
  }

  public boolean isPlus(Profile p) {
    return "PLUS".equals(p.plan)
        && (p.planExpiresAt == null || p.planExpiresAt.isAfter(clock.instant()));
  }

  public int limit(Profile p) {
    return isPlus(p) ? 100 : 3;
  }

  public void requirePlus(Profile p) {
    if (!isPlus(p)) throw new BusinessException(403, "FORBIDDEN_PLAN", "MyCoach Plus is required.");
  }

  public void requireCapacity(Profile p, long count, int freeLimit) {
    if (!isPlus(p) && count >= freeLimit)
      throw new BusinessException(403, "FORBIDDEN_PLAN", "Free plan limit reached.");
  }
}
