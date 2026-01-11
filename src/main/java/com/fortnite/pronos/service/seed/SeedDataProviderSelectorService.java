package com.fortnite.pronos.service.seed;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.service.MockDataGeneratorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeedDataProviderSelectorService {

  private static final String DEFAULT_PROVIDER = "csv";

  private final List<SeedDataProvider> providers;

  @Value("${fortnite.data.provider:csv}")
  private String providerKey;

  public MockDataGeneratorService.MockDataSet loadSeedData() {
    return resolveProvider().loadSeedData();
  }

  private SeedDataProvider resolveProvider() {
    if (providers == null || providers.isEmpty()) {
      throw new IllegalStateException("No seed data providers registered");
    }

    String desired = normalizeKey(providerKey);
    SeedDataProvider fallback = null;

    for (SeedDataProvider provider : providers) {
      String key = normalizeKey(provider.getKey());
      if (DEFAULT_PROVIDER.equals(key)) {
        fallback = provider;
      }
      if (desired.equals(key)) {
        return provider;
      }
    }

    SeedDataProvider resolvedFallback = fallback != null ? fallback : providers.get(0);
    if (!desired.equals(normalizeKey(resolvedFallback.getKey()))) {
      log.warn(
          "Seed data provider '{}' not found, falling back to '{}'",
          desired,
          resolvedFallback.getKey());
    }
    return resolvedFallback;
  }

  private String normalizeKey(String value) {
    if (value == null) {
      return DEFAULT_PROVIDER;
    }
    String trimmed = value.trim().toLowerCase();
    return trimmed.isEmpty() ? DEFAULT_PROVIDER : trimmed;
  }
}
