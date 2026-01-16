package com.fortnite.pronos.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.GameNotificationService.GameNotification;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameNotificationService Tests")
class GameNotificationServiceTest {

  @Mock private SimpMessagingTemplate messagingTemplate;

  private GameNotificationService gameNotificationService;

  @BeforeEach
  void setUp() {
    gameNotificationService = new GameNotificationService(messagingTemplate);
  }

  private Game createTestGame() {
    Game game = new Game();
    game.setId(UUID.randomUUID());
    game.setName("Test Game");
    game.setStatus(GameStatus.CREATING);
    return game;
  }

  private User createTestUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }

  @Nested
  @DisplayName("Status Changed Notifications")
  class StatusChangedNotifications {

    @Test
    @DisplayName("Should send notification when game status changes")
    void shouldSendStatusChangedNotification() {
      // Given
      Game game = createTestGame();
      game.setStatus(GameStatus.DRAFTING);
      String expectedTopic = "/topic/games/" + game.getId() + "/events";

      // When
      gameNotificationService.notifyGameStatusChanged(game);

      // Then
      verify(messagingTemplate).convertAndSend(eq(expectedTopic), any(GameNotification.class));
    }
  }

  @Nested
  @DisplayName("Player Joined Notifications")
  class PlayerJoinedNotifications {

    @Test
    @DisplayName("Should send notification when player joins game")
    void shouldSendPlayerJoinedNotification() {
      // Given
      Game game = createTestGame();
      User user = createTestUser("testplayer");
      String expectedTopic = "/topic/games/" + game.getId() + "/events";

      // When
      gameNotificationService.notifyPlayerJoined(game, user);

      // Then
      verify(messagingTemplate).convertAndSend(eq(expectedTopic), any(GameNotification.class));
    }
  }

  @Nested
  @DisplayName("Player Left Notifications")
  class PlayerLeftNotifications {

    @Test
    @DisplayName("Should send notification when player leaves game")
    void shouldSendPlayerLeftNotification() {
      // Given
      Game game = createTestGame();
      User user = createTestUser("leavingplayer");
      String expectedTopic = "/topic/games/" + game.getId() + "/events";

      // When
      gameNotificationService.notifyPlayerLeft(game, user);

      // Then
      verify(messagingTemplate).convertAndSend(eq(expectedTopic), any(GameNotification.class));
    }
  }

  @Nested
  @DisplayName("Draft Started Notifications")
  class DraftStartedNotifications {

    @Test
    @DisplayName("Should send notification when draft starts")
    void shouldSendDraftStartedNotification() {
      // Given
      Game game = createTestGame();
      game.setStatus(GameStatus.DRAFTING);
      String expectedTopic = "/topic/games/" + game.getId() + "/events";

      // When
      gameNotificationService.notifyDraftStarted(game);

      // Then
      verify(messagingTemplate).convertAndSend(eq(expectedTopic), any(GameNotification.class));
    }
  }

  @Nested
  @DisplayName("Game Finished Notifications")
  class GameFinishedNotifications {

    @Test
    @DisplayName("Should send notification when game finishes")
    void shouldSendGameFinishedNotification() {
      // Given
      Game game = createTestGame();
      game.setStatus(GameStatus.FINISHED);
      String expectedTopic = "/topic/games/" + game.getId() + "/events";

      // When
      gameNotificationService.notifyGameFinished(game);

      // Then
      verify(messagingTemplate).convertAndSend(eq(expectedTopic), any(GameNotification.class));
    }
  }
}
