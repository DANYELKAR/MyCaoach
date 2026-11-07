package com.spark.api.domain;

import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "saved_items")
public class SavedItem extends Audited {
  public UUID userId;
  public UUID folderId;
  public String kind;
  public UUID suggestionId;
  public String title;
  public String text;

  @JdbcTypeCode(SqlTypes.ARRAY)
  public String[] tags = {};
}
