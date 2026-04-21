package com.fortnite.pronos.service.ingestion;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.ResolutionPort;
import com.fortnite.pronos.service.admin.ConfidenceScoreService;

import lombok.extern.slf4j.Slf4j;

/**
 * Processes UNRESOLVED PlayerIdentityEntry items by calling ResolutionPort (swappable external
 * adapter). Non-blocking: failures and unresolved entries never stop the batch.
 */
@Service
@Slf4j
public class ResolutionQueueService {

  private final ResolutionPort resolutionPort;
  private final PlayerIdentityRepositoryPort identityRepository;
  private final ConfidenceScoreService confidenceScoreService;

  public ResolutionQueueService(
      ResolutionPort resolutionPort,
      PlayerIdentityRepositoryPort identityRepository,
      ConfidenceScoreService confidenceScoreService) {
    this.resolutionPort = resolutionPort;
    this.identityRepository = identityRepository;
    this.confidenceScoreService = confidenceScoreService;
  }

  /**
   * Loads all UNRESOLVED entries and attempts to resolve each one. Continues processing remaining
   * entries even if one fails or stays unresolved.
   *
   * @return number of entries successfully resolved in this run
   */
  public int processUnresolvedBatch() {
    List<PlayerIdentityEntry> unresolved =
        identityRepository.findByStatus(IdentityStatus.UNRESOLVED);
    int resolved = 0;
    for (PlayerIdentityEntry entry : unresolved) {
      if (isInvalidEntry(entry)) {
        log.warn(
            "Skipping invalid identity entry id={} pseudo='{}'",
            entry.getId(),
            entry.getPlayerUsername());
        continue;
      }
      resolved += tryResolveEntry(entry);
    }
    log.info("Resolution batch complete: resolved={} total={}", resolved, unresolved.size());
    return resolved;
  }

  private boolean isInvalidEntry(PlayerIdentityEntry entry) {
    return entry.getPlayerUsername() == null
        || entry.getPlayerUsername().isBlank()
        || entry.getPlayerRegion() == null
        || entry.getPlayerRegion().isBlank();
  }

  private int tryResolveEntry(PlayerIdentityEntry entry) {
    try {
      Optional<FortnitePlayerData> result =
          resolutionPort.resolvePlayer(entry.getPlayerUsername(), entry.getPlayerRegion());
      if (result.isPresent()) {
        FortnitePlayerData data = result.get();
        if (!hasValidEpicAccountId(data)) {
          log.warn(
              "Skipping resolution with missing Epic Account ID: pseudo='{}' region={}",
              entry.getPlayerUsername(),
              entry.getPlayerRegion());
          return 0;
        }
        String epicAccountId = data.epicAccountId();
        int score = confidenceScoreService.compute(entry, epicAccountId, data.displayName());
        entry.resolve(epicAccountId, score, "AUTO");
        identityRepository.save(entry);
        log.info(
            "Resolved: pseudo='{}' region={} epicId={} score={}",
            entry.getPlayerUsername(),
            entry.getPlayerRegion(),
            epicAccountId,
            score);
        return 1;
      }
      log.debug(
          "Not resolved: pseudo='{}' region={}",
          entry.getPlayerUsername(),
          entry.getPlayerRegion());
    } catch (Exception e) {
      log.error(
          "Resolution error: pseudo='{}' region={} error={}",
          entry.getPlayerUsername(),
          entry.getPlayerRegion(),
          e.getMessage());
    }
    return 0;
  }

  private boolean hasValidEpicAccountId(FortnitePlayerData data) {
    return data.epicAccountId() != null && !data.epicAccountId().isBlank();
  }
}
