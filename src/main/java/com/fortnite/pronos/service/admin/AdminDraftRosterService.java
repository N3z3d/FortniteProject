package com.fortnite.pronos.service.admin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for admin roster management — assign and remove players bypassing tranche rules. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminDraftRosterService {

  private final GameDomainRepositoryPort gameDomainRepository;
  private final DraftDomainRepositoryPort draftDomainRepository;
  private final DraftRepositoryPort draftRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final GameParticipantRepositoryPort gameParticipantRepository;

  /**
   * Assigns a player to a participant's team in the given game, bypassing tranche rules. Throws
   * PlayerAlreadySelectedException if the player is already picked in this draft.
   */
  public DraftPickDto assignPlayer(UUID gameId, UUID participantUserId, UUID playerId) {
    gameDomainRepository
        .findById(gameId)
        .orElseThrow(() -> new GameNotFoundException("Game not found: " + gameId));

    com.fortnite.pronos.model.Draft draft = findDraftEntity(gameId);
    com.fortnite.pronos.model.GameParticipant participant =
        gameParticipantRepository
            .findByUserIdAndGameId(participantUserId, gameId)
            .orElseThrow(
                () ->
                    new GameNotFoundException(
                        "Participant not found for user "
                            + participantUserId
                            + " in game "
                            + gameId));
    com.fortnite.pronos.model.Player player = findLegacyPlayer(playerId);

    requirePlayerNotAlreadyPicked(draft.getId(), playerId);

    com.fortnite.pronos.model.DraftPick pick =
        new com.fortnite.pronos.model.DraftPick(draft, participant, player, 0, 0);
    com.fortnite.pronos.model.DraftPick saved = draftPickRepository.save(pick);

    log.info(
        "Admin assigned player {} to participant {} in game {}",
        playerId,
        participantUserId,
        gameId);
    return DraftPickDto.fromDraftPick(saved);
  }

  /**
   * Removes a player from the given game's draft. Throws GameNotFoundException if the player is not
   * found in this draft.
   */
  public void removePlayer(UUID gameId, UUID playerId) {
    com.fortnite.pronos.model.Draft draft = findDraftEntity(gameId);
    Set<UUID> pickedIds =
        new HashSet<>(draftPickRepository.findPickedPlayerIdsByDraftId(draft.getId()));
    if (!pickedIds.contains(playerId)) {
      throw new GameNotFoundException("Player not found in draft for game: " + gameId);
    }
    draftPickRepository.deleteByDraftIdAndPlayerId(draft.getId(), playerId);
    log.info("Admin removed player {} from game {}", playerId, gameId);
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

  private com.fortnite.pronos.model.Player findLegacyPlayer(UUID playerId) {
    return playerRepository
        .findById(playerId)
        .map(this::toLegacyPlayer)
        .orElseThrow(() -> new GameNotFoundException("Player not found: " + playerId));
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

  private void requirePlayerNotAlreadyPicked(UUID draftId, UUID playerId) {
    Set<UUID> pickedIds = new HashSet<>(draftPickRepository.findPickedPlayerIdsByDraftId(draftId));
    if (pickedIds.contains(playerId)) {
      throw new PlayerAlreadySelectedException("Player is already selected in this draft");
    }
  }
}
