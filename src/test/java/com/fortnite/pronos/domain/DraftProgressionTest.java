package com.fortnite.pronos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.DraftProgression.PickPosition;

/** Unit tests for DraftProgression domain rules. Pure domain tests - no Spring context required. */
@DisplayName("DraftProgression")
class DraftProgressionTest {

  @Nested
  @DisplayName("calculateTotalRounds")
  class CalculateTotalRounds {

    @Test
    @DisplayName("calculates rounds for 3 participants with default players per team")
    void calculatesFor3Participants() {
      int rounds = DraftProgression.calculateTotalRounds(3);
      assertThat(rounds).isEqualTo(3);
    }

    @Test
    @DisplayName("calculates rounds for custom players per team")
    void calculatesForCustomPlayersPerTeam() {
      int rounds = DraftProgression.calculateTotalRounds(4, 5);
      assertThat(rounds).isEqualTo(5);
    }

    @Test
    @DisplayName("rejects zero participants")
    void rejectsZeroParticipants() {
      assertThatThrownBy(() -> DraftProgression.calculateTotalRounds(0))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects negative players per team")
    void rejectsNegativePlayersPerTeam() {
      assertThatThrownBy(() -> DraftProgression.calculateTotalRounds(3, -1))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("nextPick")
  class NextPick {

    @Test
    @DisplayName("advances pick within same round")
    void advancesPickWithinRound() {
      PickPosition next = DraftProgression.nextPick(1, 1, 3);

      assertThat(next.round()).isEqualTo(1);
      assertThat(next.pick()).isEqualTo(2);
    }

    @Test
    @DisplayName("advances to next round when round complete")
    void advancesToNextRound() {
      PickPosition next = DraftProgression.nextPick(1, 3, 3);

      assertThat(next.round()).isEqualTo(2);
      assertThat(next.pick()).isEqualTo(1);
    }

    @Test
    @DisplayName("rejects zero participants")
    void rejectsZeroParticipants() {
      assertThatThrownBy(() -> DraftProgression.nextPick(1, 1, 0))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("isDraftComplete")
  class IsDraftComplete {

    @Test
    @DisplayName("returns false when rounds remaining")
    void returnsFalseWhenRoundsRemaining() {
      assertThat(DraftProgression.isDraftComplete(2, 3)).isFalse();
    }

    @Test
    @DisplayName("returns false on last round")
    void returnsFalseOnLastRound() {
      assertThat(DraftProgression.isDraftComplete(3, 3)).isFalse();
    }

    @Test
    @DisplayName("returns true when past total rounds")
    void returnsTrueWhenPastTotalRounds() {
      assertThat(DraftProgression.isDraftComplete(4, 3)).isTrue();
    }
  }

  @Nested
  @DisplayName("remainingPicks")
  class RemainingPicks {

    @Test
    @DisplayName("calculates remaining picks mid-draft")
    void calculatesRemainingMidDraft() {
      // Round 2, pick 2, 3 total rounds, 3 participants
      // Remaining: 2 in round 2 + 3 in round 3 = 5
      int remaining = DraftProgression.remainingPicks(2, 2, 3, 3);
      assertThat(remaining).isEqualTo(5);
    }

    @Test
    @DisplayName("returns zero when draft complete")
    void returnsZeroWhenComplete() {
      int remaining = DraftProgression.remainingPicks(4, 1, 3, 3);
      assertThat(remaining).isZero();
    }

    @Test
    @DisplayName("calculates remaining at start of draft")
    void calculatesRemainingAtStart() {
      // Round 1, pick 1, 3 total rounds, 3 participants = 9 total picks
      int remaining = DraftProgression.remainingPicks(1, 1, 3, 3);
      assertThat(remaining).isEqualTo(9);
    }
  }

  @Nested
  @DisplayName("getParticipantForPick")
  class GetParticipantForPick {

    @Test
    @DisplayName("returns correct participant in forward order")
    void returnsCorrectInForwardOrder() {
      int participant = DraftProgression.getParticipantForPick(2, 1, 3, false);
      assertThat(participant).isEqualTo(1); // 0-indexed
    }

    @Test
    @DisplayName("uses forward order in odd rounds with snake draft")
    void usesForwardInOddRoundsSnake() {
      int participant = DraftProgression.getParticipantForPick(1, 1, 3, true);
      assertThat(participant).isEqualTo(0);
    }

    @Test
    @DisplayName("uses reverse order in even rounds with snake draft")
    void usesReverseInEvenRoundsSnake() {
      // Pick 1 in round 2 with 3 participants should be participant 2 (last)
      int participant = DraftProgression.getParticipantForPick(1, 2, 3, true);
      assertThat(participant).isEqualTo(2);
    }

    @Test
    @DisplayName("rejects invalid pick number")
    void rejectsInvalidPickNumber() {
      assertThatThrownBy(() -> DraftProgression.getParticipantForPick(0, 1, 3, false))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects pick number exceeding participants")
    void rejectsPickExceedingParticipants() {
      assertThatThrownBy(() -> DraftProgression.getParticipantForPick(4, 1, 3, false))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("PickPosition")
  class PickPositionTest {

    @Test
    @DisplayName("creates valid position")
    void createsValidPosition() {
      PickPosition pos = new PickPosition(2, 3);
      assertThat(pos.round()).isEqualTo(2);
      assertThat(pos.pick()).isEqualTo(3);
    }

    @Test
    @DisplayName("rejects zero round")
    void rejectsZeroRound() {
      assertThatThrownBy(() -> new PickPosition(0, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects zero pick")
    void rejectsZeroPick() {
      assertThatThrownBy(() -> new PickPosition(1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
