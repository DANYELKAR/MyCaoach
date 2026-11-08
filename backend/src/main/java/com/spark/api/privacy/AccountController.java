package com.spark.api.privacy;

import com.spark.api.platform.BusinessException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {
  private final AccountDeletionService accounts;

  public AccountController(AccountDeletionService accounts) {
    this.accounts = accounts;
  }

  @DeleteMapping("/v1/me")
  public void delete(Authentication a) {
    UUID user;
    try {
      user = UUID.fromString(a.getName());
    } catch (Exception e) {
      throw BusinessException.unauthorized();
    }
    accounts.delete(
        user, a instanceof JwtAuthenticationToken jwt ? jwt.getToken().getTokenValue() : null);
  }
}
