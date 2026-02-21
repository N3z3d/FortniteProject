package com.fortnite.pronos.service;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeNotificationService {

  private final SimpMessagingTemplate messagingTemplate;

  public void notifyTradeProposed(com.fortnite.pronos.model.Trade trade) {
    log.info(
        "Notifying trade proposal: {} -> {}",
        teamName(fromTeamOf(trade)),
        teamName(toTeamOf(trade)));

    TradeNotification notification = TradeNotification.proposed(trade);

    sendToUser(teamOwnerId(toTeamOf(trade)), notification);
    sendToTopic(gameId(fromTeamOf(trade)), notification);
  }

  public void notifyTradeAccepted(com.fortnite.pronos.model.Trade trade) {
    log.info("Notifying trade accepted: {}", trade.getId());

    TradeNotification notification = TradeNotification.accepted(trade);

    sendToUser(teamOwnerId(fromTeamOf(trade)), notification);
    sendToTopic(gameId(fromTeamOf(trade)), notification);
  }

  public void notifyTradeRejected(com.fortnite.pronos.model.Trade trade) {
    log.info("Notifying trade rejected: {}", trade.getId());

    TradeNotification notification = TradeNotification.rejected(trade);

    sendToUser(teamOwnerId(fromTeamOf(trade)), notification);
    sendToTopic(gameId(fromTeamOf(trade)), notification);
  }

  public void notifyTradeCancelled(com.fortnite.pronos.model.Trade trade) {
    log.info("Notifying trade cancelled: {}", trade.getId());

    TradeNotification notification = TradeNotification.cancelled(trade);

    sendToUser(teamOwnerId(toTeamOf(trade)), notification);
    sendToTopic(gameId(fromTeamOf(trade)), notification);
  }

  public void notifyTradeCountered(
      com.fortnite.pronos.model.Trade trade, com.fortnite.pronos.model.Trade counterTrade) {
    log.info("Notifying trade countered: {} with counter {}", trade.getId(), counterTrade.getId());

    TradeNotification notification = TradeNotification.countered(trade, counterTrade);

    sendToUser(teamOwnerId(fromTeamOf(trade)), notification);
    sendToTopic(gameId(fromTeamOf(trade)), notification);
  }

  private com.fortnite.pronos.model.Team fromTeamOf(com.fortnite.pronos.model.Trade trade) {
    return trade.getFromTeam();
  }

  private com.fortnite.pronos.model.Team toTeamOf(com.fortnite.pronos.model.Trade trade) {
    return trade.getToTeam();
  }

  private UUID gameId(com.fortnite.pronos.model.Team team) {
    return team.getGameId();
  }

  private UUID teamOwnerId(com.fortnite.pronos.model.Team team) {
    return team.getUserId();
  }

  private String teamName(com.fortnite.pronos.model.Team team) {
    return team.getName();
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

    public static TradeNotification proposed(com.fortnite.pronos.model.Trade trade) {
      com.fortnite.pronos.model.Team fromTeam = fromTeamOf(trade);
      com.fortnite.pronos.model.Team toTeam = toTeamOf(trade);
      return new TradeNotification(
          "TRADE_PROPOSED",
          trade.getId(),
          teamId(fromTeam),
          teamName(fromTeam),
          teamId(toTeam),
          teamName(toTeam),
          tradeStatus(trade),
          null);
    }

    public static TradeNotification accepted(com.fortnite.pronos.model.Trade trade) {
      com.fortnite.pronos.model.Team fromTeam = fromTeamOf(trade);
      com.fortnite.pronos.model.Team toTeam = toTeamOf(trade);
      return new TradeNotification(
          "TRADE_ACCEPTED",
          trade.getId(),
          teamId(fromTeam),
          teamName(fromTeam),
          teamId(toTeam),
          teamName(toTeam),
          tradeStatus(trade),
          null);
    }

    public static TradeNotification rejected(com.fortnite.pronos.model.Trade trade) {
      com.fortnite.pronos.model.Team fromTeam = fromTeamOf(trade);
      com.fortnite.pronos.model.Team toTeam = toTeamOf(trade);
      return new TradeNotification(
          "TRADE_REJECTED",
          trade.getId(),
          teamId(fromTeam),
          teamName(fromTeam),
          teamId(toTeam),
          teamName(toTeam),
          tradeStatus(trade),
          null);
    }

    public static TradeNotification cancelled(com.fortnite.pronos.model.Trade trade) {
      com.fortnite.pronos.model.Team fromTeam = fromTeamOf(trade);
      com.fortnite.pronos.model.Team toTeam = toTeamOf(trade);
      return new TradeNotification(
          "TRADE_CANCELLED",
          trade.getId(),
          teamId(fromTeam),
          teamName(fromTeam),
          teamId(toTeam),
          teamName(toTeam),
          tradeStatus(trade),
          null);
    }

    public static TradeNotification countered(
        com.fortnite.pronos.model.Trade trade, com.fortnite.pronos.model.Trade counterTrade) {
      com.fortnite.pronos.model.Team fromTeam = fromTeamOf(trade);
      com.fortnite.pronos.model.Team toTeam = toTeamOf(trade);
      return new TradeNotification(
          "TRADE_COUNTERED",
          trade.getId(),
          teamId(fromTeam),
          teamName(fromTeam),
          teamId(toTeam),
          teamName(toTeam),
          tradeStatus(trade),
          counterTrade.getId());
    }

    private static com.fortnite.pronos.model.Team fromTeamOf(
        com.fortnite.pronos.model.Trade trade) {
      return trade.getFromTeam();
    }

    private static com.fortnite.pronos.model.Team toTeamOf(com.fortnite.pronos.model.Trade trade) {
      return trade.getToTeam();
    }

    private static UUID teamId(com.fortnite.pronos.model.Team team) {
      return team.getId();
    }

    private static String teamName(com.fortnite.pronos.model.Team team) {
      return team.getName();
    }

    private static String tradeStatus(com.fortnite.pronos.model.Trade trade) {
      return trade.getStatus().name();
    }
  }
}
