package com.fortnite.pronos.domain.port.out;

import java.util.Optional;

public interface ResolutionPort {

  /**
   * Resolves a FortniteTracker display name to a Fortnite Epic Account ID.
   *
   * @param pseudo the display name scraped from FortniteTracker
   * @param region the region of origin (context for retry / logging)
   * @return Optional.of(epicId) if resolved, Optional.empty() if not found
   * @throws RuntimeException if the external call fails — caller must catch
   */
  Optional<String> resolveFortniteId(String pseudo, String region);
}
