package com.fortnite.pronos.service;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.model.Trade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeNotificationService {

  private final SimpMessagingTemplate messagingTemplate;

  public void notifyTradeProposed(Trade trade) {
    log.info(
        "Notifying trade proposal: {} -> {}",
        trade.getFromTeam().getName(),
        trade.getToTeam().getName());

    TradeNotification notification = TradeNotification.proposed(trade);

    sendToUser(trade.getToTeam().getUser().getId(), notification);
    sendToTopic(trade.getFromTeam().getGame().getId(), notification);
  }

  public void notifyTradeAccepted(Trade trade) {
    log.info("Notifying trade accepted: {}", trade.getId());

    TradeNotification notification = TradeNotification.accepted(trade);

    sendToUser(trade.getFromTeam().getUser().getId(), notification);
    sendToTopic(trade.getFromTeam().getGame().getId(), notification);
  }

  public void notifyTradeRejected(Trade trade) {
    log.info("Notifying trade rejected: {}", trade.getId());

    TradeNotification notification = TradeNotification.rejected(trade);

    sendToUser(trade.getFromTeam().getUser().getId(), notification);
    sendToTopic(trade.getFromTeam().getGame().getId(), notification);
  }

  public void notifyTradeCancelled(Trade trade) {
    log.info("Notifying trade cancelled: {}", trade.getId());

    TradeNotification notification = TradeNotification.cancelled(trade);

    sendToUser(trade.getToTeam().getUser().getId(), notification);
    sendToTopic(trade.getFromTeam().getGame().getId(), notification);
  }

  public void notifyTradeCountered(Trade trade, Trade counterTrade) {
    log.info("Notifying trade countered: {} with counter {}", trade.getId(), counterTrade.getId());

    TradeNotification notification = TradeNotification.countered(trade, counterTrade);

    sendToUser(trade.getFromTeam().getUser().getId(), notification);
    sendToTopic(trade.getFromTeam().getGame().getId(), notification);
  }

  private void sendToUser(UUID userId, TradeNotification notification) {
    String destination = "/queue/trades";
    messagingTemplate.convertAndSendToUser(userId.toString(), destination, notification);
  }

  private void sendToTopic(UUID gameId, TradeNotification notification) {
    String destination = "/topic/games/" + gameId + "/trades";
    messagingTemplate.convertAndSend(destination, notification);
  }

  public record TradeNotification(
      String type,
      UUID tradeId,
      UUID fromTeamId,
      String fromTeamName,
      UUID toTeamId,
      String toTeamName,
      String status,
      UUID counterTradeId) {

    public static TradeNotification proposed(Trade trade) {
      return new TradeNotification(
          "TRADE_PROPOSED",
          trade.getId(),
          trade.getFromTeam().getId(),
          trade.getFromTeam().getName(),
          trade.getToTeam().getId(),
          trade.getToTeam().getName(),
          trade.getStatus().name(),
          null);
    }

    public static TradeNotification accepted(Trade trade) {
      return new TradeNotification(
          "TRADE_ACCEPTED",
          trade.getId(),
          trade.getFromTeam().getId(),
          trade.getFromTeam().getName(),
          trade.getToTeam().getId(),
          trade.getToTeam().getName(),
          trade.getStatus().name(),
          null);
    }

    public static TradeNotification rejected(Trade trade) {
      return new TradeNotification(
          "TRADE_REJECTED",
          trade.getId(),
          trade.getFromTeam().getId(),
          trade.getFromTeam().getName(),
          trade.getToTeam().getId(),
          trade.getToTeam().getName(),
          trade.getStatus().name(),
          null);
    }

    public static TradeNotification cancelled(Trade trade) {
      return new TradeNotification(
          "TRADE_CANCELLED",
          trade.getId(),
          trade.getFromTeam().getId(),
          trade.getFromTeam().getName(),
          trade.getToTeam().getId(),
          trade.getToTeam().getName(),
          trade.getStatus().name(),
          null);
    }

    public static TradeNotification countered(Trade trade, Trade counterTrade) {
      return new TradeNotification(
          "TRADE_COUNTERED",
          trade.getId(),
          trade.getFromTeam().getId(),
          trade.getFromTeam().getName(),
          trade.getToTeam().getId(),
          trade.getToTeam().getName(),
          trade.getStatus().name(),
          counterTrade.getId());
    }
  }
}
