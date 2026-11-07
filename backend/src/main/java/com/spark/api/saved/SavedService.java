package com.spark.api.saved;

import com.spark.api.domain.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.profile.ProfileService;
import com.spark.api.usage.*;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedService {
  public record FolderInput(@NotBlank @Size(max = 80) String name) {}

  public record FolderView(UUID id, String name) {}

  public record Input(
      UUID folderId,
      @Pattern(regexp = "SUGGESTION|TEMPLATE") String kind,
      UUID suggestionId,
      @Size(max = 100) String title,
      @Size(max = 2000) String text,
      @Size(max = 20) List<@NotBlank @Size(max = 50) String> tags,
      Boolean clearFolder) {}

  public record View(
      UUID id,
      UUID folderId,
      String kind,
      UUID suggestionId,
      String title,
      String text,
      String[] tags,
      java.time.Instant createdAt) {}

  private final FolderRepository folders;
  private final SavedItemRepository saved;
  private final SuggestionRepository suggestions;
  private final GenerationRepository generations;
  private final ProfileService profiles;
  private final PlanService plans;
  private final UsageService usage;

  public SavedService(
      FolderRepository folders,
      SavedItemRepository saved,
      SuggestionRepository suggestions,
      GenerationRepository generations,
      ProfileService profiles,
      PlanService plans,
      UsageService usage) {
    this.folders = folders;
    this.saved = saved;
    this.suggestions = suggestions;
    this.generations = generations;
    this.profiles = profiles;
    this.plans = plans;
    this.usage = usage;
  }

  public static View view(SavedItem s) {
    return new View(s.id, s.folderId, s.kind, s.suggestionId, s.title, s.text, s.tags, s.createdAt);
  }

  public List<FolderView> folders(UUID user) {
    return folders.findByUserIdOrderByCreatedAtDesc(user).stream()
        .map(f -> new FolderView(f.id, f.name))
        .toList();
  }

  @Transactional
  public FolderView folder(UUID user, UUID id, FolderInput input) {
    Profile p = profiles.lock(user);
    if (id == null) plans.requireCapacity(p, folders.countByUserId(user), 3);
    Folder f =
        id == null
            ? new Folder()
            : folders.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing);
    f.userId = user;
    f.name = input.name().strip();
    folders.saveAndFlush(f);
    return new FolderView(f.id, f.name);
  }

  @Transactional
  public void deleteFolder(UUID user, UUID id) {
    profiles.lock(user);
    folders.delete(folders.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing));
  }

  public List<View> list(UUID user, UUID folderId, int page, int size) {
    if (folderId != null)
      folders.findByIdAndUserId(folderId, user).orElseThrow(BusinessException::missing);
    return saved.search(user, folderId, PageRequest.of(page, size)).stream()
        .map(SavedService::view)
        .toList();
  }

  @Transactional
  public View save(UUID user, UUID id, Input b) {
    Profile p = profiles.lock(user);
    SavedItem s =
        id == null
            ? new SavedItem()
            : saved.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing);
    s.userId = user;
    if (id == null) {
      s.kind = b.kind();
      if (s.kind == null) throw BusinessException.invalid("Saved item kind is required.");
      if ("SUGGESTION".equals(s.kind)) {
        Suggestion suggestion =
            suggestions
                .findByIdAndUserId(b.suggestionId(), user)
                .orElseThrow(BusinessException::missing);
        generations
            .findByIdAndUserId(suggestion.generationId, user)
            .filter(g -> g.historyDeletedAt == null && !g.createdAt.isBefore(usage.historySince(p)))
            .orElseThrow(BusinessException::missing);
        s.suggestionId = suggestion.id;
        s.text = suggestion.text;
      } else {
        plans.requirePlus(p);
        s.text = b.text();
      }
    } else {
      if (b.kind() != null && !b.kind().equals(s.kind)
          || b.suggestionId() != null && !b.suggestionId().equals(s.suggestionId))
        throw BusinessException.invalid("Saved item source cannot be changed.");
      if ("TEMPLATE".equals(s.kind)) {
        plans.requirePlus(p);
        if (b.text() != null) s.text = b.text();
      }
    }
    if (s.text == null || s.text.isBlank()) throw BusinessException.invalid("Text is required.");
    if (b.folderId() != null)
      s.folderId =
          folders.findByIdAndUserId(b.folderId(), user).orElseThrow(BusinessException::missing).id;
    if (Boolean.TRUE.equals(b.clearFolder())) s.folderId = null;
    if (b.title() != null) s.title = b.title();
    if (b.tags() != null) s.tags = b.tags().toArray(String[]::new);
    return view(saved.saveAndFlush(s));
  }

  @Transactional
  public void delete(UUID user, UUID id) {
    profiles.lock(user);
    saved.delete(saved.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing));
  }
}
