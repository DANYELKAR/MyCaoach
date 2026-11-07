package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionRepository extends JpaRepository<Suggestion, UUID> {
  Optional<Suggestion> findByIdAndUserId(UUID id, UUID userId);

  List<Suggestion> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserId(UUID userId);

  List<Suggestion> findByGenerationIdAndUserIdOrderByRankAsc(UUID generationId, UUID userId);

  @org.springframework.data.jpa.repository.Query(
      "select s from Suggestion s, Generation g where g.id=s.generationId and s.userId=:user and g.userId=:user and g.contactId=:contact and g.historyDeletedAt is null order by s.createdAt desc,s.rank asc")
  java.util.List<Suggestion> context(
      UUID user, UUID contact, org.springframework.data.domain.Pageable pageable);
}
