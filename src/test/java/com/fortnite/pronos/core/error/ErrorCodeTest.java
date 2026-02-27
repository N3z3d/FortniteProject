package com.fortnite.pronos.core.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

  @Test
  void shouldReturnBadRequestForValidationError() {
    assertThat(ErrorCode.AUTH_VAL_001.getStatusCode()).isEqualTo(400);
  }

  @Test
  void shouldReturnUnauthorizedForAuthenticationError() {
    assertThat(ErrorCode.AUTH_SEC_001.getStatusCode()).isEqualTo(401);
  }

  @Test
  void shouldReturnForbiddenForAccountAccessError() {
    assertThat(ErrorCode.AUTH_BUS_002.getStatusCode()).isEqualTo(403);
  }

  @Test
  void shouldReturnNotFoundForMissingDomainEntity() {
    assertThat(ErrorCode.PLAYER_BUS_001.getStatusCode()).isEqualTo(404);
  }

  @Test
  void shouldReturnConflictForDuplicateEntity() {
    assertThat(ErrorCode.PLAYER_BUS_002.getStatusCode()).isEqualTo(409);
  }

  @Test
  void shouldReturnServiceUnavailableForUnavailableSystem() {
    assertThat(ErrorCode.SYS_002.getStatusCode()).isEqualTo(503);
  }

  @Test
  void shouldReturnTooManyRequestsForRateLimit() {
    assertThat(ErrorCode.SYS_004.getStatusCode()).isEqualTo(429);
  }

  @Test
  void shouldReturnInternalServerErrorForUnmappedSystemError() {
    assertThat(ErrorCode.SYS_003.getStatusCode()).isEqualTo(500);
  }

  @Test
  void shouldReturnBadRequestForUnmappedBusinessError() {
    assertThat(ErrorCode.TEAM_BUS_005.getStatusCode()).isEqualTo(400);
  }
}
