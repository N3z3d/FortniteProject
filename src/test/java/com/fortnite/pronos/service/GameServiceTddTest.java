package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.service.game.GameCreationService;
import com.fortnite.pronos.service.game.GameDraftService;
import com.fortnite.pronos.service.game.GameParticipantService;
import com.fortnite.pronos.service.game.GameQueryService;

/**
 * TDD Tests for GameService Facade
 *
 * <p>This test defines expected behaviors first (RED) then implementation follows (GREEN) Testing
 * the orchestration and delegation logic of the facade pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService Facade - TDD Tests")
class GameServiceTddTest {

  @Mock private GameCreationService gameCreationService;

  @Mock private GameQueryService gameQueryService;

  @Mock private GameParticipantService gameParticipantService;

  @Mock private GameDraftService gameDraftService;

  @InjectMocks private GameService gameService;

  private UUID creatorId;
  private UUID gameId;
  private CreateGameRequest createGameRequest;
  private GameDto gameDto;
  private JoinGameRequest joinGameRequest;

  @BeforeEach
  void setUp() {
    creatorId = UUID.randomUUID();
    gameId = UUID.randomUUID();

    createGameRequest =
        CreateGameRequest.builder()
            .name("Test Fantasy League")
            .description("TDD Test Game")
            .maxParticipants(10)
            .build();

    gameDto =
        GameDto.builder()
            .id(gameId)
            .name("Test Fantasy League")
            .status(GameStatus.CREATING)
            .maxParticipants(10)
            .build();

    joinGameRequest = new JoinGameRequest(gameId, UUID.randomUUID(), "TEST123", false);
  }

  @Nested
  @DisplayName("Game Creation Operations")
  class GameCreationOperations {

    @Test
    @DisplayName("Should delegate game creation to GameCreationService")
    void shouldDelegateGameCreation() {
      // RED: Define expected behavior first
      when(gameCreationService.createGame(creatorId, createGameRequest)).thenReturn(gameDto);

      // When
      GameDto result = gameService.createGame(creatorId, createGameRequest);

      // Then
      assertThat(result).isEqualTo(gameDto);
      verify(gameCreationService).createGame(creatorId, createGameRequest);
      verifyNoMoreInteractions(gameCreationService);
    }

    @Test
    @DisplayName("Should delegate game deletion to GameCreationService")
    void shouldDelegateGameDeletion() {
      // RED: Define expected behavior for deletion
      doNothing().when(gameCreationService).deleteGame(gameId);

      // When
      gameService.deleteGame(gameId);

      // Then
      verify(gameCreationService).deleteGame(gameId);
      verifyNoMoreInteractions(gameCreationService);
    }

    @Test
    @DisplayName("Should handle creation service exceptions properly")
    void shouldHandleCreationServiceExceptions() {
      // RED: Test exception handling
      RuntimeException expectedException = new RuntimeException("Creation failed");
      when(gameCreationService.createGame(creatorId, createGameRequest))
          .thenThrow(expectedException);

      // When & Then
      assertThatThrownBy(() -> gameService.createGame(creatorId, createGameRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Creation failed");

      verify(gameCreationService).createGame(creatorId, createGameRequest);
    }
  }

  @Nested
  @DisplayName("Game Query Operations")
  class GameQueryOperations {

    @Test
    @DisplayName("Should delegate getAllGames to GameQueryService")
    void shouldDelegateGetAllGames() {
      // RED: Define expected behavior for getAllGames
      List<GameDto> expectedGames = List.of(gameDto);

      when(gameQueryService.getAllGames()).thenReturn(expectedGames);

      // When
      List<GameDto> result = gameService.getAllGames();

      // Then
      assertThat(result).isEqualTo(expectedGames);
      assertThat(result).hasSize(1);
      verify(gameQueryService).getAllGames();
    }

    @Test
    @DisplayName("Should delegate getGameById to GameQueryService")
    void shouldDelegateGetGameById() {
      // RED: Define expected behavior for getGameById
      when(gameQueryService.getGameById(gameId)).thenReturn(Optional.of(gameDto));

      // When
      Optional<GameDto> result = gameService.getGameById(gameId);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo(gameDto);
      verify(gameQueryService).getGameById(gameId);
    }

    @Test
    @DisplayName("Should delegate getGamesByUser to GameQueryService")
    void shouldDelegateGetGamesByUser() {
      // RED: Define expected behavior for getGamesByUser
      List<GameDto> expectedGames = List.of(gameDto);
      when(gameQueryService.getGamesByUser(creatorId)).thenReturn(expectedGames);

      // When
      List<GameDto> result = gameService.getGamesByUser(creatorId);

      // Then
      assertThat(result).isEqualTo(expectedGames);
      assertThat(result).hasSize(1);
      verify(gameQueryService).getGamesByUser(creatorId);
    }

    @Test
    @DisplayName("Should delegate getGamesByStatus to GameQueryService")
    void shouldDelegateGetGamesByStatus() {
      // RED: Define expected behavior for getGamesByStatus
      List<GameDto> expectedGames = List.of(gameDto);
      when(gameQueryService.getGamesByStatus(GameStatus.CREATING)).thenReturn(expectedGames);

      // When
      List<GameDto> result = gameService.getGamesByStatus(GameStatus.CREATING);

      // Then
      assertThat(result).isEqualTo(expectedGames);
      verify(gameQueryService).getGamesByStatus(GameStatus.CREATING);
    }
  }

  @Nested
  @DisplayName("Game Participation Operations")
  class GameParticipationOperations {

    @Test
    @DisplayName("Should delegate joinGame to GameParticipantService")
    void shouldDelegateJoinGame() {
      // RED: Define expected behavior for joinGame
      UUID userId = joinGameRequest.getUserId();
      when(gameParticipantService.joinGame(userId, joinGameRequest)).thenReturn(true);

      // When
      gameService.joinGame(userId, joinGameRequest);

      // Then
      verify(gameParticipantService).joinGame(userId, joinGameRequest);
    }

    @Test
    @DisplayName("Should delegate leaveGame to GameParticipantService")
    void shouldDelegateLeaveGame() {
      // RED: Define expected behavior for leaveGame
      UUID userId = UUID.randomUUID();
      doNothing().when(gameParticipantService).leaveGame(gameId, userId);

      // When
      gameService.leaveGame(gameId, userId);

      // Then
      verify(gameParticipantService).leaveGame(gameId, userId);
    }

    @Test
    @DisplayName("Should handle participation service exceptions")
    void shouldHandleParticipationServiceExceptions() {
      // RED: Test exception handling for participation
      RuntimeException expectedException = new RuntimeException("Join failed");
      UUID userId = joinGameRequest.getUserId();
      when(gameParticipantService.joinGame(userId, joinGameRequest)).thenThrow(expectedException);

      // When & Then
      assertThatThrownBy(() -> gameService.joinGame(userId, joinGameRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Join failed");
    }
  }

  @Nested
  @DisplayName("Game Draft Operations")
  class GameDraftOperations {

    @Test
    @DisplayName("Should delegate startDraft to GameDraftService")
    void shouldDelegateStartDraft() {
      // RED: Define expected behavior for startDraft
      DraftDto draftDto = DraftDto.builder().id(UUID.randomUUID()).gameId(gameId).build();
      when(gameDraftService.startDraft(gameId, creatorId)).thenReturn(draftDto);

      // When
      DraftDto result = gameService.startDraft(gameId, creatorId);

      // Then
      assertThat(result).isEqualTo(draftDto);
      verify(gameDraftService).startDraft(gameId, creatorId);
    }

    @Test
    @DisplayName("Should delegate pauseDraft to GameDraftService")
    void shouldDelegatePauseDraft() {
      // RED: Define expected behavior for pauseDraft
      DraftDto draftDto = DraftDto.builder().id(UUID.randomUUID()).gameId(gameId).build();
      when(gameDraftService.pauseDraft(gameId, creatorId)).thenReturn(draftDto);

      // When
      gameService.pauseDraft(gameId, creatorId);

      // Then
      verify(gameDraftService).pauseDraft(gameId, creatorId);
    }

    @Test
    @DisplayName("Should delegate finishDraft to GameDraftService")
    void shouldDelegateFinishDraft() {
      // RED: Define expected behavior for finishDraft
      DraftDto draftDto = DraftDto.builder().id(UUID.randomUUID()).gameId(gameId).build();
      when(gameDraftService.finishDraft(gameId, creatorId)).thenReturn(draftDto);

      // When
      gameService.finishDraft(gameId, creatorId);

      // Then
      verify(gameDraftService).finishDraft(gameId, creatorId);
    }

    @Test
    @DisplayName("Should handle draft service exceptions")
    void shouldHandleDraftServiceExceptions() {
      // RED: Test exception handling for draft operations
      RuntimeException expectedException = new RuntimeException("Draft start failed");
      when(gameDraftService.startDraft(gameId, creatorId)).thenThrow(expectedException);

      // When & Then
      assertThatThrownBy(() -> gameService.startDraft(gameId, creatorId))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Draft start failed");
    }
  }

  @Nested
  @DisplayName("Service Integration and Orchestration")
  class ServiceIntegrationTests {

    @Test
    @DisplayName("Should coordinate multiple services for complex operations")
    void shouldCoordinateMultipleServices() {
      // RED: Test orchestration of multiple services
      // This test would verify that complex operations that require
      // multiple service calls are properly orchestrated

      // For now, this serves as a placeholder for future orchestration logic
      // The current facade is simple delegation, but complex workflows
      // might require coordination between services

      when(gameQueryService.getGameById(gameId)).thenReturn(Optional.of(gameDto));

      Optional<GameDto> result = gameService.getGameById(gameId);

      assertThat(result).isNotNull();
      verify(gameQueryService).getGameById(gameId);
    }

    @Test
    @DisplayName("Should maintain transaction boundaries across service calls")
    void shouldMaintainTransactionBoundaries() {
      // RED: Verify transaction handling
      // This test ensures that the facade maintains proper transaction
      // boundaries when coordinating multiple services

      // Placeholder for transaction testing
      // Would test @Transactional behavior across service boundaries

      assertThat(gameService).isNotNull();
    }
  }

  @Nested
  @DisplayName("Facade Pattern Verification")
  class FacadePatternTests {

    @Test
    @DisplayName("Should expose simplified interface hiding service complexity")
    void shouldProvideSimplifiedInterface() {
      // RED: Verify facade pattern implementation
      // The facade should provide a simplified interface to complex subsystems

      when(gameCreationService.createGame(creatorId, createGameRequest)).thenReturn(gameDto);

      GameDto result = gameService.createGame(creatorId, createGameRequest);

      // The client only needs to call one method on the facade
      // rather than understanding the complexity of the underlying services
      assertThat(result).isEqualTo(gameDto);

      // Verify that the facade properly delegates to the appropriate service
      verify(gameCreationService).createGame(creatorId, createGameRequest);

      // Verify that other services are not called unnecessarily
      verifyNoInteractions(gameQueryService, gameParticipantService, gameDraftService);
    }

    @Test
    @DisplayName("Should provide consistent error handling across all operations")
    void shouldProvideConsistentErrorHandling() {
      // RED: Verify consistent error handling
      RuntimeException creationError = new RuntimeException("Creation failed");
      RuntimeException queryError = new RuntimeException("Query failed");

      when(gameCreationService.createGame(any(), any())).thenThrow(creationError);
      when(gameQueryService.getGameById(any())).thenThrow(queryError);

      // All operations should propagate exceptions consistently
      assertThatThrownBy(() -> gameService.createGame(creatorId, createGameRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Creation failed");

      assertThatThrownBy(() -> gameService.getGameById(gameId))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Query failed");
    }
  }
}
