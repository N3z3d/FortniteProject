package com.fortnite.pronos.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fortnite.pronos.core.error.ErrorCode;
import com.fortnite.pronos.core.error.FortnitePronosException;

class InvitationCodeAttemptGuardTest {

  @Test
  void shouldAllowAttemptsWithinLimit() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-13T19:00:00Z"));
    InvitationCodeAttemptGuard guard = new InvitationCodeAttemptGuard(clock);
    UUID userId = UUID.randomUUID();

    for (int i = 0; i < 15; i++) {
      assertDoesNotThrow(() -> guard.registerAttemptOrThrow(userId, "127.0.0.1"));
      clock.plusSeconds(2);
    }
  }

  @Test
  void shouldBlockWhenLimitExceededWithinSameMinute() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-13T19:10:00Z"));
    InvitationCodeAttemptGuard guard = new InvitationCodeAttemptGuard(clock);
    UUID userId = UUID.randomUUID();

    for (int i = 0; i < 15; i++) {
      guard.registerAttemptOrThrow(userId, "127.0.0.1");
    }

    FortnitePronosException ex =
        assertThrows(
            FortnitePronosException.class, () -> guard.registerAttemptOrThrow(userId, "127.0.0.1"));
    assertEquals(ErrorCode.SYS_004, ex.getErrorCode());
  }

  @Test
  void shouldAllowAgainAfterWindowExpires() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-13T20:00:00Z"));
    InvitationCodeAttemptGuard guard = new InvitationCodeAttemptGuard(clock);
    UUID userId = UUID.randomUUID();

    for (int i = 0; i < 15; i++) {
      guard.registerAttemptOrThrow(userId, "127.0.0.1");
    }

    clock.plusSeconds(61);
    assertDoesNotThrow(() -> guard.registerAttemptOrThrow(userId, "127.0.0.1"));
  }

  private static final class MutableClock extends Clock {
    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    Instant instant() {
      return instant;
    }

    private void plusSeconds(long seconds) {
      instant = instant.plusSeconds(seconds);
    }
  }
}
