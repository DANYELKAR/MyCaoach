package com.spark.api.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.spark.api.domain.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.profile.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
  private final BillingProvider provider;
  private final ProfileService profiles;
  private final ProfileRepository repository;
  private final JdbcTemplate jdbc;
  private final String secret;

  public BillingService(
      BillingProvider provider,
      ProfileService profiles,
      ProfileRepository repository,
      JdbcTemplate jdbc,
      @Value("${REVENUECAT_WEBHOOK_SECRET}") String secret) {
    if (secret.isBlank()) throw new IllegalArgumentException("Webhook secret required");
    this.provider = provider;
    this.profiles = profiles;
    this.repository = repository;
    this.jdbc = jdbc;
    this.secret = secret;
  }

  @Transactional
  public ProfileDtos.View sync(UUID user) {
    Profile p = profiles.lock(user);
    apply(p, provider.fetch(user));
    return ProfileDtos.view(p);
  }

  @Transactional
  public void webhook(String authorization, JsonNode body) {
    if (authorization == null
        || !MessageDigest.isEqual(
            secret.getBytes(StandardCharsets.UTF_8),
            authorization.getBytes(StandardCharsets.UTF_8))) throw BusinessException.unauthorized();
    JsonNode e = body.path("event");
    String id = e.path("id").asText();
    String type = e.path("type").asText();
    if ("TEST".equals(type)) return;
    if (id.isBlank() || id.length() > 150 || !e.path("event_timestamp_ms").canConvertToLong())
      throw BusinessException.invalid("Invalid billing event.");
    UUID user;
    Instant occurred;
    try {
      user = UUID.fromString(e.path("app_user_id").asText());
      occurred = Instant.ofEpochMilli(e.path("event_timestamp_ms").asLong());
    } catch (Exception ex) {
      throw BusinessException.invalid("Invalid billing user or timestamp.");
    }
    if (Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from deleted_accounts where user_id=?)", Boolean.class, user)))
      return;
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from auth.users where id=?)", Boolean.class, user))) return;
    Profile p = profiles.lock(user);
    if (Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from billing_events where id=?)", Boolean.class, id))) return;
    Instant latest =
        jdbc.queryForObject(
                    "select max(occurred_at) from billing_events where user_id=?",
                    java.sql.Timestamp.class,
                    user)
                == null
            ? null
            : jdbc.queryForObject(
                    "select max(occurred_at) from billing_events where user_id=?",
                    java.sql.Timestamp.class,
                    user)
                .toInstant();
    jdbc.update(
        "insert into billing_events(id,user_id,occurred_at) values(?,?,?)",
        id,
        user,
        java.sql.Timestamp.from(occurred));
    if (latest != null && !occurred.isAfter(latest)) return;
    boolean plus = false;
    for (var entitlement : e.path("entitlement_ids"))
      if ("plus".equals(entitlement.asText())) plus = true;
    if (!plus) return;
    Instant expiry =
        e.path("expiration_at_ms").isNumber()
            ? Instant.ofEpochMilli(e.path("expiration_at_ms").asLong())
            : null;
    provider.event(user, type, expiry);
    apply(p, provider.fetch(user));
  }

  private void apply(Profile p, BillingProvider.Entitlement entitlement) {
    p.plan = entitlement.active() ? "PLUS" : "FREE";
    p.planExpiresAt = entitlement.expiresAt();
    repository.save(p);
  }
}
