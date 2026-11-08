package com.spark.api.contacts;

import com.spark.api.domain.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.profile.ProfileService;
import com.spark.api.usage.PlanService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {
  public record Input(
      @Size(max = 100) String name,
      @Pattern(regexp = "HINGE|TINDER|BUMBLE|INSTAGRAM|IN_PERSON|OTHER") String source,
      @Pattern(regexp = "NEW|TALKING|DATE_PLANNED|ENDED") String status,
      @Size(max = 20) List<@NotBlank @Size(max = 80) String> interests,
      @Size(max = 3000) String notes) {}

  public record View(
      UUID id,
      String name,
      String source,
      String status,
      String[] interests,
      String notes,
      java.time.Instant createdAt) {}

  private final ContactRepository contacts;
  private final ProfileService profiles;
  private final PlanService plans;

  public ContactService(ContactRepository contacts, ProfileService profiles, PlanService plans) {
    this.contacts = contacts;
    this.profiles = profiles;
    this.plans = plans;
  }

  public static View view(Contact c) {
    return new View(c.id, c.name, c.source, c.status, c.interests, c.notes, c.createdAt);
  }

  public View get(UUID user, UUID id) {
    return view(contacts.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing));
  }

  public List<View> list(UUID user, String status, String q, int page, int size) {
    return contacts.search(user, status, q == null ? "" : q, PageRequest.of(page, size)).stream()
        .map(ContactService::view)
        .toList();
  }

  @Transactional
  public View save(UUID user, UUID id, Input b) {
    Profile p = profiles.lock(user);
    if (id == null) plans.requireCapacity(p, contacts.countByUserId(user), 3);
    Contact c =
        id == null
            ? new Contact()
            : contacts.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing);
    c.userId = user;
    if (b.name() != null) c.name = b.name().strip();
    if (c.name == null || c.name.isBlank()) throw BusinessException.invalid("Name is required.");
    if (b.source() != null) c.source = b.source();
    if (b.status() != null) c.status = b.status();
    if (b.interests() != null) c.interests = b.interests().toArray(String[]::new);
    if (b.notes() != null) c.notes = b.notes();
    return view(contacts.saveAndFlush(c));
  }

  @Transactional
  public void delete(UUID user, UUID id) {
    profiles.lock(user);
    contacts.delete(contacts.findByIdAndUserId(id, user).orElseThrow(BusinessException::missing));
  }
}
