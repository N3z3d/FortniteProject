package com.fortnite.pronos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.TeamRoster.ValidationResult;

/** Unit tests for TeamRoster domain rules. Pure domain tests - no Spring context required. */
@DisplayName("TeamRoster")
class TeamRosterTest {

  @Nested
  @DisplayName("canAddPlayer")
  class CanAddPlayer {

    @Test
    @DisplayName("allows adding player to roster with available position")
    void allowsAddingPlayer() {
      ValidationResult result =
          TeamRoster.canAddPlayer(Set.of(), UUID.randomUUID(), Set.of(), 1, 10);

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects null player ID")
    void rejectsNullPlayerId() {
      ValidationResult result = TeamRoster.canAddPlayer(Set.of(), null, Set.of(), 1, 10);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("null");
    }

    @Test
    @DisplayName("rejects duplicate player")
    void rejectsDuplicatePlayer() {
      UUID playerId = UUID.randomUUID();
      ValidationResult result =
          TeamRoster.canAddPlayer(Set.of(playerId), playerId, Set.of(), 1, 10);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("already on the team");
    }

    @Test
    @DisplayName("rejects when roster is full")
    void rejectsFullRoster() {
      UUID p1 = UUID.randomUUID();
      UUID p2 = UUID.randomUUID();
      ValidationResult result =
          TeamRoster.canAddPlayer(Set.of(p1, p2), UUID.randomUUID(), Set.of(1, 2), 3, 2);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("full");
    }

    @Test
    @DisplayName("rejects invalid position")
    void rejectsInvalidPosition() {
      ValidationResult result =
          TeamRoster.canAddPlayer(Set.of(), UUID.randomUUID(), Set.of(), 0, 10);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("Position");
    }

    @Test
    @DisplayName("rejects duplicate position")
    void rejectsDuplicatePosition() {
      ValidationResult result =
          TeamRoster.canAddPlayer(Set.of(), UUID.randomUUID(), Set.of(1), 1, 10);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("already taken");
    }
  }

  @Nested
  @DisplayName("validateUniquePositions")
  class ValidateUniquePositions {

    @Test
    @DisplayName("accepts empty positions list")
    void acceptsEmpty() {
      ValidationResult result = TeamRoster.validateUniquePositions(List.of());

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("accepts unique positions")
    void acceptsUniquePositions() {
      ValidationResult result = TeamRoster.validateUniquePositions(List.of(1, 2, 3));

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects duplicate positions")
    void rejectsDuplicates() {
      ValidationResult result = TeamRoster.validateUniquePositions(List.of(1, 2, 2, 3));

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("Duplicate");
    }

    @Test
    @DisplayName("rejects null positions")
    void rejectsNull() {
      ValidationResult result = TeamRoster.validateUniquePositions(Arrays.asList(1, null, 3));

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("positive");
    }

    @Test
    @DisplayName("rejects zero position")
    void rejectsZero() {
      ValidationResult result = TeamRoster.validateUniquePositions(List.of(1, 0, 3));

      assertThat(result.valid()).isFalse();
    }
  }

  @Nested
  @DisplayName("nextAvailablePosition")
  class NextAvailablePosition {

    @Test
    @DisplayName("returns 1 for empty roster")
    void returns1ForEmpty() {
      int next = TeamRoster.nextAvailablePosition(Set.of());

      assertThat(next).isEqualTo(1);
    }

    @Test
    @DisplayName("returns max + 1 for non-empty roster")
    void returnsMaxPlusOne() {
      int next = TeamRoster.nextAvailablePosition(Set.of(1, 3, 5));

      assertThat(next).isEqualTo(6);
    }

    @Test
    @DisplayName("handles null input")
    void handlesNull() {
      int next = TeamRoster.nextAvailablePosition(null);

      assertThat(next).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("canRemovePlayer")
  class CanRemovePlayer {

    @Test
    @DisplayName("allows removing player above minimum")
    void allowsRemovingAboveMinimum() {
      UUID p1 = UUID.randomUUID();
      UUID p2 = UUID.randomUUID();
      ValidationResult result = TeamRoster.canRemovePlayer(Set.of(p1, p2), p1, 1);

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects null player ID")
    void rejectsNullPlayerId() {
      ValidationResult result = TeamRoster.canRemovePlayer(Set.of(UUID.randomUUID()), null, 0);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("null");
    }

    @Test
    @DisplayName("rejects player not on team")
    void rejectsPlayerNotOnTeam() {
      UUID p1 = UUID.randomUUID();
      UUID p2 = UUID.randomUUID();
      ValidationResult result = TeamRoster.canRemovePlayer(Set.of(p1), p2, 0);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("not on the team");
    }

    @Test
    @DisplayName("rejects when at minimum roster size")
    void rejectsAtMinimum() {
      UUID p1 = UUID.randomUUID();
      ValidationResult result = TeamRoster.canRemovePlayer(Set.of(p1), p1, 1);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("minimum");
    }
  }

  @Nested
  @DisplayName("isValidPosition")
  class IsValidPosition {

    @Test
    @DisplayName("accepts position 1 for empty roster")
    void acceptsPosition1ForEmpty() {
      assertThat(TeamRoster.isValidPosition(1, 0)).isTrue();
    }

    @Test
    @DisplayName("accepts position up to roster size + 1")
    void acceptsUpToRosterSizePlusOne() {
      assertThat(TeamRoster.isValidPosition(4, 3)).isTrue();
    }

    @Test
    @DisplayName("rejects position 0")
    void rejectsPosition0() {
      assertThat(TeamRoster.isValidPosition(0, 3)).isFalse();
    }

    @Test
    @DisplayName("rejects position much larger than roster")
    void rejectsPositionMuchLarger() {
      assertThat(TeamRoster.isValidPosition(10, 3)).isFalse();
    }
  }
}
