package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour les réponses d'échange de joueurs Contient le résultat de l'opération d'échange */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapPlayersResponse {

  /** Indique si l'échange a réussi */
  private boolean success;

  /** Message de statut de l'échange */
  private String message;

  /** ID de l'équipe concernée */
  private UUID teamId;

  /** ID du premier joueur échangé */
  private UUID playerId1;

  /** ID du second joueur échangé */
  private UUID playerId2;

  /** Timestamp de l'échange */
  private LocalDateTime exchangeTimestamp;

  /** Code d'erreur en cas d'échec (optionnel) */
  private String errorCode;

  /** Détails supplémentaires sur l'échange */
  private String details;

  /** Détails de l'échange */
  private SwapDetails swapDetails;

  /** Constructeur pour succès */
  public static SwapPlayersResponse success(
      UUID teamId, UUID playerId1, UUID playerId2, String message) {
    return SwapPlayersResponse.builder()
        .success(true)
        .teamId(teamId)
        .playerId1(playerId1)
        .playerId2(playerId2)
        .message(message)
        .exchangeTimestamp(LocalDateTime.now())
        .build();
  }

  /** Constructeur pour échec */
  public static SwapPlayersResponse failure(String message, String errorCode) {
    return SwapPlayersResponse.builder()
        .success(false)
        .message(message)
        .errorCode(errorCode)
        .exchangeTimestamp(LocalDateTime.now())
        .build();
  }

  /** Classe interne pour les détails de l'échange */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class SwapDetails {
    private UUID fromTeamId;
    private UUID toTeamId;
    private String fromPlayerName;
    private String toPlayerName;
    private Integer fromPosition;
    private Integer toPosition;
    private String swapReason;
    private LocalDateTime processedAt;

    /** Créer des détails d'échange */
    public static SwapDetails create(
        UUID fromTeamId, UUID toTeamId, String fromPlayerName, String toPlayerName) {
      return SwapDetails.builder()
          .fromTeamId(fromTeamId)
          .toTeamId(toTeamId)
          .fromPlayerName(fromPlayerName)
          .toPlayerName(toPlayerName)
          .processedAt(LocalDateTime.now())
          .build();
    }
  }
}
