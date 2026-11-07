package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select p from Profile p where p.id=:id")
  Optional<Profile> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
}
