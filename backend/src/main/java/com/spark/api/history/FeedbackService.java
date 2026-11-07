package com.spark.api.history;

import com.spark.api.domain.*;
import com.spark.api.generation.GenerationDtos;
import com.spark.api.platform.BusinessException;
import com.spark.api.profile.ProfileService;
import com.spark.api.usage.UsageService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {
  public record Input(
      @Pattern(regexp = "LIKE|DISLIKE") String feedback,
      @Size(max = 500) String feedbackReason,
      @Pattern(regexp = "NOT_SENT|SENT") String sentStatus,
      @Pattern(regexp = "NONE|REPLIED|NO_REPLY") String outcome) {}

  public record ReportInput(
      @NotBlank @Size(max = 100) String reason, @Size(max = 2000) String details) {}

  private final SuggestionRepository suggestions;
  private final ReportRepository reports;
  private final GenerationRepository generations;
  private final ProfileService profiles;
  private final UsageService usage;

  public FeedbackService(
      SuggestionRepository suggestions,
      ReportRepository reports,
      GenerationRepository generations,
      ProfileService profiles,
      UsageService usage) {
    this.suggestions = suggestions;
    this.reports = reports;
    this.generations = generations;
    this.profiles = profiles;
    this.usage = usage;
  }

  private Suggestion owned(UUID user, UUID id) {
    var p = profiles.lock(user);
    var s = suggestions.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing);
    generations
        .findByIdAndUserId(s.generationId, user)
        .filter(g -> g.historyDeletedAt == null && !g.createdAt.isBefore(usage.historySince(p)))
        .orElseThrow(BusinessException::missing);
    return s;
  }

  @Transactional
  public GenerationDtos.SuggestionView patch(UUID user, UUID id, Input b) {
    var s = owned(user, id);
    if (b.feedback() != null) s.feedback = b.feedback();
    if (b.feedbackReason() != null) s.feedbackReason = b.feedbackReason();
    if (b.sentStatus() != null) s.sentStatus = b.sentStatus();
    if (b.outcome() != null) s.outcome = b.outcome();
    return GenerationDtos.view(suggestions.save(s));
  }

  @Transactional
  public UUID report(UUID user, UUID id, ReportInput b) {
    var s = owned(user, id);
    var r = new Report();
    r.userId = user;
    r.suggestionId = s.id;
    r.reason = b.reason();
    r.details = b.details();
    reports.saveAndFlush(r);
    return r.id;
  }
}
