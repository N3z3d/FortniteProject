package com.fortnite.pronos.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.player.PlayerDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {
  private final PlayerRepository playerRepository;
  private final ScoreRepository scoreRepository;

  /**
   * OPTIMISÉ: Retourne une page paginée de joueurs au lieu de tous à la fois Critical pour 149
   * joueurs - évite de surcharger la mémoire et le réseau
   */
  @Cacheable(
      value = "playerPages",
      key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public Page<PlayerDto> getAllPlayers(Pageable pageable) {
    log.debug(
        "Récupération paginée des joueurs - page: {}, size: {}",
        pageable.getPageNumber(),
        pageable.getPageSize());
    return playerRepository.findAll(pageable).map(PlayerDto::fromEntity);
  }

  /**
   * DEPRECATED: Utilisé uniquement pour la compatibilité ascendante Recommandé d'utiliser
   * getAllPlayers(Pageable) pour de meilleures performances
   */
  @Deprecated
  public List<PlayerDto> getAllPlayers() {
    log.warn("⚠️  getAllPlayers() sans pagination appelé - Performance impact avec 149 joueurs!");
    return playerRepository.findAll().stream()
        .map(PlayerDto::fromEntity)
        .collect(Collectors.toList());
  }

  public PlayerDto getPlayerById(UUID id) {
    log.debug("Récupération du joueur avec l'ID: {}", id);
    Player player =
        playerRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Joueur non trouvé: " + id));

    // Calculer les points totaux pour la saison courante du joueur
    Integer totalPoints = scoreRepository.sumPointsByPlayerAndSeason(id, player.getCurrentSeason());

    return PlayerDto.from(player, totalPoints != null ? totalPoints : 0, true);
  }

  public List<PlayerDto> getPlayersByRegion(Player.Region region) {
    log.debug("Récupération des joueurs de la région: {}", region);
    return playerRepository.findByRegion(region).stream()
        .map(PlayerDto::fromEntity)
        .collect(Collectors.toList());
  }

  public List<PlayerDto> getPlayersByTranche(String tranche) {
    log.debug("Récupération des joueurs de la tranche: {}", tranche);
    return playerRepository.findByTranche(tranche).stream()
        .map(PlayerDto::fromEntity)
        .collect(Collectors.toList());
  }

  public Page<PlayerDto> searchPlayers(
      String query, Player.Region region, String tranche, boolean available, Pageable pageable) {
    log.debug(
        "Recherche de joueurs avec query: {}, region: {}, tranche: {}", query, region, tranche);

    List<Player> players;

    if (query != null && !query.trim().isEmpty()) {
      Page<Player> playerPage = playerRepository.searchByNickname(query, pageable);
      players = playerPage.getContent();
    } else {
      players = playerRepository.findAll();
    }

    // Filtrer par région si spécifiée
    if (region != null) {
      players = players.stream().filter(p -> p.getRegion() == region).collect(Collectors.toList());
    }

    // Filtrer par tranche si spécifiée
    if (tranche != null) {
      players =
          players.stream().filter(p -> p.getTranche().equals(tranche)).collect(Collectors.toList());
    }

    List<PlayerDto> playerDtos =
        players.stream().map(PlayerDto::fromEntity).collect(Collectors.toList());

    return new PageImpl<>(playerDtos, pageable, playerDtos.size());
  }

  public List<PlayerDto> getActivePlayers() {
    log.debug("Récupération des joueurs actifs");
    return playerRepository.findActivePlayers().stream()
        .map(PlayerDto::fromEntity)
        .collect(Collectors.toList());
  }

  public Map<String, Object> getPlayersStats() {
    log.debug("Récupération des statistiques des joueurs");

    Map<String, Object> stats = new HashMap<>();
    List<Player> allPlayers = playerRepository.findAll();

    stats.put("totalPlayers", allPlayers.size());
    stats.put("playersByRegion", getPlayerCountByRegion(allPlayers));
    stats.put("playersByTranche", getPlayerCountByTranche(allPlayers));

    return stats;
  }

  private Map<String, Long> getPlayerCountByRegion(List<Player> players) {
    return players.stream()
        .collect(Collectors.groupingBy(p -> p.getRegion().name(), Collectors.counting()));
  }

  private Map<String, Long> getPlayerCountByTranche(List<Player> players) {
    return players.stream()
        .collect(Collectors.groupingBy(Player::getTranche, Collectors.counting()));
  }
}
