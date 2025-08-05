package com.fortnite.pronos.exception;

/** Exception levée lorsqu'un utilisateur tente une action non autorisée */
public class UnauthorizedAccessException extends RuntimeException {

  public UnauthorizedAccessException(String message) {
    super(message);
  }

  public UnauthorizedAccessException(String action, String reason) {
    super(String.format("Action non autorisée : %s. Raison : %s", action, reason));
  }
}
