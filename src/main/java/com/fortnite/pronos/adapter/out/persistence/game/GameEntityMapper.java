package com.fortnite.pronos.adapter.out.persistence.game;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.adapter.out.persistence.support.EntityReferenceFactory;
import com.fortnite.pronos.adapter.out.persistence.support.MappingUtils;
import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;

/**
 * Bidirectional mapper between pure domain models ({@code domain.game.model.Game}) and JPA entities
 * ({@code model.Game}). Lives in the adapter layer to keep both domain and entity layers clean.
 */
@Component
public class GameEntityMapper {

  // ===============================
  // DOMAIN -> ENTITY
  // ===============================

  /** Maps a domain Game to a JPA Game entity with resolved user references. */
  public Game toEntity(
      com.fortnite.pronos.domain.game.model.Game domain,
      User creator,
      Map<UUID, User> participantUsersById) {
    if (domain == null) {
      return null;
    }

    Map<UUID, User> participantUsers =
        participantUsersById != null ? participantUsersById : Collections.emptyMap();

    Game entity = new Game();
    entity.setId(domain.getId());
    entity.setName(domain.getName());
    entity.setDescription(domain.getDescription());
    entity.setMaxParticipants(domain.getMaxParticipants());
    entity.setStatus(toEntityStatus(domain.getStatus()));
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setFinishedAt(domain.getFinishedAt());
    entity.setDeletedAt(domain.getDeletedAt());
    entity.setInvitationCode(domain.getInvitationCode());
    entity.setInvitationCodeExpiresAt(domain.getInvitationCodeExpiresAt());
    entity.setTradingEnabled(domain.isTradingEnabled());
    entity.setMaxTradesPerTeam(domain.getMaxTradesPerTeam());
    entity.setTradeDeadline(domain.getTradeDeadline());
    entity.setCurrentSeason(domain.getCurrentSeason());
    entity.setDraftMode(toEntityDraftMode(domain.getDraftMode()));
    entity.setTeamSize(domain.getTeamSize());
    entity.setTrancheSize(domain.getTrancheSize());
    entity.setTranchesEnabled(domain.isTranchesEnabled());
    entity.setCompetitionStart(domain.getCompetitionStart());
    entity.setCompetitionEnd(domain.getCompetitionEnd());
    entity.setCreator(creator);

    if (domain.getDraftId() != null) {
      entity.setDraft(EntityReferenceFactory.draftRef(domain.getDraftId()));
    }

    entity.setRegionRules(toEntityRegionRules(domain.getRegionRules(), entity));
    entity.setParticipants(
        toEntityParticipants(domain.getParticipants(), entity, participantUsers));

    return entity;
  }

  /** Convenience overload used by tests or temporary callers during migration. */
  public Game toEntity(com.fortnite.pronos.domain.game.model.Game domain) {
    return toEntity(
        domain,
        EntityReferenceFactory.userRef(domain != null ? domain.getCreatorId() : null, null),
        Map.of());
  }

  private List<com.fortnite.pronos.model.GameRegionRule> toEntityRegionRules(
      List<GameRegionRule> rules, Game parentEntity) {
    if (rules == null) {
      return Collections.emptyList();
    }
    return rules.stream()
        .map(rule -> toEntityRegionRule(rule, parentEntity))
        .filter(Objects::nonNull)
        .toList();
  }

  private List<com.fortnite.pronos.model.GameParticipant> toEntityParticipants(
      List<GameParticipant> participants, Game parentEntity, Map<UUID, User> participantUsersById) {
    if (participants == null) {
      return Collections.emptyList();
    }
    return participants.stream()
        .map(participant -> toEntityParticipant(participant, parentEntity, participantUsersById))
        .filter(Objects::nonNull)
        .toList();
  }

  private com.fortnite.pronos.model.GameParticipant toEntityParticipant(
      GameParticipant domain, Game parentEntity, Map<UUID, User> participantUsersById) {
    if (domain == null) {
      return null;
    }

    com.fortnite.pronos.model.GameParticipant entity =
        new com.fortnite.pronos.model.GameParticipant();
    entity.setId(domain.getId());
    entity.setDraftOrder(domain.getDraftOrder());
    entity.setJoinedAt(domain.getJoinedAt());
    entity.setLastSelectionTime(domain.getLastSelectionTime());
    entity.setCreator(domain.isCreator());
    entity.setGame(parentEntity);
    entity.setUser(resolveParticipantUser(domain, participantUsersById));
    entity.setSelectedPlayers(toSelectedPlayers(domain.getSelectedPlayerIds()));
    return entity;
  }

  private List<Player> toSelectedPlayers(List<UUID> selectedPlayerIds) {
    if (selectedPlayerIds == null) {
      return Collections.emptyList();
    }
    return selectedPlayerIds.stream()
        .map(EntityReferenceFactory::playerRef)
        .filter(Objects::nonNull)
        .toList();
  }

  private User resolveParticipantUser(
      GameParticipant domainParticipant, Map<UUID, User> participantUsersById) {
    if (domainParticipant.getUserId() == null) {
      return null;
    }
    User resolvedUser = participantUsersById.get(domainParticipant.getUserId());
    if (resolvedUser != null) {
      return resolvedUser;
    }
    return EntityReferenceFactory.userRef(
        domainParticipant.getUserId(), domainParticipant.getUsername());
  }

  private com.fortnite.pronos.model.GameRegionRule toEntityRegionRule(
      GameRegionRule domain, Game parentEntity) {
    if (domain == null) {
      return null;
    }

    com.fortnite.pronos.model.GameRegionRule entity =
        new com.fortnite.pronos.model.GameRegionRule();
    entity.setId(domain.getId());
    entity.setRegion(toEntityRegion(domain.getRegion()));
    entity.setMaxPlayers(domain.getMaxPlayers());
    entity.setGame(parentEntity);

    return entity;
  }

  // ===============================
  // ENTITY -> DOMAIN
  // ===============================

  /** Maps a JPA Game entity to a pure domain Game using the reconstitution factory. */
  public com.fortnite.pronos.domain.game.model.Game toDomain(Game entity) {
    if (entity == null) {
      return null;
    }

    List<GameRegionRule> domainRules = toDomainRegionRules(entity.getRegionRules());
    List<GameParticipant> domainParticipants =
        ensureCreatorParticipant(
            toDomainParticipants(entity.getParticipants()),
            entity.getCreator(),
            entity.getCreatedAt());

    return com.fortnite.pronos.domain.game.model.Game.restore(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.getCreator() != null ? entity.getCreator().getId() : null,
        MappingUtils.safeInt(entity.getMaxParticipants(), 2),
        safeStatus(toDomainStatus(entity.getStatus())),
        entity.getCreatedAt(),
        entity.getFinishedAt(),
        entity.getDeletedAt(),
        entity.getInvitationCode(),
        entity.getInvitationCodeExpiresAt(),
        domainRules,
        domainParticipants,
        entity.getDraft() != null ? entity.getDraft().getId() : null,
        MappingUtils.safeBool(entity.getTradingEnabled()),
        MappingUtils.safeInt(entity.getMaxTradesPerTeam(), 5),
        entity.getTradeDeadline(),
        MappingUtils.safeInt(entity.getCurrentSeason(), java.time.Year.now().getValue()),
        toDomainDraftMode(entity.getDraftMode()),
        MappingUtils.safeInt(entity.getTeamSize(), 5),
        MappingUtils.safeInt(entity.getTrancheSize(), 10),
        MappingUtils.safeBool(entity.getTranchesEnabled()),
        entity.getCompetitionStart(),
        entity.getCompetitionEnd());
  }

  private List<GameParticipant> ensureCreatorParticipant(
      List<GameParticipant> participants, User creator, java.time.LocalDateTime createdAt) {
    if (creator == null || creator.getId() == null) {
      return participants;
    }

    boolean creatorAlreadyPresent =
        participants.stream()
            .filter(Objects::nonNull)
            .anyMatch(participant -> creator.getId().equals(participant.getUserId()));
    if (creatorAlreadyPresent) {
      return participants;
    }

    java.time.LocalDateTime joinedAt =
        createdAt != null ? createdAt : java.time.LocalDateTime.now();
    java.util.ArrayList<GameParticipant> withCreator = new java.util.ArrayList<>(participants);
    withCreator.add(
        GameParticipant.restore(
            null, creator.getId(), creator.getUsername(), null, joinedAt, null, true, List.of()));
    return withCreator;
  }

  private List<GameRegionRule> toDomainRegionRules(
      List<com.fortnite.pronos.model.GameRegionRule> rules) {
    if (rules == null) {
      return Collections.emptyList();
    }
    return rules.stream().map(this::toDomainRegionRule).filter(Objects::nonNull).toList();
  }

  private List<GameParticipant> toDomainParticipants(
      List<com.fortnite.pronos.model.GameParticipant> participants) {
    if (participants == null) {
      return Collections.emptyList();
    }
    return participants.stream().map(this::toDomainParticipant).filter(Objects::nonNull).toList();
  }

  /**
   * Maps a JPA GameParticipant entity to a domain GameParticipant. Returns null if the entity has
   * no user (invalid state in the domain model).
   */
  public GameParticipant toDomainParticipant(com.fortnite.pronos.model.GameParticipant entity) {
    if (entity == null || entity.getUser() == null) {
      return null;
    }

    List<java.util.UUID> selectedPlayerIds =
        entity.getSelectedPlayers() != null
            ? entity.getSelectedPlayers().stream().map(Player::getId).toList()
            : Collections.emptyList();

    return GameParticipant.restore(
        entity.getId(),
        entity.getUser().getId(),
        entity.getUsername(),
        entity.getDraftOrder(),
        entity.getJoinedAt(),
        entity.getLastSelectionTime(),
        MappingUtils.safeBool(entity.getCreator()),
        selectedPlayerIds);
  }

  /** Maps a JPA GameRegionRule entity to a domain GameRegionRule. */
  public GameRegionRule toDomainRegionRule(com.fortnite.pronos.model.GameRegionRule entity) {
    if (entity == null || entity.getRegion() == null) {
      return null;
    }
    return new GameRegionRule(
        entity.getId(),
        toDomainRegion(entity.getRegion()),
        MappingUtils.safeInt(entity.getMaxPlayers(), 1));
  }

  // ===============================
  // ENUM MAPPING
  // ===============================

  /** Maps domain GameStatus to JPA GameStatus. */
  public com.fortnite.pronos.model.GameStatus toEntityStatus(GameStatus domainStatus) {
    if (domainStatus == null) {
      return null;
    }
    return com.fortnite.pronos.model.GameStatus.valueOf(domainStatus.name());
  }

  /** Maps JPA GameStatus to domain GameStatus. */
  public GameStatus toDomainStatus(com.fortnite.pronos.model.GameStatus entityStatus) {
    if (entityStatus == null) {
      return null;
    }
    return GameStatus.valueOf(entityStatus.name());
  }

  /** Maps domain DraftMode to JPA DraftMode. */
  public com.fortnite.pronos.model.DraftMode toEntityDraftMode(DraftMode domainDraftMode) {
    if (domainDraftMode == null) {
      return com.fortnite.pronos.model.DraftMode.SNAKE;
    }
    return com.fortnite.pronos.model.DraftMode.valueOf(domainDraftMode.name());
  }

  /** Maps JPA DraftMode to domain DraftMode. */
  public DraftMode toDomainDraftMode(com.fortnite.pronos.model.DraftMode entityDraftMode) {
    if (entityDraftMode == null) {
      return DraftMode.SNAKE;
    }
    return DraftMode.valueOf(entityDraftMode.name());
  }

  /** Maps domain PlayerRegion to JPA Player.Region. */
  public Player.Region toEntityRegion(PlayerRegion domainRegion) {
    if (domainRegion == null) {
      return null;
    }
    return Player.Region.valueOf(domainRegion.name());
  }

  /** Maps JPA Player.Region to domain PlayerRegion. */
  public PlayerRegion toDomainRegion(Player.Region entityRegion) {
    if (entityRegion == null) {
      return null;
    }
    return PlayerRegion.valueOf(entityRegion.name());
  }

  // ===============================
  // COLLECTION MAPPING
  // ===============================

  /** Maps a list of JPA Game entities to domain Games. */
  public List<com.fortnite.pronos.domain.game.model.Game> toDomainList(List<Game> entities) {
    if (entities == null) {
      return Collections.emptyList();
    }
    return entities.stream().map(this::toDomain).toList();
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private static GameStatus safeStatus(GameStatus status) {
    return status != null ? status : GameStatus.CREATING;
  }
}
