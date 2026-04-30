package com.fortnite.pronos.adapter.out.persistence.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.adapter.out.persistence.support.EntityReferenceFactory;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.model.Pagination;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
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
  private final GameParticipantRepository participantRepository;

  @PersistenceContext private EntityManager entityManager;

  public GameRepositoryAdapter(
      GameRepository gameRepository,
      UserRepository userRepository,
      GameEntityMapper mapper,
      GameParticipantRepository participantRepository) {
    this.gameRepository = gameRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
    this.participantRepository = participantRepository;
  }

  @Override
  public Optional<Game> findById(UUID id) {
    return gameCrudRepository().findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Game> findByIdForUpdate(UUID id) {
    return gameRepository.findByIdForUpdate(id).map(mapper::toDomain);
  }

  @Override
  public Game save(Game game) {
    Objects.requireNonNull(game, "Game cannot be null");
    User creator = findRequiredCreator(game.getCreatorId());
    Map<UUID, User> participantUsersById = resolveParticipantUsersById(game.getParticipants());
    com.fortnite.pronos.model.Game entity = buildOrLoadEntity(game, creator, participantUsersById);
    com.fortnite.pronos.model.Game savedEntity = gameCrudRepository().save(entity);
    return mapper.toDomain(savedEntity);
  }

  /**
   * Returns a managed JPA entity for the given domain game. If the game already exists in the
   * database, the existing entity is loaded (so its participants are already in the Hibernate
   * session), and only scalar fields are updated. New participants are appended to the managed
   * collection. This avoids Hibernate 6.6's new strict behaviour that raises {@code
   * StaleObjectStateException} when merging a transient entity with a pre-assigned UUID.
   */
  private com.fortnite.pronos.model.Game buildOrLoadEntity(
      Game game, User creator, Map<UUID, User> participantUsersById) {
    Optional<com.fortnite.pronos.model.Game> existing =
        game.getId() != null ? gameRepository.findById(game.getId()) : Optional.empty();
    if (existing.isPresent()) {
      return updateExistingEntity(existing.get(), game, creator, participantUsersById);
    }
    return mapper.toEntity(game, creator, participantUsersById);
  }

  private com.fortnite.pronos.model.Game updateExistingEntity(
      com.fortnite.pronos.model.Game entity,
      Game domain,
      User creator,
      Map<UUID, User> participantUsersById) {
    entity.setName(domain.getName());
    entity.setDescription(domain.getDescription());
    entity.setMaxParticipants(domain.getMaxParticipants());
    entity.setStatus(com.fortnite.pronos.model.GameStatus.valueOf(domain.getStatus().name()));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setFinishedAt(domain.getFinishedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setInvitationCode(domain.getInvitationCode());
    entity.setInvitationCodeExpiresAt(domain.getInvitationCodeExpiresAt());
    entity.setTradingEnabled(domain.isTradingEnabled());
    entity.setMaxTradesPerTeam(domain.getMaxTradesPerTeam());
    entity.setTradeDeadline(domain.getTradeDeadline());
    entity.setCurrentSeason(domain.getCurrentSeason());
    entity.setTranchesEnabled(domain.isTranchesEnabled());
    entity.setTrancheSize(domain.getTrancheSize());
    entity.setTeamSize(domain.getTeamSize());
    entity.setCompetitionStart(domain.getCompetitionStart());
    entity.setCompetitionEnd(domain.getCompetitionEnd());
    entity.setCreator(creator);
    addNewParticipantsToManagedEntity(entity, domain.getParticipants(), participantUsersById);
    return entity;
  }

  /**
   * Adds participants that are not yet in the managed entity's collection. Since the entity is
   * already managed (loaded from DB), its participant list is in the Hibernate session. New
   * participants (not found in the existing list) are appended without pre-assigned IDs so that
   * Hibernate generates their UUID via {@code @GeneratedValue}.
   */
  private void addNewParticipantsToManagedEntity(
      com.fortnite.pronos.model.Game managedEntity,
      List<GameParticipant> domainParticipants,
      Map<UUID, User> participantUsersById) {
    if (domainParticipants == null || domainParticipants.isEmpty()) {
      return;
    }
    List<UUID> existingUserIds =
        managedEntity.getParticipants().stream()
            .map(p -> p.getUser() != null ? p.getUser().getId() : null)
            .filter(Objects::nonNull)
            .toList();
    for (GameParticipant domainParticipant : domainParticipants) {
      if (domainParticipant == null || domainParticipant.getUserId() == null) {
        continue;
      }
      if (!existingUserIds.contains(domainParticipant.getUserId())) {
        User user = participantUsersById.get(domainParticipant.getUserId());
        if (user == null) {
          continue;
        }
        com.fortnite.pronos.model.GameParticipant newParticipant =
            com.fortnite.pronos.model.GameParticipant.builder()
                .game(managedEntity)
                .user(user)
                .draftOrder(domainParticipant.getDraftOrder())
                .joinedAt(domainParticipant.getJoinedAt())
                .lastSelectionTime(domainParticipant.getLastSelectionTime())
                .creator(domainParticipant.isCreator())
                .build();
        managedEntity.getParticipants().add(newParticipant);
      }
    }
  }

  @Override
  public Optional<Game> findByInvitationCode(String invitationCode) {
    return gameRepository.findByInvitationCode(invitationCode).map(mapper::toDomain);
  }

  @Override
  public Optional<Game> findByInvitationCodeForUpdate(String invitationCode) {
    return gameRepository.findByInvitationCodeForUpdate(invitationCode).map(mapper::toDomain);
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

  @Override
  public List<Game> findAllWithCompetitionPeriod() {
    return mapper.toDomainList(gameRepository.findAllWithCompetitionPeriod());
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
                  () ->
                      EntityReferenceFactory.userRef(
                          participant.getUserId(), participant.getUsername()));
      usersById.put(participant.getUserId(), user);
    }
    return usersById;
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
