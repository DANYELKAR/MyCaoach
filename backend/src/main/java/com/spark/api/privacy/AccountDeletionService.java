package com.spark.api.privacy;

import com.spark.api.domain.*;
import com.spark.api.profile.ProfileService;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AccountDeletionService {
  private final ProfileService profiles;
  private final ProfileRepository repository;
  private final JdbcTemplate jdbc;
  private final InputStorage storage;
  private final AuthAdmin auth;
  private final Clock clock;
  private final TransactionTemplate tx;

  public AccountDeletionService(
      ProfileService profiles,
      ProfileRepository repository,
      JdbcTemplate jdbc,
      InputStorage storage,
      AuthAdmin auth,
      Clock clock,
      PlatformTransactionManager manager) {
    this.profiles = profiles;
    this.repository = repository;
    this.jdbc = jdbc;
    this.storage = storage;
    this.auth = auth;
    this.clock = clock;
    this.tx = new TransactionTemplate(manager);
  }

  public void delete(UUID user, String token) {
    tx.executeWithoutResult(
        status -> {
          boolean started =
              Boolean.TRUE.equals(
                  jdbc.queryForObject(
                      "select exists(select 1 from deleted_accounts where user_id=?)",
                      Boolean.class,
                      user));
          if (!started) {
            profiles.lock(user);
            jdbc.update(
                "insert into deleted_accounts(user_id) values(?) on conflict do nothing", user);
          }
          jdbc.update(
              "update profiles set deleting_at=coalesce(deleting_at,?) where id=?",
              java.sql.Timestamp.from(clock.instant()),
              user);
        });
    finish(user, token);
  }

  public void finish(UUID user, String token) {
    auth.revoke(user, token);
    storage.deleteUserObjects(user);
    auth.deleteUser(user);
    tx.executeWithoutResult(status -> jdbc.update("delete from profiles where id=?", user));
  }

  public void retryPending() {
    for (UUID user :
        jdbc.query(
            "select id from profiles where deleting_at is not null order by deleting_at limit 25",
            (rs, n) -> rs.getObject(1, UUID.class)))
      try {
        finish(user, null);
      } catch (RuntimeException e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("account_deletion_retry_pending userId={}", user);
      }
  }
}
