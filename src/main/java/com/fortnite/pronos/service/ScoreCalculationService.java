package com.fortnite.pronos.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamPlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamRepositoryPort;
import com.fortnite.pronos.dto.TeamScoreDto;
import com.fortnite.pronos.dto.TeamScoreDto.PlayerScore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour calculer les scores des Ã©quipes Clean Code : Service focalisÃ© sur le calcul des
 * scores
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoreCalculationService {

  private final TeamRepositoryPort teamRepository;
  private final TeamPlayerRepositoryPort teamPlayerRepository;
  private final ScoreRepositoryPort scoreRepository;

  /**
   * Calcule le score total d'une Ã©quipe sur une pÃ©riode donnÃ©e Clean Code : mÃ©thode principale
   * qui orchestre le calcul
   */
  public TeamScoreDto calculateTeamScore(UUID teamId, LocalDate startDate, LocalDate endDate) {
    log.debug("Calcul du score pour l'Ã©quipe {} du {} au {}", teamId, startDate, endDate);

    // Valider les dates
    validateDates(startDate, endDate);

    // RÃ©cupÃ©rer l'Ã©quipe
    com.fortnite.pronos.model.Team team = findTeamOrThrow(teamId);

    // RÃ©cupÃ©rer les joueurs de l'Ã©quipe
    List<com.fortnite.pronos.model.TeamPlayer> teamPlayers = teamPlayerRepository.findByTeam(team);

    // Calculer les scores par joueur
    List<PlayerScore> playerScores = calculatePlayerScores(teamPlayers, startDate, endDate);

    // Calculer le score total
    int totalScore = calculateTotalScore(playerScores);

    return buildTeamScoreDto(team, totalScore, startDate, endDate, playerScores);
  }

  /** Valide que les dates sont cohÃ©rentes Clean Code : validation isolÃ©e */
  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("La date de fin doit Ãªtre aprÃ¨s la date de dÃ©but");
    }
  }

  /** Trouve une Ã©quipe ou lance une exception Clean Code : responsabilitÃ© unique */
  private com.fortnite.pronos.model.Team findTeamOrThrow(UUID teamId) {
    return teamRepository
        .findByIdWithFetch(teamId)
        .orElseThrow(() -> new IllegalArgumentException("Ã‰quipe non trouvÃ©e : " + teamId));
  }

  /**
   * Calcule les scores pour chaque joueur de l'Ã©quipe Clean Code : mÃ©thode focalisÃ©e sur une
   * tÃ¢che
   */
  private List<PlayerScore> calculatePlayerScores(
      List<com.fortnite.pronos.model.TeamPlayer> teamPlayers,
      LocalDate startDate,
      LocalDate endDate) {

    List<PlayerScore> playerScores = new ArrayList<>();

    for (com.fortnite.pronos.model.TeamPlayer teamPlayer : teamPlayers) {
      PlayerScore playerScore =
          calculateSinglePlayerScore(teamPlayer.getPlayer(), startDate, endDate);
      playerScores.add(playerScore);
    }

    return playerScores;
  }

  /**
   * Calcule le score d'un joueur sur la pÃ©riode Clean Code : mÃ©thode pure, testable unitairement
   */
  private PlayerScore calculateSinglePlayerScore(
      com.fortnite.pronos.model.Player player, LocalDate startDate, LocalDate endDate) {

    // RÃ©cupÃ©rer les scores du joueur sur la pÃ©riode
    List<com.fortnite.pronos.model.Score> scores =
        scoreRepository.findByPlayerAndDateBetween(player, startDate, endDate);

    // Extraire les points
    List<Integer> points = scores.stream().map(com.fortnite.pronos.model.Score::getPoints).toList();

    // CrÃ©er le PlayerScore avec statistiques
    return PlayerScore.fromScores(
        player.getId(), player.getNickname(), player.getRegion().name(), points);
  }

  /** Calcule le score total de l'Ã©quipe Clean Code : mÃ©thode simple et claire */
  private int calculateTotalScore(List<PlayerScore> playerScores) {
    return playerScores.stream().mapToInt(PlayerScore::getTotalPoints).sum();
  }

  /** Construit le DTO de rÃ©sultat Clean Code : construction du DTO isolÃ©e */
  private TeamScoreDto buildTeamScoreDto(
      com.fortnite.pronos.model.Team team,
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
