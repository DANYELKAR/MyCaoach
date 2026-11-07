package com.spark.api.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StyleProfileRepository extends JpaRepository<StyleProfile, UUID> {}
