package com.spark.api.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "suggestions")
public class Suggestion extends Audited {
  public UUID generationId;
  public UUID userId;
  public String text;
  public int rank;
  public boolean isTopPick;
  public String feedback;
  public String feedbackReason;
  public String sentStatus = "NOT_SENT";
  public String outcome = "NONE";
}
