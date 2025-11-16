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

import com.fortnite.pronos.dto.CounterTradeRequestDto;
import com.fortnite.pronos.dto.TradeRequestDto;
import com.fortnite.pronos.dto.TradeResponseDto;
import com.fortnite.pronos.model.Trade;
import com.fortnite.pronos.service.TradingService;
import com.fortnite.pronos.service.UserContextService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:4200}")
public class TradeController {

  private final TradingService tradingService;
  private final UserContextService userContextService;

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

    // Vérifier que l'utilisateur possède l'équipe source
    UUID userId = userContextService.getUserIdFromUsername(userDetails.getUsername());

    Trade trade =
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
    Trade trade = tradingService.acceptTrade(tradeId, userId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Rejeter un trade */
  @PutMapping("/{tradeId}/reject")
  public ResponseEntity<TradeResponseDto> rejectTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} rejecting trade {}", userDetails.getUsername(), tradeId);

    UUID userId = userContextService.getUserIdFromUsername(userDetails.getUsername());
    Trade trade = tradingService.rejectTrade(tradeId, userId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Annuler un trade (par l'initiateur) */
  @PutMapping("/{tradeId}/cancel")
  public ResponseEntity<TradeResponseDto> cancelTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} cancelling trade {}", userDetails.getUsername(), tradeId);

    UUID userId = userContextService.getUserIdFromUsername(userDetails.getUsername());
    Trade trade = tradingService.cancelTrade(tradeId, userId);

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
    Trade trade =
        tradingService.counterTradeWithPlayerIds(
            tradeId, userId, request.getOfferedPlayerIds(), request.getRequestedPlayerIds());

    return ResponseEntity.status(HttpStatus.CREATED).body(TradeResponseDto.fromTrade(trade));
  }

  /** Obtenir l'historique des trades d'une équipe */
  @GetMapping("/team/{teamId}/history")
  public ResponseEntity<List<TradeResponseDto>> getTeamTradeHistory(
      @PathVariable UUID teamId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} requesting trade history for team {}", userDetails.getUsername(), teamId);

    List<Trade> trades = tradingService.getTeamTradeHistory(teamId);
    List<TradeResponseDto> response = trades.stream().map(TradeResponseDto::fromTrade).toList();

    return ResponseEntity.ok(response);
  }

  /** Obtenir les trades en attente pour une équipe */
  @GetMapping("/team/{teamId}/pending")
  public ResponseEntity<List<TradeResponseDto>> getPendingTradesForTeam(
      @PathVariable UUID teamId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} requesting pending trades for team {}", userDetails.getUsername(), teamId);

    List<Trade> trades = tradingService.getPendingTradesForTeam(teamId);
    List<TradeResponseDto> response = trades.stream().map(TradeResponseDto::fromTrade).toList();

    return ResponseEntity.ok(response);
  }

  /** Obtenir les statistiques de trade d'une game */
  @GetMapping("/game/{gameId}/statistics")
  public ResponseEntity<Map<String, Long>> getGameTradeStatistics(
      @PathVariable UUID gameId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} requesting trade statistics for game {}", userDetails.getUsername(), gameId);

    Map<String, Long> stats = tradingService.getGameTradeStatistics(gameId);

    return ResponseEntity.ok(stats);
  }

  /** Obtenir les détails d'un trade spécifique */
  @GetMapping("/{tradeId}")
  public ResponseEntity<TradeResponseDto> getTrade(
      @PathVariable UUID tradeId, @AuthenticationPrincipal UserDetails userDetails) {

    log.info("User {} requesting details for trade {}", userDetails.getUsername(), tradeId);

    Trade trade = tradingService.getTrade(tradeId);

    return ResponseEntity.ok(TradeResponseDto.fromTrade(trade));
  }

  /** Obtenir tous les trades d'une game */
  @GetMapping("/game/{gameId}")
  public ResponseEntity<List<TradeResponseDto>> getGameTrades(
      @PathVariable UUID gameId,
      @RequestParam(required = false) Trade.Status status,
      @AuthenticationPrincipal UserDetails userDetails) {

    log.info(
        "User {} requesting trades for game {} with status {}",
        userDetails.getUsername(),
        gameId,
        status);

    List<Trade> trades;
    if (status != null) {
      trades = tradingService.getGameTradesByStatus(gameId, status);
    } else {
      trades = tradingService.getAllGameTrades(gameId);
    }

    List<TradeResponseDto> response = trades.stream().map(TradeResponseDto::fromTrade).toList();

    return ResponseEntity.ok(response);
  }
}
