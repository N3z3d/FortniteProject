package com.fortnite.pronos.exception;

/** Exception levée lorsqu'on essaie de sélectionner un joueur déjà pris */
public class PlayerAlreadySelectedException extends RuntimeException {

  public PlayerAlreadySelectedException(String message) {
    super(message);
  }

  public PlayerAlreadySelectedException(String playerName, String teamName) {
    super(String.format("Le joueur %s a déjà été sélectionné par %s", playerName, teamName));
  }
}
