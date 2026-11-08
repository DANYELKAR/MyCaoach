package com.spark.api.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tones")
public class Tone extends Audited {
  public UUID userId;
  public String name;
  public String emoji;
  public String description;
  public String promptFragment;
  public int lengthLevel = 50;
  public int boldnessLevel = 50;
  public int humorLevel = 50;
  public int sortOrder;
  public boolean isActive = true;
}
