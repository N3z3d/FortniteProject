package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fortnite.pronos.domain.draft.model.DraftAsyncSelection;
import com.fortnite.pronos.domain.draft.model.DraftAsyncWindow;
import com.fortnite.pronos.domain.draft.model.DraftAsyncWindowStatus;
import com.fortnite.pronos.domain.port.out.DraftAsyncRepositoryPort;
import com.fortnite.pronos.dto.ConflictResolutionResponse;
import com.fortnite.pronos.dto.SimultaneousStatusResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftSimultaneousService")
class DraftSimultaneousServiceTest {

  @Mock private DraftAsyncRepositoryPort asyncRepository;
  @Mock private SimpMessagingTemplate messagingTemplate;
  @Mock private Random random;

  private DraftSimultaneousService service;

  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID WINDOW_ID = UUID.randomUUID();
  private static final UUID P1 = UUID.randomUUID();
  private static final UUID P2 = UUID.randomUUID();
  private static final UUID PLAYER_A = UUID.randomUUID();
  private static final UUID PLAYER_B = UUID.randomUUID();

  private DraftAsyncWindow openWindow;

  @BeforeEach
  void setUp() {
    service = new DraftSimultaneousService(asyncRepository, messagingTemplate, random);
    openWindow =
        DraftAsyncWindow.restore(
            WINDOW_ID,
            DRAFT_ID,
            "R1P1",
            Instant.now().plusSeconds(60),
            DraftAsyncWindowStatus.OPEN,
            2);
  }

  // ===== openWindow =====

  @Nested
  @DisplayName("openWindow")
  class OpenWindow {

    @Test
    @DisplayName("persists a new OPEN window and returns it")
    void shouldPersistNewWindow() {
      when(asyncRepository.saveWindow(any())).thenAnswer(inv -> inv.getArgument(0));

      DraftAsyncWindow result =
          service.openWindow(DRAFT_ID, "R1P1", Instant.now().plusSeconds(60), 2);

      verify(asyncRepository).saveWindow(any());
      assertThat(result.getDraftId()).isEqualTo(DRAFT_ID);
      assertThat(result.getStatus()).isEqualTo(DraftAsyncWindowStatus.OPEN);
      assertThat(result.getTotalExpected()).isEqualTo(2);
    }
  }

  // ===== submit =====

  @Nested
  @DisplayName("submit")
  class Submit {

    @Test
    @DisplayName("saves selection and broadcasts count update")
    void shouldSaveSelectionAndBroadcastCount() {
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.of(openWindow));
      when(asyncRepository.existsSelectionByWindowAndParticipant(WINDOW_ID, P1)).thenReturn(false);
      when(asyncRepository.saveSelection(any())).thenAnswer(inv -> inv.getArgument(0));
      when(asyncRepository.countSelectionsByWindowId(WINDOW_ID)).thenReturn(1);

      service.submit(WINDOW_ID, P1, PLAYER_A);

      verify(asyncRepository).saveSelection(any());
      verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("throws when window not found")
    void shouldThrowWhenWindowNotFound() {
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.submit(WINDOW_ID, P1, PLAYER_A))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Window not found");
    }

    @Test
    @DisplayName("throws when participant already submitted")
    void shouldThrowWhenAlreadySubmitted() {
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.of(openWindow));
      when(asyncRepository.existsSelectionByWindowAndParticipant(WINDOW_ID, P1)).thenReturn(true);

      assertThatThrownBy(() -> service.submit(WINDOW_ID, P1, PLAYER_A))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("already submitted");
    }

    @Test
    @DisplayName("broadcasts ALL_RESOLVED when last submission has no conflict")
    void shouldBroadcastAllResolvedWhenNoConflict() {
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.of(openWindow));
      when(asyncRepository.existsSelectionByWindowAndParticipant(WINDOW_ID, P2)).thenReturn(false);
      when(asyncRepository.saveSelection(any())).thenAnswer(inv -> inv.getArgument(0));
      when(asyncRepository.countSelectionsByWindowId(WINDOW_ID)).thenReturn(2); // totalExpected = 2
      when(asyncRepository.findSelectionsByWindowId(WINDOW_ID))
          .thenReturn(
              List.of(
                  DraftAsyncSelection.restore(
                      UUID.randomUUID(), WINDOW_ID, P1, PLAYER_A, Instant.now()),
                  DraftAsyncSelection.restore(
                      UUID.randomUUID(), WINDOW_ID, P2, PLAYER_B, Instant.now())));
      when(asyncRepository.saveWindow(any())).thenAnswer(inv -> inv.getArgument(0));

      service.submit(WINDOW_ID, P2, PLAYER_B);

      // submit broadcasts count update (1) + ALL_RESOLVED (1) = 2 total calls
      verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(Object.class));
      // verify window was saved as RESOLVED
      ArgumentCaptor<DraftAsyncWindow> windowCaptor =
          ArgumentCaptor.forClass(DraftAsyncWindow.class);
      verify(asyncRepository).saveWindow(windowCaptor.capture());
      assertThat(windowCaptor.getValue().getStatus()).isEqualTo(DraftAsyncWindowStatus.RESOLVED);
    }

    @Test
    @DisplayName("marks window RESOLVING and broadcasts CONFLICT_RESOLVED on conflict")
    void shouldBroadcastConflictWhenSamePlayerChosen() {
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.of(openWindow));
      when(asyncRepository.existsSelectionByWindowAndParticipant(WINDOW_ID, P2)).thenReturn(false);
      when(asyncRepository.saveSelection(any())).thenAnswer(inv -> inv.getArgument(0));
      when(asyncRepository.countSelectionsByWindowId(WINDOW_ID)).thenReturn(2);
      // Both chose PLAYER_A → conflict
      when(asyncRepository.findSelectionsByWindowId(WINDOW_ID))
          .thenReturn(
              List.of(
                  DraftAsyncSelection.restore(
                      UUID.randomUUID(), WINDOW_ID, P1, PLAYER_A, Instant.now()),
                  DraftAsyncSelection.restore(
                      UUID.randomUUID(), WINDOW_ID, P2, PLAYER_A, Instant.now())));
      when(asyncRepository.saveWindow(any())).thenAnswer(inv -> inv.getArgument(0));
      when(random.nextInt(2)).thenReturn(0); // P1 wins

      service.submit(WINDOW_ID, P2, PLAYER_A);

      ArgumentCaptor<DraftAsyncWindow> windowCaptor =
          ArgumentCaptor.forClass(DraftAsyncWindow.class);
      verify(asyncRepository).saveWindow(windowCaptor.capture());
      assertThat(windowCaptor.getValue().getStatus()).isEqualTo(DraftAsyncWindowStatus.RESOLVING);
    }
  }

  // ===== getStatus =====

  @Nested
  @DisplayName("getStatus")
  class GetStatus {

    @Test
    @DisplayName("returns submitted/total for the first open window")
    void shouldReturnSubmissionCount() {
      when(asyncRepository.findOpenWindowsByDraftId(DRAFT_ID)).thenReturn(List.of(openWindow));
      when(asyncRepository.countSelectionsByWindowId(WINDOW_ID)).thenReturn(1);

      Optional<SimultaneousStatusResponse> result = service.getStatus(DRAFT_ID);

      assertThat(result).isPresent();
      assertThat(result.get().submitted()).isEqualTo(1);
      assertThat(result.get().total()).isEqualTo(2);
      assertThat(result.get().windowId()).isEqualTo(WINDOW_ID);
    }

    @Test
    @DisplayName("returns empty when no open window")
    void shouldReturnEmptyWhenNoOpenWindow() {
      when(asyncRepository.findOpenWindowsByDraftId(DRAFT_ID)).thenReturn(List.of());

      Optional<SimultaneousStatusResponse> result = service.getStatus(DRAFT_ID);

      assertThat(result).isEmpty();
    }
  }

  // ===== resolveConflict =====

  @Nested
  @DisplayName("resolveConflict")
  class ResolveConflict {

    @Test
    @DisplayName("throws when window not found")
    void shouldThrowWhenWindowNotFound() {
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.resolveConflict(WINDOW_ID))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Window not found");
    }

    @Test
    @DisplayName("throws when window is not RESOLVING")
    void shouldThrowWhenNotResolving() {
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.of(openWindow));

      assertThatThrownBy(() -> service.resolveConflict(WINDOW_ID))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not in RESOLVING state");
    }

    @Test
    @DisplayName("coin flip picks winner deterministically using injected Random")
    void shouldPickWinnerDeterministically() {
      DraftAsyncWindow resolvingWindow =
          DraftAsyncWindow.restore(
              WINDOW_ID,
              DRAFT_ID,
              "R1P1",
              Instant.now().plusSeconds(60),
              DraftAsyncWindowStatus.RESOLVING,
              2);
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.of(resolvingWindow));
      when(asyncRepository.findSelectionsByWindowId(WINDOW_ID))
          .thenReturn(
              List.of(
                  DraftAsyncSelection.restore(
                      UUID.randomUUID(), WINDOW_ID, P1, PLAYER_A, Instant.now()),
                  DraftAsyncSelection.restore(
                      UUID.randomUUID(), WINDOW_ID, P2, PLAYER_A, Instant.now())));
      when(random.nextInt(2)).thenReturn(1); // P2 wins (index 1)

      ConflictResolutionResponse result = service.resolveConflict(WINDOW_ID);

      assertThat(result.contestedPlayerId()).isEqualTo(PLAYER_A);
      assertThat(result.winnerParticipantId()).isEqualTo(P2);
      assertThat(result.loserParticipantId()).isEqualTo(P1);
      assertThat(result.hasMoreConflicts()).isFalse();
    }

    @Test
    @DisplayName("broadcasts CONFLICT_RESOLVED via WebSocket")
    void shouldBroadcastConflictResolution() {
      DraftAsyncWindow resolvingWindow =
          DraftAsyncWindow.restore(
              WINDOW_ID,
              DRAFT_ID,
              "R1P1",
              Instant.now().plusSeconds(60),
              DraftAsyncWindowStatus.RESOLVING,
              2);
      when(asyncRepository.findWindowById(WINDOW_ID)).thenReturn(Optional.of(resolvingWindow));
      when(asyncRepository.findSelectionsByWindowId(WINDOW_ID))
          .thenReturn(
              List.of(
                  DraftAsyncSelection.restore(
                      UUID.randomUUID(), WINDOW_ID, P1, PLAYER_A, Instant.now()),
                  DraftAsyncSelection.restore(
                      UUID.randomUUID(), WINDOW_ID, P2, PLAYER_A, Instant.now())));
      when(random.nextInt(2)).thenReturn(0);

      service.resolveConflict(WINDOW_ID);

      verify(messagingTemplate)
          .convertAndSend(
              eq(
                  DraftSimultaneousService.TOPIC_PREFIX
                      + DRAFT_ID
                      + DraftSimultaneousService.TOPIC_SUFFIX),
              any(Object.class));
    }
  }

  // ===== domain constraints =====

  @Nested
  @DisplayName("window state transitions")
  class WindowStateTransitions {

    @Test
    @DisplayName("OPEN window cannot be resolved directly")
    void openWindowCannotBeDoubleResolved() {
      DraftAsyncWindow resolved =
          DraftAsyncWindow.restore(
              WINDOW_ID,
              DRAFT_ID,
              "R1P1",
              Instant.now().plusSeconds(60),
              DraftAsyncWindowStatus.RESOLVED,
              2);

      assertThatThrownBy(resolved::resolve)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("already RESOLVED");
    }

    @Test
    @DisplayName("startResolving requires OPEN status")
    void startResolvingRequiresOpen() {
      DraftAsyncWindow resolving =
          DraftAsyncWindow.restore(
              WINDOW_ID,
              DRAFT_ID,
              "R1P1",
              Instant.now().plusSeconds(60),
              DraftAsyncWindowStatus.RESOLVING,
              2);

      assertThatThrownBy(resolving::startResolving)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("must be OPEN");
    }
  }
}
