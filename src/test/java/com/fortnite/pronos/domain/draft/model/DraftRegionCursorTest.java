package com.fortnite.pronos.domain.draft.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("DraftRegionCursor")
class DraftRegionCursorTest {

  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID P1 = UUID.randomUUID();
  private static final UUID P2 = UUID.randomUUID();
  private static final UUID P3 = UUID.randomUUID();

  @Nested
  @DisplayName("advance()")
  class Advance {

    @Test
    @DisplayName("wraps to round 2 pick 1 after last pick of round 1 (2 participants)")
    void wrapsToNextRound_2participants() {
      DraftRegionCursor cursor = new DraftRegionCursor(DRAFT_ID, "EU", List.of(P1, P2));
      // Round 1, pick 1 → advance → Round 1, pick 2
      DraftRegionCursor after1 = cursor.advance();
      assertThat(after1.getCurrentRound()).isEqualTo(1);
      assertThat(after1.getCurrentPick()).isEqualTo(2);

      // Round 1, pick 2 → advance → Round 2, pick 1 (wrap)
      DraftRegionCursor after2 = after1.advance();
      assertThat(after2.getCurrentRound()).isEqualTo(2);
      assertThat(after2.getCurrentPick()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 8})
    @DisplayName("no IndexOutOfBoundsException when advancing through a full round")
    void noIndexOutOfBounds_fullRound(int participantCount) {
      List<UUID> participants = generateParticipants(participantCount);
      DraftRegionCursor cursor = new DraftRegionCursor(DRAFT_ID, "GLOBAL", participants);

      // Advance through all picks in round 1 — no exception expected
      for (int i = 1; i < participantCount; i++) {
        cursor = cursor.advance();
        assertThat(cursor.getCurrentRound()).isEqualTo(1);
        assertThat(cursor.getCurrentPick()).isEqualTo(i + 1);
      }

      // One more advance wraps to round 2
      DraftRegionCursor round2 = cursor.advance();
      assertThat(round2.getCurrentRound()).isEqualTo(2);
      assertThat(round2.getCurrentPick()).isEqualTo(1);
    }

    @Test
    @DisplayName("advances multiple rounds without error (boundary: round 3)")
    void advancesBeyondRound2() {
      List<UUID> participants = List.of(P1, P2);
      DraftRegionCursor cursor = new DraftRegionCursor(DRAFT_ID, "NAW", participants);

      // Advance through rounds 1 and 2 completely (4 total picks) → start of round 3
      for (int i = 0; i < 4; i++) {
        cursor = cursor.advance();
      }

      assertThat(cursor.getCurrentRound()).isEqualTo(3);
      assertThat(cursor.getCurrentPick()).isEqualTo(1);
    }

    @Test
    @DisplayName("immutable — advance() returns new cursor without mutating original")
    void isImmutable() {
      DraftRegionCursor original = new DraftRegionCursor(DRAFT_ID, "EU", List.of(P1, P2));
      DraftRegionCursor advanced = original.advance();

      assertThat(original.getCurrentRound()).isEqualTo(1);
      assertThat(original.getCurrentPick()).isEqualTo(1);
      assertThat(advanced.getCurrentPick()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("constructor validation")
  class ConstructorValidation {

    @Test
    @DisplayName("throws when snakeOrder is empty")
    void throwsOnEmptySnakeOrder() {
      assertThatThrownBy(() -> new DraftRegionCursor(DRAFT_ID, "EU", List.of()))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("throws when draftId is null")
    void throwsOnNullDraftId() {
      assertThatThrownBy(() -> new DraftRegionCursor(null, "EU", List.of(P1)))
          .isInstanceOf(NullPointerException.class);
    }
  }

  // ===== HELPERS =====

  private List<UUID> generateParticipants(int count) {
    return java.util.stream.IntStream.range(0, count).mapToObj(i -> UUID.randomUUID()).toList();
  }
}
