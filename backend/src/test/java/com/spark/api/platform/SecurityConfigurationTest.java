package com.spark.api.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigurationTest.ProbeController.class)
@Import({SecurityConfiguration.class, RequestIdFilter.class})
class SecurityConfigurationTest {
  @Autowired MockMvc mvc;

  @Test
  void protectedApiRequiresAuthenticationWithCorrelatedError() throws Exception {
    mvc.perform(get("/v1/me").header("X-Request-ID", "untrusted-input"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.message").value("Authentication required."))
        .andExpect(
            result -> {
              String requestId = result.getResponse().getHeader("X-Request-ID");
              java.util.UUID.fromString(requestId);
              org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                  .contains(requestId)
                  .doesNotContain("untrusted-input");
            });
  }

  @Test
  void bearerTokenCannotBypassUnconfiguredIdentityProvider() throws Exception {
    mvc.perform(get("/v1/me").header("Authorization", "Bearer fake-token"))
        .andExpect(status().isUnauthorized());
  }

  @RestController
  static class ProbeController {
    @GetMapping("/v1/me")
    String protectedEndpoint() {
      return "must not be exposed";
    }
  }
}
