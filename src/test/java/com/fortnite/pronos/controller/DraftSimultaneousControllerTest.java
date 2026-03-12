package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.ConflictResolutionResponse;
import com.fortnite.pronos.dto.SimultaneousStatusResponse;
import com.fortnite.pronos.dto.SimultaneousSubmitRequest;
import com.fortnite.pronos.exception.InvalidTrancheViolationException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;
import com.fortnite.pronos.service.draft.DraftSimultaneousService;
import com.fortnite.pronos.service.draft.DraftTrancheService;

@ExtendWith(MockitoExtension.class)
class DraftSimultaneousControllerTest {

  @Mock private DraftSimultaneousService simultaneousService;
  @Mock private DraftTrancheService draftTrancheService;

  private DraftSimultaneousController controller;

  private final UUID draftId = UUID.randomUUID();
  private final UUID windowId = UUID.randomUUID();
  private final UUID participantId = UUID.randomUUID();
  private final UUID playerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    controller = new DraftSimultaneousController(simultaneousService, draftTrancheService);
  }

  @Nested
  @DisplayName("Submit")
  class Submit {

    @Test
    void whenPickValid_submitsAndReturns200() {
      SimultaneousSubmitRequest request =
          new SimultaneousSubmitRequest(windowId, participantId, playerId);

      var response = controller.submit(draftId, request);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      verify(draftTrancheService).validatePickByDraftId(draftId, "GLOBAL", playerId);
      verify(simultaneousService).submit(windowId, participantId, playerId);
    }

    @Test
    void whenPlayerAlreadyPickedInDraft_validatePickThrows() {
      SimultaneousSubmitRequest request =
          new SimultaneousSubmitRequest(windowId, participantId, playerId);

      doThrow(new PlayerAlreadySelectedException("Player is already selected in this draft"))
          .when(draftTrancheService)
          .validatePickByDraftId(draftId, "GLOBAL", playerId);

      org.junit.jupiter.api.Assertions.assertThrows(
          PlayerAlreadySelectedException.class, () -> controller.submit(draftId, request));

      verifyNoInteractions(simultaneousService);
    }

    @Test
    void whenTrancheViolation_validatePickCalledBeforeSubmit() {
      SimultaneousSubmitRequest request =
          new SimultaneousSubmitRequest(windowId, participantId, playerId);

      doThrow(new InvalidTrancheViolationException("Tranche violation"))
          .when(draftTrancheService)
          .validatePickByDraftId(draftId, "GLOBAL", playerId);

      org.junit.jupiter.api.Assertions.assertThrows(
          InvalidTrancheViolationException.class, () -> controller.submit(draftId, request));

      verifyNoInteractions(simultaneousService);
    }
  }

  @Nested
  @DisplayName("Get Status")
  class GetStatus {

    @Test
    void whenStatusFound_returns200() {
      SimultaneousStatusResponse status = new SimultaneousStatusResponse(draftId, windowId, 2, 4);
      when(simultaneousService.getStatus(draftId)).thenReturn(Optional.of(status));

      var response = controller.getStatus(draftId);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void whenNotFound_returns404() {
      when(simultaneousService.getStatus(draftId)).thenReturn(Optional.empty());

      var response = controller.getStatus(draftId);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("Resolve Conflict")
  class ResolveConflict {

    @Test
    void whenValid_returns200WithResolution() {
      ConflictResolutionResponse resolution =
          new ConflictResolutionResponse(
              windowId, playerId, participantId, UUID.randomUUID(), false);
      when(simultaneousService.resolveConflict(windowId)).thenReturn(resolution);

      var response = controller.resolveConflict(draftId, windowId);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEqualTo(resolution);
      verify(simultaneousService).resolveConflict(windowId);
    }

    @Test
    void whenWindowNotResolving_serviceThrowsIllegalState() {
      doThrow(new IllegalStateException("Window is not in RESOLVING state: " + windowId))
          .when(simultaneousService)
          .resolveConflict(windowId);

      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalStateException.class, () -> controller.resolveConflict(draftId, windowId));
    }
  }
}
