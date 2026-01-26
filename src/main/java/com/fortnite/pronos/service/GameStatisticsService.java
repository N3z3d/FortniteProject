package com.fortnite.pronos.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.GameStatisticsUseCase;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.Player;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service pour calculer les statistiques des games */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameStatisticsService implements GameStatisticsUseCase {

  private final GameRepositoryPort gameRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;

  /**
   * Calcule la distribution des joueurs par région pour une game
   *
   * @param gameId l'identifiant de la game
   * @return une map avec le nombre de joueurs par région
   */
  public Map<Player.Region, Integer> getPlayerDistributionByRegion(UUID gameId) {
    log.debug("Calcul de la distribution par région pour la game {}", gameId);

    Game game = findGameOrThrow(gameId);
    List<GameParticipant> participants = gameParticipantRepository.findByGame(game);

    return calculateRegionDistribution(participants);
  }

  /** Trouve une game ou lève une exception Clean Code : méthode avec responsabilité unique */
  private Game findGameOrThrow(UUID gameId) {
    return gameRepository
        .findById(gameId)
        .orElseThrow(() -> new IllegalArgumentException("Game non trouvée: " + gameId));
  }

  /**
   * Calcule la distribution par région à partir des participants Clean Code : extraction de la
   * logique métier
   */
  private Map<Player.Region, Integer> calculateRegionDistribution(
      List<GameParticipant> participants) {
    Map<Player.Region, Integer> distribution = new HashMap<>();

    participants.stream()
        .flatMap(participant -> participant.getSelectedPlayers().stream())
        .forEach(player -> distribution.merge(player.getRegion(), 1, Integer::sum));

    return distribution;
  }

  /**
   * Calcule la distribution des joueurs par région en pourcentage
   *
   * @param gameId l'identifiant de la game
   * @return une map avec le pourcentage de joueurs par région
   */
  public Map<Player.Region, Double> getPlayerDistributionByRegionPercentage(UUID gameId) {
    log.debug("Calcul de la distribution en pourcentage par région pour la game {}", gameId);

    Map<Player.Region, Integer> distribution = getPlayerDistributionByRegion(gameId);

    return convertToPercentages(distribution);
  }

  /**
   * Convertit une distribution absolue en pourcentages Clean Code : méthode pure avec
   * responsabilité unique
   */
  private Map<Player.Region, Double> convertToPercentages(
      Map<Player.Region, Integer> distribution) {
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

  /** Calcule le nombre total de joueurs Clean Code : méthode focalisée */
  private int calculateTotalPlayers(Map<Player.Region, Integer> distribution) {
    return distribution.values().stream().mapToInt(Integer::intValue).sum();
  }

  /** Calcule un pourcentage Clean Code : méthode pure, testable unitairement */
  private double calculatePercentage(int value, int total) {
    return (value * 100.0) / total;
  }
}
