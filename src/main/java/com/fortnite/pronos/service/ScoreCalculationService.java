package com.fortnite.pronos.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.TeamScoreDto;
import com.fortnite.pronos.dto.TeamScoreDto.PlayerScore;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamPlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour calculer les scores des équipes Clean Code : Service focalisé sur le calcul des
 * scores
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreCalculationService {

  private final TeamRepository teamRepository;
  private final TeamPlayerRepository teamPlayerRepository;
  private final ScoreRepository scoreRepository;

  /**
   * Calcule le score total d'une équipe sur une période donnée Clean Code : méthode principale qui
   * orchestre le calcul
   */
  public TeamScoreDto calculateTeamScore(UUID teamId, LocalDate startDate, LocalDate endDate) {
    log.debug("Calcul du score pour l'équipe {} du {} au {}", teamId, startDate, endDate);

    // Valider les dates
    validateDates(startDate, endDate);

    // Récupérer l'équipe
    Team team = findTeamOrThrow(teamId);

    // Récupérer les joueurs de l'équipe
    List<TeamPlayer> teamPlayers = teamPlayerRepository.findByTeam(team);

    // Calculer les scores par joueur
    List<PlayerScore> playerScores = calculatePlayerScores(teamPlayers, startDate, endDate);

    // Calculer le score total
    int totalScore = calculateTotalScore(playerScores);

    return buildTeamScoreDto(team, totalScore, startDate, endDate, playerScores);
  }

  /** Valide que les dates sont cohérentes Clean Code : validation isolée */
  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("La date de fin doit être après la date de début");
    }
  }

  /** Trouve une équipe ou lance une exception Clean Code : responsabilité unique */
  private Team findTeamOrThrow(UUID teamId) {
    return teamRepository
        .findById(teamId)
        .orElseThrow(() -> new IllegalArgumentException("Équipe non trouvée : " + teamId));
  }

  /**
   * Calcule les scores pour chaque joueur de l'équipe Clean Code : méthode focalisée sur une tâche
   */
  private List<PlayerScore> calculatePlayerScores(
      List<TeamPlayer> teamPlayers, LocalDate startDate, LocalDate endDate) {

    List<PlayerScore> playerScores = new ArrayList<>();

    for (TeamPlayer teamPlayer : teamPlayers) {
      PlayerScore playerScore =
          calculateSinglePlayerScore(teamPlayer.getPlayer(), startDate, endDate);
      playerScores.add(playerScore);
    }

    return playerScores;
  }

  /**
   * Calcule le score d'un joueur sur la période Clean Code : méthode pure, testable unitairement
   */
  private PlayerScore calculateSinglePlayerScore(
      com.fortnite.pronos.model.Player player, LocalDate startDate, LocalDate endDate) {

    // Récupérer les scores du joueur sur la période
    List<Score> scores = scoreRepository.findByPlayerAndDateBetween(player, startDate, endDate);

    // Extraire les points
    List<Integer> points = scores.stream().map(Score::getPoints).collect(Collectors.toList());

    // Créer le PlayerScore avec statistiques
    return PlayerScore.fromScores(
        player.getId(), player.getNickname(), player.getRegion().name(), points);
  }

  /** Calcule le score total de l'équipe Clean Code : méthode simple et claire */
  private int calculateTotalScore(List<PlayerScore> playerScores) {
    return playerScores.stream().mapToInt(PlayerScore::getTotalPoints).sum();
  }

  /** Construit le DTO de résultat Clean Code : construction du DTO isolée */
  private TeamScoreDto buildTeamScoreDto(
      Team team,
      int totalScore,
      LocalDate startDate,
      LocalDate endDate,
      List<PlayerScore> playerScores) {

    return TeamScoreDto.builder()
        .teamId(team.getId())
        .teamName(team.getName())
        .totalScore(totalScore)
        .startDate(startDate)
        .endDate(endDate)
        .playerScores(playerScores)
        .build();
  }
}
