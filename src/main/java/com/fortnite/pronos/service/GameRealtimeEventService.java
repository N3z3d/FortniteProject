package com.fortnite.pronos.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameRealtimeEventService {

  public static final String CONNECTED = "CONNECTED";
  public static final String GAME_DELETED = "GAME_DELETED";
  public static final String GAME_JOINED = "GAME_JOINED";
  public static final String GAME_LEFT = "GAME_LEFT";
  public static final String GAME_UPDATED = "GAME_UPDATED";

  private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

  private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByUserId =
      new ConcurrentHashMap<>();

  public SseEmitter subscribe(UUID userId) {
    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
    emittersByUserId.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

    emitter.onCompletion(() -> removeEmitter(userId, emitter));
    emitter.onTimeout(
        () -> {
          emitter.complete();
          removeEmitter(userId, emitter);
        });
    emitter.onError(error -> removeEmitter(userId, emitter));

    sendEvent(userId, emitter, CONNECTED, null);
    return emitter;
  }

  public void publishToUsers(Set<UUID> userIds, String eventType, UUID gameId) {
    if (userIds == null || userIds.isEmpty()) {
      return;
    }

    userIds.forEach(userId -> publishToUser(userId, eventType, gameId));
  }

  private void publishToUser(UUID userId, String eventType, UUID gameId) {
    List<SseEmitter> emitters = emittersByUserId.get(userId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }

    for (SseEmitter emitter : emitters) {
      sendEvent(userId, emitter, eventType, gameId);
    }
  }

  private void sendEvent(UUID userId, SseEmitter emitter, String eventType, UUID gameId) {
    try {
      emitter.send(
          SseEmitter.event()
              .name("game-event")
              .data(new GameRealtimeEvent(eventType, gameId, Instant.now())));
    } catch (IOException ex) {
      log.debug(
          "GameRealtimeEventService: dropping stale emitter - userId={}, eventType={}, reason={}",
          userId,
          eventType,
          ex.getMessage());
      emitter.complete();
      removeEmitter(userId, emitter);
    }
  }

  private void removeEmitter(UUID userId, SseEmitter emitter) {
    CopyOnWriteArrayList<SseEmitter> emitters = emittersByUserId.get(userId);
    if (emitters == null) {
      return;
    }
    emitters.remove(emitter);
    if (emitters.isEmpty()) {
      emittersByUserId.remove(userId);
    }
  }

  public record GameRealtimeEvent(String type, UUID gameId, Instant timestamp) {}
}
