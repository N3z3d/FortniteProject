package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.controller.DraftAuditController;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;
import com.fortnite.pronos.service.draft.DraftAuditService;

@WebMvcTest(controllers = DraftAuditController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SecurityConfig — DraftAuditController authorization")
class SecurityConfigDraftAuditAuthorizationTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  @org.springframework.boot.test.mock.mockito.MockBean private DraftAuditService draftAuditService;

  @org.springframework.boot.test.mock.mockito.MockBean private UserResolver userResolver;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  private static final UUID GAME_ID = UUID.randomUUID();

  @Test
  @DisplayName("Unauthenticated user receives 401/403 on GET draft audit")
  void unauthenticatedCannotGetAudit() throws Exception {
    int status =
        mockMvc
            .perform(get("/api/games/{gameId}/draft/audit", GAME_ID))
            .andReturn()
            .getResponse()
            .getStatus();
    assertThat(status).isIn(401, 403);
  }
}
