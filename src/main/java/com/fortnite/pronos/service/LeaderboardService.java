package com.fortnite.pronos.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;
import com.fortnite.pronos.dto.LeaderboardStatsDTO;
import com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO;
import com.fortnite.pronos.dto.PronostiqueurLeaderboardEntryDTO;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardService {

  private final TeamRepository teamRepository;
  private final ScoreRepository scoreRepository;
  private final PlayerRepository playerRepository;
  private final UserRepository userRepository;

  /** Obtenir le leaderboard complet - VERSION OPTIMISÉE SANS N+1 + CACHE */
  @Transactional(readOnly = true)
  @Cacheable(value = "leaderboard", key = "#season", unless = "#result.isEmpty()")
  public List<LeaderboardEntryDTO> getLeaderboard(int season) {
    log.info("🏆 Récupération du leaderboard pour la saison {} - VERSION OPTIMISÉE", season);

    // 1. Récupérer toutes les équipes avec FETCH EAGER optimisé
    List<Team> teams = teamRepository.findBySeasonWithFetch(season);
    log.debug("📊 {} équipes trouvées", teams.size());

    // 2. OPTIMISATION: Récupérer tous les scores en UNE SEULE requête
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
    log.debug("⚡ {} scores récupérés en une seule requête optimisée", playerPointsMap.size());

    // 3. Construire le leaderboard sans requêtes supplémentaires
    List<LeaderboardEntryDTO> entries = new ArrayList<>();

    for (Team team : teams) {
      long totalPoints = 0;
      List<LeaderboardEntryDTO.PlayerInfo> playerInfos = new ArrayList<>();

      // Récupérer les joueurs de l'équipe (déjà en mémoire via fetch)
      for (TeamPlayer teamPlayer : team.getPlayers()) {
        if (!teamPlayer.isActive()) continue;

        Player player = teamPlayer.getPlayer();

        // Utiliser les points du cache au lieu d'une nouvelle requête
        Integer points = playerPointsMap.getOrDefault(player.getId(), 0);
        totalPoints += points;

        playerInfos.add(
            LeaderboardEntryDTO.PlayerInfo.builder()
                .playerId(player.getId())
                .username(player.getUsername())
                .nickname(player.getNickname())
                .region(player.getRegion())
                .tranche(player.getTranche())
                .points(points.longValue())
                .build());
      }

      entries.add(
          LeaderboardEntryDTO.builder()
              .teamId(team.getId())
              .teamName(team.getName())
              .ownerId(team.getOwner().getId())
              .ownerUsername(team.getOwner().getUsername())
              .totalPoints(totalPoints)
              .players(playerInfos)
              .build());
    }

    // 4. Trier par points décroissants et assigner les rangs
    entries.sort((a, b) -> Long.compare(b.getTotalPoints(), a.getTotalPoints()));

    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }

    log.info("✅ Leaderboard généré avec {} équipes - SANS N+1 queries", entries.size());
    return entries;
  }

  /** Obtenir le leaderboard pour une game spécifique */
  @Transactional(readOnly = true)
  public List<LeaderboardEntryDTO> getLeaderboardByGame(UUID gameId) {
    log.info("🏆 Récupération du leaderboard pour la game {}", gameId);

    // 1. Récupérer les équipes de cette game avec FETCH EAGER
    List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);
    log.debug("📊 {} équipes trouvées pour la game {}", teams.size(), gameId);

    if (teams.isEmpty()) {
      log.warn("⚠️ Aucune équipe trouvée pour la game {}", gameId);
      return new ArrayList<>();
    }

    // 2. Récupérer tous les scores en UNE SEULE requête
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(2025);

    // 3. Construire le leaderboard
    List<LeaderboardEntryDTO> entries = new ArrayList<>();

    for (Team team : teams) {
      long totalPoints = 0;
      List<LeaderboardEntryDTO.PlayerInfo> playerInfos = new ArrayList<>();

      for (TeamPlayer teamPlayer : team.getPlayers()) {
        if (!teamPlayer.isActive()) continue;

        Player player = teamPlayer.getPlayer();
        Integer points = playerPointsMap.getOrDefault(player.getId(), 0);
        totalPoints += points;

        playerInfos.add(
            LeaderboardEntryDTO.PlayerInfo.builder()
                .playerId(player.getId())
                .username(player.getUsername())
                .nickname(player.getNickname())
                .region(player.getRegion())
                .tranche(player.getTranche())
                .points(points.longValue())
                .build());
      }

      entries.add(
          LeaderboardEntryDTO.builder()
              .teamId(team.getId())
              .teamName(team.getName())
              .ownerId(team.getOwner().getId())
              .ownerUsername(team.getOwner().getUsername())
              .totalPoints(totalPoints)
              .players(playerInfos)
              .build());
    }

    // 4. Trier par points décroissants et assigner les rangs
    entries.sort((a, b) -> Long.compare(b.getTotalPoints(), a.getTotalPoints()));

    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }

    log.info("✅ Leaderboard game {} généré avec {} équipes", gameId, entries.size());
    return entries;
  }

  /** Récupère le classement d'une équipe spécifique */
  public LeaderboardEntryDTO getTeamRanking(String teamId) {
    UUID teamUuid = UUID.fromString(teamId);
    Optional<Team> teamOpt = teamRepository.findById(teamUuid);
    if (teamOpt.isEmpty()) {
      throw new RuntimeException("Équipe non trouvée");
    }

    Team team = teamOpt.get();
    List<LeaderboardEntryDTO> allEntries = getLeaderboard(team.getSeason());

    return allEntries.stream()
        .filter(entry -> entry.getTeamId().equals(teamUuid))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Équipe non trouvée dans le classement"));
  }

  /** Obtenir les statistiques du leaderboard - VERSION OPTIMISÉE SANS N+1 */
  @Cacheable(value = "gameStats", key = "'stats_default'")
  public LeaderboardStatsDTO getLeaderboardStats() {
    return getLeaderboardStats(2025); // Utiliser saison 2025 par défaut, cohérent avec le front-end
  }

  /** Obtenir les statistiques du leaderboard pour une saison spécifique */
  @Cacheable(value = "gameStats", key = "#season")
  public LeaderboardStatsDTO getLeaderboardStats(int season) {
    log.info("🔍 Calcul des statistiques pour la saison {}", season);

    List<Team> teams = teamRepository.findBySeason(season);
    log.info("📊 Équipes trouvées: {}", teams.size());

    // OPTIMISATION: Une seule requête pour tous les joueurs
    List<Player> allPlayers = playerRepository.findAll();
    int totalPlayers = allPlayers.size();
    log.info("🎮 Total joueurs trouvés: {}", totalPlayers);

    // OPTIMISATION CRITIQUE: Récupérer TOUS les scores en UNE SEULE requête
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
    log.info("⚡ Scores récupérés: {} joueurs ont des scores", playerPointsMap.size());

    int totalTeams = teams.size();
    long totalPoints = 0;
    Map<String, Long> regionPoints = new HashMap<>();

    // Calculer les points totaux et les stats régionales EN MÉMOIRE
    for (Player player : allPlayers) {
      String region = player.getRegion().name();
      Integer playerPoints = playerPointsMap.getOrDefault(player.getId(), 0);
      long points = playerPoints != null ? playerPoints.longValue() : 0L;

      totalPoints += points;
      regionPoints.merge(region, points, Long::sum);
    }

    // Calculer la moyenne des points par équipe
    double averagePoints = totalTeams > 0 ? (double) totalPoints / totalTeams : 0.0;

    log.info(
        "🎯 Stats calculées pour saison {} - {} équipes, {} joueurs total, {} points total",
        season,
        totalTeams,
        totalPlayers,
        totalPoints);

    return LeaderboardStatsDTO.builder()
        .totalTeams(totalTeams)
        .totalPlayers(totalPlayers)
        .totalPoints(totalPoints)
        .averagePoints(averagePoints)
        .regionStats(regionPoints)
        .build();
  }

  /** Obtenir les statistiques du leaderboard pour une game spécifique */
  @Transactional(readOnly = true)
  public LeaderboardStatsDTO getLeaderboardStatsByGame(UUID gameId) {
    log.info("🔍 Calcul des statistiques pour la game {}", gameId);

    List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);
    log.info("📊 Équipes trouvées pour la game: {}", teams.size());

    if (teams.isEmpty()) {
      return LeaderboardStatsDTO.builder()
          .totalTeams(0)
          .totalPlayers(0)
          .totalPoints(0L)
          .averagePoints(0.0)
          .regionStats(new HashMap<>())
          .build();
    }

    // Extraire les joueurs des équipes de cette game
    Set<UUID> playerIds = new HashSet<>();
    for (Team team : teams) {
      for (TeamPlayer tp : team.getPlayers()) {
        if (tp.isActive()) {
          playerIds.add(tp.getPlayer().getId());
        }
      }
    }

    List<Player> gamePlayers = playerRepository.findAllById(playerIds);
    int totalPlayers = gamePlayers.size();

    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(2025);

    int totalTeams = teams.size();
    long totalPoints = 0;
    Map<String, Long> regionPoints = new HashMap<>();

    for (Player player : gamePlayers) {
      String region = player.getRegion().name();
      Integer playerPoints = playerPointsMap.getOrDefault(player.getId(), 0);
      long points = playerPoints != null ? playerPoints.longValue() : 0L;

      totalPoints += points;
      regionPoints.merge(region, points, Long::sum);
    }

    double averagePoints = totalTeams > 0 ? (double) totalPoints / totalTeams : 0.0;

    log.info(
        "🎯 Stats calculées pour game {} - {} équipes, {} joueurs, {} points",
        gameId,
        totalTeams,
        totalPlayers,
        totalPoints);

    return LeaderboardStatsDTO.builder()
        .totalTeams(totalTeams)
        .totalPlayers(totalPlayers)
        .totalPoints(totalPoints)
        .averagePoints(averagePoints)
        .regionStats(regionPoints)
        .build();
  }

  public Map<String, Object> getDebugStats(int season) {
    Map<String, Object> debug = new HashMap<>();

    long teamCount = teamRepository.findBySeason(season).size();
    debug.put("totalTeams", teamCount);
    debug.put("season", season);

    long playerCount = playerRepository.count();
    debug.put("totalPlayers", playerCount);

    long scoreCount = scoreRepository.count();
    debug.put("totalScores", scoreCount);

    List<Object[]> rawScores = scoreRepository.findAllBySeasonGroupedByPlayerRaw(season);
    debug.put("rawScoresCount", rawScores.size());
    debug.put(
        "rawScoresSample",
        rawScores.stream()
            .limit(3)
            .map(row -> Map.of("playerId", row[0], "totalPoints", row[1]))
            .collect(Collectors.toList()));

    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
    debug.put("playerPointsMapSize", playerPointsMap.size());
    debug.put(
        "totalPointsFromMap", playerPointsMap.values().stream().mapToInt(Integer::intValue).sum());

    return debug;
  }

  public Map<String, Object> getDebugSimple() {
    Map<String, Object> debug = new HashMap<>();

    debug.put("totalPlayers", playerRepository.count());
    debug.put("totalTeams", teamRepository.count());
    debug.put("totalScores", scoreRepository.count());

    debug.put(
        "playersSample",
        playerRepository.findAll().stream()
            .limit(3)
            .map(
                player ->
                    Map.of(
                        "id",
                        player.getId(),
                        "nickname",
                        player.getNickname(),
                        "region",
                        player.getRegion().name(),
                        "season",
                        player.getCurrentSeason()))
            .collect(Collectors.toList()));

    debug.put(
        "scoresSample",
        scoreRepository.findAll().stream()
            .limit(3)
            .map(
                score ->
                    Map.of(
                        "playerNickname",
                        score.getPlayer().getNickname(),
                        "season",
                        score.getSeason(),
                        "points",
                        score.getPoints()))
            .collect(Collectors.toList()));

    return debug;
  }

  /** Obtenir la répartition par région de tous les joueurs */
  @Cacheable(value = "regionDistribution", key = "'all_regions'")
  public Map<String, Integer> getRegionDistribution() {
    List<Player> allPlayers = playerRepository.findAll();
    Map<String, Integer> regionCounts = new HashMap<>();

    for (Player player : allPlayers) {
      String region = player.getRegion().name();
      regionCounts.merge(region, 1, Integer::sum);
    }

    log.info("🌍 Répartition par région: {}", regionCounts);
    return regionCounts;
  }

  /** Obtenir la répartition par région pour une game spécifique */
  @Transactional(readOnly = true)
  public Map<String, Integer> getRegionDistributionByGame(UUID gameId) {
    List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);

    Set<UUID> playerIds = new HashSet<>();
    for (Team team : teams) {
      for (TeamPlayer tp : team.getPlayers()) {
        if (tp.isActive()) {
          playerIds.add(tp.getPlayer().getId());
        }
      }
    }

    List<Player> gamePlayers = playerRepository.findAllById(playerIds);
    Map<String, Integer> regionCounts = new HashMap<>();

    for (Player player : gamePlayers) {
      String region = player.getRegion().name();
      regionCounts.merge(region, 1, Integer::sum);
    }

    log.info("🌍 Répartition par région pour game {}: {}", gameId, regionCounts);
    return regionCounts;
  }

  /** Obtenir la répartition par tranche de tous les joueurs */
  @Cacheable(value = "regionDistribution", key = "'all_tranches'")
  public Map<String, Integer> getTrancheDistribution() {
    List<Player> allPlayers = playerRepository.findAll();
    Map<String, Integer> trancheCounts = new HashMap<>();

    for (Player player : allPlayers) {
      String tranche = "Tranche " + player.getTranche();
      trancheCounts.merge(tranche, 1, Integer::sum);
    }

    log.info("📊 Répartition par tranche: {}", trancheCounts);
    return trancheCounts;
  }

  /** Obtenir le classement des pronostiqueurs */
  @Transactional(readOnly = true)
  @Cacheable(value = "leaderboard", key = "'pronostiqueurs_' + #season")
  public List<PronostiqueurLeaderboardEntryDTO> getPronostiqueurLeaderboard(int season) {
    log.info("👥 Récupération du classement des pronostiqueurs - Saison: {}", season);

    // 1. Récupérer toutes les équipes avec leurs propriétaires
    List<Team> teams = teamRepository.findBySeasonWithFetch(season);

    // 2. Récupérer tous les scores en une seule requête
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);

    // 3. Grouper par pronostiqueur et calculer les statistiques
    Map<UUID, List<Team>> teamsByUser =
        teams.stream().collect(Collectors.groupingBy(team -> team.getOwner().getId()));

    List<PronostiqueurLeaderboardEntryDTO> entries = new ArrayList<>();

    for (Map.Entry<UUID, List<Team>> entry : teamsByUser.entrySet()) {
      UUID userId = entry.getKey();
      List<Team> userTeams = entry.getValue();
      User user = userTeams.get(0).getOwner(); // Récupérer l'utilisateur

      int totalPoints = 0;
      int bestTeamPoints = 0;
      String bestTeamName = "";

      // Calculer les points pour chaque équipe de l'utilisateur
      for (Team team : userTeams) {
        int teamPoints = 0;
        for (TeamPlayer teamPlayer : team.getPlayers()) {
          if (teamPlayer.isActive()) {
            teamPoints += playerPointsMap.getOrDefault(teamPlayer.getPlayer().getId(), 0);
          }
        }
        totalPoints += teamPoints;

        if (teamPoints > bestTeamPoints) {
          bestTeamPoints = teamPoints;
          bestTeamName = team.getName();
        }
      }

      int avgPointsPerTeam = userTeams.size() > 0 ? totalPoints / userTeams.size() : 0;

      entries.add(
          PronostiqueurLeaderboardEntryDTO.builder()
              .userId(userId)
              .username(user.getUsername())
              .email(user.getEmail())
              .totalPoints(totalPoints)
              .totalTeams(userTeams.size())
              .avgPointsPerTeam(avgPointsPerTeam)
              .bestTeamPoints(bestTeamPoints)
              .bestTeamName(bestTeamName)
              .victories(0) // À calculer selon les critères de victoire
              .winRate(0.0) // À calculer selon les critères de victoire
              .build());
    }

    // 4. Trier par points décroissants et assigner les rangs
    entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));

    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }

    log.info("✅ Classement pronostiqueurs généré avec {} utilisateurs", entries.size());
    return entries;
  }

  /** Obtenir le classement des joueurs Fortnite */
  @Transactional(readOnly = true)
  @Cacheable(value = "playerScores", key = "'players_' + #season")
  public List<PlayerLeaderboardEntryDTO> getPlayerLeaderboard(int season) {
    log.info("🎮 Récupération du classement des joueurs - Saison: {}", season);

    try {
      // 1. Récupérer tous les joueurs
      List<Player> players = playerRepository.findAll();
      log.info("📊 {} joueurs trouvés", players.size());

      // 2. Récupérer tous les scores groupés par joueur
      Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
      log.info("📊 {} scores trouvés pour la saison {}", playerPointsMap.size(), season);

      // 3. Récupérer toutes les équipes avec leurs joueurs pour cette saison
      List<Team> teams = teamRepository.findBySeasonWithFetch(season);
      Map<UUID, List<TeamInfo>> playerTeamsMap = new HashMap<>();
      Map<UUID, List<String>> playerPronostiqueurMap = new HashMap<>();

      // Construire les maps des équipes et pronostiqueurs par joueur
      for (Team team : teams) {
        for (TeamPlayer teamPlayer : team.getPlayers()) {
          if (teamPlayer.isActive()) {
            UUID playerId = teamPlayer.getPlayer().getId();

            // Ajouter l'équipe
            playerTeamsMap
                .computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(
                    new TeamInfo(
                        team.getId().toString(), team.getName(), team.getOwner().getUsername()));

            // Ajouter le pronostiqueur
            playerPronostiqueurMap
                .computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(team.getOwner().getUsername());
          }
        }
      }

      // 4. Créer les entrées pour chaque joueur
      List<PlayerLeaderboardEntryDTO> entries = new ArrayList<>();

      for (Player player : players) {
        UUID playerId = player.getId();
        int totalPoints = playerPointsMap.getOrDefault(playerId, 0);

        // Calculer les statistiques de base
        double avgPointsPerGame =
            totalPoints > 0 ? (double) totalPoints / Math.max(1, totalPoints / 1000) : 0.0;
        int bestScore = totalPoints; // Simplification : on utilise le total comme meilleur score

        // Récupérer les équipes et pronostiqueurs
        List<TeamInfo> playerTeams = playerTeamsMap.getOrDefault(playerId, new ArrayList<>());
        List<String> playerPronostiqueurs =
            playerPronostiqueurMap.getOrDefault(playerId, new ArrayList<>());

        PlayerLeaderboardEntryDTO entry = new PlayerLeaderboardEntryDTO();
        entry.setPlayerId(playerId.toString());
        entry.setNickname(player.getNickname());
        entry.setUsername(player.getUsername());
        entry.setRegion(player.getRegion());
        entry.setTranche(player.getTranche());
        entry.setTotalPoints(totalPoints);
        entry.setAvgPointsPerGame(avgPointsPerGame);
        entry.setBestScore(bestScore);
        entry.setTeamsCount(playerTeams.size());
        entry.setTeams(playerTeams);
        entry.setPronostiqueurs(
            playerPronostiqueurs.stream().distinct().collect(Collectors.toList()));

        entries.add(entry);
      }

      // 5. Trier par points décroissants et assigner les rangs
      entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));

      for (int i = 0; i < entries.size(); i++) {
        entries.get(i).setRank(i + 1);
      }

      log.info("✅ Classement joueurs généré avec {} joueurs", entries.size());
      return entries;

    } catch (Exception e) {
      log.error("❌ Erreur lors de la génération du classement des joueurs", e);
      throw new RuntimeException("Erreur lors de la génération du classement des joueurs", e);
    }
  }

  /** Obtenir le classement des joueurs Fortnite pour une game spécifique */
  @Transactional(readOnly = true)
  public List<PlayerLeaderboardEntryDTO> getPlayerLeaderboardByGame(UUID gameId) {
    log.info("🎮 Récupération du classement des joueurs pour la game {}", gameId);

    try {
      // 1. Récupérer les équipes de cette game avec leurs joueurs
      List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);
      log.info("📊 {} équipes trouvées pour la game {}", teams.size(), gameId);

      if (teams.isEmpty()) {
        log.warn("⚠️ Aucune équipe trouvée pour la game {}", gameId);
        return new ArrayList<>();
      }

      // 2. Extraire tous les joueurs uniques des équipes
      Set<UUID> playerIds = new HashSet<>();
      Map<UUID, List<TeamInfo>> playerTeamsMap = new HashMap<>();
      Map<UUID, List<String>> playerPronostiqueurMap = new HashMap<>();

      for (Team team : teams) {
        for (TeamPlayer teamPlayer : team.getPlayers()) {
          if (teamPlayer.isActive()) {
            UUID playerId = teamPlayer.getPlayer().getId();
            playerIds.add(playerId);

            // Ajouter l'équipe
            playerTeamsMap
                .computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(
                    new TeamInfo(
                        team.getId().toString(), team.getName(), team.getOwner().getUsername()));

            // Ajouter le pronostiqueur
            playerPronostiqueurMap
                .computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(team.getOwner().getUsername());
          }
        }
      }

      log.info("📊 {} joueurs uniques trouvés dans les équipes", playerIds.size());

      // 3. Récupérer les informations des joueurs
      List<Player> players = playerRepository.findAllById(playerIds);

      // 4. Récupérer les scores (saison 2025 par défaut)
      Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(2025);

      // 5. Créer les entrées pour chaque joueur
      List<PlayerLeaderboardEntryDTO> entries = new ArrayList<>();

      for (Player player : players) {
        UUID playerId = player.getId();
        int totalPoints = playerPointsMap.getOrDefault(playerId, 0);

        double avgPointsPerGame =
            totalPoints > 0 ? (double) totalPoints / Math.max(1, totalPoints / 1000) : 0.0;
        int bestScore = totalPoints;

        List<TeamInfo> playerTeams = playerTeamsMap.getOrDefault(playerId, new ArrayList<>());
        List<String> playerPronostiqueurs =
            playerPronostiqueurMap.getOrDefault(playerId, new ArrayList<>());

        PlayerLeaderboardEntryDTO entry = new PlayerLeaderboardEntryDTO();
        entry.setPlayerId(playerId.toString());
        entry.setNickname(player.getNickname());
        entry.setUsername(player.getUsername());
        entry.setRegion(player.getRegion());
        entry.setTranche(player.getTranche());
        entry.setTotalPoints(totalPoints);
        entry.setAvgPointsPerGame(avgPointsPerGame);
        entry.setBestScore(bestScore);
        entry.setTeamsCount(playerTeams.size());
        entry.setTeams(playerTeams);
        entry.setPronostiqueurs(playerPronostiqueurs.stream().distinct().toList());

        entries.add(entry);
      }

      // 6. Trier par points décroissants et assigner les rangs
      entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));

      for (int i = 0; i < entries.size(); i++) {
        entries.get(i).setRank(i + 1);
      }

      log.info("✅ Classement joueurs pour game {} généré avec {} joueurs", gameId, entries.size());
      return entries;

    } catch (Exception e) {
      log.error(
          "❌ Erreur lors de la génération du classement des joueurs pour la game {}", gameId, e);
      throw new RuntimeException("Erreur lors de la génération du classement des joueurs", e);
    }
  }

  // Classe interne pour les informations d'équipe
  public static class TeamInfo {
    private String id;
    private String name;
    private String ownerUsername;

    public TeamInfo(String id, String name, String ownerUsername) {
      this.id = id;
      this.name = name;
      this.ownerUsername = ownerUsername;
    }

    // Getters
    public String getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getOwnerUsername() {
      return ownerUsername;
    }
  }
}
