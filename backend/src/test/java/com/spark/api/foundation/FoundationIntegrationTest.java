package com.spark.api.foundation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc(
    print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@ActiveProfiles("local")
@Testcontainers
class FoundationIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", DB::getJdbcUrl);
    r.add("spring.datasource.username", DB::getUsername);
    r.add("spring.datasource.password", DB::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;

  @Test
  void migratesRegistersAuthenticatesAndPersistsProfile() throws Exception {
    var credentials = "{\"email\":\"foundation@example.test\",\"password\":\"test-password-123\"}";
    var response =
        mvc.perform(
                post("/local/auth/register").contentType("application/json").content(credentials))
            .andExpect(status().isOk())
            .andReturn();
    var token =
        json.readTree(response.getResponse().getContentAsString()).get("accessToken").asText();
    mvc.perform(get("/v1/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plan").value("FREE"));
    mvc.perform(
            patch("/v1/me")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(
                    "{\"ageConfirmed\":true,\"aiConsent\":true,\"timezone\":\"Europe/Istanbul\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiConsentAt").isNotEmpty());
    mvc.perform(get("/v1/me/usage").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remainingToday").value(3));
    mvc.perform(
            get("/v1/me")
                .header(
                    "Authorization",
                    "Bearer " + token.substring(0, token.length() - 8) + "invalid!"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/local/auth/login").contentType("application/json").content(credentials))
        .andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject("select count(*) from tones where user_id is null", Integer.class))
        .isEqualTo(7);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from pg_class where relname in ('profiles','tones','contacts','threads','generations','suggestions','folders','saved_items','style_profiles','reports') and relrowsecurity",
                Integer.class))
        .isEqualTo(10);
  }

  @Autowired org.springframework.context.ApplicationContext context;

  @Test
  void mvcDoesNotExposeSseFragmentsOrXsltViews() {
    assertThat(
            context.getBeansOfType(
                org.springframework.web.servlet.view.xslt.XsltViewResolver.class))
        .isEmpty();
    var mapping =
        context.getBean(
            "requestMappingHandlerMapping",
            org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
                .class);
    mapping
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              if (handler.getBeanType().getPackageName().startsWith("com.spark.api")) {
                assertThat(
                        org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation(
                            handler.getBeanType(),
                            org.springframework.web.bind.annotation.RestController.class))
                    .isTrue();
                assertThat(info.getPatternValues()).doesNotContain("/**");
                assertThat(info.getProducesCondition().getProducibleMediaTypes())
                    .doesNotContain(org.springframework.http.MediaType.TEXT_EVENT_STREAM);
                assertThat(handler.getMethod().getReturnType().getSimpleName())
                    .isNotIn("SseEmitter", "FragmentsRendering", "ModelAndView");
              }
            });
  }
}
