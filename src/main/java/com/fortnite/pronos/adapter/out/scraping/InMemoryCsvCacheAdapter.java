package com.fortnite.pronos.adapter.out.scraping;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.ingestion.CsvCachePort;

/**
 * In-memory CSV cache per region. Cache is volatile: cleared on application restart. Intended as a
 * short-term fallback when live scraping returns empty (e.g., proxy quota exhausted). Not
 * persistent — do not rely on this cache surviving a restart or redeploy.
 */
@Component
public class InMemoryCsvCacheAdapter implements CsvCachePort {

  private final ConcurrentHashMap<PrRegion, String> cache = new ConcurrentHashMap<>();

  @Override
  public void save(PrRegion region, String csv) {
    cache.put(region, csv);
  }

  @Override
  public Optional<String> load(PrRegion region) {
    return Optional.ofNullable(cache.get(region));
  }
}
