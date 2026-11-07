package com.spark.api.tones;

import com.spark.api.identity.CurrentUser;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tones")
public class ToneController {
  private final ToneService tones;
  private final CurrentUser users;

  public ToneController(ToneService tones, CurrentUser users) {
    this.tones = tones;
    this.users = users;
  }

  @GetMapping
  public List<ToneService.View> list(Authentication a) {
    return tones.list(users.id(a));
  }

  @PostMapping
  public ToneService.View create(Authentication a, @Valid @RequestBody ToneService.Input b) {
    return tones.save(users.id(a), null, b);
  }

  @PatchMapping("/{id}")
  public ToneService.View update(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody ToneService.Input b) {
    return tones.save(users.id(a), id, b);
  }

  @DeleteMapping("/{id}")
  public void delete(Authentication a, @PathVariable UUID id) {
    tones.delete(users.id(a), id);
  }
}
