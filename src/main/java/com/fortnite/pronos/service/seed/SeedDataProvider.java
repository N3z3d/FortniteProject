package com.fortnite.pronos.service.seed;

import com.fortnite.pronos.service.MockDataGeneratorService;

public interface SeedDataProvider {
  String getKey();

  MockDataGeneratorService.MockDataSet loadSeedData();
}
