package com.fortnite.pronos.domain.port.out;

import java.util.Optional;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;

public interface ResolutionPort {

  /**
   * Resolves a display name to a Fortnite player, returning full player data.
   *
   * <p>Implementations should return {@code Optional.empty()} when the player is not found
   * <strong>or</strong> when the external call fails (resilient contract). Callers cannot
   * distinguish the two cases — use the result only as a best-effort suggestion.
   *
   * @param pseudo the display name scraped from FortniteTracker
   * @param region the region of origin (context for logging)
   * @return Optional.of(playerData) if resolved, Optional.empty() if not found or API unavailable
   */
  Optional<FortnitePlayerData> resolvePlayer(String pseudo, String region);

  /** Returns the adapter identifier (e.g. "stub", "fortnite-api"). */
  default String adapterName() {
    return "unknown";
  }
}
