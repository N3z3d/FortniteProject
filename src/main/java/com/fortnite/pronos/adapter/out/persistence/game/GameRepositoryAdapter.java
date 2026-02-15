package com.fortnite.pronos.adapter.out.persistence.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.model.Pagination;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Persistence adapter for the Game domain migration.
 *
 * <p>Implements a dedicated domain port without modifying the legacy {@code GameRepositoryPort}.
 */
@Component
public class GameRepositoryAdapter implements GameDomainRepositoryPort {

  private final GameRepository gameRepository;
  private final UserRepository userRepository;
  private final GameEntityMapper mapper;

  public GameRepositoryAdapter(
      GameRepository gameRepository, UserRepository userRepository, GameEntityMapper mapper) {
    this.gameRepository = gameRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Game> findById(UUID id) {
    return gameCrudRepository().findById(id).map(mapper::toDomain);
  }

  @Override
  public Game save(Game game) {
    Objects.requireNonNull(game, "Game cannot be null");
    User creator = findRequiredCreator(game.getCreatorId());
    Map<UUID, User> participantUsersById = resolveParticipantUsersById(game.getParticipants());
    com.fortnite.pronos.model.Game entity = mapper.toEntity(game, creator, participantUsersById);
    com.fortnite.pronos.model.Game savedEntity = gameCrudRepository().save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public Optional<Game> findByInvitationCode(String invitationCode) {
    return gameRepository.findByInvitationCode(invitationCode).map(mapper::toDomain);
  }

  @Override
  public boolean existsById(UUID id) {
    return gameCrudRepository().existsById(id);
  }

  @Override
  public long countByCreatorAndStatusIn(UUID creatorId, List<GameStatus> statuses) {
    if (creatorId == null || statuses == null || statuses.isEmpty()) {
      return 0L;
    }
    Optional<User> creator = userCrudRepository().findById(creatorId);
    if (creator.isEmpty()) {
      return 0L;
    }
    List<com.fortnite.pronos.model.GameStatus> entityStatuses =
        statuses.stream().filter(Objects::nonNull).map(this::toEntityStatus).toList();
    if (entityStatuses.isEmpty()) {
      return 0L;
    }
    return gameRepository.countByCreatorAndStatusInAndDeletedAtIsNull(
        creator.orElseThrow(), entityStatuses);
  }

  @Override
  public List<Game> findByStatus(GameStatus status) {
    if (status == null) {
      return Collections.emptyList();
    }
    return mapper.toDomainList(gameRepository.findByStatus(toEntityStatus(status)));
  }

  @Override
  public List<Game> findByCreatorId(UUID creatorId) {
    if (creatorId == null) {
      return Collections.emptyList();
    }
    return mapper.toDomainList(gameRepository.findByCreatorId(creatorId));
  }

  @Override
  public List<Game> findAllByOrderByCreatedAtDesc() {
    return mapper.toDomainList(gameRepository.findAllByOrderByCreatedAtDesc());
  }

  @Override
  public List<Game> findGamesWithAvailableSlots() {
    return mapper.toDomainList(gameRepository.findGamesWithAvailableSlots());
  }

  @Override
  public List<Game> findGamesByUserId(UUID userId) {
    if (userId == null) {
      return Collections.emptyList();
    }
    return mapper.toDomainList(gameRepository.findGamesByUserId(userId));
  }

  @Override
  public List<Game> findByStatusNot(GameStatus status) {
    if (status == null) {
      return Collections.emptyList();
    }
    return mapper.toDomainList(gameRepository.findByStatusNot(toEntityStatus(status)));
  }

  @Override
  public List<Game> findAllGames(Pagination pagination) {
    Objects.requireNonNull(pagination, "Pagination cannot be null");
    PageRequest pageRequest =
        PageRequest.of(
            pagination.getPage(),
            pagination.getSize(),
            Sort.by(
                Sort.Direction.fromString(pagination.getSortDirection()), pagination.getSortBy()));
    return mapper.toDomainList(gameRepository.findAllWithFetchPaginated(pageRequest).getContent());
  }

  @Override
  public List<Game> findByNameContainingIgnoreCase(String name) {
    if (name == null) {
      return Collections.emptyList();
    }
    return mapper.toDomainList(gameRepository.findByNameContainingIgnoreCase(name));
  }

  @Override
  public boolean existsByInvitationCode(String code) {
    if (code == null) {
      return false;
    }
    return gameRepository.existsByInvitationCode(code);
  }

  @Override
  public long count() {
    return gameRepository.count();
  }

  @Override
  public long countByStatus(GameStatus status) {
    if (status == null) {
      return 0L;
    }
    return gameRepository.countByStatus(toEntityStatus(status));
  }

  @Override
  public List<Game> findByCurrentSeasonWithFetch(Integer season) {
    if (season == null) {
      return Collections.emptyList();
    }
    return mapper.toDomainList(gameRepository.findByCurrentSeasonWithFetch(season));
  }

  private User findRequiredCreator(UUID creatorId) {
    if (creatorId == null) {
      throw new IllegalArgumentException("Creator not found: null");
    }
    return userCrudRepository()
        .findById(creatorId)
        .orElseThrow(() -> new IllegalArgumentException("Creator not found: " + creatorId));
  }

  private Map<UUID, User> resolveParticipantUsersById(List<GameParticipant> participants) {
    if (participants == null || participants.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<UUID, User> usersById = new HashMap<>();
    for (GameParticipant participant : participants) {
      if (participant == null || participant.getUserId() == null) {
        continue;
      }
      User user =
          userCrudRepository()
              .findById(participant.getUserId())
              .orElseGet(
                  () -> createUserReference(participant.getUserId(), participant.getUsername()));
      usersById.put(participant.getUserId(), user);
    }
    return usersById;
  }

  private User createUserReference(UUID userId, String username) {
    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    return user;
  }

  private com.fortnite.pronos.model.GameStatus toEntityStatus(GameStatus status) {
    return com.fortnite.pronos.model.GameStatus.valueOf(status.name());
  }

  private CrudRepository<com.fortnite.pronos.model.Game, UUID> gameCrudRepository() {
    return gameRepository;
  }

  private CrudRepository<User, UUID> userCrudRepository() {
    return userRepository;
  }
}
