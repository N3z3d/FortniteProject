package com.fortnite.pronos.exception;

/** Exception levée lors d'une tentative d'échange invalide */
public class InvalidSwapException extends RuntimeException {

  public InvalidSwapException(String message) {
    super(message);
  }

  public InvalidSwapException(String message, Throwable cause) {
    super(message, cause);
  }
}
