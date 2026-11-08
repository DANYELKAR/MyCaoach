package com.spark.api.profile;

import com.spark.api.identity.CurrentUser;
import com.spark.api.usage.UsageService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/me")
public class ProfileController {
  private final ProfileService profiles;
  private final CurrentUser users;
  private final UsageService usage;

  public ProfileController(ProfileService profiles, CurrentUser users, UsageService usage) {
    this.profiles = profiles;
    this.users = users;
    this.usage = usage;
  }

  @GetMapping
  public ProfileDtos.View me(Authentication a) {
    return ProfileDtos.view(profiles.ensure(users.id(a)));
  }

  @PatchMapping
  public ProfileDtos.View patch(Authentication a, @Valid @RequestBody ProfileDtos.Patch body) {
    return ProfileDtos.view(profiles.patch(users.id(a), body));
  }

  @GetMapping("/usage")
  public UsageService.Usage usage(Authentication a) {
    return usage.usage(profiles.ensure(users.id(a)));
  }
}
