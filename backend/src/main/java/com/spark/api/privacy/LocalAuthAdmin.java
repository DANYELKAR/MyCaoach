package com.spark.api.privacy;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalAuthAdmin implements AuthAdmin {
  private final JdbcTemplate jdbc;

  public LocalAuthAdmin(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void revoke(
      UUID user,
      String
          token) {} // Local tokens are rejected by deleting_at/tombstone checks; no refresh token

  // is issued.

  public void deleteUser(UUID user) {
    jdbc.update("delete from auth.users where id=?", user);
  }
}
