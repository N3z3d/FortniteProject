package com.fortnite.pronos.service;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for broadcasting game events via WebSocket. Allows spectators and participants to receive
 * real-time updates about game state changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameNotificationService {

  private final SimpMessagingTemplate messagingTemplate;

  public void notifyGameStatusChanged(com.fortnite.pronos.model.Game game) {
    log.info("Notifying game status changed: {} -> {}", game.getId(), game.getStatus());

    GameNotification notification = GameNotification.statusChanged(game);
    sendToTopic(game.getId(), notification);
  }

  public void notifyPlayerJoined(
      com.fortnite.pronos.model.Game game, com.fortnite.pronos.model.User user) {
    log.info("Notifying player joined game {}: {}", game.getId(), user.getUsername());

    GameNotification notification = GameNotification.playerJoined(game, user);
    sendToTopic(game.getId(), notification);
  }

  public void notifyPlayerLeft(
      com.fortnite.pronos.model.Game game, com.fortnite.pronos.model.User user) {
    log.info("Notifying player left game {}: {}", game.getId(), user.getUsername());

    GameNotification notification = GameNotification.playerLeft(game, user);
    sendToTopic(game.getId(), notification);
  }

  public void notifyDraftStarted(com.fortnite.pronos.model.Game game) {
    log.info("Notifying draft started for game: {}", game.getId());

    GameNotification notification = GameNotification.draftStarted(game);
    sendToTopic(game.getId(), notification);
  }

  public void notifyDraftStarted(UUID gameId) {
    log.info("Notifying draft started for game: {}", gameId);

    GameNotification notification =
        new GameNotification("DRAFT_STARTED", gameId, null, "DRAFTING", null, "Draft has started");
    sendToTopic(gameId, notification);
  }

  public void notifyGameFinished(com.fortnite.pronos.model.Game game) {
    log.info("Notifying game finished: {}", game.getId());

    GameNotification notification = GameNotification.gameFinished(game);
    sendToTopic(game.getId(), notification);
  }

  private void sendToTopic(UUID gameId, GameNotification notification) {
    String destination = "/topic/games/" + gameId + "/events";
    messagingTemplate.convertAndSend(destination, notification);
  }

  public record GameNotification(
      String type, UUID gameId, String gameName, String status, String username, String message) {

    public static GameNotification statusChanged(com.fortnite.pronos.model.Game game) {
      return new GameNotification(
          "GAME_STATUS_CHANGED",
          game.getId(),
          game.getName(),
          game.getStatus().name(),
          null,
          "Game status changed to " + game.getStatus().name());
    }

    public static GameNotification playerJoined(
        com.fortnite.pronos.model.Game game, com.fortnite.pronos.model.User user) {
      return new GameNotification(
          "PLAYER_JOINED",
          game.getId(),
          game.getName(),
          game.getStatus().name(),
          user.getUsername(),
          user.getUsername() + " joined the game");
    }

    public static GameNotification playerLeft(
        com.fortnite.pronos.model.Game game, com.fortnite.pronos.model.User user) {
      return new GameNotification(
          "PLAYER_LEFT",
          game.getId(),
          game.getName(),
          game.getStatus().name(),
          user.getUsername(),
          user.getUsername() + " left the game");
    }

    public static GameNotification draftStarted(com.fortnite.pronos.model.Game game) {
      return new GameNotification(
          "DRAFT_STARTED",
          game.getId(),
          game.getName(),
          "DRAFTING",
          null,
          "Draft has started for " + game.getName());
    }

    public static GameNotification gameFinished(com.fortnite.pronos.model.Game game) {
      return new GameNotification(
          "GAME_FINISHED",
          game.getId(),
          game.getName(),
          "FINISHED",
          null,
          "Game " + game.getName() + " has finished");
    }
  }
}
