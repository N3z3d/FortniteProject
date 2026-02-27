package com.fortnite.pronos.exception;

public class InvalidEpicIdException extends RuntimeException {

  public InvalidEpicIdException(String epicId) {
    super("Epic ID introuvable sur les serveurs Fortnite: " + epicId);
  }
}
