package com.fortnite.pronos.service.game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.repository.GameRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service responsible for game query operations Handles reading and searching games */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameQueryService {

  private final GameRepository gameRepository;

  /** Gets all games - PHASE 1B: OPTIMIZED with EntityGraph to prevent N+1 */
  @Cacheable("games")
  public List<GameDto> getAllGames() {
    log.debug("Retrieving all games with optimized fetch strategy");
    return gameRepository
        .findAllByOrderByCreatedAtDesc() // Uses EntityGraph for N+1 prevention
        .stream()
        .map(GameDto::fromGame)
        .collect(Collectors.toList());
  }

  /** Gets games by user ID */
  public List<GameDto> getGamesByUser(UUID userId) {
    log.debug("Retrieving games for user {}", userId);
    return gameRepository.findGamesByUserId(userId).stream()
        .map(GameDto::fromGame)
        .collect(Collectors.toList());
  }

  /** Gets a game by ID - PHASE 1B: OPTIMIZED with full details EntityGraph */
  public Optional<GameDto> getGameById(UUID gameId) {
    log.debug("Retrieving game {} with full details fetch strategy", gameId);
    return gameRepository
        .findWithFullDetailsById(gameId) // Uses full EntityGraph
        .map(GameDto::fromGame);
  }

  /** Gets a game by ID or throws exception */
  public GameDto getGameByIdOrThrow(UUID gameId) {
    return getGameById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  /** Gets game entity by ID or throws exception */
  public Game getGameEntityById(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  /** Gets a game by invitation code */
  public Optional<GameDto> getGameByInvitationCode(String invitationCode) {
    log.debug("Retrieving game by invitation code");
    return gameRepository.findByInvitationCode(invitationCode).map(GameDto::fromGame);
  }

  /** Gets games by status */
  public List<GameDto> getGamesByStatus(GameStatus status) {
    log.debug("Retrieving games with status {}", status);
    return gameRepository.findByStatus(status).stream()
        .map(GameDto::fromGame)
        .collect(Collectors.toList());
  }

  /** Gets active games (games that are not finished) */
  @Cacheable("activeGames")
  public List<GameDto> getActiveGames() {
    log.debug("Retrieving active games");
    return gameRepository.findByStatusNot(GameStatus.FINISHED).stream()
        .map(GameDto::fromGame)
        .collect(Collectors.toList());
  }

  /** Gets games with pagination */
  public Page<GameDto> getGamesWithPagination(Pageable pageable) {
    log.debug("Retrieving games with pagination");
    return gameRepository.findAll(pageable).map(GameDto::fromGame);
  }

  /** Searches games by name */
  public List<GameDto> searchGamesByName(String name) {
    log.debug("Searching games by name: {}", name);
    return gameRepository.findByNameContainingIgnoreCase(name).stream()
        .map(GameDto::fromGame)
        .collect(Collectors.toList());
  }

  /** Gets games created by user */
  public List<GameDto> getGamesCreatedByUser(UUID userId) {
    log.debug("Retrieving games created by user {}", userId);
    return gameRepository.findByCreatorId(userId).stream()
        .map(GameDto::fromGame)
        .collect(Collectors.toList());
  }

  /** Checks if game exists */
  public boolean gameExists(UUID gameId) {
    return gameRepository.existsById(gameId);
  }

  /** Checks if game exists by invitation code */
  public boolean gameExistsByInvitationCode(String invitationCode) {
    return gameRepository.existsByInvitationCode(invitationCode);
  }

  /** Gets game count */
  @Cacheable("gameCount")
  public long getGameCount() {
    return gameRepository.count();
  }

  /** @deprecated Games publiques supprimées - utilisez getGamesByUser() */
  @Deprecated
  public List<GameDto> getAvailableGames() {
    log.debug("Endpoint déprécié - plus de games publiques");
    return List.of();
  }

  /** Gets game count by status */
  public long getGameCountByStatus(GameStatus status) {
    return gameRepository.countByStatus(status);
  }
}
