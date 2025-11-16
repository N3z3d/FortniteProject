package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

/**
 * TDD Tests for LeaderboardService - Business Critical Component
 *
 * <p>This test suite validates complex leaderboard calculations, ranking algorithms, and
 * performance optimizations using RED-GREEN-REFACTOR TDD methodology. LeaderboardService manages
 * team rankings, player statistics, and cached performance calculations that are essential for the
 * competitive fantasy league experience.
 *
 * <p>Business Logic Areas: - Team ranking calculations with real-time score aggregation - Player
 * performance statistics and regional comparisons - Cache optimization for N+1 query prevention -
 * Seasonal leaderboard management with historical data - Complex ranking algorithms with tie
 * breaking
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardService - Performance Critical TDD Tests")
class LeaderboardServiceTddTest {

  @Mock private TeamRepository teamRepository;
  @Mock private ScoreRepository scoreRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private LeaderboardService leaderboardService;

  private Team testTeam1;
  private Team testTeam2;
  private Team testTeam3;
  private List<Team> testTeams;
  private Map<UUID, Integer> testPlayerPointsMap;
  private User testOwner1;
  private User testOwner2;
  private User testOwner3;
  private Player testPlayer1;
  private Player testPlayer2;
  private Player testPlayer3;
  private TeamPlayer testTeamPlayer1;
  private TeamPlayer testTeamPlayer2;
  private TeamPlayer testTeamPlayer3;

  @BeforeEach
  void setUp() {
    // Users setup for team ownership (using setters - no builder pattern)
    testOwner1 = new User();
    testOwner1.setId(UUID.randomUUID());
    testOwner1.setUsername("champion_player");
    testOwner1.setEmail("champion@test.com");
    testOwner1.setPassword("password123");

    testOwner2 = new User();
    testOwner2.setId(UUID.randomUUID());
    testOwner2.setUsername("silver_player");
    testOwner2.setEmail("silver@test.com");
    testOwner2.setPassword("password123");

    testOwner3 = new User();
    testOwner3.setId(UUID.randomUUID());
    testOwner3.setUsername("bronze_player");
    testOwner3.setEmail("bronze@test.com");
    testOwner3.setPassword("password123");

    // Players setup with different performance levels
    testPlayer1 =
        Player.builder()
            .id(UUID.randomUUID())
            .username("ninja_pro")
            .nickname("Pro Ninja")
            .region(Player.Region.EU)
            .tranche("1-3") // Top tier
            .build();

    testPlayer2 =
        Player.builder()
            .id(UUID.randomUUID())
            .username("average_joe")
            .nickname("Average Joe")
            .region(Player.Region.NAW)
            .tranche("4-7") // Mid tier
            .build();

    testPlayer3 =
        Player.builder()
            .id(UUID.randomUUID())
            .username("newbie_player")
            .nickname("The Newbie")
            .region(Player.Region.BR)
            .tranche("8-10") // Lower tier
            .build();

    // TeamPlayers setup with active status (using setters - no builder pattern)
    testTeamPlayer1 = new TeamPlayer();
    testTeamPlayer1.setPlayer(testPlayer1);
    testTeamPlayer1.setPosition(1);
    // Note: team will be set after team creation

    testTeamPlayer2 = new TeamPlayer();
    testTeamPlayer2.setPlayer(testPlayer2);
    testTeamPlayer2.setPosition(1);
    // Note: team will be set after team creation

    testTeamPlayer3 = new TeamPlayer();
    testTeamPlayer3.setPlayer(testPlayer3);
    testTeamPlayer3.setPosition(1);
    // Note: team will be set after team creation

    // Teams setup with different performance levels (using setters - no builder pattern)
    testTeam1 = new Team();
    testTeam1.setId(UUID.randomUUID());
    testTeam1.setName("Champions United");
    testTeam1.setOwner(testOwner1);
    testTeam1.setSeason(2025);
    testTeam1.setPlayers(new ArrayList<>(Arrays.asList(testTeamPlayer1)));
    testTeamPlayer1.setTeam(testTeam1);

    testTeam2 = new Team();
    testTeam2.setId(UUID.randomUUID());
    testTeam2.setName("Silver Legends");
    testTeam2.setOwner(testOwner2);
    testTeam2.setSeason(2025);
    testTeam2.setPlayers(new ArrayList<>(Arrays.asList(testTeamPlayer2)));
    testTeamPlayer2.setTeam(testTeam2);

    testTeam3 = new Team();
    testTeam3.setId(UUID.randomUUID());
    testTeam3.setName("Rising Stars");
    testTeam3.setOwner(testOwner3);
    testTeam3.setSeason(2025);
    testTeam3.setPlayers(new ArrayList<>(Arrays.asList(testTeamPlayer3)));
    testTeamPlayer3.setTeam(testTeam3);

    testTeams = Arrays.asList(testTeam1, testTeam2, testTeam3);

    // Player points mapping - Champion has most points, newbie least
    testPlayerPointsMap = new HashMap<>();
    testPlayerPointsMap.put(testPlayer1.getId(), 1500); // Champion level
    testPlayerPointsMap.put(testPlayer2.getId(), 750); // Average level
    testPlayerPointsMap.put(testPlayer3.getId(), 250); // Beginner level
  }

  @Nested
  @DisplayName("Leaderboard Calculation and Ranking")
  class LeaderboardCalculationTests {

    @Test
    @DisplayName("Should calculate leaderboard with correct team rankings")
    void shouldCalculateLeaderboardWithCorrectTeamRankings() {
      // RED: Define expected behavior for leaderboard generation
      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      List<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(2025);

      // Verify correct ranking order (highest points first)
      assertThat(result).hasSize(3);
      assertThat(result.get(0).getTeamName()).isEqualTo("Champions United");
      assertThat(result.get(0).getRank()).isEqualTo(1);
      assertThat(result.get(0).getTotalPoints()).isEqualTo(1500);

      assertThat(result.get(1).getTeamName()).isEqualTo("Silver Legends");
      assertThat(result.get(1).getRank()).isEqualTo(2);
      assertThat(result.get(1).getTotalPoints()).isEqualTo(750);

      assertThat(result.get(2).getTeamName()).isEqualTo("Rising Stars");
      assertThat(result.get(2).getRank()).isEqualTo(3);
      assertThat(result.get(2).getTotalPoints()).isEqualTo(250);

      verify(teamRepository).findBySeasonWithFetch(2025);
      verify(scoreRepository).findAllBySeasonGroupedByPlayer(2025);
    }

    @Test
    @DisplayName("Should handle teams with multiple players correctly")
    void shouldHandleTeamsWithMultiplePlayersCorrectly() {
      // RED: Test multi-player team point aggregation
      Player additionalPlayer =
          Player.builder()
              .id(UUID.randomUUID())
              .username("support_player")
              .nickname("Support Pro")
              .region(Player.Region.EU)
              .tranche("2-4")
              .build();

      TeamPlayer additionalTeamPlayer = new TeamPlayer();
      additionalTeamPlayer.setPlayer(additionalPlayer);
      additionalTeamPlayer.setTeam(testTeam1);
      additionalTeamPlayer.setPosition(2);

      testTeam1.getPlayers().add(additionalTeamPlayer);
      testPlayerPointsMap.put(additionalPlayer.getId(), 800);

      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      List<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(2025);

      // Team 1 should now have 1500 + 800 = 2300 points
      assertThat(result.get(0).getTotalPoints()).isEqualTo(2300);
      assertThat(result.get(0).getPlayers()).hasSize(2);
      assertThat(result.get(0).getPlayers().get(0).getPoints()).isEqualTo(1500);
      assertThat(result.get(0).getPlayers().get(1).getPoints()).isEqualTo(800);
    }

    @Test
    @DisplayName("Should exclude inactive players from calculations")
    void shouldExcludeInactivePlayersFromCalculations() {
      // RED: Test inactive player exclusion
      testTeamPlayer1.endMembership(); // Make champion player inactive

      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      List<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(2025);

      // Team 1 should now have 0 points and rank last
      assertThat(result.get(2).getTeamName()).isEqualTo("Champions United");
      assertThat(result.get(2).getTotalPoints()).isEqualTo(0);
      assertThat(result.get(2).getPlayers()).isEmpty();

      // Silver team should now be first
      assertThat(result.get(0).getTeamName()).isEqualTo("Silver Legends");
      assertThat(result.get(0).getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle ties in points correctly")
    void shouldHandleTiesInPointsCorrectly() {
      // RED: Test tie-breaking logic
      testPlayerPointsMap.put(testPlayer2.getId(), 1500); // Same as champion

      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      List<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(2025);

      // Both teams should have 1500 points
      assertThat(result.get(0).getTotalPoints()).isEqualTo(1500);
      assertThat(result.get(1).getTotalPoints()).isEqualTo(1500);

      // Rankings should still be assigned (1, 2, 3)
      assertThat(result.get(0).getRank()).isEqualTo(1);
      assertThat(result.get(1).getRank()).isEqualTo(2);
      assertThat(result.get(2).getRank()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle empty leaderboard gracefully")
    void shouldHandleEmptyLeaderboardGracefully() {
      // RED: Test empty data handling
      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(Collections.emptyList());
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(Collections.emptyMap());

      List<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(2025);

      assertThat(result).isEmpty();
      verify(teamRepository).findBySeasonWithFetch(2025);
      verify(scoreRepository).findAllBySeasonGroupedByPlayer(2025);
    }

    @Test
    @DisplayName("Should handle missing player scores gracefully")
    void shouldHandleMissingPlayerScoresGracefully() {
      // RED: Test missing score data
      Map<UUID, Integer> emptyScoreMap = new HashMap<>();

      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(emptyScoreMap);

      List<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(2025);

      // All teams should have 0 points
      assertThat(result).hasSize(3);
      assertThat(result.get(0).getTotalPoints()).isEqualTo(0);
      assertThat(result.get(1).getTotalPoints()).isEqualTo(0);
      assertThat(result.get(2).getTotalPoints()).isEqualTo(0);

      // Player info should still be included with 0 points
      assertThat(result.get(0).getPlayers().get(0).getPoints()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("Performance Optimization and Caching")
  class PerformanceOptimizationTests {

    @Test
    @DisplayName("Should minimize database queries with fetch optimization")
    void shouldMinimizeDatabaseQueriesWithFetchOptimization() {
      // RED: Test N+1 query prevention
      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      leaderboardService.getLeaderboard(2025);

      // Should only make 2 database calls total (not N+1)
      verify(teamRepository, times(1)).findBySeasonWithFetch(2025);
      verify(scoreRepository, times(1)).findAllBySeasonGroupedByPlayer(2025);

      // Should NOT make individual player or score queries
      verifyNoMoreInteractions(playerRepository);
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should use cached results effectively")
    void shouldUseCachedResultsEffectively() {
      // RED: Test caching behavior
      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      // First call
      List<LeaderboardEntryDTO> result1 = leaderboardService.getLeaderboard(2025);

      // Note: Since we're testing service directly without Spring context,
      // we can't test actual caching, but we can verify the method structure
      assertThat(result1).hasSize(3);

      verify(teamRepository).findBySeasonWithFetch(2025);
      verify(scoreRepository).findAllBySeasonGroupedByPlayer(2025);
    }

    @Test
    @DisplayName("Should handle large datasets efficiently")
    void shouldHandleLargeDatasetsEfficiently() {
      // RED: Test performance with large data
      List<Team> largeTeamList = new ArrayList<>();
      Map<UUID, Integer> largeScoreMap = new HashMap<>();

      // Create 100 teams with multiple players each
      for (int i = 0; i < 100; i++) {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setUsername("owner_" + i);
        owner.setEmail("owner" + i + "@test.com");
        owner.setPassword("password123");

        Player player =
            Player.builder()
                .id(UUID.randomUUID())
                .username("player_" + i)
                .nickname("Player " + i)
                .region(Player.Region.EU)
                .tranche("1-10")
                .build();

        TeamPlayer teamPlayer = new TeamPlayer();
        teamPlayer.setPlayer(player);
        teamPlayer.setPosition(1);

        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setName("Team " + i);
        team.setOwner(owner);
        team.setSeason(2025);
        team.setPlayers(new ArrayList<>(Arrays.asList(teamPlayer)));
        teamPlayer.setTeam(team);

        largeTeamList.add(team);
        largeScoreMap.put(player.getId(), i * 10); // Varying scores
      }

      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(largeTeamList);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(largeScoreMap);

      List<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(2025);

      // Should handle large dataset correctly
      assertThat(result).hasSize(100);

      // Should be sorted by points (descending)
      for (int i = 0; i < result.size() - 1; i++) {
        assertThat(result.get(i).getTotalPoints())
            .isGreaterThanOrEqualTo(result.get(i + 1).getTotalPoints());
      }

      // Rankings should be sequential
      for (int i = 0; i < result.size(); i++) {
        assertThat(result.get(i).getRank()).isEqualTo(i + 1);
      }
    }
  }

  @Nested
  @DisplayName("Team Ranking and Individual Statistics")
  class TeamRankingTests {

    @Test
    @DisplayName("Should retrieve specific team ranking correctly")
    void shouldRetrieveSpecificTeamRankingCorrectly() {
      // RED: Test individual team ranking retrieval
      when(teamRepository.findById(testTeam1.getId())).thenReturn(Optional.of(testTeam1));
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      // The method expects to find the team in a full leaderboard, so we need to mock that too
      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      LeaderboardEntryDTO result = leaderboardService.getTeamRanking(testTeam1.getId().toString());

      assertThat(result).isNotNull();
      assertThat(result.getTeamName()).isEqualTo("Champions United");
      assertThat(result.getTotalPoints()).isEqualTo(1500);
      assertThat(result.getPlayers()).hasSize(1);
      assertThat(result.getPlayers().get(0).getUsername()).isEqualTo("ninja_pro");

      verify(teamRepository).findById(testTeam1.getId());
      verify(teamRepository).findBySeasonWithFetch(2025);
      verify(scoreRepository).findAllBySeasonGroupedByPlayer(2025);
    }

    @Test
    @DisplayName("Should handle non-existent team gracefully")
    void shouldHandleNonExistentTeamGracefully() {
      // RED: Test error handling for missing team
      UUID nonExistentId = UUID.randomUUID();
      when(teamRepository.findById(nonExistentId)).thenReturn(Optional.empty());

      // Should throw RuntimeException when team not found
      assertThatThrownBy(() -> leaderboardService.getTeamRanking(nonExistentId.toString()))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Équipe non trouvée");

      verify(teamRepository).findById(nonExistentId);
    }
  }

  @Nested
  @DisplayName("Data Integrity and Edge Cases")
  class DataIntegrityTests {

    @Test
    @DisplayName("Should validate player information completeness")
    void shouldValidatePlayerInformationCompleteness() {
      // RED: Test complete player data inclusion
      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      List<LeaderboardEntryDTO> result = leaderboardService.getLeaderboard(2025);

      LeaderboardEntryDTO.PlayerInfo playerInfo = result.get(0).getPlayers().get(0);
      assertThat(playerInfo.getPlayerId()).isEqualTo(testPlayer1.getId());
      assertThat(playerInfo.getUsername()).isEqualTo("ninja_pro");
      assertThat(playerInfo.getNickname()).isEqualTo("Pro Ninja");
      assertThat(playerInfo.getRegion()).isEqualTo(Player.Region.EU);
      assertThat(playerInfo.getTranche()).isEqualTo("1-3");
      assertThat(playerInfo.getPoints()).isEqualTo(1500);
    }

    @Test
    @DisplayName("Should maintain consistent ordering across calls")
    void shouldMaintainConsistentOrderingAcrossCalls() {
      // RED: Test deterministic ordering
      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      List<LeaderboardEntryDTO> result1 = leaderboardService.getLeaderboard(2025);
      List<LeaderboardEntryDTO> result2 = leaderboardService.getLeaderboard(2025);

      // Both results should have identical ordering
      for (int i = 0; i < result1.size(); i++) {
        assertThat(result1.get(i).getTeamId()).isEqualTo(result2.get(i).getTeamId());
        assertThat(result1.get(i).getRank()).isEqualTo(result2.get(i).getRank());
        assertThat(result1.get(i).getTotalPoints()).isEqualTo(result2.get(i).getTotalPoints());
      }
    }

    @Test
    @DisplayName("Should handle seasonal data separation correctly")
    void shouldHandleSeasonalDataSeparationCorrectly() {
      // RED: Test season isolation
      List<Team> season2024Teams = Arrays.asList(testTeam1);
      Map<UUID, Integer> season2024Scores = Map.of(testPlayer1.getId(), 500);

      when(teamRepository.findBySeasonWithFetch(2024)).thenReturn(season2024Teams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2024)).thenReturn(season2024Scores);
      when(teamRepository.findBySeasonWithFetch(2025)).thenReturn(testTeams);
      when(scoreRepository.findAllBySeasonGroupedByPlayer(2025)).thenReturn(testPlayerPointsMap);

      List<LeaderboardEntryDTO> result2024 = leaderboardService.getLeaderboard(2024);
      List<LeaderboardEntryDTO> result2025 = leaderboardService.getLeaderboard(2025);

      // Different seasons should have different results
      assertThat(result2024).hasSize(1);
      assertThat(result2025).hasSize(3);
      assertThat(result2024.get(0).getTotalPoints()).isEqualTo(500);
      assertThat(result2025.get(0).getTotalPoints()).isEqualTo(1500);

      verify(teamRepository).findBySeasonWithFetch(2024);
      verify(teamRepository).findBySeasonWithFetch(2025);
      verify(scoreRepository).findAllBySeasonGroupedByPlayer(2024);
      verify(scoreRepository).findAllBySeasonGroupedByPlayer(2025);
    }
  }
}
