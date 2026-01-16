package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.Trade;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.TradeNotificationService.TradeNotification;

@ExtendWith(MockitoExtension.class)
class TradeNotificationServiceTest {

  @Mock private SimpMessagingTemplate messagingTemplate;
  @Captor private ArgumentCaptor<TradeNotification> notificationCaptor;

  private TradeNotificationService service;

  private Trade trade;
  private Team fromTeam;
  private Team toTeam;
  private Game game;
  private User fromUser;
  private User toUser;

  @BeforeEach
  void setUp() {
    service = new TradeNotificationService(messagingTemplate);

    fromUser = new User();
    fromUser.setId(UUID.randomUUID());

    toUser = new User();
    toUser.setId(UUID.randomUUID());

    game = new Game();
    game.setId(UUID.randomUUID());

    fromTeam = new Team();
    fromTeam.setId(UUID.randomUUID());
    fromTeam.setName("FromTeam");
    fromTeam.setUser(fromUser);
    fromTeam.setGame(game);

    toTeam = new Team();
    toTeam.setId(UUID.randomUUID());
    toTeam.setName("ToTeam");
    toTeam.setUser(toUser);
    toTeam.setGame(game);

    trade = new Trade();
    trade.setId(UUID.randomUUID());
    trade.setFromTeam(fromTeam);
    trade.setToTeam(toTeam);
    trade.setStatus(Trade.Status.PENDING);
  }

  @Test
  void notifyTradeProposedSendsToTargetUserQueue() {
    service.notifyTradeProposed(trade);

    verify(messagingTemplate)
        .convertAndSendToUser(
            eq(toUser.getId().toString()), eq("/queue/trades"), notificationCaptor.capture());

    TradeNotification notification = notificationCaptor.getValue();
    assertThat(notification.type()).isEqualTo("TRADE_PROPOSED");
    assertThat(notification.tradeId()).isEqualTo(trade.getId());
    assertThat(notification.fromTeamName()).isEqualTo("FromTeam");
    assertThat(notification.toTeamName()).isEqualTo("ToTeam");
  }

  @Test
  void notifyTradeProposedBroadcastsToGameTopic() {
    service.notifyTradeProposed(trade);

    String expectedTopic = "/topic/games/" + game.getId() + "/trades";
    verify(messagingTemplate).convertAndSend(eq(expectedTopic), any(TradeNotification.class));
  }

  @Test
  void notifyTradeAcceptedSendsToInitiatorUserQueue() {
    trade.setStatus(Trade.Status.ACCEPTED);

    service.notifyTradeAccepted(trade);

    verify(messagingTemplate)
        .convertAndSendToUser(
            eq(fromUser.getId().toString()), eq("/queue/trades"), notificationCaptor.capture());

    TradeNotification notification = notificationCaptor.getValue();
    assertThat(notification.type()).isEqualTo("TRADE_ACCEPTED");
    assertThat(notification.status()).isEqualTo("ACCEPTED");
  }

  @Test
  void notifyTradeRejectedSendsToInitiatorUserQueue() {
    trade.setStatus(Trade.Status.REJECTED);

    service.notifyTradeRejected(trade);

    verify(messagingTemplate)
        .convertAndSendToUser(
            eq(fromUser.getId().toString()), eq("/queue/trades"), notificationCaptor.capture());

    TradeNotification notification = notificationCaptor.getValue();
    assertThat(notification.type()).isEqualTo("TRADE_REJECTED");
    assertThat(notification.status()).isEqualTo("REJECTED");
  }

  @Test
  void notifyTradeCancelledSendsToTargetUserQueue() {
    trade.setStatus(Trade.Status.CANCELLED);

    service.notifyTradeCancelled(trade);

    verify(messagingTemplate)
        .convertAndSendToUser(
            eq(toUser.getId().toString()), eq("/queue/trades"), notificationCaptor.capture());

    TradeNotification notification = notificationCaptor.getValue();
    assertThat(notification.type()).isEqualTo("TRADE_CANCELLED");
    assertThat(notification.status()).isEqualTo("CANCELLED");
  }

  @Test
  void notifyTradeCounteredIncludesCounterTradeId() {
    trade.setStatus(Trade.Status.COUNTERED);

    Trade counterTrade = new Trade();
    counterTrade.setId(UUID.randomUUID());
    counterTrade.setFromTeam(toTeam);
    counterTrade.setToTeam(fromTeam);
    counterTrade.setStatus(Trade.Status.PENDING);

    service.notifyTradeCountered(trade, counterTrade);

    verify(messagingTemplate)
        .convertAndSendToUser(
            eq(fromUser.getId().toString()), eq("/queue/trades"), notificationCaptor.capture());

    TradeNotification notification = notificationCaptor.getValue();
    assertThat(notification.type()).isEqualTo("TRADE_COUNTERED");
    assertThat(notification.counterTradeId()).isEqualTo(counterTrade.getId());
  }

  @Test
  void tradeNotificationRecordContainsAllFields() {
    TradeNotification notification = TradeNotification.proposed(trade);

    assertThat(notification.type()).isEqualTo("TRADE_PROPOSED");
    assertThat(notification.tradeId()).isEqualTo(trade.getId());
    assertThat(notification.fromTeamId()).isEqualTo(fromTeam.getId());
    assertThat(notification.fromTeamName()).isEqualTo("FromTeam");
    assertThat(notification.toTeamId()).isEqualTo(toTeam.getId());
    assertThat(notification.toTeamName()).isEqualTo("ToTeam");
    assertThat(notification.status()).isEqualTo("PENDING");
    assertThat(notification.counterTradeId()).isNull();
  }
}
