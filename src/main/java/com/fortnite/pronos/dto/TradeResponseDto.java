package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fortnite.pronos.dto.player.PlayerDto;
import com.fortnite.pronos.dto.team.TeamDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour les réponses de trade */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
   * Convertit une entité Trade en DTO
   *
   * @param trade l'entité Trade
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
    return players.stream().map(PlayerDto::fromEntity).collect(Collectors.toList());
  }

  /**
   * Vérifie si le trade est en cours (pending)
   *
   * @return true si le trade est en attente
   */
  public boolean isPending() {
    return status == Trade.Status.PENDING;
  }

  /**
   * Vérifie si le trade est accepté
   *
   * @return true si le trade est accepté
   */
  public boolean isAccepted() {
    return status == Trade.Status.ACCEPTED;
  }

  /**
   * Vérifie si le trade est rejeté
   *
   * @return true si le trade est rejeté
   */
  public boolean isRejected() {
    return status == Trade.Status.REJECTED;
  }

  /**
   * Vérifie si le trade est annulé
   *
   * @return true si le trade est annulé
   */
  public boolean isCancelled() {
    return status == Trade.Status.CANCELLED;
  }

  /**
   * Vérifie si le trade a été contré
   *
   * @return true si le trade a été contré
   */
  public boolean isCountered() {
    return status == Trade.Status.COUNTERED;
  }

  /**
   * Vérifie si le trade est terminé (accepté, rejeté, ou annulé)
   *
   * @return true si le trade est terminé
   */
  public boolean isFinalized() {
    return status == Trade.Status.ACCEPTED
        || status == Trade.Status.REJECTED
        || status == Trade.Status.CANCELLED;
  }

  /**
   * Obtient la date de finalisation du trade
   *
   * @return date de finalisation ou null si non finalisé
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
   * Vérifie si c'est une contre-proposition
   *
   * @return true si c'est une contre-proposition
   */
  public boolean isCounterOffer() {
    return originalTradeId != null;
  }

  /**
   * Obtient le nombre total de joueurs impliqués
   *
   * @return nombre total de joueurs
   */
  public int getTotalPlayersCount() {
    int offered = offeredPlayers != null ? offeredPlayers.size() : 0;
    int requested = requestedPlayers != null ? requestedPlayers.size() : 0;
    return offered + requested;
  }

  /**
   * Vérifie si le trade est équilibré
   *
   * @return true si le nombre de joueurs offerts égale le nombre demandé
   */
  public boolean isBalanced() {
    int offered = offeredPlayers != null ? offeredPlayers.size() : 0;
    int requested = requestedPlayers != null ? requestedPlayers.size() : 0;
    return offered == requested;
  }
}
