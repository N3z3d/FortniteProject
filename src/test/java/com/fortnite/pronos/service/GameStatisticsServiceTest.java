package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;

@ExtendWith(MockitoExtension.class)
class GameStatisticsServiceTest {

  @Mock private GameRepository gameRepository;

  @Mock private GameParticipantRepository gameParticipantRepository;

  @InjectMocks private GameStatisticsService gameStatisticsService;

  private Game testGame;
  private GameParticipant participant1;
  private GameParticipant participant2;
  private GameParticipant participant3;

  @BeforeEach
  void setUp() {
    // Créer une game de test
    User creator = new User();
    creator.setId(UUID.randomUUID());
    creator.setUsername("Creator");

    testGame = new Game();
    testGame.setId(UUID.randomUUID());
    testGame.setName("Test Game");
    testGame.setCreator(creator);
    testGame.setStatus(GameStatus.ACTIVE);
    testGame.setMaxParticipants(10);

    // Créer des participants avec des joueurs de différentes régions
    participant1 =
        createParticipantWithPlayers(
            "User1",
            createPlayer("Player1", Player.Region.EU),
            createPlayer("Player2", Player.Region.EU),
            createPlayer("Player3", Player.Region.NAW));

    participant2 =
        createParticipantWithPlayers(
            "User2",
            createPlayer("Player4", Player.Region.EU),
            createPlayer("Player5", Player.Region.BR),
            createPlayer("Player6", Player.Region.BR));

    participant3 =
        createParticipantWithPlayers(
            "User3",
            createPlayer("Player7", Player.Region.ASIA),
            createPlayer("Player8", Player.Region.EU),
            createPlayer("Player9", Player.Region.NAW));
  }

  @Test
  @DisplayName("devrait calculer correctement la distribution des joueurs par région")
  void shouldCalculateRegionDistributionCorrectly() {
    // Given
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame))
        .thenReturn(Arrays.asList(participant1, participant2, participant3));

    // When
    Map<Player.Region, Integer> distribution =
        gameStatisticsService.getPlayerDistributionByRegion(testGame.getId());

    // Then
    assertThat(distribution).isNotNull();
    assertThat(distribution).hasSize(4); // EU, NAW, BR, ASIA
    assertThat(distribution.get(Player.Region.EU)).isEqualTo(4); // Players 1, 2, 4, 8
    assertThat(distribution.get(Player.Region.NAW)).isEqualTo(2); // Players 3, 9
    assertThat(distribution.get(Player.Region.BR)).isEqualTo(2); // Players 5, 6
    assertThat(distribution.get(Player.Region.ASIA)).isEqualTo(1); // Player 7
  }

  @Test
  @DisplayName("devrait retourner une map vide si aucun participant")
  void shouldReturnEmptyMapIfNoParticipants() {
    // Given
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame)).thenReturn(Arrays.asList());

    // When
    Map<Player.Region, Integer> distribution =
        gameStatisticsService.getPlayerDistributionByRegion(testGame.getId());

    // Then
    assertThat(distribution).isEmpty();
  }

  @Test
  @DisplayName("devrait lever une exception si la game n'existe pas")
  void shouldThrowExceptionIfGameNotFound() {
    // Given
    UUID unknownGameId = UUID.randomUUID();
    when(gameRepository.findById(unknownGameId)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> gameStatisticsService.getPlayerDistributionByRegion(unknownGameId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Game non trouvée");
  }

  @Test
  @DisplayName("devrait calculer le pourcentage de distribution par région")
  void shouldCalculateRegionDistributionPercentage() {
    // Given
    when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
    when(gameParticipantRepository.findByGame(testGame))
        .thenReturn(Arrays.asList(participant1, participant2, participant3));

    // When
    Map<Player.Region, Double> percentages =
        gameStatisticsService.getPlayerDistributionByRegionPercentage(testGame.getId());

    // Then
    assertThat(percentages).isNotNull();
    assertThat(percentages).hasSize(4);
    assertThat(percentages.get(Player.Region.EU))
        .isCloseTo(44.4, org.assertj.core.api.Assertions.within(0.1)); // 4/9
    assertThat(percentages.get(Player.Region.NAW))
        .isCloseTo(22.2, org.assertj.core.api.Assertions.within(0.1)); // 2/9
    assertThat(percentages.get(Player.Region.BR))
        .isCloseTo(22.2, org.assertj.core.api.Assertions.within(0.1)); // 2/9
    assertThat(percentages.get(Player.Region.ASIA))
        .isCloseTo(11.1, org.assertj.core.api.Assertions.within(0.1)); // 1/9
  }

  // Méthodes utilitaires pour créer des données de test
  private GameParticipant createParticipantWithPlayers(String username, Player... players) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);

    GameParticipant participant = new GameParticipant();
    participant.setId(UUID.randomUUID());
    participant.setUser(user);
    participant.setGame(testGame);
    participant.setSelectedPlayers(Arrays.asList(players));

    return participant;
  }

  private Player createPlayer(String nickname, Player.Region region) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setNickname(nickname);
    player.setUsername(nickname);
    player.setRegion(region);
    player.setTranche("1-10");
    player.setCurrentSeason(2025);
    return player;
  }
}
