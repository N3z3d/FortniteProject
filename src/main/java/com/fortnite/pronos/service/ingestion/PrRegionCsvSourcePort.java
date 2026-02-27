package com.fortnite.pronos.service.ingestion;

import java.util.Optional;

import com.fortnite.pronos.model.PrRegion;

/**
 * Port for retrieving a raw PR CSV payload per region.
 *
 * <p>Implementations can fetch from local files, scraping adapters, or any external source.
 */
public interface PrRegionCsvSourcePort {

  Optional<String> fetchCsv(PrRegion region);
}
