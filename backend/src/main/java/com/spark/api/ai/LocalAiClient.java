package com.spark.api.ai;

import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalAiClient implements AiClient {
  public Result generate(String prompt, List<Image> images, int count) {
    var options =
        List.of(
            "That looks like a story worth hearing. What was the highlight?",
            "I have a feeling you have a great recommendation. What is your latest favorite?",
            "You have my curiosity. What is something you could talk about for hours?",
            "A small adventure sounds like a good start. What would you choose?",
            "I would love to hear more. What made you smile today?");
    return new Result(options.subList(0, count), false, "local-deterministic-fixture", 0, 0);
  }

  public Map<String, Object> analyzeStyle(List<String> samples) {
    return Map.of(
        "traits",
        List.of("friendly", "curious"),
        "formality",
        "casual",
        "length",
        "short",
        "emojiUsage",
        "light",
        "sampleLine",
        "What made you smile today?",
        "source",
        "local-fixture");
  }
}
