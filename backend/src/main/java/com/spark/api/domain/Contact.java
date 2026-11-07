package com.spark.api.domain;

import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contacts")
public class Contact extends Audited {
  public UUID userId;
  public String name;
  public String source;
  public String status = "NEW";

  @JdbcTypeCode(SqlTypes.ARRAY)
  public String[] interests = {};

  public String notes;
  public String photoPath;
}
