package com.spark.api.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spark.api.ai.*;
import com.spark.api.platform.BusinessException;
import com.spark.api.privacy.InputStorage;
import java.security.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;

@Service
public class GenerationService {
  private static final Logger LOG = LoggerFactory.getLogger(GenerationService.class);
  private final GenerationTransactions tx;
  private final AiClient ai;
  private final OutputValidator validator;
  private final InputStorage storage;
  private final ObjectMapper json;

  public GenerationService(
      GenerationTransactions tx,
      AiClient ai,
      OutputValidator validator,
      InputStorage storage,
      ObjectMapper json) {
    this.tx = tx;
    this.ai = ai;
    this.validator = validator;
    this.storage = storage;
    this.json = json;
  }

  public GenerationDtos.Result generate(UUID user, GenerationInput input) {
    var reservation = tx.reserve(user, input, fingerprint(input));
    if (reservation.replay()) return tx.get(user, reservation.id());
    List<String> paths = new ArrayList<>();
    boolean completed = false;
    boolean retainImages = false;
    long started = System.nanoTime();
    try {
      if (reservation.storeImages())
        for (int i = 0; i < input.images().size(); i++) {
          String path =
              user
                  + "/"
                  + reservation.id()
                  + "/"
                  + reservation.attempt()
                  + "-"
                  + i
                  + (input.images().get(i).mimeType().equals("image/png") ? ".png" : ".jpg");
          paths.add(path);
          storage.put(path, input.images().get(i).bytes(), input.images().get(i).mimeType());
        }
      var result =
          ai.generate(reservation.prompt(), input.images(), "REPLY".equals(input.type()) ? 4 : 5);
      var values = validator.validate(result, "REPLY".equals(input.type()) ? 4 : 5);
      var response = tx.complete(user, reservation, result, values, paths);
      completed = true;
      retainImages = response.retainImages();
      LOG.info(
          "generation_succeeded id={} durationMs={} tokensIn={} tokensOut={}",
          reservation.id(),
          (System.nanoTime() - started) / 1000000,
          result.tokensIn(),
          result.tokensOut());
      return response.response();
    } catch (RuntimeException e) {
      tx.fail(
          user, reservation, e instanceof BusinessException b && "CONTENT_BLOCKED".equals(b.code));
      throw e;
    } finally {
      if (!completed || !retainImages) for (String path : paths) storage.delete(path);
    }
  }

  private String fingerprint(GenerationInput in) {
    try {
      var imageHashes = in.images().stream().map(i -> hash(i.bytes())).toList();
      return hash(
          json.writeValueAsBytes(
              Arrays.asList(
                  in.type(), in.toneId(), in.note(), in.text(), in.contactId(), imageHashes)));
    } catch (Exception e) {
      throw new IllegalStateException("Cannot fingerprint request", e);
    }
  }

  private static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
