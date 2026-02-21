package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"java:S5838", "java:S5853"})
class GameStatisticsServiceTest {

  @Mock private GameDomainRepositoryPort gameRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;

  @InjectMocks private GameStatisticsService gameStatisticsService;

  private UUID gameId;
  private UUID playerEu1;
  private UUID playerEu2;
  private UUID playerNaw1;

  @BeforeEach
  void setUp() {
    gameId = UUID.randomUUID();
    playerEu1 = UUID.randomUUID();
    playerEu2 = UUID.randomUUID();
    playerNaw1 = UUID.randomUUID();
  }

  @Test
  @DisplayName("devrait calculer correctement la distribution des joueurs par region")
  void shouldCalculateRegionDistributionCorrectly() {
    Game game =
        createGame(
            gameId,
            List.of(
                createParticipant(UUID.randomUUID(), List.of(playerEu1, playerEu2)),
                createParticipant(UUID.randomUUID(), List.of(playerNaw1))));

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(playerRepository.findById(playerEu1))
        .thenReturn(Optional.of(createPlayer(playerEu1, PlayerRegion.EU)));
    when(playerRepository.findById(playerEu2))
        .thenReturn(Optional.of(createPlayer(playerEu2, PlayerRegion.EU)));
    when(playerRepository.findById(playerNaw1))
        .thenReturn(Optional.of(createPlayer(playerNaw1, PlayerRegion.NAW)));

    Map<com.fortnite.pronos.model.Player.Region, Integer> distribution =
        gameStatisticsService.getPlayerDistributionByRegion(gameId);

    assertThat(distribution)
        .hasSize(2)
        .containsEntry(com.fortnite.pronos.model.Player.Region.EU, 2);
    assertThat(distribution).containsEntry(com.fortnite.pronos.model.Player.Region.NAW, 1);
  }

  @Test
  @DisplayName("devrait retourner une map vide si aucun participant")
  void shouldReturnEmptyMapIfNoParticipants() {
    Game game = createGame(gameId, List.of());

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

    Map<com.fortnite.pronos.model.Player.Region, Integer> distribution =
        gameStatisticsService.getPlayerDistributionByRegion(gameId);

    assertThat(distribution).isEmpty();
  }

  @Test
  @DisplayName("devrait lever une exception si la game n'existe pas")
  void shouldThrowExceptionIfGameNotFound() {
    UUID unknownGameId = UUID.randomUUID();
    when(gameRepository.findById(unknownGameId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> gameStatisticsService.getPlayerDistributionByRegion(unknownGameId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Game non trouvee");
  }

  @Test
  @DisplayName("devrait calculer le pourcentage de distribution par region")
  void shouldCalculateRegionDistributionPercentage() {
    Game game =
        createGame(
            gameId,
            List.of(
                createParticipant(UUID.randomUUID(), List.of(playerEu1, playerEu2)),
                createParticipant(UUID.randomUUID(), List.of(playerNaw1))));

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(playerRepository.findById(playerEu1))
        .thenReturn(Optional.of(createPlayer(playerEu1, PlayerRegion.EU)));
    when(playerRepository.findById(playerEu2))
        .thenReturn(Optional.of(createPlayer(playerEu2, PlayerRegion.EU)));
    when(playerRepository.findById(playerNaw1))
        .thenReturn(Optional.of(createPlayer(playerNaw1, PlayerRegion.NAW)));

    Map<com.fortnite.pronos.model.Player.Region, Double> percentages =
        gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

    assertThat(percentages).hasSize(2);
    assertThat(percentages.get(com.fortnite.pronos.model.Player.Region.EU))
        .isEqualTo(66.66666666666667);
    assertThat(percentages.get(com.fortnite.pronos.model.Player.Region.NAW))
        .isEqualTo(33.333333333333336);
  }

  @Test
  @DisplayName("devrait mapper les joueurs introuvables sur UNKNOWN")
  void shouldMapMissingPlayersToUnknownRegion() {
    UUID missingPlayerId = UUID.randomUUID();
    Game game =
        createGame(gameId, List.of(createParticipant(UUID.randomUUID(), List.of(missingPlayerId))));

    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(playerRepository.findById(missingPlayerId)).thenReturn(Optional.empty());

    Map<com.fortnite.pronos.model.Player.Region, Integer> distribution =
        gameStatisticsService.getPlayerDistributionByRegion(gameId);

    assertThat(distribution).containsEntry(com.fortnite.pronos.model.Player.Region.UNKNOWN, 1);
  }

  private Game createGame(UUID id, List<GameParticipant> participants) {
    return Game.restore(
        id,
        "Test Game",
        null,
        UUID.randomUUID(),
        10,
        GameStatus.ACTIVE,
        LocalDateTime.now().minusDays(1),
        null,
        null,
        null,
        null,
        List.of(),
        participants,
        null,
        false,
        5,
        null,
        2025);
  }

  private GameParticipant createParticipant(UUID userId, List<UUID> selectedPlayerIds) {
    return GameParticipant.restore(
        UUID.randomUUID(),
        userId,
        "User-" + userId,
        1,
        LocalDateTime.now().minusHours(2),
        null,
        false,
        selectedPlayerIds);
  }

  private Player createPlayer(UUID id, PlayerRegion region) {
    return Player.restore(id, null, "user-" + id, "nick-" + id, region, "1-10", 2025, false);
  }
}
