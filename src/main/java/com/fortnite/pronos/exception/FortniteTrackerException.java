package com.fortnite.pronos.exception;

/** Exception spécifique pour les erreurs liées à l'API FortniteTracker */
public class FortniteTrackerException extends RuntimeException {

  public FortniteTrackerException(String message) {
    super(message);
  }

  public FortniteTrackerException(String message, Throwable cause) {
    super(message, cause);
  }

  public FortniteTrackerException(Throwable cause) {
    super(cause);
  }
}
