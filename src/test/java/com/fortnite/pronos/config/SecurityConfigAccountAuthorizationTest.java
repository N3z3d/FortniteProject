package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.controller.AccountController;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserDeletionService;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@WebMvcTest(controllers = AccountController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — AccountController authorization")
class SecurityConfigAccountAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDeletionService userDeletionService;

  @org.springframework.boot.test.mock.mockito.MockBean private UserResolver userResolver;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  @Test
  @DisplayName("Unauthenticated user receives 401 on DELETE /api/account")
  void unauthenticatedUserCannotDeleteAccount() throws Exception {
    // userResolver mock returns null by default (unauthenticated)
    int statusCode = mockMvc.perform(delete("/api/account")).andReturn().getResponse().getStatus();
    assertThat(statusCode).isIn(401, 403);
  }

  @Test
  @DisplayName("Authenticated user receives 204 on DELETE /api/account")
  @WithMockUser(username = "player1")
  void authenticatedUserCanDeleteAccount() throws Exception {
    User mockUser = new User();
    mockUser.setId(UUID.randomUUID());
    mockUser.setUsername("player1");

    when(userResolver.resolve(nullable(String.class), any(HttpServletRequest.class)))
        .thenReturn(mockUser);

    mockMvc.perform(delete("/api/account")).andExpect(status().isNoContent());
  }
}
