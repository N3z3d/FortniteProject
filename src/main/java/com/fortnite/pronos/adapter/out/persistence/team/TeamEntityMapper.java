package com.fortnite.pronos.adapter.out.persistence.team;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.team.model.TeamMember;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;

/**
 * Bidirectional mapper between pure domain models ({@code domain.team.model.Team}) and JPA entities
 * ({@code model.Team}). Lives in the adapter layer to keep both domain and entity layers clean.
 */
@Component
public class TeamEntityMapper {

  // ===============================
  // ENTITY -> DOMAIN
  // ===============================

  /** Maps a JPA Team entity to a pure domain Team using the reconstitution factory. */
  public com.fortnite.pronos.domain.team.model.Team toDomain(
      com.fortnite.pronos.model.Team entity) {
    if (entity == null) {
      return null;
    }

    List<TeamMember> members = toDomainMembers(entity.getPlayers());

    return com.fortnite.pronos.domain.team.model.Team.restore(
        entity.getId(),
        entity.getName(),
        entity.getOwner() != null ? entity.getOwner().getId() : null,
        safeInt(entity.getSeason(), 2025),
        entity.getGame() != null ? entity.getGame().getId() : null,
        safeInt(entity.getCompletedTradesCount(), 0),
        members);
  }

  private List<TeamMember> toDomainMembers(List<TeamPlayer> teamPlayers) {
    if (teamPlayers == null) {
      return Collections.emptyList();
    }
    return teamPlayers.stream().map(this::toDomainMember).filter(Objects::nonNull).toList();
  }

  private TeamMember toDomainMember(TeamPlayer entity) {
    if (entity == null || entity.getPlayer() == null) {
      return null;
    }
    return TeamMember.restore(
        entity.getPlayer().getId(), safeInt(entity.getPosition(), 1), entity.getUntil());
  }

  // ===============================
  // DOMAIN -> ENTITY
  // ===============================

  /** Maps a domain Team to a JPA Team entity with resolved owner reference. */
  public com.fortnite.pronos.model.Team toEntity(
      com.fortnite.pronos.domain.team.model.Team domain, User owner) {
    if (domain == null) {
      return null;
    }

    com.fortnite.pronos.model.Team entity = new com.fortnite.pronos.model.Team();
    entity.setId(domain.getId());
    entity.setName(domain.getName());
    entity.setOwner(owner);
    entity.setSeason(domain.getSeason());
    entity.setCompletedTradesCount(domain.getCompletedTradesCount());

    if (domain.getGameId() != null) {
      entity.setGame(createGameReference(domain.getGameId()));
    }

    List<TeamPlayer> entityPlayers = toEntityMembers(domain.getMembers(), entity);
    entity.setPlayers(entityPlayers);

    return entity;
  }

  /** Convenience overload for tests or temporary callers. */
  public com.fortnite.pronos.model.Team toEntity(
      com.fortnite.pronos.domain.team.model.Team domain) {
    return toEntity(domain, createUserReference(domain != null ? domain.getOwnerId() : null));
  }

  private List<TeamPlayer> toEntityMembers(
      List<TeamMember> members, com.fortnite.pronos.model.Team parentEntity) {
    if (members == null) {
      return Collections.emptyList();
    }
    return members.stream()
        .map(m -> toEntityMember(m, parentEntity))
        .filter(Objects::nonNull)
        .toList();
  }

  private TeamPlayer toEntityMember(
      TeamMember domain, com.fortnite.pronos.model.Team parentEntity) {
    if (domain == null) {
      return null;
    }
    TeamPlayer entity = new TeamPlayer();
    entity.setTeam(parentEntity);
    entity.setPlayer(createPlayerReference(domain.getPlayerId()));
    entity.setPosition(domain.getPosition());
    entity.setUntil(domain.getUntil());
    return entity;
  }

  // ===============================
  // COLLECTION MAPPING
  // ===============================

  /** Maps a list of JPA Team entities to domain Teams. */
  public List<com.fortnite.pronos.domain.team.model.Team> toDomainList(
      List<com.fortnite.pronos.model.Team> entities) {
    if (entities == null) {
      return Collections.emptyList();
    }
    return entities.stream().map(this::toDomain).toList();
  }

  // ===============================
  // PRIVATE HELPERS
  // ===============================

  private User createUserReference(java.util.UUID userId) {
    if (userId == null) {
      return null;
    }
    User user = new User();
    user.setId(userId);
    return user;
  }

  private Player createPlayerReference(java.util.UUID playerId) {
    if (playerId == null) {
      return null;
    }
    return Player.builder().id(playerId).build();
  }

  private com.fortnite.pronos.model.Game createGameReference(java.util.UUID gameId) {
    com.fortnite.pronos.model.Game game = new com.fortnite.pronos.model.Game();
    game.setId(gameId);
    return game;
  }

  private static int safeInt(Integer value, int defaultValue) {
    return value != null ? value : defaultValue;
  }
}
