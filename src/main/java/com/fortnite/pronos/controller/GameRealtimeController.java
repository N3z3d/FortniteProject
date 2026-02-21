package com.fortnite.pronos.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.GameRealtimeEventService;
import com.fortnite.pronos.service.UserResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Slf4j
public class GameRealtimeController {

  private final UserResolver userResolver;
  private final GameRealtimeEventService gameRealtimeEventService;

  @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> streamEvents(
      @RequestParam(name = "user", required = false) String username, HttpServletRequest request) {
    User user = userResolver.resolve(username, request);
    if (user == null) {
      log.warn(
          "GameRealtimeController: unauthorized stream request - requestedUser={}, remoteAddr={}",
          username != null ? username : "-",
          request.getRemoteAddr());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(gameRealtimeEventService.subscribe(user.getId()));
  }
}
