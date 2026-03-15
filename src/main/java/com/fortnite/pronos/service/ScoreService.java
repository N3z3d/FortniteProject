package com.fortnite.pronos.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.ScoreCommandUseCase;
import com.fortnite.pronos.application.usecase.ScoreQueryUseCase;
import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service de gestion des scores des joueurs et Ã©quipes */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService implements ScoreQueryUseCase, ScoreCommandUseCase {

  private static final String USER_NOT_FOUND_MESSAGE = "Utilisateur non trouvé";
  private static final String TEAM_NOT_FOUND_MESSAGE = "Équipe non trouvée";
  private static final String PLAYER_NOT_FOUND_MESSAGE = "Joueur non trouvé";

  private final com.fortnite.pronos.repository.ScoreRepository scoreRepository;
  private final PlayerRepositoryPort playerRepository;
  private final TeamRepositoryPort teamRepository;
  private final UserRepositoryPort userRepository;

  /** Met Ã jour les scores d'un joueur pour une pÃ©riode donnÃ©e */
  @Transactional
  public void updatePlayerScores(UUID playerId, int points, OffsetDateTime timestamp) {
    log.info(
        "Mise Ã  jour du score pour le joueur {} : {} points Ã  {}", playerId, points, timestamp);

    com.fortnite.pronos.model.Player player = findPlayerById(playerId);

    // VÃ©rifier si un score existe dÃ©jÃ  pour cette pÃ©riode
    OffsetDateTime dayStart = timestamp.withHour(0).withMinute(0).withSecond(0).withNano(0);
    OffsetDateTime dayEnd = dayStart.plusDays(1);

    List<com.fortnite.pronos.model.Score> existingScores =
        scoreRepository.findByPlayerIdAndTimestampBetween(playerId, dayStart, dayEnd);

    if (!existingScores.isEmpty()) {
      // Mettre Ã  jour le score existant
      com.fortnite.pronos.model.Score existingScore = existingScores.get(0);
      existingScore.setPoints(points);
      existingScore.setTimestamp(timestamp);
      ((ScoreRepositoryPort) scoreRepository).save(existingScore);
      log.debug("Score existant mis Ã  jour pour le joueur {}", playerId);
    } else {
      // CrÃ©er un nouveau score
      com.fortnite.pronos.model.Score newScore = buildScore(player, points, timestamp);
      ((ScoreRepositoryPort) scoreRepository).save(newScore);
      log.debug("Nouveau score crÃ©Ã© pour le joueur {}", playerId);
    }

    // Mettre Ã  jour les scores des Ã©quipes contenant ce joueur
    updateTeamScoresForPlayer(playerId);

    log.info("Scores mis Ã  jour avec succÃ¨s pour le joueur {}", playerId);
  }

  /** Met Ã jour les scores en lot pour plusieurs joueurs */
  @Transactional
  public void updateBatchPlayerScores(Map<UUID, Integer> playerScores, OffsetDateTime timestamp) {
    log.info("Mise Ã  jour en lot de {} scores de joueurs Ã  {}", playerScores.size(), timestamp);

    int successCount = 0;
    int errorCount = 0;

    for (Map.Entry<UUID, Integer> entry : playerScores.entrySet()) {
      try {
        updatePlayerScores(entry.getKey(), entry.getValue(), timestamp);
        successCount++;
      } catch (Exception e) {
        log.error(
            "Erreur lors de la mise Ã  jour du score pour le joueur {}: {}",
            entry.getKey(),
            e.getMessage());
        errorCount++;
      }
    }

    log.info("Mise Ã  jour en lot terminÃ©e - SuccÃ¨s: {}, Erreurs: {}", successCount, errorCount);
  }

  /** Recalcule les scores totaux de toutes les Ã©quipes d'une saison */
  @Transactional
  public void recalculateSeasonScores(int season) {
    log.info("Recalcul des scores pour la saison {}", season);

    List<com.fortnite.pronos.model.Team> teams = teamRepository.findBySeason(season);
    log.debug("{} Ã©quipes trouvÃ©es pour la saison {}", teams.size(), season);

    int updatedTeams = 0;
    for (com.fortnite.pronos.model.Team team : teams) {
      try {
        int newScore = calculateTeamScore(team);

        // Note: totalScore supprimÃ©, le score sera calculÃ© dynamiquement
        log.debug("Score de l'Ã©quipe {} calculÃ©: {}", team.getId(), newScore);
        updatedTeams++;
      } catch (Exception e) {
        log.error(
            "Erreur lors du recalcul du score pour l'Ã©quipe {}: {}", team.getId(), e.getMessage());
      }
    }

    log.info(
        "Recalcul terminÃ© pour la saison {} - {} Ã©quipes mises Ã  jour", season, updatedTeams);
  }

  /** RÃ©cupÃ¨re les derniers scores d'un utilisateur */
  @Transactional(readOnly = true)
  public List<com.fortnite.pronos.model.Score> getUserLatestScores(UUID userId) {
    log.debug("RÃ©cupÃ©ration des derniers scores pour l'utilisateur {}", userId);

    com.fortnite.pronos.model.User user = findUserById(userId);
    com.fortnite.pronos.model.Team team = findTeamByUserAndSeason(user, user.getCurrentSeason());

    List<com.fortnite.pronos.model.Score> userScores =
        team.getPlayers().stream()
            .filter(tp -> tp.getUntil() == null)
            .flatMap(
                tp ->
                    scoreRepository
                        .findLatestScoreByPlayer(tp.getPlayer().getId(), OffsetDateTime.now())
                        .stream())
            .toList();

    log.info("Scores rÃ©cupÃ©rÃ©s pour {} joueurs de l'utilisateur {}", userScores.size(), userId);
    return userScores;
  }

  /** RÃ©cupÃ¨re l'historique des scores d'un joueur */
  @Transactional(readOnly = true)
  public Map<UUID, List<com.fortnite.pronos.model.Score>> getPlayerScoreHistory(UUID playerId) {
    log.debug("RÃ©cupÃ©ration de l'historique des scores pour le joueur {}", playerId);

    findPlayerById(playerId); // Validation de l'existence

    List<com.fortnite.pronos.model.Score> scores =
        scoreRepository.findByPlayerIdAndTimestampBetween(
            playerId, OffsetDateTime.now().minusDays(7), OffsetDateTime.now());

    log.info("{} scores trouvÃ©s dans l'historique du joueur {}", scores.size(), playerId);
    return Map.of(playerId, scores);
  }

  /** RÃ©cupÃ¨re tous les scores */
  @Transactional(readOnly = true)
  public List<com.fortnite.pronos.model.Score> getAllScores() {
    log.debug("RÃ©cupÃ©ration de tous les scores");
    return scoreRepository.findAll();
  }

  /** Sauvegarde un score */
  @Transactional
  public com.fortnite.pronos.model.Score saveScore(com.fortnite.pronos.model.Score score) {
    log.debug("Sauvegarde du score: {}", score);
    if (score.getTimestamp() == null) {
      score.setTimestamp(OffsetDateTime.now());
    }
    return ((ScoreRepositoryPort) scoreRepository).save(score);
  }

  /** Supprime un score (mÃ©thode simplifiÃ©e - supprime tous les scores du joueur) */
  @Transactional
  public void deleteScore(UUID playerId) {
    log.debug("Suppression des scores du joueur: {}", playerId);
    // Supprime tous les scores du joueur
    List<com.fortnite.pronos.model.Score> scores =
        scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId);
    scoreRepository.deleteAll(scores);
  }

  // MÃ©thodes utilitaires privÃ©es

  private com.fortnite.pronos.model.User findUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () -> {
              log.warn("Utilisateur non trouvé avec l'ID: {}", userId);
              return new EntityNotFoundException(USER_NOT_FOUND_MESSAGE);
            });
  }

  private com.fortnite.pronos.model.Team findTeamByUserAndSeason(
      com.fortnite.pronos.model.User user, int season) {
    return teamRepository
        .findByOwnerAndSeason(user, season)
        .orElseThrow(
            () -> {
              log.warn(
                  "Équipe non trouvée pour l'utilisateur {} et la saison {}", user.getId(), season);
              return new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE);
            });
  }

  private com.fortnite.pronos.model.Player findPlayerById(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .orElseThrow(
            () -> {
              log.warn("Joueur non trouvé avec l'ID: {}", playerId);
              return new EntityNotFoundException(PLAYER_NOT_FOUND_MESSAGE);
            });
  }

  private com.fortnite.pronos.model.Score buildScore(
      com.fortnite.pronos.model.Player player, int points, OffsetDateTime timestamp) {
    com.fortnite.pronos.model.Score score = new com.fortnite.pronos.model.Score();
    score.setPlayer(player);
    score.setPoints(points);
    score.setTimestamp(timestamp);
    return score;
  }

  private int calculateTeamScore(com.fortnite.pronos.model.Team team) {
    return team.getPlayers().stream()
        .filter(tp -> tp.getUntil() == null) // Joueurs actifs uniquement
        .mapToInt(
            tp -> {
              List<com.fortnite.pronos.model.Score> scores =
                  scoreRepository.findByPlayerIdOrderByTimestampDesc(tp.getPlayer().getId());
              return scores.isEmpty() ? 0 : scores.get(0).getPoints();
            })
        .sum();
  }

  private void updateTeamScoresForPlayer(UUID playerId) {
    log.debug("Mise Ã  jour des scores d'Ã©quipes pour le joueur {}", playerId);

    List<com.fortnite.pronos.model.Team> teamsWithPlayer =
        teamRepository.findTeamsWithActivePlayer(playerId);

    for (com.fortnite.pronos.model.Team team : teamsWithPlayer) {
      try {
        int newScore = calculateTeamScore(team);
        // Note: totalScore supprimÃ©, le score sera calculÃ© dynamiquement

        log.debug(
            "Score de l'Ã©quipe {} calculÃ©: {} pour le joueur {}",
            team.getId(),
            newScore,
            playerId);
      } catch (Exception e) {
        log.error(
            "Erreur lors du calcul du score de l'Ã©quipe {} pour le joueur {}: {}",
            team.getId(),
            playerId,
            e.getMessage());
      }
    }
  }
}
