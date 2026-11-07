package com.spark.api.generation;

import com.spark.api.ai.AiClient;
import com.spark.api.platform.BusinessException;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

public record GenerationInput(
    String type,
    UUID toneId,
    String note,
    String text,
    UUID contactId,
    String idempotencyKey,
    List<AiClient.Image> images) {
  public GenerationInput {
    if (toneId == null || idempotencyKey == null || !idempotencyKey.matches("[a-zA-Z0-9_-]{1,100}"))
      throw BusinessException.invalid("Tone and a valid idempotency key are required.");
    if (note != null && note.length() > 1000 || text != null && text.length() > 10000)
      throw BusinessException.invalid("Input is too long.");
    if ("OPENER".equals(type) && images.size() != 1)
      throw BusinessException.invalid("Exactly one image is required.");
    if ("REPLY".equals(type)
        && ((images.isEmpty() && (text == null || text.isBlank()))
            || (!images.isEmpty() && text != null && !text.isBlank())
            || images.size() > 3))
      throw BusinessException.invalid("Provide text or one to three screenshots.");
    images = List.copyOf(images);
  }

  public static AiClient.Image image(MultipartFile file) {
    if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw BusinessException.invalid("Image must be between 1 byte and 5 MB.");
    try {
      byte[] bytes = file.getBytes();
      try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
        var readers = ImageIO.getImageReaders(stream);
        if (!readers.hasNext()) throw BusinessException.invalid("Invalid image.");
        var reader = readers.next();
        try {
          reader.setInput(stream);
          String format = reader.getFormatName().toLowerCase(Locale.ROOT);
          if (!Set.of("jpeg", "jpg", "png").contains(format)
              || reader.getWidth(0) > 4096
              || reader.getHeight(0) > 4096)
            throw BusinessException.invalid("Use JPEG/PNG up to 4096 pixels per edge.");
          if (reader.read(0) == null) throw BusinessException.invalid("Invalid image.");
          return new AiClient.Image(format.equals("png") ? "image/png" : "image/jpeg", bytes);
        } finally {
          reader.dispose();
        }
      }
    } catch (IOException e) {
      throw BusinessException.invalid("Invalid image.");
    }
  }
}
