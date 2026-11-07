package com.spark.api.saved;

import com.spark.api.identity.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@org.springframework.validation.annotation.Validated
public class SavedController {
  private final SavedService saved;
  private final CurrentUser users;

  public SavedController(SavedService saved, CurrentUser users) {
    this.saved = saved;
    this.users = users;
  }

  @GetMapping("/saved")
  public List<SavedService.View> list(
      Authentication a,
      @RequestParam(required = false) UUID folderId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
    return saved.list(users.id(a), folderId, page, size);
  }

  @PostMapping("/saved")
  public SavedService.View create(Authentication a, @Valid @RequestBody SavedService.Input b) {
    return saved.save(users.id(a), null, b);
  }

  @PatchMapping("/saved/{id}")
  public SavedService.View patch(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody SavedService.Input b) {
    return saved.save(users.id(a), id, b);
  }

  @DeleteMapping("/saved/{id}")
  public void delete(Authentication a, @PathVariable UUID id) {
    saved.delete(users.id(a), id);
  }

  @GetMapping("/folders")
  public List<SavedService.FolderView> folders(Authentication a) {
    return saved.folders(users.id(a));
  }

  @PostMapping("/folders")
  public SavedService.FolderView createFolder(
      Authentication a, @Valid @RequestBody SavedService.FolderInput b) {
    return saved.folder(users.id(a), null, b);
  }

  @PatchMapping("/folders/{id}")
  public SavedService.FolderView patchFolder(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody SavedService.FolderInput b) {
    return saved.folder(users.id(a), id, b);
  }

  @DeleteMapping("/folders/{id}")
  public void deleteFolder(Authentication a, @PathVariable UUID id) {
    saved.deleteFolder(users.id(a), id);
  }
}
