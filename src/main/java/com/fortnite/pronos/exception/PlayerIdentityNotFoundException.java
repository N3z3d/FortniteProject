package com.fortnite.pronos.exception;

import java.util.UUID;

public class PlayerIdentityNotFoundException extends RuntimeException {

  public PlayerIdentityNotFoundException(UUID playerId) {
    super("No identity pipeline entry found for player: " + playerId);
  }
}
