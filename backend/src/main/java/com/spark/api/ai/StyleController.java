package com.spark.api.ai;

import com.spark.api.identity.CurrentUser;
import com.spark.api.platform.RateLimitService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/style-profile")
public class StyleController {
  private final StyleService styles;
  private final CurrentUser users;
  private final RateLimitService limits;

  public StyleController(StyleService styles, CurrentUser users, RateLimitService limits) {
    this.styles = styles;
    this.users = users;
    this.limits = limits;
  }

  @GetMapping
  public StyleService.View get(Authentication a) {
    return styles.get(users.id(a));
  }

  @PutMapping
  public StyleService.View save(Authentication a, @Valid @RequestBody StyleService.Input b) {
    return styles.save(users.id(a), b);
  }

  @PostMapping("/analyze")
  public StyleService.View analyze(Authentication a) {
    var user = users.id(a);
    limits.check(user);
    return styles.analyze(user);
  }

  @DeleteMapping
  public void delete(Authentication a) {
    styles.delete(users.id(a));
  }
}
