package com.fortnite.pronos.adapter.out.persistence.team;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.team.model.TeamMember;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;

class TeamEntityMapperTest {

  private TeamEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TeamEntityMapper();
  }

  @Nested
  class ToDomain {

    @Test
    void mapsScalarFieldsCorrectly() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      UUID gameId = UUID.randomUUID();
      Team entity = buildEntity(teamId, "Team A", ownerId, 2025, gameId, 3);

      com.fortnite.pronos.domain.team.model.Team domain = mapper.toDomain(entity);

      assertThat(domain).isNotNull();
      assertThat(domain.getId()).isEqualTo(teamId);
      assertThat(domain.getName()).isEqualTo("Team A");
      assertThat(domain.getOwnerId()).isEqualTo(ownerId);
      assertThat(domain.getSeason()).isEqualTo(2025);
      assertThat(domain.getGameId()).isEqualTo(gameId);
      assertThat(domain.getCompletedTradesCount()).isEqualTo(3);
    }

    @Test
    void mapsTeamPlayersToMembers() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      UUID playerId = UUID.randomUUID();
      Team entity = buildEntity(teamId, "Team B", ownerId, 2025, null, 0);

      TeamPlayer tp = buildTeamPlayer(entity, playerId, 2, null);
      entity.setPlayers(List.of(tp));

      com.fortnite.pronos.domain.team.model.Team domain = mapper.toDomain(entity);

      assertThat(domain.getMembers()).hasSize(1);
      TeamMember member = domain.getMembers().get(0);
      assertThat(member.getPlayerId()).isEqualTo(playerId);
      assertThat(member.getPosition()).isEqualTo(2);
      assertThat(member.isActive()).isTrue();
    }

    @Test
    void mapsEndedMembership() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      UUID playerId = UUID.randomUUID();
      OffsetDateTime endTime = OffsetDateTime.now().minusDays(1);
      Team entity = buildEntity(teamId, "Team C", ownerId, 2025, null, 0);

      TeamPlayer tp = buildTeamPlayer(entity, playerId, 1, endTime);
      entity.setPlayers(List.of(tp));

      com.fortnite.pronos.domain.team.model.Team domain = mapper.toDomain(entity);

      assertThat(domain.getMembers().get(0).isActive()).isFalse();
      assertThat(domain.getMembers().get(0).getUntil()).isEqualTo(endTime);
    }

    @Test
    void returnsNullForNullEntity() {
      assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void handlesNullOwner() {
      Team entity = new Team();
      entity.setId(UUID.randomUUID());
      entity.setName("No Owner");
      entity.setSeason(2025);

      com.fortnite.pronos.domain.team.model.Team domain = mapper.toDomain(entity);

      assertThat(domain.getOwnerId()).isNull();
    }

    @Test
    void handlesNullGame() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      Team entity = buildEntity(teamId, "No Game", ownerId, 2025, null, 0);

      com.fortnite.pronos.domain.team.model.Team domain = mapper.toDomain(entity);

      assertThat(domain.getGameId()).isNull();
    }

    @Test
    void handlesNullPlayersList() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      Team entity = buildEntity(teamId, "Empty", ownerId, 2025, null, 0);
      entity.setPlayers(null);

      com.fortnite.pronos.domain.team.model.Team domain = mapper.toDomain(entity);

      assertThat(domain.getMembers()).isEmpty();
    }
  }

  @Nested
  class ToEntity {

    @Test
    void mapsScalarFieldsCorrectly() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      UUID gameId = UUID.randomUUID();
      com.fortnite.pronos.domain.team.model.Team domain =
          com.fortnite.pronos.domain.team.model.Team.restore(
              teamId, "Domain Team", ownerId, 2024, gameId, 5, null);

      Team entity = mapper.toEntity(domain);

      assertThat(entity).isNotNull();
      assertThat(entity.getId()).isEqualTo(teamId);
      assertThat(entity.getName()).isEqualTo("Domain Team");
      assertThat(entity.getSeason()).isEqualTo(2024);
      assertThat(entity.getCompletedTradesCount()).isEqualTo(5);
      assertThat(entity.getGame()).isNotNull();
      assertThat(entity.getGame().getId()).isEqualTo(gameId);
    }

    @Test
    void mapsMembersToTeamPlayers() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      UUID playerId = UUID.randomUUID();
      TeamMember member = TeamMember.restore(playerId, 3, null);
      com.fortnite.pronos.domain.team.model.Team domain =
          com.fortnite.pronos.domain.team.model.Team.restore(
              teamId, "Team", ownerId, 2025, null, 0, List.of(member));

      Team entity = mapper.toEntity(domain);

      assertThat(entity.getPlayers()).hasSize(1);
      TeamPlayer tp = entity.getPlayers().get(0);
      assertThat(tp.getPlayer().getId()).isEqualTo(playerId);
      assertThat(tp.getPosition()).isEqualTo(3);
      assertThat(tp.getTeam()).isEqualTo(entity);
    }

    @Test
    void returnsNullForNullDomain() {
      assertThat(mapper.toEntity((com.fortnite.pronos.domain.team.model.Team) null)).isNull();
    }

    @Test
    void handlesNullGameId() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      com.fortnite.pronos.domain.team.model.Team domain =
          com.fortnite.pronos.domain.team.model.Team.restore(
              teamId, "Team", ownerId, 2025, null, 0, null);

      Team entity = mapper.toEntity(domain);

      assertThat(entity.getGame()).isNull();
    }
  }

  @Nested
  class ToDomainList {

    @Test
    void mapsMultipleEntities() {
      UUID id1 = UUID.randomUUID();
      UUID id2 = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      Team e1 = buildEntity(id1, "Team1", ownerId, 2025, null, 0);
      Team e2 = buildEntity(id2, "Team2", ownerId, 2024, null, 1);

      List<com.fortnite.pronos.domain.team.model.Team> result =
          mapper.toDomainList(List.of(e1, e2));

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getId()).isEqualTo(id1);
      assertThat(result.get(1).getId()).isEqualTo(id2);
    }

    @Test
    void returnsEmptyForNullList() {
      assertThat(mapper.toDomainList(null)).isEmpty();
    }

    @Test
    void returnsEmptyForEmptyList() {
      assertThat(mapper.toDomainList(List.of())).isEmpty();
    }
  }

  @Nested
  class RoundTrip {

    @Test
    void toDomainOfToEntityPreservesFields() {
      UUID teamId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();
      UUID gameId = UUID.randomUUID();
      UUID playerId = UUID.randomUUID();
      TeamMember member = TeamMember.restore(playerId, 4, null);
      com.fortnite.pronos.domain.team.model.Team original =
          com.fortnite.pronos.domain.team.model.Team.restore(
              teamId, "RoundTrip", ownerId, 2024, gameId, 2, List.of(member));

      Team entity = mapper.toEntity(original);
      com.fortnite.pronos.domain.team.model.Team roundTripped = mapper.toDomain(entity);

      assertThat(roundTripped.getId()).isEqualTo(original.getId());
      assertThat(roundTripped.getName()).isEqualTo(original.getName());
      assertThat(roundTripped.getSeason()).isEqualTo(original.getSeason());
      assertThat(roundTripped.getGameId()).isEqualTo(original.getGameId());
      assertThat(roundTripped.getCompletedTradesCount())
          .isEqualTo(original.getCompletedTradesCount());
      assertThat(roundTripped.getMembers()).hasSize(1);
      assertThat(roundTripped.getMembers().get(0).getPlayerId()).isEqualTo(playerId);
      assertThat(roundTripped.getMembers().get(0).getPosition()).isEqualTo(4);
    }
  }

  // ===============================
  // HELPERS
  // ===============================

  private Team buildEntity(
      UUID id, String name, UUID ownerId, int season, UUID gameId, int completedTradesCount) {
    Team entity = new Team();
    entity.setId(id);
    entity.setName(name);
    entity.setSeason(season);
    entity.setCompletedTradesCount(completedTradesCount);

    User owner = new User();
    owner.setId(ownerId);
    entity.setOwner(owner);

    if (gameId != null) {
      Game game = new Game();
      game.setId(gameId);
      entity.setGame(game);
    }

    return entity;
  }

  private TeamPlayer buildTeamPlayer(Team team, UUID playerId, int position, OffsetDateTime until) {
    TeamPlayer tp = new TeamPlayer();
    tp.setTeam(team);
    Player player = new Player();
    player.setId(playerId);
    tp.setPlayer(player);
    tp.setPosition(position);
    tp.setUntil(until);
    return tp;
  }
}
