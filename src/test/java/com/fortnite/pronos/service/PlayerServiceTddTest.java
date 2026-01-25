package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.dto.player.PlayerDto;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;

/**
 * TDD Tests for PlayerService - Business Critical Component
 *
 * <p>This test suite validates player management, search functionality, and performance
 * optimization using RED-GREEN-REFACTOR TDD methodology. PlayerService handles player queries,
 * pagination, filtering, and statistics essential for the fantasy league player management.
 *
 * <p>Business Logic Areas: - Player pagination and performance optimization - Player search and
 * filtering by region/tranche - Player statistics and data aggregation - Cache optimization for
 * large datasets - Active player management and availability tracking
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService - Performance Critical TDD Tests")
class PlayerServiceTddTest {

  @Mock private PlayerRepository playerRepository;
  @Mock private ScoreRepository scoreRepository;

  @InjectMocks private PlayerService playerService;

  private Player testPlayer1;
  private Player testPlayer2;
  private Player testPlayer3;
  private List<Player> testPlayers;
  private Pageable testPageable;
  private Page<Player> testPlayerPage;

  @BeforeEach
  void setUp() {
    // Test players with different characteristics
    testPlayer1 =
        Player.builder()
            .id(UUID.randomUUID())
            .username("ninja_pro")
            .nickname("Pro Ninja")
            .region(Player.Region.EU)
            .tranche("1-3")
            .currentSeason(2025)
            .build();

    testPlayer2 =
        Player.builder()
            .id(UUID.randomUUID())
            .username("average_joe")
            .nickname("Average Joe")
            .region(Player.Region.NAW)
            .tranche("4-7")
            .currentSeason(2025)
            .build();

    testPlayer3 =
        Player.builder()
            .id(UUID.randomUUID())
            .username("newbie_player")
            .nickname("The Newbie")
            .region(Player.Region.BR)
            .tranche("8-10")
            .currentSeason(2025)
            .build();

    testPlayers = Arrays.asList(testPlayer1, testPlayer2, testPlayer3);

    // Pageable setup
    testPageable = PageRequest.of(0, 10, Sort.by("nickname").ascending());
    testPlayerPage = new PageImpl<>(testPlayers, testPageable, testPlayers.size());
  }

  @Nested
  @DisplayName("Player Pagination and Performance")
  class PlayerPaginationTests {

    @Test
    @DisplayName("Should return paginated players efficiently")
    void shouldReturnPaginatedPlayersEfficiently() {
      // RED: Define expected behavior for paginated player retrieval
      when(playerRepository.findAll(testPageable)).thenReturn(testPlayerPage);

      Page<PlayerDto> result = playerService.getAllPlayers(testPageable);

      // Verify pagination working correctly
      assertThat(result.getContent()).hasSize(3);
      assertThat(result.getTotalElements()).isEqualTo(3);
      assertThat(result.getNumber()).isEqualTo(0);
      assertThat(result.getSize()).isEqualTo(10);

      // Verify player data mapping
      PlayerDto firstPlayer = result.getContent().get(0);
      assertThat(firstPlayer.getNickname()).isEqualTo("Pro Ninja");
      assertThat(firstPlayer.getRegion()).isEqualTo(Player.Region.EU);

      verify(playerRepository).findAll(testPageable);
    }

    @Test
    @DisplayName("Should handle empty player pages gracefully")
    void shouldHandleEmptyPlayerPagesGracefully() {
      // RED: Test empty page scenario
      Page<Player> emptyPage = new PageImpl<>(Collections.emptyList(), testPageable, 0);
      when(playerRepository.findAll(testPageable)).thenReturn(emptyPage);

      Page<PlayerDto> result = playerService.getAllPlayers(testPageable);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isEqualTo(0);
      assertThat(result.getNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle large page requests efficiently")
    void shouldHandleLargePageRequestsEfficiently() {
      // RED: Test performance with large page sizes
      Pageable largePageable = PageRequest.of(0, 100);
      List<Player> largePlayers = createLargePlayerList(100);
      Page<Player> largePage = new PageImpl<>(largePlayers, largePageable, largePlayers.size());

      when(playerRepository.findAll(largePageable)).thenReturn(largePage);

      Page<PlayerDto> result = playerService.getAllPlayers(largePageable);

      assertThat(result.getContent()).hasSize(100);
      assertThat(result.getTotalElements()).isEqualTo(100);

      // Should still make only one repository call
      verify(playerRepository, times(1)).findAll(largePageable);
    }
  }

  // Helper method for creating large player lists - available to all nested classes
  private List<Player> createLargePlayerList(int size) {
    return IntStream.range(0, size)
        .mapToObj(
            i ->
                Player.builder()
                    .id(UUID.randomUUID())
                    .username("player_" + i)
                    .nickname("Player " + i)
                    .region(Player.Region.EU)
                    .tranche("1-10")
                    .currentSeason(2025)
                    .build())
        .toList();
  }

  // Helper method for creating mixed player lists with different regions/tranches
  private List<Player> createMixedPlayerList(int size) {
    Player.Region[] regions = Player.Region.values();
    String[] tranches = {"1-3", "4-7", "8-10"};

    return IntStream.range(0, size)
        .mapToObj(
            i ->
                Player.builder()
                    .id(UUID.randomUUID())
                    .username("player_" + i)
                    .nickname("Player " + i)
                    .region(regions[i % regions.length])
                    .tranche(tranches[i % tranches.length])
                    .currentSeason(2025)
                    .build())
        .toList();
  }

  @Nested
  @DisplayName("Player Retrieval and Details")
  class PlayerRetrievalTests {

    @Test
    @DisplayName("Should retrieve player by ID with score calculation")
    void shouldRetrievePlayerByIdWithScoreCalculation() {
      // RED: Test individual player retrieval with scores
      UUID playerId = testPlayer1.getId();
      Integer totalPoints = 1500;

      when(((PlayerRepositoryPort) playerRepository).findById(playerId))
          .thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.sumPointsByPlayerAndSeason(playerId, 2025)).thenReturn(totalPoints);

      PlayerDto result = playerService.getPlayerById(playerId);

      assertThat(result.getId()).isEqualTo(playerId);
      assertThat(result.getNickname()).isEqualTo("Pro Ninja");
      assertThat(result.getUsername()).isEqualTo("ninja_pro");
      assertThat(result.getRegion()).isEqualTo(Player.Region.EU);
      assertThat(result.getTotalPoints()).isEqualTo(1500);

      verify(((PlayerRepositoryPort) playerRepository)).findById(playerId);
      verify(scoreRepository).sumPointsByPlayerAndSeason(playerId, 2025);
    }

    @Test
    @DisplayName("Should handle player with no scores gracefully")
    void shouldHandlePlayerWithNoScoresGracefully() {
      // RED: Test player with null score sum
      UUID playerId = testPlayer1.getId();

      when(((PlayerRepositoryPort) playerRepository).findById(playerId))
          .thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.sumPointsByPlayerAndSeason(playerId, 2025)).thenReturn(null);

      PlayerDto result = playerService.getPlayerById(playerId);

      assertThat(result.getTotalPoints()).isEqualTo(0);
      assertThat(result.getNickname()).isEqualTo("Pro Ninja");
    }

    @Test
    @DisplayName("Should throw exception for non-existent player")
    void shouldThrowExceptionForNonExistentPlayer() {
      // RED: Test player not found scenario
      UUID nonExistentId = UUID.randomUUID();
      when(((PlayerRepositoryPort) playerRepository).findById(nonExistentId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> playerService.getPlayerById(nonExistentId))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Joueur non trouvé: " + nonExistentId);

      verify(((PlayerRepositoryPort) playerRepository)).findById(nonExistentId);
      verifyNoInteractions(scoreRepository);
    }
  }

  @Nested
  @DisplayName("Player Filtering and Search")
  class PlayerFilteringTests {

    @Test
    @DisplayName("Should filter players by region correctly")
    void shouldFilterPlayersByRegionCorrectly() {
      // RED: Test region-based filtering
      List<Player> euPlayers = Arrays.asList(testPlayer1);
      when(playerRepository.findByRegion(Player.Region.EU)).thenReturn(euPlayers);

      List<PlayerDto> result = playerService.getPlayersByRegion(Player.Region.EU);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getRegion()).isEqualTo(Player.Region.EU);
      assertThat(result.get(0).getNickname()).isEqualTo("Pro Ninja");

      verify(playerRepository).findByRegion(Player.Region.EU);
    }

    @Test
    @DisplayName("Should filter players by tranche correctly")
    void shouldFilterPlayersByTrancheCorrectly() {
      // RED: Test tranche-based filtering
      List<Player> tranche13Players = Arrays.asList(testPlayer1);
      when(playerRepository.findByTranche("1-3")).thenReturn(tranche13Players);

      List<PlayerDto> result = playerService.getPlayersByTranche("1-3");

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTranche()).isEqualTo("1-3");
      assertThat(result.get(0).getNickname()).isEqualTo("Pro Ninja");

      verify(playerRepository).findByTranche("1-3");
    }

    @Test
    @DisplayName("Should search players by nickname with pagination")
    void shouldSearchPlayersByNicknameWithPagination() {
      // RED: Test nickname search with pagination
      String searchQuery = "Pro";
      Page<Player> searchResults = new PageImpl<>(Arrays.asList(testPlayer1), testPageable, 1);

      when(playerRepository.searchPlayers(searchQuery, null, null, false, testPageable))
          .thenReturn(searchResults);

      Page<PlayerDto> result =
          playerService.searchPlayers(searchQuery, null, null, false, testPageable);

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getNickname()).contains("Pro");

      verify(playerRepository).searchPlayers(searchQuery, null, null, false, testPageable);
    }

    @Test
    @DisplayName("Should combine multiple search filters correctly")
    void shouldCombineMultipleSearchFiltersCorrectly() {
      // RED: Test complex filtering with multiple criteria
      String searchQuery = "Player";
      Player.Region filterRegion = Player.Region.EU;
      String filterTranche = "1-3";

      Page<Player> filteredPage = new PageImpl<>(Arrays.asList(testPlayer1), testPageable, 1);
      when(playerRepository.searchPlayers(
              searchQuery, filterRegion, filterTranche, false, testPageable))
          .thenReturn(filteredPage);

      Page<PlayerDto> result =
          playerService.searchPlayers(
              searchQuery, filterRegion, filterTranche, false, testPageable);

      // Should only return players matching ALL criteria
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getRegion()).isEqualTo(Player.Region.EU);
      assertThat(result.getContent().get(0).getTranche()).isEqualTo("1-3");
    }

    @Test
    @DisplayName("Should handle empty search results gracefully")
    void shouldHandleEmptySearchResultsGracefully() {
      // RED: Test empty search results
      String searchQuery = "NonExistentPlayer";
      Page<Player> emptyResults = new PageImpl<>(Collections.emptyList(), testPageable, 0);

      when(playerRepository.searchPlayers(searchQuery, null, null, false, testPageable))
          .thenReturn(emptyResults);

      Page<PlayerDto> result =
          playerService.searchPlayers(searchQuery, null, null, false, testPageable);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should search without query and apply filters")
    void shouldSearchWithoutQueryAndApplyFilters() {
      // RED: Test filtering without search query
      Page<Player> filteredPage = new PageImpl<>(Arrays.asList(testPlayer1), testPageable, 1);
      when(playerRepository.searchPlayers(null, Player.Region.EU, null, false, testPageable))
          .thenReturn(filteredPage);

      Page<PlayerDto> result =
          playerService.searchPlayers(null, Player.Region.EU, null, false, testPageable);

      // Should filter testPlayers for EU region only
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).getRegion()).isEqualTo(Player.Region.EU);
    }
  }

  @Nested
  @DisplayName("Player Statistics and Analytics")
  class PlayerStatisticsTests {

    @Test
    @DisplayName("Should calculate comprehensive player statistics")
    void shouldCalculateComprehensivePlayerStatistics() {
      // RED: Test statistics calculation
      when(playerRepository.findAll()).thenReturn(testPlayers);

      Map<String, Object> result = playerService.getPlayersStats();

      // Verify basic stats
      assertThat(result.get("totalPlayers")).isEqualTo(3);

      // Verify region distribution
      @SuppressWarnings("unchecked")
      Map<String, Long> regionStats = (Map<String, Long>) result.get("playersByRegion");
      assertThat(regionStats.get("EU")).isEqualTo(1L);
      assertThat(regionStats.get("NAW")).isEqualTo(1L);
      assertThat(regionStats.get("BR")).isEqualTo(1L);

      // Verify tranche distribution
      @SuppressWarnings("unchecked")
      Map<String, Long> trancheStats = (Map<String, Long>) result.get("playersByTranche");
      assertThat(trancheStats.get("1-3")).isEqualTo(1L);
      assertThat(trancheStats.get("4-7")).isEqualTo(1L);
      assertThat(trancheStats.get("8-10")).isEqualTo(1L);

      verify(playerRepository).findAll();
    }

    @Test
    @DisplayName("Should handle empty player list in statistics")
    void shouldHandleEmptyPlayerListInStatistics() {
      // RED: Test statistics with no players
      when(playerRepository.findAll()).thenReturn(Collections.emptyList());

      Map<String, Object> result = playerService.getPlayersStats();

      assertThat(result.get("totalPlayers")).isEqualTo(0);

      @SuppressWarnings("unchecked")
      Map<String, Long> regionStats = (Map<String, Long>) result.get("playersByRegion");
      assertThat(regionStats).isEmpty();

      @SuppressWarnings("unchecked")
      Map<String, Long> trancheStats = (Map<String, Long>) result.get("playersByTranche");
      assertThat(trancheStats).isEmpty();
    }

    @Test
    @DisplayName("Should calculate statistics for large player datasets")
    void shouldCalculateStatisticsForLargePlayerDatasets() {
      // RED: Test statistics performance with large dataset
      List<Player> largePlayers = createMixedPlayerList(100);
      when(playerRepository.findAll()).thenReturn(largePlayers);

      Map<String, Object> result = playerService.getPlayersStats();

      assertThat(result.get("totalPlayers")).isEqualTo(100);

      // Should group regions and tranches correctly
      @SuppressWarnings("unchecked")
      Map<String, Long> regionStats = (Map<String, Long>) result.get("playersByRegion");
      assertThat(regionStats.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(100);

      @SuppressWarnings("unchecked")
      Map<String, Long> trancheStats = (Map<String, Long>) result.get("playersByTranche");
      assertThat(trancheStats.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("Active Player Management")
  class ActivePlayerTests {

    @Test
    @DisplayName("Should retrieve active players only")
    void shouldRetrieveActivePlayersOnly() {
      // RED: Test active player filtering
      List<Player> activePlayers = Arrays.asList(testPlayer1, testPlayer2);
      when(playerRepository.findActivePlayers()).thenReturn(activePlayers);

      List<PlayerDto> result = playerService.getActivePlayers();

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getNickname()).isEqualTo("Pro Ninja");
      assertThat(result.get(1).getNickname()).isEqualTo("Average Joe");

      verify(playerRepository).findActivePlayers();
    }

    @Test
    @DisplayName("Should handle no active players gracefully")
    void shouldHandleNoActivePlayersGracefully() {
      // RED: Test empty active players scenario
      when(playerRepository.findActivePlayers()).thenReturn(Collections.emptyList());

      List<PlayerDto> result = playerService.getActivePlayers();

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should retrieve active players efficiently for large datasets")
    void shouldRetrieveActivePlayersEfficientlyForLargeDatasets() {
      // RED: Test active players performance
      List<Player> largePlayers = createLargePlayerList(50);
      when(playerRepository.findActivePlayers()).thenReturn(largePlayers);

      List<PlayerDto> result = playerService.getActivePlayers();

      assertThat(result).hasSize(50);

      // Should make only one repository call
      verify(playerRepository, times(1)).findActivePlayers();
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
      // RED: Test repository exception handling
      when(playerRepository.findAll(testPageable))
          .thenThrow(new RuntimeException("Database connection failed"));

      assertThatThrownBy(() -> playerService.getAllPlayers(testPageable))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Database connection failed");

      verify(playerRepository).findAll(testPageable);
    }

    @Test
    @DisplayName("Should handle score repository exceptions in player details")
    void shouldHandleScoreRepositoryExceptionsInPlayerDetails() {
      // RED: Test score repository exception handling
      UUID playerId = testPlayer1.getId();

      when(((PlayerRepositoryPort) playerRepository).findById(playerId))
          .thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.sumPointsByPlayerAndSeason(playerId, 2025))
          .thenThrow(new RuntimeException("Score calculation failed"));

      assertThatThrownBy(() -> playerService.getPlayerById(playerId))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Score calculation failed");
    }

    @Test
    @DisplayName("Should validate pageable parameters")
    void shouldValidatePageableParameters() {
      // RED: Test pageable validation - Spring validates PageRequest creation
      assertThatThrownBy(() -> PageRequest.of(-1, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Page index must not be less than zero");

      // Test that PlayerService would handle invalid parameters gracefully
      Pageable validPageable = PageRequest.of(0, 10);
      when(playerRepository.findAll(validPageable))
          .thenThrow(new IllegalArgumentException("Repository validation failed"));

      assertThatThrownBy(() -> playerService.getAllPlayers(validPageable))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Repository validation failed");
    }
  }
}
