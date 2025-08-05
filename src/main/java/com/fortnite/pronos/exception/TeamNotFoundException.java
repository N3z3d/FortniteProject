package com.fortnite.pronos.exception;

/** Exception levée quand une équipe n'est pas trouvée */
public class TeamNotFoundException extends RuntimeException {

  public TeamNotFoundException(String message) {
    super(message);
  }

  public TeamNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
