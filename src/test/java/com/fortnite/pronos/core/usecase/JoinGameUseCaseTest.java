package com.fortnite.pronos.core.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
class JoinGameUseCaseTest {

  @Mock private GameRepositoryPort gameRepositoryPort;

  @Mock private UserRepositoryPort userRepositoryPort;

  @Mock private GameParticipantRepositoryPort gameParticipantRepositoryPort;

  @InjectMocks private JoinGameUseCase joinGameUseCase;

  private UUID userId;
  private UUID gameId;
  private User user;
  private User creator;
  private JoinGameRequest request;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    gameId = UUID.randomUUID();

    user = new User();
    user.setId(userId);
    user.setUsername("participant");

    creator = new User();
    creator.setId(UUID.randomUUID());
    creator.setUsername("creator");

    request = new JoinGameRequest(gameId, userId);
  }

  @Test
  void shouldJoinGameWhenValid() {
    Game game = buildGame(GameStatus.CREATING, 3, creator);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));
    when(gameParticipantRepositoryPort.countByGame(game)).thenReturn(1L);
    when(gameParticipantRepositoryPort.existsByGameAndUser(game, user)).thenReturn(false);
    when(gameParticipantRepositoryPort.save(any(GameParticipant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(gameRepositoryPort.save(game)).thenReturn(game);

    boolean result = joinGameUseCase.execute(userId, request);

    assertThat(result).isTrue();

    ArgumentCaptor<GameParticipant> participantCaptor =
        ArgumentCaptor.forClass(GameParticipant.class);
    verify(gameParticipantRepositoryPort).save(participantCaptor.capture());
    GameParticipant saved = participantCaptor.getValue();
    assertThat(saved.getGame()).isEqualTo(game);
    assertThat(saved.getUser()).isEqualTo(user);

    verify(gameRepositoryPort).save(game);
  }

  @Test
  void shouldThrowWhenUserMissing() {
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(UserNotFoundException.class);

    verify(gameRepositoryPort, never()).findById(gameId);
  }

  @Test
  void shouldThrowWhenGameMissing() {
    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameRepositoryPort.findById(gameId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(GameNotFoundException.class);
  }

  @Test
  void shouldThrowWhenGameNotJoinable() {
    Game game = buildGame(GameStatus.ACTIVE, 3, creator);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameStateException.class);

    verify(gameParticipantRepositoryPort, never()).save(any(GameParticipant.class));
  }

  @Test
  void shouldThrowWhenGameIsFull() {
    Game game = buildGame(GameStatus.CREATING, 2, creator);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));
    when(gameParticipantRepositoryPort.countByGame(game)).thenReturn(2L);

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(GameFullException.class);

    verify(gameParticipantRepositoryPort, never()).save(any(GameParticipant.class));
  }

  @Test
  void shouldThrowWhenUserAlreadyInGame() {
    Game game = buildGame(GameStatus.CREATING, 3, creator);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));
    when(gameParticipantRepositoryPort.countByGame(game)).thenReturn(1L);
    when(gameParticipantRepositoryPort.existsByGameAndUser(game, user)).thenReturn(true);

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(UserAlreadyInGameException.class);

    verify(gameParticipantRepositoryPort, never()).save(any(GameParticipant.class));
  }

  @Test
  void shouldThrowWhenUserIsCreator() {
    Game game = buildGame(GameStatus.CREATING, 3, user);

    when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
    when(gameRepositoryPort.findById(gameId)).thenReturn(Optional.of(game));
    when(gameParticipantRepositoryPort.countByGame(game)).thenReturn(0L);
    when(gameParticipantRepositoryPort.existsByGameAndUser(game, user)).thenReturn(false);

    assertThatThrownBy(() -> joinGameUseCase.execute(userId, request))
        .isInstanceOf(InvalidGameStateException.class);

    verify(gameParticipantRepositoryPort, never()).save(any(GameParticipant.class));
  }

  private Game buildGame(GameStatus status, int maxParticipants, User gameCreator) {
    Game game = new Game();
    game.setId(gameId);
    game.setStatus(status);
    game.setMaxParticipants(maxParticipants);
    game.setCreator(gameCreator);
    return game;
  }
}
