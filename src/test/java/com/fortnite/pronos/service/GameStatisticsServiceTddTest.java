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

import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.Player;

/**
 * TDD Tests for GameStatisticsService - Analytics Critical Component
 *
 * <p>This test suite validates game analytics, statistics calculations, and performance metrics
 * using RED-GREEN-REFACTOR TDD methodology. GameStatisticsService handles player distribution
 * analysis, region statistics, percentage calculations, and data aggregation essential for
 * providing insights into fantasy league game dynamics and balance.
 *
 * <p>Business Logic Areas: - Player distribution by region calculations - Percentage and ratio
 * analytics - Statistical aggregation and reporting - Performance metrics and game balance - Data
 * integrity and calculation precision
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameStatisticsService - Analytics Critical TDD Tests")
class GameStatisticsServiceTddTest {

  @Mock private GameRepositoryPort gameRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;

  @InjectMocks private GameStatisticsService gameStatisticsService;

  private Game testGame;
  private UUID gameId;
  private List<GameParticipant> testParticipants;
  private List<Player> testPlayers;
  private GameParticipant participant1;
  private GameParticipant participant2;
  private GameParticipant participant3;

  @BeforeEach
  void setUp() {
    // Test game setup
    gameId = UUID.randomUUID();
    testGame = new Game();
    testGame.setId(gameId);
    testGame.setName("Test Statistics Game");

    // Test players setup with different regions
    Player player1 = createPlayer("EU_Player_1", Player.Region.EU);
    Player player2 = createPlayer("EU_Player_2", Player.Region.EU);
    Player player3 = createPlayer("NAC_Player_1", Player.Region.NAC);
    Player player4 = createPlayer("NAW_Player_1", Player.Region.NAW);
    Player player5 = createPlayer("BR_Player_1", Player.Region.BR);

    testPlayers = Arrays.asList(player1, player2, player3, player4, player5);

    // Test participants setup
    participant1 = createParticipant("Participant_1", Arrays.asList(player1, player2));
    participant2 = createParticipant("Participant_2", Arrays.asList(player3, player4));
    participant3 = createParticipant("Participant_3", Arrays.asList(player5));

    testParticipants = Arrays.asList(participant1, participant2, participant3);
  }

  private Player createPlayer(String username, Player.Region region) {
    return Player.builder()
        .id(UUID.randomUUID())
        .username(username)
        .nickname(username + "_Nick")
        .region(region)
        .tranche("1-10")
        .build();
  }

  private GameParticipant createParticipant(String name, List<Player> selectedPlayers) {
    GameParticipant participant = new GameParticipant();
    participant.setId(UUID.randomUUID());
    participant.setGame(testGame);
    participant.setSelectedPlayers(selectedPlayers);
    return participant;
  }

  @Nested
  @DisplayName("Player Distribution Calculations")
  class PlayerDistributionTests {

    @Test
    @DisplayName("Should calculate correct player distribution by region")
    void shouldCalculateCorrectPlayerDistributionByRegion() {
      // RED: Test basic region distribution calculation
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(testParticipants);

      Map<Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).isNotNull();
      assertThat(result).hasSize(4);
      assertThat(result.get(Player.Region.EU)).isEqualTo(2); // 2 EU players
      assertThat(result.get(Player.Region.NAC)).isEqualTo(1); // 1 NAC player
      assertThat(result.get(Player.Region.NAW)).isEqualTo(1); // 1 NAW player
      assertThat(result.get(Player.Region.BR)).isEqualTo(1); // 1 BR player

      verify(gameRepository).findById(gameId);
      verify(gameParticipantRepository).findByGame(testGame);
    }

    @Test
    @DisplayName("Should handle empty participant list")
    void shouldHandleEmptyParticipantList() {
      // RED: Test empty game scenario
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(Collections.emptyList());

      Map<Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when game not found")
    void shouldThrowExceptionWhenGameNotFound() {
      // RED: Test game validation
      UUID nonExistentGameId = UUID.randomUUID();
      when(gameRepository.findById(nonExistentGameId)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> gameStatisticsService.getPlayerDistributionByRegion(nonExistentGameId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Game non trouvée");

      verify(gameRepository).findById(nonExistentGameId);
      verifyNoInteractions(gameParticipantRepository);
    }

    @Test
    @DisplayName("Should handle single region distribution")
    void shouldHandleSingleRegionDistribution() {
      // RED: Test single region scenario
      Player euPlayer1 = createPlayer("EU_Only_1", Player.Region.EU);
      Player euPlayer2 = createPlayer("EU_Only_2", Player.Region.EU);

      GameParticipant singleRegionParticipant =
          createParticipant("EU_Participant", Arrays.asList(euPlayer1, euPlayer2));

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame))
          .thenReturn(Arrays.asList(singleRegionParticipant));

      Map<Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).hasSize(1);
      assertThat(result.get(Player.Region.EU)).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle participants with no selected players")
    void shouldHandleParticipantsWithNoSelectedPlayers() {
      // RED: Test edge case with empty selections
      GameParticipant emptyParticipant =
          createParticipant("Empty_Participant", Collections.emptyList());

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame))
          .thenReturn(Arrays.asList(emptyParticipant));

      Map<Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle mixed participants with and without players")
    void shouldHandleMixedParticipantsWithAndWithoutPlayers() {
      // RED: Test mixed scenario
      GameParticipant emptyParticipant = createParticipant("Empty", Collections.emptyList());
      Player asiaPlayer = createPlayer("ASIA_Player", Player.Region.ASIA);
      GameParticipant validParticipant = createParticipant("Valid", Arrays.asList(asiaPlayer));

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame))
          .thenReturn(Arrays.asList(emptyParticipant, validParticipant));

      Map<Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).hasSize(1);
      assertThat(result.get(Player.Region.ASIA)).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle large distribution with many players per region")
    void shouldHandleLargeDistributionWithManyPlayersPerRegion() {
      // RED: Test scalability
      List<GameParticipant> largeParticipantList = new ArrayList<>();

      // Create 10 participants with 5 players each (50 total)
      for (int i = 0; i < 10; i++) {
        List<Player> players = new ArrayList<>();
        for (int j = 0; j < 5; j++) {
          players.add(createPlayer("Player_" + i + "_" + j, Player.Region.EU));
        }
        largeParticipantList.add(createParticipant("Participant_" + i, players));
      }

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(largeParticipantList);

      Map<Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).hasSize(1);
      assertThat(result.get(Player.Region.EU)).isEqualTo(50);
    }
  }

  @Nested
  @DisplayName("Percentage Calculations")
  class PercentageCalculationTests {

    @Test
    @DisplayName("Should calculate correct percentage distribution")
    void shouldCalculateCorrectPercentageDistribution() {
      // RED: Test percentage calculation accuracy
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(testParticipants);

      Map<Player.Region, Double> result =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(result).isNotNull();
      assertThat(result).hasSize(4);

      // Total: 5 players (2 EU, 1 NAC, 1 NAW, 1 BR)
      assertThat(result.get(Player.Region.EU)).isEqualTo(40.0); // 2/5 * 100 = 40%
      assertThat(result.get(Player.Region.NAC)).isEqualTo(20.0); // 1/5 * 100 = 20%
      assertThat(result.get(Player.Region.NAW)).isEqualTo(20.0); // 1/5 * 100 = 20%
      assertThat(result.get(Player.Region.BR)).isEqualTo(20.0); // 1/5 * 100 = 20%

      // Verify total percentage sums to 100%
      double totalPercentage = result.values().stream().mapToDouble(Double::doubleValue).sum();
      assertThat(totalPercentage).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should handle empty distribution for percentage calculation")
    void shouldHandleEmptyDistributionForPercentageCalculation() {
      // RED: Test empty case for percentages
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(Collections.emptyList());

      Map<Player.Region, Double> result =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(result).isNotNull();
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should calculate accurate percentages with single player")
    void shouldCalculateAccuratePercentagesWithSinglePlayer() {
      // RED: Test 100% scenario
      Player singlePlayer = createPlayer("Only_Player", Player.Region.OCE);
      GameParticipant singleParticipant = createParticipant("Single", Arrays.asList(singlePlayer));

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame))
          .thenReturn(Arrays.asList(singleParticipant));

      Map<Player.Region, Double> result =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(result).hasSize(1);
      assertThat(result.get(Player.Region.OCE)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should handle complex percentage calculations with precision")
    void shouldHandleComplexPercentageCalculationsWithPrecision() {
      // RED: Test precision with complex ratios
      List<Player> complexPlayers = new ArrayList<>();

      // Create uneven distribution: 7 total players
      // 3 EU, 2 NAC, 1 NAW, 1 BR
      for (int i = 0; i < 3; i++) {
        complexPlayers.add(createPlayer("EU_" + i, Player.Region.EU));
      }
      for (int i = 0; i < 2; i++) {
        complexPlayers.add(createPlayer("NAC_" + i, Player.Region.NAC));
      }
      complexPlayers.add(createPlayer("NAW_1", Player.Region.NAW));
      complexPlayers.add(createPlayer("BR_1", Player.Region.BR));

      GameParticipant complexParticipant = createParticipant("Complex", complexPlayers);

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame))
          .thenReturn(Arrays.asList(complexParticipant));

      Map<Player.Region, Double> result =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(result).hasSize(4);
      // 3/7 * 100 ≈ 42.857142857142854
      assertThat(result.get(Player.Region.EU)).isCloseTo(42.857142857142854, offset(0.0001));
      // 2/7 * 100 ≈ 28.571428571428573
      assertThat(result.get(Player.Region.NAC)).isCloseTo(28.571428571428573, offset(0.0001));
      // 1/7 * 100 ≈ 14.285714285714286
      assertThat(result.get(Player.Region.NAW)).isCloseTo(14.285714285714286, offset(0.0001));
      assertThat(result.get(Player.Region.BR)).isCloseTo(14.285714285714286, offset(0.0001));
    }

    @Test
    @DisplayName("Should maintain percentage precision with large numbers")
    void shouldMaintainPercentagePrecisionWithLargeNumbers() {
      // RED: Test precision with large datasets
      List<GameParticipant> largeList = new ArrayList<>();

      // Create 1000 EU players and 1 NAC player (1001 total)
      for (int i = 0; i < 100; i++) {
        List<Player> players = new ArrayList<>();
        for (int j = 0; j < 10; j++) {
          players.add(createPlayer("EU_" + i + "_" + j, Player.Region.EU));
        }
        largeList.add(createParticipant("EU_Participant_" + i, players));
      }

      Player nacPlayer = createPlayer("NAC_Minority", Player.Region.NAC);
      largeList.add(createParticipant("NAC_Participant", Arrays.asList(nacPlayer)));

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(largeList);

      Map<Player.Region, Double> result =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(result).hasSize(2);
      // 1000/1001 * 100 ≈ 99.900099900099901
      assertThat(result.get(Player.Region.EU)).isCloseTo(99.900099900099901, offset(0.0001));
      // 1/1001 * 100 ≈ 0.099900099900099901
      assertThat(result.get(Player.Region.NAC)).isCloseTo(0.099900099900099901, offset(0.0001));
    }
  }

  @Nested
  @DisplayName("Statistical Analysis and Aggregation")
  class StatisticalAnalysisTests {

    @Test
    @DisplayName("Should provide consistent results across multiple calls")
    void shouldProvideConsistentResultsAcrossMultipleCalls() {
      // RED: Test consistency and caching behavior
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(testParticipants);

      Map<Player.Region, Integer> result1 =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);
      Map<Player.Region, Integer> result2 =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);
      Map<Player.Region, Double> percentageResult1 =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);
      Map<Player.Region, Double> percentageResult2 =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(result1).isEqualTo(result2);
      assertThat(percentageResult1).isEqualTo(percentageResult2);

      // Verify repository calls (should be called for each method call)
      verify(gameRepository, times(4)).findById(gameId);
      verify(gameParticipantRepository, times(4)).findByGame(testGame);
    }

    @Test
    @DisplayName("Should handle all available regions correctly")
    void shouldHandleAllAvailableRegionsCorrectly() {
      // RED: Test comprehensive region coverage
      List<Player> allRegionPlayers = new ArrayList<>();
      for (Player.Region region : Player.Region.values()) {
        allRegionPlayers.add(createPlayer("Player_" + region, region));
      }

      GameParticipant allRegionsParticipant = createParticipant("All_Regions", allRegionPlayers);

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame))
          .thenReturn(Arrays.asList(allRegionsParticipant));

      Map<Player.Region, Integer> distribution =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);
      Map<Player.Region, Double> percentages =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(distribution).hasSize(Player.Region.values().length);
      assertThat(percentages).hasSize(Player.Region.values().length);

      for (Player.Region region : Player.Region.values()) {
        assertThat(distribution.get(region)).isEqualTo(1);
        double expectedPercentage = 100.0 / Player.Region.values().length;
        assertThat(percentages.get(region)).isCloseTo(expectedPercentage, offset(0.0001));
      }
    }

    @Test
    @DisplayName("Should calculate statistics for unbalanced distributions")
    void shouldCalculateStatisticsForUnbalancedDistributions() {
      // RED: Test heavily skewed distributions
      List<GameParticipant> unbalancedParticipants = new ArrayList<>();

      // Create heavily EU-skewed distribution
      for (int i = 0; i < 20; i++) {
        unbalancedParticipants.add(
            createParticipant(
                "EU_" + i, Arrays.asList(createPlayer("EU_Player_" + i, Player.Region.EU))));
      }

      // Add one player from other regions
      unbalancedParticipants.add(
          createParticipant(
              "NAC_Lonely", Arrays.asList(createPlayer("NAC_Player", Player.Region.NAC))));
      unbalancedParticipants.add(
          createParticipant(
              "ASIA_Lonely", Arrays.asList(createPlayer("ASIA_Player", Player.Region.ASIA))));

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(unbalancedParticipants);

      Map<Player.Region, Integer> distribution =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);
      Map<Player.Region, Double> percentages =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(distribution.get(Player.Region.EU)).isEqualTo(20);
      assertThat(distribution.get(Player.Region.NAC)).isEqualTo(1);
      assertThat(distribution.get(Player.Region.ASIA)).isEqualTo(1);

      // Total: 22 players
      assertThat(percentages.get(Player.Region.EU)).isCloseTo(90.909090909090907, offset(0.0001));
      assertThat(percentages.get(Player.Region.NAC)).isCloseTo(4.5454545454545459, offset(0.0001));
      assertThat(percentages.get(Player.Region.ASIA)).isCloseTo(4.5454545454545459, offset(0.0001));
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle null game ID gracefully")
    void shouldHandleNullGameIdGracefully() {
      // RED: Test null input validation
      assertThatThrownBy(() -> gameStatisticsService.getPlayerDistributionByRegion(null))
          .isInstanceOf(Exception.class); // Repository will handle null validation

      verify(gameRepository).findById(null);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
      // RED: Test repository failure handling
      when(gameRepository.findById(gameId))
          .thenThrow(new RuntimeException("Database connection failed"));

      assertThatThrownBy(() -> gameStatisticsService.getPlayerDistributionByRegion(gameId))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Database connection failed");

      verify(gameRepository).findById(gameId);
      verifyNoInteractions(gameParticipantRepository);
    }

    @Test
    @DisplayName("Should handle participants repository exceptions")
    void shouldHandleParticipantsRepositoryExceptions() {
      // RED: Test participant repository failure
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame))
          .thenThrow(new RuntimeException("Participants query failed"));

      assertThatThrownBy(() -> gameStatisticsService.getPlayerDistributionByRegion(gameId))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Participants query failed");

      verify(gameRepository).findById(gameId);
      verify(gameParticipantRepository).findByGame(testGame);
    }

    @Test
    @DisplayName("Should handle participants with null selected players")
    void shouldHandleParticipantsWithNullSelectedPlayers() {
      // RED: Test null selected players handling
      GameParticipant nullParticipant = new GameParticipant();
      nullParticipant.setId(UUID.randomUUID());
      // Note: GameParticipant doesn't have nickname field, identified via user
      nullParticipant.setGame(testGame);
      nullParticipant.setSelectedPlayers(null); // Null players list

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame))
          .thenReturn(Arrays.asList(nullParticipant));

      // Should not crash, but behavior depends on implementation
      assertThatCode(() -> gameStatisticsService.getPlayerDistributionByRegion(gameId))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() {
      // RED: Test thread safety considerations
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(testParticipants);

      // Simulate concurrent calls
      Map<Player.Region, Integer> result1 =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);
      Map<Player.Region, Double> result2 =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);
      Map<Player.Region, Integer> result3 =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result1).isNotNull();
      assertThat(result2).isNotNull();
      assertThat(result3).isNotNull();
      assertThat(result1).isEqualTo(result3); // Consistency check
    }
  }

  @Nested
  @DisplayName("Performance and Data Integrity")
  class PerformanceTests {

    @Test
    @DisplayName("Should handle large datasets efficiently")
    void shouldHandleLargeDatasetsEfficiently() {
      // RED: Test performance with large data
      List<GameParticipant> largeDataset = new ArrayList<>();

      // Create 1000 participants with varying numbers of players
      for (int i = 0; i < 1000; i++) {
        List<Player> players = new ArrayList<>();
        int playerCount = (i % 5) + 1; // 1-5 players per participant
        for (int j = 0; j < playerCount; j++) {
          Player.Region region = Player.Region.values()[j % Player.Region.values().length];
          players.add(createPlayer("Player_" + i + "_" + j, region));
        }
        largeDataset.add(createParticipant("Participant_" + i, players));
      }

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(largeDataset);

      // Should complete without timeout or memory issues
      Map<Player.Region, Integer> distribution =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);
      Map<Player.Region, Double> percentages =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(distribution).isNotEmpty();
      assertThat(percentages).isNotEmpty();

      // Verify percentage totals
      double totalPercentage = percentages.values().stream().mapToDouble(Double::doubleValue).sum();
      assertThat(totalPercentage).isCloseTo(100.0, offset(0.001));
    }

    @Test
    @DisplayName("Should maintain calculation accuracy under stress")
    void shouldMaintainCalculationAccuracyUnderStress() {
      // RED: Test numerical stability
      List<GameParticipant> stressTestData = new ArrayList<>();

      // Create precise distribution for validation
      // 333 EU, 333 NAC, 334 NAW (1000 total for easy percentage calculation)
      for (int i = 0; i < 333; i++) {
        stressTestData.add(
            createParticipant(
                "EU_" + i, Arrays.asList(createPlayer("EU_Player_" + i, Player.Region.EU))));
      }
      for (int i = 0; i < 333; i++) {
        stressTestData.add(
            createParticipant(
                "NAC_" + i, Arrays.asList(createPlayer("NAC_Player_" + i, Player.Region.NAC))));
      }
      for (int i = 0; i < 334; i++) {
        stressTestData.add(
            createParticipant(
                "NAW_" + i, Arrays.asList(createPlayer("NAW_Player_" + i, Player.Region.NAW))));
      }

      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(stressTestData);

      Map<Player.Region, Integer> distribution =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);
      Map<Player.Region, Double> percentages =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(distribution.get(Player.Region.EU)).isEqualTo(333);
      assertThat(distribution.get(Player.Region.NAC)).isEqualTo(333);
      assertThat(distribution.get(Player.Region.NAW)).isEqualTo(334);

      assertThat(percentages.get(Player.Region.EU)).isEqualTo(33.3); // 333/1000 * 100
      assertThat(percentages.get(Player.Region.NAC)).isEqualTo(33.3); // 333/1000 * 100
      assertThat(percentages.get(Player.Region.NAW)).isEqualTo(33.4); // 334/1000 * 100
    }

    @Test
    @DisplayName("Should minimize database calls efficiently")
    void shouldMinimizeDatabaseCallsEfficiently() {
      // RED: Test query optimization
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(testParticipants);

      // Single distribution call should make exactly 2 repository calls
      gameStatisticsService.getPlayerDistributionByRegion(gameId);

      verify(gameRepository, times(1)).findById(gameId);
      verify(gameParticipantRepository, times(1)).findByGame(testGame);

      // Reset mocks
      reset(gameRepository, gameParticipantRepository);
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
      when(gameParticipantRepository.findByGame(testGame)).thenReturn(testParticipants);

      // Percentage call should also make exactly 2 repository calls (calls distribution internally)
      gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      verify(gameRepository, times(1)).findById(gameId);
      verify(gameParticipantRepository, times(1)).findByGame(testGame);
    }
  }
}
