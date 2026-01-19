package com.fortnite.pronos.domain.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.GameLifecycle;
import com.fortnite.pronos.domain.ParticipantRules;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

/** Unit tests for GameDomainFacade. Tests integration between entities and domain rules. */
@DisplayName("GameDomainFacade")
class GameDomainFacadeTest {

  private GameDomainFacade facade;
  private Game game;
  private User creator;

  @BeforeEach
  void setUp() {
    facade = new GameDomainFacade();
    creator = createUser("creator");
    game = createGame(creator, GameStatus.CREATING, 10);
  }

  @Nested
  @DisplayName("tryStartDraft")
  class TryStartDraft {

    @Test
    @DisplayName("starts draft when conditions are met")
    void startsDraftWhenConditionsMet() {
      addParticipant(game, createUser("player1"));

      GameLifecycle.TransitionResult result = facade.tryStartDraft(game);

      assertThat(result.allowed()).isTrue();
      assertThat(game.getStatus()).isEqualTo(GameStatus.DRAFTING);
    }

    @Test
    @DisplayName("rejects when not enough participants")
    void rejectsWhenNotEnoughParticipants() {
      // Only creator, no other participants
      game.setParticipants(new ArrayList<>());

      GameLifecycle.TransitionResult result = facade.tryStartDraft(game);

      assertThat(result.allowed()).isFalse();
      assertThat(game.getStatus()).isEqualTo(GameStatus.CREATING);
    }
  }

  @Nested
  @DisplayName("canAddParticipant")
  class CanAddParticipant {

    @Test
    @DisplayName("allows adding new participant")
    void allowsAddingNewParticipant() {
      User newUser = createUser("newPlayer");

      ParticipantRules.ValidationResult result = facade.canAddParticipant(game, newUser);

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects adding creator")
    void rejectsAddingCreator() {
      ParticipantRules.ValidationResult result = facade.canAddParticipant(game, creator);

      assertThat(result.valid()).isFalse();
    }

    @Test
    @DisplayName("rejects when game is full")
    void rejectsWhenFull() {
      game.setMaxParticipants(2);
      addParticipant(game, createUser("player1"));

      User newUser = createUser("player2");
      ParticipantRules.ValidationResult result = facade.canAddParticipant(game, newUser);

      assertThat(result.valid()).isFalse();
    }
  }

  @Nested
  @DisplayName("ensureInvitationCode")
  class EnsureInvitationCode {

    @Test
    @DisplayName("generates code when none exists")
    void generatesCodeWhenNoneExists() {
      String code = facade.ensureInvitationCode(game);

      assertThat(code).isNotNull().hasSize(8);
      assertThat(game.getInvitationCode()).isEqualTo(code);
    }

    @Test
    @DisplayName("returns existing code")
    void returnsExistingCode() {
      game.setInvitationCode("EXISTING1");

      String code = facade.ensureInvitationCode(game);

      assertThat(code).isEqualTo("EXISTING1");
    }
  }

  @Nested
  @DisplayName("validateGameCreation")
  class ValidateGameCreation {

    @Test
    @DisplayName("accepts valid parameters")
    void acceptsValidParams() {
      ParticipantRules.ValidationResult result =
          facade.validateGameCreation("Test Game", creator, 10);

      assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("rejects empty name")
    void rejectsEmptyName() {
      ParticipantRules.ValidationResult result = facade.validateGameCreation("", creator, 10);

      assertThat(result.valid()).isFalse();
    }
  }

  private User createUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }

  private Game createGame(User creator, GameStatus status, int maxParticipants) {
    Game g = new Game();
    g.setId(UUID.randomUUID());
    g.setCreator(creator);
    g.setStatus(status);
    g.setMaxParticipants(maxParticipants);
    g.setParticipants(new ArrayList<>());
    return g;
  }

  private void addParticipant(Game game, User user) {
    GameParticipant participant = new GameParticipant();
    participant.setUser(user);
    participant.setGame(game);
    game.getParticipants().add(participant);
  }
}
