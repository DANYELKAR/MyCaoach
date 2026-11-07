package com.spark.api.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.api.platform.BusinessException;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("supabase")
public class RevenueCatProvider implements BillingProvider {
  private final ObjectMapper json;
  private final String key, base;
  private final Clock clock;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public RevenueCatProvider(
      ObjectMapper json,
      Clock clock,
      @Value("${REVENUECAT_API_KEY}") String key,
      @Value("${REVENUECAT_BASE_URL:https://api.revenuecat.com/v1}") String base) {
    if (key.isBlank()) throw new IllegalArgumentException("RevenueCat key required");
    this.json = json;
    this.clock = clock;
    this.key = key;
    this.base = base;
  }

  public Entitlement fetch(UUID user) {
    try {
      var req =
          HttpRequest.newBuilder(URI.create(base + "/subscribers/" + user))
              .timeout(Duration.ofSeconds(10))
              .header("Authorization", "Bearer " + key)
              .GET()
              .build();
      var response = http.send(req, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 404) return new Entitlement(false, null);
      if (response.statusCode() != 200) throw unavailable();
      var plus =
          json.readTree(response.body()).path("subscriber").path("entitlements").path("plus");
      if (plus.isMissingNode() || plus.isNull()) return new Entitlement(false, null);
      Instant expiry =
          plus.path("expires_date").isNull()
              ? null
              : Instant.parse(plus.path("expires_date").asText());
      return new Entitlement(expiry == null || expiry.isAfter(clock.instant()), expiry);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw unavailable();
    } catch (Exception e) {
      throw unavailable();
    }
  }

  private BusinessException unavailable() {
    return new BusinessException(
        503, "BILLING_UNAVAILABLE", "Subscription status is temporarily unavailable.");
  }
}
