package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToneRepository extends JpaRepository<Tone, UUID> {
  Optional<Tone> findByIdAndUserId(UUID id, UUID userId);

  List<Tone> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserId(UUID userId);

  List<Tone> findByUserIdIsNullOrUserIdOrderBySortOrderAsc(UUID userId);
}
