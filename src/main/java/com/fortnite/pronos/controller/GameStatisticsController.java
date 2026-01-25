package com.fortnite.pronos.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.application.usecase.GameStatisticsUseCase;
import com.fortnite.pronos.model.Player;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST pour les statistiques des games Clean Code : Responsabilité unique - exposition
 * des endpoints de statistiques
 */
@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}/statistics")
@RequiredArgsConstructor
public class GameStatisticsController {

  private final GameStatisticsUseCase gameStatisticsUseCase;

  /** Récupère la distribution des joueurs par région Clean Code : méthode courte, nom explicite */
  @GetMapping("/region-distribution")
  public ResponseEntity<Map<Player.Region, Integer>> getRegionDistribution(
      @PathVariable UUID gameId) {

    log.debug("Récupération de la distribution par région pour la game {}", gameId);

    Map<Player.Region, Integer> distribution =
        gameStatisticsUseCase.getPlayerDistributionByRegion(gameId);
    return ResponseEntity.ok(distribution);
  }

  /**
   * Récupère la distribution des joueurs par région en pourcentage Clean Code : méthode focalisée
   * sur une seule responsabilité
   */
  @GetMapping("/region-distribution-percentage")
  public ResponseEntity<Map<Player.Region, Double>> getRegionDistributionPercentage(
      @PathVariable UUID gameId) {

    log.debug("Récupération de la distribution en pourcentage pour la game {}", gameId);

    Map<Player.Region, Double> percentages =
        gameStatisticsUseCase.getPlayerDistributionByRegionPercentage(gameId);
    return ResponseEntity.ok(percentages);
  }

  /**
   * Gestion centralisée des erreurs Clean Code : DRY - évite la duplication du code de gestion
   * d'erreur
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleGameNotFound(IllegalArgumentException e) {
    log.error("Erreur : {}", e.getMessage());
    return Map.of("error", "NOT_FOUND", "message", e.getMessage());
  }
}
