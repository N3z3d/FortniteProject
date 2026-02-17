package com.fortnite.pronos.adapter.out.persistence.trade;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.port.out.TradeDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.TradeRepositoryPort;
import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;

/** Persistence adapter for Trade domain migration (ARCH-015). */
@Component
public class TradeRepositoryAdapter implements TradeDomainRepositoryPort {

  private final TradeRepositoryPort tradeRepository;
  private final TradeEntityMapper mapper;

  public TradeRepositoryAdapter(TradeRepositoryPort tradeRepository, TradeEntityMapper mapper) {
    this.tradeRepository = tradeRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Trade> findById(UUID id) {
    return tradeCrudRepository().findById(id).map(mapper::toDomain);
  }

  @Override
  public Trade save(Trade trade) {
    Objects.requireNonNull(trade, "Trade cannot be null");
    com.fortnite.pronos.model.Trade existing =
        tradeCrudRepository().findById(trade.getId()).orElse(null);

    com.fortnite.pronos.model.Trade entity;
    if (existing != null) {
      updateEntityFromDomain(existing, trade);
      entity = existing;
    } else {
      entity = mapper.toEntity(trade, null, null, List.of(), List.of());
    }

    com.fortnite.pronos.model.Trade saved = tradeCrudRepository().save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public List<Trade> findByTeamId(UUID teamId) {
    return mapper.toDomainList(tradeRepository.findByTeamId(teamId));
  }

  @Override
  public List<Trade> findPendingTradesForTeam(UUID teamId) {
    return mapper.toDomainList(tradeRepository.findPendingTradesForTeam(teamId));
  }

  @Override
  public long countByGameIdAndStatus(UUID gameId, TradeStatus status) {
    com.fortnite.pronos.model.Trade.Status entityStatus = mapper.toEntityStatus(status);
    Long count = tradeRepository.countByGameIdAndStatus(gameId, entityStatus);
    return count != null ? count : 0L;
  }

  @Override
  public List<Trade> findByGameId(UUID gameId) {
    return mapper.toDomainList(tradeRepository.findByGameId(gameId));
  }

  @Override
  public List<Trade> findByGameIdAndStatus(UUID gameId, TradeStatus status) {
    com.fortnite.pronos.model.Trade.Status entityStatus = mapper.toEntityStatus(status);
    return mapper.toDomainList(tradeRepository.findByGameIdAndStatus(gameId, entityStatus));
  }

  @Override
  public List<Trade> findTradesBetweenTeams(UUID teamId1, UUID teamId2) {
    return mapper.toDomainList(tradeRepository.findTradesBetweenTeams(teamId1, teamId2));
  }

  private void updateEntityFromDomain(com.fortnite.pronos.model.Trade entity, Trade domain) {
    entity.setStatus(mapper.toEntityStatus(domain.getStatus()));
    entity.setAcceptedAt(domain.getAcceptedAt());
    entity.setRejectedAt(domain.getRejectedAt());
    entity.setCancelledAt(domain.getCancelledAt());
    entity.setOriginalTradeId(domain.getOriginalTradeId());
  }

  @SuppressWarnings("unchecked")
  private CrudRepository<com.fortnite.pronos.model.Trade, UUID> tradeCrudRepository() {
    return (CrudRepository<com.fortnite.pronos.model.Trade, UUID>) tradeRepository;
  }
}
