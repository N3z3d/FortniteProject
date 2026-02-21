package com.fortnite.pronos.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.service.game.GameCreationService;
import com.fortnite.pronos.service.game.GameDraftService;
import com.fortnite.pronos.service.game.GameParticipantService;
import com.fortnite.pronos.service.game.GameQueryService;

@ExtendWith(MockitoExtension.class)
class GameServiceRealtimeEventTest {

  @Mock private GameCreationService gameCreationService;

  @Mock private GameQueryService gameQueryService;

  @Mock private GameParticipantService gameParticipantService;

  @Mock private GameDraftService gameDraftService;

  @Mock private TeamRepository teamRepository;

  @Mock private GameRealtimeEventService gameRealtimeEventService;

  @InjectMocks private GameService gameService;

  private UUID gameId;
  private UUID creatorId;
  private UUID participantId;
  private JoinGameRequest joinGameRequest;

  @BeforeEach
  void setUp() {
    gameId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    participantId = UUID.randomUUID();

    joinGameRequest = new JoinGameRequest();
    joinGameRequest.setGameId(gameId);
    joinGameRequest.setUserId(participantId);
    joinGameRequest.setInvitationCode("JOIN123");
  }

  @Test
  void shouldPublishGameJoinedEventAfterSuccessfulJoin() {
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setParticipants(Map.of(creatorId, "creator", participantId, "participant"));
    when(gameParticipantService.joinGame(participantId, joinGameRequest)).thenReturn(true);
    when(gameQueryService.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    gameService.joinGame(participantId, joinGameRequest);

    verify(gameRealtimeEventService)
        .publishToUsers(
            Set.of(creatorId, participantId), GameRealtimeEventService.GAME_JOINED, gameId);
  }

  @Test
  void shouldNotPublishJoinEventWhenJoinFails() {
    when(gameParticipantService.joinGame(participantId, joinGameRequest)).thenReturn(false);

    gameService.joinGame(participantId, joinGameRequest);

    verifyNoInteractions(gameRealtimeEventService);
  }

  @Test
  void shouldPublishGameDeletedEventUsingParticipantsBeforeDelete() {
    GameDto gameDto = new GameDto();
    gameDto.setId(gameId);
    gameDto.setParticipants(Map.of(creatorId, "creator", participantId, "participant"));
    when(gameQueryService.getGameByIdOrThrow(gameId)).thenReturn(gameDto);

    gameService.deleteGame(gameId);

    verify(gameCreationService).deleteGame(gameId);
    verify(gameRealtimeEventService)
        .publishToUsers(
            Set.of(creatorId, participantId), GameRealtimeEventService.GAME_DELETED, gameId);
  }

  @Test
  void shouldPublishGameLeftEventToRemainingParticipantsAndLeaver() {
    GameDto gameDtoAfterLeave = new GameDto();
    gameDtoAfterLeave.setId(gameId);
    gameDtoAfterLeave.setParticipants(Map.of(creatorId, "creator"));
    when(gameQueryService.getGameByIdOrThrow(gameId)).thenReturn(gameDtoAfterLeave);

    gameService.leaveGame(participantId, gameId);

    verify(gameParticipantService).leaveGame(participantId, gameId);
    verify(gameRealtimeEventService)
        .publishToUsers(
            Set.of(creatorId, participantId), GameRealtimeEventService.GAME_LEFT, gameId);
    verify(gameQueryService, times(1)).getGameByIdOrThrow(gameId);
  }
}
