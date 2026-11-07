package com.spark.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "generations")
public class Generation extends Audited {
  public UUID userId;
  public UUID threadId;
  public UUID contactId;
  public String type;
  public UUID toneId;
  public String userNote;
  public String inputText;

  @JdbcTypeCode(SqlTypes.ARRAY)
  public String[] inputImagePaths = {};

  public String status = "PENDING";
  public String model;
  public Integer tokensIn;
  public Integer tokensOut;
  public String idempotencyKey;
  public Instant historyDeletedAt;
  public String fingerprint;
  public UUID attemptToken;
  public Instant reservationExpiresAt;
}
