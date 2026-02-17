package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.Player;

/**
 * Output port for GameRegionRule persistence operations. Implemented by the persistence adapter
 * (GameRegionRuleRepository).
 */
public interface GameRegionRuleRepositoryPort {

  List<GameRegionRule> findByGame(Game game);

  List<GameRegionRule> findByRegion(Player.Region region);

  Optional<GameRegionRule> findByGameAndRegion(Game game, Player.Region region);

  boolean existsByGameAndRegion(Game game, Player.Region region);

  long countByGame(Game game);

  long countByRegion(Player.Region region);

  List<Player.Region> findRegionsByGame(Game game);

  List<Game> findGamesByRegion(Player.Region region);

  List<GameRegionRule> findMaxPlayerRulesByGame(Game game);

  List<GameRegionRule> findMinPlayerRulesByGame(Game game);
}
