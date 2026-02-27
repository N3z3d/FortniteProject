package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

import com.fortnite.pronos.controller.UserController;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.JwtService;
import com.fortnite.pronos.service.UserService;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, SecurityConfigUserAuthorizationTest.SecurityTestBeans.class})
@ActiveProfiles("security-it")
class SecurityConfigUserAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean private UserService userService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  @Test
  @DisplayName("Anonymous user cannot access /api/users")
  void anonymousCannotAccessUsersEndpoint() throws Exception {
    int statusCode = mockMvc.perform(get("/api/users")).andReturn().getResponse().getStatus();
    assertUnauthorizedOrForbidden(statusCode);
  }

  @Test
  @DisplayName("Authenticated non-admin user cannot access /api/users")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminCannotAccessUsersEndpoint() throws Exception {
    mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin can access /api/users")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessUsersEndpoint() throws Exception {
    User user = buildUser();
    when(userService.getAllUsers()).thenReturn(List.of(user));

    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(user.getId().toString()))
        .andExpect(jsonPath("$[0].username").value(user.getUsername()));
  }

  @Test
  @DisplayName("Admin response for /api/users never exposes password fields")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminResponseNeverExposesPasswordFields() throws Exception {
    User user = buildUser();
    when(userService.getAllUsers()).thenReturn(List.of(user));

    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].password").doesNotExist())
        .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
  }

  @Test
  @DisplayName("Authenticated non-admin user cannot access /api/users/{id}")
  @WithMockUser(
      username = "user",
      roles = {"USER"})
  void nonAdminCannotAccessUserByIdEndpoint() throws Exception {
    mockMvc.perform(get("/api/users/{id}", UUID.randomUUID())).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin can access /api/users/{id} and payload has no secrets")
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminCanAccessUserByIdWithoutSecrets() throws Exception {
    User user = buildUser();
    when(userService.findUserById(user.getId())).thenReturn(Optional.of(user));

    mockMvc
        .perform(get("/api/users/{id}", user.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.getId().toString()))
        .andExpect(jsonPath("$.username").value(user.getUsername()))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  private User buildUser() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("secure-user");
    user.setEmail("secure-user@fortnite.com");
    user.setPassword("$2a$10$secret.hash.for.tests");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2026);
    return user;
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
