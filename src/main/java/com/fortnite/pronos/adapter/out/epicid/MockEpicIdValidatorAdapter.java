package com.fortnite.pronos.adapter.out.epicid;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.port.out.EpicIdValidatorPort;

/**
 * Mock implementation of EpicIdValidatorPort. Considers any non-blank Epic ID as valid. Swap this
 * with a real Fortnite API adapter when the API key is available.
 */
@Component
public class MockEpicIdValidatorAdapter implements EpicIdValidatorPort {

  @Override
  public boolean validate(String epicId) {
    return epicId != null && !epicId.isBlank();
  }
}
