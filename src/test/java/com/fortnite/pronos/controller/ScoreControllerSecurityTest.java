package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.application.usecase.ScoreCommandUseCase;
import com.fortnite.pronos.application.usecase.ScoreQueryUseCase;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
class ScoreControllerSecurityTest {

  @Mock private ScoreQueryUseCase scoreQueryUseCase;
  @Mock private ScoreCommandUseCase scoreCommandUseCase;
  @Mock private com.fortnite.pronos.service.UserResolver userResolver;

  @InjectMocks private ScoreController scoreController;

  private HttpServletRequest httpRequest;
  private User adminUser;
  private User regularUser;

  @BeforeEach
  void setUp() {
    httpRequest = new MockHttpServletRequest();
    adminUser = new User();
    adminUser.setId(UUID.randomUUID());
    adminUser.setUsername("admin");
    adminUser.setRole(User.UserRole.ADMIN);

    regularUser = new User();
    regularUser.setId(UUID.randomUUID());
    regularUser.setUsername("user");
    regularUser.setRole(User.UserRole.USER);
  }

  @Test
  void shouldReturnUnauthorizedWhenUpdatingScoreWithoutResolvedUser() {
    UUID playerId = UUID.randomUUID();
    ScoreController.ScoreUpdateRequest request =
        new ScoreController.ScoreUpdateRequest(100, OffsetDateTime.now());
    when(userResolver.resolve(null, httpRequest)).thenReturn(null);

    ResponseEntity<Void> response =
        scoreController.updatePlayerScore(playerId, request, null, httpRequest);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verify(scoreCommandUseCase, never()).updatePlayerScores(any(UUID.class), anyInt(), any());
  }

  @Test
  void shouldReturnForbiddenWhenUpdatingScoreWithNonAdminUser() {
    UUID playerId = UUID.randomUUID();
    ScoreController.ScoreUpdateRequest request =
        new ScoreController.ScoreUpdateRequest(100, OffsetDateTime.now());
    when(userResolver.resolve(null, httpRequest)).thenReturn(regularUser);

    ResponseEntity<Void> response =
        scoreController.updatePlayerScore(playerId, request, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(scoreCommandUseCase, never()).updatePlayerScores(any(UUID.class), anyInt(), any());
  }

  @Test
  void shouldAllowUpdatingScoreForAdminUser() {
    UUID playerId = UUID.randomUUID();
    ScoreController.ScoreUpdateRequest request =
        new ScoreController.ScoreUpdateRequest(100, OffsetDateTime.now());
    when(userResolver.resolve(null, httpRequest)).thenReturn(adminUser);

    ResponseEntity<Void> response =
        scoreController.updatePlayerScore(playerId, request, null, httpRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(scoreCommandUseCase).updatePlayerScores(playerId, request.points(), request.timestamp());
  }

  @Test
  void shouldReturnForbiddenForBatchUpdateWithNonAdminUser() {
    ScoreController.BatchScoreUpdateRequest request =
        new ScoreController.BatchScoreUpdateRequest(
            Map.of(UUID.randomUUID(), 42), OffsetDateTime.now());
    when(userResolver.resolve(null, httpRequest)).thenReturn(regularUser);

    ResponseEntity<Void> response =
        scoreController.updateBatchPlayerScores(request, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(scoreCommandUseCase, never()).updateBatchPlayerScores(anyMap(), any());
  }

  @Test
  void shouldReturnForbiddenForSeasonRecalculationWithNonAdminUser() {
    when(userResolver.resolve(null, httpRequest)).thenReturn(regularUser);

    ResponseEntity<Void> response =
        scoreController.recalculateSeasonScores(2026, null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(scoreCommandUseCase, never()).recalculateSeasonScores(anyInt());
  }

  @Test
  void shouldReturnForbiddenWhenCreatingScoreWithNonAdminUser() {
    when(userResolver.resolve(null, httpRequest)).thenReturn(regularUser);

    ResponseEntity<Score> response = scoreController.createScore(new Score(), null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(scoreCommandUseCase, never()).saveScore(any(Score.class));
  }

  @Test
  void shouldReturnForbiddenWhenDeletingScoresWithNonAdminUser() {
    when(userResolver.resolve(null, httpRequest)).thenReturn(regularUser);

    ResponseEntity<Void> response =
        scoreController.deletePlayerScores(UUID.randomUUID(), null, httpRequest);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    verify(scoreCommandUseCase, never()).deleteScore(any(UUID.class));
  }

  @Test
  void shouldAllowDeletingScoresForAdminUser() {
    UUID playerId = UUID.randomUUID();
    when(userResolver.resolve(anyString(), any(HttpServletRequest.class))).thenReturn(adminUser);

    ResponseEntity<Void> response =
        scoreController.deletePlayerScores(playerId, "admin", httpRequest);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    verify(scoreCommandUseCase).deleteScore(playerId);
  }
}
