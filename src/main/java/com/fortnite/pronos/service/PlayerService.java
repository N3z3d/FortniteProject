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
import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.domain.port.out.ScoreRepositoryPort;
import com.fortnite.pronos.dto.player.PlayerDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService implements PlayerQueryUseCase {
  private final com.fortnite.pronos.repository.PlayerRepository playerRepository;
  private final ScoreRepositoryPort scoreRepository;

  /**
   * OPTIMISÃ‰: Retourne une page paginÃ©e de joueurs au lieu de tous Ã  la fois Critical pour 147
   * joueurs - Ã©vite de surcharger la mÃ©moire et le rÃ©seau
   */
  @Cacheable(
      value = "playerPages",
      key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public Page<PlayerDto> getAllPlayers(Pageable pageable) {
    log.debug(
        "RÃ©cupÃ©ration paginÃ©e des joueurs - page: {}, size: {}",
        pageable.getPageNumber(),
        pageable.getPageSize());
    return playerRepository.findAll(pageable).map(PlayerDto::fromEntity);
  }

  public Page<PlayerDto> getAllPlayers() {
    return getAllPlayers(PageRequest.of(0, 200));
  }

  public List<com.fortnite.pronos.model.Player> findAllPlayers() {
    return playerRepository.findAll();
  }

  public java.util.Optional<com.fortnite.pronos.model.Player> findPlayerById(UUID id) {
    return ((PlayerRepositoryPort) playerRepository).findById(id);
  }

  public List<com.fortnite.pronos.model.Player> findPlayersByRegion(
      com.fortnite.pronos.model.Player.Region region) {
    return playerRepository.findByRegion(region);
  }

  public PlayerDto getPlayerById(UUID id) {
    log.debug("RÃ©cupÃ©ration du joueur avec l'ID: {}", id);
    com.fortnite.pronos.model.Player player =
        findPlayerById(id).orElseThrow(() -> new RuntimeException("Joueur non trouvÃ©: " + id));

    // Calculer les points totaux pour la saison courante du joueur
    Integer totalPoints = scoreRepository.sumPointsByPlayerAndSeason(id, player.getCurrentSeason());

    return PlayerDto.from(player, totalPoints != null ? totalPoints : 0, true);
  }

  public List<PlayerDto> getPlayersByRegion(com.fortnite.pronos.model.Player.Region region) {
    log.debug("RÃ©cupÃ©ration des joueurs de la rÃ©gion: {}", region);
    return findPlayersByRegion(region).stream().map(PlayerDto::fromEntity).toList();
  }

  public List<PlayerDto> getPlayersByTranche(String tranche) {
    log.debug("RÃ©cupÃ©ration des joueurs de la tranche: {}", tranche);
    return playerRepository.findByTranche(tranche).stream().map(PlayerDto::fromEntity).toList();
  }

  public Page<PlayerDto> searchPlayers(
      String query,
      com.fortnite.pronos.model.Player.Region region,
      String tranche,
      boolean available,
      Pageable pageable) {
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
    log.debug("RÃ©cupÃ©ration des joueurs actifs");
    return playerRepository.findActivePlayers().stream().map(PlayerDto::fromEntity).toList();
  }

  public Map<String, Object> getPlayersStats() {
    log.debug("RÃ©cupÃ©ration des statistiques des joueurs");

    Map<String, Object> stats = new HashMap<>();
    List<com.fortnite.pronos.model.Player> allPlayers = findAllPlayers();

    stats.put("totalPlayers", allPlayers.size());
    stats.put("playersByRegion", getPlayerCountByRegion(allPlayers));
    stats.put("playersByTranche", getPlayerCountByTranche(allPlayers));

    return stats;
  }

  private Map<String, Long> getPlayerCountByRegion(List<com.fortnite.pronos.model.Player> players) {
    return players.stream()
        .collect(Collectors.groupingBy(p -> p.getRegion().name(), Collectors.counting()));
  }

  private Map<String, Long> getPlayerCountByTranche(
      List<com.fortnite.pronos.model.Player> players) {
    return players.stream()
        .collect(
            Collectors.groupingBy(
                com.fortnite.pronos.model.Player::getTranche, Collectors.counting()));
  }
}
