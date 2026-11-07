package com.spark.api.generation;

import com.spark.api.domain.*;
import com.spark.api.usage.UsageService;
import java.time.Instant;
import java.util.*;

public class GenerationDtos {
  public record View(
      UUID id,
      String type,
      UUID toneId,
      UUID contactId,
      UUID threadId,
      String status,
      Instant createdAt) {}

  public record SuggestionView(
      UUID id,
      String text,
      int rank,
      boolean isTopPick,
      String feedback,
      String feedbackReason,
      String sentStatus,
      String outcome) {}

  public record Result(
      View generation, List<SuggestionView> suggestions, UsageService.Usage usage) {}

  public static View view(Generation g) {
    return new View(g.id, g.type, g.toneId, g.contactId, g.threadId, g.status, g.createdAt);
  }

  public static SuggestionView view(Suggestion s) {
    return new SuggestionView(
        s.id, s.text, s.rank, s.isTopPick, s.feedback, s.feedbackReason, s.sentStatus, s.outcome);
  }
}
