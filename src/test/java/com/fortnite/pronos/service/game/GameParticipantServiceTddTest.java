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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.exception.GameFullException;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.InvalidInvitationCodeException;
import com.fortnite.pronos.exception.UserAlreadyInGameException;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"java:S5778"})
class GameParticipantServiceTddTest {

  @Mock private GameDomainRepositoryPort gameRepository;
  @Mock private GameRepositoryPort legacyGameRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private GameParticipantService service;

  private UUID userId;
  private UUID creatorId;
  private UUID gameId;
  private User user;
  private Game game;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    gameId = UUID.randomUUID();

    user = new User();
    user.setId(userId);
    user.setUsername("player1");

    game =
        Game.restore(
            gameId,
            "Test Game",
            null,
            creatorId,
            4,
            GameStatus.CREATING,
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
  }

  @Nested
  class JoinWithCode {

    @Test
    void joinWithValidCode_permanent_returnsTrue() {
      game.setInvitationCode("VALIDCODE"); // no expiry = permanent
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("VALIDCODE");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("VALIDCODE")).thenReturn(Optional.of(game));
      when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      boolean result = service.joinGame(userId, request);

      assertThat(result).isTrue();
      verify(gameRepository).findByInvitationCodeForUpdate("VALIDCODE");
      verify(gameRepository).save(game);
    }

    @Test
    void joinWithValidCode_notYetExpired_returnsTrue() {
      game.setInvitationCode("FUTUREXP");
      game.setInvitationCodeExpiresAt(LocalDateTime.now().plusHours(24));
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("FUTUREXP");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("FUTUREXP")).thenReturn(Optional.of(game));
      when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      boolean result = service.joinGame(userId, request);

      assertThat(result).isTrue();
    }

    @Test
    void joinWithValidCode_consumesCodeAndExpiryAfterSuccessfulJoin() {
      LocalDateTime expiration = LocalDateTime.now().plusHours(24);
      game.setInvitationCode("ONESHOT1");
      game.setInvitationCodeExpiresAt(expiration);
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("ONESHOT1");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("ONESHOT1")).thenReturn(Optional.of(game));
      when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      boolean result = service.joinGame(userId, request);

      assertThat(result).isTrue();
      assertThat(game.getInvitationCode()).isNull();
      assertThat(game.getInvitationCodeExpiresAt()).isNull();
      verify(gameRepository).save(game);
    }

    @Test
    void joinWithValidCode_cannotBeReusedAfterFirstSuccessfulJoin() {
      UUID secondUserId = UUID.randomUUID();
      User secondUser = new User();
      secondUser.setId(secondUserId);
      secondUser.setUsername("player2");

      game.setInvitationCode("ONESHOT2");
      game.setInvitationCodeExpiresAt(LocalDateTime.now().plusHours(24));
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("ONESHOT2");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(userRepository.findById(secondUserId)).thenReturn(Optional.of(secondUser));
      when(gameRepository.findByInvitationCodeForUpdate("ONESHOT2"))
          .thenAnswer(
              ignored ->
                  "ONESHOT2".equals(game.getInvitationCode())
                      ? Optional.of(game)
                      : Optional.empty());
      when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      assertThat(service.joinGame(userId, request)).isTrue();

      assertThatThrownBy(() -> service.joinGame(secondUserId, request))
          .isInstanceOf(GameNotFoundException.class)
          .hasMessageContaining("ONESHOT2");
    }

    @Test
    void joinWithExpiredCode_throwsInvalidInvitationCodeException() {
      game.setInvitationCode("EXPIREDX");
      game.setInvitationCodeExpiresAt(LocalDateTime.now().minusHours(1));
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("EXPIREDX");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("EXPIREDX")).thenReturn(Optional.of(game));

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(InvalidInvitationCodeException.class)
          .hasMessageContaining("Invitation code is expired or invalid");

      verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void joinWithNullCode_noInvitationCode_fallsBackToGameId() {
      JoinGameRequest request = new JoinGameRequest();
      request.setGameId(gameId); // no invitationCode → finds by ID

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
      when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

      boolean result = service.joinGame(userId, request);

      assertThat(result).isTrue();
      verify(gameRepository).findById(gameId);
      verify(gameRepository, never()).findByInvitationCodeForUpdate(any());
      verify(gameRepository, never()).findByInvitationCode(any());
    }

    @Test
    void joinWithNonExistentCode_throwsGameNotFoundException() {
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("UNKNOWN1");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("UNKNOWN1")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(GameNotFoundException.class);

      verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void joinWithValidCode_gameFull_throwsGameFullException() {
      // maxParticipants = 4 with a full game (3 non-creator participants + 1 creator slot)
      game.setInvitationCode("FULLGAME");
      LocalDateTime expiration = LocalDateTime.now().plusHours(24);
      game.setInvitationCodeExpiresAt(expiration);
      game.addParticipant(
          new com.fortnite.pronos.domain.game.model.GameParticipant(
              UUID.randomUUID(), "p1", false));
      game.addParticipant(
          new com.fortnite.pronos.domain.game.model.GameParticipant(
              UUID.randomUUID(), "p2", false));
      game.addParticipant(
          new com.fortnite.pronos.domain.game.model.GameParticipant(
              UUID.randomUUID(), "p3", false));
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("FULLGAME");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("FULLGAME")).thenReturn(Optional.of(game));

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(GameFullException.class);

      verify(gameRepository, never()).save(any(Game.class));
      assertThat(game.getInvitationCode()).isEqualTo("FULLGAME");
      assertThat(game.getInvitationCodeExpiresAt()).isEqualTo(expiration);
    }

    @Test
    void joinWithValidCode_alreadyParticipant_throwsUserAlreadyInGameException() {
      game.setInvitationCode("ALREADYIN");
      game.addParticipant(
          new com.fortnite.pronos.domain.game.model.GameParticipant(userId, "player1", false));
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("ALREADYIN");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("ALREADYIN")).thenReturn(Optional.of(game));

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(UserAlreadyInGameException.class);

      verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void joinWithValidCode_gameNotInCreatingStatus_throwsInvalidGameStateException() {
      Game draftingGame =
          Game.restore(
              gameId,
              "Drafting Game",
              null,
              creatorId,
              4,
              GameStatus.ACTIVE,
              LocalDateTime.now(),
              null,
              null,
              "ACTIVECD",
              null,
              List.of(),
              List.of(),
              null,
              false,
              5,
              null,
              2026);
      JoinGameRequest request = new JoinGameRequest();
      request.setInvitationCode("ACTIVECD");

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameRepository.findByInvitationCodeForUpdate("ACTIVECD"))
          .thenReturn(Optional.of(draftingGame));

      assertThatThrownBy(() -> service.joinGame(userId, request))
          .isInstanceOf(InvalidGameStateException.class);

      verify(gameRepository, never()).save(any(Game.class));
    }
  }
}
