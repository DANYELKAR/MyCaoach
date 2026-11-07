package com.spark.api.ai;

import com.spark.api.domain.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.profile.ProfileService;
import com.spark.api.usage.PlanService;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StyleService {
  public record Input(
      @Size(min = 5, max = 10) List<@NotBlank @Size(min = 2, max = 500) String> sampleMessages,
      Boolean enabled) {}

  public record View(String[] sampleMessages, Map<String, Object> summary, boolean enabled) {}

  private final StyleProfileRepository styles;
  private final ProfileService profiles;
  private final PlanService plans;
  private final AiClient ai;
  private final Clock clock;
  private final TransactionTemplate tx;

  public StyleService(
      StyleProfileRepository styles,
      ProfileService profiles,
      PlanService plans,
      AiClient ai,
      Clock clock,
      PlatformTransactionManager manager) {
    this.styles = styles;
    this.profiles = profiles;
    this.plans = plans;
    this.ai = ai;
    this.clock = clock;
    this.tx = new TransactionTemplate(manager);
  }

  static View view(StyleProfile s) {
    return new View(s.sampleMessages, s.summary, s.enabled);
  }

  public View get(UUID user) {
    plans.requirePlus(profiles.ensure(user));
    return styles
        .findById(user)
        .map(StyleService::view)
        .orElse(new View(new String[] {}, null, true));
  }

  public View save(UUID user, Input b) {
    return tx.execute(
        status -> {
          plans.requirePlus(profiles.lock(user));
          var s = styles.findById(user).orElseGet(StyleProfile::new);
          s.userId = user;
          if (b.sampleMessages() != null) {
            s.sampleMessages = b.sampleMessages().toArray(String[]::new);
            s.summary = null;
          }
          if (b.enabled() != null) s.enabled = b.enabled();
          s.updatedAt = clock.instant();
          return view(styles.save(s));
        });
  }

  public View analyze(UUID user) {
    View snapshot = get(user);
    if (snapshot.sampleMessages().length < 5)
      throw BusinessException.invalid("Provide 5 to 10 sample messages.");
    Map<String, Object> summary = ai.analyzeStyle(List.of(snapshot.sampleMessages()));
    if (summary == null
        || !summary
            .keySet()
            .containsAll(Set.of("traits", "formality", "length", "emojiUsage", "sampleLine")))
      throw OutputValidator.unavailable();
    return tx.execute(
        status -> {
          plans.requirePlus(profiles.lock(user));
          var s = styles.findById(user).orElseThrow(BusinessException::missing);
          if (!Arrays.equals(s.sampleMessages, snapshot.sampleMessages()))
            throw new BusinessException(
                409, "STYLE_CHANGED", "Style samples changed. Please analyze again.");
          s.summary = summary;
          s.updatedAt = clock.instant();
          return view(styles.save(s));
        });
  }

  public void delete(UUID user) {
    tx.executeWithoutResult(
        status -> {
          profiles.lock(user);
          styles.deleteById(user);
        });
  }
}
