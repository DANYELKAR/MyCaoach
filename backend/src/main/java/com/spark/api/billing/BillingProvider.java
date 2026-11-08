package com.spark.api.billing;

import java.time.Instant;
import java.util.UUID;

public interface BillingProvider {
  record Entitlement(boolean active, Instant expiresAt) {}

  Entitlement fetch(UUID user);

  default void event(UUID user, String type, Instant expiresAt) {}
}
