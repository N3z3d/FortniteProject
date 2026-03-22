package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.DraftStatus;
import com.fortnite.pronos.domain.draft.model.SnakeTurn;
import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.PlayerRecommendResponse;
import com.fortnite.pronos.exception.InvalidTrancheViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftTrancheService — BUG-03 region fixes")
class DraftTrancheServiceBugFixTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private DraftPickRepositoryPort draftPickRepository;
  @Mock private DraftPickOrchestratorService orchestratorService;

  private DraftTrancheService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new DraftTrancheService(
            gameDomainRepository,
            draftDomainRepository,
            playerRepository,
            draftPickRepository,
            orchestratorService);
  }

  private Game buildGame() {
    Game game = new Game("Test", UUID.randomUUID(), 2, DraftMode.SNAKE, 5, 10, true);
    game.addRegionRule(new GameRegionRule(PlayerRegion.EU, 3));
    return game;
  }

  private Draft buildDraft() {
    return Draft.restore(
        DRAFT_ID,
        GAME_ID,
        DraftStatus.ACTIVE,
        1,
        1,
        5,
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.now(),
        null);
  }

  private Player buildPlayer(UUID id, PlayerRegion region, String tranche) {
    return Player.restore(id, null, "player-" + id, "nick", region, tranche, 2025, false);
  }

  @Nested
  @DisplayName("recommendPlayer — BUG-03 region filter")
  class RecommendPlayerRegionFilter {

    @Test
    @DisplayName("should not recommend an OCE player when region is EU even if better tranche")
    void shouldNotRecommendOcePlayerForEuRegion() {
      UUID ocePlayerId = UUID.randomUUID();
      UUID euPlayerId = UUID.randomUUID();
      Player ocePlayer = buildPlayer(ocePlayerId, PlayerRegion.OCE, "1-10"); // better rank
      Player euPlayer = buildPlayer(euPlayerId, PlayerRegion.EU, "11-20"); // worse rank

      SnakeTurn turn = new SnakeTurn(UUID.randomUUID(), 1, 1, false);

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildGame()));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(buildDraft()));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "EU")).thenReturn(Optional.of(turn));
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(playerRepository.findActivePlayers()).thenReturn(List.of(ocePlayer, euPlayer));

      Optional<PlayerRecommendResponse> result = service.recommendPlayer(GAME_ID, "EU");

      assertThat(result).isPresent();
      assertThat(result.get().id()).isEqualTo(euPlayerId);
    }

    @Test
    @DisplayName("should return empty when no player of the requested region is available")
    void shouldReturnEmptyWhenNoPlayerMatchesRegion() {
      Player ocePlayer = buildPlayer(UUID.randomUUID(), PlayerRegion.OCE, "1-10");

      SnakeTurn turn = new SnakeTurn(UUID.randomUUID(), 1, 1, false);

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildGame()));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(buildDraft()));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "EU")).thenReturn(Optional.of(turn));
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(playerRepository.findActivePlayers()).thenReturn(List.of(ocePlayer));

      Optional<PlayerRecommendResponse> result = service.recommendPlayer(GAME_ID, "EU");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("GLOBAL region should recommend best player regardless of region")
    void globalRegionShouldRecommendAnyPlayer() {
      Game globalGame = new Game("Global", UUID.randomUUID(), 2, DraftMode.SNAKE, 5, 10, true);
      UUID ocePlayerId = UUID.randomUUID();
      Player ocePlayer = buildPlayer(ocePlayerId, PlayerRegion.OCE, "1-10");

      SnakeTurn turn = new SnakeTurn(UUID.randomUUID(), 1, 1, false);

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(globalGame));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(buildDraft()));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(turn));
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(playerRepository.findActivePlayers()).thenReturn(List.of(ocePlayer));

      Optional<PlayerRecommendResponse> result = service.recommendPlayer(GAME_ID, "GLOBAL");

      assertThat(result).isPresent();
      assertThat(result.get().id()).isEqualTo(ocePlayerId);
    }
  }

  @Nested
  @DisplayName("validatePick — BUG-03 region validation")
  class ValidatePickRegionValidation {

    @Test
    @DisplayName("should reject an OCE player when region is EU")
    void shouldRejectOcePlayerForEuRegion() {
      UUID ocePlayerId = UUID.randomUUID();
      Player ocePlayer = buildPlayer(ocePlayerId, PlayerRegion.OCE, "11-20");

      // Region check now runs before tranche check — draftRepository/orchestrator not called
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildGame()));
      when(playerRepository.findById(ocePlayerId)).thenReturn(Optional.of(ocePlayer));

      assertThatThrownBy(() -> service.validatePick(GAME_ID, "EU", ocePlayerId))
          .isInstanceOf(InvalidTrancheViolationException.class)
          .hasMessageContaining("Region violation");
    }

    @Test
    @DisplayName("should accept EU player for EU region when tranche is valid")
    void shouldAcceptEuPlayerForEuRegion() {
      UUID euPlayerId = UUID.randomUUID();
      Player euPlayer = buildPlayer(euPlayerId, PlayerRegion.EU, "1-10");
      SnakeTurn turn = new SnakeTurn(UUID.randomUUID(), 1, 1, false);

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildGame()));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(buildDraft()));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "EU")).thenReturn(Optional.of(turn));
      when(playerRepository.findById(euPlayerId)).thenReturn(Optional.of(euPlayer));

      // Should not throw
      service.validatePick(GAME_ID, "EU", euPlayerId);
    }

    @Test
    @DisplayName("should reject OCE player for EU region even when tranchesEnabled=false (H1 fix)")
    void shouldRejectOcePlayerForEuRegionEvenWhenTranchesDisabled() {
      // Game with tranchesEnabled=false — region check must still apply
      Game gameNoTranches =
          new Game("NoTranches", UUID.randomUUID(), 2, DraftMode.SNAKE, 5, 10, false);
      gameNoTranches.addRegionRule(new GameRegionRule(PlayerRegion.EU, 3));

      UUID ocePlayerId = UUID.randomUUID();
      Player ocePlayer = buildPlayer(ocePlayerId, PlayerRegion.OCE, "11-20");

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(gameNoTranches));
      when(playerRepository.findById(ocePlayerId)).thenReturn(Optional.of(ocePlayer));

      assertThatThrownBy(() -> service.validatePick(GAME_ID, "EU", ocePlayerId))
          .isInstanceOf(InvalidTrancheViolationException.class)
          .hasMessageContaining("Region violation");
    }
  }
}
