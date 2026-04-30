package com.fortnite.pronos.service.game;

import java.util.Map;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.exception.InvalidGameRequestException;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.service.ValidationService;

public final class CreateGameRegionRulesContract {

  private static final String REQUIRED_REGION_RULES_MESSAGE =
      "regionRules est requis et ne peut pas etre vide";

  private CreateGameRegionRulesContract() {}

  public static void validateAndApply(
      Game game, Map<Player.Region, Integer> regionRules, ValidationService validationService) {
    validate(regionRules, validationService);
    regionRules.forEach(
        (region, maxPlayers) ->
            game.addRegionRule(
                new GameRegionRule(PlayerRegion.valueOf(region.name()), maxPlayers)));
  }

  public static void validate(
      Map<Player.Region, Integer> regionRules, ValidationService validationService) {
    if (regionRules == null || regionRules.isEmpty()) {
      throw new InvalidGameRequestException(REQUIRED_REGION_RULES_MESSAGE);
    }

    try {
      validationService.validateRegionRules(regionRules);
    } catch (IllegalArgumentException e) {
      throw new InvalidGameRequestException(e.getMessage(), e);
    }
  }
}
