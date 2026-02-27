package com.fortnite.pronos.service.ingestion;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.ResolutionPort;

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

  public ResolutionQueueService(
      ResolutionPort resolutionPort, PlayerIdentityRepositoryPort identityRepository) {
    this.resolutionPort = resolutionPort;
    this.identityRepository = identityRepository;
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
    return entry.getPlayerUsername() == null || entry.getPlayerUsername().isBlank();
  }

  private int tryResolveEntry(PlayerIdentityEntry entry) {
    try {
      Optional<String> epicId =
          resolutionPort.resolveFortniteId(entry.getPlayerUsername(), entry.getPlayerRegion());
      if (epicId.isPresent()) {
        entry.resolve(epicId.get(), 100, "AUTO");
        identityRepository.save(entry);
        log.info(
            "Resolved: pseudo='{}' region={} epicId={}",
            entry.getPlayerUsername(),
            entry.getPlayerRegion(),
            epicId.get());
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
}
