package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.dto.admin.AdminRosterAssignRequest;
import com.fortnite.pronos.service.admin.AdminAuditLogService;
import com.fortnite.pronos.service.admin.AdminDraftRosterService;

@ExtendWith(MockitoExtension.class)
class AdminDraftRosterControllerTest {

  @Mock private AdminDraftRosterService rosterService;
  @Mock private AdminAuditLogService auditLogService;

  private AdminDraftRosterController controller;

  private final UUID gameId = UUID.randomUUID();
  private final UUID participantUserId = UUID.randomUUID();
  private final UUID playerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    controller = new AdminDraftRosterController(rosterService, auditLogService);
  }

  @Nested
  @DisplayName("Assign Player")
  class AssignPlayer {

    @Test
    void whenValid_returns200WithDto() {
      DraftPickDto dto = DraftPickDto.builder().id(UUID.randomUUID()).playerId(playerId).build();
      when(rosterService.assignPlayer(gameId, participantUserId, playerId)).thenReturn(dto);

      AdminRosterAssignRequest request = new AdminRosterAssignRequest(participantUserId, playerId);
      var response = controller.assignPlayer(gameId, request, null);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEqualTo(dto);
      verify(rosterService).assignPlayer(gameId, participantUserId, playerId);
    }
  }

  @Nested
  @DisplayName("Remove Player")
  class RemovePlayer {

    @Test
    void whenValid_returns204() {
      var response = controller.removePlayer(gameId, playerId, null);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
      verify(rosterService).removePlayer(gameId, playerId);
    }
  }
}
