package com.fortnite.pronos.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fortnite.pronos.dto.GameDetailDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.service.GameDetailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST pour les détails des games Clean Code : Responsabilité unique - exposition des
 * endpoints de détails
 */
@Slf4j
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameDetailController {

  private final GameDetailService gameDetailService;

  /** Récupère les détails complets d'une game Clean Code : méthode simple et claire */
  @GetMapping("/{gameId}/details")
  public ResponseEntity<GameDetailDto> getGameDetails(@PathVariable UUID gameId) {
    log.debug("Récupération des détails de la game {}", gameId);

    GameDetailDto gameDetails = gameDetailService.getGameDetails(gameId);

    log.info("Détails de la game {} récupérés avec succès", gameId);
    return ResponseEntity.ok(gameDetails);
  }

  /** Gestion des erreurs GameNotFoundException Clean Code : gestion centralisée des exceptions */
  @ExceptionHandler(GameNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleGameNotFound(GameNotFoundException e) {
    log.error("Game non trouvée : {}", e.getMessage());
    return Map.of("error", "GAME_NOT_FOUND", "message", e.getMessage());
  }

  /**
   * Gestion des erreurs de conversion d'UUID Clean Code : gestion spécifique des erreurs de type
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    if (e.getRequiredType() != null && e.getRequiredType().equals(UUID.class)) {
      log.error("UUID invalide : {}", e.getValue());
      return Map.of(
          "error", "INVALID_REQUEST",
          "message", "Format d'UUID invalide");
    }
    return Map.of(
        "error", "INVALID_REQUEST",
        "message", "Paramètre invalide");
  }
}
