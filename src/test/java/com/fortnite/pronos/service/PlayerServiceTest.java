package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fortnite.pronos.dto.player.PlayerDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;

/** Tests TDD pour PlayerService */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests TDD - PlayerService")
class PlayerServiceTest {

  @Mock private PlayerRepository playerRepository;

  @Mock private ScoreRepository scoreRepository;

  @InjectMocks private PlayerService playerService;

  private Player testPlayer1;
  private Player testPlayer2;
  private Player testPlayer3;
  private PlayerDto testPlayerDto1;
  private PlayerDto testPlayerDto2;
  private PlayerDto testPlayerDto3;

  @BeforeEach
  void setUp() {
    // Créer des joueurs de test
    testPlayer1 = new Player();
    testPlayer1.setId(UUID.randomUUID());
    testPlayer1.setUsername("TestPlayer1");
    testPlayer1.setNickname("TP1");
    testPlayer1.setRegion(Player.Region.EU);
    testPlayer1.setTranche("S");
    testPlayer1.setCurrentSeason(2025);

    testPlayer2 = new Player();
    testPlayer2.setId(UUID.randomUUID());
    testPlayer2.setUsername("TestPlayer2");
    testPlayer2.setNickname("TP2");
    testPlayer2.setRegion(Player.Region.NAW);
    testPlayer2.setTranche("A");
    testPlayer2.setCurrentSeason(2025);

    testPlayer3 = new Player();
    testPlayer3.setId(UUID.randomUUID());
    testPlayer3.setUsername("TestPlayer3");
    testPlayer3.setNickname("TP3");
    testPlayer3.setRegion(Player.Region.BR);
    testPlayer3.setTranche("B");
    testPlayer3.setCurrentSeason(2025);

    // Créer des DTOs de test
    testPlayerDto1 = new PlayerDto();
    testPlayerDto1.setId(testPlayer1.getId());
    testPlayerDto1.setUsername(testPlayer1.getUsername());
    testPlayerDto1.setNickname(testPlayer1.getNickname());
    testPlayerDto1.setRegion(testPlayer1.getRegion());
    testPlayerDto1.setTranche(testPlayer1.getTranche());

    testPlayerDto2 = new PlayerDto();
    testPlayerDto2.setId(testPlayer2.getId());
    testPlayerDto2.setUsername(testPlayer2.getUsername());
    testPlayerDto2.setNickname(testPlayer2.getNickname());
    testPlayerDto2.setRegion(testPlayer2.getRegion());
    testPlayerDto2.setTranche(testPlayer2.getTranche());

    testPlayerDto3 = new PlayerDto();
    testPlayerDto3.setId(testPlayer3.getId());
    testPlayerDto3.setUsername(testPlayer3.getUsername());
    testPlayerDto3.setNickname(testPlayer3.getNickname());
    testPlayerDto3.setRegion(testPlayer3.getRegion());
    testPlayerDto3.setTranche(testPlayer3.getTranche());
  }

  @Test
  @DisplayName("Devrait récupérer tous les joueurs avec pagination")
  void shouldGetAllPlayers() {
    // Given
    List<Player> players = Arrays.asList(testPlayer1, testPlayer2, testPlayer3);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Player> playerPage = new PageImpl<>(players, pageable, players.size());
    when(playerRepository.findAll(pageable)).thenReturn(playerPage);

    // When
    Page<PlayerDto> result = playerService.getAllPlayers(pageable);

    // Then
    assertThat(result.getContent()).hasSize(3);
    verify(playerRepository).findAll(pageable);
  }

  @Test
  @DisplayName("Devrait retourner une page vide quand aucun joueur n'existe")
  void shouldReturnEmptyListWhenNoPlayersExist() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Player> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);
    when(playerRepository.findAll(pageable)).thenReturn(emptyPage);

    // When
    Page<PlayerDto> result = playerService.getAllPlayers(pageable);

    // Then
    assertThat(result.getContent()).isEmpty();
    verify(playerRepository).findAll(pageable);
  }

  @Test
  @DisplayName("Devrait récupérer un joueur par ID avec succès")
  void shouldGetPlayerByIdSuccessfully() {
    // Given
    Integer totalPoints = 150;
    when(playerRepository.findById(testPlayer1.getId())).thenReturn(Optional.of(testPlayer1));
    when(scoreRepository.sumPointsByPlayerAndSeason(
            testPlayer1.getId(), testPlayer1.getCurrentSeason()))
        .thenReturn(totalPoints);

    // When
    PlayerDto result = playerService.getPlayerById(testPlayer1.getId());

    // Then
    assertThat(result).isNotNull();
    verify(playerRepository).findById(testPlayer1.getId());
    verify(scoreRepository)
        .sumPointsByPlayerAndSeason(testPlayer1.getId(), testPlayer1.getCurrentSeason());
  }

  @Test
  @DisplayName("Devrait lever une exception quand le joueur n'existe pas")
  void shouldThrowExceptionWhenPlayerNotFound() {
    // Given
    when(playerRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> playerService.getPlayerById(UUID.randomUUID()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Joueur non trouvé");
  }

  @Test
  @DisplayName("Devrait retourner 0 points quand le joueur n'a pas de scores")
  void shouldReturnZeroPointsWhenPlayerHasNoScores() {
    // Given
    when(playerRepository.findById(testPlayer1.getId())).thenReturn(Optional.of(testPlayer1));
    when(scoreRepository.sumPointsByPlayerAndSeason(
            testPlayer1.getId(), testPlayer1.getCurrentSeason()))
        .thenReturn(null);

    // When
    PlayerDto result = playerService.getPlayerById(testPlayer1.getId());

    // Then
    assertThat(result).isNotNull();
    verify(playerRepository).findById(testPlayer1.getId());
    verify(scoreRepository)
        .sumPointsByPlayerAndSeason(testPlayer1.getId(), testPlayer1.getCurrentSeason());
  }

  @Test
  @DisplayName("Devrait récupérer les joueurs par région")
  void shouldGetPlayersByRegion() {
    // Given
    List<Player> euPlayers = Arrays.asList(testPlayer1);
    when(playerRepository.findByRegion(Player.Region.EU)).thenReturn(euPlayers);

    // When
    List<PlayerDto> result = playerService.getPlayersByRegion(Player.Region.EU);

    // Then
    assertThat(result).hasSize(1);
    verify(playerRepository).findByRegion(Player.Region.EU);
  }

  @Test
  @DisplayName("Devrait retourner une liste vide pour une région sans joueurs")
  void shouldReturnEmptyListForRegionWithoutPlayers() {
    // Given
    when(playerRepository.findByRegion(Player.Region.ASIA)).thenReturn(Arrays.asList());

    // When
    List<PlayerDto> result = playerService.getPlayersByRegion(Player.Region.ASIA);

    // Then
    assertThat(result).isEmpty();
    verify(playerRepository).findByRegion(Player.Region.ASIA);
  }

  @Test
  @DisplayName("Devrait récupérer les joueurs par tranche")
  void shouldGetPlayersByTranche() {
    // Given
    List<Player> sTranchePlayers = Arrays.asList(testPlayer1);
    when(playerRepository.findByTranche("S")).thenReturn(sTranchePlayers);

    // When
    List<PlayerDto> result = playerService.getPlayersByTranche("S");

    // Then
    assertThat(result).hasSize(1);
    verify(playerRepository).findByTranche("S");
  }

  @Test
  @DisplayName("Devrait retourner une liste vide pour une tranche sans joueurs")
  void shouldReturnEmptyListForTrancheWithoutPlayers() {
    // Given
    when(playerRepository.findByTranche("Z")).thenReturn(Arrays.asList());

    // When
    List<PlayerDto> result = playerService.getPlayersByTranche("Z");

    // Then
    assertThat(result).isEmpty();
    verify(playerRepository).findByTranche("Z");
  }

  @Test
  @DisplayName("Devrait rechercher des joueurs avec une requête")
  void shouldSearchPlayersWithQuery() {
    // Given
    String query = "TP";
    Pageable pageable = PageRequest.of(0, 10);
    Page<Player> playerPage = new PageImpl<>(Arrays.asList(testPlayer1, testPlayer2), pageable, 2);
    when(playerRepository.searchByNickname(query, pageable)).thenReturn(playerPage);

    // When
    Page<PlayerDto> result = playerService.searchPlayers(query, null, null, false, pageable);

    // Then
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(2);
    verify(playerRepository).searchByNickname(query, pageable);
  }

  @Test
  @DisplayName("Devrait rechercher tous les joueurs quand la requête est vide")
  void shouldSearchAllPlayersWhenQueryIsEmpty() {
    // Given
    String query = "";
    Pageable pageable = PageRequest.of(0, 10);
    List<Player> allPlayers = Arrays.asList(testPlayer1, testPlayer2, testPlayer3);
    when(playerRepository.findAll()).thenReturn(allPlayers);

    // When
    Page<PlayerDto> result = playerService.searchPlayers(query, null, null, false, pageable);

    // Then
    assertThat(result.getContent()).hasSize(3);
    verify(playerRepository).findAll();
  }

  @Test
  @DisplayName("Devrait rechercher tous les joueurs quand la requête est null")
  void shouldSearchAllPlayersWhenQueryIsNull() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    List<Player> allPlayers = Arrays.asList(testPlayer1, testPlayer2, testPlayer3);
    when(playerRepository.findAll()).thenReturn(allPlayers);

    // When
    Page<PlayerDto> result = playerService.searchPlayers(null, null, null, false, pageable);

    // Then
    assertThat(result.getContent()).hasSize(3);
    verify(playerRepository).findAll();
  }

  @Test
  @DisplayName("Devrait filtrer les joueurs par région lors de la recherche")
  void shouldFilterPlayersByRegionInSearch() {
    // Given
    String query = "TP";
    Pageable pageable = PageRequest.of(0, 10);
    Page<Player> playerPage = new PageImpl<>(Arrays.asList(testPlayer1, testPlayer2), pageable, 2);
    when(playerRepository.searchByNickname(query, pageable)).thenReturn(playerPage);

    // When
    Page<PlayerDto> result =
        playerService.searchPlayers(query, Player.Region.EU, null, false, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    verify(playerRepository).searchByNickname(query, pageable);
  }

  @Test
  @DisplayName("Devrait filtrer les joueurs par tranche lors de la recherche")
  void shouldFilterPlayersByTrancheInSearch() {
    // Given
    String query = "TP";
    Pageable pageable = PageRequest.of(0, 10);
    Page<Player> playerPage = new PageImpl<>(Arrays.asList(testPlayer1, testPlayer2), pageable, 2);
    when(playerRepository.searchByNickname(query, pageable)).thenReturn(playerPage);

    // When
    Page<PlayerDto> result = playerService.searchPlayers(query, null, "S", false, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    verify(playerRepository).searchByNickname(query, pageable);
  }

  @Test
  @DisplayName("Devrait filtrer les joueurs par région et tranche lors de la recherche")
  void shouldFilterPlayersByRegionAndTrancheInSearch() {
    // Given
    String query = "TP";
    Pageable pageable = PageRequest.of(0, 10);
    Page<Player> playerPage = new PageImpl<>(Arrays.asList(testPlayer1, testPlayer2), pageable, 2);
    when(playerRepository.searchByNickname(query, pageable)).thenReturn(playerPage);

    // When
    Page<PlayerDto> result =
        playerService.searchPlayers(query, Player.Region.EU, "S", false, pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    verify(playerRepository).searchByNickname(query, pageable);
  }

  @Test
  @DisplayName("Devrait récupérer les joueurs actifs")
  void shouldGetActivePlayers() {
    // Given
    List<Player> activePlayers = Arrays.asList(testPlayer1, testPlayer2);
    when(playerRepository.findActivePlayers()).thenReturn(activePlayers);

    // When
    List<PlayerDto> result = playerService.getActivePlayers();

    // Then
    assertThat(result).hasSize(2);
    verify(playerRepository).findActivePlayers();
  }

  @Test
  @DisplayName("Devrait retourner une liste vide pour les joueurs actifs")
  void shouldReturnEmptyListForActivePlayers() {
    // Given
    when(playerRepository.findActivePlayers()).thenReturn(Arrays.asList());

    // When
    List<PlayerDto> result = playerService.getActivePlayers();

    // Then
    assertThat(result).isEmpty();
    verify(playerRepository).findActivePlayers();
  }

  @Test
  @DisplayName("Devrait récupérer les statistiques des joueurs")
  void shouldGetPlayersStats() {
    // Given
    List<Player> allPlayers = Arrays.asList(testPlayer1, testPlayer2, testPlayer3);
    when(playerRepository.findAll()).thenReturn(allPlayers);

    // When
    Map<String, Object> result = playerService.getPlayersStats();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).containsKey("totalPlayers");
    assertThat(result).containsKey("playersByRegion");
    assertThat(result).containsKey("playersByTranche");
    assertThat(result.get("totalPlayers")).isEqualTo(3);

    @SuppressWarnings("unchecked")
    Map<String, Long> playersByRegion = (Map<String, Long>) result.get("playersByRegion");
    assertThat(playersByRegion).containsEntry("EU", 1L);
    assertThat(playersByRegion).containsEntry("NAW", 1L);
    assertThat(playersByRegion).containsEntry("BR", 1L);

    @SuppressWarnings("unchecked")
    Map<String, Long> playersByTranche = (Map<String, Long>) result.get("playersByTranche");
    assertThat(playersByTranche).containsEntry("S", 1L);
    assertThat(playersByTranche).containsEntry("A", 1L);
    assertThat(playersByTranche).containsEntry("B", 1L);

    verify(playerRepository).findAll();
  }

  @Test
  @DisplayName("Devrait retourner des statistiques vides quand aucun joueur n'existe")
  void shouldReturnEmptyStatsWhenNoPlayersExist() {
    // Given
    when(playerRepository.findAll()).thenReturn(Arrays.asList());

    // When
    Map<String, Object> result = playerService.getPlayersStats();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.get("totalPlayers")).isEqualTo(0);

    @SuppressWarnings("unchecked")
    Map<String, Long> playersByRegion = (Map<String, Long>) result.get("playersByRegion");
    assertThat(playersByRegion).isEmpty();

    @SuppressWarnings("unchecked")
    Map<String, Long> playersByTranche = (Map<String, Long>) result.get("playersByTranche");
    assertThat(playersByTranche).isEmpty();

    verify(playerRepository).findAll();
  }

  @Test
  @DisplayName("Devrait gérer les joueurs avec des régions multiples")
  void shouldHandlePlayersWithMultipleRegions() {
    // Given
    Player player4 = new Player();
    player4.setId(UUID.randomUUID());
    player4.setRegion(Player.Region.EU);
    player4.setTranche("S");

    List<Player> players = Arrays.asList(testPlayer1, player4); // 2 joueurs EU
    when(playerRepository.findAll()).thenReturn(players);

    // When
    Map<String, Object> result = playerService.getPlayersStats();

    // Then
    @SuppressWarnings("unchecked")
    Map<String, Long> playersByRegion = (Map<String, Long>) result.get("playersByRegion");
    assertThat(playersByRegion).containsEntry("EU", 2L);
    assertThat(result.get("totalPlayers")).isEqualTo(2);
  }

  @Test
  @DisplayName("Devrait gérer les joueurs avec des tranches multiples")
  void shouldHandlePlayersWithMultipleTranches() {
    // Given
    Player player4 = new Player();
    player4.setId(UUID.randomUUID());
    player4.setRegion(Player.Region.OCE);
    player4.setTranche("S"); // Même tranche que testPlayer1

    List<Player> players = Arrays.asList(testPlayer1, player4); // 2 joueurs S
    when(playerRepository.findAll()).thenReturn(players);

    // When
    Map<String, Object> result = playerService.getPlayersStats();

    // Then
    @SuppressWarnings("unchecked")
    Map<String, Long> playersByTranche = (Map<String, Long>) result.get("playersByTranche");
    assertThat(playersByTranche).containsEntry("S", 2L);
    assertThat(result.get("totalPlayers")).isEqualTo(2);
  }
}
