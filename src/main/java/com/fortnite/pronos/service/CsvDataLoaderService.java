package com.fortnite.pronos.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour charger les données des joueurs depuis le fichier CSV avec assignations par
 * pronostiqueur
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvDataLoaderService {

  private final PlayerRepository playerRepository;
  private final ScoreRepository scoreRepository;

  // Map pour stocker les assignations des joueurs par pronostiqueur
  private final Map<String, List<Player>> playersByPronosticator = new HashMap<>();

  /** Charge tous les joueurs et leurs scores depuis le fichier CSV */
  @Transactional
  public void loadAllCsvData() {
    try {
      log.info("🎮 Début du chargement des données CSV...");

      // Réinitialise les assignations en mémoire pour éviter de réutiliser des entités détachées
      playersByPronosticator.clear();

      ClassPathResource resource = new ClassPathResource("data/fortnite_data.csv");

      if (!resource.exists()) {
        log.warn("⚠️ Fichier CSV non trouvé: data/fortnite_data.csv");
        return;
      }

      List<Player> players = new ArrayList<>();
      List<Score> scores = new ArrayList<>();

      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

        String line = reader.readLine(); // Skip header
        if (line == null || !line.contains("Pronostiqueur")) {
          log.warn(
              "⚠️ Format CSV invalide - header manquant ou incorrect. Attendu: 'Pronostiqueur,Joueur,Région attribué,Score PR,Classement,Basé 2024'");
          return;
        }

        int lineNumber = 1;
        while ((line = reader.readLine()) != null) {
          lineNumber++;
          try {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 6) {
              log.warn(
                  "Ligne {} ignorée - format invalide (attendu 6 colonnes): {}", lineNumber, line);
              continue;
            }

            // Nouveau format CSV: Pronostiqueur,Joueur,Région attribué,Score PR,Classement,Basé
            // 2024
            String pronostiqueur = parts[0].trim().replace("\"", "");
            String nickname = parts[1].trim().replace("\"", "");
            String region = parts[2].trim().replace("\"", "");
            String pointsStr = parts[3].trim().replace("\"", "");
            String classementStr = parts[4].trim().replace("\"", "");
            String tranche = parts[5].trim().replace("\"", "");

            if (pronostiqueur.isEmpty() || nickname.isEmpty() || region.isEmpty()) {
              log.warn("Ligne {} ignorée - données essentielles manquantes: {}", lineNumber, line);
              continue;
            }

            // Parse points
            int points;
            try {
              points = Integer.parseInt(pointsStr);
            } catch (NumberFormatException e) {
              log.warn("Ligne {} - Points invalides '{}', utilisation de 0", lineNumber, pointsStr);
              points = 0;
            }

            // Parse classement
            int classement = 0;
            try {
              classement = Integer.parseInt(classementStr);
            } catch (NumberFormatException e) {
              log.warn(
                  "Ligne {} - Classement invalide '{}', utilisation de 0",
                  lineNumber,
                  classementStr);
            }

            // Valider la région
            Player.Region playerRegion;
            try {
              playerRegion = Player.Region.valueOf(region.toUpperCase());
            } catch (IllegalArgumentException e) {
              log.warn("Ligne {} - Région invalide '{}', utilisation de EU", lineNumber, region);
              playerRegion = Player.Region.EU;
            }

            // Créer un username valide à partir du nickname
            String cleanUsername = nickname.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (cleanUsername.isEmpty()) {
              // Si le nickname ne contient que des caractères spéciaux, générer un username basique
              cleanUsername = "player" + Math.abs(nickname.hashCode());
            }

            // Créer le joueur
            Player player =
                Player.builder()
                    .username(cleanUsername)
                    .nickname(nickname)
                    .region(playerRegion)
                    .tranche(tranche.isEmpty() ? "1-5" : tranche)
                    .currentSeason(2025)
                    .build();

            players.add(player);

            // Stocker l'assignation par pronostiqueur
            playersByPronosticator
                .computeIfAbsent(pronostiqueur, k -> new ArrayList<>())
                .add(player);

            // Créer le score associé
            Score score = new Score();
            score.setPlayer(player);
            score.setSeason(2025);
            score.setPoints(points);
            score.setDate(LocalDate.now());
            score.setTimestamp(OffsetDateTime.now());

            scores.add(score);

          } catch (Exception e) {
            log.error("Erreur ligne {} : {}", lineNumber, e.getMessage());
          }
        }
      }

      if (players.isEmpty()) {
        log.warn("⚠️ Aucun joueur valide trouvé dans le CSV");
        return;
      }

      // Sauvegarder en base
      List<Player> savedPlayers = playerRepository.saveAll(players);
      log.info("✅ {} joueurs sauvegardés", savedPlayers.size());

      // Lier les scores aux joueurs sauvegardés et sauvegarder
      for (int i = 0; i < savedPlayers.size() && i < scores.size(); i++) {
        scores.get(i).setPlayer(savedPlayers.get(i));
      }

      List<Score> savedScores = scoreRepository.saveAll(scores);
      log.info("✅ {} scores sauvegardés", savedScores.size());

      log.info("🎉 Chargement CSV complété avec succès:");
      log.info("   - {} joueurs chargés", savedPlayers.size());
      log.info("   - {} scores associés", savedScores.size());
      log.info("   - Répartition par pronostiqueur:");
      playersByPronosticator.forEach(
          (pronostiqueur, playerList) -> {
            log.info("     • {}: {} joueurs", pronostiqueur, playerList.size());
          });

    } catch (Exception e) {
      log.error("❌ Erreur lors du chargement des données CSV", e);
      throw new RuntimeException("Échec du chargement CSV", e);
    }
  }

  /** Retourne les joueurs assignés à un pronostiqueur spécifique */
  public List<Player> getPlayersByPronosticator(String pronostiqueur) {
    return playersByPronosticator.getOrDefault(pronostiqueur, new ArrayList<>());
  }

  /** Retourne tous les pronostiqueurs disponibles */
  public List<String> getAllPronosticators() {
    return new ArrayList<>(playersByPronosticator.keySet());
  }

  /** Retourne la map complète des assignations */
  public Map<String, List<Player>> getAllPlayerAssignments() {
    return new HashMap<>(playersByPronosticator);
  }

  /** Nettoie les assignations en mémoire (utile pour les tests) */
  public void clearAssignments() {
    playersByPronosticator.clear();
  }
}
