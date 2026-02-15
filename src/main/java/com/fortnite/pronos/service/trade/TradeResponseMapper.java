package com.fortnite.pronos.service.trade;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.dto.TradeResponseDto;
import com.fortnite.pronos.dto.player.PlayerDto;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

/**
 * Maps domain Trade to TradeResponseDto by resolving Team and Player entities from persistence.
 * Lives in the service layer to comply with architecture rules (controllers cannot access
 * repositories directly).
 */
@Component
@RequiredArgsConstructor
public class TradeResponseMapper {

  private final TeamRepository teamRepository;
  private final PlayerRepository playerRepository;

  /** Maps a domain Trade to a TradeResponseDto with hydrated Team and Player data. */
  public TradeResponseDto toDto(Trade trade) {
    if (trade == null) {
      return null;
    }

    TeamDto fromTeam = resolveTeamDto(trade.getFromTeamId());
    TeamDto toTeam = resolveTeamDto(trade.getToTeamId());
    List<PlayerDto> offeredPlayers = resolvePlayerDtos(trade.getOfferedPlayerIds());
    List<PlayerDto> requestedPlayers = resolvePlayerDtos(trade.getRequestedPlayerIds());

    return TradeResponseDto.builder()
        .id(trade.getId())
        .fromTeam(fromTeam)
        .toTeam(toTeam)
        .offeredPlayers(offeredPlayers)
        .requestedPlayers(requestedPlayers)
        .status(toLegacyStatus(trade.getStatus()))
        .proposedAt(trade.getProposedAt())
        .acceptedAt(trade.getAcceptedAt())
        .rejectedAt(trade.getRejectedAt())
        .cancelledAt(trade.getCancelledAt())
        .originalTradeId(trade.getOriginalTradeId())
        .build();
  }

  /** Maps a list of domain Trades to TradeResponseDtos. */
  public List<TradeResponseDto> toDtos(List<Trade> trades) {
    if (trades == null || trades.isEmpty()) {
      return List.of();
    }
    return trades.stream().map(this::toDto).toList();
  }

  private TeamDto resolveTeamDto(UUID teamId) {
    if (teamId == null) {
      return null;
    }
    return teamRepository.findById(teamId).map(TeamDto::from).orElse(null);
  }

  private List<PlayerDto> resolvePlayerDtos(List<UUID> playerIds) {
    if (playerIds == null || playerIds.isEmpty()) {
      return null;
    }
    List<Player> players = playerRepository.findAllById(playerIds);
    return players.stream().map(PlayerDto::fromEntity).toList();
  }

  private com.fortnite.pronos.model.Trade.Status toLegacyStatus(TradeStatus status) {
    if (status == null) {
      return com.fortnite.pronos.model.Trade.Status.PENDING;
    }
    return com.fortnite.pronos.model.Trade.Status.valueOf(status.name());
  }
}
