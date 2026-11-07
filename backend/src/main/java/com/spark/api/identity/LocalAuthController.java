package com.spark.api.identity;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("local")
@RequestMapping("/local/auth")
public class LocalAuthController {
  private final LocalAuthService auth;

  public LocalAuthController(LocalAuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/register")
  public LocalAuthService.Token register(@Valid @RequestBody LocalAuthService.Credentials c) {
    return auth.register(c);
  }

  @PostMapping("/login")
  public LocalAuthService.Token login(@Valid @RequestBody LocalAuthService.Credentials c) {
    return auth.login(c);
  }
}
