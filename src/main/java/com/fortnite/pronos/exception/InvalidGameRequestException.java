package com.fortnite.pronos.exception;

/** Exception levée lorsqu'une requête de création de game est invalide */
public class InvalidGameRequestException extends RuntimeException {

  public InvalidGameRequestException(String message) {
    super(message);
  }

  public InvalidGameRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
