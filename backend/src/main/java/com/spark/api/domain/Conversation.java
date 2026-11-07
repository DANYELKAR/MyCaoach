package com.spark.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threads")
public class Conversation extends Audited {
  public UUID userId;
  public UUID contactId;
  public String title;
  public Instant lastActivityAt;
}
