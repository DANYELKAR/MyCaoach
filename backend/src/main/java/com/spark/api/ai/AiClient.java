package com.spark.api.ai;

import java.util.*;

public interface AiClient {
  record Image(String mimeType, byte[] bytes) {}

  record Result(
      List<String> suggestions, boolean blocked, String model, int tokensIn, int tokensOut) {}

  Result generate(String prompt, List<Image> images, int count);

  Map<String, Object> analyzeStyle(List<String> samples);
}
