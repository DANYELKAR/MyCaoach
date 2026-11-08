package com.spark.api.generation;

import com.spark.api.ai.*;
import com.spark.api.domain.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.profile.ProfileService;
import com.spark.api.tones.ToneService;
import com.spark.api.usage.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerationTransactions {
  public record Completion(GenerationDtos.Result response, boolean retainImages) {}

  public record Reservation(
      UUID id, UUID attempt, String prompt, boolean storeImages, boolean replay) {}

  private final GenerationRepository generations;
  private final SuggestionRepository suggestions;
  private final ConversationRepository threads;
  private final ContactRepository contacts;
  private final StyleProfileRepository styles;
  private final ProfileService profiles;
  private final PlanService plans;
  private final UsageService usage;
  private final ToneService tones;
  private final PromptBuilder prompts;
  private final Clock clock;

  public GenerationTransactions(
      GenerationRepository generations,
      SuggestionRepository suggestions,
      ConversationRepository threads,
      ContactRepository contacts,
      StyleProfileRepository styles,
      ProfileService profiles,
      PlanService plans,
      UsageService usage,
      ToneService tones,
      PromptBuilder prompts,
      Clock clock) {
    this.generations = generations;
    this.suggestions = suggestions;
    this.threads = threads;
    this.contacts = contacts;
    this.styles = styles;
    this.profiles = profiles;
    this.plans = plans;
    this.usage = usage;
    this.tones = tones;
    this.prompts = prompts;
    this.clock = clock;
  }

  @Transactional
  public Reservation reserve(UUID user, GenerationInput in, String fingerprint) {
    Profile p = profiles.lock(user);
    Instant now = clock.instant();
    Generation g =
        generations.findByUserIdAndIdempotencyKey(user, in.idempotencyKey()).orElse(null);
    if (g != null) {
      if (!g.fingerprint.equals(fingerprint))
        throw new BusinessException(
            409, "IDEMPOTENCY_CONFLICT", "This key belongs to a different request.");
      if (g.historyDeletedAt != null)
        throw new BusinessException(410, "HISTORY_DELETED", "This generation was deleted.");
      if ("SUCCEEDED".equals(g.status)) {
        if (g.createdAt.isBefore(usage.historySince(p)))
          throw new BusinessException(
              403, "FORBIDDEN_PLAN", "Older history requires MyCoach Plus.");
        return new Reservation(g.id, null, null, false, true);
      }
      if ("PENDING".equals(g.status) && g.reservationExpiresAt.isAfter(now))
        throw new BusinessException(
            409, "GENERATION_PENDING", "This generation is still processing.");
      if ("BLOCKED".equals(g.status))
        throw new BusinessException(
            422, "CONTENT_BLOCKED", "This request cannot be processed safely.");
    }
    if (p.ageConfirmedAt == null || p.aiConsentAt == null)
      throw new BusinessException(
          422, "CONSENT_REQUIRED", "Confirm you are 18+ and consent to AI processing.");
    if ("REPLY".equals(in.type())) plans.requirePlus(p);
    Tone tone = tones.ownedOrBuiltIn(user, in.toneId());
    if (tone.userId != null) plans.requirePlus(p);
    Contact contact =
        in.contactId() == null
            ? null
            : contacts
                .findByIdAndUserId(in.contactId(), user)
                .orElseThrow(BusinessException::missing);
    var window = UsageService.window(now, p.timezone);
    if (generations.reservedUsage(user, window.startsAt(), now) >= plans.limit(p))
      throw new BusinessException(402, "OUT_OF_CREDITS", "Daily generation limit reached.");
    if (g == null) {
      g = new Generation();
      g.userId = user;
      g.idempotencyKey = in.idempotencyKey();
      g.fingerprint = fingerprint;
    }
    g.createdAt = now;
    g.type = in.type();
    g.toneId = tone.id;
    g.contactId = in.contactId();
    g.userNote = in.note();
    g.inputText = in.text();
    g.status = "PENDING";
    g.attemptToken = UUID.randomUUID();
    g.reservationExpiresAt = now.plusSeconds(120);
    g = generations.saveAndFlush(g);
    List<String> previous =
        contact == null
            ? List.of()
            : suggestions
                .context(user, contact.id, org.springframework.data.domain.PageRequest.of(0, 5))
                .stream()
                .map(s -> s.text + " [feedback=" + s.feedback + ",outcome=" + s.outcome + "]")
                .toList();
    Map<String, Object> style =
        plans.isPlus(p)
            ? styles.findById(user).filter(s -> s.enabled).map(s -> s.summary).orElse(null)
            : null;
    return new Reservation(
        g.id,
        g.attemptToken,
        prompts.build(in.type(), tone, in.note(), in.text(), contact, previous, style),
        p.storeImages,
        false);
  }

  @Transactional
  public Completion complete(
      UUID user,
      Reservation reservation,
      AiClient.Result result,
      List<String> values,
      List<String> paths) {
    Profile p = profiles.lock(user);
    Generation g =
        generations
            .findByIdAndUserId(reservation.id(), user)
            .orElseThrow(BusinessException::missing);
    if (!"PENDING".equals(g.status)
        || !reservation.attempt().equals(g.attemptToken)
        || !g.reservationExpiresAt.isAfter(clock.instant()))
      throw new BusinessException(
          409, "GENERATION_EXPIRED", "Generation reservation expired. Please retry.");
    if (p.ageConfirmedAt == null || p.aiConsentAt == null)
      throw new BusinessException(
          422, "CONSENT_REQUIRED", "AI processing consent has been withdrawn.");
    Conversation thread =
        g.contactId == null
            ? new Conversation()
            : threads
                .findFirstByUserIdAndContactIdOrderByLastActivityAtDesc(user, g.contactId)
                .orElseGet(Conversation::new);
    thread.userId = user;
    thread.contactId = g.contactId;
    if (thread.title == null)
      thread.title = "REPLY".equals(g.type) ? "Continue chat" : "New opener";
    thread.lastActivityAt = clock.instant();
    threads.saveAndFlush(thread);
    g.threadId = thread.id;
    g.inputImagePaths = p.storeImages ? paths.toArray(String[]::new) : new String[] {};
    g.model = result.model();
    g.tokensIn = result.tokensIn();
    g.tokensOut = result.tokensOut();
    g.status = "SUCCEEDED";
    List<Suggestion> saved = new ArrayList<>();
    for (int i = 0; i < values.size(); i++) {
      var s = new Suggestion();
      s.generationId = g.id;
      s.userId = user;
      s.text = values.get(i);
      s.rank = i + 1;
      s.isTopPick = i == 0;
      saved.add(suggestions.save(s));
    }
    generations.saveAndFlush(g);
    return new Completion(
        new GenerationDtos.Result(
            GenerationDtos.view(g),
            saved.stream().map(GenerationDtos::view).toList(),
            usage.usage(p)),
        p.storeImages);
  }

  @Transactional
  public void fail(UUID user, Reservation reservation, boolean blocked) {
    // No account provisioning here: deletion or a newer attempt must never be undone.
    generations
        .lockOwned(reservation.id(), user)
        .filter(g -> "PENDING".equals(g.status) && reservation.attempt().equals(g.attemptToken))
        .ifPresent(
            g -> {
              g.status = blocked ? "BLOCKED" : "FAILED";
              generations.save(g);
            });
  }

  @Transactional
  public GenerationDtos.Result get(UUID user, UUID id) {
    Profile p = profiles.ensure(user);
    Generation g =
        generations
            .findByIdAndUserId(id, user)
            .filter(x -> x.historyDeletedAt == null && !x.createdAt.isBefore(usage.historySince(p)))
            .orElseThrow(BusinessException::missing);
    return new GenerationDtos.Result(
        GenerationDtos.view(g),
        suggestions.findByGenerationIdAndUserIdOrderByRankAsc(id, user).stream()
            .map(GenerationDtos::view)
            .toList(),
        usage.usage(p));
  }

  public List<GenerationDtos.View> list(UUID user, int page, int size) {
    Profile p = profiles.ensure(user);
    return generations
        .visible(
            user, usage.historySince(p), org.springframework.data.domain.PageRequest.of(page, size))
        .stream()
        .map(GenerationDtos::view)
        .toList();
  }

  @Transactional
  public String imagePath(UUID user, UUID id, int index) {
    Profile p = profiles.ensure(user);
    Generation g =
        generations
            .findByIdAndUserId(id, user)
            .filter(x -> x.historyDeletedAt == null && !x.createdAt.isBefore(usage.historySince(p)))
            .orElseThrow(BusinessException::missing);
    if (index < 0 || index >= g.inputImagePaths.length) throw BusinessException.missing();
    return g.inputImagePaths[index];
  }
}
