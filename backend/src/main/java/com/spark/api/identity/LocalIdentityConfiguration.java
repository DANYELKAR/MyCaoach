package com.spark.api.identity;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

@Configuration
@Profile("local")
public class LocalIdentityConfiguration {
  @Bean
  RSAKey localKey() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var pair = generator.generateKeyPair();
    return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
        .privateKey((RSAPrivateKey) pair.getPrivate())
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  @Bean
  JwtEncoder localEncoder(RSAKey key) {
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }

  @Bean
  JwtDecoder localDecoder(RSAKey key) throws Exception {
    var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
    OAuth2TokenValidator<Jwt> audience =
        jwt ->
            jwt.getExpiresAt() != null
                    && jwt.getSubject() != null
                    && jwt.getAudience().contains("authenticated")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer("http://spark-local"), audience));
    return decoder;
  }
}
