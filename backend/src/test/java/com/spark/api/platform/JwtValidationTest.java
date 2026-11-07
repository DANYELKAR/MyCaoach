package com.spark.api.platform;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;

class JwtValidationTest {
  @Test
  void jwksChecksSignatureIssuerAudienceAndExpiry() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var pair = generator.generateKeyPair();
    var key =
        new RSAKey.Builder((RSAPublicKey) pair.getPublic())
            .privateKey((RSAPrivateKey) pair.getPrivate())
            .keyID("test-key")
            .build();
    var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/jwks",
        x -> {
          byte[] bytes =
              new JWKSet(key.toPublicJWK())
                  .toString()
                  .getBytes(java.nio.charset.StandardCharsets.UTF_8);
          x.getResponseHeaders().add("Content-Type", "application/json");
          x.sendResponseHeaders(200, bytes.length);
          x.getResponseBody().write(bytes);
          x.close();
        });
    server.start();
    try {
      var decoder =
          new SecurityConfiguration(new ObjectMapper())
              .supabaseDecoder(
                  "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks",
                  "https://issuer.test/auth/v1",
                  "authenticated");
      assertThat(
              decoder
                  .decode(
                      token(
                          encoder,
                          "https://issuer.test/auth/v1",
                          "authenticated",
                          Instant.now().plusSeconds(300)))
                  .getSubject())
          .isNotBlank();
      assertThatThrownBy(
              () ->
                  decoder.decode(
                      token(
                          encoder,
                          "wrong-issuer",
                          "authenticated",
                          Instant.now().plusSeconds(300))))
          .isInstanceOf(JwtException.class);
      assertThatThrownBy(
              () ->
                  decoder.decode(
                      token(
                          encoder,
                          "https://issuer.test/auth/v1",
                          "wrong-audience",
                          Instant.now().plusSeconds(300))))
          .isInstanceOf(JwtException.class);
      assertThatThrownBy(
              () ->
                  decoder.decode(
                      token(
                          encoder,
                          "https://issuer.test/auth/v1",
                          "authenticated",
                          Instant.now().minusSeconds(120))))
          .isInstanceOf(JwtException.class);
      assertThatThrownBy(() -> decoder.decode("not.a.token")).isInstanceOf(JwtException.class);
    } finally {
      server.stop(0);
    }
  }

  String token(JwtEncoder encoder, String issuer, String audience, Instant expires) {
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwtClaimsSet.builder()
                    .issuer(issuer)
                    .subject(UUID.randomUUID().toString())
                    .audience(List.of(audience))
                    .issuedAt(Instant.now().minusSeconds(600))
                    .expiresAt(expires)
                    .build()))
        .getTokenValue();
  }

  @Test
  void explicitLegacyHs256ConfigurationValidatesTokensAndRequiresStrongSecret() {
    String secret = "local-fixture-secret-at-least-thirty-two-bytes";
    var config = new SecurityConfiguration(new ObjectMapper());
    var decoder = config.legacyDecoder(secret, "https://issuer.test/auth/v1", "authenticated");
    var encoder =
        new NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    var claims =
        JwtClaimsSet.builder()
            .issuer("https://issuer.test/auth/v1")
            .subject(UUID.randomUUID().toString())
            .audience(List.of("authenticated"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    var parameters =
        JwtEncoderParameters.from(
            JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),
            claims);
    var token = encoder.encode(parameters).getTokenValue();
    assertThat(decoder.decode(token).getSubject()).isEqualTo(claims.getSubject());
    String[] parts = token.split("\\.");
    var body = new ObjectMapper().createObjectNode().put("sub", UUID.randomUUID().toString());
    String tampered =
        parts[0]
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))
            + "."
            + parts[2];
    assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
    assertThatThrownBy(() -> config.legacyDecoder("short", "issuer", "audience"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
