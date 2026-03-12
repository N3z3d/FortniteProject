package com.fortnite.pronos.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.SwapSoloRequest;
import com.fortnite.pronos.dto.SwapSoloResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.SwapSoloService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** REST API for solo swap operations (FR-32, FR-33). */
@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}/draft")
@RequiredArgsConstructor
public class SwapSoloController {

  private final SwapSoloService swapSoloService;
  private final UserResolver userResolver;

  /**
   * Executes a solo swap: replaces one of the caller's players with a free player of strictly worse
   * rank in the same region.
   *
   * @return 200 with swap details, 401 if unauthenticated, 400 if validation fails
   */
  @PostMapping("/swap-solo")
  public ResponseEntity<SwapSoloResponse> swapSolo(
      @PathVariable UUID gameId,
      @Valid @RequestBody SwapSoloRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("SwapSoloController: unauthenticated swap attempt for game {}", gameId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    log.info(
        "Solo swap: game={} user={} out={} in={}",
        gameId,
        user.getId(),
        request.playerOutId(),
        request.playerInId());

    SwapSoloResponse response =
        swapSoloService.executeSoloSwap(
            gameId, user.getId(), request.playerOutId(), request.playerInId());
    return ResponseEntity.ok(response);
  }
}
