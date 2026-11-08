package com.spark.api.ai;

import com.spark.api.platform.BusinessException;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class OutputValidator {
  public List<String> validate(AiClient.Result result, int count) {
    if (result.blocked())
      throw new BusinessException(
          422, "CONTENT_BLOCKED", "This request cannot be processed safely.");
    if (result.suggestions() == null || result.suggestions().size() != count) throw unavailable();
    var seen = new HashSet<String>();
    var values = new ArrayList<String>();
    for (String text : result.suggestions()) {
      if (text == null
          || text.isBlank()
          || text.codePointCount(0, text.length()) > 220
          || !seen.add(text.strip().toLowerCase(Locale.ROOT))) throw unavailable();
      values.add(text.strip());
    }
    return List.copyOf(values);
  }

  public static BusinessException unavailable() {
    return new BusinessException(
        503, "AI_UNAVAILABLE", "AI is temporarily unavailable. Please try again.");
  }
}
