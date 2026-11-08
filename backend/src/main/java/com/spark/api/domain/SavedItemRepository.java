package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedItemRepository extends JpaRepository<SavedItem, UUID> {
  Optional<SavedItem> findByIdAndUserId(UUID id, UUID userId);

  List<SavedItem> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserId(UUID userId);

  @org.springframework.data.jpa.repository.Query(
      "select s from SavedItem s where s.userId=:user and (:folderId is null or s.folderId=:folderId) order by s.createdAt desc")
  org.springframework.data.domain.Page<SavedItem> search(
      UUID user, UUID folderId, org.springframework.data.domain.Pageable pageable);
}
