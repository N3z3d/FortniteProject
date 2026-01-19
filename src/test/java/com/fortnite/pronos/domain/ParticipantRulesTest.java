package com.fortnite.pronos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.ParticipantRules.ValidationResult;
import com.fortnite.pronos.model.GameStatus;

/** Unit tests for ParticipantRules domain rules. Pure domain tests - no Spring context required. */
@DisplayName("ParticipantRules")
class ParticipantRulesTest {

  @Nested
  @DisplayName("validateGameCreation")
  class ValidateGameCreation {

    @Test
    @DisplayName("accepts valid game creation parameters")
    void acceptsValidParameters() {
      ValidationResult result =
          ParticipantRules.validateGameCreation("Test Game", UUID.randomUUID(), 10);

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects null game name")
    void rejectsNullName() {
      ValidationResult result = ParticipantRules.validateGameCreation(null, UUID.randomUUID(), 10);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("name");
    }

    @Test
    @DisplayName("rejects empty game name")
    void rejectsEmptyName() {
      ValidationResult result = ParticipantRules.validateGameCreation("  ", UUID.randomUUID(), 10);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("name");
    }

    @Test
    @DisplayName("rejects null creator")
    void rejectsNullCreator() {
      ValidationResult result = ParticipantRules.validateGameCreation("Test", null, 10);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("creator");
    }

    @Test
    @DisplayName("rejects max participants below minimum")
    void rejectsMaxParticipantsBelowMin() {
      ValidationResult result = ParticipantRules.validateGameCreation("Test", UUID.randomUUID(), 1);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("between 2 and 50");
    }

    @Test
    @DisplayName("rejects max participants above maximum")
    void rejectsMaxParticipantsAboveMax() {
      ValidationResult result =
          ParticipantRules.validateGameCreation("Test", UUID.randomUUID(), 51);

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("between 2 and 50");
    }
  }

  @Nested
  @DisplayName("canAddParticipant")
  class CanAddParticipant {

    private final UUID creatorId = UUID.randomUUID();
    private final UUID newUserId = UUID.randomUUID();

    @Test
    @DisplayName("allows adding participant to CREATING game with space")
    void allowsAddingToCreatingGame() {
      ValidationResult result =
          ParticipantRules.canAddParticipant(
              GameStatus.CREATING, 2, 10, newUserId, creatorId, Set.of());

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects adding participant to non-CREATING game")
    void rejectsNonCreatingGame() {
      ValidationResult result =
          ParticipantRules.canAddParticipant(
              GameStatus.DRAFTING, 2, 10, newUserId, creatorId, Set.of());

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("already started");
    }

    @Test
    @DisplayName("rejects adding creator as participant")
    void rejectsAddingCreator() {
      ValidationResult result =
          ParticipantRules.canAddParticipant(
              GameStatus.CREATING, 2, 10, creatorId, creatorId, Set.of());

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("Creator");
    }

    @Test
    @DisplayName("rejects adding duplicate participant")
    void rejectsDuplicateParticipant() {
      ValidationResult result =
          ParticipantRules.canAddParticipant(
              GameStatus.CREATING, 2, 10, newUserId, creatorId, Set.of(newUserId));

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("already a participant");
    }

    @Test
    @DisplayName("rejects adding participant to full game")
    void rejectsFullGame() {
      ValidationResult result =
          ParticipantRules.canAddParticipant(
              GameStatus.CREATING, 10, 10, newUserId, creatorId, Set.of());

      assertThat(result.valid()).isFalse();
      assertThat(result.errorMessage()).contains("full");
    }
  }

  @Nested
  @DisplayName("calculateTotalParticipants")
  class CalculateTotalParticipants {

    @Test
    @DisplayName("adds 1 when creator not in participants list")
    void addsOneWhenCreatorNotInList() {
      int total = ParticipantRules.calculateTotalParticipants(3, false);

      assertThat(total).isEqualTo(4);
    }

    @Test
    @DisplayName("does not add when creator already in participants list")
    void doesNotAddWhenCreatorInList() {
      int total = ParticipantRules.calculateTotalParticipants(3, true);

      assertThat(total).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("calculateAvailableSpots")
  class CalculateAvailableSpots {

    @Test
    @DisplayName("returns difference when spots available")
    void returnsDifferenceWhenAvailable() {
      int spots = ParticipantRules.calculateAvailableSpots(10, 3);

      assertThat(spots).isEqualTo(7);
    }

    @Test
    @DisplayName("returns zero when full")
    void returnsZeroWhenFull() {
      int spots = ParticipantRules.calculateAvailableSpots(10, 10);

      assertThat(spots).isZero();
    }

    @Test
    @DisplayName("returns zero when over capacity")
    void returnsZeroWhenOverCapacity() {
      int spots = ParticipantRules.calculateAvailableSpots(10, 12);

      assertThat(spots).isZero();
    }
  }
}
