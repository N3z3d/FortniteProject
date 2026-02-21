package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("GameStatisticsService - Analytics TDD")
class GameStatisticsServiceTddTest {

  @Mock private GameDomainRepositoryPort gameRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;

  @InjectMocks private GameStatisticsService gameStatisticsService;

  @Nested
  @DisplayName("Region distribution")
  class RegionDistributionTests {

    @Test
    @DisplayName("calculates expected distribution by region")
    void shouldCalculateDistributionByRegion() {
      UUID gameId = UUID.randomUUID();
      UUID eu1 = UUID.randomUUID();
      UUID eu2 = UUID.randomUUID();
      UUID naw1 = UUID.randomUUID();

      when(gameRepository.findById(gameId))
          .thenReturn(
              Optional.of(
                  createGame(
                      gameId,
                      List.of(
                          createParticipant(UUID.randomUUID(), List.of(eu1, eu2)),
                          createParticipant(UUID.randomUUID(), List.of(naw1))))));
      when(playerRepository.findById(eu1))
          .thenReturn(Optional.of(createPlayer(eu1, PlayerRegion.EU)));
      when(playerRepository.findById(eu2))
          .thenReturn(Optional.of(createPlayer(eu2, PlayerRegion.EU)));
      when(playerRepository.findById(naw1))
          .thenReturn(Optional.of(createPlayer(naw1, PlayerRegion.NAW)));

      Map<com.fortnite.pronos.model.Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result)
          .containsEntry(com.fortnite.pronos.model.Player.Region.EU, 2)
          .containsEntry(com.fortnite.pronos.model.Player.Region.NAW, 1);
    }

    @Test
    @DisplayName("returns empty distribution when game has no participants")
    void shouldReturnEmptyDistributionWhenNoParticipants() {
      UUID gameId = UUID.randomUUID();
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(createGame(gameId, List.of())));

      Map<com.fortnite.pronos.model.Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns UNKNOWN when player id is null")
    void shouldMapNullPlayerIdsToUnknown() {
      UUID gameId = UUID.randomUUID();
      when(gameRepository.findById(gameId))
          .thenReturn(
              Optional.of(
                  createGame(
                      gameId,
                      List.of(
                          createParticipant(
                              UUID.randomUUID(), java.util.Collections.singletonList(null))))));

      Map<com.fortnite.pronos.model.Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).containsEntry(com.fortnite.pronos.model.Player.Region.UNKNOWN, 1);
      verifyNoInteractions(playerRepository);
    }

    @Test
    @DisplayName("returns UNKNOWN when player does not exist")
    void shouldMapMissingPlayerToUnknown() {
      UUID gameId = UUID.randomUUID();
      UUID missing = UUID.randomUUID();
      when(gameRepository.findById(gameId))
          .thenReturn(
              Optional.of(
                  createGame(
                      gameId, List.of(createParticipant(UUID.randomUUID(), List.of(missing))))));
      when(playerRepository.findById(missing)).thenReturn(Optional.empty());

      Map<com.fortnite.pronos.model.Player.Region, Integer> result =
          gameStatisticsService.getPlayerDistributionByRegion(gameId);

      assertThat(result).containsEntry(com.fortnite.pronos.model.Player.Region.UNKNOWN, 1);
      verify(playerRepository).findById(missing);
    }
  }

  @Nested
  @DisplayName("Percentage distribution")
  class PercentageTests {

    @Test
    @DisplayName("returns empty map when there is no selected player")
    void shouldReturnEmptyPercentagesWhenDistributionIsEmpty() {
      UUID gameId = UUID.randomUUID();
      when(gameRepository.findById(gameId)).thenReturn(Optional.of(createGame(gameId, List.of())));

      Map<com.fortnite.pronos.model.Player.Region, Double> result =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("keeps percentage total to 100")
    void shouldKeepPercentageTotalAtHundred() {
      UUID gameId = UUID.randomUUID();
      UUID eu = UUID.randomUUID();
      UUID nac = UUID.randomUUID();
      UUID br = UUID.randomUUID();
      when(gameRepository.findById(gameId))
          .thenReturn(
              Optional.of(
                  createGame(
                      gameId,
                      List.of(createParticipant(UUID.randomUUID(), List.of(eu, nac, br))))));
      when(playerRepository.findById(eu))
          .thenReturn(Optional.of(createPlayer(eu, PlayerRegion.EU)));
      when(playerRepository.findById(nac))
          .thenReturn(Optional.of(createPlayer(nac, PlayerRegion.NAC)));
      when(playerRepository.findById(br))
          .thenReturn(Optional.of(createPlayer(br, PlayerRegion.BR)));

      Map<com.fortnite.pronos.model.Player.Region, Double> result =
          gameStatisticsService.getPlayerDistributionByRegionPercentage(gameId);

      double total = result.values().stream().mapToDouble(Double::doubleValue).sum();
      assertThat(total).isEqualTo(100.0);
    }
  }

  @Nested
  @DisplayName("Errors")
  class ErrorHandlingTests {

    @Test
    @DisplayName("throws when game does not exist")
    void shouldThrowWhenGameDoesNotExist() {
      UUID gameId = UUID.randomUUID();
      when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> gameStatisticsService.getPlayerDistributionByRegion(gameId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Game non trouvee");

      verify(gameRepository).findById(gameId);
      verifyNoInteractions(playerRepository);
    }

    @Test
    @DisplayName("propagates repository error")
    void shouldPropagateRepositoryError() {
      UUID gameId = UUID.randomUUID();
      when(gameRepository.findById(gameId)).thenThrow(new RuntimeException("DB down"));

      assertThatThrownBy(() -> gameStatisticsService.getPlayerDistributionByRegion(gameId))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("DB down");
    }

    @Test
    @DisplayName("accepts game with participant without selected players")
    void shouldHandleParticipantWithoutSelectedPlayers() {
      UUID gameId = UUID.randomUUID();
      when(gameRepository.findById(gameId))
          .thenReturn(
              Optional.of(
                  createGame(gameId, List.of(createParticipant(UUID.randomUUID(), List.of())))));

      assertThatCode(() -> gameStatisticsService.getPlayerDistributionByRegion(gameId))
          .doesNotThrowAnyException();
    }
  }

  private Game createGame(UUID gameId, List<GameParticipant> participants) {
    return Game.restore(
        gameId,
        "Statistics game",
        null,
        UUID.randomUUID(),
        8,
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
        "user-" + userId,
        1,
        LocalDateTime.now().minusHours(1),
        null,
        false,
        selectedPlayerIds);
  }

  private Player createPlayer(UUID playerId, PlayerRegion region) {
    return Player.restore(playerId, null, "user", "nick", region, "1-10", 2025, false);
  }
}
