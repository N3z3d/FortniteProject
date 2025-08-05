package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.TeamScoreDto;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamPlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreCalculationService - calculateScore TDD")
class ScoreCalculationServiceTest {

  @Mock private TeamRepository teamRepository;

  @Mock private TeamPlayerRepository teamPlayerRepository;

  @Mock private ScoreRepository scoreRepository;

  @InjectMocks private ScoreCalculationService scoreCalculationService;

  private Team testTeam;
  private Player player1;
  private Player player2;
  private Player player3;
  private List<Score> scoresPlayer1;
  private List<Score> scoresPlayer2;
  private List<Score> scoresPlayer3;

  @BeforeEach
  void setUp() {
    // Créer l'équipe
    User owner = new User();
    owner.setId(UUID.randomUUID());
    owner.setUsername("owner");
    owner.setPassword("password123");

    testTeam = new Team();
    testTeam.setId(UUID.randomUUID());
    testTeam.setName("Test Team");
    testTeam.setOwner(owner);
    testTeam.setSeason(2025);

    // Créer les joueurs
    player1 = createPlayer("Player1", Player.Region.EU);
    player2 = createPlayer("Player2", Player.Region.NAW);
    player3 = createPlayer("Player3", Player.Region.BR);

    // Créer les scores
    scoresPlayer1 =
        Arrays.asList(
            createScore(player1, 100, LocalDate.now().minusDays(5)),
            createScore(player1, 150, LocalDate.now().minusDays(3)),
            createScore(player1, 200, LocalDate.now().minusDays(1)));

    scoresPlayer2 =
        Arrays.asList(
            createScore(player2, 80, LocalDate.now().minusDays(4)),
            createScore(player2, 120, LocalDate.now().minusDays(2)));

    scoresPlayer3 =
        Arrays.asList(
            createScore(player3, 300, LocalDate.now().minusDays(10)), // Hors période
            createScore(player3, 90, LocalDate.now().minusDays(1)));
  }

  @Test
  @DisplayName("devrait calculer le score total d'une équipe sur une période")
  void shouldCalculateTeamScoreForPeriod() {
    // Given
    LocalDate startDate = LocalDate.now().minusDays(7);
    LocalDate endDate = LocalDate.now();

    List<TeamPlayer> teamPlayers =
        Arrays.asList(
            createTeamPlayer(testTeam, player1),
            createTeamPlayer(testTeam, player2),
            createTeamPlayer(testTeam, player3));

    when(teamRepository.findById(testTeam.getId())).thenReturn(Optional.of(testTeam));
    when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(teamPlayers);
    when(scoreRepository.findByPlayerAndDateBetween(player1, startDate, endDate))
        .thenReturn(scoresPlayer1);
    when(scoreRepository.findByPlayerAndDateBetween(player2, startDate, endDate))
        .thenReturn(scoresPlayer2);
    when(scoreRepository.findByPlayerAndDateBetween(player3, startDate, endDate))
        .thenReturn(Arrays.asList(scoresPlayer3.get(1))); // Seulement le score dans la période

    // When
    TeamScoreDto result =
        scoreCalculationService.calculateTeamScore(testTeam.getId(), startDate, endDate);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTeamId()).isEqualTo(testTeam.getId());
    assertThat(result.getTeamName()).isEqualTo(testTeam.getName());
    assertThat(result.getTotalScore()).isEqualTo(740); // 450 + 200 + 90
    assertThat(result.getPlayerScores()).hasSize(3);
    assertThat(result.getStartDate()).isEqualTo(startDate);
    assertThat(result.getEndDate()).isEqualTo(endDate);
  }

  @Test
  @DisplayName("devrait calculer le score par joueur avec détails")
  void shouldCalculateScorePerPlayerWithDetails() {
    // Given
    LocalDate startDate = LocalDate.now().minusDays(7);
    LocalDate endDate = LocalDate.now();

    List<TeamPlayer> teamPlayers = Arrays.asList(createTeamPlayer(testTeam, player1));

    when(teamRepository.findById(testTeam.getId())).thenReturn(Optional.of(testTeam));
    when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(teamPlayers);
    when(scoreRepository.findByPlayerAndDateBetween(player1, startDate, endDate))
        .thenReturn(scoresPlayer1);

    // When
    TeamScoreDto result =
        scoreCalculationService.calculateTeamScore(testTeam.getId(), startDate, endDate);

    // Then
    assertThat(result.getPlayerScores()).hasSize(1);

    TeamScoreDto.PlayerScore playerScore = result.getPlayerScores().get(0);
    assertThat(playerScore.getPlayerId()).isEqualTo(player1.getId());
    assertThat(playerScore.getPlayerName()).isEqualTo(player1.getNickname());
    assertThat(playerScore.getTotalPoints()).isEqualTo(450);
    assertThat(playerScore.getScoreCount()).isEqualTo(3);
    assertThat(playerScore.getAverageScore()).isEqualTo(150.0);
  }

  @Test
  @DisplayName("devrait retourner 0 si aucun score dans la période")
  void shouldReturnZeroIfNoScoresInPeriod() {
    // Given
    LocalDate startDate = LocalDate.now().minusDays(30);
    LocalDate endDate = LocalDate.now().minusDays(20);

    List<TeamPlayer> teamPlayers = Arrays.asList(createTeamPlayer(testTeam, player1));

    when(teamRepository.findById(testTeam.getId())).thenReturn(Optional.of(testTeam));
    when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(teamPlayers);
    when(scoreRepository.findByPlayerAndDateBetween(player1, startDate, endDate))
        .thenReturn(Collections.emptyList());

    // When
    TeamScoreDto result =
        scoreCalculationService.calculateTeamScore(testTeam.getId(), startDate, endDate);

    // Then
    assertThat(result.getTotalScore()).isEqualTo(0);
    assertThat(result.getPlayerScores()).hasSize(1);
    assertThat(result.getPlayerScores().get(0).getTotalPoints()).isEqualTo(0);
  }

  @Test
  @DisplayName("devrait lever une exception si l'équipe n'existe pas")
  void shouldThrowExceptionIfTeamNotFound() {
    // Given
    UUID unknownTeamId = UUID.randomUUID();
    LocalDate startDate = LocalDate.now().minusDays(7);
    LocalDate endDate = LocalDate.now();

    when(teamRepository.findById(unknownTeamId)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(
            () -> scoreCalculationService.calculateTeamScore(unknownTeamId, startDate, endDate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Équipe non trouvée");
  }

  @Test
  @DisplayName("devrait valider que la date de fin est après la date de début")
  void shouldValidateEndDateAfterStartDate() {
    // Given
    LocalDate startDate = LocalDate.now();
    LocalDate endDate = LocalDate.now().minusDays(7); // Date de fin avant début

    // When/Then
    assertThatThrownBy(
            () -> scoreCalculationService.calculateTeamScore(testTeam.getId(), startDate, endDate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("La date de fin doit être après la date de début");
  }

  @Test
  @DisplayName("devrait calculer les statistiques avancées (min, max, médiane)")
  void shouldCalculateAdvancedStatistics() {
    // Given
    LocalDate startDate = LocalDate.now().minusDays(7);
    LocalDate endDate = LocalDate.now();

    List<TeamPlayer> teamPlayers = Arrays.asList(createTeamPlayer(testTeam, player1));

    when(teamRepository.findById(testTeam.getId())).thenReturn(Optional.of(testTeam));
    when(teamPlayerRepository.findByTeam(testTeam)).thenReturn(teamPlayers);
    when(scoreRepository.findByPlayerAndDateBetween(player1, startDate, endDate))
        .thenReturn(scoresPlayer1);

    // When
    TeamScoreDto result =
        scoreCalculationService.calculateTeamScore(testTeam.getId(), startDate, endDate);

    // Then
    TeamScoreDto.PlayerScore playerScore = result.getPlayerScores().get(0);
    assertThat(playerScore.getMinScore()).isEqualTo(100);
    assertThat(playerScore.getMaxScore()).isEqualTo(200);
    assertThat(playerScore.getMedianScore()).isEqualTo(150.0);
  }

  // Méthodes utilitaires
  private Player createPlayer(String nickname, Player.Region region) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setNickname(nickname);
    player.setUsername(nickname.toLowerCase());
    player.setRegion(region);
    player.setTranche("1-10");
    player.setCurrentSeason(2025);
    return player;
  }

  private Score createScore(Player player, int points, LocalDate date) {
    Score score = new Score();
    score.setPlayer(player);
    score.setSeason(2025);
    score.setPoints(points);
    score.setDate(date);
    score.setTimestamp(date.atStartOfDay().atOffset(java.time.ZoneOffset.UTC));
    return score;
  }

  private TeamPlayer createTeamPlayer(Team team, Player player) {
    TeamPlayer teamPlayer = new TeamPlayer();
    teamPlayer.setTeam(team);
    teamPlayer.setPlayer(player);
    teamPlayer.setPosition(1);
    return teamPlayer;
  }
}
