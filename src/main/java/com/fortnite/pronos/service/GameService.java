package com.fortnite.pronos.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.TeamDomainRepositoryPort;
import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.service.game.GameCreationService;
import com.fortnite.pronos.service.game.GameDraftService;
import com.fortnite.pronos.service.game.GameParticipantService;
import com.fortnite.pronos.service.game.GameQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Refactored Game Service using Facade Pattern Orchestrates multiple smaller, focused services
 * following Single Responsibility Principle Replaces the monolithic GameService (905 lines) with
 * composed services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

  private final GameCreationService gameCreationService;
  private final GameQueryService gameQueryService;
  private final GameParticipantService gameParticipantService;
  private final GameDraftService gameDraftService;
  private final TeamDomainRepositoryPort teamDomainRepository;
  private final GameRealtimeEventService gameRealtimeEventService;

  // Game Creation Operations

  /** Creates a new game */
  public GameDto createGame(UUID creatorId, CreateGameRequest request) {
    return gameCreationService.createGame(creatorId, request);
  }

  /** Deletes a game */
  public void deleteGame(UUID gameId) {
    Set<UUID> participants = getParticipantIdsOrEmpty(gameId);
    gameCreationService.deleteGame(gameId);
    publishRealtimeEventSafely(participants, GameRealtimeEventService.GAME_DELETED, gameId);
  }

  /** Regenerates the invitation code for a game (permanent by default) */
  public GameDto regenerateInvitationCode(UUID gameId) {
    GameDto updatedGame = gameCreationService.regenerateInvitationCode(gameId);
    publishGameUpdate(gameId);
    return updatedGame;
  }

  /** Regenerates the invitation code for a game with configurable duration */
  public GameDto regenerateInvitationCode(UUID gameId, String duration) {
    GameDto updatedGame = gameCreationService.regenerateInvitationCode(gameId, duration);
    publishGameUpdate(gameId);
    return updatedGame;
  }

  /** Deletes the invitation code for a game. */
  public GameDto deleteInvitationCode(UUID gameId) {
    GameDto updatedGame = gameCreationService.deleteInvitationCode(gameId);
    publishGameUpdate(gameId);
    return updatedGame;
  }

  /** Renames a game */
  public GameDto renameGame(UUID gameId, String newName) {
    GameDto updatedGame = gameCreationService.renameGame(gameId, newName);
    publishGameUpdate(gameId);
    return updatedGame;
  }

  /** Configures the competition period for a game */
  public GameDto configureCompetitionPeriod(UUID gameId, LocalDate startDate, LocalDate endDate) {
    GameDto updatedGame =
        gameCreationService.configureCompetitionPeriod(gameId, startDate, endDate);
    publishGameUpdate(gameId);
    return updatedGame;
  }

  // Game Query Operations

  /** Gets all games */
  public List<GameDto> getAllGames() {
    return gameQueryService.getAllGames();
  }

  /** Gets games with available slots */
  public List<GameDto> getAvailableGames() {
    return gameQueryService.getAvailableGames();
  }

  /** Gets games by user ID */
  public List<GameDto> getGamesByUser(UUID userId) {
    return gameQueryService.getGamesByUser(userId);
  }

  /** Gets a game by ID */
  public Optional<GameDto> getGameById(UUID gameId) {
    return gameQueryService.getGameById(gameId);
  }

  /** Gets a game by ID or throws exception */
  public GameDto getGameByIdOrThrow(UUID gameId) {
    return gameQueryService.getGameByIdOrThrow(gameId);
  }

  /** Gets a game by invitation code */
  public Optional<GameDto> getGameByInvitationCode(String invitationCode) {
    return gameQueryService.getGameByInvitationCode(invitationCode);
  }

  /** Gets games by status */
  public List<GameDto> getGamesByStatus(GameStatus status) {
    return gameQueryService.getGamesByStatus(status);
  }

  /** Gets active games */
  public List<GameDto> getActiveGames() {
    return gameQueryService.getActiveGames();
  }

  /** Gets games with pagination */
  public List<GameDto> getGamesWithPagination(Pageable pageable) {
    return gameQueryService.getGamesWithPagination(pageable);
  }

  /** Searches games by name */
  public List<GameDto> searchGamesByName(String name) {
    return gameQueryService.searchGamesByName(name);
  }

  /** Gets games created by user */
  public List<GameDto> getGamesCreatedByUser(UUID userId) {
    return gameQueryService.getGamesCreatedByUser(userId);
  }

  /** Checks if game exists */
  public boolean gameExists(UUID gameId) {
    return gameQueryService.gameExists(gameId);
  }

  /** Gets game count */
  public long getGameCount() {
    return gameQueryService.getGameCount();
  }

  // Game Participant Operations

  /** Adds a user to a game */
  public boolean joinGame(UUID userId, JoinGameRequest request) {
    boolean joined = gameParticipantService.joinGame(userId, request);
    if (joined) {
      publishParticipantEvent(request.getGameId(), userId, GameRealtimeEventService.GAME_JOINED);
    }
    return joined;
  }

  /** Removes a user from a game */
  public void leaveGame(UUID userId, UUID gameId) {
    gameParticipantService.leaveGame(userId, gameId);
    publishParticipantEvent(gameId, userId, GameRealtimeEventService.GAME_LEFT);
  }

  /** Checks if user is participant in game */
  public boolean isUserParticipant(UUID userId, UUID gameId) {
    return gameParticipantService.isUserParticipant(userId, gameId);
  }

  /** Gets participant count for game */
  public long getParticipantCount(UUID gameId) {
    return gameParticipantService.getParticipantCount(gameId);
  }

  // Game Draft Operations

  /** Starts draft for a game */
  public DraftDto startDraft(UUID gameId, UUID creatorId) {
    DraftDto draft = gameDraftService.startDraft(gameId, creatorId);
    publishParticipantEvent(gameId, creatorId, GameRealtimeEventService.GAME_UPDATED);
    return draft;
  }

  /** Pauses draft for a game */
  public void pauseDraft(UUID gameId, UUID userId) {
    gameDraftService.pauseDraft(gameId, userId);
  }

  /** Resumes draft for a game */
  public void resumeDraft(UUID gameId, UUID userId) {
    gameDraftService.resumeDraft(gameId, userId);
  }

  /** Finishes draft for a game */
  public void finishDraft(UUID gameId, UUID userId) {
    gameDraftService.finishDraft(gameId, userId);
  }

  /** Selects a player in the draft */
  public DraftPickDto selectPlayer(UUID gameId, UUID userId, UUID playerId) {
    return gameDraftService.selectPlayer(gameId, userId, playerId);
  }

  /** Gets draft for a game */
  public DraftDto getDraftByGame(UUID gameId) {
    return gameDraftService.getDraftByGame(gameId);
  }

  /** Gets draft picks for a game */
  public List<DraftPickDto> getDraftPicks(UUID gameId) {
    return gameDraftService.getDraftPicks(gameId);
  }

  // Team Operations

  /** Gets all teams for a specific game */
  public List<com.fortnite.pronos.domain.team.model.Team> getGameTeams(UUID gameId) {
    log.debug("Getting teams for game: {}", gameId);

    // VÃ©rifier que le jeu existe
    gameQueryService.getGameByIdOrThrow(gameId);

    // RÃ©cupÃ©rer les Ã©quipes du jeu avec fetch optimisÃ©
    return teamDomainRepository.findByGameIdWithFetch(gameId);
  }

  private void publishGameUpdate(UUID gameId) {
    publishRealtimeEventSafely(
        getParticipantIdsOrEmpty(gameId), GameRealtimeEventService.GAME_UPDATED, gameId);
  }

  private void publishParticipantEvent(UUID gameId, UUID actorUserId, String eventType) {
    Set<UUID> recipients = new HashSet<>(getParticipantIdsOrEmpty(gameId));
    recipients.add(actorUserId);
    publishRealtimeEventSafely(recipients, eventType, gameId);
  }

  private void publishRealtimeEventSafely(Set<UUID> recipients, String eventType, UUID gameId) {
    try {
      gameRealtimeEventService.publishToUsers(recipients, eventType, gameId);
    } catch (RuntimeException exception) {
      log.debug(
          "GameService: realtime event ignored - gameId={}, eventType={}, reason={}",
          gameId,
          eventType,
          exception.getMessage());
    }
  }

  private Set<UUID> getParticipantIdsOrEmpty(UUID gameId) {
    try {
      GameDto game = gameQueryService.getGameByIdOrThrow(gameId);
      if (game == null || game.getParticipants() == null) {
        return Set.of();
      }
      return new HashSet<>(game.getParticipants().keySet());
    } catch (Exception exception) {
      log.debug(
          "GameService: unable to resolve participants for realtime event - gameId={}, reason={}",
          gameId,
          exception.getMessage());
      return Set.of();
    }
  }
}
