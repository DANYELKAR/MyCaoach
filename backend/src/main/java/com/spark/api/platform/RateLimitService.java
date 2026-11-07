package com.spark.api.platform;

import com.github.benmanes.caffeine.cache.*;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
  private final Cache<UUID, Bucket> buckets =
      Caffeine.newBuilder().maximumSize(10000).expireAfterAccess(Duration.ofHours(1)).build();

  public void check(UUID user) {
    var bucket =
        buckets.get(
            user,
            id ->
                Bucket.builder()
                    .addLimit(l -> l.capacity(20).refillGreedy(20, Duration.ofMinutes(1)))
                    .build());
    if (!bucket.tryConsume(1))
      throw new BusinessException(
          429, "RATE_LIMITED", "Too many requests. Please try again shortly.");
  }
}
