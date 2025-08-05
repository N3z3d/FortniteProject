package com.fortnite.pronos.exception;

/** Exception levée lorsqu'un joueur essaie de picker alors que ce n'est pas son tour */
public class NotYourTurnException extends RuntimeException {

  public NotYourTurnException(String message) {
    super(message);
  }

  public NotYourTurnException(String currentPlayer, String attemptingPlayer) {
    super(
        String.format(
            "Ce n'est pas votre tour. Tour actuel : %s, Tentative par : %s",
            currentPlayer, attemptingPlayer));
  }
}
