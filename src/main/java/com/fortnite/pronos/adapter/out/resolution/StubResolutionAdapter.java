package com.fortnite.pronos.adapter.out.resolution;

import java.util.Locale;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.ResolutionPort;

/**
 * Stub implementation of ResolutionPort.
 *
 * <p>Active by default (matchIfMissing=true) while the real Fortnite-API.com key is unavailable.
 * Swap with FortniteApiResolutionAdapter when the API key is configured
 * (resolution.adapter=fortnite-api).
 */
@Component
@Primary
@ConditionalOnProperty(name = "resolution.adapter", havingValue = "stub", matchIfMissing = true)
public class StubResolutionAdapter implements ResolutionPort {

  @Override
  public String adapterName() {
    return "stub";
  }

  @Override
  public Optional<FortnitePlayerData> resolvePlayer(String pseudo, String region) {
    if (pseudo == null || pseudo.isBlank()) {
      return Optional.empty();
    }
    String fakeEpicId = "STUB-EPIC-" + pseudo.toUpperCase(Locale.ROOT);
    return Optional.of(FortnitePlayerData.stub(fakeEpicId, pseudo));
  }
}
