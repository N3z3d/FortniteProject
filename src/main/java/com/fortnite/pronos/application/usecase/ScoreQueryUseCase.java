package com.fortnite.pronos.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fortnite.pronos.model.Score;

/** Application use case for Score query operations. Defines the public API for querying scores. */
public interface ScoreQueryUseCase {

  List<Score> getAllScores();

  List<Score> getUserLatestScores(UUID userId);

  Map<UUID, List<Score>> getPlayerScoreHistory(UUID playerId);
}
