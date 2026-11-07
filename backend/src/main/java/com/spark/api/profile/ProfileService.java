package com.spark.api.profile;

import com.spark.api.domain.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.usage.PlanService;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
  @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entities;
  private final ProfileRepository profiles;
  private final ToneRepository tones;
  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final PlanService plans;

  public ProfileService(
      ProfileRepository profiles,
      ToneRepository tones,
      JdbcTemplate jdbc,
      Clock clock,
      PlanService plans) {
    this.profiles = profiles;
    this.tones = tones;
    this.jdbc = jdbc;
    this.clock = clock;
    this.plans = plans;
  }

  @Transactional
  public Profile ensure(UUID id) {
    if (Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from deleted_accounts where user_id=?)", Boolean.class, id)))
      throw BusinessException.unauthorized();
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from auth.users where id=?)", Boolean.class, id)))
      throw BusinessException.unauthorized();
    if (!profiles.existsById(id))
      jdbc.update("insert into profiles(id) values(?) on conflict(id) do nothing", id);
    Profile p = profiles.findById(id).orElseThrow(BusinessException::unauthorized);
    if (p.deletingAt != null) throw BusinessException.unauthorized();
    return p;
  }

  @Transactional
  public Profile lock(UUID id) {
    ensure(id);
    Profile p = profiles.lockById(id).orElseThrow(BusinessException::unauthorized);
    entities.refresh(p);
    if (p.deletingAt != null) throw BusinessException.unauthorized();
    return p;
  }

  @Transactional
  public Profile patch(UUID id, ProfileDtos.Patch body) {
    Profile p = lock(id);
    Instant now = clock.instant();
    if (body.displayName() != null) p.displayName = body.displayName().strip();
    if (Boolean.TRUE.equals(body.ageConfirmed()) && p.ageConfirmedAt == null)
      p.ageConfirmedAt = now;
    if (body.ageConfirmed() != null && !body.ageConfirmed()) p.ageConfirmedAt = null;
    if (Boolean.TRUE.equals(body.aiConsent()) && p.aiConsentAt == null) p.aiConsentAt = now;
    if (body.aiConsent() != null && !body.aiConsent()) p.aiConsentAt = null;
    if (body.goals() != null) p.goals = body.goals().toArray(String[]::new);
    if (body.humorLevel() != null) p.humorLevel = body.humorLevel();
    if (body.timezone() != null && !body.timezone().equals(p.timezone)) {
      try {
        ZoneId.of(body.timezone());
      } catch (DateTimeException e) {
        throw BusinessException.invalid("Invalid IANA timezone.");
      }
      if (p.timezoneChangedAt != null && p.timezoneChangedAt.plus(Duration.ofDays(1)).isAfter(now))
        throw new BusinessException(
            409, "TIMEZONE_LOCKED", "Timezone can be changed once per 24 hours.");
      Long used =
          jdbc.queryForObject(
              "select count(*) from generations where user_id=? and created_at>? and status in ('PENDING','SUCCEEDED')",
              Long.class,
              id,
              java.sql.Timestamp.from(now.minus(Duration.ofDays(1))));
      if (used != null && used > 0)
        throw new BusinessException(
            409, "TIMEZONE_LOCKED", "Wait 24 hours after your last generation to change timezone.");
      p.timezone = body.timezone();
      p.timezoneChangedAt = now;
    }
    if (body.defaultToneId() != null) {
      Tone tone =
          tones
              .findById(body.defaultToneId())
              .filter(t -> t.isActive && (t.userId == null || id.equals(t.userId)))
              .orElseThrow(BusinessException::missing);
      if (tone.userId != null) plans.requirePlus(p);
      p.defaultToneId = tone.id;
    }
    if (body.storeImages() != null) p.storeImages = body.storeImages();
    if (body.analyticsOptIn() != null) p.analyticsOptIn = body.analyticsOptIn();
    return profiles.save(p);
  }
}
