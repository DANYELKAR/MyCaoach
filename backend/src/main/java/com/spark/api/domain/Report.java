package com.spark.api.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report extends Audited {
  public UUID userId;
  public UUID suggestionId;
  public String reason;
  public String details;
}
