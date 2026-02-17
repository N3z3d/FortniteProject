package com.fortnite.pronos.adapter.out.persistence.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;

/** Maps between Trade JPA entities and Trade domain models. */
@Component
public class TradeEntityMapper {

  /** Converts a JPA entity to a domain model. */
  public Trade toDomain(com.fortnite.pronos.model.Trade entity) {
    if (entity == null) {
      return null;
    }
    return Trade.restore(
        entity.getId(),
        extractTeamId(entity.getFromTeam()),
        extractTeamId(entity.getToTeam()),
        extractPlayerIds(entity.getOfferedPlayers()),
        extractPlayerIds(entity.getRequestedPlayers()),
        toDomainStatus(entity.getStatus()),
        entity.getProposedAt(),
        entity.getAcceptedAt(),
        entity.getRejectedAt(),
        entity.getCancelledAt(),
        entity.getOriginalTradeId());
  }

  /** Converts a domain model to a JPA entity. Requires team and player entity resolution. */
  public com.fortnite.pronos.model.Trade toEntity(
      Trade domain,
      Team fromTeam,
      Team toTeam,
      List<Player> offeredPlayers,
      List<Player> requestedPlayers) {
    if (domain == null) {
      return null;
    }
    com.fortnite.pronos.model.Trade entity = new com.fortnite.pronos.model.Trade();
    entity.setId(domain.getId());
    entity.setFromTeam(fromTeam);
    entity.setToTeam(toTeam);
    entity.setOfferedPlayers(
        offeredPlayers != null ? new ArrayList<>(offeredPlayers) : new ArrayList<>());
    entity.setRequestedPlayers(
        requestedPlayers != null ? new ArrayList<>(requestedPlayers) : new ArrayList<>());
    entity.setStatus(toEntityStatus(domain.getStatus()));
    entity.setProposedAt(domain.getProposedAt());
    entity.setAcceptedAt(domain.getAcceptedAt());
    entity.setRejectedAt(domain.getRejectedAt());
    entity.setCancelledAt(domain.getCancelledAt());
    entity.setOriginalTradeId(domain.getOriginalTradeId());
    return entity;
  }

  /** Converts a list of JPA entities to domain models. */
  public List<Trade> toDomainList(List<com.fortnite.pronos.model.Trade> entities) {
    if (entities == null) {
      return Collections.emptyList();
    }
    return entities.stream().map(this::toDomain).toList();
  }

  /** Maps entity Status to domain TradeStatus. */
  public TradeStatus toDomainStatus(com.fortnite.pronos.model.Trade.Status status) {
    if (status == null) {
      return TradeStatus.PENDING;
    }
    return TradeStatus.valueOf(status.name());
  }

  /** Maps domain TradeStatus to entity Status. */
  public com.fortnite.pronos.model.Trade.Status toEntityStatus(TradeStatus status) {
    if (status == null) {
      return com.fortnite.pronos.model.Trade.Status.PENDING;
    }
    return com.fortnite.pronos.model.Trade.Status.valueOf(status.name());
  }

  private UUID extractTeamId(Team team) {
    return team != null ? team.getId() : null;
  }

  private List<UUID> extractPlayerIds(List<Player> players) {
    if (players == null) {
      return Collections.emptyList();
    }
    return players.stream().map(Player::getId).toList();
  }
}
