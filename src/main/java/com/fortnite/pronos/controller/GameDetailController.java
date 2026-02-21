package com.fortnite.pronos.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fortnite.pronos.application.usecase.GameDetailUseCase;
import com.fortnite.pronos.dto.GameDetailDto;
import com.fortnite.pronos.exception.GameNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST pour les dÃ©tails des games Clean Code : ResponsabilitÃ© unique - exposition des
 * endpoints de dÃ©tails
 */
@Slf4j
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@SuppressWarnings({"java:S2259"})
public class GameDetailController {

  private static final String ERROR_KEY = "error";
  private static final String MESSAGE_KEY = "message";

  private final GameDetailUseCase gameDetailUseCase;

  /** RÃ©cupÃ¨re les dÃ©tails complets d'une game Clean Code : mÃ©thode simple et claire */
  @GetMapping("/{gameId}/details")
  public ResponseEntity<GameDetailDto> getGameDetails(@PathVariable UUID gameId) {
    log.debug("RÃ©cupÃ©ration des dÃ©tails de la game {}", gameId);

    GameDetailDto gameDetails = gameDetailUseCase.getGameDetails(gameId);

    log.info("DÃ©tails de la game {} rÃ©cupÃ©rÃ©s avec succÃ¨s", gameId);
    return ResponseEntity.ok(gameDetails);
  }

  /** Gestion des erreurs GameNotFoundException Clean Code : gestion centralisÃ©e des exceptions */
  @ExceptionHandler(GameNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public Map<String, String> handleGameNotFound(GameNotFoundException e) {
    log.error("Game non trouvÃ©e : {}", e.getMessage());
    return Map.of(ERROR_KEY, "GAME_NOT_FOUND", MESSAGE_KEY, e.getMessage());
  }

  /**
   * Gestion des erreurs de conversion d'UUID Clean Code : gestion spÃ©cifique des erreurs de type
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    if (e.getRequiredType() != null && e.getRequiredType().equals(UUID.class)) {
      log.error("UUID invalide : {}", e.getValue());
      return Map.of(
          ERROR_KEY, "INVALID_REQUEST",
          MESSAGE_KEY, "Format d'UUID invalide");
    }
    return Map.of(
        ERROR_KEY, "INVALID_REQUEST",
        MESSAGE_KEY, "Parametre invalide");
  }
}
