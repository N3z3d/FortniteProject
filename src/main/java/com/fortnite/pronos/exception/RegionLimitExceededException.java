package com.fortnite.pronos.exception;

import com.fortnite.pronos.model.Player;

/** Exception levée lorsque la limite de joueurs par région est dépassée */
public class RegionLimitExceededException extends RuntimeException {

  public RegionLimitExceededException(String message) {
    super(message);
  }

  public RegionLimitExceededException(Player.Region region, int limit, int current) {
    super(String.format("Limite de joueurs %s atteinte : %d/%d", region, current, limit));
  }
}
