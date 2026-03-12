package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.DraftPick;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.draft.DraftService;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameDraftService - TDD selectPlayer persistence")
class GameDraftServiceTddTest {

  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftRepositoryPort draftRepository;
  @Mock private DraftPickRepositoryPort draftPickRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;
  @Mock private DraftService draftService;

  @InjectMocks private GameDraftService gameDraftService;

  private UUID gameId;
  private UUID userId;
  private UUID playerId;
  private UUID draftId;
  private Game domainGame;
  private com.fortnite.pronos.domain.draft.model.Draft domainDraft;
  private com.fortnite.pronos.model.Game game;
  private Draft draft;
  private com.fortnite.pronos.domain.player.model.Player player;
  private GameParticipant participant;

  @BeforeEach
  void setUp() {
    gameId = UUID.randomUUID();
    userId = UUID.randomUUID();
    playerId = UUID.randomUUID();
    draftId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);

    domainGame =
        Game.restore(
            gameId,
            "Test Game",
            null,
            UUID.randomUUID(),
            4,
            GameStatus.DRAFTING,
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            null,
            false,
            5,
            null,
            2026);

    game =
        com.fortnite.pronos.model.Game.builder()
            .id(gameId)
            .name("Test Game")
            .maxParticipants(4)
            .build();

    participant = new GameParticipant();
    participant.setGame(game);
    participant.setUser(user);
    participant.setDraftOrder(1);

    draft = new Draft(game);
    draft.setId(draftId);
    draft.setStatus(Draft.Status.ACTIVE);
    draft.setCurrentRound(1);
    draft.setCurrentPick(1);

    player =
        com.fortnite.pronos.domain.player.model.Player.restore(
            playerId,
            null,
            "player1",
            "Player-1",
            com.fortnite.pronos.domain.game.model.PlayerRegion.EU,
            "1-5",
            2026,
            false);

    domainDraft =
        com.fortnite.pronos.domain.draft.model.Draft.restore(
            draftId,
            gameId,
            com.fortnite.pronos.domain.draft.model.DraftStatus.ACTIVE,
            1,
            1,
            10,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null);
  }

  @Test
  @DisplayName("selectPlayer doit persister un DraftPick et avancer le draft")
  void selectPlayer_shouldPersistPick_andAdvanceDraft() {
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(domainGame));
    when(draftDomainRepository.findActiveByGameId(gameId)).thenReturn(Optional.of(domainDraft));
    when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));
    when(draftService.isUserTurn(draft, userId)).thenReturn(true);
    when(draftPickRepository.existsByDraftAndPlayer(
            eq(draft), any(com.fortnite.pronos.model.Player.class)))
        .thenReturn(false);
    when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
    when(gameParticipantRepository.findByUserIdAndGameId(userId, gameId))
        .thenReturn(Optional.of(participant));

    when(draftService.nextPick(any(Draft.class), anyInt()))
        .thenAnswer(
            invocation -> {
              Draft d = invocation.getArgument(0);
              d.setCurrentPick(d.getCurrentPick() + 1);
              return d;
            });

    when(draftPickRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    DraftPickDto dto = gameDraftService.selectPlayer(gameId, userId, playerId);

    assertThat(dto).isNotNull();
    assertThat(dto.getPlayerId()).isEqualTo(playerId);
    assertThat(dto.getPickerId()).isEqualTo(userId);
    assertThat(dto.getRound()).isEqualTo(1);
    assertThat(dto.getPick()).isEqualTo(1);

    ArgumentCaptor<DraftPick> pickCaptor = ArgumentCaptor.forClass(DraftPick.class);
    verify(draftPickRepository).save(pickCaptor.capture());
    DraftPick savedPick = pickCaptor.getValue();
    assertThat(savedPick.getParticipantLabel()).isEqualTo(userId.toString());
    assertThat(savedPick.getRegionSlot()).isEqualTo(DraftPick.DraftRegionSlot.EU);

    verify(draftService).nextPick(draft, domainGame.getMaxParticipants());
    assertThat(draft.getCurrentPick()).isEqualTo(2);
  }

  @Test
  @DisplayName("selectPlayer doit aussi mettre a jour le roster du participant")
  void selectPlayer_shouldUpdateParticipantRoster() {
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(domainGame));
    when(draftDomainRepository.findActiveByGameId(gameId)).thenReturn(Optional.of(domainDraft));
    when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));
    when(draftService.isUserTurn(draft, userId)).thenReturn(true);
    when(draftPickRepository.existsByDraftAndPlayer(
            eq(draft), any(com.fortnite.pronos.model.Player.class)))
        .thenReturn(false);
    when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
    when(gameParticipantRepository.findByUserIdAndGameId(userId, gameId))
        .thenReturn(Optional.of(participant));
    when(draftService.nextPick(any(Draft.class), anyInt())).thenReturn(draft);
    when(draftPickRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(gameParticipantRepository.save(any(GameParticipant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameDraftService.selectPlayer(gameId, userId, playerId);

    assertThat(participant.getSelectedPlayers()).hasSize(1);
    assertThat(participant.getSelectedPlayers().get(0).getId()).isEqualTo(playerId);
    assertThat(participant.getLastSelectionTime()).isNotNull();
    verify(gameParticipantRepository).save(participant);
  }

  @Test
  @DisplayName("selectPlayer doit echouer si aucun draft actif n'est trouve")
  void selectPlayer_shouldFail_whenNoActiveDraft() {
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(domainGame));
    when(draftDomainRepository.findActiveByGameId(gameId)).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> gameDraftService.selectPlayer(gameId, userId, playerId))
        .isInstanceOf(InvalidDraftStateException.class);
    verify(gameParticipantRepository, never()).save(any(GameParticipant.class));
  }

  @Test
  @DisplayName("startDraft doit echouer proprement si creatorId est manquant")
  void startDraft_shouldFail_whenCreatorIdIsMissing() {
    Game gameWithoutCreator =
        Game.restore(
            gameId,
            "Test Game",
            null,
            null,
            4,
            GameStatus.CREATING,
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            null,
            false,
            5,
            null,
            2026);

    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(gameWithoutCreator));

    assertThatThrownBy(() -> gameDraftService.startDraft(gameId, userId))
        .isInstanceOf(InvalidGameStateException.class)
        .hasMessage("Game creator is missing");
  }
}
