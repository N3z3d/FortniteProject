package com.fortnite.pronos.domain.port.out;

import java.util.Optional;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;

/**
 * Port for fetching player data from the external Fortnite API. Implementations may call
 * Fortnite-API.com or return empty when the API key is not configured.
 */
public interface FortniteApiPort {

  /** Searches for a player by display name. Returns empty when not found or API unavailable. */
  Optional<FortnitePlayerData> searchByName(String playerName);

  /** Fetches player data by Epic account ID. Returns empty when not found or API unavailable. */
  Optional<FortnitePlayerData> fetchByEpicId(String epicAccountId);
}
