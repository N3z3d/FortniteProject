package com.fortnite.pronos.service.draft;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.draft.model.DraftSwapAuditEntry;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for solo player swap operations (FR-32, FR-33). */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SwapSoloService {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftRepositoryPort draftRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;
  private final DraftSwapAuditRepositoryPort auditRepository;

  /**
   * Executes a solo swap: removes playerOut from the participant's team and adds playerIn.
   * Validates that playerIn is free, same region, and strictly worse rank than playerOut.
   */
  public SwapSoloResponse executeSoloSwap(
      UUID gameId, UUID userId, UUID playerOutId, UUID playerInId) {
    gameDomainRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

    com.fortnite.pronos.model.Draft draft = findDraftEntity(gameId);
    com.fortnite.pronos.model.GameParticipant participant =
        gameParticipantRepository
            .findByUserIdAndGameId(userId, gameId)
            .orElseThrow(() -> new InvalidSwapException("Participant not found for this game"));

    requirePlayerInParticipantTeam(draft.getId(), participant.getId(), playerOutId);

    Set<UUID> pickedIds =
        new HashSet<>(draftPickRepository.findPickedPlayerIdsByDraftId(draft.getId()));
    requirePlayerIsFree(pickedIds, playerInId);

    com.fortnite.pronos.domain.player.model.Player playerOut = findDomainPlayer(playerOutId);
    com.fortnite.pronos.domain.player.model.Player playerIn = findDomainPlayer(playerInId);

    requireSameRegion(playerOut, playerIn);
    requireStrictlyWorseRank(playerOut, playerIn);

    draftPickRepository.deleteByDraftIdAndPlayerId(draft.getId(), playerOutId);
    com.fortnite.pronos.model.Player legacyPlayerIn = toLegacyPlayer(playerIn);
    com.fortnite.pronos.model.DraftPick newPick =
        new com.fortnite.pronos.model.DraftPick(draft, participant, legacyPlayerIn, 0, 0);
    draftPickRepository.save(newPick);

    auditRepository.save(
        new DraftSwapAuditEntry(draft.getId(), participant.getId(), playerOutId, playerInId));

    log.info(
        "Solo swap executed: game={} user={} out={} in={}",
        gameId,
        userId,
        playerOutId,
        playerInId);
    return new SwapSoloResponse(draft.getId(), participant.getId(), playerOutId, playerInId);
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

  private com.fortnite.pronos.domain.player.model.Player findDomainPlayer(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .orElseThrow(() -> new InvalidSwapException("Player not found: " + playerId));
  }

  private void requirePlayerInParticipantTeam(UUID draftId, UUID participantId, UUID playerId) {
    if (!draftPickRepository.existsByDraftIdAndParticipantIdAndPlayerId(
        draftId, participantId, playerId)) {
      throw new InvalidSwapException("Player to swap out is not in your team");
    }
  }

  private void requirePlayerIsFree(Set<UUID> pickedIds, UUID playerInId) {
    if (pickedIds.contains(playerInId)) {
      throw new InvalidSwapException("Player to swap in is already selected in this draft");
    }
  }

  private void requireSameRegion(
      com.fortnite.pronos.domain.player.model.Player playerOut,
      com.fortnite.pronos.domain.player.model.Player playerIn) {
    PlayerRegion regionOut = playerOut.getRegion();
    PlayerRegion regionIn = playerIn.getRegion();
    if (regionOut == null || !regionOut.equals(regionIn)) {
      throw new InvalidSwapException("Players must be in the same region for a solo swap");
    }
  }

  private void requireStrictlyWorseRank(
      com.fortnite.pronos.domain.player.model.Player playerOut,
      com.fortnite.pronos.domain.player.model.Player playerIn) {
    int trancheOut = parseTranche(playerOut);
    int trancheIn = parseTranche(playerIn);
    if (trancheIn <= trancheOut) {
      throw new InvalidSwapException(
          "Target player must have a strictly worse rank (higher tranche number)");
    }
  }

  private int parseTranche(com.fortnite.pronos.domain.player.model.Player player) {
    String tranche = player.getTranche();
    if (tranche == null) {
      throw new InvalidSwapException("Invalid tranche format for player: " + player.getId());
    }
    try {
      return Integer.parseInt(tranche.trim());
    } catch (NumberFormatException e) {
      throw new InvalidSwapException("Invalid tranche format for player: " + player.getId());
    }
  }

  private com.fortnite.pronos.model.Player toLegacyPlayer(
      com.fortnite.pronos.domain.player.model.Player domainPlayer) {
    com.fortnite.pronos.model.Player legacy = new com.fortnite.pronos.model.Player();
    legacy.setId(domainPlayer.getId());
    legacy.setFortniteId(domainPlayer.getFortniteId());
    legacy.setUsername(domainPlayer.getUsername());
    legacy.setNickname(domainPlayer.getNickname());
    legacy.setRegion(toLegacyRegion(domainPlayer.getRegion()));
    legacy.setTranche(domainPlayer.getTranche());
    legacy.setCurrentSeason(domainPlayer.getCurrentSeason());
    legacy.setLocked(domainPlayer.isLocked());
    return legacy;
  }

  private com.fortnite.pronos.model.Player.Region toLegacyRegion(PlayerRegion region) {
    if (region == null) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }
    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(region.name());
    } catch (IllegalArgumentException ex) {
      return com.fortnite.pronos.model.Player.Region.UNKNOWN;
    }
  }
}
