package com.fortnite.pronos.domain.port.out;

public interface EpicIdValidatorPort {

  /**
   * Validates that the given Epic Games ID exists and is reachable.
   *
   * @param epicId the Epic Games account ID or username
   * @return true if the ID is valid and reachable
   */
  boolean validate(String epicId);
}
