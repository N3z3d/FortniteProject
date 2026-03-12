package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class GameRealtimeEventServiceTest {

  @Test
  @DisplayName("drops stale emitters when Spring raises IllegalStateException during publish")
  void shouldDropStaleEmitterWhenPublishRaisesIllegalStateException() throws Exception {
    GameRealtimeEventService service = new GameRealtimeEventService();
    UUID userId = UUID.randomUUID();
    UUID gameId = UUID.randomUUID();

    CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    emitters.add(new IllegalStateEmitter());
    setEmitters(service, userId, emitters);

    assertThatCode(
            () ->
                service.publishToUsers(
                    Set.of(userId), GameRealtimeEventService.GAME_JOINED, gameId))
        .doesNotThrowAnyException();
    assertThat(getEmitters(service)).doesNotContainKey(userId);
  }

  @Test
  @DisplayName("ignores completion failures when dropping a stale emitter")
  void shouldIgnoreEmitterCompletionFailureWhenDroppingStaleEmitter() throws Exception {
    GameRealtimeEventService service = new GameRealtimeEventService();
    UUID userId = UUID.randomUUID();
    UUID gameId = UUID.randomUUID();

    CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    emitters.add(new IllegalStateEmitterThatFailsOnComplete());
    setEmitters(service, userId, emitters);

    assertThatCode(
            () ->
                service.publishToUsers(
                    Set.of(userId), GameRealtimeEventService.GAME_JOINED, gameId))
        .doesNotThrowAnyException();
    assertThat(getEmitters(service)).doesNotContainKey(userId);
  }

  private void setEmitters(
      GameRealtimeEventService service, UUID userId, CopyOnWriteArrayList<SseEmitter> emitters)
      throws Exception {
    getEmitters(service).put(userId, emitters);
  }

  @SuppressWarnings("unchecked")
  private ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> getEmitters(
      GameRealtimeEventService service) throws Exception {
    Field field = GameRealtimeEventService.class.getDeclaredField("emittersByUserId");
    field.setAccessible(true);
    return (ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>) field.get(service);
  }

  private static final class IllegalStateEmitter extends SseEmitter {
    @Override
    public synchronized void send(SseEventBuilder builder) throws IOException {
      throw new IllegalStateException("stale async context");
    }
  }

  private static final class IllegalStateEmitterThatFailsOnComplete extends SseEmitter {
    @Override
    public synchronized void send(SseEventBuilder builder) throws IOException {
      throw new IllegalStateException("stale async context");
    }

    @Override
    public synchronized void complete() {
      throw new IllegalStateException("async context already released");
    }
  }
}
