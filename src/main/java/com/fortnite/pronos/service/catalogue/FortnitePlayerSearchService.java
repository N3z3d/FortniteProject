package com.fortnite.pronos.service.catalogue;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.FortniteApiPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Searches for Fortnite players via the external Fortnite API. Returns empty Optional when the API
 * is not configured or the player does not exist.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FortnitePlayerSearchService {

  private final FortniteApiPort fortniteApiPort;

  public Optional<FortnitePlayerData> searchByName(String playerName) {
    if (playerName == null || playerName.isBlank()) {
      return Optional.empty();
    }
    log.debug("Searching Fortnite player by name: '{}'", playerName);
    return fortniteApiPort.searchByName(playerName.strip());
  }

  public Optional<FortnitePlayerData> fetchByEpicId(String epicAccountId) {
    if (epicAccountId == null || epicAccountId.isBlank()) {
      return Optional.empty();
    }
    log.debug("Fetching Fortnite player by epic ID: '{}'", epicAccountId);
    return fortniteApiPort.fetchByEpicId(epicAccountId.strip());
  }
}
