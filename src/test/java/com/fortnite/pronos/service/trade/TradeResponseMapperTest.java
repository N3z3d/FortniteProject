package com.fortnite.pronos.service.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.dto.TradeResponseDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradeResponseMapper - Domain Trade to DTO mapping")
class TradeResponseMapperTest {

  @Mock private TeamRepository teamRepository;
  @Mock private PlayerRepository playerRepository;

  @InjectMocks private TradeResponseMapper mapper;

  @Test
  @DisplayName("toDto maps domain trade with resolved teams and players")
  void toDtoMapsDomainTradeWithResolvedData() {
    UUID tradeId = UUID.randomUUID();
    UUID fromTeamId = UUID.randomUUID();
    UUID toTeamId = UUID.randomUUID();
    UUID offeredPlayerId = UUID.randomUUID();
    UUID requestedPlayerId = UUID.randomUUID();
    LocalDateTime proposedAt = LocalDateTime.of(2026, 2, 14, 10, 0);

    Trade domainTrade =
        Trade.restore(
            tradeId,
            fromTeamId,
            toTeamId,
            List.of(offeredPlayerId),
            List.of(requestedPlayerId),
            TradeStatus.PENDING,
            proposedAt,
            null,
            null,
            null,
            null);

    Team fromTeam = new Team();
    fromTeam.setId(fromTeamId);
    fromTeam.setName("Team Alpha");

    Team toTeam = new Team();
    toTeam.setId(toTeamId);
    toTeam.setName("Team Beta");

    Player offeredPlayer = new Player();
    offeredPlayer.setId(offeredPlayerId);
    offeredPlayer.setNickname("PlayerA");

    Player requestedPlayer = new Player();
    requestedPlayer.setId(requestedPlayerId);
    requestedPlayer.setNickname("PlayerB");

    when(teamRepository.findById(fromTeamId)).thenReturn(Optional.of(fromTeam));
    when(teamRepository.findById(toTeamId)).thenReturn(Optional.of(toTeam));
    when(playerRepository.findAllById(List.of(offeredPlayerId))).thenReturn(List.of(offeredPlayer));
    when(playerRepository.findAllById(List.of(requestedPlayerId)))
        .thenReturn(List.of(requestedPlayer));

    TradeResponseDto dto = mapper.toDto(domainTrade);

    assertThat(dto.getId()).isEqualTo(tradeId);
    assertThat(dto.getFromTeam()).isNotNull();
    assertThat(dto.getFromTeam().getName()).isEqualTo("Team Alpha");
    assertThat(dto.getToTeam()).isNotNull();
    assertThat(dto.getToTeam().getName()).isEqualTo("Team Beta");
    assertThat(dto.getOfferedPlayers()).hasSize(1);
    assertThat(dto.getRequestedPlayers()).hasSize(1);
    assertThat(dto.getStatus()).isEqualTo(com.fortnite.pronos.model.Trade.Status.PENDING);
    assertThat(dto.getProposedAt()).isEqualTo(proposedAt);
  }

  @Test
  @DisplayName("toDto returns null for null input")
  void toDtoReturnsNullForNullInput() {
    assertThat(mapper.toDto(null)).isNull();
  }

  @Test
  @DisplayName("toDto handles missing team gracefully")
  void toDtoHandlesMissingTeam() {
    UUID tradeId = UUID.randomUUID();
    UUID fromTeamId = UUID.randomUUID();
    UUID toTeamId = UUID.randomUUID();

    Trade domainTrade =
        Trade.restore(
            tradeId,
            fromTeamId,
            toTeamId,
            List.of(UUID.randomUUID()),
            List.of(UUID.randomUUID()),
            TradeStatus.ACCEPTED,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            null,
            null);

    when(teamRepository.findById(fromTeamId)).thenReturn(Optional.empty());
    when(teamRepository.findById(toTeamId)).thenReturn(Optional.empty());
    when(playerRepository.findAllById(domainTrade.getOfferedPlayerIds())).thenReturn(List.of());
    when(playerRepository.findAllById(domainTrade.getRequestedPlayerIds())).thenReturn(List.of());

    TradeResponseDto dto = mapper.toDto(domainTrade);

    assertThat(dto.getId()).isEqualTo(tradeId);
    assertThat(dto.getFromTeam()).isNull();
    assertThat(dto.getToTeam()).isNull();
    assertThat(dto.getStatus()).isEqualTo(com.fortnite.pronos.model.Trade.Status.ACCEPTED);
  }

  @Test
  @DisplayName("toDtos maps list of domain trades")
  void toDtosMapsListOfDomainTrades() {
    UUID tradeId1 = UUID.randomUUID();
    UUID tradeId2 = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Trade trade1 =
        Trade.restore(
            tradeId1,
            teamId,
            UUID.randomUUID(),
            List.of(UUID.randomUUID()),
            List.of(UUID.randomUUID()),
            TradeStatus.PENDING,
            LocalDateTime.now(),
            null,
            null,
            null,
            null);
    Trade trade2 =
        Trade.restore(
            tradeId2,
            UUID.randomUUID(),
            teamId,
            List.of(UUID.randomUUID()),
            List.of(UUID.randomUUID()),
            TradeStatus.REJECTED,
            LocalDateTime.now(),
            null,
            LocalDateTime.now(),
            null,
            null);

    when(teamRepository.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
    when(playerRepository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

    List<TradeResponseDto> dtos = mapper.toDtos(List.of(trade1, trade2));

    assertThat(dtos).hasSize(2);
    assertThat(dtos.get(0).getId()).isEqualTo(tradeId1);
    assertThat(dtos.get(1).getId()).isEqualTo(tradeId2);
  }

  @Test
  @DisplayName("toDtos returns empty list for empty input")
  void toDtosReturnsEmptyListForEmptyInput() {
    assertThat(mapper.toDtos(List.of())).isEmpty();
    assertThat(mapper.toDtos(null)).isEmpty();
  }

  @Test
  @DisplayName("toDto maps all trade statuses correctly")
  void toDtoMapsAllTradeStatusesCorrectly() {
    for (TradeStatus status : TradeStatus.values()) {
      Trade trade =
          Trade.restore(
              UUID.randomUUID(),
              UUID.randomUUID(),
              UUID.randomUUID(),
              List.of(UUID.randomUUID()),
              List.of(UUID.randomUUID()),
              status,
              LocalDateTime.now(),
              null,
              null,
              null,
              null);

      when(teamRepository.findById(org.mockito.ArgumentMatchers.any()))
          .thenReturn(Optional.empty());
      when(playerRepository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

      TradeResponseDto dto = mapper.toDto(trade);

      assertThat(dto.getStatus().name()).isEqualTo(status.name());
    }
  }

  @Test
  @DisplayName("toDto preserves originalTradeId for counter-offers")
  void toDtoPreservesOriginalTradeIdForCounterOffers() {
    UUID originalId = UUID.randomUUID();
    Trade trade =
        Trade.restore(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            List.of(UUID.randomUUID()),
            List.of(UUID.randomUUID()),
            TradeStatus.PENDING,
            LocalDateTime.now(),
            null,
            null,
            null,
            originalId);

    when(teamRepository.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
    when(playerRepository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

    TradeResponseDto dto = mapper.toDto(trade);

    assertThat(dto.getOriginalTradeId()).isEqualTo(originalId);
  }
}
