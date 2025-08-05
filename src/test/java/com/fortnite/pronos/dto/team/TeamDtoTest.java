package com.fortnite.pronos.dto.team;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;
import com.fortnite.pronos.model.User;

/** Tests TDD pour TeamDto */
@DisplayName("Tests TDD - TeamDto")
class TeamDtoTest {

  private User testUser;
  private Player testPlayer1;
  private Player testPlayer2;
  private Team testTeam;
  private TeamPlayer testTeamPlayer1;
  private TeamPlayer testTeamPlayer2;
  private Score testScore1;
  private Score testScore2;

  @BeforeEach
  void setUp() {
    // Créer un utilisateur de test
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");

    // Créer des joueurs de test
    testPlayer1 = new Player();
    testPlayer1.setId(UUID.randomUUID());
    testPlayer1.setUsername("TestPlayer1");
    testPlayer1.setNickname("TP1");
    testPlayer1.setRegion(Player.Region.EU);
    testPlayer1.setTranche("S");

    testPlayer2 = new Player();
    testPlayer2.setId(UUID.randomUUID());
    testPlayer2.setUsername("TestPlayer2");
    testPlayer2.setNickname("TP2");
    testPlayer2.setRegion(Player.Region.NAW);
    testPlayer2.setTranche("A");

    // Créer des scores de test
    testScore1 = new Score();
    testScore1.setPlayer(testPlayer1);
    testScore1.setSeason(2025);
    testScore1.setPoints(100);

    testScore2 = new Score();
    testScore2.setPlayer(testPlayer2);
    testScore2.setSeason(2025);
    testScore2.setPoints(150);

    // Créer des TeamPlayer de test
    testTeamPlayer1 = new TeamPlayer();
    testTeamPlayer1.setTeam(null); // Sera défini plus tard
    testTeamPlayer1.setPlayer(testPlayer1);
    testTeamPlayer1.setPosition(1);
    testTeamPlayer1.setUntil(null); // Joueur actuel

    testTeamPlayer2 = new TeamPlayer();
    testTeamPlayer2.setTeam(null); // Sera défini plus tard
    testTeamPlayer2.setPlayer(testPlayer2);
    testTeamPlayer2.setPosition(2);
    testTeamPlayer2.setUntil(null); // Joueur actuel

    // Créer une équipe de test
    testTeam = new Team();
    testTeam.setId(UUID.randomUUID());
    testTeam.setName("Test Team");
    testTeam.setOwner(testUser);
    testTeam.setSeason(2025);

    // Configurer les relations
    List<TeamPlayer> teamPlayers = new ArrayList<>();
    teamPlayers.add(testTeamPlayer1);
    teamPlayers.add(testTeamPlayer2);
    testTeam.setPlayers(teamPlayers);

    testTeamPlayer1.setTeam(testTeam);
    testTeamPlayer2.setTeam(testTeam);

    // Configurer les scores des joueurs
    List<Score> player1Scores = new ArrayList<>();
    player1Scores.add(testScore1);
    testPlayer1.setScores(player1Scores);

    List<Score> player2Scores = new ArrayList<>();
    player2Scores.add(testScore2);
    testPlayer2.setScores(player2Scores);
  }

  @Test
  @DisplayName("Devrait créer un TeamDto à partir d'une Team")
  void shouldCreateTeamDtoFromTeam() {
    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(testTeam.getId());
    assertThat(result.getName()).isEqualTo(testTeam.getName());
    assertThat(result.getSeason()).isEqualTo(testTeam.getSeason());
    assertThat(result.getOwnerUsername()).isEqualTo(testUser.getUsername());
  }

  @Test
  @DisplayName("Devrait calculer le score total correctement")
  void shouldCalculateTotalScoreCorrectly() {
    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    assertThat(result.getTotalScore()).isEqualTo(250); // 100 + 150
  }

  @Test
  @DisplayName("Devrait inclure tous les joueurs actuels")
  void shouldIncludeAllCurrentPlayers() {
    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    assertThat(result.getPlayers()).hasSize(2);
    assertThat(result.getPlayers()).anyMatch(p -> p.getPlayerId().equals(testPlayer1.getId()));
    assertThat(result.getPlayers()).anyMatch(p -> p.getPlayerId().equals(testPlayer2.getId()));
  }

  @Test
  @DisplayName("Devrait mapper correctement les propriétés des joueurs")
  void shouldMapPlayerPropertiesCorrectly() {
    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    TeamDto.TeamPlayerDto player1Dto =
        result.getPlayers().stream()
            .filter(p -> p.getPlayerId().equals(testPlayer1.getId()))
            .findFirst()
            .orElse(null);

    assertThat(player1Dto).isNotNull();
    assertThat(player1Dto.getNickname()).isEqualTo(testPlayer1.getNickname());
    assertThat(player1Dto.getRegion()).isEqualTo(testPlayer1.getRegion().name());
    assertThat(player1Dto.getTranche()).isEqualTo(testPlayer1.getTranche());

    TeamDto.TeamPlayerDto player2Dto =
        result.getPlayers().stream()
            .filter(p -> p.getPlayerId().equals(testPlayer2.getId()))
            .findFirst()
            .orElse(null);

    assertThat(player2Dto).isNotNull();
    assertThat(player2Dto.getNickname()).isEqualTo(testPlayer2.getNickname());
    assertThat(player2Dto.getRegion()).isEqualTo(testPlayer2.getRegion().name());
    assertThat(player2Dto.getTranche()).isEqualTo(testPlayer2.getTranche());
  }

  @Test
  @DisplayName("Ne devrait pas inclure les joueurs inactifs")
  void shouldNotIncludeInactivePlayers() {
    // Given
    testTeamPlayer2.setUntil(OffsetDateTime.now().minusDays(1)); // Joueur inactif

    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    assertThat(result.getPlayers()).hasSize(1);
    assertThat(result.getPlayers().get(0).getPlayerId()).isEqualTo(testPlayer1.getId());
  }

  @Test
  @DisplayName("Devrait retourner un score total de 0 pour une équipe sans joueurs")
  void shouldReturnZeroTotalScoreForTeamWithoutPlayers() {
    // Given
    testTeam.setPlayers(new ArrayList<>());

    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    assertThat(result.getTotalScore()).isEqualTo(0);
    assertThat(result.getPlayers()).isEmpty();
  }

  @Test
  @DisplayName("Devrait gérer les joueurs sans scores")
  void shouldHandlePlayersWithoutScores() {
    // Given
    testPlayer1.setScores(new ArrayList<>());
    testPlayer2.setScores(new ArrayList<>());

    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    assertThat(result.getTotalScore()).isEqualTo(0);
    assertThat(result.getPlayers()).hasSize(2);
  }

  @Test
  @DisplayName("Devrait filtrer les scores par saison")
  void shouldFilterScoresBySeason() {
    // Given
    Score oldScore = new Score();
    oldScore.setId(UUID.randomUUID());
    oldScore.setPlayer(testPlayer1);
    oldScore.setSeason(2024); // Saison différente
    oldScore.setPoints(50);

    List<Score> allScores = new ArrayList<>();
    allScores.add(testScore1);
    allScores.add(oldScore);
    testPlayer1.setScores(allScores);

    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    assertThat(result.getTotalScore()).isEqualTo(250); // Seulement les scores de 2025
  }

  @Test
  @DisplayName("Devrait créer un TeamDto avec des propriétés null pour une équipe vide")
  void shouldCreateTeamDtoWithNullPropertiesForEmptyTeam() {
    // Given
    Team emptyTeam = new Team();
    emptyTeam.setId(UUID.randomUUID());
    emptyTeam.setName("Empty Team");
    emptyTeam.setOwner(testUser);
    emptyTeam.setSeason(2025);
    emptyTeam.setPlayers(new ArrayList<>());

    // When
    TeamDto result = TeamDto.from(emptyTeam);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(emptyTeam.getId());
    assertThat(result.getName()).isEqualTo(emptyTeam.getName());
    assertThat(result.getSeason()).isEqualTo(emptyTeam.getSeason());
    assertThat(result.getOwnerUsername()).isEqualTo(testUser.getUsername());
    assertThat(result.getTotalScore()).isEqualTo(0);
    assertThat(result.getPlayers()).isEmpty();
  }

  @Test
  @DisplayName("Devrait gérer les joueurs avec des scores multiples")
  void shouldHandlePlayersWithMultipleScores() {
    // Given
    Score additionalScore = new Score();
    additionalScore.setId(UUID.randomUUID());
    additionalScore.setPlayer(testPlayer1);
    additionalScore.setSeason(2025);
    additionalScore.setPoints(75);

    List<Score> multipleScores = new ArrayList<>();
    multipleScores.add(testScore1);
    multipleScores.add(additionalScore);
    testPlayer1.setScores(multipleScores);

    // When
    TeamDto result = TeamDto.from(testTeam);

    // Then
    assertThat(result.getTotalScore()).isEqualTo(325); // 100 + 75 + 150
  }

  @Test
  @DisplayName("Devrait créer un TeamPlayerDto avec les bonnes propriétés")
  void shouldCreateTeamPlayerDtoWithCorrectProperties() {
    // When
    TeamDto result = TeamDto.from(testTeam);
    TeamDto.TeamPlayerDto playerDto = result.getPlayers().get(0);

    // Then
    assertThat(playerDto).isNotNull();
    assertThat(playerDto.getPlayerId()).isNotNull();
    assertThat(playerDto.getNickname()).isNotNull();
    assertThat(playerDto.getRegion()).isNotNull();
    assertThat(playerDto.getTranche()).isNotNull();
  }
}
