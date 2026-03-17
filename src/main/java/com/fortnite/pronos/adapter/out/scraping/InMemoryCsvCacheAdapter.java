package com.fortnite.pronos.adapter.out.scraping;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.ingestion.CsvCachePort;

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
