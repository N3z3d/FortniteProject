package com.fortnite.pronos.model;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests TDD pour un modèle de domaine riche de Game
 *
 * <p>Ce test définit d'abord les comportements attendus (RED) puis l'implémentation suivra (GREEN)
 * puis le refactoring (REFACTOR)
 */
@DisplayName("Game - Rich Domain Model Tests")
class GameRichDomainTest {

  private User creator;
  private Game game;
  private User participant1;
  private User participant2;

  @BeforeEach
  void setUp() {
    creator = createTestUser("creator", "creator@test.com");
    participant1 = createTestUser("participant1", "p1@test.com");
    participant2 = createTestUser("participant2", "p2@test.com");

    // RED: This will fail initially until we implement rich domain behavior
    game = new Game("Test Game", creator, 10);
  }

  @Nested
  @DisplayName("Game Creation Business Rules")
  class GameCreationRules {

    @Test
    @DisplayName("Should create game with valid parameters")
    void shouldCreateGameWithValidParameters() {
      // RED: Define expected behavior first
      Game newGame = new Game("Fantasy League", creator, 8);

      assertThat(newGame.getName()).isEqualTo("Fantasy League");
      assertThat(newGame.getCreator()).isEqualTo(creator);
      assertThat(newGame.getMaxParticipants()).isEqualTo(8);
      assertThat(newGame.getStatus()).isEqualTo(GameStatus.CREATING);
      assertThat(newGame.getParticipants()).isEmpty();
      assertThat(newGame.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should reject game with null or empty name")
    void shouldRejectGameWithInvalidName() {
      // RED: Business rule - name cannot be null or empty
      assertThatThrownBy(() -> new Game(null, creator, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Game name cannot be null or empty");

      assertThatThrownBy(() -> new Game("", creator, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Game name cannot be null or empty");

      assertThatThrownBy(() -> new Game("   ", creator, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Game name cannot be null or empty");
    }

    @Test
    @DisplayName("Should reject game with null creator")
    void shouldRejectGameWithNullCreator() {
      // RED: Business rule - creator cannot be null
      assertThatThrownBy(() -> new Game("Test Game", null, 10))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Game creator cannot be null");
    }

    @Test
    @DisplayName("Should reject game with invalid max participants")
    void shouldRejectGameWithInvalidMaxParticipants() {
      // RED: Business rule - max participants must be between 2 and 50
      assertThatThrownBy(() -> new Game("Test Game", creator, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max participants must be between 2 and 50");

      assertThatThrownBy(() -> new Game("Test Game", creator, 51))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Max participants must be between 2 and 50");
    }
  }

  @Nested
  @DisplayName("Game Participation Business Rules")
  class GameParticipationRules {

    @Test
    @DisplayName("Should allow user to join game successfully")
    void shouldAllowUserToJoinGame() {
      // RED: Business logic for joining game
      boolean joined = game.addParticipant(participant1);

      assertThat(joined).isTrue();
      assertThat(game.getParticipants()).hasSize(1);
      assertThat(game.getParticipants().get(0).getUser()).isEqualTo(participant1);
      assertThat(game.isParticipant(participant1)).isTrue();
    }

    @Test
    @DisplayName("Should prevent duplicate participation")
    void shouldPreventDuplicateParticipation() {
      // RED: Business rule - no duplicate participants
      game.addParticipant(participant1);

      boolean secondJoin = game.addParticipant(participant1);

      assertThat(secondJoin).isFalse();
      assertThat(game.getParticipants()).hasSize(1);
    }

    @Test
    @DisplayName("Should prevent creator from joining as participant")
    void shouldPreventCreatorFromJoining() {
      // RED: Business rule - creator is automatically part of the game
      boolean creatorJoin = game.addParticipant(creator);

      assertThat(creatorJoin).isFalse();
      assertThat(game.getParticipants()).isEmpty();
      assertThat(game.isParticipant(creator)).isTrue(); // Creator is always a participant
    }

    @Test
    @DisplayName("Should prevent joining when game is full")
    void shouldPreventJoiningWhenGameIsFull() {
      // RED: Create a game with max 2 participants for easier testing
      Game smallGame = new Game("Small Game", creator, 2);

      // Fill the game
      smallGame.addParticipant(participant1);

      // Try to add one more (should fail as creator + 1 participant = 2)
      boolean joined = smallGame.addParticipant(participant2);

      assertThat(joined).isFalse();
      assertThat(smallGame.isFull()).isTrue();
    }

    @Test
    @DisplayName("Should prevent joining when game has started")
    void shouldPreventJoiningWhenGameStarted() {
      // RED: Business rule - cannot join started games
      game.addParticipant(participant1);
      game.startDraft(); // This should change status to DRAFTING

      boolean joined = game.addParticipant(participant2);

      assertThat(joined).isFalse();
      assertThat(game.getStatus()).isEqualTo(GameStatus.DRAFTING);
    }
  }

  @Nested
  @DisplayName("Game State Transitions")
  class GameStateTransitions {

    @Test
    @DisplayName("Should start draft when minimum participants joined")
    void shouldStartDraftWhenReady() {
      // RED: Business rule - can start draft with at least 2 total players (creator + 1)
      game.addParticipant(participant1);

      boolean started = game.startDraft();

      assertThat(started).isTrue();
      assertThat(game.getStatus()).isEqualTo(GameStatus.DRAFTING);
      assertThat(game.canAddParticipants()).isFalse();
    }

    @Test
    @DisplayName("Should not start draft with insufficient participants")
    void shouldNotStartDraftWithInsufficientParticipants() {
      // RED: Cannot start with just creator (need at least 2 total)
      boolean started = game.startDraft();

      assertThat(started).isFalse();
      assertThat(game.getStatus()).isEqualTo(GameStatus.CREATING);
    }

    @Test
    @DisplayName("Should complete draft and start active game")
    void shouldCompleteExchangeAndStartActiveGame() {
      // RED: Game lifecycle - CREATING -> DRAFTING -> ACTIVE
      game.addParticipant(participant1);
      game.startDraft();

      boolean completed = game.completeDraft();

      assertThat(completed).isTrue();
      assertThat(game.getStatus()).isEqualTo(GameStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should finish game and calculate final results")
    void shouldFinishGameAndCalculateResults() {
      // RED: Complete game lifecycle
      game.addParticipant(participant1);
      game.startDraft();
      game.completeDraft();

      boolean finished = game.finishGame();

      assertThat(finished).isTrue();
      assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
      assertThat(game.getFinishedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Game Business Invariants")
  class GameBusinessInvariants {

    @Test
    @DisplayName("Should maintain participant count invariant")
    void shouldMaintainParticipantCountInvariant() {
      // RED: Total participants (including creator) never exceeds max
      game.addParticipant(participant1);
      game.addParticipant(participant2);

      assertThat(game.getTotalParticipantCount()).isLessThanOrEqualTo(game.getMaxParticipants());
    }

    @Test
    @DisplayName("Should validate game can only move forward in states")
    void shouldValidateStateTransitionDirection() {
      // RED: State machine - cannot go backwards
      game.addParticipant(participant1);
      game.startDraft();

      // Try to go back to CREATING (should fail)
      assertThatThrownBy(() -> game.resetToCreating())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot transition from DRAFTING to CREATING");
    }

    @Test
    @DisplayName("Should generate unique invitation codes")
    void shouldGenerateUniqueInvitationCodes() {
      // RED: Each game should have a unique invitation code
      String invitationCode = game.generateInvitationCode();

      assertThat(invitationCode).isNotNull();
      assertThat(invitationCode).hasSize(8); // 8-character codes
      assertThat(invitationCode).matches("[A-Z0-9]{8}"); // Alphanumeric uppercase

      // Code should be stored in the game
      assertThat(game.getInvitationCode()).isEqualTo(invitationCode);
    }
  }

  @Nested
  @DisplayName("Game Query Methods")
  class GameQueryMethods {

    @Test
    @DisplayName("Should correctly identify if user is participant")
    void shouldIdentifyParticipant() {
      // RED: Query methods for business logic
      game.addParticipant(participant1);

      assertThat(game.isParticipant(creator)).isTrue(); // Creator is always participant
      assertThat(game.isParticipant(participant1)).isTrue();
      assertThat(game.isParticipant(participant2)).isFalse();
    }

    @Test
    @DisplayName("Should calculate available spots correctly")
    void shouldCalculateAvailableSpots() {
      // RED: Business calculation
      assertThat(game.getAvailableSpots()).isEqualTo(9); // 10 max - 1 creator

      game.addParticipant(participant1);
      assertThat(game.getAvailableSpots()).isEqualTo(8); // 10 max - 1 creator - 1 participant
    }

    @Test
    @DisplayName("Should determine if game accepts new participants")
    void shouldDetermineIfAcceptsNewParticipants() {
      // RED: Business rule check
      assertThat(game.canAddParticipants()).isTrue();

      game.addParticipant(participant1); // Need minimum participants to start draft
      game.startDraft();
      assertThat(game.canAddParticipants()).isFalse();
    }
  }

  // Helper method to create test users
  private User createTestUser(String username, String email) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword("testpassword123");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2025);
    return user;
  }
}
