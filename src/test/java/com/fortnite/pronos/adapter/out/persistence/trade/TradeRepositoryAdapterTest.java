package com.fortnite.pronos.adapter.out.persistence.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import com.fortnite.pronos.domain.port.out.TradeRepositoryPort;
import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.model.Team;

@ExtendWith(MockitoExtension.class)
class TradeRepositoryAdapterTest {

  private TradeRepositoryPort tradeRepository;
  private TradeRepositoryAdapter adapter;
  private CrudRepository<com.fortnite.pronos.model.Trade, UUID> tradeCrudRepository;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    tradeRepository =
        mock(TradeRepositoryPort.class, withSettings().extraInterfaces(CrudRepository.class));
    adapter = new TradeRepositoryAdapter(tradeRepository, new TradeEntityMapper());
    tradeCrudRepository = (CrudRepository<com.fortnite.pronos.model.Trade, UUID>) tradeRepository;
  }

  @Test
  void findByIdReturnsEmptyWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(tradeCrudRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(adapter.findById(id)).isEmpty();
  }

  @Test
  void findByIdReturnsMappedDomain() {
    UUID tradeId = UUID.randomUUID();
    com.fortnite.pronos.model.Trade entity = buildEntity(tradeId);
    when(tradeCrudRepository.findById(tradeId)).thenReturn(Optional.of(entity));

    Optional<Trade> result = adapter.findById(tradeId);

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getId()).isEqualTo(tradeId);
  }

  @Test
  void saveThrowsWhenNull() {
    assertThatThrownBy(() -> adapter.save(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void saveUpdatesExistingEntity() {
    UUID tradeId = UUID.randomUUID();
    com.fortnite.pronos.model.Trade entity = buildEntity(tradeId);
    Trade domain = buildDomain(tradeId);

    when(tradeCrudRepository.findById(tradeId)).thenReturn(Optional.of(entity));
    when(tradeCrudRepository.save(any(com.fortnite.pronos.model.Trade.class))).thenReturn(entity);

    Trade saved = adapter.save(domain);

    assertThat(saved.getId()).isEqualTo(tradeId);
    verify(tradeCrudRepository).save(entity);
  }

  @Test
  void saveCreatesNewEntityWhenNotFound() {
    UUID tradeId = UUID.randomUUID();
    Trade domain = buildDomain(tradeId);
    com.fortnite.pronos.model.Trade entity = buildEntity(tradeId);

    when(tradeCrudRepository.findById(tradeId)).thenReturn(Optional.empty());
    when(tradeCrudRepository.save(any(com.fortnite.pronos.model.Trade.class))).thenReturn(entity);

    Trade saved = adapter.save(domain);

    assertThat(saved.getId()).isEqualTo(tradeId);
  }

  @Test
  void findByTeamIdDelegatesAndMaps() {
    UUID teamId = UUID.randomUUID();
    com.fortnite.pronos.model.Trade entity = buildEntity(UUID.randomUUID());
    when(tradeRepository.findByTeamId(teamId)).thenReturn(List.of(entity));

    List<Trade> result = adapter.findByTeamId(teamId);

    assertThat(result).hasSize(1);
    verify(tradeRepository).findByTeamId(teamId);
  }

  @Test
  void findPendingTradesForTeamDelegates() {
    UUID teamId = UUID.randomUUID();
    when(tradeRepository.findPendingTradesForTeam(teamId)).thenReturn(List.of());

    List<Trade> result = adapter.findPendingTradesForTeam(teamId);

    assertThat(result).isEmpty();
    verify(tradeRepository).findPendingTradesForTeam(teamId);
  }

  @Test
  void countByGameIdAndStatusConvertsEnum() {
    UUID gameId = UUID.randomUUID();
    when(tradeRepository.countByGameIdAndStatus(
            gameId, com.fortnite.pronos.model.Trade.Status.ACCEPTED))
        .thenReturn(3L);

    long count = adapter.countByGameIdAndStatus(gameId, TradeStatus.ACCEPTED);

    assertThat(count).isEqualTo(3L);
  }

  @Test
  void countByGameIdAndStatusReturnsZeroForNull() {
    UUID gameId = UUID.randomUUID();
    when(tradeRepository.countByGameIdAndStatus(
            gameId, com.fortnite.pronos.model.Trade.Status.PENDING))
        .thenReturn(null);

    long count = adapter.countByGameIdAndStatus(gameId, TradeStatus.PENDING);

    assertThat(count).isZero();
  }

  @Test
  void findByGameIdDelegates() {
    UUID gameId = UUID.randomUUID();
    when(tradeRepository.findByGameId(gameId)).thenReturn(List.of());

    List<Trade> result = adapter.findByGameId(gameId);

    assertThat(result).isEmpty();
    verify(tradeRepository).findByGameId(gameId);
  }

  @Test
  void findByGameIdAndStatusConvertsEnum() {
    UUID gameId = UUID.randomUUID();
    com.fortnite.pronos.model.Trade entity = buildEntity(UUID.randomUUID());
    when(tradeRepository.findByGameIdAndStatus(
            gameId, com.fortnite.pronos.model.Trade.Status.REJECTED))
        .thenReturn(List.of(entity));

    List<Trade> result = adapter.findByGameIdAndStatus(gameId, TradeStatus.REJECTED);

    assertThat(result).hasSize(1);
  }

  @Test
  void findTradesBetweenTeamsDelegates() {
    UUID t1 = UUID.randomUUID();
    UUID t2 = UUID.randomUUID();
    when(tradeRepository.findTradesBetweenTeams(t1, t2)).thenReturn(List.of());

    List<Trade> result = adapter.findTradesBetweenTeams(t1, t2);

    assertThat(result).isEmpty();
    verify(tradeRepository).findTradesBetweenTeams(t1, t2);
  }

  private Trade buildDomain(UUID tradeId) {
    return Trade.restore(
        tradeId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        List.of(UUID.randomUUID()),
        List.of(UUID.randomUUID()),
        TradeStatus.PENDING,
        LocalDateTime.now(),
        null,
        null,
        null,
        null);
  }

  private com.fortnite.pronos.model.Trade buildEntity(UUID tradeId) {
    Team teamA = new Team();
    teamA.setId(UUID.randomUUID());
    Team teamB = new Team();
    teamB.setId(UUID.randomUUID());
    com.fortnite.pronos.model.Trade entity = new com.fortnite.pronos.model.Trade();
    entity.setId(tradeId);
    entity.setFromTeam(teamA);
    entity.setToTeam(teamB);
    entity.setStatus(com.fortnite.pronos.model.Trade.Status.PENDING);
    entity.setProposedAt(LocalDateTime.now());
    return entity;
  }
}
