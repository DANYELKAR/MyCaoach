package com.spark.api.privacy;

import java.time.Instant;
import java.util.*;

public interface InputStorage {
  record Access(String signedUrl, byte[] bytes, String mimeType) {}

  Access access(String path);

  void put(String path, byte[] bytes, String mimeType);

  void delete(String path);

  void deleteUserObjects(UUID user);

  void cleanOrphans(Instant olderThan, Set<String> retained);
}
