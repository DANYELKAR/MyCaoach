package com.spark.api.contacts;

import com.spark.api.identity.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/contacts")
@org.springframework.validation.annotation.Validated
public class ContactController {
  private final ContactService contacts;
  private final CurrentUser users;

  public ContactController(ContactService contacts, CurrentUser users) {
    this.contacts = contacts;
    this.users = users;
  }

  @GetMapping
  public List<ContactService.View> list(
      Authentication a,
      @RequestParam(required = false) @Pattern(regexp = "NEW|TALKING|DATE_PLANNED|ENDED")
          String status,
      @RequestParam(required = false) @Size(max = 100) String q,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
    return contacts.list(users.id(a), status, q, page, size);
  }

  @GetMapping("/{id}")
  public ContactService.View get(Authentication a, @PathVariable UUID id) {
    return contacts.get(users.id(a), id);
  }

  @PostMapping
  public ContactService.View create(Authentication a, @Valid @RequestBody ContactService.Input b) {
    return contacts.save(users.id(a), null, b);
  }

  @PatchMapping("/{id}")
  public ContactService.View patch(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody ContactService.Input b) {
    return contacts.save(users.id(a), id, b);
  }

  @DeleteMapping("/{id}")
  public void delete(Authentication a, @PathVariable UUID id) {
    contacts.delete(users.id(a), id);
  }
}
