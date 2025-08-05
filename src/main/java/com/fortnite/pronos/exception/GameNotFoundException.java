package com.fortnite.pronos.exception;

import java.util.UUID;

/** Exception levée lorsqu'une game n'est pas trouvée */
public class GameNotFoundException extends RuntimeException {

  public GameNotFoundException(String message) {
    super(message);
  }

  public GameNotFoundException(UUID gameId) {
    super("Game non trouvée : " + gameId);
  }

  public GameNotFoundException(String invitationCode, boolean byCode) {
    super("Game non trouvée avec le code d'invitation : " + invitationCode);
  }
}
