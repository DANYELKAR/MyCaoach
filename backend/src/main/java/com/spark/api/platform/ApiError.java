package com.spark.api.platform;

public record ApiError(String code, String message, String requestId) {}
