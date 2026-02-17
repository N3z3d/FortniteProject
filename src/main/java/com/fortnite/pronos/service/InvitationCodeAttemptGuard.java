package com.fortnite.pronos.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.core.error.ErrorCode;
import com.fortnite.pronos.core.error.FortnitePronosException;

/**
 * Lightweight in-memory throttle for join-with-code attempts to reduce brute-force guessing.
 *
 * <p>Scope is intentionally narrow: protect invitation-code lookup endpoint without adding external
 * infrastructure.
 */
@Component
public class InvitationCodeAttemptGuard {

  private static final int MAX_ATTEMPTS_PER_MINUTE = 15;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final ConcurrentMap<String, Deque<Instant>> attemptsByActor = new ConcurrentHashMap<>();
  private final Clock clock;

  public InvitationCodeAttemptGuard() {
    this(Clock.systemUTC());
  }

  InvitationCodeAttemptGuard(Clock clock) {
    this.clock = clock;
  }

  /** Records an attempt for actor and throws 429-equivalent exception if threshold is exceeded. */
  public void registerAttemptOrThrow(UUID userId, String remoteAddress) {
    String actorKey = buildActorKey(userId, remoteAddress);
    Instant now = clock.instant();
    Deque<Instant> attempts =
        attemptsByActor.computeIfAbsent(actorKey, key -> new ConcurrentLinkedDeque<>());

    synchronized (attempts) {
      Instant cutoff = now.minus(WINDOW);
      while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
        attempts.pollFirst();
      }

      if (attempts.size() >= MAX_ATTEMPTS_PER_MINUTE) {
        throw new FortnitePronosException(
                ErrorCode.SYS_004,
                "Trop de tentatives de code d'invitation. Reessayez dans une minute.")
            .addContext("actorKey", actorKey)
            .addContext("limitPerMinute", MAX_ATTEMPTS_PER_MINUTE);
      }

      attempts.addLast(now);
    }
  }

  private String buildActorKey(UUID userId, String remoteAddress) {
    String userPart = userId != null ? userId.toString() : "anonymous";
    String ipPart = normalizeRemoteAddress(remoteAddress);
    return userPart + "|" + ipPart;
  }

  private String normalizeRemoteAddress(String remoteAddress) {
    if (remoteAddress == null || remoteAddress.isBlank()) {
      return "unknown";
    }
    return remoteAddress.trim();
  }
}
