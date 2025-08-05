package com.fortnite.pronos.exception;

/** Exception levée lorsqu'un utilisateur essaie de rejoindre une game où il est déjà */
public class UserAlreadyInGameException extends RuntimeException {

  public UserAlreadyInGameException(String message) {
    super(message);
  }

  public UserAlreadyInGameException(String username, String gameName) {
    super(String.format("L'utilisateur %s est déjà dans la game '%s'", username, gameName));
  }
}
