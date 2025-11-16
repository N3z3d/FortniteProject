package com.fortnite.pronos.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.Trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour les demandes de trade */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequestDto {

  @NotNull(message = "L'équipe source est requise") private UUID fromTeamId;

  @NotNull(message = "L'équipe cible est requise") private UUID toTeamId;

  @NotEmpty(message = "Au moins un joueur doit être offert")
  @Size(max = 5, message = "Maximum 5 joueurs peuvent être offerts")
  private List<UUID> offeredPlayerIds;

  @NotEmpty(message = "Au moins un joueur doit être demandé")
  @Size(max = 5, message = "Maximum 5 joueurs peuvent être demandés")
  private List<UUID> requestedPlayerIds;

  /**
   * Convertit le DTO en entité Trade
   *
   * @param fromTeam Équipe source
   * @param toTeam Équipe cible
   * @param offeredPlayers Joueurs offerts
   * @param requestedPlayers Joueurs demandés
   * @return Trade entity
   */
  public Trade toTrade(
      Team fromTeam, Team toTeam, List<Player> offeredPlayers, List<Player> requestedPlayers) {
    return Trade.builder()
        .fromTeam(fromTeam)
        .toTeam(toTeam)
        .offeredPlayers(offeredPlayers)
        .requestedPlayers(requestedPlayers)
        .status(Trade.Status.PENDING)
        .build();
  }

  /**
   * Valide la cohérence de la requête
   *
   * @return true si la requête est valide
   */
  public boolean isValid() {
    // Vérifier que les équipes sont différentes
    if (fromTeamId != null && fromTeamId.equals(toTeamId)) {
      return false;
    }

    // Vérifier qu'il n'y a pas de doublons dans les joueurs offerts
    if (offeredPlayerIds != null
        && offeredPlayerIds.stream().distinct().count() != offeredPlayerIds.size()) {
      return false;
    }

    // Vérifier qu'il n'y a pas de doublons dans les joueurs demandés
    if (requestedPlayerIds != null
        && requestedPlayerIds.stream().distinct().count() != requestedPlayerIds.size()) {
      return false;
    }

    // Vérifier qu'un joueur n'est pas à la fois offert et demandé
    if (offeredPlayerIds != null && requestedPlayerIds != null) {
      return offeredPlayerIds.stream().noneMatch(requestedPlayerIds::contains);
    }

    return true;
  }

  /**
   * Obtient le nombre total de joueurs impliqués dans le trade
   *
   * @return nombre total de joueurs
   */
  public int getTotalPlayersCount() {
    int offered = offeredPlayerIds != null ? offeredPlayerIds.size() : 0;
    int requested = requestedPlayerIds != null ? requestedPlayerIds.size() : 0;
    return offered + requested;
  }

  /**
   * Vérifie si le trade est équilibré (même nombre de joueurs de chaque côté)
   *
   * @return true si le trade est équilibré
   */
  public boolean isBalanced() {
    int offered = offeredPlayerIds != null ? offeredPlayerIds.size() : 0;
    int requested = requestedPlayerIds != null ? requestedPlayerIds.size() : 0;
    return offered == requested;
  }
}
