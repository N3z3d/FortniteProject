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

import com.fortnite.pronos.dto.DraftTradeProposalRequest;
import com.fortnite.pronos.dto.DraftTradeProposalResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.UserResolver;
import com.fortnite.pronos.service.draft.DraftParticipantTradeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** REST API for 1v1 draft participant trades with explicit acceptance (FR-34, FR-35). */
@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}/draft/trade")
@RequiredArgsConstructor
public class DraftParticipantTradeController {

  private final DraftParticipantTradeService tradeService;
  private final UserResolver userResolver;

  /**
   * Proposes a 1v1 trade between two draft participants. No region or rank restriction applies.
   *
   * @return 201 with trade proposal details, 401 if unauthenticated, 400 if validation fails
   */
  @PostMapping
  public ResponseEntity<DraftTradeProposalResponse> proposeTrade(
      @PathVariable UUID gameId,
      @Valid @RequestBody DraftTradeProposalRequest request,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("DraftParticipantTradeController: unauthenticated proposeTrade for game {}", gameId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    log.info(
        "Draft trade propose: game={} proposer={} target={} out={} in={}",
        gameId,
        user.getId(),
        request.targetParticipantId(),
        request.playerFromProposerId(),
        request.playerFromTargetId());

    DraftTradeProposalResponse response =
        tradeService.proposeTrade(
            gameId,
            user.getId(),
            request.targetParticipantId(),
            request.playerFromProposerId(),
            request.playerFromTargetId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Accepts a pending trade proposal, swapping both players' picks. Only the target participant may
   * accept.
   *
   * @return 200 with accepted trade details, 401 if unauthenticated, 400 if validation fails
   */
  @PostMapping("/{tradeId}/accept")
  public ResponseEntity<DraftTradeProposalResponse> acceptTrade(
      @PathVariable UUID gameId,
      @PathVariable UUID tradeId,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("DraftParticipantTradeController: unauthenticated acceptTrade for game {}", gameId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    log.info("Draft trade accept: game={} caller={} tradeId={}", gameId, user.getId(), tradeId);
    DraftTradeProposalResponse response = tradeService.acceptTrade(gameId, user.getId(), tradeId);
    return ResponseEntity.ok(response);
  }

  /**
   * Rejects a pending trade proposal. Only the target participant may reject.
   *
   * @return 200 with rejected trade details, 401 if unauthenticated, 400 if validation fails
   */
  @PostMapping("/{tradeId}/reject")
  public ResponseEntity<DraftTradeProposalResponse> rejectTrade(
      @PathVariable UUID gameId,
      @PathVariable UUID tradeId,
      @RequestParam(name = "user", required = false) String username,
      HttpServletRequest httpRequest) {

    User user = userResolver.resolve(username, httpRequest);
    if (user == null) {
      log.warn("DraftParticipantTradeController: unauthenticated rejectTrade for game {}", gameId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    log.info("Draft trade reject: game={} caller={} tradeId={}", gameId, user.getId(), tradeId);
    DraftTradeProposalResponse response = tradeService.rejectTrade(gameId, user.getId(), tradeId);
    return ResponseEntity.ok(response);
  }
}
