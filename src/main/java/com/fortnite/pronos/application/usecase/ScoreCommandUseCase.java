package com.fortnite.pronos.application.usecase;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.fortnite.pronos.model.Score;

/**
 * Application use case for Score command operations. Defines the public API for modifying scores.
 */
public interface ScoreCommandUseCase {

  void updatePlayerScores(UUID playerId, int points, OffsetDateTime timestamp);

  void updateBatchPlayerScores(Map<UUID, Integer> playerScores, OffsetDateTime timestamp);

  void recalculateSeasonScores(int season);

  Score saveScore(Score score);

  void deleteScore(UUID playerId);
}
