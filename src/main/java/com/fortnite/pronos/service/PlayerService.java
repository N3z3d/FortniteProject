package com.fortnite.pronos.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.application.usecase.PlayerQueryUseCase;
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
public class PlayerService implements PlayerQueryUseCase {
  private final PlayerRepository playerRepository;
  private final ScoreRepository scoreRepository;

  /**
   * OPTIMISÉ: Retourne une page paginée de joueurs au lieu de tous à la fois Critical pour 147
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

  public Page<PlayerDto> getAllPlayers() {
    return getAllPlayers(PageRequest.of(0, 200));
  }

  public List<Player> findAllPlayers() {
    return playerRepository.findAll();
  }

  public java.util.Optional<Player> findPlayerById(UUID id) {
    return playerRepository.findById(id);
  }

  public List<Player> findPlayersByRegion(Player.Region region) {
    return playerRepository.findByRegion(region);
  }

  public PlayerDto getPlayerById(UUID id) {
    log.debug("Récupération du joueur avec l'ID: {}", id);
    Player player =
        findPlayerById(id).orElseThrow(() -> new RuntimeException("Joueur non trouvé: " + id));

    // Calculer les points totaux pour la saison courante du joueur
    Integer totalPoints = scoreRepository.sumPointsByPlayerAndSeason(id, player.getCurrentSeason());

    return PlayerDto.from(player, totalPoints != null ? totalPoints : 0, true);
  }

  public List<PlayerDto> getPlayersByRegion(Player.Region region) {
    log.debug("Récupération des joueurs de la région: {}", region);
    return findPlayersByRegion(region).stream()
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

    String normalizedQuery = normalizeQuery(query);
    return playerRepository
        .searchPlayers(normalizedQuery, region, tranche, available, pageable)
        .map(PlayerDto::fromEntity);
  }

  private String normalizeQuery(String query) {
    if (query == null) {
      return null;
    }
    String trimmed = query.trim();
    return trimmed.isEmpty() ? null : trimmed;
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
    List<Player> allPlayers = findAllPlayers();

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
