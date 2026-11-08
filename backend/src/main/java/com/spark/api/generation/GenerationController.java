package com.spark.api.generation;

import com.spark.api.identity.CurrentUser;
import com.spark.api.platform.RateLimitService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/generations")
@org.springframework.validation.annotation.Validated
public class GenerationController {
  private final com.spark.api.privacy.InputStorage storage;
  private final GenerationService service;
  private final GenerationTransactions tx;
  private final CurrentUser users;
  private final RateLimitService limits;

  public GenerationController(
      com.spark.api.privacy.InputStorage storage,
      GenerationService service,
      GenerationTransactions tx,
      CurrentUser users,
      RateLimitService limits) {
    this.storage = storage;
    this.service = service;
    this.tx = tx;
    this.users = users;
    this.limits = limits;
  }

  @PostMapping(value = "/opener", consumes = "multipart/form-data")
  public GenerationDtos.Result opener(
      Authentication a,
      @RequestPart MultipartFile image,
      @RequestParam UUID toneId,
      @RequestParam(required = false) String note,
      @RequestParam(required = false) UUID contactId,
      @RequestParam String idempotencyKey) {
    UUID user = users.id(a);
    limits.check(user);
    return service.generate(
        user,
        new GenerationInput(
            "OPENER",
            toneId,
            note,
            null,
            contactId,
            idempotencyKey,
            List.of(GenerationInput.image(image))));
  }

  @PostMapping(value = "/reply", consumes = "multipart/form-data")
  public GenerationDtos.Result reply(
      Authentication a,
      @RequestPart(required = false) List<MultipartFile> images,
      @RequestParam(required = false) String text,
      @RequestParam UUID toneId,
      @RequestParam(required = false) String note,
      @RequestParam(required = false) UUID contactId,
      @RequestParam String idempotencyKey) {
    UUID user = users.id(a);
    limits.check(user);
    return service.generate(
        user,
        new GenerationInput(
            "REPLY",
            toneId,
            note,
            text,
            contactId,
            idempotencyKey,
            images == null ? List.of() : images.stream().map(GenerationInput::image).toList()));
  }

  @GetMapping
  public List<GenerationDtos.View> list(
      Authentication a,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
    return tx.list(users.id(a), page, size);
  }

  @GetMapping("/{id}")
  public GenerationDtos.Result get(Authentication a, @PathVariable UUID id) {
    return tx.get(users.id(a), id);
  }

  @GetMapping("/{id}/images/{index}")
  public org.springframework.http.ResponseEntity<byte[]> image(
      Authentication a, @PathVariable UUID id, @PathVariable int index) {
    var access = storage.access(tx.imagePath(users.id(a), id, index));
    if (access.signedUrl() != null)
      return org.springframework.http.ResponseEntity.status(303)
          .location(java.net.URI.create(access.signedUrl()))
          .cacheControl(org.springframework.http.CacheControl.noStore())
          .build();
    return org.springframework.http.ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.parseMediaType(access.mimeType()))
        .cacheControl(org.springframework.http.CacheControl.noStore())
        .body(access.bytes());
  }
}
