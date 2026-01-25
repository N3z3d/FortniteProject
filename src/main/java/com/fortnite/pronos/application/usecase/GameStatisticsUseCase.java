package com.fortnite.pronos.application.usecase;

import java.util.Map;
import java.util.UUID;

import com.fortnite.pronos.model.Player;

public interface GameStatisticsUseCase {

  Map<Player.Region, Integer> getPlayerDistributionByRegion(UUID gameId);

  Map<Player.Region, Double> getPlayerDistributionByRegionPercentage(UUID gameId);
}
