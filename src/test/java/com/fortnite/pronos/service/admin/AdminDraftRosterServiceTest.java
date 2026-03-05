package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.DraftPickDto;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.PlayerAlreadySelectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDraftRosterService")
class AdminDraftRosterServiceTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private DraftRepositoryPort draftRepository;
  @Mock private DraftPickRepositoryPort draftPickRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;

  // JPA entity mocks for DTO assembly chain
  @Mock private com.fortnite.pronos.model.Draft legacyDraft;
  @Mock private com.fortnite.pronos.model.GameParticipant participant;
  @Mock private com.fortnite.pronos.model.DraftPick savedPick;
  @Mock private com.fortnite.pronos.model.Player legacyPlayer;
  @Mock private com.fortnite.pronos.model.Game legacyGame;
  @Mock private com.fortnite.pronos.model.User mockUser;

  // Domain model mocks
  @Mock private com.fortnite.pronos.domain.draft.model.Draft domainDraft;
  @Mock private com.fortnite.pronos.domain.player.model.Player domainPlayer;

  private AdminDraftRosterService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID PLAYER_ID = UUID.randomUUID();
  private static final UUID PARTICIPANT_USER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new AdminDraftRosterService(
            gameDomainRepository,
            draftDomainRepository,
            draftRepository,
            draftPickRepository,
            playerRepository,
            gameParticipantRepository);
  }

  private Game buildDomainGame() {
    return new Game("TestGame", UUID.randomUUID(), 10, DraftMode.SNAKE, 5, 3, true);
  }

  private void stubDraftLookup() {
    when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(domainDraft));
    when(domainDraft.getId()).thenReturn(DRAFT_ID);
    when(draftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(legacyDraft));
    lenient().when(legacyDraft.getId()).thenReturn(DRAFT_ID);
  }

  private void stubPlayerLookup() {
    when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(domainPlayer));
    when(domainPlayer.getId()).thenReturn(PLAYER_ID);
    when(domainPlayer.getFortniteId()).thenReturn("fortnite-id");
    when(domainPlayer.getUsername()).thenReturn("testUser");
    when(domainPlayer.getNickname()).thenReturn("TestPlayer");
    when(domainPlayer.getRegion()).thenReturn(null);
    when(domainPlayer.getTranche()).thenReturn("1");
    when(domainPlayer.getCurrentSeason()).thenReturn(0);
    when(domainPlayer.isLocked()).thenReturn(false);
  }

  private void stubSavedPickForDto() {
    when(savedPick.getId()).thenReturn(UUID.randomUUID());
    when(savedPick.getDraft()).thenReturn(legacyDraft);
    when(savedPick.getPlayer()).thenReturn(legacyPlayer);
    when(savedPick.getParticipant()).thenReturn(participant);
    when(savedPick.getRound()).thenReturn(0);
    when(savedPick.getPickNumber()).thenReturn(0);
    when(savedPick.getSelectionTime()).thenReturn(LocalDateTime.now());
    when(legacyDraft.getGameParticipantCount()).thenReturn(0);
    when(legacyPlayer.getId()).thenReturn(PLAYER_ID);
    when(legacyPlayer.getNickname()).thenReturn("TestPlayer");
    when(legacyPlayer.getRegion()).thenReturn(null);
    when(participant.getUserId()).thenReturn(PARTICIPANT_USER_ID);
    when(participant.getUsername()).thenReturn("testUser");
  }

  @Nested
  @DisplayName("AssignPlayer")
  class AssignPlayer {

    @Test
    void assignPlayer_whenValid_createsDraftPick() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildDomainGame()));
      stubDraftLookup();
      when(gameParticipantRepository.findByUserIdAndGameId(PARTICIPANT_USER_ID, GAME_ID))
          .thenReturn(Optional.of(participant));
      stubPlayerLookup();
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(draftPickRepository.save(any())).thenReturn(savedPick);
      stubSavedPickForDto();

      DraftPickDto result = service.assignPlayer(GAME_ID, PARTICIPANT_USER_ID, PLAYER_ID);

      assertThat(result).isNotNull();
      verify(draftPickRepository).save(any(com.fortnite.pronos.model.DraftPick.class));
    }

    @Test
    void assignPlayer_whenPlayerAlreadyPicked_throwsPlayerAlreadySelectedException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildDomainGame()));
      stubDraftLookup();
      when(gameParticipantRepository.findByUserIdAndGameId(PARTICIPANT_USER_ID, GAME_ID))
          .thenReturn(Optional.of(participant));
      stubPlayerLookup();
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_ID));

      assertThatThrownBy(() -> service.assignPlayer(GAME_ID, PARTICIPANT_USER_ID, PLAYER_ID))
          .isInstanceOf(PlayerAlreadySelectedException.class)
          .hasMessageContaining("already selected in this draft");

      verify(draftPickRepository, never()).save(any());
    }

    @Test
    void assignPlayer_whenGameNotFound_throwsGameNotFoundException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.assignPlayer(GAME_ID, PARTICIPANT_USER_ID, PLAYER_ID))
          .isInstanceOf(GameNotFoundException.class)
          .hasMessageContaining(GAME_ID.toString());
    }

    @Test
    void assignPlayer_whenNoDraftActive_throwsInvalidDraftStateException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildDomainGame()));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.assignPlayer(GAME_ID, PARTICIPANT_USER_ID, PLAYER_ID))
          .isInstanceOf(InvalidDraftStateException.class)
          .hasMessageContaining("No active draft");
    }

    @Test
    void assignPlayer_whenParticipantNotFound_throwsGameNotFoundException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildDomainGame()));
      stubDraftLookup();
      when(gameParticipantRepository.findByUserIdAndGameId(PARTICIPANT_USER_ID, GAME_ID))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.assignPlayer(GAME_ID, PARTICIPANT_USER_ID, PLAYER_ID))
          .isInstanceOf(GameNotFoundException.class)
          .hasMessageContaining("Participant not found");
    }
  }

  @Nested
  @DisplayName("RemovePlayer")
  class RemovePlayer {

    @Test
    void removePlayer_whenValid_deletesFromDraft() {
      stubDraftLookup();
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_ID));

      service.removePlayer(GAME_ID, PLAYER_ID);

      verify(draftPickRepository).deleteByDraftIdAndPlayerId(DRAFT_ID, PLAYER_ID);
    }

    @Test
    void removePlayer_whenPlayerNotInDraft_throwsGameNotFoundException() {
      stubDraftLookup();
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID)).thenReturn(List.of());

      assertThatThrownBy(() -> service.removePlayer(GAME_ID, PLAYER_ID))
          .isInstanceOf(GameNotFoundException.class)
          .hasMessageContaining("Player not found in draft");
    }
  }
}
