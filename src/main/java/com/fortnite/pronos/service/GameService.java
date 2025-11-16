package com.fortnite.pronos.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.dto.DraftDto;
import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.JoinGameRequest;
import com.fortnite.pronos.model.GameStatus;
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

  // Game Creation Operations

  /** Creates a new game */
  public GameDto createGame(UUID creatorId, CreateGameRequest request) {
    return gameCreationService.createGame(creatorId, request);
  }

  /** Deletes a game */
  public void deleteGame(UUID gameId) {
    gameCreationService.deleteGame(gameId);
  }

  // Game Query Operations

  /** Gets all games */
  public List<GameDto> getAllGames() {
    return gameQueryService.getAllGames();
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

  /** @deprecated Games publiques supprimées - utilisez getGamesByUser() */
  @Deprecated
  public List<GameDto> getAvailableGames() {
    return List.of(); // Plus de games publiques
  }

  /** Gets games with pagination */
  public Page<GameDto> getGamesWithPagination(Pageable pageable) {
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
  public void joinGame(UUID userId, JoinGameRequest request) {
    gameParticipantService.joinGame(userId, request);
  }

  /** Removes a user from a game */
  public void leaveGame(UUID userId, UUID gameId) {
    gameParticipantService.leaveGame(userId, gameId);
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
    return gameDraftService.startDraft(gameId, creatorId);
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
}
