package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.fortnite.pronos.application.facade.GameDomainFacade;
import com.fortnite.pronos.domain.ParticipantRules;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameParticipantService")
class GameParticipantServiceTest {

  @Mock private GameRepository gameRepository;
  @Mock private GameParticipantRepository gameParticipantRepository;
  @Mock private UserRepository userRepository;
  @Mock private GameDomainFacade gameDomainFacade;

  @InjectMocks private GameParticipantService service;

  private UUID userId;
  private UUID gameId;
  private User user;
  private Game game;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    gameId = UUID.randomUUID();

    user = new User();
    user.setId(userId);
    user.setUsername("testuser");

    game = new Game();
    game.setId(gameId);
    game.setName("Test Game");
    game.setStatus(GameStatus.CREATING);
    game.setCreator(user);
  }

  @Nested
  @DisplayName("joinGame")
  class JoinGame {

    @Test
    @DisplayName("throws UserNotFoundException when user does not exist")
    void throwsUserNotFoundWhenUserDoesNotExist() {
      UUID unknownUserId = UUID.randomUUID();
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId);

      when(((UserRepositoryPort) userRepository).findById(unknownUserId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.joinGame(unknownUserId, request))
          .isInstanceOf(UserNotFoundException.class)
          .hasMessageContaining(unknownUserId.toString());

      verify(((UserRepositoryPort) userRepository), never()).save(any(User.class));
    }

    @Test
    @DisplayName("throws GameNotFoundException when game does not exist")
    void throwsGameNotFoundWhenGameDoesNotExist() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(UUID.randomUUID());

      when(((UserRepositoryPort) userRepository).findById(userId)).thenReturn(Optional.of(user));
      when(((GameRepositoryPort) gameRepository).findById(request.getGameId()))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("successfully joins game when user and game exist")
    void successfullyJoinsGame() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId);

      User creator = new User();
      creator.setId(UUID.randomUUID());
      game.setCreator(creator);

      when(((UserRepositoryPort) userRepository).findById(userId)).thenReturn(Optional.of(user));
      when(((GameRepositoryPort) gameRepository).findById(gameId)).thenReturn(Optional.of(game));
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .existsByUserIdAndGameId(creator.getId(), gameId))
          .thenReturn(true);
      when(gameDomainFacade.canAddParticipant(game, user))
          .thenReturn(new ParticipantRules.ValidationResult(true, null));

      boolean result = service.joinGame(userId, request);

      assertThat(result).isTrue();
      verify(((GameParticipantRepositoryPort) gameParticipantRepository)).save(any());
    }

    @Test
    @DisplayName("finds game by invitation code when provided")
    void findsGameByInvitationCode() {
      String invitationCode = "TESTCODE";
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode(invitationCode);

      User creator = new User();
      creator.setId(UUID.randomUUID());
      game.setCreator(creator);

      when(((UserRepositoryPort) userRepository).findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCode(invitationCode)).thenReturn(Optional.of(game));
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .existsByUserIdAndGameId(creator.getId(), game.getId()))
          .thenReturn(true);
      when(gameDomainFacade.canAddParticipant(game, user))
          .thenReturn(new ParticipantRules.ValidationResult(true, null));

      boolean result = service.joinGame(userId, request);

      assertThat(result).isTrue();
      verify(gameRepository).findByInvitationCode(invitationCode);
    }
  }

  @Nested
  @DisplayName("leaveGame")
  class LeaveGame {

    @Test
    @DisplayName("throws UserNotFoundException when user does not exist")
    void throwsUserNotFoundWhenUserDoesNotExist() {
      UUID unknownUserId = UUID.randomUUID();

      when(((UserRepositoryPort) userRepository).findById(unknownUserId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.leaveGame(unknownUserId, gameId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("throws GameNotFoundException when game does not exist")
    void throwsGameNotFoundWhenGameDoesNotExist() {
      when(((UserRepositoryPort) userRepository).findById(userId)).thenReturn(Optional.of(user));
      when(((GameRepositoryPort) gameRepository).findById(gameId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.leaveGame(userId, gameId))
          .isInstanceOf(GameNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("isUserParticipant")
  class IsUserParticipant {

    @Test
    @DisplayName("returns true when user is participant")
    void returnsTrueWhenParticipant() {
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .existsByUserIdAndGameId(userId, gameId))
          .thenReturn(true);

      boolean result = service.isUserParticipant(userId, gameId);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("returns false when user is not participant")
    void returnsFalseWhenNotParticipant() {
      when(((GameParticipantRepositoryPort) gameParticipantRepository)
              .existsByUserIdAndGameId(userId, gameId))
          .thenReturn(false);

      boolean result = service.isUserParticipant(userId, gameId);

      assertThat(result).isFalse();
    }
  }
}
