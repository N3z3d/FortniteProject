package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.fortnite.pronos.domain.draft.model.DraftSwapAuditEntry;
import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftSwapAuditRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.dto.SwapSoloResponse;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidSwapException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SwapSoloService")
class SwapSoloServiceTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private DraftRepositoryPort draftRepository;
  @Mock private DraftPickRepositoryPort draftPickRepository;
  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;
  @Mock private DraftSwapAuditRepositoryPort auditRepository;

  // JPA entity mocks
  @Mock private com.fortnite.pronos.model.Draft legacyDraft;
  @Mock private com.fortnite.pronos.model.GameParticipant participant;

  // Domain model mocks
  @Mock private com.fortnite.pronos.domain.draft.model.Draft domainDraft;
  @Mock private com.fortnite.pronos.domain.player.model.Player playerOut;
  @Mock private com.fortnite.pronos.domain.player.model.Player playerIn;

  private SwapSoloService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PARTICIPANT_ID = UUID.randomUUID();
  private static final UUID PLAYER_OUT_ID = UUID.randomUUID();
  private static final UUID PLAYER_IN_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new SwapSoloService(
            gameDomainRepository,
            draftDomainRepository,
            draftRepository,
            draftPickRepository,
            playerRepository,
            gameParticipantRepository,
            auditRepository);
  }

  private Game buildDomainGame() {
    return new Game("TestGame", UUID.randomUUID(), 10, DraftMode.SNAKE, 5, 3, true);
  }

  private void stubGameAndDraft() {
    when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildDomainGame()));
    when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(domainDraft));
    when(domainDraft.getId()).thenReturn(DRAFT_ID);
    when(draftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(legacyDraft));
    when(legacyDraft.getId()).thenReturn(DRAFT_ID);
  }

  private void stubParticipant() {
    when(gameParticipantRepository.findByUserIdAndGameId(USER_ID, GAME_ID))
        .thenReturn(Optional.of(participant));
    when(participant.getId()).thenReturn(PARTICIPANT_ID);
  }

  private void stubPlayers(String trancheOut, String trancheIn) {
    when(playerRepository.findById(PLAYER_OUT_ID)).thenReturn(Optional.of(playerOut));
    when(playerRepository.findById(PLAYER_IN_ID)).thenReturn(Optional.of(playerIn));
    when(playerOut.getRegion()).thenReturn(PlayerRegion.EU);
    when(playerOut.getTranche()).thenReturn(trancheOut);
    when(playerIn.getRegion()).thenReturn(PlayerRegion.EU);
    when(playerIn.getTranche()).thenReturn(trancheIn);
    // toLegacyPlayer fields — only consumed in the happy path
    lenient().when(playerIn.getId()).thenReturn(PLAYER_IN_ID);
    lenient().when(playerIn.getFortniteId()).thenReturn("fid-in");
    lenient().when(playerIn.getUsername()).thenReturn("playerIn");
    lenient().when(playerIn.getNickname()).thenReturn("PlayerIn");
    lenient().when(playerIn.getCurrentSeason()).thenReturn(0);
    lenient().when(playerIn.isLocked()).thenReturn(false);
  }

  @Nested
  @DisplayName("executeSoloSwap")
  class ExecuteSoloSwap {

    @Test
    void whenValid_executesSwapAndReturnsResponse() {
      stubGameAndDraft();
      stubParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PARTICIPANT_ID, PLAYER_OUT_ID))
          .thenReturn(true);
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_OUT_ID));
      stubPlayers("2", "4");
      when(auditRepository.save(any(DraftSwapAuditEntry.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      SwapSoloResponse response =
          service.executeSoloSwap(GAME_ID, USER_ID, PLAYER_OUT_ID, PLAYER_IN_ID);

      assertThat(response).isNotNull();
      assertThat(response.draftId()).isEqualTo(DRAFT_ID);
      assertThat(response.participantId()).isEqualTo(PARTICIPANT_ID);
      assertThat(response.playerOutId()).isEqualTo(PLAYER_OUT_ID);
      assertThat(response.playerInId()).isEqualTo(PLAYER_IN_ID);
      verify(draftPickRepository).deleteByDraftIdAndPlayerId(DRAFT_ID, PLAYER_OUT_ID);
      verify(draftPickRepository).save(any(com.fortnite.pronos.model.DraftPick.class));
      verify(auditRepository).save(any(DraftSwapAuditEntry.class));
    }

    @Test
    void whenPlayerOutNotInTeam_throwsInvalidSwapException() {
      stubGameAndDraft();
      stubParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PARTICIPANT_ID, PLAYER_OUT_ID))
          .thenReturn(false);

      assertThatThrownBy(
              () -> service.executeSoloSwap(GAME_ID, USER_ID, PLAYER_OUT_ID, PLAYER_IN_ID))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("not in your team");

      verify(draftPickRepository, never()).save(any());
    }

    @Test
    void whenPlayerInAlreadyPicked_throwsInvalidSwapException() {
      stubGameAndDraft();
      stubParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PARTICIPANT_ID, PLAYER_OUT_ID))
          .thenReturn(true);
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_OUT_ID, PLAYER_IN_ID));

      assertThatThrownBy(
              () -> service.executeSoloSwap(GAME_ID, USER_ID, PLAYER_OUT_ID, PLAYER_IN_ID))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("already selected");

      verify(draftPickRepository, never()).save(any());
    }

    @Test
    void whenDifferentRegion_throwsInvalidSwapException() {
      stubGameAndDraft();
      stubParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PARTICIPANT_ID, PLAYER_OUT_ID))
          .thenReturn(true);
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_OUT_ID));
      when(playerRepository.findById(PLAYER_OUT_ID)).thenReturn(Optional.of(playerOut));
      when(playerRepository.findById(PLAYER_IN_ID)).thenReturn(Optional.of(playerIn));
      when(playerOut.getRegion()).thenReturn(PlayerRegion.EU);
      when(playerIn.getRegion()).thenReturn(PlayerRegion.NA);

      assertThatThrownBy(
              () -> service.executeSoloSwap(GAME_ID, USER_ID, PLAYER_OUT_ID, PLAYER_IN_ID))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("same region");

      verify(draftPickRepository, never()).save(any());
    }

    @Test
    void whenRankNotStrictlyWorse_trancheEqual_throwsInvalidSwapException() {
      stubGameAndDraft();
      stubParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PARTICIPANT_ID, PLAYER_OUT_ID))
          .thenReturn(true);
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_OUT_ID));
      stubPlayers("3", "3");

      assertThatThrownBy(
              () -> service.executeSoloSwap(GAME_ID, USER_ID, PLAYER_OUT_ID, PLAYER_IN_ID))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("strictly worse rank");

      verify(draftPickRepository, never()).save(any());
    }

    @Test
    void whenRankBetter_trancheInLowerThanOut_throwsInvalidSwapException() {
      stubGameAndDraft();
      stubParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PARTICIPANT_ID, PLAYER_OUT_ID))
          .thenReturn(true);
      when(draftPickRepository.findPickedPlayerIdsByDraftId(DRAFT_ID))
          .thenReturn(List.of(PLAYER_OUT_ID));
      stubPlayers("3", "1");

      assertThatThrownBy(
              () -> service.executeSoloSwap(GAME_ID, USER_ID, PLAYER_OUT_ID, PLAYER_IN_ID))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("strictly worse rank");

      verify(draftPickRepository, never()).save(any());
    }

    @Test
    void whenGameNotFound_throwsGameNotFoundException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.executeSoloSwap(GAME_ID, USER_ID, PLAYER_OUT_ID, PLAYER_IN_ID))
          .isInstanceOf(GameNotFoundException.class)
          .hasMessageContaining(GAME_ID.toString());
    }

    @Test
    void whenNoDraftActive_throwsInvalidDraftStateException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildDomainGame()));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> service.executeSoloSwap(GAME_ID, USER_ID, PLAYER_OUT_ID, PLAYER_IN_ID))
          .isInstanceOf(InvalidDraftStateException.class)
          .hasMessageContaining("No active draft");
    }
  }
}
