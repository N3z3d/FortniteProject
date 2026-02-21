package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fortnite.pronos.dto.player.PlayerDto;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour les rÃ©ponses de trade */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings({"java:S1168"})
public class TradeResponseDto {

  private UUID id;
  private TeamDto fromTeam;
  private TeamDto toTeam;
  private List<PlayerDto> offeredPlayers;
  private List<PlayerDto> requestedPlayers;
  private Trade.Status status;
  private LocalDateTime proposedAt;
  private LocalDateTime acceptedAt;
  private LocalDateTime rejectedAt;
  private LocalDateTime cancelledAt;
  private UUID originalTradeId;

  /**
   * Convertit une entitÃ© Trade en DTO
   *
   * @param trade l'entitÃ© Trade
   * @return TradeResponseDto
   */
  public static TradeResponseDto fromTrade(Trade trade) {
    if (trade == null) {
      return null;
    }

    return TradeResponseDto.builder()
        .id(trade.getId())
        .fromTeam(TeamDto.from(trade.getFromTeam()))
        .toTeam(TeamDto.from(trade.getToTeam()))
        .offeredPlayers(convertPlayersToDto(trade.getOfferedPlayers()))
        .requestedPlayers(convertPlayersToDto(trade.getRequestedPlayers()))
        .status(trade.getStatus())
        .proposedAt(trade.getProposedAt())
        .acceptedAt(trade.getAcceptedAt())
        .rejectedAt(trade.getRejectedAt())
        .cancelledAt(trade.getCancelledAt())
        .originalTradeId(trade.getOriginalTradeId())
        .build();
  }

  /**
   * Convertit une liste de joueurs en liste de DTOs
   *
   * @param players liste de joueurs
   * @return liste de PlayerDto
   */
  private static List<PlayerDto> convertPlayersToDto(List<Player> players) {
    if (players == null || players.isEmpty()) {
      return null;
    }
    return players.stream().map(PlayerDto::fromEntity).toList();
  }

  /**
   * VÃ©rifie si le trade est en cours (pending)
   *
   * @return true si le trade est en attente
   */
  public boolean isPending() {
    return status == Trade.Status.PENDING;
  }

  /**
   * VÃ©rifie si le trade est acceptÃ©
   *
   * @return true si le trade est acceptÃ©
   */
  public boolean isAccepted() {
    return status == Trade.Status.ACCEPTED;
  }

  /**
   * VÃ©rifie si le trade est rejetÃ©
   *
   * @return true si le trade est rejetÃ©
   */
  public boolean isRejected() {
    return status == Trade.Status.REJECTED;
  }

  /**
   * VÃ©rifie si le trade est annulÃ©
   *
   * @return true si le trade est annulÃ©
   */
  public boolean isCancelled() {
    return status == Trade.Status.CANCELLED;
  }

  /**
   * VÃ©rifie si le trade a Ã©tÃ© contrÃ©
   *
   * @return true si le trade a Ã©tÃ© contrÃ©
   */
  public boolean isCountered() {
    return status == Trade.Status.COUNTERED;
  }

  /**
   * VÃ©rifie si le trade est terminÃ© (acceptÃ©, rejetÃ©, ou annulÃ©)
   *
   * @return true si le trade est terminÃ©
   */
  public boolean isFinalized() {
    return status == Trade.Status.ACCEPTED
        || status == Trade.Status.REJECTED
        || status == Trade.Status.CANCELLED;
  }

  /**
   * Obtient la date de finalisation du trade
   *
   * @return date de finalisation ou null si non finalisÃ©
   */
  public LocalDateTime getFinalizationDate() {
    if (acceptedAt != null) {
      return acceptedAt;
    } else if (rejectedAt != null) {
      return rejectedAt;
    } else if (cancelledAt != null) {
      return cancelledAt;
    }
    return null;
  }

  /**
   * VÃ©rifie si c'est une contre-proposition
   *
   * @return true si c'est une contre-proposition
   */
  public boolean isCounterOffer() {
    return originalTradeId != null;
  }

  /**
   * Obtient le nombre total de joueurs impliquÃ©s
   *
   * @return nombre total de joueurs
   */
  public int getTotalPlayersCount() {
    int offered = offeredPlayers != null ? offeredPlayers.size() : 0;
    int requested = requestedPlayers != null ? requestedPlayers.size() : 0;
    return offered + requested;
  }

  /**
   * VÃ©rifie si le trade est Ã©quilibrÃ©
   *
   * @return true si le nombre de joueurs offerts Ã©gale le nombre demandÃ©
   */
  public boolean isBalanced() {
    int offered = offeredPlayers != null ? offeredPlayers.size() : 0;
    int requested = requestedPlayers != null ? requestedPlayers.size() : 0;
    return offered == requested;
  }
}
