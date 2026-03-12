package com.fortnite.pronos.service.draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.fortnite.pronos.domain.draft.model.DraftParticipantTrade;
import com.fortnite.pronos.domain.draft.model.DraftParticipantTradeStatus;
import com.fortnite.pronos.domain.draft.model.DraftSwapAuditEntry;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftParticipantTradeRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftSwapAuditRepositoryPort;
import com.fortnite.pronos.dto.DraftAuditEntryResponse;
import com.fortnite.pronos.exception.InvalidDraftStateException;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftAuditService")
class DraftAuditServiceTest {

  @Mock private DraftDomainRepositoryPort draftDomainRepository;
  @Mock private DraftSwapAuditRepositoryPort swapAuditRepository;
  @Mock private DraftParticipantTradeRepositoryPort tradeRepository;
  @Mock private com.fortnite.pronos.domain.draft.model.Draft domainDraft;

  private DraftAuditService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID DRAFT_ID = UUID.randomUUID();
  private static final UUID PARTICIPANT_ID = UUID.randomUUID();
  private static final UUID PROPOSER_ID = UUID.randomUUID();
  private static final UUID TARGET_ID = UUID.randomUUID();
  private static final UUID PLAYER_A = UUID.randomUUID();
  private static final UUID PLAYER_B = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new DraftAuditService(draftDomainRepository, swapAuditRepository, tradeRepository);
  }

  private void stubActiveDraft() {
    when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.of(domainDraft));
    when(domainDraft.getId()).thenReturn(DRAFT_ID);
  }

  private DraftSwapAuditEntry buildSwap(LocalDateTime at) {
    return DraftSwapAuditEntry.restore(
        UUID.randomUUID(), DRAFT_ID, PARTICIPANT_ID, PLAYER_A, PLAYER_B, at);
  }

  private DraftParticipantTrade buildTrade(
      DraftParticipantTradeStatus status, LocalDateTime proposed, LocalDateTime resolved) {
    return DraftParticipantTrade.restore(
        UUID.randomUUID(),
        DRAFT_ID,
        PROPOSER_ID,
        TARGET_ID,
        PLAYER_A,
        PLAYER_B,
        status,
        proposed,
        resolved);
  }

  @Nested
  @DisplayName("getAuditForGame")
  class GetAuditForGame {

    @Test
    void whenNoDraftActive_throwsInvalidDraftStateException() {
      when(draftDomainRepository.findActiveByGameId(GAME_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getAuditForGame(GAME_ID))
          .isInstanceOf(InvalidDraftStateException.class)
          .hasMessageContaining("No active draft");
    }

    @Test
    void whenNoEventsExist_returnsEmptyList() {
      stubActiveDraft();
      when(swapAuditRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(tradeRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of());

      List<DraftAuditEntryResponse> result = service.getAuditForGame(GAME_ID);

      assertThat(result).isEmpty();
    }

    @Test
    void whenSwapsAndTradesExist_returnsMergedListSortedByDateDesc() {
      stubActiveDraft();
      LocalDateTime older = LocalDateTime.now().minusHours(2);
      LocalDateTime recent = LocalDateTime.now().minusHours(1);

      DraftSwapAuditEntry swap = buildSwap(recent);
      DraftParticipantTrade pendingTrade =
          buildTrade(DraftParticipantTradeStatus.PENDING, older, null);

      when(swapAuditRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of(swap));
      when(tradeRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of(pendingTrade));

      List<DraftAuditEntryResponse> result = service.getAuditForGame(GAME_ID);

      // swap (recent) before trade_proposed (older)
      assertThat(result).hasSize(2);
      assertThat(result.get(0).type()).isEqualTo("SWAP_SOLO");
      assertThat(result.get(1).type()).isEqualTo("TRADE_PROPOSED");
    }

    @Test
    void whenTradeIsPending_returnsOnlyProposedEntry() {
      stubActiveDraft();
      DraftParticipantTrade trade =
          buildTrade(DraftParticipantTradeStatus.PENDING, LocalDateTime.now(), null);
      when(swapAuditRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(tradeRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of(trade));

      List<DraftAuditEntryResponse> result = service.getAuditForGame(GAME_ID);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).type()).isEqualTo("TRADE_PROPOSED");
      assertThat(result.get(0).proposerParticipantId()).isEqualTo(PROPOSER_ID);
      assertThat(result.get(0).targetParticipantId()).isEqualTo(TARGET_ID);
      assertThat(result.get(0).participantId()).isNull();
    }

    @Test
    void whenTradeIsAccepted_returnsProposedAndAcceptedEntries() {
      stubActiveDraft();
      LocalDateTime proposed = LocalDateTime.now().minusMinutes(5);
      LocalDateTime resolved = LocalDateTime.now();
      DraftParticipantTrade trade =
          buildTrade(DraftParticipantTradeStatus.ACCEPTED, proposed, resolved);
      when(swapAuditRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(tradeRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of(trade));

      List<DraftAuditEntryResponse> result = service.getAuditForGame(GAME_ID);

      assertThat(result).hasSize(2);
      // sorted desc: TRADE_ACCEPTED (resolved=now) before TRADE_PROPOSED (proposed=5min ago)
      assertThat(result.get(0).type()).isEqualTo("TRADE_ACCEPTED");
      assertThat(result.get(1).type()).isEqualTo("TRADE_PROPOSED");
    }

    @Test
    void whenTradeIsRejected_returnsProposedAndRejectedEntries() {
      stubActiveDraft();
      LocalDateTime proposed = LocalDateTime.now().minusMinutes(5);
      LocalDateTime resolved = LocalDateTime.now();
      DraftParticipantTrade trade =
          buildTrade(DraftParticipantTradeStatus.REJECTED, proposed, resolved);
      when(swapAuditRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of());
      when(tradeRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of(trade));

      List<DraftAuditEntryResponse> result = service.getAuditForGame(GAME_ID);

      assertThat(result).hasSize(2);
      assertThat(result.get(0).type()).isEqualTo("TRADE_REJECTED");
      assertThat(result.get(1).type()).isEqualTo("TRADE_PROPOSED");
    }

    @Test
    void swapEntry_hasCorrectFields() {
      stubActiveDraft();
      LocalDateTime at = LocalDateTime.now();
      DraftSwapAuditEntry swap =
          DraftSwapAuditEntry.restore(
              UUID.randomUUID(), DRAFT_ID, PARTICIPANT_ID, PLAYER_A, PLAYER_B, at);
      when(swapAuditRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of(swap));
      when(tradeRepository.findByDraftId(DRAFT_ID)).thenReturn(List.of());

      List<DraftAuditEntryResponse> result = service.getAuditForGame(GAME_ID);

      assertThat(result).hasSize(1);
      DraftAuditEntryResponse entry = result.get(0);
      assertThat(entry.type()).isEqualTo("SWAP_SOLO");
      assertThat(entry.participantId()).isEqualTo(PARTICIPANT_ID);
      assertThat(entry.playerOutId()).isEqualTo(PLAYER_A);
      assertThat(entry.playerInId()).isEqualTo(PLAYER_B);
      assertThat(entry.proposerParticipantId()).isNull();
      assertThat(entry.targetParticipantId()).isNull();
    }
  }
}
