package com.fortnite.pronos.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service de gestion des scores des joueurs et équipes */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

  private static final String USER_NOT_FOUND_MESSAGE = "Utilisateur non trouvé";
  private static final String TEAM_NOT_FOUND_MESSAGE = "Équipe non trouvée";
  private static final String PLAYER_NOT_FOUND_MESSAGE = "Joueur non trouvé";

  private final ScoreRepository scoreRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;

  /** Met à jour les scores d'un joueur pour une période donnée */
  @Transactional
  public void updatePlayerScores(UUID playerId, int points, OffsetDateTime timestamp) {
    log.info(
        "Mise à jour du score pour le joueur {} : {} points à {}", playerId, points, timestamp);

    Player player = findPlayerById(playerId);

    // Vérifier si un score existe déjà pour cette période
    OffsetDateTime dayStart = timestamp.withHour(0).withMinute(0).withSecond(0).withNano(0);
    OffsetDateTime dayEnd = dayStart.plusDays(1);

    List<Score> existingScores =
        scoreRepository.findByPlayerIdAndTimestampBetween(playerId, dayStart, dayEnd);

    if (!existingScores.isEmpty()) {
      // Mettre à jour le score existant
      Score existingScore = existingScores.get(0);
      existingScore.setPoints(points);
      existingScore.setTimestamp(timestamp);
      scoreRepository.save(existingScore);
      log.debug("Score existant mis à jour pour le joueur {}", playerId);
    } else {
      // Créer un nouveau score
      Score newScore = buildScore(player, points, timestamp);
      scoreRepository.save(newScore);
      log.debug("Nouveau score créé pour le joueur {}", playerId);
    }

    // Mettre à jour les scores des équipes contenant ce joueur
    updateTeamScoresForPlayer(playerId);

    log.info("Scores mis à jour avec succès pour le joueur {}", playerId);
  }

  /** Met à jour les scores en lot pour plusieurs joueurs */
  @Transactional
  public void updateBatchPlayerScores(Map<UUID, Integer> playerScores, OffsetDateTime timestamp) {
    log.info("Mise à jour en lot de {} scores de joueurs à {}", playerScores.size(), timestamp);

    int successCount = 0;
    int errorCount = 0;

    for (Map.Entry<UUID, Integer> entry : playerScores.entrySet()) {
      try {
        updatePlayerScores(entry.getKey(), entry.getValue(), timestamp);
        successCount++;
      } catch (Exception e) {
        log.error(
            "Erreur lors de la mise à jour du score pour le joueur {}: {}",
            entry.getKey(),
            e.getMessage());
        errorCount++;
      }
    }

    log.info("Mise à jour en lot terminée - Succès: {}, Erreurs: {}", successCount, errorCount);
  }

  /** Recalcule les scores totaux de toutes les équipes d'une saison */
  @Transactional
  public void recalculateSeasonScores(int season) {
    log.info("Recalcul des scores pour la saison {}", season);

    List<Team> teams = teamRepository.findBySeason(season);
    log.debug("{} équipes trouvées pour la saison {}", teams.size(), season);

    int updatedTeams = 0;
    for (Team team : teams) {
      try {
        int newScore = calculateTeamScore(team);

        // Note: totalScore supprimé, le score sera calculé dynamiquement
        log.debug("Score de l'équipe {} calculé: {}", team.getId(), newScore);
        updatedTeams++;
      } catch (Exception e) {
        log.error(
            "Erreur lors du recalcul du score pour l'équipe {}: {}", team.getId(), e.getMessage());
      }
    }

    log.info("Recalcul terminé pour la saison {} - {} équipes mises à jour", season, updatedTeams);
  }

  /** Récupère les derniers scores d'un utilisateur */
  @Transactional(readOnly = true)
  public List<Score> getUserLatestScores(UUID userId) {
    log.debug("Récupération des derniers scores pour l'utilisateur {}", userId);

    User user = findUserById(userId);
    Team team = findTeamByUserAndSeason(user, user.getCurrentSeason());

    List<Score> userScores =
        team.getPlayers().stream()
            .filter(tp -> tp.getUntil() == null)
            .flatMap(
                tp ->
                    scoreRepository
                        .findLatestScoreByPlayer(tp.getPlayer().getId(), OffsetDateTime.now())
                        .stream())
            .collect(Collectors.toList());

    log.info("Scores récupérés pour {} joueurs de l'utilisateur {}", userScores.size(), userId);
    return userScores;
  }

  /** Récupère l'historique des scores d'un joueur */
  @Transactional(readOnly = true)
  public Map<UUID, List<Score>> getPlayerScoreHistory(UUID playerId) {
    log.debug("Récupération de l'historique des scores pour le joueur {}", playerId);

    findPlayerById(playerId); // Validation de l'existence

    List<Score> scores =
        scoreRepository.findByPlayerIdAndTimestampBetween(
            playerId, OffsetDateTime.now().minusDays(7), OffsetDateTime.now());

    log.info("{} scores trouvés dans l'historique du joueur {}", scores.size(), playerId);
    return Map.of(playerId, scores);
  }

  /** Récupère tous les scores */
  @Transactional(readOnly = true)
  public List<Score> getAllScores() {
    log.debug("Récupération de tous les scores");
    return scoreRepository.findAll();
  }

  /** Sauvegarde un score */
  @Transactional
  public Score saveScore(Score score) {
    log.debug("Sauvegarde du score: {}", score);
    if (score.getTimestamp() == null) {
      score.setTimestamp(OffsetDateTime.now());
    }
    return scoreRepository.save(score);
  }

  /** Supprime un score (méthode simplifiée - supprime tous les scores du joueur) */
  @Transactional
  public void deleteScore(UUID playerId) {
    log.debug("Suppression des scores du joueur: {}", playerId);
    // Supprime tous les scores du joueur
    List<Score> scores = scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId);
    scoreRepository.deleteAll(scores);
  }

  // Méthodes utilitaires privées

  private User findUserById(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(
            () -> {
              log.warn("Utilisateur non trouvé avec l'ID: {}", userId);
              return new EntityNotFoundException(USER_NOT_FOUND_MESSAGE);
            });
  }

  private Team findTeamByUserAndSeason(User user, int season) {
    return teamRepository
        .findByOwnerAndSeason(user, season)
        .orElseThrow(
            () -> {
              log.warn(
                  "Équipe non trouvée pour l'utilisateur {} et la saison {}", user.getId(), season);
              return new EntityNotFoundException(TEAM_NOT_FOUND_MESSAGE);
            });
  }

  private Player findPlayerById(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .orElseThrow(
            () -> {
              log.warn("Joueur non trouvé avec l'ID: {}", playerId);
              return new EntityNotFoundException(PLAYER_NOT_FOUND_MESSAGE);
            });
  }

  private Score buildScore(Player player, int points, OffsetDateTime timestamp) {
    Score score = new Score();
    score.setPlayer(player);
    score.setPoints(points);
    score.setTimestamp(timestamp);
    return score;
  }

  private int calculateTeamScore(Team team) {
    return team.getPlayers().stream()
        .filter(tp -> tp.getUntil() == null) // Joueurs actifs uniquement
        .mapToInt(
            tp -> {
              List<Score> scores =
                  scoreRepository.findByPlayerIdOrderByTimestampDesc(tp.getPlayer().getId());
              return scores.isEmpty() ? 0 : scores.get(0).getPoints();
            })
        .sum();
  }

  private void updateTeamScoresForPlayer(UUID playerId) {
    log.debug("Mise à jour des scores d'équipes pour le joueur {}", playerId);

    List<Team> teamsWithPlayer = teamRepository.findTeamsWithActivePlayer(playerId);

    for (Team team : teamsWithPlayer) {
      try {
        int newScore = calculateTeamScore(team);
        // Note: totalScore supprimé, le score sera calculé dynamiquement

        log.debug(
            "Score de l'équipe {} calculé: {} pour le joueur {}", team.getId(), newScore, playerId);
      } catch (Exception e) {
        log.error(
            "Erreur lors du calcul du score de l'équipe {} pour le joueur {}: {}",
            team.getId(),
            playerId,
            e.getMessage());
      }
    }
  }
}
