package com.spark.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class Audited {
  @Id public UUID id;
  public Instant createdAt;
  public Instant updatedAt;

  @PrePersist
  void create() {
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void update() {
    updatedAt = Instant.now();
  }
}
