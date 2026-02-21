package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.GameRealtimeEventService;
import com.fortnite.pronos.service.UserResolver;

@ExtendWith(MockitoExtension.class)
class GameRealtimeControllerTest {

  @Mock private UserResolver userResolver;

  @Mock private GameRealtimeEventService gameRealtimeEventService;

  @InjectMocks private GameRealtimeController controller;

  private User user;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("thibaut");
    request = new MockHttpServletRequest();
  }

  @Test
  void shouldReturnUnauthorizedWhenUserCannotBeResolved() {
    when(userResolver.resolve(null, request)).thenReturn(null);

    ResponseEntity<SseEmitter> response = controller.streamEvents(null, request);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void shouldSubscribeWhenUserIsResolved() {
    SseEmitter emitter = new SseEmitter();
    when(userResolver.resolve("thibaut", request)).thenReturn(user);
    when(gameRealtimeEventService.subscribe(user.getId())).thenReturn(emitter);

    ResponseEntity<SseEmitter> response = controller.streamEvents("thibaut", request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    verify(gameRealtimeEventService).subscribe(user.getId());
  }
}
