package com.fortnite.pronos.adapter.out.resolution;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.FortniteApiPort;
import com.fortnite.pronos.domain.port.out.ResolutionPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Real implementation of ResolutionPort using the Fortnite-API.com external API.
 *
 * <p>Active when {@code resolution.adapter=fortnite-api} is set in configuration (requires
 * FORTNITE_API_KEY). Falls back to {@link StubResolutionAdapter} when the property is absent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "resolution.adapter", havingValue = "fortnite-api")
public class FortniteApiResolutionAdapter implements ResolutionPort {

  private final FortniteApiPort fortniteApiPort;

  @Override
  public String adapterName() {
    return "fortnite-api";
  }

  @Override
  public Optional<FortnitePlayerData> resolvePlayer(String pseudo, String region) {
    if (pseudo == null || pseudo.isBlank()) {
      return Optional.empty();
    }
    try {
      Optional<FortnitePlayerData> result = fortniteApiPort.searchByName(pseudo);
      log.debug(
          "FortniteApiResolutionAdapter: resolved '{}' (region={}) → {}",
          pseudo,
          region,
          result.map(FortnitePlayerData::epicAccountId).orElse("not found"));
      return result;
    } catch (Exception e) {
      log.warn(
          "FortniteApiResolutionAdapter: API unavailable for '{}' (region={}): {}",
          pseudo,
          region,
          e.getMessage());
      return Optional.empty();
    }
  }
}
