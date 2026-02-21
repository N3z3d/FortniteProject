package com.fortnite.pronos.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.GameStatisticsUseCase;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service pour calculer les statistiques des games */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings({"java:S1640"})
public class GameStatisticsService implements GameStatisticsUseCase {

  private final GameDomainRepositoryPort gameRepository;
  private final PlayerDomainRepositoryPort playerRepository;

  /**
   * Calcule la distribution des joueurs par region pour une game.
   *
   * @param gameId l'identifiant de la game
   * @return une map avec le nombre de joueurs par region
   */
  public Map<com.fortnite.pronos.model.Player.Region, Integer> getPlayerDistributionByRegion(
      UUID gameId) {
    log.debug("Calcul de la distribution par region pour la game {}", gameId);

    Game game = findGameOrThrow(gameId);
    return calculateRegionDistribution(game.getParticipants());
  }

  /** Trouve une game ou leve une exception. */
  private Game findGameOrThrow(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new IllegalArgumentException("Game non trouvee: " + gameId));
  }

  /** Calcule la distribution par region a partir des participants. */
  private Map<com.fortnite.pronos.model.Player.Region, Integer> calculateRegionDistribution(
      List<GameParticipant> participants) {
    Map<com.fortnite.pronos.model.Player.Region, Integer> distribution = new HashMap<>();

    for (GameParticipant participant : participants) {
      for (UUID playerId : participant.getSelectedPlayerIds()) {
        com.fortnite.pronos.model.Player.Region region = resolveRegion(playerId);
        distribution.merge(region, 1, Integer::sum);
      }
    }

    return distribution;
  }

  /**
   * Calcule la distribution des joueurs par region en pourcentage.
   *
   * @param gameId l'identifiant de la game
   * @return une map avec le pourcentage de joueurs par region
   */
  public Map<com.fortnite.pronos.model.Player.Region, Double>
      getPlayerDistributionByRegionPercentage(UUID gameId) {
    log.debug("Calcul de la distribution en pourcentage par region pour la game {}", gameId);

    Map<com.fortnite.pronos.model.Player.Region, Integer> distribution =
        getPlayerDistributionByRegion(gameId);

    return convertToPercentages(distribution);
  }

  /** Convertit une distribution absolue en pourcentages. */
  private Map<com.fortnite.pronos.model.Player.Region, Double> convertToPercentages(
      Map<com.fortnite.pronos.model.Player.Region, Integer> distribution) {
    if (distribution.isEmpty()) {
      return new HashMap<>();
    }

    int totalPlayers = calculateTotalPlayers(distribution);

    return distribution.entrySet().stream()
        .collect(
            HashMap::new,
            (map, entry) ->
                map.put(entry.getKey(), calculatePercentage(entry.getValue(), totalPlayers)),
            HashMap::putAll);
  }

  /** Calcule le nombre total de joueurs. */
  private int calculateTotalPlayers(
      Map<com.fortnite.pronos.model.Player.Region, Integer> distribution) {
    return distribution.values().stream().mapToInt(Integer::intValue).sum();
  }

  /** Calcule un pourcentage. */
  private double calculatePercentage(int value, int total) {
    return (value * 100.0) / total;
  }

  private com.fortnite.pronos.model.Player.Region resolveRegion(UUID playerId) {
    if (playerId == null) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }

    return playerRepository
        .findById(playerId)
        .map(player -> toLegacyRegion(player.getRegion()))
        .orElse(com.fortnite.pronos.model.Player.Region.UNKNOWN);
  }

  private com.fortnite.pronos.model.Player.Region toLegacyRegion(PlayerRegion region) {
    if (region == null) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }

    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(region.name());
    } catch (IllegalArgumentException ex) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }
  }
}
