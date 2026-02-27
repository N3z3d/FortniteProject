package com.fortnite.pronos.service.catalogue;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.game.model.PlayerRegion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pre-warms the player catalogue cache at application startup and after nightly scraping (FR-13,
 * NFR-P05). Ensures all catalogue endpoints respond from cache before user traffic arrives.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerCatalogueWarmupService implements ApplicationListener<ApplicationReadyEvent> {

  private final PlayerCatalogueService playerCatalogueService;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    warmup();
  }

  /**
   * Warms the catalogue cache by pre-loading all-regions and per-region entries. Can be called
   * explicitly by the nightly scraping scheduler after batch completion.
   *
   * <p>Errors are caught and logged as warnings so that a DB unavailability at startup does not
   * prevent the application from starting (users will experience cold-start latency instead).
   */
  public void warmup() {
    try {
      log.info("[CATALOGUE-WARMUP] Starting catalogue cache warmup...");
      playerCatalogueService.findAll();
      for (PlayerRegion region : PlayerRegion.values()) {
        if (region == PlayerRegion.UNKNOWN) {
          continue;
        }
        playerCatalogueService.findByRegion(region);
      }
      long warmedRegions =
          java.util.Arrays.stream(PlayerRegion.values())
              .filter(r -> r != PlayerRegion.UNKNOWN)
              .count();
      log.info(
          "[CATALOGUE-WARMUP] Cache warmed: all-regions + {} per-region entries", warmedRegions);
    } catch (Exception e) {
      log.warn(
          "[CATALOGUE-WARMUP] Cache warmup failed — users may experience cold-start latency: {}",
          e.getMessage());
    }
  }
}
