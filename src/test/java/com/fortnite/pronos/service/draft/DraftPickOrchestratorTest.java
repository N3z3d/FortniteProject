package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.draft.model.DraftRegionCursor;
import com.fortnite.pronos.domain.draft.model.SnakeTurn;
import com.fortnite.pronos.domain.port.out.DraftRegionCursorRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftPickOrchestrator")
class DraftPickOrchestratorTest {

  @Mock private DraftRegionCursorRepositoryPort cursorRepository;

  @InjectMocks private DraftPickOrchestratorService orchestrator;

  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final String REGION = "NAE";
  private static final UUID P1 = UUID.randomUUID();
  private static final UUID P2 = UUID.randomUUID();
  private static final UUID P3 = UUID.randomUUID();
  private static final List<UUID> ORDER = List.of(P1, P2, P3);

  private DraftRegionCursor cursorRound1Pick1;

  @BeforeEach
  void setUp() {
    cursorRound1Pick1 = new DraftRegionCursor(DRAFT_ID, REGION, ORDER);
  }

  // ===== getOrInitTurn =====

  @Nested
  @DisplayName("getOrInitTurn")
  class GetOrInitTurn {

    @Test
    @DisplayName("creates and persists cursor when none exists")
    void shouldCreateCursorWhenAbsent() {
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION)).thenReturn(Optional.empty());
      when(cursorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      orchestrator.getOrInitTurn(DRAFT_ID, REGION, ORDER);

      ArgumentCaptor<DraftRegionCursor> captor = ArgumentCaptor.forClass(DraftRegionCursor.class);
      verify(cursorRepository).save(captor.capture());
      assertThat(captor.getValue().getDraftId()).isEqualTo(DRAFT_ID);
      assertThat(captor.getValue().getRegion()).isEqualTo(REGION);
    }

    @Test
    @DisplayName("returns existing cursor without persisting a new one")
    void shouldReturnExistingCursorWithoutSaving() {
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(cursorRound1Pick1));

      orchestrator.getOrInitTurn(DRAFT_ID, REGION, ORDER);

      verify(cursorRepository, never()).save(any());
    }

    @Test
    @DisplayName("round 1 pick 1 → participant is P1 (first in forward order)")
    void shouldReturnFirstParticipantOnRound1Pick1() {
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(cursorRound1Pick1));

      SnakeTurn turn = orchestrator.getOrInitTurn(DRAFT_ID, REGION, ORDER);

      assertThat(turn.getParticipantId()).isEqualTo(P1);
      assertThat(turn.getRound()).isEqualTo(1);
      assertThat(turn.getPickNumber()).isEqualTo(1);
      assertThat(turn.isReversed()).isFalse();
    }
  }

  // ===== advance =====

  @Nested
  @DisplayName("advance")
  class Advance {

    @Test
    @DisplayName("returns empty when no cursor found")
    void shouldReturnEmptyWhenNoCursor() {
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION)).thenReturn(Optional.empty());

      Optional<SnakeTurn> result = orchestrator.advance(DRAFT_ID, REGION);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("advances cursor from pick 1 to pick 2 within same round")
    void shouldAdvanceToNextPick() {
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(cursorRound1Pick1));
      when(cursorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      Optional<SnakeTurn> result = orchestrator.advance(DRAFT_ID, REGION);

      assertThat(result).isPresent();
      assertThat(result.get().getPickNumber()).isEqualTo(2);
      assertThat(result.get().getRound()).isEqualTo(1);
    }

    @Test
    @DisplayName("advances to round 2 when last pick exhausted — order reverses")
    void shouldAdvanceToRound2WhenLastPickInRound1() {
      // Place cursor at last pick of round 1 (pick 3 of 3)
      DraftRegionCursor lastPickR1 = DraftRegionCursor.restore(DRAFT_ID, REGION, 1, 3, ORDER);
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(lastPickR1));
      when(cursorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      Optional<SnakeTurn> result = orchestrator.advance(DRAFT_ID, REGION);

      assertThat(result).isPresent();
      SnakeTurn turn = result.get();
      assertThat(turn.getRound()).isEqualTo(2);
      assertThat(turn.getPickNumber()).isEqualTo(1);
      assertThat(turn.isReversed()).isTrue();
    }

    @Test
    @DisplayName("round 2 pick 1 reversed → participant is last in snake order (P3)")
    void shouldResolveLastParticipantOnRound2Pick1() {
      DraftRegionCursor lastPickR1 = DraftRegionCursor.restore(DRAFT_ID, REGION, 1, 3, ORDER);
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(lastPickR1));
      when(cursorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      Optional<SnakeTurn> result = orchestrator.advance(DRAFT_ID, REGION);

      assertThat(result.get().getParticipantId()).isEqualTo(P3);
    }

    @Test
    @DisplayName("persists advanced cursor to repository")
    void shouldPersistAdvancedCursor() {
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(cursorRound1Pick1));
      when(cursorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      orchestrator.advance(DRAFT_ID, REGION);

      ArgumentCaptor<DraftRegionCursor> captor = ArgumentCaptor.forClass(DraftRegionCursor.class);
      verify(cursorRepository).save(captor.capture());
      assertThat(captor.getValue().getCurrentPick()).isEqualTo(2);
    }
  }

  // ===== getCurrentTurn =====

  @Nested
  @DisplayName("getCurrentTurn")
  class GetCurrentTurn {

    @Test
    @DisplayName("returns empty when cursor not initialised")
    void shouldReturnEmptyWhenNotInitialised() {
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION)).thenReturn(Optional.empty());

      Optional<SnakeTurn> result = orchestrator.getCurrentTurn(DRAFT_ID, REGION);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns correct turn without modifying state")
    void shouldReturnCurrentTurnWithoutSaving() {
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(cursorRound1Pick1));

      Optional<SnakeTurn> result = orchestrator.getCurrentTurn(DRAFT_ID, REGION);

      assertThat(result).isPresent();
      assertThat(result.get().getParticipantId()).isEqualTo(P1);
      verify(cursorRepository, never()).save(any());
    }
  }

  // ===== Snake direction edge cases =====

  @Nested
  @DisplayName("snake direction")
  class SnakeDirection {

    @Test
    @DisplayName("odd rounds are forward (isReversed = false)")
    void oddRoundsAreForward() {
      DraftRegionCursor cursor = DraftRegionCursor.restore(DRAFT_ID, REGION, 3, 2, ORDER);
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(cursor));

      Optional<SnakeTurn> turn = orchestrator.getCurrentTurn(DRAFT_ID, REGION);

      assertThat(turn.get().isReversed()).isFalse();
    }

    @Test
    @DisplayName("even rounds are reversed (isReversed = true)")
    void evenRoundsAreReversed() {
      DraftRegionCursor cursor = DraftRegionCursor.restore(DRAFT_ID, REGION, 2, 1, ORDER);
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(cursor));

      Optional<SnakeTurn> turn = orchestrator.getCurrentTurn(DRAFT_ID, REGION);

      assertThat(turn.get().isReversed()).isTrue();
    }

    @Test
    @DisplayName("2-participant draft — P2 picks last in round 1, P2 picks first in round 2")
    void twoParticipantSnakeDirectionFlip() {
      List<UUID> twoOrder = List.of(P1, P2);
      // Round 1, pick 2 = P2
      DraftRegionCursor cursor = DraftRegionCursor.restore(DRAFT_ID, REGION, 1, 2, twoOrder);
      when(cursorRepository.findByDraftIdAndRegion(DRAFT_ID, REGION))
          .thenReturn(Optional.of(cursor));
      when(cursorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      Optional<SnakeTurn> r1p2 = orchestrator.getCurrentTurn(DRAFT_ID, REGION);
      assertThat(r1p2.get().getParticipantId()).isEqualTo(P2);

      // Advance -> round 2, pick 1 reversed = P2 again (last element)
      Optional<SnakeTurn> r2p1 = orchestrator.advance(DRAFT_ID, REGION);
      assertThat(r2p1.get().getParticipantId()).isEqualTo(P2);
    }
  }
}
