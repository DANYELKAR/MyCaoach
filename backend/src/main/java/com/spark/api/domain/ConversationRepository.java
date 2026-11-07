package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
  Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

  List<Conversation> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserId(UUID userId);

  Optional<Conversation> findFirstByUserIdAndContactIdOrderByLastActivityAtDesc(
      UUID userId, UUID contactId);

  @org.springframework.data.jpa.repository.Query(
      "select t from Conversation t where t.userId=:user and t.lastActivityAt>=:since and (:type is null or exists(select g.id from Generation g where g.threadId=t.id and g.type=:type and g.historyDeletedAt is null and g.createdAt>=:since)) order by t.lastActivityAt desc")
  org.springframework.data.domain.Page<Conversation> visible(
      UUID user,
      java.time.Instant since,
      String type,
      org.springframework.data.domain.Pageable pageable);
}
