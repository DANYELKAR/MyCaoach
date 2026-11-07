package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, UUID> {
  Optional<Folder> findByIdAndUserId(UUID id, UUID userId);

  List<Folder> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserId(UUID userId);
}
