package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.service.JwtService;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = TestActuatorController.class)
@Import({SecurityConfig.class, SecurityConfigActuatorAuthorizationTest.SecurityTestBeans.class})
@ActiveProfiles("security-it")
class SecurityConfigActuatorAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  @Test
  @DisplayName("Anonymous user can access actuator health")
  void anonymousCanAccessActuatorHealth() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("Anonymous user cannot access non-health actuator endpoints")
  void anonymousCannotAccessNonHealthActuatorEndpoints() throws Exception {
    int statusCode = mockMvc.perform(get("/actuator/info")).andReturn().getResponse().getStatus();
    assertUnauthorizedOrForbidden(statusCode);
  }

  @Test
  @DisplayName("Authenticated non-admin user cannot access non-health actuator endpoints")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminCannotAccessNonHealthActuatorEndpoints() throws Exception {
    mockMvc.perform(get("/actuator/info")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin user can access non-health actuator endpoints")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessNonHealthActuatorEndpoints() throws Exception {
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
  }

  private void assertUnauthorizedOrForbidden(int statusCode) {
    assertThat(statusCode).isIn(401, 403);
  }

  @TestConfiguration
  public static class SecurityTestBeans {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
      JwtService jwtService = org.mockito.Mockito.mock(JwtService.class);
      return new JwtAuthenticationFilter(jwtService, userDetailsService) {
        @Override
        protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
          filterChain.doFilter(request, response);
        }
      };
    }
  }
}
