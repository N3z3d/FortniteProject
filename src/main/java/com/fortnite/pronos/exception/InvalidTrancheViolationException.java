package com.fortnite.pronos.exception;

/** Exception raised when a pick violates the tranche floor rule. */
public class InvalidTrancheViolationException extends RuntimeException {

  public InvalidTrancheViolationException(String message) {
    super(message);
  }
}
