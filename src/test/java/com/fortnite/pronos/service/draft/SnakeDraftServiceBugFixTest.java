package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.DraftStatus;
import com.fortnite.pronos.domain.draft.model.SnakeTurn;
import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.dto.SnakeTurnResponse;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("SnakeDraftService — BUG-06 participantUsername + topic fixes")
class SnakeDraftServiceBugFixTest {

  @Mock private DraftPickOrchestratorService orchestratorService;
  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;
  @Mock private Random random;

  private SnakeDraftService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID USER_A = UUID.randomUUID();
  private static final UUID USER_B = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new SnakeDraftService(
            orchestratorService,
            gameDomainRepository,
            draftDomainRepository,
            gameParticipantRepository,
            random);
  }

  private Game buildGame() {
    return new Game("Test Game", UUID.randomUUID(), 2, DraftMode.SNAKE, 5, 10, false);
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

  private GameParticipant buildParticipantWithUsername(UUID userId, String username) {
    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    return GameParticipant.builder().user(user).build();
  }

  @Nested
  @DisplayName("participantUsername resolution")
  class ParticipantUsernameResolution {

    @Test
    @DisplayName("initializeCursors returns response with participantUsername populated")
    void initializeCursors_populatesParticipantUsername() {
      Game game = buildGame();
      Draft draft = buildDraft();
      SnakeTurn firstTurn = new SnakeTurn(USER_A, 1, 1, false);

      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(gameParticipantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(
              List.of(
                  buildParticipantWithUsername(USER_A, "thibaut"),
                  buildParticipantWithUsername(USER_B, "teddy")));
      when(orchestratorService.getOrInitTurn(eq(DRAFT_ID), eq("GLOBAL"), anyList()))
          .thenReturn(firstTurn);

      SnakeTurnResponse result = service.initializeCursors(GAME_ID);

      assertThat(result.participantUsername()).isEqualTo("thibaut");
    }

    @Test
    @DisplayName("validateAndAdvance returns next turn with participantUsername populated")
    void validateAndAdvance_populatesParticipantUsernameForNextTurn() {
      Draft draft = buildDraft();
      SnakeTurn currentTurn = new SnakeTurn(USER_A, 1, 1, false);
      SnakeTurn nextTurn = new SnakeTurn(USER_B, 1, 2, false);

      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL"))
          .thenReturn(Optional.of(currentTurn));
      when(orchestratorService.advance(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(nextTurn));
      when(gameParticipantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(
              List.of(
                  buildParticipantWithUsername(USER_A, "thibaut"),
                  buildParticipantWithUsername(USER_B, "teddy")));

      SnakeTurnResponse result = service.validateAndAdvance(GAME_ID, USER_A, "GLOBAL");

      assertThat(result.participantId()).isEqualTo(USER_B);
      assertThat(result.participantUsername()).isEqualTo("teddy");
    }

    @Test
    @DisplayName("validateAndAdvance returns null username when participant not found (defensive)")
    void validateAndAdvance_returnsNullUsernameWhenParticipantNotFound() {
      Draft draft = buildDraft();
      SnakeTurn currentTurn = new SnakeTurn(USER_A, 1, 1, false);
      UUID unknownUser = UUID.randomUUID();
      SnakeTurn nextTurn = new SnakeTurn(unknownUser, 1, 2, false);

      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(draft));
      when(orchestratorService.getCurrentTurn(DRAFT_ID, "GLOBAL"))
          .thenReturn(Optional.of(currentTurn));
      when(orchestratorService.advance(DRAFT_ID, "GLOBAL")).thenReturn(Optional.of(nextTurn));
      when(gameParticipantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(List.of(buildParticipantWithUsername(USER_A, "thibaut")));

      SnakeTurnResponse result = service.validateAndAdvance(GAME_ID, USER_A, "GLOBAL");

      assertThat(result.participantId()).isEqualTo(unknownUser);
      assertThat(result.participantUsername()).isNull();
    }
  }
}
