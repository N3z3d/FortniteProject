package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.PlayerRecommendResponse;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidTrancheViolationException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftTrancheService")
class DraftTrancheServiceTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private DraftPickRepositoryPort draftPickRepository;
  @Mock private DraftPickOrchestratorService orchestratorService;

  private DraftTrancheService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID PLAYER_A = UUID.randomUUID();
  private static final UUID PLAYER_B = UUID.randomUUID();
  private static final UUID PLAYER_C = UUID.randomUUID();

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

  private Game buildGame(int trancheSize, boolean tranchesEnabled) {
    return new Game(
        "Test", UUID.randomUUID(), 10, DraftMode.SNAKE, 5, trancheSize, tranchesEnabled);
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

  private SnakeTurn buildTurn(int round, int pick) {
    return new SnakeTurn(UUID.randomUUID(), round, pick, false);
  }

  private Player buildPlayer(UUID id, String tranche) {
    return Player.restore(
        id, null, "user_" + id, "nick_" + id, PlayerRegion.EU, tranche, 2025, false);
  }

  @Nested
  @DisplayName("validatePick")
  class ValidatePick {

    @Test
    @DisplayName("when tranches disabled skips validation entirely")
    void tranchesDisabled_skipsValidation() {
      Game game = buildGame(10, false);
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

      service.validatePick(GAME_ID, "GLOBAL", PLAYER_A);

      verifyNoInteractions(draftDomainRepository, orchestratorService, playerRepository);
    }

    @Test
    @DisplayName("when no active draft throws InvalidDraftStateException")
    void noActiveDraft_throwsInvalidDraftState() {
      Game game = buildGame(10, true);
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.validatePick(GAME_ID, "GLOBAL", PLAYER_A))
          .isInstanceOf(InvalidDraftStateException.class);
    }

    @Test
    @DisplayName("when player tranche meets floor does not throw")
    void playerRespectedFloor_noException() {
      // slot=1, trancheSize=10 → floor=(1-1)*10+1 = 1. Player "1-5" → floor=1 ≥ 1 → OK
      Game game = buildGame(10, true);
      Draft draft = buildDraft();
      SnakeTurn turn = buildTurn(1, 1); // round=1, pick=1 → slot=1
      Player player = buildPlayer(PLAYER_A, "1-5");

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(turn));
      when(playerRepository.findById(PLAYER_A)).thenReturn(Optional.of(player));

      service.validatePick(GAME_ID, "GLOBAL", PLAYER_A);
    }

    @Test
    @DisplayName("when player is too good throws InvalidTrancheViolationException")
    void playerTooGood_throwsInvalidTrancheViolation() {
      // slot=2 (round=1, pick=2, maxParticipants=10) → floor=(2-1)*10+1=11. Player "1-5" → floor=1
      // < 11
      Game game = buildGame(10, true);
      Draft draft = buildDraft();
      SnakeTurn turn = buildTurn(1, 2);
      Player player = buildPlayer(PLAYER_A, "1-5");

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(turn));
      when(playerRepository.findById(PLAYER_A)).thenReturn(Optional.of(player));

      assertThatThrownBy(() -> service.validatePick(GAME_ID, "GLOBAL", PLAYER_A))
          .isInstanceOf(InvalidTrancheViolationException.class)
          .hasMessageContaining("floor");
    }

    @Test
    @DisplayName("when player tranche is 31-infini and floor=11 does not throw")
    void trancheInfini_valid() {
      // slot=2, trancheSize=10 → floor=11. Player "31-infini" → floor=31 ≥ 11 → OK
      Game game = buildGame(10, true);
      Draft draft = buildDraft();
      SnakeTurn turn = buildTurn(1, 2);
      Player player = buildPlayer(PLAYER_A, "31-infini");

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(turn));
      when(playerRepository.findById(PLAYER_A)).thenReturn(Optional.of(player));

      service.validatePick(GAME_ID, "GLOBAL", PLAYER_A);
    }
  }

  @Nested
  @DisplayName("validatePickByDraftId")
  class ValidatePickByDraftId {

    @Test
    @DisplayName("resolves gameId from draft and delegates to validatePick")
    void resolveGameIdFromDraft_delegates() {
      // Tranches disabled → no exception after lookup
      Game game = buildGame(10, false);
      Draft draft = buildDraft();

      when(draftDomainRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draft));
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

      service.validatePickByDraftId(DRAFT_ID, "GLOBAL", PLAYER_A);

      verifyNoInteractions(orchestratorService, playerRepository);
    }

    @Test
    @DisplayName("throws InvalidDraftStateException when draft not found")
    void draftNotFound_throwsInvalidDraftState() {
      when(draftDomainRepository.findById(DRAFT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.validatePickByDraftId(DRAFT_ID, "GLOBAL", PLAYER_A))
          .isInstanceOf(InvalidDraftStateException.class);
    }

    @Test
    @DisplayName("throws PlayerAlreadySelectedException when player already picked in draft")
    void playerAlreadyPickedInDraft_throws() {
      Draft draft = buildDraft();
      when(draftDomainRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draft));
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_A));

      assertThatThrownBy(() -> service.validatePickByDraftId(DRAFT_ID, "GLOBAL", PLAYER_A))
          .isInstanceOf(PlayerAlreadySelectedException.class)
          .hasMessageContaining("already selected in this draft");
    }

    @Test
    @DisplayName("player not in picked list passes through without exception")
    void playerNotPicked_passesThrough() {
      Game game = buildGame(10, false);
      Draft draft = buildDraft();
      when(draftDomainRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draft));
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_B));
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

      service.validatePickByDraftId(DRAFT_ID, "GLOBAL", PLAYER_A);

      verifyNoInteractions(orchestratorService, playerRepository);
    }

    @Test
    @DisplayName("player picked in different draft is allowed in this draft")
    void playerPickedInDifferentDraft_allowed() {
      Game game = buildGame(10, false);
      Draft draft = buildDraft();
      when(draftDomainRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draft));
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

      service.validatePickByDraftId(DRAFT_ID, "GLOBAL", PLAYER_A);

      verifyNoInteractions(orchestratorService, playerRepository);
    }
  }

  @Nested
  @DisplayName("recommendPlayer")
  class RecommendPlayer {

    @Test
    @DisplayName("when tranches disabled returns empty")
    void tranchesDisabled_returnsEmpty() {
      Game game = buildGame(10, false);
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

      Optional<PlayerRecommendResponse> result = service.recommendPlayer(GAME_ID, "GLOBAL");

      assertThat(result).isEmpty();
      verifyNoInteractions(draftDomainRepository, orchestratorService, playerRepository);
    }

    @Test
    @DisplayName("when no active draft returns empty")
    void noActiveDraft_returnsEmpty() {
      Game game = buildGame(10, true);
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.empty());

      Optional<PlayerRecommendResponse> result = service.recommendPlayer(GAME_ID, "GLOBAL");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("when no cursor returns empty")
    void noCursor_returnsEmpty() {
      Game game = buildGame(10, true);
      Draft draft = buildDraft();
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.empty());

      Optional<PlayerRecommendResponse> result = service.recommendPlayer(GAME_ID, "GLOBAL");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns best conforming player not yet picked")
    void returnsBestConformingPlayer() {
      // slot=2, trancheSize=10, maxParticipants=10 → floor=11
      // PLAYER_A already picked. PLAYER_B "6-10" → floor=6 < 11 invalid. PLAYER_C "11-20" →
      // floor=11 OK.
      Game game = buildGame(10, true);
      Draft draft = buildDraft();
      SnakeTurn turn = buildTurn(1, 2); // slot=2

      Player playerB = buildPlayer(PLAYER_B, "6-10");
      Player playerC = buildPlayer(PLAYER_C, "11-20");

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(turn));
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_A));
      when(playerRepository.findActivePlayers()).thenReturn(List.of(playerB, playerC));

      Optional<PlayerRecommendResponse> result = service.recommendPlayer(GAME_ID, "GLOBAL");

      assertThat(result).isPresent();
      assertThat(result.get().id()).isEqualTo(PLAYER_C);
      assertThat(result.get().tranche()).isEqualTo("11-20");
      assertThat(result.get().trancheFloor()).isEqualTo(11);
    }

    @Test
    @DisplayName("when no conforming player available returns empty")
    void noConformingPlayer_returnsEmpty() {
      // slot=2, floor=11. All available players have floor < 11.
      Game game = buildGame(10, true);
      Draft draft = buildDraft();
      SnakeTurn turn = buildTurn(1, 2);

      Player playerA = buildPlayer(PLAYER_A, "1-5");
      Player playerB = buildPlayer(PLAYER_B, "6-10");

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(turn));
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(playerRepository.findActivePlayers()).thenReturn(List.of(playerA, playerB));

      Optional<PlayerRecommendResponse> result = service.recommendPlayer(GAME_ID, "GLOBAL");

      assertThat(result).isEmpty();
    }
  }
}
