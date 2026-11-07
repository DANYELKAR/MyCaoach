package com.spark.api.history;

import com.spark.api.generation.GenerationDtos;
import com.spark.api.identity.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@org.springframework.validation.annotation.Validated
public class HistoryController {
  private final HistoryService history;
  private final FeedbackService feedback;
  private final CurrentUser users;

  public HistoryController(HistoryService history, FeedbackService feedback, CurrentUser users) {
    this.history = history;
    this.feedback = feedback;
    this.users = users;
  }

  @GetMapping("/threads")
  public List<HistoryService.View> list(
      Authentication a,
      @RequestParam(required = false) @Pattern(regexp = "OPENER|REPLY") String type,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
    return history.list(users.id(a), type, page, size);
  }

  @GetMapping("/threads/{id}")
  public HistoryService.Timeline get(Authentication a, @PathVariable UUID id) {
    return history.get(users.id(a), id);
  }

  @DeleteMapping("/threads/{id}")
  public void delete(Authentication a, @PathVariable UUID id) {
    history.delete(users.id(a), id);
  }

  @DeleteMapping("/threads")
  public void deleteAll(Authentication a) {
    history.delete(users.id(a), null);
  }

  @PatchMapping("/suggestions/{id}")
  public GenerationDtos.SuggestionView patch(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody FeedbackService.Input b) {
    return feedback.patch(users.id(a), id, b);
  }

  @PostMapping("/suggestions/{id}/report")
  public Map<String, UUID> report(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody FeedbackService.ReportInput b) {
    return Map.of("id", feedback.report(users.id(a), id, b));
  }
}
