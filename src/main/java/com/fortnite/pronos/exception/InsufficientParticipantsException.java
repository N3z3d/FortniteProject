package com.fortnite.pronos.exception;

/** Exception levée lorsqu'il n'y a pas assez de participants pour démarrer une action */
public class InsufficientParticipantsException extends RuntimeException {

  public InsufficientParticipantsException(String message) {
    super(message);
  }

  public InsufficientParticipantsException(int current, int minimum) {
    super(
        String.format(
            "Nombre insuffisant de participants : %d actuellement, minimum %d requis",
            current, minimum));
  }
}
