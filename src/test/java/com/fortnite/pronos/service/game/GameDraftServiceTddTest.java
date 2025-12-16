package com.fortnite.pronos.service.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.DraftPickRepository;
import com.fortnite.pronos.repository.DraftRepository;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.service.draft.DraftService;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameDraftService - TDD selectPlayer persistence")
class GameDraftServiceTddTest {

  @Mock private GameRepository gameRepository;
  @Mock private DraftRepository draftRepository;
  @Mock private DraftPickRepository draftPickRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private GameParticipantRepository gameParticipantRepository;
  @Mock private DraftService draftService;

  @InjectMocks private GameDraftService gameDraftService;

  private UUID gameId;
  private UUID userId;
  private UUID playerId;
  private Game game;
  private Draft draft;
  private Player player;
  private GameParticipant participant;

  @BeforeEach
  void setUp() {
    gameId = UUID.randomUUID();
    userId = UUID.randomUUID();
    playerId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);

    game = Game.builder().id(gameId).name("Test Game").maxParticipants(4).build();

    participant = new GameParticipant();
    participant.setGame(game);
    participant.setUser(user);
    participant.setDraftOrder(1);

    draft = new Draft(game);
    draft.setStatus(Draft.Status.ACTIVE);
    draft.setCurrentRound(1);
    draft.setCurrentPick(1);

    player =
        Player.builder()
            .id(playerId)
            .nickname("Player-1")
            .username("player1")
            .region(Player.Region.EU)
            .tranche("1-5")
            .build();
  }

  @Test
  @DisplayName("selectPlayer doit persister un DraftPick et avancer le draft")
  void selectPlayer_shouldPersistPick_andAdvanceDraft() {
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(draftRepository.findByGame(game)).thenReturn(Optional.of(draft));
    when(draftService.isUserTurn(draft, userId)).thenReturn(true);
    when(draftPickRepository.existsByDraftAndPlayer(draft, player)).thenReturn(false);
    when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
    when(gameParticipantRepository.findByUserIdAndGameId(userId, gameId))
        .thenReturn(Optional.of(participant));

    // Simuler l'avancement du draft
    when(draftService.nextPick(any(Draft.class), anyInt()))
        .thenAnswer(
            invocation -> {
              Draft d = invocation.getArgument(0);
              d.setCurrentPick(d.getCurrentPick() + 1);
              return d;
            });

    // Simuler la sauvegarde du pick en renvoyant l'entité passée
    when(draftPickRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    DraftPickDto dto = gameDraftService.selectPlayer(gameId, userId, playerId);

    assertThat(dto).isNotNull();
    assertThat(dto.getPlayerId()).isEqualTo(playerId);
    assertThat(dto.getPickerId()).isEqualTo(userId);
    assertThat(dto.getRound()).isEqualTo(1);
    assertThat(dto.getPick()).isEqualTo(1);

    // Vérifier que la sauvegarde a été faite et que le draft a avancé
    verify(draftPickRepository).save(any());
    verify(draftService).nextPick(draft, game.getMaxParticipants());
    assertThat(draft.getCurrentPick()).isEqualTo(2);
  }

  @Test
  @DisplayName("selectPlayer doit échouer si aucun draft actif n'est trouvé")
  void selectPlayer_shouldFail_whenNoActiveDraft() {
    when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));
    when(draftRepository.findByGame(game)).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> gameDraftService.selectPlayer(gameId, userId, playerId))
        .isInstanceOf(InvalidDraftStateException.class);
  }
}
