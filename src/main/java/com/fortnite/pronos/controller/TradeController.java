package com.fortnite.pronos.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.application.usecase.TradeQueryUseCase;
import com.fortnite.pronos.domain.trade.model.Trade;
import com.fortnite.pronos.domain.trade.model.TradeStatus;
import com.fortnite.pronos.dto.CounterTradeRequestDto;
import com.fortnite.pronos.dto.TradeRequestDto;
import com.fortnite.pronos.dto.TradeResponseDto;
import com.fortnite.pronos.service.TradingService;
import com.fortnite.pronos.service.UserContextService;
import com.fortnite.pronos.service.trade.TradeResponseMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

  private final TradingService tradingService;
  private final TradeQueryUseCase tradeQueryUseCase;
  private final UserContextService userContextService;
  private final TradeResponseMapper tradeResponseMapper;

  /** Propose un nouveau trade */
  @PostMapping
  public ResponseEntity<TradeResponseDto> proposeTrade(
      @Valid @RequestBody TradeRequestDto request,
      @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info(
        "TradeController: proposeTrade requested - username={}, fromTeamId={}, toTeamId={}",
        username,
        request.getFromTeamId(),
        request.getToTeamId());

    UUID userId = userContextService.getUserIdFromUsername(username);

    com.fortnite.pronos.model.Trade trade =
        tradingService.proposeTradeWithPlayerIds(
            request.getFromTeamId(),
            request.getToTeamId(),
            request.getOfferedPlayerIds(),
            request.getRequestedPlayerIds());
    log.info(
        "TradeController: proposeTrade succeeded - username={}, userId={}, tradeId={}",
        username,
        userId,
        trade.getId());

    return ResponseEntity.status(HttpStatus.CREATED).body(TradeResponseDto.fromTrade(trade));
  }

  /** Accepter un trade */
  @PutMapping("/{tradeId}/accept")
  public ResponseEntity<TradeResponseDto> acceptTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info("TradeController: acceptTrade requested - username={}, tradeId={}", username, tradeId);

    UUID userId = userContextService.getUserIdFromUsername(username);
    com.fortnite.pronos.model.Trade trade = tradingService.acceptTrade(tradeId, userId);
    log.info(
        "TradeController: acceptTrade succeeded - username={}, userId={}, tradeId={}",
        username,
        userId,
        tradeId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Rejeter un trade */
  @PutMapping("/{tradeId}/reject")
  public ResponseEntity<TradeResponseDto> rejectTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info("TradeController: rejectTrade requested - username={}, tradeId={}", username, tradeId);

    UUID userId = userContextService.getUserIdFromUsername(username);
    com.fortnite.pronos.model.Trade trade = tradingService.rejectTrade(tradeId, userId);
    log.info(
        "TradeController: rejectTrade succeeded - username={}, userId={}, tradeId={}",
        username,
        userId,
        tradeId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Annuler un trade (par l'initiateur) */
  @PutMapping("/{tradeId}/cancel")
  public ResponseEntity<TradeResponseDto> cancelTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info("TradeController: cancelTrade requested - username={}, tradeId={}", username, tradeId);

    UUID userId = userContextService.getUserIdFromUsername(username);
    com.fortnite.pronos.model.Trade trade = tradingService.cancelTrade(tradeId, userId);
    log.info(
        "TradeController: cancelTrade succeeded - username={}, userId={}, tradeId={}",
        username,
        userId,
        tradeId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Faire une contre-proposition */
  @PostMapping("/{tradeId}/counter")
  public ResponseEntity<TradeResponseDto> counterTrade(
      @PathVariable UUID tradeId,
      @Valid @RequestBody CounterTradeRequestDto request,
      @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info(
        "TradeController: counterTrade requested - username={}, tradeId={}", username, tradeId);

    UUID userId = userContextService.getUserIdFromUsername(username);
    com.fortnite.pronos.model.Trade trade =
        tradingService.counterTradeWithPlayerIds(
            tradeId, userId, request.getOfferedPlayerIds(), request.getRequestedPlayerIds());
    log.info(
        "TradeController: counterTrade succeeded - username={}, userId={}, tradeId={}",
        username,
        userId,
        trade.getId());

    return ResponseEntity.status(HttpStatus.CREATED).body(TradeResponseDto.fromTrade(trade));
  }

  /** Obtenir l'historique des trades d'une equipe */
  @GetMapping("/team/{teamId}/history")
  public ResponseEntity<List<TradeResponseDto>> getTeamTradeHistory(
      @PathVariable UUID teamId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info(
        "TradeController: getTeamTradeHistory requested - username={}, teamId={}",
        username,
        teamId);

    List<Trade> trades = tradeQueryUseCase.getTeamTradeHistory(teamId);
    log.debug(
        "TradeController: getTeamTradeHistory succeeded - username={}, teamId={}, tradeCount={}",
        username,
        teamId,
        trades.size());

    return ResponseEntity.ok(tradeResponseMapper.toDtos(trades));
  }

  /** Obtenir les trades en attente pour une equipe */
  @GetMapping("/team/{teamId}/pending")
  public ResponseEntity<List<TradeResponseDto>> getPendingTradesForTeam(
      @PathVariable UUID teamId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info(
        "TradeController: getPendingTradesForTeam requested - username={}, teamId={}",
        username,
        teamId);

    List<Trade> trades = tradeQueryUseCase.getPendingTradesForTeam(teamId);
    log.debug(
        "TradeController: getPendingTradesForTeam succeeded - username={}, teamId={}, tradeCount={}",
        username,
        teamId,
        trades.size());

    return ResponseEntity.ok(tradeResponseMapper.toDtos(trades));
  }

  /** Obtenir les statistiques de trade d'une game */
  @GetMapping("/game/{gameId}/statistics")
  public ResponseEntity<Map<String, Object>> getGameTradeStatistics(
      @PathVariable UUID gameId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info(
        "TradeController: getGameTradeStatistics requested - username={}, gameId={}",
        username,
        gameId);

    Map<String, Object> stats = tradeQueryUseCase.getGameTradeStatistics(gameId);
    log.debug(
        "TradeController: getGameTradeStatistics succeeded - username={}, gameId={}",
        username,
        gameId);

    return ResponseEntity.ok(stats);
  }

  /** Obtenir les details d'un trade specifique */
  @GetMapping("/{tradeId}")
  public ResponseEntity<TradeResponseDto> getTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info("TradeController: getTrade requested - username={}, tradeId={}", username, tradeId);

    Trade trade = tradeQueryUseCase.getTrade(tradeId);
    log.debug("TradeController: getTrade succeeded - username={}, tradeId={}", username, tradeId);

    return ResponseEntity.ok(tradeResponseMapper.toDto(trade));
  }

  /** Obtenir tous les trades d'une game */
  @GetMapping("/game/{gameId}")
  public ResponseEntity<List<TradeResponseDto>> getGameTrades(
      @PathVariable UUID gameId,
      @RequestParam(required = false) TradeStatus status,
      @AuthenticationPrincipal UserDetails userDetails) {

    String username = resolveUsername(userDetails);
    log.info(
        "TradeController: getGameTrades requested - username={}, gameId={}, status={}",
        username,
        gameId,
        status);

    List<Trade> trades;
    if (status != null) {
      trades = tradeQueryUseCase.getGameTradesByStatus(gameId, status);
    } else {
      trades = tradeQueryUseCase.getAllGameTrades(gameId);
    }
    log.debug(
        "TradeController: getGameTrades succeeded - username={}, gameId={}, status={}, tradeCount={}",
        username,
        gameId,
        status,
        trades.size());

    return ResponseEntity.ok(tradeResponseMapper.toDtos(trades));
  }

  private String resolveUsername(UserDetails userDetails) {
    return userDetails != null ? userDetails.getUsername() : "anonymous";
  }
}
