package com.spark.api.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "folders")
public class Folder extends Audited {
  public UUID userId;
  public String name;
}
