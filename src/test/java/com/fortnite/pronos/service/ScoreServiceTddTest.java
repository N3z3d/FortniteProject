package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

/**
 * TDD Tests for ScoreService - Business Critical Component
 *
 * <p>This test suite validates score management, batch operations, and team calculations using
 * RED-GREEN-REFACTOR TDD methodology. ScoreService handles player score updates, team score
 * aggregation, and historical data essential for the fantasy league competition system.
 *
 * <p>Business Logic Areas: - Player score updates with timestamp handling - Batch score operations
 * and error resilience - Team score calculations and season recalculation - Score history and data
 * retrieval - Performance optimization for large datasets
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreService - Business Critical TDD Tests")
class ScoreServiceTddTest {

  @Mock private ScoreRepository scoreRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private UserRepositoryPort userRepository;

  @InjectMocks private ScoreService scoreService;

  private Player testPlayer1;
  private Player testPlayer2;
  private User testUser1;
  private Team testTeam1;
  private Score testScore1;
  private Score testScore2;
  private TeamPlayer testTeamPlayer1;
  private TeamPlayer testTeamPlayer2;
  private UUID playerId1;
  private UUID playerId2;
  private UUID userId1;
  private UUID teamId1;
  private OffsetDateTime testTimestamp;
  private int testSeason = 2025;

  @BeforeEach
  void setUp() {
    // Test timestamp setup
    testTimestamp = OffsetDateTime.now();

    // IDs setup
    playerId1 = UUID.randomUUID();
    playerId2 = UUID.randomUUID();
    userId1 = UUID.randomUUID();
    teamId1 = UUID.randomUUID();

    // Players setup
    testPlayer1 =
        Player.builder()
            .id(playerId1)
            .username("champion_player")
            .nickname("Champion Player")
            .region(Player.Region.EU)
            .tranche("1-3")
            .currentSeason(testSeason)
            .build();

    testPlayer2 =
        Player.builder()
            .id(playerId2)
            .username("backup_player")
            .nickname("Backup Player")
            .region(Player.Region.NAW)
            .tranche("4-7")
            .currentSeason(testSeason)
            .build();

    // User setup
    testUser1 = new User();
    testUser1.setId(userId1);
    testUser1.setUsername("fantasy_owner");
    testUser1.setEmail("owner@test.com");
    testUser1.setCurrentSeason(testSeason);

    // Team setup
    testTeam1 = new Team();
    testTeam1.setId(teamId1);
    testTeam1.setName("Champions Team");
    testTeam1.setOwner(testUser1);
    testTeam1.setSeason(testSeason);

    // TeamPlayers setup
    testTeamPlayer1 = new TeamPlayer();
    testTeamPlayer1.setTeam(testTeam1);
    testTeamPlayer1.setPlayer(testPlayer1);
    testTeamPlayer1.setPosition(1);

    testTeamPlayer2 = new TeamPlayer();
    testTeamPlayer2.setTeam(testTeam1);
    testTeamPlayer2.setPlayer(testPlayer2);
    testTeamPlayer2.setPosition(2);

    testTeam1.setPlayers(Arrays.asList(testTeamPlayer1, testTeamPlayer2));

    // Scores setup
    testScore1 = new Score();
    testScore1.setPlayer(testPlayer1);
    testScore1.setPoints(1500);
    testScore1.setTimestamp(testTimestamp);
    testScore1.setSeason(testSeason);

    testScore2 = new Score();
    testScore2.setPlayer(testPlayer1);
    testScore2.setPoints(800);
    testScore2.setTimestamp(testTimestamp.minusDays(1));
    testScore2.setSeason(testSeason);
  }

  @Nested
  @DisplayName("Player Score Updates")
  class PlayerScoreUpdateTests {

    @Test
    @DisplayName("Should create new score when no existing score for the day")
    void shouldCreateNewScoreWhenNoExistingScoreForTheDay() {
      // RED: Test new score creation
      int newPoints = 2000;
      OffsetDateTime dayStart = testTimestamp.withHour(0).withMinute(0).withSecond(0).withNano(0);
      OffsetDateTime dayEnd = dayStart.plusDays(1);

      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.findByPlayerIdAndTimestampBetween(playerId1, dayStart, dayEnd))
          .thenReturn(Collections.emptyList());
      when(scoreRepository.save(any(Score.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(teamRepository.findTeamsWithActivePlayer(playerId1))
          .thenReturn(Arrays.asList(testTeam1));

      scoreService.updatePlayerScores(playerId1, newPoints, testTimestamp);

      verify(playerRepository).findById(playerId1);
      verify(scoreRepository).findByPlayerIdAndTimestampBetween(playerId1, dayStart, dayEnd);
      verify(scoreRepository)
          .save(
              argThat(
                  score ->
                      score.getPlayer().equals(testPlayer1)
                          && score.getPoints() == newPoints
                          && score.getTimestamp().equals(testTimestamp)));
      verify(teamRepository).findTeamsWithActivePlayer(playerId1);
    }

    @Test
    @DisplayName("Should update existing score when score exists for the day")
    void shouldUpdateExistingScoreWhenScoreExistsForTheDay() {
      // RED: Test score update instead of creation
      int updatedPoints = 2500;
      OffsetDateTime dayStart = testTimestamp.withHour(0).withMinute(0).withSecond(0).withNano(0);
      OffsetDateTime dayEnd = dayStart.plusDays(1);

      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.findByPlayerIdAndTimestampBetween(playerId1, dayStart, dayEnd))
          .thenReturn(Arrays.asList(testScore1));
      when(scoreRepository.save(testScore1)).thenReturn(testScore1);
      when(teamRepository.findTeamsWithActivePlayer(playerId1))
          .thenReturn(Arrays.asList(testTeam1));

      scoreService.updatePlayerScores(playerId1, updatedPoints, testTimestamp);

      assertThat(testScore1.getPoints()).isEqualTo(updatedPoints);
      assertThat(testScore1.getTimestamp()).isEqualTo(testTimestamp);

      verify(scoreRepository).save(testScore1);
      verify(scoreRepository, never()).save(argThat(score -> score != testScore1));
    }

    @Test
    @DisplayName("Should throw exception when player not found")
    void shouldThrowExceptionWhenPlayerNotFound() {
      // RED: Test player validation
      UUID nonExistentPlayerId = UUID.randomUUID();
      when(playerRepository.findById(nonExistentPlayerId)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> scoreService.updatePlayerScores(nonExistentPlayerId, 1000, testTimestamp))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Joueur non trouvé");

      verify(playerRepository).findById(nonExistentPlayerId);
      verifyNoInteractions(scoreRepository, teamRepository);
    }

    @Test
    @DisplayName("Should handle team score updates after player score change")
    void shouldHandleTeamScoreUpdatesAfterPlayerScoreChange() {
      // RED: Test team score recalculation
      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.findByPlayerIdAndTimestampBetween(any(), any(), any()))
          .thenReturn(Collections.emptyList());
      when(scoreRepository.save(any(Score.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(teamRepository.findTeamsWithActivePlayer(playerId1))
          .thenReturn(Arrays.asList(testTeam1));
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId1))
          .thenReturn(Arrays.asList(testScore1));
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId2))
          .thenReturn(Arrays.asList(testScore2));

      scoreService.updatePlayerScores(playerId1, 1500, testTimestamp);

      verify(teamRepository).findTeamsWithActivePlayer(playerId1);
      // Team score calculation should happen but we don't store it anymore (calculated dynamically)
    }
  }

  @Nested
  @DisplayName("Batch Score Operations")
  class BatchScoreOperationTests {

    @Test
    @DisplayName("Should process batch score updates successfully")
    void shouldProcessBatchScoreUpdatesSuccessfully() {
      // RED: Test batch processing
      Map<UUID, Integer> playerScores =
          Map.of(
              playerId1, 1800,
              playerId2, 1200);

      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(playerRepository.findById(playerId2)).thenReturn(Optional.of(testPlayer2));
      when(scoreRepository.findByPlayerIdAndTimestampBetween(any(), any(), any()))
          .thenReturn(Collections.emptyList());
      when(scoreRepository.save(any(Score.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(teamRepository.findTeamsWithActivePlayer(any())).thenReturn(Arrays.asList(testTeam1));

      scoreService.updateBatchPlayerScores(playerScores, testTimestamp);

      verify(playerRepository).findById(playerId1);
      verify(playerRepository).findById(playerId2);
      verify(scoreRepository, times(2)).save(any(Score.class));
    }

    @Test
    @DisplayName("Should handle partial failures in batch operations gracefully")
    void shouldHandlePartialFailuresInBatchOperationsGracefully() {
      // RED: Test error resilience
      UUID invalidPlayerId = UUID.randomUUID();
      Map<UUID, Integer> playerScores =
          Map.of(
              playerId1, 1800,
              invalidPlayerId, 1200 // This will fail
              );

      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(playerRepository.findById(invalidPlayerId)).thenReturn(Optional.empty());
      when(scoreRepository.findByPlayerIdAndTimestampBetween(eq(playerId1), any(), any()))
          .thenReturn(Collections.emptyList());
      when(scoreRepository.save(any(Score.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(teamRepository.findTeamsWithActivePlayer(playerId1))
          .thenReturn(Arrays.asList(testTeam1));

      // Should not throw exception, just log errors
      assertThatCode(() -> scoreService.updateBatchPlayerScores(playerScores, testTimestamp))
          .doesNotThrowAnyException();

      // Verify successful update happened
      verify(playerRepository).findById(playerId1);
      verify(scoreRepository).save(any(Score.class));

      // Verify failed update was attempted
      verify(playerRepository).findById(invalidPlayerId);
    }

    @Test
    @DisplayName("Should handle empty batch gracefully")
    void shouldHandleEmptyBatchGracefully() {
      // RED: Test empty batch scenario
      Map<UUID, Integer> emptyScores = Map.of();

      scoreService.updateBatchPlayerScores(emptyScores, testTimestamp);

      verifyNoInteractions(playerRepository, scoreRepository, teamRepository);
    }
  }

  @Nested
  @DisplayName("Season Score Recalculation")
  class SeasonScoreRecalculationTests {

    @Test
    @DisplayName("Should recalculate scores for all teams in season")
    void shouldRecalculateScoresForAllTeamsInSeason() {
      // RED: Test season-wide recalculation
      Team testTeam2 = new Team();
      testTeam2.setId(UUID.randomUUID());
      testTeam2.setName("Competitors Team");
      testTeam2.setSeason(testSeason);

      List<Team> seasonTeams = Arrays.asList(testTeam1, testTeam2);

      when(teamRepository.findBySeason(testSeason)).thenReturn(seasonTeams);
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId1))
          .thenReturn(Arrays.asList(testScore1));
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId2))
          .thenReturn(Arrays.asList(testScore2));

      scoreService.recalculateSeasonScores(testSeason);

      verify(teamRepository).findBySeason(testSeason);
      // Verify scores were calculated for each team
      verify(scoreRepository, atLeast(1)).findByPlayerIdOrderByTimestampDesc(any());
    }

    @Test
    @DisplayName("Should handle teams with no players gracefully")
    void shouldHandleTeamsWithNoPlayersGracefully() {
      // RED: Test empty team handling
      Team emptyTeam = new Team();
      emptyTeam.setId(UUID.randomUUID());
      emptyTeam.setName("Empty Team");
      emptyTeam.setSeason(testSeason);
      emptyTeam.setPlayers(Collections.emptyList());

      when(teamRepository.findBySeason(testSeason)).thenReturn(Arrays.asList(emptyTeam));

      scoreService.recalculateSeasonScores(testSeason);

      verify(teamRepository).findBySeason(testSeason);
      // Should not crash on empty team
    }

    @Test
    @DisplayName("Should continue recalculation when individual team fails")
    void shouldContinueRecalculationWhenIndividualTeamFails() {
      // RED: Test error resilience in batch operations
      Team failingTeam = new Team();
      failingTeam.setId(UUID.randomUUID());
      failingTeam.setName("Failing Team");
      failingTeam.setSeason(testSeason);

      // Create a failing team player that will cause exception
      Player failingPlayer =
          Player.builder()
              .id(UUID.randomUUID())
              .username("failing_player")
              .nickname("Failing Player")
              .region(Player.Region.BR)
              .tranche("8-10")
              .build();

      TeamPlayer failingTeamPlayer = new TeamPlayer();
      failingTeamPlayer.setTeam(failingTeam);
      failingTeamPlayer.setPlayer(failingPlayer);
      failingTeam.setPlayers(Arrays.asList(failingTeamPlayer));

      List<Team> seasonTeams = Arrays.asList(testTeam1, failingTeam);

      when(teamRepository.findBySeason(testSeason)).thenReturn(seasonTeams);
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId1))
          .thenReturn(Arrays.asList(testScore1));
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(failingPlayer.getId()))
          .thenThrow(new RuntimeException("Database error"));

      // Should not throw exception
      assertThatCode(() -> scoreService.recalculateSeasonScores(testSeason))
          .doesNotThrowAnyException();

      verify(teamRepository).findBySeason(testSeason);
    }
  }

  @Nested
  @DisplayName("Score History and Data Retrieval")
  class ScoreHistoryTests {

    @Test
    @DisplayName("Should retrieve user latest scores from active team players")
    void shouldRetrieveUserLatestScoresFromActiveTeamPlayers() {
      // RED: Test user score retrieval
      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason))
          .thenReturn(Optional.of(testTeam1));
      when(scoreRepository.findLatestScoreByPlayer(eq(playerId1), any()))
          .thenReturn(Optional.of(testScore1));
      when(scoreRepository.findLatestScoreByPlayer(eq(playerId2), any()))
          .thenReturn(Optional.of(testScore2));

      List<Score> result = scoreService.getUserLatestScores(userId1);

      assertThat(result).hasSize(2);
      assertThat(result).containsExactlyInAnyOrder(testScore1, testScore2);

      verify(userRepository).findById(userId1);
      verify(teamRepository).findByOwnerAndSeason(testUser1, testSeason);
      verify(scoreRepository).findLatestScoreByPlayer(eq(playerId1), any());
      verify(scoreRepository).findLatestScoreByPlayer(eq(playerId2), any());
    }

    @Test
    @DisplayName("Should throw exception when user not found for score retrieval")
    void shouldThrowExceptionWhenUserNotFoundForScoreRetrieval() {
      // RED: Test user validation
      UUID nonExistentUserId = UUID.randomUUID();
      when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> scoreService.getUserLatestScores(nonExistentUserId))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Utilisateur non trouvé");

      verify(userRepository).findById(nonExistentUserId);
      verifyNoInteractions(teamRepository, scoreRepository);
    }

    @Test
    @DisplayName("Should retrieve player score history for last 7 days")
    void shouldRetrievePlayerScoreHistoryForLast7Days() {
      // RED: Test player history retrieval
      List<Score> historicalScores = Arrays.asList(testScore1, testScore2);

      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.findByPlayerIdAndTimestampBetween(eq(playerId1), any(), any()))
          .thenReturn(historicalScores);

      Map<UUID, List<Score>> result = scoreService.getPlayerScoreHistory(playerId1);

      assertThat(result).containsKey(playerId1);
      assertThat(result.get(playerId1)).hasSize(2);
      assertThat(result.get(playerId1)).containsExactlyInAnyOrder(testScore1, testScore2);

      verify(playerRepository).findById(playerId1);
      verify(scoreRepository).findByPlayerIdAndTimestampBetween(eq(playerId1), any(), any());
    }

    @Test
    @DisplayName("Should retrieve all scores efficiently")
    void shouldRetrieveAllScoresEfficiently() {
      // RED: Test bulk score retrieval
      List<Score> allScores = Arrays.asList(testScore1, testScore2);
      when(scoreRepository.findAll()).thenReturn(allScores);

      List<Score> result = scoreService.getAllScores();

      assertThat(result).hasSize(2);
      assertThat(result).containsExactlyInAnyOrder(testScore1, testScore2);
      verify(scoreRepository).findAll();
    }
  }

  @Nested
  @DisplayName("Score Management Operations")
  class ScoreManagementTests {

    @Test
    @DisplayName("Should save score with automatic timestamp when not provided")
    void shouldSaveScoreWithAutomaticTimestampWhenNotProvided() {
      // RED: Test score saving with timestamp generation
      Score scoreWithoutTimestamp = new Score();
      scoreWithoutTimestamp.setPlayer(testPlayer1);
      scoreWithoutTimestamp.setPoints(1000);
      // No timestamp set

      when(scoreRepository.save(scoreWithoutTimestamp)).thenReturn(scoreWithoutTimestamp);

      Score result = scoreService.saveScore(scoreWithoutTimestamp);

      assertThat(result.getTimestamp()).isNotNull();
      verify(scoreRepository).save(scoreWithoutTimestamp);
    }

    @Test
    @DisplayName("Should save score preserving existing timestamp")
    void shouldSaveScorePreservingExistingTimestamp() {
      // RED: Test score saving with existing timestamp
      OffsetDateTime existingTimestamp = OffsetDateTime.now().minusHours(2);
      testScore1.setTimestamp(existingTimestamp);

      when(scoreRepository.save(testScore1)).thenReturn(testScore1);

      Score result = scoreService.saveScore(testScore1);

      assertThat(result.getTimestamp()).isEqualTo(existingTimestamp);
      verify(scoreRepository).save(testScore1);
    }

    @Test
    @DisplayName("Should delete all player scores")
    void shouldDeleteAllPlayerScores() {
      // RED: Test score deletion
      List<Score> playerScores = Arrays.asList(testScore1, testScore2);
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId1)).thenReturn(playerScores);

      scoreService.deleteScore(playerId1);

      verify(scoreRepository).findByPlayerIdOrderByTimestampDesc(playerId1);
      verify(scoreRepository).deleteAll(playerScores);
    }

    @Test
    @DisplayName("Should handle deletion when no scores exist")
    void shouldHandleDeletionWhenNoScoresExist() {
      // RED: Test deletion with no scores
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId1))
          .thenReturn(Collections.emptyList());

      scoreService.deleteScore(playerId1);

      verify(scoreRepository).findByPlayerIdOrderByTimestampDesc(playerId1);
      verify(scoreRepository).deleteAll(Collections.emptyList());
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle repository exceptions gracefully in score updates")
    void shouldHandleRepositoryExceptionsGracefullyInScoreUpdates() {
      // RED: Test repository exception handling
      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.findByPlayerIdAndTimestampBetween(any(), any(), any()))
          .thenThrow(new RuntimeException("Database connection failed"));

      assertThatThrownBy(() -> scoreService.updatePlayerScores(playerId1, 1000, testTimestamp))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Database connection failed");

      verify(playerRepository).findById(playerId1);
    }

    @Test
    @DisplayName("Should handle team not found for user score retrieval")
    void shouldHandleTeamNotFoundForUserScoreRetrieval() {
      // RED: Test team not found scenario
      when(userRepository.findById(userId1)).thenReturn(Optional.of(testUser1));
      when(teamRepository.findByOwnerAndSeason(testUser1, testSeason)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> scoreService.getUserLatestScores(userId1))
          .isInstanceOf(EntityNotFoundException.class)
          .hasMessageContaining("Équipe non trouvée");

      verify(userRepository).findById(userId1);
      verify(teamRepository).findByOwnerAndSeason(testUser1, testSeason);
    }

    @Test
    @DisplayName("Should handle large batch operations efficiently")
    void shouldHandleLargeBatchOperationsEfficiently() {
      // RED: Test performance with large batches
      Map<UUID, Integer> largeBatch = new HashMap<>();
      List<Player> largePlayers = new ArrayList<>();

      // Create 100 player score updates
      for (int i = 0; i < 100; i++) {
        UUID playerId = UUID.randomUUID();
        Player player =
            Player.builder()
                .id(playerId)
                .username("player_" + i)
                .nickname("Player " + i)
                .region(Player.Region.EU)
                .tranche("1-10")
                .build();
        largePlayers.add(player);
        largeBatch.put(playerId, 1000 + i);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(scoreRepository.findByPlayerIdAndTimestampBetween(eq(playerId), any(), any()))
            .thenReturn(Collections.emptyList());
        when(teamRepository.findTeamsWithActivePlayer(playerId))
            .thenReturn(Collections.emptyList());
      }

      when(scoreRepository.save(any(Score.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      scoreService.updateBatchPlayerScores(largeBatch, testTimestamp);

      // Should make exactly 100 player lookups and saves
      verify(playerRepository, times(100)).findById(any());
      verify(scoreRepository, times(100)).save(any(Score.class));
    }
  }

  @Nested
  @DisplayName("Performance and Data Integrity")
  class PerformanceTests {

    @Test
    @DisplayName("Should minimize database queries in team score calculation")
    void shouldMinimizeDatabaseQueriesInTeamScoreCalculation() {
      // RED: Test query optimization
      when(teamRepository.findBySeason(testSeason)).thenReturn(Arrays.asList(testTeam1));
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId1))
          .thenReturn(Arrays.asList(testScore1));
      when(scoreRepository.findByPlayerIdOrderByTimestampDesc(playerId2))
          .thenReturn(Arrays.asList(testScore2));

      scoreService.recalculateSeasonScores(testSeason);

      // Should make minimal queries: 1 for teams + 1 per player
      verify(teamRepository, times(1)).findBySeason(testSeason);
      verify(scoreRepository, times(1)).findByPlayerIdOrderByTimestampDesc(playerId1);
      verify(scoreRepository, times(1)).findByPlayerIdOrderByTimestampDesc(playerId2);
    }

    @Test
    @DisplayName("Should handle timestamp precision correctly")
    void shouldHandleTimestampPrecisionCorrectly() {
      // RED: Test timestamp handling precision
      OffsetDateTime preciseTimestamp = OffsetDateTime.now().withNano(123456789);
      OffsetDateTime dayStart =
          preciseTimestamp.withHour(0).withMinute(0).withSecond(0).withNano(0);
      OffsetDateTime dayEnd = dayStart.plusDays(1);

      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.findByPlayerIdAndTimestampBetween(playerId1, dayStart, dayEnd))
          .thenReturn(Collections.emptyList());
      when(scoreRepository.save(any(Score.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(teamRepository.findTeamsWithActivePlayer(playerId1)).thenReturn(Collections.emptyList());

      scoreService.updatePlayerScores(playerId1, 1000, preciseTimestamp);

      verify(scoreRepository).save(argThat(score -> score.getTimestamp().equals(preciseTimestamp)));
    }

    @Test
    @DisplayName("Should validate score data integrity")
    void shouldValidateScoreDataIntegrity() {
      // RED: Test data consistency
      when(playerRepository.findById(playerId1)).thenReturn(Optional.of(testPlayer1));
      when(scoreRepository.findByPlayerIdAndTimestampBetween(any(), any(), any()))
          .thenReturn(Collections.emptyList());
      when(scoreRepository.save(any(Score.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(teamRepository.findTeamsWithActivePlayer(playerId1)).thenReturn(Collections.emptyList());

      scoreService.updatePlayerScores(playerId1, 2500, testTimestamp);

      verify(scoreRepository)
          .save(
              argThat(
                  score ->
                      score.getPlayer().equals(testPlayer1)
                          && score.getPoints() == 2500
                          && score.getTimestamp().equals(testTimestamp)
                          && score.getSeason() == null // Season might be set by entity lifecycle
                  ));
    }
  }
}
