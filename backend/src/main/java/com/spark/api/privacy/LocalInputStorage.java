package com.spark.api.privacy;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalInputStorage implements InputStorage {
  private final Path root;

  public LocalInputStorage(@Value("${LOCAL_STORAGE_PATH:./.local/inputs}") String root)
      throws java.io.IOException {
    this.root = Path.of(root).toAbsolutePath().normalize();
    Files.createDirectories(this.root);
  }

  private Path resolve(String path) {
    Path p = root.resolve(path).normalize();
    if (!p.startsWith(root) || p.equals(root))
      throw new IllegalArgumentException("Invalid storage path");
    return p;
  }

  public Access access(String path) {
    try {
      return new Access(
          null,
          Files.readAllBytes(resolve(path)),
          path.endsWith(".png") ? "image/png" : "image/jpeg");
    } catch (java.io.IOException e) {
      throw com.spark.api.platform.BusinessException.missing();
    }
  }

  public void put(String path, byte[] bytes, String mime) {
    try {
      Path p = resolve(path);
      Files.createDirectories(p.getParent());
      Files.write(p, bytes);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Storage unavailable", e);
    }
  }

  public void delete(String path) {
    try {
      Files.deleteIfExists(resolve(path));
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Storage unavailable", e);
    }
  }

  public void deleteUserObjects(UUID user) {
    Path dir = resolve(user.toString());
    if (!Files.exists(dir)) return;
    try (var paths = Files.walk(dir)) {
      for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Storage unavailable", e);
    }
  }

  public void cleanOrphans(Instant before, Set<String> retained) {
    try (var paths = Files.walk(root)) {
      for (Path p : paths.filter(Files::isRegularFile).toList())
        if (Files.getLastModifiedTime(p).toInstant().isBefore(before)
            && !retained.contains(root.relativize(p).toString())) Files.deleteIfExists(p);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Storage unavailable", e);
    }
  }
}
