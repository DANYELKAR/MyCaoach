package com.spark.api.tones;

import com.spark.api.domain.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.profile.ProfileService;
import com.spark.api.usage.PlanService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToneService {
  public record Input(
      @Size(max = 40) String name,
      @Size(max = 20) String emoji,
      @Size(max = 300) String description,
      @Size(max = 1000) String styleInstruction,
      @Min(0) @Max(100) Integer lengthLevel,
      @Min(0) @Max(100) Integer boldnessLevel,
      @Min(0) @Max(100) Integer humorLevel) {}

  public record View(
      UUID id,
      String name,
      String emoji,
      String description,
      boolean builtIn,
      int lengthLevel,
      int boldnessLevel,
      int humorLevel) {}

  private final ToneRepository tones;
  private final ProfileService profiles;
  private final PlanService plans;

  public ToneService(ToneRepository tones, ProfileService profiles, PlanService plans) {
    this.tones = tones;
    this.profiles = profiles;
    this.plans = plans;
  }

  public static View view(Tone t) {
    return new View(
        t.id,
        t.name,
        t.emoji,
        t.description,
        t.userId == null,
        t.lengthLevel,
        t.boldnessLevel,
        t.humorLevel);
  }

  public List<View> list(UUID user) {
    return tones.findByUserIdIsNullOrUserIdOrderBySortOrderAsc(user).stream()
        .filter(t -> t.isActive)
        .map(ToneService::view)
        .toList();
  }

  public Tone ownedOrBuiltIn(UUID user, UUID id) {
    return tones
        .findById(id)
        .filter(t -> t.isActive && (t.userId == null || user.equals(t.userId)))
        .orElseThrow(BusinessException::missing);
  }

  @Transactional
  public View save(UUID user, UUID id, Input b) {
    plans.requirePlus(profiles.lock(user));
    Tone t =
        id == null
            ? new Tone()
            : tones.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing);
    t.userId = user;
    if (b.name() != null) t.name = b.name().strip();
    if (t.name == null || t.name.isBlank())
      throw BusinessException.invalid("Tone name is required.");
    if (b.emoji() != null) t.emoji = b.emoji();
    if (b.description() != null) t.description = b.description();
    if (b.styleInstruction() != null) {
      // Instructions are treated as untrusted data by the prompt builder, never as system rules.
      String instruction = b.styleInstruction().strip();
      if (instruction.isEmpty()
          || instruction.matches(
              "(?is).*(ignore.{0,30}(previous|system)|system\\s*:|developer\\s*:).*"))
        throw BusinessException.invalid("Invalid style instruction.");
      t.promptFragment = instruction;
    }
    if (t.promptFragment == null) throw BusinessException.invalid("Style instruction is required.");
    if (b.lengthLevel() != null) t.lengthLevel = b.lengthLevel();
    if (b.boldnessLevel() != null) t.boldnessLevel = b.boldnessLevel();
    if (b.humorLevel() != null) t.humorLevel = b.humorLevel();
    return view(tones.save(t));
  }

  @Transactional
  public void delete(UUID user, UUID id) {
    plans.requirePlus(profiles.lock(user));
    Tone t = tones.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing);
    t.isActive = false;
    tones.save(t);
  }
}
