package com.spark.api.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.core.env.Environment environment;

  private final ObjectMapper mapper;

  public SecurityConfiguration(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Bean
  @Profile("!supabase & !local")
  SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
    configure(http);
    // Until a real identity provider is configured, reject every protected request.
    http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
    return http.build();
  }

  @Bean
  @Profile("supabase | local")
  SecurityFilterChain supabaseSecurity(HttpSecurity http, JwtDecoder decoder) throws Exception {
    configure(http);
    http.authorizeHttpRequests(
        auth -> {
          if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("local")))
            auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .permitAll();
          auth.requestMatchers("/local/auth/**", "/v1/billing/webhook")
              .permitAll()
              .anyRequest()
              .authenticated();
        });
    http.oauth2ResourceServer(
        oauth ->
            oauth
                .bearerTokenResolver(
                    request ->
                        request.getRequestURI().equals("/v1/billing/webhook")
                            ? null
                            : new org.springframework.security.oauth2.server.resource.web
                                    .DefaultBearerTokenResolver()
                                .resolve(request))
                .jwt(jwt -> jwt.decoder(decoder))
                .authenticationEntryPoint(
                    (request, response, error) ->
                        writeError(
                            request, response, 401, "UNAUTHORIZED", "Authentication required."))
                .accessDeniedHandler(
                    (request, response, error) ->
                        writeError(request, response, 403, "FORBIDDEN", "Access denied.")));
    return http.build();
  }

  @Bean
  @Profile("supabase")
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
      name = "spark.auth.legacy-hs256-enabled",
      havingValue = "false",
      matchIfMissing = true)
  JwtDecoder supabaseDecoder(
      @Value("${spark.auth.jwks-url}") String jwksUrl,
      @Value("${spark.auth.issuer}") String issuer,
      @Value("${spark.auth.audience}") String audience) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSetUri(jwksUrl)
            .jwsAlgorithms(
                algorithms -> {
                  algorithms.add(SignatureAlgorithm.ES256);
                  algorithms.add(SignatureAlgorithm.RS256);
                })
            .build();
    OAuth2TokenValidator<Jwt> audienceValidator =
        jwt ->
            jwt.getExpiresAt() != null
                    && jwt.getSubject() != null
                    && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
    return decoder;
  }

  @Bean
  @Profile("supabase")
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
      name = "spark.auth.legacy-hs256-enabled",
      havingValue = "true")
  JwtDecoder legacyDecoder(
      @Value("${SUPABASE_JWT_SECRET}") String secret,
      @Value("${spark.auth.issuer}") String issuer,
      @Value("${spark.auth.audience}") String audience) {
    byte[] key = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    if (key.length < 32)
      throw new IllegalArgumentException("Legacy JWT secret must be at least 32 bytes");
    var decoder =
        NimbusJwtDecoder.withSecretKey(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"))
            .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
            .build();
    OAuth2TokenValidator<Jwt> required =
        jwt ->
            jwt.getExpiresAt() != null
                    && jwt.getSubject() != null
                    && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), required));
    return decoder;
  }

  private void configure(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .requestCache(cache -> cache.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, error) ->
                            writeError(
                                request, response, 401, "UNAUTHORIZED", "Authentication required."))
                    .accessDeniedHandler(
                        (request, response, error) ->
                            writeError(request, response, 403, "FORBIDDEN", "Access denied.")));
    // No CORS configuration: cross-origin browser access is not enabled.
  }

  private void writeError(
      HttpServletRequest request,
      HttpServletResponse response,
      int status,
      String code,
      String message)
      throws IOException {
    response.setStatus(status);
    if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(
        response.getOutputStream(),
        new ApiError(code, message, RequestIdFilter.requestId(request)));
  }
}
