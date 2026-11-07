package com.spark.api.billing;

import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalBillingProvider implements BillingProvider {
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;
  private final Clock clock;

  public LocalBillingProvider(Clock clock, org.springframework.jdbc.core.JdbcTemplate jdbc) {
    this.clock = clock;
    this.jdbc = jdbc;
  }

  public Entitlement fetch(UUID user) {
    var rows =
        jdbc.query(
            "select active,expires_at from auth.local_entitlements where user_id=?",
            (rs, n) ->
                new Entitlement(
                    rs.getBoolean(1),
                    rs.getTimestamp(2) == null ? null : rs.getTimestamp(2).toInstant()),
            user);
    var e = rows.isEmpty() ? new Entitlement(false, null) : rows.getFirst();
    return new Entitlement(
        e.active() && (e.expiresAt() == null || e.expiresAt().isAfter(clock.instant())),
        e.expiresAt());
  }

  public void set(UUID user, Entitlement value) {
    jdbc.update(
        "insert into auth.local_entitlements(user_id,active,expires_at) values(?,?,?) on conflict(user_id) do update set active=excluded.active,expires_at=excluded.expires_at",
        user,
        value.active(),
        value.expiresAt() == null ? null : java.sql.Timestamp.from(value.expiresAt()));
  }

  public void event(UUID user, String type, Instant expiresAt) {
    switch (type) {
      case "INITIAL_PURCHASE",
          "RENEWAL",
          "UNCANCELLATION",
          "PRODUCT_CHANGE",
          "SUBSCRIPTION_EXTENDED",
          "NON_RENEWING_PURCHASE" ->
          set(
              user,
              new Entitlement(expiresAt == null || expiresAt.isAfter(clock.instant()), expiresAt));
      case "EXPIRATION", "REFUND" -> set(user, new Entitlement(false, expiresAt));
      case "CANCELLATION", "BILLING_ISSUE" -> {
        if (expiresAt != null)
          set(user, new Entitlement(expiresAt.isAfter(clock.instant()), expiresAt));
      }
      default -> {}
    }
  }
}
