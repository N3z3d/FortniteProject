package com.fortnite.pronos.service.game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.GameQueryUseCase;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.model.Pagination;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.GameDto;
import com.fortnite.pronos.dto.mapper.GameDtoMapper;
import com.fortnite.pronos.exception.GameNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service responsible for game query operations. Handles reading and searching games. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings({"java:S3864"})
public class GameQueryService implements GameQueryUseCase {

  private final GameDomainRepositoryPort gameRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final UserRepositoryPort userRepository;

  /** Gets all games with optimized fetch strategy */
  @Override
  public List<GameDto> getAllGames() {
    log.debug("Retrieving all games with optimized fetch strategy");
    long playerCount = playerRepository.count();
    return gameRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(this::toDtoWithCreator)
        .peek(dto -> dto.setFortnitePlayerCount(playerCount))
        .toList();
  }

  /** Gets games with available slots */
  @Override
  public List<GameDto> getAvailableGames() {
    log.debug("Retrieving games with available slots");
    long playerCount = playerRepository.count();
    return gameRepository.findGamesWithAvailableSlots().stream()
        .map(this::toDtoWithCreator)
        .peek(dto -> dto.setFortnitePlayerCount(playerCount))
        .toList();
  }

  /** Gets games by user ID */
  @Override
  public List<GameDto> getGamesByUser(UUID userId) {
    log.debug("Retrieving games for user {}", userId);
    long playerCount = playerRepository.count();
    return gameRepository.findGamesByUserId(userId).stream()
        .map(this::toDtoWithCreator)
        .peek(dto -> dto.setFortnitePlayerCount(playerCount))
        .toList();
  }

  /** Gets a game by ID with full details */
  @Override
  public Optional<GameDto> getGameById(UUID gameId) {
    log.debug("Retrieving game {} with full details fetch strategy", gameId);
    return gameRepository
        .findById(gameId)
        .map(this::toDtoWithCreator)
        .map(this::enrichWithPlayerCount);
  }

  /** Gets a game by ID or throws exception */
  @Override
  public GameDto getGameByIdOrThrow(UUID gameId) {
    return getGameById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));
  }

  /** Gets a game by invitation code */
  @Override
  public Optional<GameDto> getGameByInvitationCode(String invitationCode) {
    log.debug("Retrieving game by invitation code");
    return gameRepository.findByInvitationCode(invitationCode).map(this::toDtoWithCreator);
  }

  /** Gets games by status */
  @Override
  public List<GameDto> getGamesByStatus(GameStatus status) {
    log.debug("Retrieving games with status {}", status);
    return gameRepository.findByStatus(status).stream().map(this::toDtoWithCreator).toList();
  }

  /** Gets active games (games that are not finished) */
  @Override
  public List<GameDto> getActiveGames() {
    log.debug("Retrieving active games");
    return gameRepository.findByStatusNot(GameStatus.FINISHED).stream()
        .map(this::toDtoWithCreator)
        .toList();
  }

  /** Gets games with pagination */
  public List<GameDto> getGamesWithPagination(Pageable pageable) {
    log.debug("Retrieving games with pagination");
    Pagination pagination =
        Pagination.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().iterator().hasNext()
                ? pageable.getSort().iterator().next().getProperty()
                : "createdAt",
            pageable.getSort().iterator().hasNext()
                ? pageable.getSort().iterator().next().getDirection().name()
                : "DESC");
    return gameRepository.findAllGames(pagination).stream().map(this::toDtoWithCreator).toList();
  }

  /** Searches games by name */
  @Override
  public List<GameDto> searchGamesByName(String name) {
    log.debug("Searching games by name: {}", name);
    return gameRepository.findByNameContainingIgnoreCase(name).stream()
        .map(this::toDtoWithCreator)
        .toList();
  }

  /** Gets games created by user */
  @Override
  public List<GameDto> getGamesCreatedByUser(UUID userId) {
    log.debug("Retrieving games created by user {}", userId);
    return gameRepository.findByCreatorId(userId).stream().map(this::toDtoWithCreator).toList();
  }

  /** Checks if game exists */
  @Override
  public boolean gameExists(UUID gameId) {
    return gameRepository.existsById(gameId);
  }

  /** Checks if game exists by invitation code */
  @Override
  public boolean gameExistsByInvitationCode(String invitationCode) {
    return gameRepository.existsByInvitationCode(invitationCode);
  }

  /** Gets game count */
  @Override
  public long getGameCount() {
    return gameRepository.count();
  }

  /** Gets game count by status */
  @Override
  public long getGameCountByStatus(GameStatus status) {
    return gameRepository.countByStatus(status);
  }

  /** Gets games by season */
  @Override
  public List<GameDto> getGamesBySeason(Integer season) {
    log.debug("Retrieving games for season {}", season);
    long playerCount = playerRepository.count();
    return gameRepository.findByCurrentSeasonWithFetch(season).stream()
        .map(this::toDtoWithCreator)
        .peek(dto -> dto.setFortnitePlayerCount(playerCount))
        .toList();
  }

  /** Gets the current season (based on current year) */
  @Override
  public Integer getCurrentSeason() {
    return java.time.Year.now().getValue();
  }

  private GameDto enrichWithPlayerCount(GameDto dto) {
    dto.setFortnitePlayerCount(playerRepository.count());
    return dto;
  }

  private GameDto toDtoWithCreator(com.fortnite.pronos.domain.game.model.Game game) {
    GameDto dto = GameDtoMapper.fromDomainGame(game);
    enrichCreatorIdentity(dto);
    return dto;
  }

  private void enrichCreatorIdentity(GameDto dto) {
    if (dto == null) {
      return;
    }

    if (hasText(dto.getCreatorUsername())) {
      if (!hasText(dto.getCreatorName())) {
        dto.setCreatorName(dto.getCreatorUsername());
      }
      return;
    }

    if (hasText(dto.getCreatorName())) {
      dto.setCreatorUsername(dto.getCreatorName());
      return;
    }

    UUID creatorId = dto.getCreatorId();
    if (creatorId == null) {
      return;
    }

    userRepository
        .findById(creatorId)
        .map(com.fortnite.pronos.model.User::getUsername)
        .filter(this::hasText)
        .ifPresent(
            username -> {
              dto.setCreatorUsername(username);
              dto.setCreatorName(username);
            });
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
