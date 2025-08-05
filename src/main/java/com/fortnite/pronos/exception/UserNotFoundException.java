package com.fortnite.pronos.exception;

import java.util.UUID;

/** Exception levée lorsqu'un utilisateur n'est pas trouvé */
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(UUID userId) {
    super("Utilisateur non trouvé : " + userId);
  }

  public UserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
