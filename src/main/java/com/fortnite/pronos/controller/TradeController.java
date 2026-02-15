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

    log.info(
        "User {} proposing trade from team {} to team {}",
        userDetails.getUsername(),
        request.getFromTeamId(),
        request.getToTeamId());

    UUID userId = userContextService.getUserIdFromUsername(userDetails.getUsername());

    com.fortnite.pronos.model.Trade trade =
        tradingService.proposeTradeWithPlayerIds(
            request.getFromTeamId(),
            request.getToTeamId(),
            request.getOfferedPlayerIds(),
            request.getRequestedPlayerIds());

    return ResponseEntity.status(HttpStatus.CREATED).body(TradeResponseDto.fromTrade(trade));
  }

  /** Accepter un trade */
  @PutMapping("/{tradeId}/accept")
  public ResponseEntity<TradeResponseDto> acceptTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} accepting trade {}", userDetails.getUsername(), tradeId);

    UUID userId = userContextService.getUserIdFromUsername(userDetails.getUsername());
    com.fortnite.pronos.model.Trade trade = tradingService.acceptTrade(tradeId, userId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Rejeter un trade */
  @PutMapping("/{tradeId}/reject")
  public ResponseEntity<TradeResponseDto> rejectTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} rejecting trade {}", userDetails.getUsername(), tradeId);

    UUID userId = userContextService.getUserIdFromUsername(userDetails.getUsername());
    com.fortnite.pronos.model.Trade trade = tradingService.rejectTrade(tradeId, userId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Annuler un trade (par l'initiateur) */
  @PutMapping("/{tradeId}/cancel")
  public ResponseEntity<TradeResponseDto> cancelTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} cancelling trade {}", userDetails.getUsername(), tradeId);

    UUID userId = userContextService.getUserIdFromUsername(userDetails.getUsername());
    com.fortnite.pronos.model.Trade trade = tradingService.cancelTrade(tradeId, userId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Faire une contre-proposition */
  @PostMapping("/{tradeId}/counter")
  public ResponseEntity<TradeResponseDto> counterTrade(
      @PathVariable UUID tradeId,
      @Valid @RequestBody CounterTradeRequestDto request,
      @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} making counter-offer for trade {}", userDetails.getUsername(), tradeId);

    UUID userId = userContextService.getUserIdFromUsername(userDetails.getUsername());
    com.fortnite.pronos.model.Trade trade =
        tradingService.counterTradeWithPlayerIds(
            tradeId, userId, request.getOfferedPlayerIds(), request.getRequestedPlayerIds());

    return ResponseEntity.status(HttpStatus.CREATED).body(TradeResponseDto.fromTrade(trade));
  }

  /** Obtenir l'historique des trades d'une equipe */
  @GetMapping("/team/{teamId}/history")
  public ResponseEntity<List<TradeResponseDto>> getTeamTradeHistory(
      @PathVariable UUID teamId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = userDetails != null ? userDetails.getUsername() : "anonymous";
    log.info("User {} requesting trade history for team {}", username, teamId);

    List<Trade> trades = tradeQueryUseCase.getTeamTradeHistory(teamId);

    return ResponseEntity.ok(tradeResponseMapper.toDtos(trades));
  }

  /** Obtenir les trades en attente pour une equipe */
  @GetMapping("/team/{teamId}/pending")
  public ResponseEntity<List<TradeResponseDto>> getPendingTradesForTeam(
      @PathVariable UUID teamId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = userDetails != null ? userDetails.getUsername() : "anonymous";
    log.info("User {} requesting pending trades for team {}", username, teamId);

    List<Trade> trades = tradeQueryUseCase.getPendingTradesForTeam(teamId);

    return ResponseEntity.ok(tradeResponseMapper.toDtos(trades));
  }

  /** Obtenir les statistiques de trade d'une game */
  @GetMapping("/game/{gameId}/statistics")
  public ResponseEntity<Map<String, Object>> getGameTradeStatistics(
      @PathVariable UUID gameId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = userDetails != null ? userDetails.getUsername() : "anonymous";
    log.info("User {} requesting trade statistics for game {}", username, gameId);

    Map<String, Object> stats = tradeQueryUseCase.getGameTradeStatistics(gameId);

    return ResponseEntity.ok(stats);
  }

  /** Obtenir les details d'un trade specifique */
  @GetMapping("/{tradeId}")
  public ResponseEntity<TradeResponseDto> getTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    String username = userDetails != null ? userDetails.getUsername() : "anonymous";
    log.info("User {} requesting details for trade {}", username, tradeId);

    Trade trade = tradeQueryUseCase.getTrade(tradeId);

    return ResponseEntity.ok(tradeResponseMapper.toDto(trade));
  }

  /** Obtenir tous les trades d'une game */
  @GetMapping("/game/{gameId}")
  public ResponseEntity<List<TradeResponseDto>> getGameTrades(
      @PathVariable UUID gameId,
      @RequestParam(required = false) TradeStatus status,
      @AuthenticationPrincipal UserDetails userDetails) {

    String username = userDetails != null ? userDetails.getUsername() : "anonymous";
    log.info("User {} requesting trades for game {} with status {}", username, gameId, status);

    List<Trade> trades;
    if (status != null) {
      trades = tradeQueryUseCase.getGameTradesByStatus(gameId, status);
    } else {
      trades = tradeQueryUseCase.getAllGameTrades(gameId);
    }

    return ResponseEntity.ok(tradeResponseMapper.toDtos(trades));
  }
}
