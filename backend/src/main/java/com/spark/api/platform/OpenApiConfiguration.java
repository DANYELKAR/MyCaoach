package com.spark.api.platform;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfiguration {
  @Bean
  OpenAPI api() {
    return new OpenAPI()
        .info(
            new Info()
                .title("MyCoach API")
                .version("v1")
                .description(
                    "Local development and Supabase-backed MyCoach backend. Local AI and billing are deterministic development fixtures."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSchemas(
                    "ApiError",
                    new ObjectSchema()
                        .addProperty("code", new StringSchema())
                        .addProperty("message", new StringSchema())
                        .addProperty("requestId", new StringSchema().format("uuid"))))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  @Bean
  OperationCustomizer errors() {
    return (operation, handler) -> {
      if (handler.getBeanType().getSimpleName().equals("LocalAuthController")
          || handler.getMethod().getName().equals("webhook"))
        operation.setSecurity(java.util.List.of());
      for (String status :
          java.util.List.of("401", "402", "403", "404", "409", "410", "422", "429", "503"))
        operation
            .getResponses()
            .addApiResponse(
                status,
                new ApiResponse()
                    .description(
                        "Standard MyCoach error: UNAUTHORIZED, OUT_OF_CREDITS, FORBIDDEN_PLAN, NOT_FOUND, IDEMPOTENCY_CONFLICT / GENERATION_PENDING / TIMEZONE_LOCKED / STYLE_CHANGED, HISTORY_DELETED, VALIDATION_ERROR / CONSENT_REQUIRED / CONTENT_BLOCKED, RATE_LIMITED, AI_UNAVAILABLE / BILLING_UNAVAILABLE / STORAGE_UNAVAILABLE.")
                    .content(
                        new Content()
                            .addMediaType(
                                "application/json",
                                new MediaType()
                                    .schema(
                                        new Schema<>().$ref("#/components/schemas/ApiError")))));
      return operation;
    };
  }
}
