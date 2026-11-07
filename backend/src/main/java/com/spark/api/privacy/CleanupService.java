package com.spark.api.privacy;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CleanupService {
  private final JdbcTemplate jdbc;
  private final InputStorage storage;
  private final AccountDeletionService accounts;
  private final Clock clock;
  private final TransactionTemplate tx;

  public CleanupService(
      JdbcTemplate jdbc,
      InputStorage storage,
      AccountDeletionService accounts,
      Clock clock,
      PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.storage = storage;
    this.accounts = accounts;
    this.clock = clock;
    this.tx = new TransactionTemplate(manager);
  }

  @Scheduled(cron = "${spark.cleanup.cron:0 0 3 * * *}", zone = "UTC")
  public void nightly() {
    clean();
    accounts.retryPending();
  }

  public void clean() {
    tx.executeWithoutResult(
        status -> {
          if (!Boolean.TRUE.equals(
              jdbc.queryForObject("select pg_try_advisory_xact_lock(782134)", Boolean.class)))
            return;
          jdbc.update(
              "update generations set status='FAILED' where status='PENDING' and reservation_expires_at<?",
              java.sql.Timestamp.from(clock.instant()));
          Set<String> retained =
              new HashSet<>(
                  jdbc.queryForList(
                      "select unnest(input_image_paths) from generations where status='SUCCEEDED'",
                      String.class));
          storage.cleanOrphans(clock.instant().minus(Duration.ofDays(1)), retained);
        });
  }
}
