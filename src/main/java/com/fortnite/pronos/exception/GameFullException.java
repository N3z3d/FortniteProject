package com.fortnite.pronos.exception;

/** Exception levée lorsqu'une game est pleine */
public class GameFullException extends RuntimeException {

  public GameFullException(String message) {
    super(message);
  }

  public GameFullException(String gameName, int maxParticipants) {
    super(
        String.format(
            "La game '%s' est pleine (%d participants maximum)", gameName, maxParticipants));
  }
}
