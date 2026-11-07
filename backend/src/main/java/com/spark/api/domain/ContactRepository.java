package com.spark.api.domain;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
  Optional<Contact> findByIdAndUserId(UUID id, UUID userId);

  List<Contact> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserId(UUID userId);

  @org.springframework.data.jpa.repository.Query(
      "select c from Contact c where c.userId=:user and (:status is null or c.status=:status) and lower(c.name) like lower(concat('%',:q,'%')) order by c.createdAt desc")
  org.springframework.data.domain.Page<Contact> search(
      UUID user, String status, String q, org.springframework.data.domain.Pageable pageable);
}
