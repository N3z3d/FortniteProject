package com.fortnite.pronos.exception;

import com.fortnite.pronos.model.GameStatus;

/** Exception levée lorsqu'une game n'est pas dans un état valide pour l'opération */
public class InvalidGameStateException extends RuntimeException {

  public InvalidGameStateException(String message) {
    super(message);
  }

  public InvalidGameStateException(GameStatus currentStatus, GameStatus expectedStatus) {
    super(
        String.format(
            "État de game invalide. État actuel : %s, État attendu : %s",
            currentStatus, expectedStatus));
  }

  public InvalidGameStateException(GameStatus currentStatus, String operation) {
    super(
        String.format(
            "La game n'est pas ouverte aux inscriptions. État actuel : %s", currentStatus));
  }
}
