package com.spark.api.history;

import com.spark.api.domain.*;
import com.spark.api.generation.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.privacy.InputStorage;
import com.spark.api.profile.ProfileService;
import com.spark.api.usage.UsageService;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {
  public record View(UUID id, UUID contactId, String title, Instant lastActivityAt) {}

  public record Timeline(View thread, List<GenerationDtos.Result> generations) {}

  private final ConversationRepository threads;
  private final GenerationRepository generations;
  private final SuggestionRepository suggestions;
  private final GenerationTransactions tx;
  private final ProfileService profiles;
  private final UsageService usage;
  private final InputStorage storage;
  private final Clock clock;

  public HistoryService(
      ConversationRepository threads,
      GenerationRepository generations,
      SuggestionRepository suggestions,
      GenerationTransactions tx,
      ProfileService profiles,
      UsageService usage,
      InputStorage storage,
      Clock clock) {
    this.threads = threads;
    this.generations = generations;
    this.suggestions = suggestions;
    this.tx = tx;
    this.profiles = profiles;
    this.usage = usage;
    this.storage = storage;
    this.clock = clock;
  }

  static View view(Conversation t) {
    return new View(t.id, t.contactId, t.title, t.lastActivityAt);
  }

  public List<View> list(UUID user, String type, int page, int size) {
    var p = profiles.ensure(user);
    return threads
        .visible(
            user,
            usage.historySince(p),
            type,
            org.springframework.data.domain.PageRequest.of(page, size))
        .stream()
        .map(HistoryService::view)
        .toList();
  }

  @Transactional
  public Timeline get(UUID user, UUID id) {
    var p = profiles.ensure(user);
    var t =
        threads
            .findByIdAndUserId(id, user)
            .filter(x -> !x.lastActivityAt.isBefore(usage.historySince(p)))
            .orElseThrow(BusinessException::missing);
    return new Timeline(
        view(t),
        generations.findByThreadIdAndUserIdOrderByCreatedAtAsc(id, user).stream()
            .filter(g -> g.historyDeletedAt == null && !g.createdAt.isBefore(usage.historySince(p)))
            .map(g -> tx.get(user, g.id))
            .toList());
  }

  @Transactional
  public void delete(UUID user, UUID id) {
    profiles.lock(user);
    if (id != null) threads.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing);
    for (var g : generations.findByUserIdOrderByCreatedAtDesc(user))
      if (id == null || id.equals(g.threadId)) {
        for (String path : g.inputImagePaths) storage.delete(path);
        suggestions.deleteAll(suggestions.findByGenerationIdAndUserIdOrderByRankAsc(g.id, user));
        g.inputImagePaths = new String[] {};
        g.userNote = null;
        g.inputText = null;
        g.threadId = null;
        g.contactId = null;
        g.historyDeletedAt = clock.instant();
        if ("PENDING".equals(g.status)) g.status = "FAILED";
        generations.save(g);
      }
    generations.flush();
    if (id == null) threads.deleteAll(threads.findByUserIdOrderByCreatedAtDesc(user));
    else threads.deleteById(id);
  }
}
