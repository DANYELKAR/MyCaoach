package com.spark.api.identity;

import com.spark.api.platform.BusinessException;
import com.spark.api.profile.ProfileService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
  private final ProfileService profiles;

  public CurrentUser(ProfileService profiles) {
    this.profiles = profiles;
  }

  public UUID id(Authentication authentication) {
    try {
      UUID id = UUID.fromString(authentication.getName());
      profiles.ensure(id);
      return id;
    } catch (IllegalArgumentException | NullPointerException e) {
      throw BusinessException.unauthorized();
    }
  }
}
