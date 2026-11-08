package com.spark.api.platform;

public class BusinessException extends RuntimeException {
  public final int status;
  public final String code;

  public BusinessException(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static BusinessException invalid(String message) {
    return new BusinessException(422, "VALIDATION_ERROR", message);
  }

  public static BusinessException missing() {
    return new BusinessException(404, "NOT_FOUND", "Resource not found.");
  }

  public static BusinessException unauthorized() {
    return new BusinessException(401, "UNAUTHORIZED", "Authentication required.");
  }
}
