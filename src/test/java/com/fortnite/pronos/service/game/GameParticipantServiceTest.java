package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameParticipantService")
@SuppressWarnings({"java:S5778"})
class GameParticipantServiceTest {

  @Mock private GameDomainRepositoryPort gameRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private GameParticipantService service;

  private UUID userId;
  private UUID gameId;
  private User user;
  private User creator;
  private Game game;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    gameId = UUID.randomUUID();

    creator = new User();
    creator.setId(UUID.randomUUID());
    creator.setUsername("creator");

    user = new User();
    user.setId(userId);
    user.setUsername("testuser");

    GameParticipant creatorParticipant =
        GameParticipant.restore(
            UUID.randomUUID(),
            creator.getId(),
            "creator",
            1,
            LocalDateTime.now(),
            null,
            true,
            List.of());
    game =
        Game.restore(
            gameId,
            "Test Game",
            null,
            creator.getId(),
            3,
            GameStatus.CREATING,
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            List.of(),
            List.of(creatorParticipant),
            null,
            false,
            5,
            null,
            2026);
  }

  @Nested
  @DisplayName("joinGame")
  class JoinGame {

    @Test
    @DisplayName("throws UserNotFoundException when user does not exist")
    void throwsUserNotFoundWhenUserDoesNotExist() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId);

      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessageContaining(userId.toString());
    }

    @Test
    @DisplayName("throws GameNotFoundException when game does not exist")
    void throwsGameNotFoundWhenGameDoesNotExist() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByIdForUpdate(gameId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("successfully joins game when user and game exist")
    void successfullyJoinsGame() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByIdForUpdate(gameId)).thenReturn(Optional.of(game));
      when(gameRepository.save(any(Game.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      boolean result = service.joinGame(userId, request);

      assertThat(result).isTrue();
      verify(gameRepository).save(game);
    }

    @Test
    @DisplayName("throws UserAlreadyInGameException when user is already participant")
    void throwsUserAlreadyInGameWhenUserAlreadyParticipant() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId);
      game.addParticipant(new GameParticipant(userId, user.getUsername(), false));

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByIdForUpdate(gameId)).thenReturn(Optional.of(game));

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(UserAlreadyInGameException.class);

      verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    @DisplayName("throws GameFullException when game is full")
    void throwsGameFullWhenGameIsFull() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId);
      game.addParticipant(new GameParticipant(UUID.randomUUID(), "p1", false));
      game.addParticipant(new GameParticipant(UUID.randomUUID(), "p2", false));

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByIdForUpdate(gameId)).thenReturn(Optional.of(game));

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(GameFullException.class);

      verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    @DisplayName("throws InvalidGameStateException when game status is not creating")
    void throwsInvalidGameStateWhenGameStarted() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId);
      Game activeGame =
          Game.restore(
              gameId,
              "Test Game",
              null,
              creator.getId(),
              3,
              GameStatus.ACTIVE,
              LocalDateTime.now(),
              null,
              null,
              null,
              null,
              List.of(),
              List.of(),
              null,
              false,
              5,
              null,
              2026);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByIdForUpdate(gameId)).thenReturn(Optional.of(activeGame));

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(InvalidGameStateException.class);

      verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    @DisplayName("finds game by locked invitation code when provided")
    void findsGameByInvitationCode() {
      game.setInvitationCode("TESTCODE"); // valid: no expiry → permanent
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("TESTCODE");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("TESTCODE")).thenReturn(Optional.of(game));
      when(gameRepository.save(any(Game.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      boolean result = service.joinGame(userId, request);

      assertThat(result).isTrue();
      verify(gameRepository).findByInvitationCodeForUpdate("TESTCODE");
      verify(gameRepository).save(game);
    }
  }

  @Nested
  @DisplayName("leaveGame")
  class LeaveGame {

    @Test
    @DisplayName("throws UserNotFoundException when user does not exist")
    void throwsUserNotFoundWhenUserDoesNotExist() {
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.leaveGame(userId, gameId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("throws GameNotFoundException when game does not exist")
    void throwsGameNotFoundWhenGameDoesNotExist() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByIdForUpdate(gameId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.leaveGame(userId, gameId))
          .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("throws UnauthorizedAccessException when user is creator")
    void throwsUnauthorizedWhenUserIsCreator() {
      User creatorUser = new User();
      creatorUser.setId(creator.getId());
      creatorUser.setUsername("creator");

      when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creatorUser));
      when(gameRepository.findByIdForUpdate(gameId)).thenReturn(Optional.of(game));

      assertThatThrownBy(() -> service.leaveGame(creator.getId(), gameId))
          .isInstanceOf(UnauthorizedAccessException.class);
    }
  }

  @Nested
  @DisplayName("isUserParticipant")
  class IsUserParticipant {

    @Test
    @DisplayName("returns true when user is participant")
    void returnsTrueWhenParticipant() {
      when(gameParticipantRepository.existsByUserIdAndGameId(userId, gameId)).thenReturn(true);

      boolean result = service.isUserParticipant(userId, gameId);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("returns false when user is not participant")
    void returnsFalseWhenNotParticipant() {
      when(gameParticipantRepository.existsByUserIdAndGameId(userId, gameId)).thenReturn(false);

      boolean result = service.isUserParticipant(userId, gameId);

      assertThat(result).isFalse();
    }
  }
}
