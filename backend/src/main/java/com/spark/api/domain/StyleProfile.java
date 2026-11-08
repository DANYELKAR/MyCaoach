package com.spark.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "style_profiles")
public class StyleProfile {
  @Id public UUID userId;

  @JdbcTypeCode(SqlTypes.ARRAY)
  public String[] sampleMessages = {};

  @JdbcTypeCode(SqlTypes.JSON)
  public Map<String, Object> summary;

  public boolean enabled = true;
  public Instant createdAt = Instant.now();
  public Instant updatedAt = Instant.now();
}
