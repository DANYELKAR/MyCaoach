package com.spark.api.profile;

import com.spark.api.domain.Profile;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public class ProfileDtos {
  public record View(
      UUID id,
      String displayName,
      Instant ageConfirmedAt,
      Instant aiConsentAt,
      String[] goals,
      int humorLevel,
      UUID defaultToneId,
      String plan,
      Instant planExpiresAt,
      String timezone,
      boolean storeImages,
      boolean analyticsOptIn) {}

  public record Patch(
      @Size(max = 80) String displayName,
      Boolean ageConfirmed,
      Boolean aiConsent,
      @Size(max = 10) List<@NotBlank @Size(max = 80) String> goals,
      @Min(0) @Max(100) Integer humorLevel,
      UUID defaultToneId,
      @Size(max = 80) String timezone,
      Boolean storeImages,
      Boolean analyticsOptIn) {}

  public static View view(Profile p) {
    return new View(
        p.id,
        p.displayName,
        p.ageConfirmedAt,
        p.aiConsentAt,
        p.goals,
        p.humorLevel,
        p.defaultToneId,
        p.plan,
        p.planExpiresAt,
        p.timezone,
        p.storeImages,
        p.analyticsOptIn);
  }
}
