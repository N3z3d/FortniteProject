package com.fortnite.pronos.service.seed;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.service.MockDataGeneratorService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvSeedDataProviderService implements SeedDataProvider {

  private static final String KEY = "csv";

  private final MockDataGeneratorService mockDataGeneratorService;

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public MockDataGeneratorService.MockDataSet loadSeedData() {
    return mockDataGeneratorService.loadMockDataFromCsv();
  }
}
