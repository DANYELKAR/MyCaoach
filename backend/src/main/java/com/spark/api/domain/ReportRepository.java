package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, UUID> {
  Optional<Report> findByIdAndUserId(UUID id, UUID userId);

  List<Report> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserId(UUID userId);
}
