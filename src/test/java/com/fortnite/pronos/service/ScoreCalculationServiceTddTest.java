package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.TeamScoreDto;
import com.fortnite.pronos.dto.TeamScoreDto.PlayerScore;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

/**
 * TDD Tests for ScoreCalculationService - Business Critical Component
 *
 * <p>This test suite validates complex score calculations, data aggregation, and performance
 * optimization using RED-GREEN-REFACTOR TDD methodology. ScoreCalculationService handles team
 * scoring logic, player statistics, and temporal calculations essential for the competitive fantasy
 * league experience.
 *
 * <p>Business Logic Areas: - Team score calculation with date range filtering - Player performance
 * aggregation and statistics - Data validation and error handling - Performance optimization for
 * large datasets - Complex date-based queries and calculations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreCalculationService - Business Critical TDD Tests")
class ScoreCalculationServiceTddTest {

  @Mock private TeamRepository teamRepository;
  @Mock private TeamPlayerRepository teamPlayerRepository;
  @Mock private ScoreRepository scoreRepository;

  @InjectMocks private ScoreCalculationService scoreCalculationService;

  private Team testTeam;
  private Player testPlayer1;
  private Player testPlayer2;
  private TeamPlayer testTeamPlayer1;
  private TeamPlayer testTeamPlayer2;
  private List<TeamPlayer> testTeamPlayers;
  private Score testScore1;
  private Score testScore2;
  private Score testScore3;
  private LocalDate startDate;
  private LocalDate endDate;
  private UUID teamId;

  @BeforeEach
  void setUp() {
    // Date setup for calculations
    startDate = LocalDate.of(2025, 1, 1);
    endDate = LocalDate.of(2025, 1, 31);
    teamId = UUID.randomUUID();

    // Team setup
    testTeam = new Team();
    testTeam.setId(teamId);
    testTeam.setName("Test Champions");
    testTeam.setSeason(2025);

    // Players setup with different performance levels
    testPlayer1 =
        Player.builder()
            .id(UUID.randomUUID())
            .username("pro_player")
            .nickname("Pro Player")
            .region(Player.Region.EU)
            .tranche("1-3")
            .build();

    testPlayer2 =
        Player.builder()
            .id(UUID.randomUUID())
            .username("avg_player")
            .nickname("Average Player")
            .region(Player.Region.NAW)
            .tranche("4-7")
            .build();

    // TeamPlayers setup
    testTeamPlayer1 = new TeamPlayer();
    testTeamPlayer1.setTeam(testTeam);
    testTeamPlayer1.setPlayer(testPlayer1);
    testTeamPlayer1.setPosition(1);

    testTeamPlayer2 = new TeamPlayer();
    testTeamPlayer2.setTeam(testTeam);
    testTeamPlayer2.setPlayer(testPlayer2);
    testTeamPlayer2.setPosition(2);

    testTeamPlayers = Arrays.asList(testTeamPlayer1, testTeamPlayer2);

    // Scores setup with realistic data
    testScore1 = new Score();
    testScore1.setPlayer(testPlayer1);
    testScore1.setPoints(1500);
    testScore1.setSeason(2025);
    testScore1.setDate(LocalDate.of(2025, 1, 15));

    testScore2 = new Score();
    testScore2.setPlayer(testPlayer1);
    testScore2.setPoints(800);
    testScore2.setSeason(2025);
    testScore2.setDate(LocalDate.of(2025, 1, 20));

    testScore3 = new Score();
    testScore3.setPlayer(testPlayer2);
    testScore3.setPoints(600);
    testScore3.setSeason(2025);
    testScore3.setDate(LocalDate.of(2025, 1, 10));
  }

  @Nested
  @DisplayName("Team Score Calculation Tests")
  class TeamScoreCalculationTests {

    @Test
    @DisplayName("Should calculate team score with correct aggregation")
    void shouldCalculateTeamScoreWithCorrectAggregation() {
      // RED: Define expected behavior for team score calculation
      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(testTeamPlayers);
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer1, startDate, endDate))
          .thenReturn(Arrays.asList(testScore1, testScore2));
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer2, startDate, endDate))
          .thenReturn(Arrays.asList(testScore3));

      TeamScoreDto result = scoreCalculationService.calculateTeamScore(teamId, startDate, endDate);

      // Verify team information
      assertThat(result.getTeamId()).isEqualTo(teamId);
      assertThat(result.getTeamName()).isEqualTo("Test Champions");
      assertThat(result.getStartDate()).isEqualTo(startDate);
      assertThat(result.getEndDate()).isEqualTo(endDate);

      // Verify total score calculation (1500 + 800 + 600 = 2900)
      assertThat(result.getTotalScore()).isEqualTo(2900);

      // Verify player scores breakdown
      assertThat(result.getPlayerScores()).hasSize(2);

      PlayerScore player1Score =
          result.getPlayerScores().stream()
              .filter(ps -> ps.getPlayerId().equals(testPlayer1.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(player1Score.getTotalPoints()).isEqualTo(2300); // 1500 + 800
      assertThat(player1Score.getPlayerName()).isEqualTo("Pro Player");

      PlayerScore player2Score =
          result.getPlayerScores().stream()
              .filter(ps -> ps.getPlayerId().equals(testPlayer2.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(player2Score.getTotalPoints()).isEqualTo(600);
      assertThat(player2Score.getPlayerName()).isEqualTo("Average Player");

      verify(teamRepository).findById(teamId);
      verify(teamPlayerRepository).findByTeam(testTeam);
      verify(scoreRepository).findByPlayerAndDateBetween(testPlayer1, startDate, endDate);
      verify(scoreRepository).findByPlayerAndDateBetween(testPlayer2, startDate, endDate);
    }

    @Test
    @DisplayName("Should handle team with no scores gracefully")
    void shouldHandleTeamWithNoScoresGracefully() {
      // RED: Test empty score scenario
      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(testTeamPlayers);
      when(scoreRepository.findByPlayerAndDateBetween(any(), eq(startDate), eq(endDate)))
          .thenReturn(Collections.emptyList());

      TeamScoreDto result = scoreCalculationService.calculateTeamScore(teamId, startDate, endDate);

      assertThat(result.getTotalScore()).isEqualTo(0);
      assertThat(result.getPlayerScores()).hasSize(2);
      assertThat(result.getPlayerScores().get(0).getTotalPoints()).isEqualTo(0);
      assertThat(result.getPlayerScores().get(1).getTotalPoints()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle team with no players gracefully")
    void shouldHandleTeamWithNoPlayersGracefully() {
      // RED: Test empty team scenario
      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(Collections.emptyList());

      TeamScoreDto result = scoreCalculationService.calculateTeamScore(teamId, startDate, endDate);

      assertThat(result.getTotalScore()).isEqualTo(0);
      assertThat(result.getPlayerScores()).isEmpty();
      assertThat(result.getTeamName()).isEqualTo("Test Champions");
    }

    @Test
    @DisplayName("Should calculate scores for large teams efficiently")
    void shouldCalculateScoresForLargeTeamsEfficiently() {
      // RED: Test performance with large team
      List<TeamPlayer> largeTeam = new ArrayList<>();
      List<Player> largePlayers = new ArrayList<>();

      // Create team with 10 players
      for (int i = 0; i < 10; i++) {
        Player player =
            Player.builder()
                .id(UUID.randomUUID())
                .username("player_" + i)
                .nickname("Player " + i)
                .region(Player.Region.EU)
                .tranche("1-10")
                .build();
        largePlayers.add(player);

        TeamPlayer teamPlayer = new TeamPlayer();
        teamPlayer.setTeam(testTeam);
        teamPlayer.setPlayer(player);
        teamPlayer.setPosition(i + 1);
        largeTeam.add(teamPlayer);

        // Mock scores for each player
        List<Score> playerScores =
            Arrays.asList(
                createScore(player, 100 * (i + 1), LocalDate.of(2025, 1, 5)),
                createScore(player, 200 * (i + 1), LocalDate.of(2025, 1, 25)));
        when(scoreRepository.findByPlayerAndDateBetween(player, startDate, endDate))
            .thenReturn(playerScores);
      }

      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(largeTeam);

      TeamScoreDto result = scoreCalculationService.calculateTeamScore(teamId, startDate, endDate);

      // Expected total: Sum of (100*(i+1) + 200*(i+1)) for i=0 to 9
      // = Sum of 300*(i+1) for i=0 to 9 = 300 * (1+2+...+10) = 300 * 55 = 16500
      assertThat(result.getTotalScore()).isEqualTo(16500);
      assertThat(result.getPlayerScores()).hasSize(10);

      // Verify each player score
      for (int i = 0; i < 10; i++) {
        int expectedPlayerScore = 300 * (i + 1);
        UUID playerId = largePlayers.get(i).getId();
        PlayerScore playerScore =
            result.getPlayerScores().stream()
                .filter(ps -> ps.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow();
        assertThat(playerScore.getTotalPoints()).isEqualTo(expectedPlayerScore);
      }
    }
  }

  // Helper method for creating test scores - available to all nested classes
  private Score createScore(Player player, int points, LocalDate date) {
    Score score = new Score();
    score.setPlayer(player);
    score.setPoints(points);
    score.setSeason(2025);
    score.setDate(date);
    return score;
  }

  @Nested
  @DisplayName("Date Validation and Edge Cases")
  class DateValidationTests {

    @Test
    @DisplayName("Should reject invalid date ranges")
    void shouldRejectInvalidDateRanges() {
      // RED: Test date validation
      LocalDate invalidStartDate = LocalDate.of(2025, 2, 1);
      LocalDate invalidEndDate = LocalDate.of(2025, 1, 1);

      assertThatThrownBy(
              () ->
                  scoreCalculationService.calculateTeamScore(
                      teamId, invalidStartDate, invalidEndDate))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("La date de fin doit être après la date de début");

      verifyNoInteractions(teamRepository, teamPlayerRepository, scoreRepository);
    }

    @Test
    @DisplayName("Should handle same start and end date")
    void shouldHandleSameStartAndEndDate() {
      // RED: Test single day calculation
      LocalDate singleDate = LocalDate.of(2025, 1, 15);

      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(testTeamPlayers);
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer1, singleDate, singleDate))
          .thenReturn(Arrays.asList(testScore1)); // testScore1 has date 2025-01-15
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer2, singleDate, singleDate))
          .thenReturn(Collections.emptyList());

      TeamScoreDto result =
          scoreCalculationService.calculateTeamScore(teamId, singleDate, singleDate);

      assertThat(result.getTotalScore()).isEqualTo(1500);
      assertThat(result.getStartDate()).isEqualTo(singleDate);
      assertThat(result.getEndDate()).isEqualTo(singleDate);
    }

    @Test
    @DisplayName("Should handle future date ranges gracefully")
    void shouldHandleFutureDateRangesGracefully() {
      // RED: Test future dates (no scores expected)
      LocalDate futureStart = LocalDate.of(2025, 12, 1);
      LocalDate futureEnd = LocalDate.of(2025, 12, 31);

      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(testTeamPlayers);
      when(scoreRepository.findByPlayerAndDateBetween(any(), eq(futureStart), eq(futureEnd)))
          .thenReturn(Collections.emptyList());

      TeamScoreDto result =
          scoreCalculationService.calculateTeamScore(teamId, futureStart, futureEnd);

      assertThat(result.getTotalScore()).isEqualTo(0);
      assertThat(result.getPlayerScores()).hasSize(2);
      assertThat(result.getPlayerScores().stream().allMatch(ps -> ps.getTotalPoints() == 0))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should throw exception for non-existent team")
    void shouldThrowExceptionForNonExistentTeam() {
      // RED: Test team not found scenario
      UUID nonExistentTeamId = UUID.randomUUID();
      when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  scoreCalculationService.calculateTeamScore(nonExistentTeamId, startDate, endDate))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Équipe non trouvée : " + nonExistentTeamId);

      verify(teamRepository).findById(nonExistentTeamId);
      verifyNoInteractions(teamPlayerRepository, scoreRepository);
    }

    @Test
    @DisplayName("Should handle database errors gracefully")
    void shouldHandleDatabaseErrorsGracefully() {
      // RED: Test database exception handling
      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam))
          .thenThrow(new RuntimeException("Database connection failed"));

      assertThatThrownBy(
              () -> scoreCalculationService.calculateTeamScore(teamId, startDate, endDate))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Database connection failed");

      verify(teamRepository).findById(teamId);
      verify(teamPlayerRepository).findByTeam(testTeam);
      verifyNoInteractions(scoreRepository);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
      // RED: Test score repository exception handling
      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(testTeamPlayers);
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer1, startDate, endDate))
          .thenThrow(new RuntimeException("Score repository connection failed"));

      // Should propagate the exception properly
      assertThatThrownBy(
              () -> scoreCalculationService.calculateTeamScore(teamId, startDate, endDate))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Score repository connection failed");

      verify(teamRepository).findById(teamId);
      verify(teamPlayerRepository).findByTeam(testTeam);
      verify(scoreRepository).findByPlayerAndDateBetween(testPlayer1, startDate, endDate);
    }
  }

  @Nested
  @DisplayName("Performance and Data Integrity")
  class PerformanceTests {

    @Test
    @DisplayName("Should minimize database queries efficiently")
    void shouldMinimizeDatabaseQueriesEfficiently() {
      // RED: Test query optimization
      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(testTeamPlayers);
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer1, startDate, endDate))
          .thenReturn(Arrays.asList(testScore1));
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer2, startDate, endDate))
          .thenReturn(Arrays.asList(testScore3));

      scoreCalculationService.calculateTeamScore(teamId, startDate, endDate);

      // Should make exactly the expected number of queries
      verify(teamRepository, times(1)).findById(teamId);
      verify(teamPlayerRepository, times(1)).findByTeam(testTeam);
      verify(scoreRepository, times(1)).findByPlayerAndDateBetween(testPlayer1, startDate, endDate);
      verify(scoreRepository, times(1)).findByPlayerAndDateBetween(testPlayer2, startDate, endDate);

      // Total: 4 queries (1 team + 1 teamPlayers + 2 player scores)
      verifyNoMoreInteractions(teamRepository, teamPlayerRepository, scoreRepository);
    }

    @Test
    @DisplayName("Should maintain calculation accuracy with large numbers")
    void shouldMaintainCalculationAccuracyWithLargeNumbers() {
      // RED: Test numerical precision
      Score largeScore1 =
          createScore(testPlayer1, Integer.MAX_VALUE - 1000, LocalDate.of(2025, 1, 10));
      Score largeScore2 = createScore(testPlayer1, 1000, LocalDate.of(2025, 1, 20));

      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(Arrays.asList(testTeamPlayer1));
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer1, startDate, endDate))
          .thenReturn(Arrays.asList(largeScore1, largeScore2));

      TeamScoreDto result = scoreCalculationService.calculateTeamScore(teamId, startDate, endDate);

      // Should handle large numbers correctly without overflow
      long expectedTotal = (long) (Integer.MAX_VALUE - 1000) + 1000L;
      assertThat(result.getTotalScore()).isEqualTo((int) expectedTotal);
    }

    @Test
    @DisplayName("Should validate all DTO fields are populated correctly")
    void shouldValidateAllDtoFieldsArePopulatedCorrectly() {
      // RED: Test complete DTO construction
      when(teamRepository.findById(teamId)).thenReturn(Optional.of(testTeam));
      when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(testTeamPlayers);
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer1, startDate, endDate))
          .thenReturn(Arrays.asList(testScore1, testScore2));
      when(scoreRepository.findByPlayerAndDateBetween(testPlayer2, startDate, endDate))
          .thenReturn(Arrays.asList(testScore3));

      TeamScoreDto result = scoreCalculationService.calculateTeamScore(teamId, startDate, endDate);

      // Validate all DTO fields
      assertThat(result.getTeamId()).isNotNull().isEqualTo(teamId);
      assertThat(result.getTeamName()).isNotNull().isEqualTo("Test Champions");
      assertThat(result.getTotalScore()).isNotNull().isEqualTo(2900);
      assertThat(result.getStartDate()).isNotNull().isEqualTo(startDate);
      assertThat(result.getEndDate()).isNotNull().isEqualTo(endDate);
      assertThat(result.getPlayerScores()).isNotNull().hasSize(2);

      // Validate PlayerScore fields
      for (PlayerScore playerScore : result.getPlayerScores()) {
        assertThat(playerScore.getPlayerId()).isNotNull();
        assertThat(playerScore.getPlayerName()).isNotNull().isNotEmpty();
        assertThat(playerScore.getPlayerRegion()).isNotNull().isNotEmpty();
        assertThat(playerScore.getTotalPoints()).isNotNull();
      }
    }
  }
}
