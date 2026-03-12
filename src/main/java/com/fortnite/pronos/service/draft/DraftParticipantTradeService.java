package com.fortnite.pronos.service.draft;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.draft.model.DraftParticipantTrade;
import com.fortnite.pronos.domain.draft.model.DraftParticipantTradeStatus;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for 1v1 draft participant trades with explicit acceptance (FR-34, FR-35). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DraftParticipantTradeService {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftRepositoryPort draftRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;
  private final DraftParticipantTradeRepositoryPort tradeRepository;

  /**
   * Proposes a 1v1 draft trade: the caller's player for the target participant's player. No region
   * or rank restriction (FR-34).
   */
  public DraftTradeProposalResponse proposeTrade(
      UUID gameId,
      UUID proposerUserId,
      UUID targetParticipantId,
      UUID playerFromProposerId,
      UUID playerFromTargetId) {

    gameDomainRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

    com.fortnite.pronos.model.Draft legacyDraft = findDraftEntity(gameId);
    UUID draftId = legacyDraft.getId();

    com.fortnite.pronos.model.GameParticipant proposerParticipant =
        gameParticipantRepository
            .findByUserIdAndGameId(proposerUserId, gameId)
            .orElseThrow(
                () -> new InvalidSwapException("Proposer is not a participant in this game"));

    com.fortnite.pronos.model.GameParticipant targetParticipant =
        gameParticipantRepository
            .findById(targetParticipantId)
            .orElseThrow(
                () ->
                    new InvalidSwapException(
                        "Target participant not found: " + targetParticipantId));

    requirePlayerInTeam(
        draftId,
        proposerParticipant.getId(),
        playerFromProposerId,
        "Offered player is not in your team");
    requirePlayerInTeam(
        draftId,
        targetParticipant.getId(),
        playerFromTargetId,
        "Requested player is not in the target participant's team");

    DraftParticipantTrade trade =
        new DraftParticipantTrade(
            draftId,
            proposerParticipant.getId(),
            targetParticipantId,
            playerFromProposerId,
            playerFromTargetId);

    DraftParticipantTrade saved = tradeRepository.save(trade);
    log.info(
        "Draft trade proposed: game={} proposer={} target={} out={} in={}",
        gameId,
        proposerUserId,
        targetParticipantId,
        playerFromProposerId,
        playerFromTargetId);
    return toResponse(saved);
  }

  /**
   * Accepts a pending trade: swaps both players' DraftPicks between participants (FR-35). Only the
   * target participant may accept.
   */
  public DraftTradeProposalResponse acceptTrade(UUID gameId, UUID callerUserId, UUID tradeId) {
    DraftParticipantTrade trade = loadTrade(tradeId);
    requireTradePending(trade, tradeId);
    com.fortnite.pronos.model.Draft legacyDraft = findDraftEntity(gameId);
    UUID draftId = legacyDraft.getId();

    com.fortnite.pronos.model.GameParticipant callerParticipant =
        gameParticipantRepository
            .findByUserIdAndGameId(callerUserId, gameId)
            .orElseThrow(
                () -> new InvalidSwapException("Caller is not a participant in this game"));

    requireIsTargetParticipant(trade, callerParticipant.getId());

    com.fortnite.pronos.model.GameParticipant proposerParticipant =
        gameParticipantRepository
            .findById(trade.getProposerParticipantId())
            .orElseThrow(() -> new InvalidSwapException("Proposer participant no longer exists"));

    com.fortnite.pronos.model.DraftPick proposerPick =
        findPickOrThrow(draftId, trade.getProposerParticipantId(), trade.getPlayerFromProposerId());
    com.fortnite.pronos.model.DraftPick targetPick =
        findPickOrThrow(draftId, trade.getTargetParticipantId(), trade.getPlayerFromTargetId());

    draftPickRepository.deleteByDraftIdAndPlayerId(draftId, trade.getPlayerFromProposerId());
    draftPickRepository.deleteByDraftIdAndPlayerId(draftId, trade.getPlayerFromTargetId());

    draftPickRepository.save(
        new com.fortnite.pronos.model.DraftPick(
            legacyDraft, callerParticipant, proposerPick.getPlayer(), 0, 0));
    draftPickRepository.save(
        new com.fortnite.pronos.model.DraftPick(
            legacyDraft, proposerParticipant, targetPick.getPlayer(), 0, 0));

    DraftParticipantTrade accepted = trade.accept();
    DraftParticipantTrade saved = tradeRepository.save(accepted);
    log.info("Draft trade accepted: tradeId={} game={} caller={}", tradeId, gameId, callerUserId);
    return toResponse(saved);
  }

  /** Rejects a pending trade. Only the target participant may reject. No picks are modified. */
  public DraftTradeProposalResponse rejectTrade(UUID gameId, UUID callerUserId, UUID tradeId) {
    DraftParticipantTrade trade = loadTrade(tradeId);
    requireTradePending(trade, tradeId);

    com.fortnite.pronos.model.GameParticipant callerParticipant =
        gameParticipantRepository
            .findByUserIdAndGameId(callerUserId, gameId)
            .orElseThrow(
                () -> new InvalidSwapException("Caller is not a participant in this game"));

    requireIsTargetParticipant(trade, callerParticipant.getId());

    DraftParticipantTrade rejected = trade.reject();
    DraftParticipantTrade saved = tradeRepository.save(rejected);
    log.info("Draft trade rejected: tradeId={} game={} caller={}", tradeId, gameId, callerUserId);
    return toResponse(saved);
  }

  private com.fortnite.pronos.model.Draft findDraftEntity(UUID gameId) {
    com.fortnite.pronos.domain.draft.model.Draft domainDraft =
        draftDomainRepository
            .findActiveByGameId(gameId)
            .orElseThrow(
                () -> new InvalidDraftStateException("No active draft found for game: " + gameId));
    return draftRepository
        .findById(domainDraft.getId())
        .orElseThrow(
            () -> new InvalidDraftStateException("Draft entity not found for game: " + gameId));
  }

  private DraftParticipantTrade loadTrade(UUID tradeId) {
    return tradeRepository
        .findById(tradeId)
        .orElseThrow(() -> new InvalidSwapException("Trade proposal not found: " + tradeId));
  }

  private void requirePlayerInTeam(
      UUID draftId, UUID participantId, UUID playerId, String errorMessage) {
    if (!draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
        draftId, participantId, playerId)) {
      throw new InvalidSwapException(errorMessage);
    }
  }

  private void requireTradePending(DraftParticipantTrade trade, UUID tradeId) {
    if (trade.getStatus() != DraftParticipantTradeStatus.PENDING) {
      throw new InvalidSwapException("Trade is not pending: " + tradeId);
    }
  }

  private void requireIsTargetParticipant(DraftParticipantTrade trade, UUID callerParticipantId) {
    if (!trade.getTargetParticipantId().equals(callerParticipantId)) {
      throw new InvalidSwapException("Only the target participant can accept or reject this trade");
    }
  }

  private com.fortnite.pronos.model.DraftPick findPickOrThrow(
      UUID draftId, UUID participantId, UUID playerId) {
    return draftPickRepository
        .findByDraftIdAndParticipantIdAndPlayerId(draftId, participantId, playerId)
        .orElseThrow(
            () ->
                new InvalidSwapException(
                    "Pick not found for participant " + participantId + " and player " + playerId));
  }

  private DraftTradeProposalResponse toResponse(DraftParticipantTrade trade) {
    return new DraftTradeProposalResponse(
        trade.getId(),
        trade.getDraftId(),
        trade.getProposerParticipantId(),
        trade.getTargetParticipantId(),
        trade.getPlayerFromProposerId(),
        trade.getPlayerFromTargetId(),
        trade.getStatus().name());
  }
}
