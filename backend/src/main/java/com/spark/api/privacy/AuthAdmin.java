package com.spark.api.privacy;

import java.util.UUID;

public interface AuthAdmin {
  void revoke(UUID user, String accessToken);

  void deleteUser(UUID user);
}
