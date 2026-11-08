package com.spark.api.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.spark.api.identity.CurrentUser;
import com.spark.api.profile.ProfileDtos;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/billing")
public class BillingController {
  private final BillingService billing;
  private final CurrentUser users;

  public BillingController(BillingService billing, CurrentUser users) {
    this.billing = billing;
    this.users = users;
  }

  @PostMapping("/sync")
  public ProfileDtos.View sync(Authentication a) {
    return billing.sync(users.id(a));
  }

  @PostMapping("/webhook")
  public void webhook(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody JsonNode body) {
    billing.webhook(auth, body);
  }
}
