package com.fortnite.pronos.adapter.out.persistence.trade;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;

class TradeEntityMapperTest {

  private TradeEntityMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TradeEntityMapper();
  }

  @Nested
  class ToDomain {

    @Test
    void mapsAllFields() {
      UUID tradeId = UUID.randomUUID();
      UUID teamAId = UUID.randomUUID();
      UUID teamBId = UUID.randomUUID();
      UUID playerId1 = UUID.randomUUID();
      UUID playerId2 = UUID.randomUUID();
      UUID origId = UUID.randomUUID();
      LocalDateTime now = LocalDateTime.now();

      Team teamA = new Team();
      teamA.setId(teamAId);
      Team teamB = new Team();
      teamB.setId(teamBId);
      Player p1 = Player.builder().id(playerId1).build();
      Player p2 = Player.builder().id(playerId2).build();

      com.fortnite.pronos.model.Trade entity = new com.fortnite.pronos.model.Trade();
      entity.setId(tradeId);
      entity.setFromTeam(teamA);
      entity.setToTeam(teamB);
      entity.setOfferedPlayers(List.of(p1));
      entity.setRequestedPlayers(List.of(p2));
      entity.setStatus(com.fortnite.pronos.model.Trade.Status.ACCEPTED);
      entity.setProposedAt(now);
      entity.setAcceptedAt(now.plusMinutes(5));
      entity.setOriginalTradeId(origId);

      Trade domain = mapper.toDomain(entity);

      assertThat(domain.getId()).isEqualTo(tradeId);
      assertThat(domain.getFromTeamId()).isEqualTo(teamAId);
      assertThat(domain.getToTeamId()).isEqualTo(teamBId);
      assertThat(domain.getOfferedPlayerIds()).containsExactly(playerId1);
      assertThat(domain.getRequestedPlayerIds()).containsExactly(playerId2);
      assertThat(domain.getStatus()).isEqualTo(TradeStatus.ACCEPTED);
      assertThat(domain.getProposedAt()).isEqualTo(now);
      assertThat(domain.getAcceptedAt()).isEqualTo(now.plusMinutes(5));
      assertThat(domain.getOriginalTradeId()).isEqualTo(origId);
    }

    @Test
    void returnsNullForNullEntity() {
      assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void handlesNullTeams() {
      com.fortnite.pronos.model.Trade entity = new com.fortnite.pronos.model.Trade();
      entity.setId(UUID.randomUUID());
      entity.setStatus(com.fortnite.pronos.model.Trade.Status.PENDING);

      Trade domain = mapper.toDomain(entity);

      assertThat(domain.getFromTeamId()).isNull();
      assertThat(domain.getToTeamId()).isNull();
    }

    @Test
    void handlesNullPlayerLists() {
      com.fortnite.pronos.model.Trade entity = new com.fortnite.pronos.model.Trade();
      entity.setId(UUID.randomUUID());
      entity.setStatus(com.fortnite.pronos.model.Trade.Status.PENDING);
      entity.setOfferedPlayers(null);
      entity.setRequestedPlayers(null);

      Trade domain = mapper.toDomain(entity);

      assertThat(domain.getOfferedPlayerIds()).isEmpty();
      assertThat(domain.getRequestedPlayerIds()).isEmpty();
    }
  }

  @Nested
  class ToEntity {

    @Test
    void mapsAllFields() {
      UUID tradeId = UUID.randomUUID();
      UUID teamAId = UUID.randomUUID();
      UUID teamBId = UUID.randomUUID();
      UUID playerId1 = UUID.randomUUID();
      UUID playerId2 = UUID.randomUUID();
      LocalDateTime now = LocalDateTime.now();

      Trade domain =
          Trade.restore(
              tradeId,
              teamAId,
              teamBId,
              List.of(playerId1),
              List.of(playerId2),
              TradeStatus.PENDING,
              now,
              null,
              null,
              null,
              null);

      Team teamA = new Team();
      teamA.setId(teamAId);
      Team teamB = new Team();
      teamB.setId(teamBId);
      Player p1 = Player.builder().id(playerId1).build();
      Player p2 = Player.builder().id(playerId2).build();

      com.fortnite.pronos.model.Trade entity =
          mapper.toEntity(domain, teamA, teamB, List.of(p1), List.of(p2));

      assertThat(entity.getId()).isEqualTo(tradeId);
      assertThat(entity.getFromTeam().getId()).isEqualTo(teamAId);
      assertThat(entity.getToTeam().getId()).isEqualTo(teamBId);
      assertThat(entity.getOfferedPlayers()).hasSize(1);
      assertThat(entity.getRequestedPlayers()).hasSize(1);
      assertThat(entity.getStatus()).isEqualTo(com.fortnite.pronos.model.Trade.Status.PENDING);
      assertThat(entity.getProposedAt()).isEqualTo(now);
    }

    @Test
    void returnsNullForNullDomain() {
      assertThat(mapper.toEntity(null, null, null, null, null)).isNull();
    }
  }

  @Nested
  class ToDomainList {

    @Test
    void mapsMultipleEntities() {
      com.fortnite.pronos.model.Trade e1 = buildEntity(UUID.randomUUID());
      com.fortnite.pronos.model.Trade e2 = buildEntity(UUID.randomUUID());

      List<Trade> result = mapper.toDomainList(List.of(e1, e2));

      assertThat(result).hasSize(2);
    }

    @Test
    void returnsEmptyForNull() {
      assertThat(mapper.toDomainList(null)).isEmpty();
    }

    @Test
    void returnsEmptyForEmptyList() {
      assertThat(mapper.toDomainList(List.of())).isEmpty();
    }
  }

  @Nested
  class StatusMapping {

    @Test
    void mapsAllStatusValues() {
      for (com.fortnite.pronos.model.Trade.Status status :
          com.fortnite.pronos.model.Trade.Status.values()) {
        TradeStatus domainStatus = mapper.toDomainStatus(status);
        assertThat(domainStatus.name()).isEqualTo(status.name());
      }
    }

    @Test
    void mapsAllDomainStatusValues() {
      for (TradeStatus status : TradeStatus.values()) {
        com.fortnite.pronos.model.Trade.Status entityStatus = mapper.toEntityStatus(status);
        assertThat(entityStatus.name()).isEqualTo(status.name());
      }
    }

    @Test
    void nullStatusDefaultsToPending() {
      assertThat(mapper.toDomainStatus(null)).isEqualTo(TradeStatus.PENDING);
      assertThat(mapper.toEntityStatus(null))
          .isEqualTo(com.fortnite.pronos.model.Trade.Status.PENDING);
    }
  }

  private com.fortnite.pronos.model.Trade buildEntity(UUID tradeId) {
    Team team = new Team();
    team.setId(UUID.randomUUID());
    com.fortnite.pronos.model.Trade entity = new com.fortnite.pronos.model.Trade();
    entity.setId(tradeId);
    entity.setFromTeam(team);
    entity.setToTeam(team);
    entity.setStatus(com.fortnite.pronos.model.Trade.Status.PENDING);
    entity.setProposedAt(LocalDateTime.now());
    return entity;
  }
}
