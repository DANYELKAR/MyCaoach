package com.spark.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "profiles")
public class Profile extends Audited {
  public String displayName;
  public Instant ageConfirmedAt;
  public Instant aiConsentAt;

  @JdbcTypeCode(SqlTypes.ARRAY)
  public String[] goals = {};

  public int humorLevel = 50;
  public UUID defaultToneId;
  public String plan = "FREE";
  public Instant planExpiresAt;
  public String timezone = "UTC";
  public boolean storeImages = false;
  public boolean analyticsOptIn = true;
  public Instant timezoneChangedAt;
  public Instant deletingAt;
}
