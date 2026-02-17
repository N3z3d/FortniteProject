package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.GameParticipant;
import com.fortnite.pronos.domain.game.model.GameStatus;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerRepositoryPort;
import com.fortnite.pronos.exception.DraftIncompleteException;
import com.fortnite.pronos.exception.InvalidGameStateException;
import com.fortnite.pronos.exception.UnauthorizedAccessException;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.service.draft.DraftService;

@ExtendWith(MockitoExtension.class)
class GameDraftServiceDomainMigrationTest {

  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftRepositoryPort draftRepository;
  @Mock private DraftPickRepositoryPort draftPickRepository;
  @Mock private PlayerRepositoryPort playerRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;
  @Mock private DraftService draftService;

  @InjectMocks private GameDraftService service;

  private UUID gameId;
  private UUID creatorId;
  private UUID draftId;
  private Game domainCreatingGame;
  private Game domainDraftingGame;
  private com.fortnite.pronos.domain.draft.model.Draft domainDraft;
  private com.fortnite.pronos.model.Game gameEntity;

  @BeforeEach
  void setUp() {
    gameId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    draftId = UUID.randomUUID();

    GameParticipant participant =
        GameParticipant.restore(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "other-user",
            1,
            LocalDateTime.now(),
            null,
            false,
            List.of());

    domainCreatingGame =
        Game.restore(
            gameId,
            "Game",
            null,
            creatorId,
            4,
            GameStatus.CREATING,
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            List.of(),
            List.of(participant),
            null,
            false,
            5,
            null,
            2026);

    domainDraftingGame =
        Game.restore(
            gameId,
            "Game",
            null,
            creatorId,
            4,
            GameStatus.DRAFTING,
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            List.of(),
            List.of(participant),
            null,
            false,
            5,
            null,
            2026);

    domainDraft =
        com.fortnite.pronos.domain.draft.model.Draft.restore(
            draftId,
            gameId,
            com.fortnite.pronos.domain.draft.model.DraftStatus.ACTIVE,
            1,
            1,
            8,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null);

    User creator = new User();
    creator.setId(creatorId);
    gameEntity =
        com.fortnite.pronos.model.Game.builder().id(gameId).name("Game").creator(creator).build();
  }

  @Test
  void startDraftSavesDomainStatusAndStartsDraft() {
    Draft draft = new Draft(gameEntity);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(domainCreatingGame));
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(draftService.startDraftForGame(gameId)).thenReturn(draft);

    var result = service.startDraft(gameId, creatorId);

    assertThat(result).isNotNull();
    verify(gameDomainRepository).save(domainCreatingGame);
    verify(draftService).startDraftForGame(gameId);
  }

  @Test
  void startDraftThrowsWhenUserIsNotCreator() {
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(domainCreatingGame));

    assertThatThrownBy(() -> service.startDraft(gameId, UUID.randomUUID()))
        .isInstanceOf(UnauthorizedAccessException.class);

    verify(gameDomainRepository, never()).save(any(Game.class));
    verify(draftService, never()).startDraft(any(com.fortnite.pronos.model.Game.class));
  }

  @Test
  void startDraftThrowsWhenGameCannotStartDraft() {
    Game notEnoughParticipants =
        Game.restore(
            gameId,
            "Game",
            null,
            creatorId,
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
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(notEnoughParticipants));

    assertThatThrownBy(() -> service.startDraft(gameId, creatorId))
        .isInstanceOf(InvalidGameStateException.class)
        .hasMessageContaining("Not enough participants");

    verify(draftService, never()).startDraft(any(com.fortnite.pronos.model.Game.class));
  }

  @Test
  void finishDraftCompletesDomainGameAndSaves() {
    Draft draft = new Draft(gameEntity);
    draft.setId(draftId);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(domainDraftingGame));
    when(draftDomainRepository.findActiveByGameId(gameId)).thenReturn(Optional.of(domainDraft));
    when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));
    when(draftService.isDraftComplete(draft)).thenReturn(true);
    when(gameDomainRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.finishDraft(gameId, creatorId);

    assertThat(result).isNotNull();
    verify(draftService).finishDraft(draft);
    verify(gameDomainRepository).save(domainDraftingGame);
  }

  @Test
  void finishDraftThrowsWhenDraftIncomplete() {
    Draft draft = new Draft(gameEntity);
    draft.setId(draftId);
    when(gameDomainRepository.findById(gameId)).thenReturn(Optional.of(domainDraftingGame));
    when(draftDomainRepository.findActiveByGameId(gameId)).thenReturn(Optional.of(domainDraft));
    when(draftRepository.findById(draftId)).thenReturn(Optional.of(draft));
    when(draftService.isDraftComplete(draft)).thenReturn(false);

    assertThatThrownBy(() -> service.finishDraft(gameId, creatorId))
        .isInstanceOf(DraftIncompleteException.class);

    verify(gameDomainRepository, never()).save(any(Game.class));
  }
}
