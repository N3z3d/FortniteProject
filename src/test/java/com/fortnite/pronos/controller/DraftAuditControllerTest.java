package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.DraftAuditEntryResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.DraftAuditService;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftAuditController")
class DraftAuditControllerTest {

  @Mock private DraftAuditService auditService;
  @Mock private UserResolver userResolver;
  @Mock private HttpServletRequest httpRequest;

  private DraftAuditController controller;

  private final UUID gameId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    controller = new DraftAuditController(auditService, userResolver);
  }

  private User stubUser() {
    User user = new User();
    user.setId(userId);
    user.setUsername("auditor");
    return user;
  }

  private DraftAuditEntryResponse buildEntry(String type) {
    return new DraftAuditEntryResponse(
        UUID.randomUUID(),
        type,
        LocalDateTime.now(),
        UUID.randomUUID(),
        null,
        null,
        UUID.randomUUID(),
        UUID.randomUUID());
  }

  @Nested
  @DisplayName("Get Audit")
  class GetAudit {

    @Test
    void whenAuthenticated_returns200WithAuditList() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(stubUser());
      List<DraftAuditEntryResponse> expected =
          List.of(buildEntry("SWAP_SOLO"), buildEntry("TRADE_PROPOSED"));
      when(auditService.getAuditForGame(gameId)).thenReturn(expected);

      var response = controller.getAudit(gameId, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void whenNotAuthenticated_returns401() {
      when(userResolver.resolve(null, httpRequest)).thenReturn(null);

      var response = controller.getAudit(gameId, null, httpRequest);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      verifyNoInteractions(auditService);
    }
  }
}
