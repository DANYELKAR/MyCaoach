package com.spark.api.privacy;

import com.fasterxml.jackson.databind.*;
import com.spark.api.platform.BusinessException;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("supabase")
public class SupabaseAdminClient implements InputStorage, AuthAdmin {
  private final String base, key;
  private final ObjectMapper json;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public SupabaseAdminClient(
      ObjectMapper json,
      @Value("${SUPABASE_URL}") String base,
      @Value("${SUPABASE_SERVICE_KEY}") String key) {
    if (key.isBlank()) throw new IllegalArgumentException("Supabase server key required");
    this.json = json;
    this.base = base;
    this.key = key;
  }

  private HttpResponse<String> request(
      String method, String path, byte[] data, String mime, String token, boolean notFoundOk) {
    try {
      var builder =
          HttpRequest.newBuilder(URI.create(base + path))
              .timeout(Duration.ofSeconds(15))
              .header("apikey", key)
              .header("Authorization", "Bearer " + (token == null ? key : token));
      if (mime != null) builder.header("Content-Type", mime);
      var result =
          http.send(
              builder
                  .method(
                      method,
                      data == null
                          ? HttpRequest.BodyPublishers.noBody()
                          : HttpRequest.BodyPublishers.ofByteArray(data))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      if (result.statusCode() < 200 || result.statusCode() >= 300) {
        if (!(notFoundOk && result.statusCode() == 404)
            && !(path.startsWith("/auth/v1/logout")
                && (result.statusCode() == 401 || result.statusCode() == 403))) throw unavailable();
      }
      return result;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw unavailable();
    } catch (Exception e) {
      throw unavailable();
    }
  }

  private byte[] bytes(Object body) {
    try {
      return json.writeValueAsBytes(body);
    } catch (Exception e) {
      throw unavailable();
    }
  }

  private static String safe(String path) {
    if (!path.matches("[a-zA-Z0-9_./-]+") || path.contains("..") || path.startsWith("/"))
      throw new IllegalArgumentException("Invalid storage path");
    return path;
  }

  public Access access(String path) {
    var response =
        request(
            "POST",
            "/storage/v1/object/sign/inputs/" + safe(path),
            bytes(Map.of("expiresIn", 60)),
            "application/json",
            null,
            false);
    try {
      String signed = json.readTree(response.body()).path("signedURL").asText();
      if (!signed.startsWith("/object/sign/inputs/")) throw unavailable();
      return new Access(base + "/storage/v1" + signed, null, null);
    } catch (Exception e) {
      throw unavailable();
    }
  }

  public void put(String path, byte[] bytes, String mime) {
    request("POST", "/storage/v1/object/inputs/" + safe(path), bytes, mime, null, false);
  }

  public void delete(String path) {
    request(
        "DELETE",
        "/storage/v1/object/inputs",
        bytes(Map.of("prefixes", List.of(safe(path)))),
        "application/json",
        null,
        true);
  }

  public void deleteUserObjects(UUID user) {
    for (var object : list(user.toString())) delete(object.path());
  }

  record ObjectInfo(String path, Instant createdAt) {}

  private List<ObjectInfo> list(String prefix) {
    List<ObjectInfo> result = new ArrayList<>();
    int offset = 0;
    while (true) {
      var response =
          request(
              "POST",
              "/storage/v1/object/list/inputs",
              bytes(
                  Map.of(
                      "prefix",
                      prefix,
                      "limit",
                      100,
                      "offset",
                      offset,
                      "sortBy",
                      Map.of("column", "name", "order", "asc"))),
              "application/json",
              null,
              false);
      try {
        JsonNode rows = json.readTree(response.body());
        if (!rows.isArray()) throw unavailable();
        for (var row : rows) {
          String name = row.path("name").asText();
          String path = prefix.isBlank() ? name : prefix + "/" + name;
          if (row.path("id").isNull() || row.path("id").isMissingNode()) result.addAll(list(path));
          else result.add(new ObjectInfo(path, Instant.parse(row.path("created_at").asText())));
        }
        if (rows.size() < 100) break;
        offset += 100;
      } catch (Exception e) {
        throw unavailable();
      }
    }
    return result;
  }

  public void cleanOrphans(Instant olderThan, Set<String> retained) {
    for (var object : list(""))
      if (object.createdAt().isBefore(olderThan) && !retained.contains(object.path()))
        delete(object.path());
  }

  public void revoke(UUID user, String token) {
    // Ban refresh/sign-in before deleting storage; deletion removes all sessions afterwards.
    request(
        "PUT",
        "/auth/v1/admin/users/" + user,
        bytes(Map.of("ban_duration", "876000h")),
        "application/json",
        null,
        true);
    if (token != null) request("POST", "/auth/v1/logout?scope=global", null, null, token, true);
  }

  public void deleteUser(UUID user) {
    request("DELETE", "/auth/v1/admin/users/" + user, null, null, null, true);
  }

  private static BusinessException unavailable() {
    return new BusinessException(
        503, "STORAGE_UNAVAILABLE", "Account or storage service is temporarily unavailable.");
  }
}
