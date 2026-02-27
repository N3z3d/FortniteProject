package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.SnakeTurn;
import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameRegionRule;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.dto.SnakeTurnResponse;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.NotYourTurnException;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnakeDraftService")
class SnakeDraftServiceTest {

  @Mock private DraftPickOrchestratorService orchestratorService;
  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;
  @Mock private SimpMessagingTemplate messagingTemplate;
  @Mock private Random random;

  private SnakeDraftService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID USER_A = UUID.randomUUID();
  private static final UUID USER_B = UUID.randomUUID();
  private static final UUID USER_C = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new SnakeDraftService(
            orchestratorService,
            gameDomainRepository,
            draftDomainRepository,
            gameParticipantRepository,
            messagingTemplate,
            random);
  }

  private Game buildGame() {
    return new Game("Test Game", UUID.randomUUID(), 3, DraftMode.SNAKE, 5, 10, false);
  }

  private Draft buildDraft() {
    return Draft.restore(
        DRAFT_ID,
        GAME_ID,
        com.fortnite.pronos.domain.draft.model.DraftStatus.ACTIVE,
        1,
        1,
        5,
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.now(),
        null);
  }

  private GameParticipant buildParticipant(UUID userId) {
    User user = new User();
    user.setId(userId);
    return GameParticipant.builder().user(user).build();
  }

  private SnakeTurn buildTurn(UUID participantId, int round, int pick, boolean reversed) {
    return new SnakeTurn(participantId, round, pick, reversed);
  }

  @Nested
  @DisplayName("initializeCursors")
  class InitializeCursors {

    @Test
    @DisplayName("with no region rules creates GLOBAL cursor and returns first turn")
    void withNoRegionRules_createsGlobalCursorAndReturnsFirstTurn() {
      Game game = buildGame();
      Draft draft = buildDraft();
      SnakeTurn firstTurn = buildTurn(USER_A, 1, 1, false);

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(gameParticipantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(
              List.of(
                  buildParticipant(USER_A), buildParticipant(USER_B), buildParticipant(USER_C)));
      when(orchestratorService.getOrInitTurn(eq(DRAFT_ID), eq("GLOBAL"), anyList()))
          .thenReturn(firstTurn);

      SnakeTurnResponse result = service.initializeCursors(GAME_ID);

      assertThat(result.draftId()).isEqualTo(DRAFT_ID);
      assertThat(result.region()).isEqualTo("GLOBAL");
      assertThat(result.participantId()).isEqualTo(USER_A);
      assertThat(result.round()).isEqualTo(1);
      assertThat(result.pickNumber()).isEqualTo(1);
      assertThat(result.reversed()).isFalse();
      verify(orchestratorService).getOrInitTurn(eq(DRAFT_ID), eq("GLOBAL"), anyList());
    }

    @Test
    @DisplayName("with two region rules creates one cursor per region")
    void withTwoRegionRules_createsCursorPerRegion() {
      Game game = buildGame();
      game.addRegionRule(new GameRegionRule(PlayerRegion.EU, 3));
      game.addRegionRule(new GameRegionRule(PlayerRegion.NAW, 3));
      Draft draft = buildDraft();
      SnakeTurn turn = buildTurn(USER_A, 1, 1, false);

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(gameParticipantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(List.of(buildParticipant(USER_A), buildParticipant(USER_B)));
      when(orchestratorService.getOrInitTurn(eq(DRAFT_ID), anyString(), anyList()))
          .thenReturn(turn);

      service.initializeCursors(GAME_ID);

      verify(orchestratorService).getOrInitTurn(eq(DRAFT_ID), eq("EU"), anyList());
      verify(orchestratorService).getOrInitTurn(eq(DRAFT_ID), eq("NAW"), anyList());
    }

    @Test
    @DisplayName("throws InvalidDraftStateException when no active draft")
    void withNoActiveDraft_throwsInvalidDraftState() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildGame()));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.initializeCursors(GAME_ID))
          .isInstanceOf(InvalidDraftStateException.class);
    }
  }

  @Nested
  @DisplayName("getCurrentTurn")
  class GetCurrentTurn {

    @Test
    @DisplayName("when cursor exists returns SnakeTurnResponse")
    void whenCursorExists_returnsTurn() {
      Draft draft = buildDraft();
      SnakeTurn turn = buildTurn(USER_B, 1, 2, false);

      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(turn));

      Optional<SnakeTurnResponse> result = service.getCurrentTurn(GAME_ID, "GLOBAL");

      assertThat(result).isPresent();
      assertThat(result.get().participantId()).isEqualTo(USER_B);
      assertThat(result.get().round()).isEqualTo(1);
    }

    @Test
    @DisplayName("when no active draft returns empty")
    void whenNoActiveDraft_returnsEmpty() {
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.empty());

      Optional<SnakeTurnResponse> result = service.getCurrentTurn(GAME_ID, "GLOBAL");

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("when cursor absent returns empty")
    void whenCursorAbsent_returnsEmpty() {
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(buildDraft()));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL")).thenReturn(Optional.empty());

      Optional<SnakeTurnResponse> result = service.getCurrentTurn(GAME_ID, "GLOBAL");

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("validateAndAdvance")
  class ValidateAndAdvance {

    @Test
    @DisplayName("when correct user advances cursor and returns next turn")
    void whenCorrectUser_advancesCursorAndReturnsNextTurn() {
      Draft draft = buildDraft();
      SnakeTurn currentTurn = buildTurn(USER_A, 1, 1, false);
      SnakeTurn nextTurn = buildTurn(USER_B, 1, 2, false);

      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL"))
          .thenReturn(Optional.of(currentTurn));
      when(orchestratorService.advance(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(nextTurn));

      SnakeTurnResponse result = service.validateAndAdvance(GAME_ID, USER_A, "GLOBAL");

      assertThat(result.participantId()).isEqualTo(USER_B);
      assertThat(result.round()).isEqualTo(1);
      assertThat(result.pickNumber()).isEqualTo(2);
      verify(messagingTemplate)
          .convertAndSend(
              eq(SnakeDraftService.TOPIC_PREFIX + DRAFT_ID + SnakeDraftService.TOPIC_SUFFIX),
              any(SnakeTurnResponse.class));
    }

    @Test
    @DisplayName("when wrong user throws NotYourTurnException")
    void whenWrongUser_throwsNotYourTurnException() {
      Draft draft = buildDraft();
      SnakeTurn currentTurn = buildTurn(USER_A, 1, 1, false);

      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL"))
          .thenReturn(Optional.of(currentTurn));

      assertThatThrownBy(() -> service.validateAndAdvance(GAME_ID, USER_B, "GLOBAL"))
          .isInstanceOf(NotYourTurnException.class);
    }

    @Test
    @DisplayName("when last pick of round1 advances to round2 reversed")
    void whenLastPickOfRound1_advancesToRound2Reversed() {
      Draft draft = buildDraft();
      SnakeTurn currentTurn = buildTurn(USER_C, 1, 3, false);
      SnakeTurn nextTurn = buildTurn(USER_C, 2, 1, true);

      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL"))
          .thenReturn(Optional.of(currentTurn));
      when(orchestratorService.advance(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(nextTurn));

      SnakeTurnResponse result = service.validateAndAdvance(GAME_ID, USER_C, "GLOBAL");

      assertThat(result.round()).isEqualTo(2);
      assertThat(result.reversed()).isTrue();
      assertThat(result.participantId()).isEqualTo(USER_C);
    }
  }
}
