package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.exception.AccountDeletionBlockedException;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.game.GameParticipantService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"java:S5778"})
class UserDeletionServiceTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private GameParticipantService gameParticipantService;

  @InjectMocks private UserDeletionService service;

  private UUID userId;
  private UUID creatorId;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    creatorId = UUID.randomUUID();

    user = new User();
    user.setId(userId);
    user.setUsername("player1");
    user.setEmail("player1@test.com");
    user.setPassword("hashed");
  }

  private Game activeGame(UUID id, UUID gameCreatorId, GameStatus status) {
    return Game.restore(
        id,
        "Test Game",
        null,
        gameCreatorId,
        4,
        status,
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
  class NominalCases {

    @Test
    void deleteAccount_withNoGames_softDeletesUser() {
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameDomainRepository.findByCreatorId(userId)).thenReturn(List.of());
      when(gameDomainRepository.findGamesByUserId(userId)).thenReturn(List.of());

      service.deleteAccount(userId);

      verify(userRepository).softDelete(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void deleteAccount_withActiveParticipation_leavesGameBeforeSoftDelete() {
      UUID gameId = UUID.randomUUID();
      Game participantGame = activeGame(gameId, creatorId, GameStatus.CREATING);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameDomainRepository.findByCreatorId(userId)).thenReturn(List.of());
      when(gameDomainRepository.findGamesByUserId(userId)).thenReturn(List.of(participantGame));

      service.deleteAccount(userId);

      verify(gameParticipantService).leaveGame(userId, gameId);
      verify(userRepository).softDelete(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void deleteAccount_withMultipleActiveParticipations_leavesAllGames() {
      UUID gameId1 = UUID.randomUUID();
      UUID gameId2 = UUID.randomUUID();
      Game game1 = activeGame(gameId1, creatorId, GameStatus.CREATING);
      Game game2 = activeGame(gameId2, creatorId, GameStatus.DRAFTING);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameDomainRepository.findByCreatorId(userId)).thenReturn(List.of());
      when(gameDomainRepository.findGamesByUserId(userId)).thenReturn(List.of(game1, game2));

      service.deleteAccount(userId);

      verify(gameParticipantService, times(2)).leaveGame(eq(userId), any(UUID.class));
      verify(userRepository).softDelete(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void deleteAccount_withFinishedGameOnly_doesNotLeaveFinishedGame() {
      UUID gameId = UUID.randomUUID();
      Game finishedGame = activeGame(gameId, creatorId, GameStatus.FINISHED);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameDomainRepository.findByCreatorId(userId)).thenReturn(List.of());
      when(gameDomainRepository.findGamesByUserId(userId)).thenReturn(List.of(finishedGame));

      service.deleteAccount(userId);

      verify(gameParticipantService, never()).leaveGame(any(UUID.class), any(UUID.class));
      verify(userRepository).softDelete(eq(userId), any(LocalDateTime.class));
    }
  }

  @Nested
  class EdgeCases {

    @Test
    void deleteAccount_userNotFound_throwsUserNotFoundException() {
      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deleteAccount(userId))
          .isInstanceOf(UserNotFoundException.class);

      verify(userRepository, never()).softDelete(any(), any());
    }

    @Test
    void deleteAccount_alreadySoftDeleted_throwsUserNotFoundException() {
      user.setDeletedAt(LocalDateTime.now().minusDays(1));
      when(userRepository.findById(userId)).thenReturn(Optional.of(user));

      assertThatThrownBy(() -> service.deleteAccount(userId))
          .isInstanceOf(UserNotFoundException.class);

      verify(userRepository, never()).softDelete(any(), any());
    }

    @Test
    void deleteAccount_userIsCreatorOfActiveGame_throwsAccountDeletionBlockedException() {
      UUID gameId = UUID.randomUUID();
      Game creatorGame = activeGame(gameId, userId, GameStatus.CREATING);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameDomainRepository.findByCreatorId(userId)).thenReturn(List.of(creatorGame));

      assertThatThrownBy(() -> service.deleteAccount(userId))
          .isInstanceOf(AccountDeletionBlockedException.class)
          .hasMessageContaining("creator");

      verify(userRepository, never()).softDelete(any(), any());
    }

    @Test
    void deleteAccount_userIsCreatorOfFinishedGameOnly_proceedsWithDeletion() {
      UUID gameId = UUID.randomUUID();
      Game finishedCreatorGame = activeGame(gameId, userId, GameStatus.FINISHED);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameDomainRepository.findByCreatorId(userId)).thenReturn(List.of(finishedCreatorGame));
      when(gameDomainRepository.findGamesByUserId(userId)).thenReturn(List.of());

      service.deleteAccount(userId);

      verify(userRepository).softDelete(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void deleteAccount_activeParticipationNotAsCreator_doesNotCallLeaveForCreatorGames() {
      UUID ownGameId = UUID.randomUUID();
      UUID otherGameId = UUID.randomUUID();
      // user is creator of an ACTIVE game they own → blocked
      // This test verifies that even if findGamesByUserId returns the creator game, it's skipped
      Game ownCreatorGame = activeGame(ownGameId, userId, GameStatus.FINISHED); // finished = OK
      Game participantGame = activeGame(otherGameId, creatorId, GameStatus.ACTIVE);

      when(userRepository.findById(userId)).thenReturn(Optional.of(user));
      when(gameDomainRepository.findByCreatorId(userId)).thenReturn(List.of(ownCreatorGame));
      when(gameDomainRepository.findGamesByUserId(userId))
          .thenReturn(List.of(ownCreatorGame, participantGame));

      service.deleteAccount(userId);

      // Only the non-creator active game triggers leaveGame
      verify(gameParticipantService).leaveGame(userId, otherGameId);
      verify(gameParticipantService, never()).leaveGame(userId, ownGameId);
      verify(userRepository).softDelete(eq(userId), any(LocalDateTime.class));
    }
  }
}
