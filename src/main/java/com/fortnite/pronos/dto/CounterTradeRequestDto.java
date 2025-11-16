package com.fortnite.pronos.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour les contre-propositions de trade */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterTradeRequestDto {

  @NotEmpty(message = "Au moins un joueur doit être offert dans la contre-proposition")
  @Size(max = 5, message = "Maximum 5 joueurs peuvent être offerts")
  private List<UUID> offeredPlayerIds;

  @NotEmpty(message = "Au moins un joueur doit être demandé dans la contre-proposition")
  @Size(max = 5, message = "Maximum 5 joueurs peuvent être demandés")
  private List<UUID> requestedPlayerIds;

  /**
   * Valide la cohérence de la contre-proposition
   *
   * @return true si la contre-proposition est valide
   */
  public boolean isValid() {
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
   * Obtient le nombre total de joueurs impliqués dans la contre-proposition
   *
   * @return nombre total de joueurs
   */
  public int getTotalPlayersCount() {
    int offered = offeredPlayerIds != null ? offeredPlayerIds.size() : 0;
    int requested = requestedPlayerIds != null ? requestedPlayerIds.size() : 0;
    return offered + requested;
  }

  /**
   * Vérifie si la contre-proposition est équilibrée (même nombre de joueurs de chaque côté)
   *
   * @return true si la contre-proposition est équilibrée
   */
  public boolean isBalanced() {
    int offered = offeredPlayerIds != null ? offeredPlayerIds.size() : 0;
    int requested = requestedPlayerIds != null ? requestedPlayerIds.size() : 0;
    return offered == requested;
  }

  /**
   * Convertit en TradeRequestDto pour réutiliser la logique existante
   *
   * @param fromTeamId équipe qui fait la contre-proposition
   * @param toTeamId équipe qui reçoit la contre-proposition
   * @return TradeRequestDto équivalent
   */
  public TradeRequestDto toTradeRequestDto(UUID fromTeamId, UUID toTeamId) {
    return TradeRequestDto.builder()
        .fromTeamId(fromTeamId)
        .toTeamId(toTeamId)
        .offeredPlayerIds(this.offeredPlayerIds)
        .requestedPlayerIds(this.requestedPlayerIds)
        .build();
  }
}
