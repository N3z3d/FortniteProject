package com.fortnite.pronos.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les demandes d'échange de joueurs entre équipes Utilisé dans le système de trading des
 * joueurs de fantasy league
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwapPlayersRequest {

  @NotNull(message = "L'ID de l'équipe est obligatoire") private UUID teamId;

  @NotNull(message = "L'ID de la première équipe est obligatoire") private UUID teamId1;

  @NotNull(message = "L'ID de la seconde équipe est obligatoire") private UUID teamId2;

  @NotNull(message = "L'ID du premier joueur est obligatoire") private UUID playerId1;

  @NotNull(message = "L'ID du second joueur est obligatoire") private UUID playerId2;

  /** Position du premier joueur dans l'équipe (optionnel) */
  private Integer position1;

  /** Position du second joueur dans l'équipe (optionnel) */
  private Integer position2;

  /** Commentaire sur l'échange (optionnel) */
  private String comment;

  /** Constructeur avec les paramètres essentiels */
  public SwapPlayersRequest(UUID teamId1, UUID teamId2, UUID playerId1, UUID playerId2) {
    this.teamId1 = teamId1;
    this.teamId2 = teamId2;
    this.teamId = teamId1; // Pour compatibilité
    this.playerId1 = playerId1;
    this.playerId2 = playerId2;
  }
}
