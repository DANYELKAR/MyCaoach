package com.spark.api.billing;

import com.spark.api.identity.CurrentUser;
import com.spark.api.profile.ProfileDtos;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("local")
@RequestMapping("/local/billing")
public class LocalBillingController {
  private final LocalBillingProvider provider;
  private final BillingService billing;
  private final CurrentUser users;

  public LocalBillingController(
      LocalBillingProvider provider, BillingService billing, CurrentUser users) {
    this.provider = provider;
    this.billing = billing;
    this.users = users;
  }

  @PutMapping("/entitlement")
  public ProfileDtos.View entitlement(
      Authentication a, @Valid @RequestBody BillingProvider.Entitlement value) {
    var user = users.id(a);
    provider.set(user, value);
    return billing.sync(user);
  }
}
