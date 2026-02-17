package com.fortnite.pronos.core.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
class JoinGameUseCaseTest {

  @Mock private GameDomainRepositoryPort gameDomainRepositoryPort;

  @Mock private UserRepositoryPort userRepositoryPort;

  @InjectMocks private JoinGameUseCase joinGameUseCase;

  private UUID userId;
  private UUID gameId;
  private UUID creatorId;
  private User user;
  private JoinGameRequest request;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    gameId = UUID.randomUUID();
    creatorId = UUID.randomUUID();

    user = new User();
    user.setId(userId);
    user.setUsername("participant");

    request = new JoinGameRequest(gameId, userId);
  }

  @Test
  void shouldJoinGameWhenValid() {
    Game game = buildDomainGame(GameStatus.CREATING, 3, creatorId);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));
    when(gameDomainRepositoryPort.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    boolean result = joinGameUseCase.execute(userId, request);

    assertThat(result).isTrue();
    verify(gameDomainRepositoryPort).save(any(Game.class));
  }

  @Test
  void shouldThrowWhenUserMissing() {
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(UserNotFoundException.class);

    verify(gameDomainRepositoryPort, never()).findById(gameId);
  }

  @Test
  void shouldThrowWhenGameMissing() {
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.findById(gameId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(GameNotFoundException.class);
  }

  @Test
  void shouldThrowWhenGameNotJoinable() {
    Game game = buildDomainGame(GameStatus.ACTIVE, 3, creatorId);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameStateException.class);

    verify(gameDomainRepositoryPort, never()).save(any(Game.class));
  }

  @Test
  void shouldThrowWhenGameIsFull() {
    Game game = buildDomainGame(GameStatus.CREATING, 2, creatorId);
    // Add a participant to fill the game (creator counts as 1, add 1 more = 2 = full)
    game.addParticipant(new GameParticipant(UUID.randomUUID(), "other", false));

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(GameFullException.class);

    verify(gameDomainRepositoryPort, never()).save(any(Game.class));
  }

  @Test
  void shouldThrowWhenUserAlreadyInGame() {
    // Build game with the user already as a participant
    List<GameParticipant> participants = new ArrayList<>();
    participants.add(new GameParticipant(creatorId, "creator", true));
    participants.add(new GameParticipant(userId, "participant", false));
    Game game =
        Game.restore(
            gameId,
            "Test",
            null,
            creatorId,
            5,
            GameStatus.CREATING,
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            List.of(),
            participants,
            null,
            false,
            5,
            null,
            2026);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(UserAlreadyInGameException.class);

    verify(gameDomainRepositoryPort, never()).save(any(Game.class));
  }

  @Test
  void shouldThrowWhenUserIsCreator() {
    Game game = buildDomainGame(GameStatus.CREATING, 3, userId);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameDomainRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameStateException.class);

    verify(gameDomainRepositoryPort, never()).save(any(Game.class));
  }

  private Game buildDomainGame(GameStatus status, int maxParticipants, UUID gameCreatorId) {
    List<GameParticipant> participants = new ArrayList<>();
    participants.add(new GameParticipant(gameCreatorId, "creator", true));
    return Game.restore(
        gameId,
        "Test Game",
        null,
        gameCreatorId,
        maxParticipants,
        status,
        LocalDateTime.now(),
        null,
        null,
        null,
        null,
        List.of(),
        participants,
        null,
        false,
        5,
        null,
        2026);
  }
}
