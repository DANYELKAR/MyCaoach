package com.spark.api.identity;

import com.spark.api.platform.BusinessException;
import com.spark.api.profile.ProfileService;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
public class LocalAuthService {
  public record Credentials(
      @NotBlank @Email @Size(max = 200) String email,
      @NotBlank @Size(min = 10, max = 72) String password) {}

  public record Token(UUID userId, String accessToken, long expiresIn) {}

  private final JdbcTemplate jdbc;
  private final JwtEncoder encoder;
  private final Clock clock;
  private final ProfileService profiles;
  private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

  public LocalAuthService(
      JdbcTemplate jdbc, JwtEncoder encoder, Clock clock, ProfileService profiles) {
    this.jdbc = jdbc;
    this.encoder = encoder;
    this.clock = clock;
    this.profiles = profiles;
  }

  @Transactional
  public Token register(Credentials c) {
    UUID id = UUID.randomUUID();
    try {
      jdbc.update(
          "insert into auth.users(id,email,password_hash) values(?,?,?)",
          id,
          c.email().toLowerCase(Locale.ROOT),
          passwords.encode(c.password()));
    } catch (DuplicateKeyException e) {
      throw new BusinessException(409, "ACCOUNT_EXISTS", "Account already exists.");
    }
    profiles.ensure(id);
    return token(id);
  }

  public Token login(Credentials c) {
    var rows =
        jdbc.queryForList(
            "select id,password_hash from auth.users where email=?",
            c.email().toLowerCase(Locale.ROOT));
    if (rows.isEmpty()
        || !passwords.matches(c.password(), (String) rows.getFirst().get("password_hash")))
      throw BusinessException.unauthorized();
    UUID id = (UUID) rows.getFirst().get("id");
    profiles.ensure(id);
    return token(id);
  }

  private Token token(UUID id) {
    Instant now = clock.instant();
    var claims =
        JwtClaimsSet.builder()
            .issuer("http://spark-local")
            .subject(id.toString())
            .audience(List.of("authenticated"))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .build();
    return new Token(id, encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue(), 3600);
  }
}
