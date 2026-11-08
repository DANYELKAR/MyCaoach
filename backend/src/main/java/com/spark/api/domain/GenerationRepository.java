package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationRepository extends JpaRepository<Generation, UUID> {
  Optional<Generation> findByIdAndUserId(UUID id, UUID userId);

  List<Generation> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserId(UUID userId);

  Optional<Generation> findByUserIdAndIdempotencyKey(UUID userId, String key);

  long countByUserIdAndStatusAndCreatedAtGreaterThanEqual(
      UUID userId, String status, java.time.Instant since);

  @org.springframework.data.jpa.repository.Query(
      "select count(g) from Generation g where g.userId=:userId and ((g.status='SUCCEEDED' and g.createdAt>=:since) or (g.status='PENDING' and g.reservationExpiresAt>:now))")
  long reservedUsage(UUID userId, java.time.Instant since, java.time.Instant now);

  List<Generation> findByThreadIdAndUserIdOrderByCreatedAtAsc(UUID threadId, UUID userId);

  List<Generation> findByStatusAndReservationExpiresAtBefore(String status, java.time.Instant now);

  @org.springframework.data.jpa.repository.Query(
      "select g from Generation g where g.userId=:user and g.historyDeletedAt is null and g.createdAt>=:since order by g.createdAt desc")
  org.springframework.data.domain.Page<Generation> visible(
      UUID user, java.time.Instant since, org.springframework.data.domain.Pageable pageable);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select g from Generation g where g.id=:id and g.userId=:user")
  Optional<Generation> lockOwned(UUID id, UUID user);
}
