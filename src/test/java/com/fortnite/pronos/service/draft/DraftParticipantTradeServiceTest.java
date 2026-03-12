package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.draft.model.DraftParticipantTrade;
import com.fortnite.pronos.domain.draft.model.DraftParticipantTradeStatus;
import com.fortnite.pronos.domain.game.model.DraftMode;
import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftParticipantTradeRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.dto.DraftTradeProposalResponse;
import com.fortnite.pronos.exception.GameNotFoundException;
import com.fortnite.pronos.exception.InvalidDraftStateException;
import com.fortnite.pronos.exception.InvalidSwapException;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftParticipantTradeService")
class DraftParticipantTradeServiceTest {

  @Mock private GameDomainRepositoryPort gameDomainRepository;
  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private DraftRepositoryPort draftRepository;
  @Mock private DraftPickRepositoryPort draftPickRepository;
  @Mock private GameParticipantRepositoryPort gameParticipantRepository;
  @Mock private DraftParticipantTradeRepositoryPort tradeRepository;

  // JPA entity mocks
  @Mock private com.fortnite.pronos.model.Draft legacyDraft;
  @Mock private com.fortnite.pronos.model.GameParticipant proposerParticipant;
  @Mock private com.fortnite.pronos.model.GameParticipant targetParticipant;
  @Mock private com.fortnite.pronos.model.DraftPick proposerPick;
  @Mock private com.fortnite.pronos.model.DraftPick targetPick;
  @Mock private com.fortnite.pronos.model.Player proposerPlayer;
  @Mock private com.fortnite.pronos.model.Player targetPlayer;

  // Domain mocks
  @Mock private com.fortnite.pronos.domain.draft.model.Draft domainDraft;

  private DraftParticipantTradeService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID PROPOSER_USER_ID = UUID.randomUUID();
  private static final UUID PROPOSER_PARTICIPANT_ID = UUID.randomUUID();
  private static final UUID TARGET_PARTICIPANT_ID = UUID.randomUUID();
  private static final UUID PLAYER_FROM_PROPOSER_ID = UUID.randomUUID();
  private static final UUID PLAYER_FROM_TARGET_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new DraftParticipantTradeService(
            gameDomainRepository,
            draftDomainRepository,
            draftRepository,
            draftPickRepository,
            gameParticipantRepository,
            tradeRepository);
  }

  private Game buildDomainGame() {
    return new Game("TestGame", UUID.randomUUID(), 10, DraftMode.SNAKE, 5, 3, true);
  }

  private void stubGameAndDraft() {
    when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildDomainGame()));
    when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(domainDraft));
    when(domainDraft.getId()).thenReturn(DRAFT_ID);
    when(draftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(legacyDraft));
    lenient().when(legacyDraft.getId()).thenReturn(DRAFT_ID);
  }

  private void stubProposerParticipant() {
    when(gameParticipantRepository.findByUserIdAndGameId(PROPOSER_USER_ID, GAME_ID))
        .thenReturn(Optional.of(proposerParticipant));
    when(proposerParticipant.getId()).thenReturn(PROPOSER_PARTICIPANT_ID);
  }

  private void stubTargetParticipant() {
    when(gameParticipantRepository.findById(TARGET_PARTICIPANT_ID))
        .thenReturn(Optional.of(targetParticipant));
    lenient().when(targetParticipant.getId()).thenReturn(TARGET_PARTICIPANT_ID);
  }

  private DraftParticipantTrade buildPendingTrade() {
    return DraftParticipantTrade.restore(
        UUID.randomUUID(),
        DRAFT_ID,
        PROPOSER_PARTICIPANT_ID,
        TARGET_PARTICIPANT_ID,
        PLAYER_FROM_PROPOSER_ID,
        PLAYER_FROM_TARGET_ID,
        DraftParticipantTradeStatus.PENDING,
        java.time.LocalDateTime.now(),
        null);
  }

  @Nested
  @DisplayName("proposeTrade")
  class ProposeTrade {

    @Test
    void whenValid_createsPendingTradeAndReturnsResponse() {
      stubGameAndDraft();
      stubProposerParticipant();
      stubTargetParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PROPOSER_PARTICIPANT_ID, PLAYER_FROM_PROPOSER_ID))
          .thenReturn(true);
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, TARGET_PARTICIPANT_ID, PLAYER_FROM_TARGET_ID))
          .thenReturn(true);
      DraftParticipantTrade savedTrade = buildPendingTrade();
      when(tradeRepository.save(any(DraftParticipantTrade.class))).thenReturn(savedTrade);

      DraftTradeProposalResponse response =
          service.proposeTrade(
              GAME_ID,
              PROPOSER_USER_ID,
              TARGET_PARTICIPANT_ID,
              PLAYER_FROM_PROPOSER_ID,
              PLAYER_FROM_TARGET_ID);

      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo("PENDING");
      assertThat(response.draftId()).isEqualTo(DRAFT_ID);
      verify(tradeRepository).save(any(DraftParticipantTrade.class));
    }

    @Test
    void whenOfferedPlayerNotInProposerTeam_throwsInvalidSwapException() {
      stubGameAndDraft();
      stubProposerParticipant();
      stubTargetParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PROPOSER_PARTICIPANT_ID, PLAYER_FROM_PROPOSER_ID))
          .thenReturn(false);

      assertThatThrownBy(
              () ->
                  service.proposeTrade(
                      GAME_ID,
                      PROPOSER_USER_ID,
                      TARGET_PARTICIPANT_ID,
                      PLAYER_FROM_PROPOSER_ID,
                      PLAYER_FROM_TARGET_ID))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("not in your team");

      verify(tradeRepository, never()).save(any());
    }

    @Test
    void whenRequestedPlayerNotInTargetTeam_throwsInvalidSwapException() {
      stubGameAndDraft();
      stubProposerParticipant();
      stubTargetParticipant();
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PROPOSER_PARTICIPANT_ID, PLAYER_FROM_PROPOSER_ID))
          .thenReturn(true);
      when(draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, TARGET_PARTICIPANT_ID, PLAYER_FROM_TARGET_ID))
          .thenReturn(false);

      assertThatThrownBy(
              () ->
                  service.proposeTrade(
                      GAME_ID,
                      PROPOSER_USER_ID,
                      TARGET_PARTICIPANT_ID,
                      PLAYER_FROM_PROPOSER_ID,
                      PLAYER_FROM_TARGET_ID))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("not in the target participant's team");

      verify(tradeRepository, never()).save(any());
    }

    @Test
    void whenGameNotFound_throwsGameNotFoundException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.proposeTrade(
                      GAME_ID,
                      PROPOSER_USER_ID,
                      TARGET_PARTICIPANT_ID,
                      PLAYER_FROM_PROPOSER_ID,
                      PLAYER_FROM_TARGET_ID))
          .isInstanceOf(GameNotFoundException.class)
          .hasMessageContaining(GAME_ID.toString());
    }

    @Test
    void whenNoDraftActive_throwsInvalidDraftStateException() {
      when(gameDomainRepository.findById(GAME_ID)).thenReturn(Optional.of(buildDomainGame()));
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () ->
                  service.proposeTrade(
                      GAME_ID,
                      PROPOSER_USER_ID,
                      TARGET_PARTICIPANT_ID,
                      PLAYER_FROM_PROPOSER_ID,
                      PLAYER_FROM_TARGET_ID))
          .isInstanceOf(InvalidDraftStateException.class)
          .hasMessageContaining("No active draft");
    }
  }

  @Nested
  @DisplayName("acceptTrade")
  class AcceptTrade {

    @Test
    void whenValid_swapsPicksAndReturnsAcceptedResponse() {
      UUID tradeId = UUID.randomUUID();
      DraftParticipantTrade pendingTrade = buildPendingTrade();
      when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(pendingTrade));

      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(domainDraft));
      when(domainDraft.getId()).thenReturn(DRAFT_ID);
      when(draftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(legacyDraft));
      when(legacyDraft.getId()).thenReturn(DRAFT_ID);

      when(gameParticipantRepository.findByUserIdAndGameId(TARGET_PARTICIPANT_ID, GAME_ID))
          .thenReturn(Optional.of(targetParticipant));
      when(targetParticipant.getId()).thenReturn(TARGET_PARTICIPANT_ID);

      when(gameParticipantRepository.findById(PROPOSER_PARTICIPANT_ID))
          .thenReturn(Optional.of(proposerParticipant));

      when(draftPickRepository.findByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, PROPOSER_PARTICIPANT_ID, PLAYER_FROM_PROPOSER_ID))
          .thenReturn(Optional.of(proposerPick));
      when(draftPickRepository.findByDraftIdAndParticipantIdAndPlayerId(
              DRAFT_ID, TARGET_PARTICIPANT_ID, PLAYER_FROM_TARGET_ID))
          .thenReturn(Optional.of(targetPick));
      when(proposerPick.getPlayer()).thenReturn(proposerPlayer);
      when(targetPick.getPlayer()).thenReturn(targetPlayer);

      DraftParticipantTrade acceptedTrade =
          DraftParticipantTrade.restore(
              pendingTrade.getId(),
              DRAFT_ID,
              PROPOSER_PARTICIPANT_ID,
              TARGET_PARTICIPANT_ID,
              PLAYER_FROM_PROPOSER_ID,
              PLAYER_FROM_TARGET_ID,
              DraftParticipantTradeStatus.ACCEPTED,
              pendingTrade.getProposedAt(),
              java.time.LocalDateTime.now());
      when(tradeRepository.save(any(DraftParticipantTrade.class))).thenReturn(acceptedTrade);

      DraftTradeProposalResponse response =
          service.acceptTrade(GAME_ID, TARGET_PARTICIPANT_ID, tradeId);

      assertThat(response.status()).isEqualTo("ACCEPTED");
      verify(draftPickRepository).deleteByDraftIdAndPlayerId(DRAFT_ID, PLAYER_FROM_PROPOSER_ID);
      verify(draftPickRepository).deleteByDraftIdAndPlayerId(DRAFT_ID, PLAYER_FROM_TARGET_ID);
      verify(draftPickRepository, times(2)).save(any(com.fortnite.pronos.model.DraftPick.class));
    }

    @Test
    void whenCallerIsNotTargetParticipant_throwsInvalidSwapException() {
      UUID tradeId = UUID.randomUUID();
      UUID differentUserId = UUID.randomUUID();
      UUID differentParticipantId = UUID.randomUUID();
      DraftParticipantTrade pendingTrade = buildPendingTrade();
      when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(pendingTrade));

      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(domainDraft));
      when(domainDraft.getId()).thenReturn(DRAFT_ID);
      when(draftRepository.findById(DRAFT_ID)).thenReturn(Optional.of(legacyDraft));
      lenient().when(legacyDraft.getId()).thenReturn(DRAFT_ID);

      com.fortnite.pronos.model.GameParticipant differentParticipant =
          org.mockito.Mockito.mock(com.fortnite.pronos.model.GameParticipant.class);
      when(gameParticipantRepository.findByUserIdAndGameId(differentUserId, GAME_ID))
          .thenReturn(Optional.of(differentParticipant));
      when(differentParticipant.getId()).thenReturn(differentParticipantId);

      assertThatThrownBy(() -> service.acceptTrade(GAME_ID, differentUserId, tradeId))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("target participant");

      verify(draftPickRepository, never()).deleteByDraftIdAndPlayerId(any(), any());
    }

    @Test
    void whenTradeNotPending_throwsInvalidSwapException() {
      UUID tradeId = UUID.randomUUID();
      DraftParticipantTrade alreadyAccepted =
          DraftParticipantTrade.restore(
              UUID.randomUUID(),
              DRAFT_ID,
              PROPOSER_PARTICIPANT_ID,
              TARGET_PARTICIPANT_ID,
              PLAYER_FROM_PROPOSER_ID,
              PLAYER_FROM_TARGET_ID,
              DraftParticipantTradeStatus.ACCEPTED,
              java.time.LocalDateTime.now(),
              java.time.LocalDateTime.now());
      when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(alreadyAccepted));

      assertThatThrownBy(() -> service.acceptTrade(GAME_ID, TARGET_PARTICIPANT_ID, tradeId))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("not pending");

      verify(draftPickRepository, never()).deleteByDraftIdAndPlayerId(any(), any());
    }
  }

  @Nested
  @DisplayName("rejectTrade")
  class RejectTrade {

    @Test
    void whenValid_rejectsTradeWithoutModifyingPicks() {
      UUID tradeId = UUID.randomUUID();
      DraftParticipantTrade pendingTrade = buildPendingTrade();
      when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(pendingTrade));
      when(gameParticipantRepository.findByUserIdAndGameId(TARGET_PARTICIPANT_ID, GAME_ID))
          .thenReturn(Optional.of(targetParticipant));
      when(targetParticipant.getId()).thenReturn(TARGET_PARTICIPANT_ID);

      DraftParticipantTrade rejectedTrade =
          DraftParticipantTrade.restore(
              pendingTrade.getId(),
              DRAFT_ID,
              PROPOSER_PARTICIPANT_ID,
              TARGET_PARTICIPANT_ID,
              PLAYER_FROM_PROPOSER_ID,
              PLAYER_FROM_TARGET_ID,
              DraftParticipantTradeStatus.REJECTED,
              pendingTrade.getProposedAt(),
              java.time.LocalDateTime.now());
      when(tradeRepository.save(any(DraftParticipantTrade.class))).thenReturn(rejectedTrade);

      DraftTradeProposalResponse response =
          service.rejectTrade(GAME_ID, TARGET_PARTICIPANT_ID, tradeId);

      assertThat(response.status()).isEqualTo("REJECTED");
      verify(draftPickRepository, never()).deleteByDraftIdAndPlayerId(any(), any());
      verify(draftPickRepository, never()).save(any());
    }

    @Test
    void whenCallerIsNotTargetParticipant_throwsInvalidSwapException() {
      UUID tradeId = UUID.randomUUID();
      UUID differentUserId = UUID.randomUUID();
      UUID differentParticipantId = UUID.randomUUID();
      DraftParticipantTrade pendingTrade = buildPendingTrade();
      when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(pendingTrade));

      com.fortnite.pronos.model.GameParticipant differentParticipant =
          org.mockito.Mockito.mock(com.fortnite.pronos.model.GameParticipant.class);
      when(gameParticipantRepository.findByUserIdAndGameId(differentUserId, GAME_ID))
          .thenReturn(Optional.of(differentParticipant));
      when(differentParticipant.getId()).thenReturn(differentParticipantId);

      assertThatThrownBy(() -> service.rejectTrade(GAME_ID, differentUserId, tradeId))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("target participant");
    }

    @Test
    void whenTradeNotPending_throwsInvalidSwapException() {
      UUID tradeId = UUID.randomUUID();
      DraftParticipantTrade alreadyRejected =
          DraftParticipantTrade.restore(
              UUID.randomUUID(),
              DRAFT_ID,
              PROPOSER_PARTICIPANT_ID,
              TARGET_PARTICIPANT_ID,
              PLAYER_FROM_PROPOSER_ID,
              PLAYER_FROM_TARGET_ID,
              DraftParticipantTradeStatus.REJECTED,
              java.time.LocalDateTime.now(),
              java.time.LocalDateTime.now());
      when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(alreadyRejected));

      assertThatThrownBy(() -> service.rejectTrade(GAME_ID, TARGET_PARTICIPANT_ID, tradeId))
          .isInstanceOf(InvalidSwapException.class)
          .hasMessageContaining("not pending");

      verify(draftPickRepository, never()).deleteByDraftIdAndPlayerId(any(), any());
    }
  }
}
