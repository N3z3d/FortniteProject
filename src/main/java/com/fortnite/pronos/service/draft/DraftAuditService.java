package com.fortnite.pronos.service.draft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.draft.model.DraftParticipantTrade;
import com.fortnite.pronos.domain.draft.model.DraftParticipantTradeStatus;
import com.fortnite.pronos.domain.draft.model.DraftSwapAuditEntry;
import com.fortnite.pronos.domain.port.out.DraftDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftParticipantTradeRepositoryPort;
import com.fortnite.pronos.domain.port.out.DraftSwapAuditRepositoryPort;
import com.fortnite.pronos.dto.DraftAuditEntryResponse;
import com.fortnite.pronos.exception.InvalidDraftStateException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for querying the consolidated swap/trade audit trail (FR-36). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DraftAuditService {

  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftSwapAuditRepositoryPort swapAuditRepository;
  private final DraftParticipantTradeRepositoryPort tradeRepository;

  /**
   * Returns all swap and trade audit entries for the active draft of the given game, sorted by
   * occurredAt descending.
   */
  public List<DraftAuditEntryResponse> getAuditForGame(UUID gameId) {
    UUID draftId =
        draftDomainRepository
            .findActiveByGameId(gameId)
            .orElseThrow(
                () -> new InvalidDraftStateException("No active draft found for game: " + gameId))
            .getId();

    List<DraftAuditEntryResponse> swapEntries =
        swapAuditRepository.findByDraftId(draftId).stream().map(this::toSwapResponse).toList();

    List<DraftAuditEntryResponse> tradeEntries =
        tradeRepository.findByDraftId(draftId).stream()
            .flatMap(t -> toTradeResponses(t).stream())
            .toList();

    return Stream.concat(swapEntries.stream(), tradeEntries.stream())
        .sorted(Comparator.comparing(DraftAuditEntryResponse::occurredAt).reversed())
        .toList();
  }

  private DraftAuditEntryResponse toSwapResponse(DraftSwapAuditEntry entry) {
    return new DraftAuditEntryResponse(
        entry.getId(),
        "SWAP_SOLO",
        entry.getOccurredAt(),
        entry.getParticipantId(),
        null,
        null,
        entry.getPlayerOutId(),
        entry.getPlayerInId());
  }

  private List<DraftAuditEntryResponse> toTradeResponses(DraftParticipantTrade trade) {
    List<DraftAuditEntryResponse> entries = new ArrayList<>();

    entries.add(
        new DraftAuditEntryResponse(
            trade.getId(),
            "TRADE_PROPOSED",
            trade.getProposedAt(),
            null,
            trade.getProposerParticipantId(),
            trade.getTargetParticipantId(),
            trade.getPlayerFromProposerId(),
            trade.getPlayerFromTargetId()));

    if (trade.getResolvedAt() != null) {
      String type =
          trade.getStatus() == DraftParticipantTradeStatus.ACCEPTED
              ? "TRADE_ACCEPTED"
              : "TRADE_REJECTED";
      entries.add(
          new DraftAuditEntryResponse(
              trade.getId(),
              type,
              trade.getResolvedAt(),
              null,
              trade.getProposerParticipantId(),
              trade.getTargetParticipantId(),
              trade.getPlayerFromProposerId(),
              trade.getPlayerFromTargetId()));
    }

    return entries;
  }
}
