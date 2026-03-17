package com.fortnite.pronos.service.ingestion;

import java.util.Optional;

import com.fortnite.pronos.model.PrRegion;

public interface CsvCachePort {
  void save(PrRegion region, String csv);

  Optional<String> load(PrRegion region);
}
