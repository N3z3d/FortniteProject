package com.fortnite.pronos.service.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fortnite.pronos.service.MockDataGeneratorService;

class SeedDataProviderSelectorServiceTest {

  @Test
  void usesRequestedProvider() {
    SeedDataProvider csvProvider = mock(SeedDataProvider.class);
    SeedDataProvider supabaseProvider = mock(SeedDataProvider.class);
    MockDataGeneratorService.MockDataSet csvData = MockDataGeneratorService.MockDataSet.empty();
    MockDataGeneratorService.MockDataSet supabaseData =
        new MockDataGeneratorService.MockDataSet(java.util.Map.of(), 1);

    when(csvProvider.getKey()).thenReturn("csv");
    when(csvProvider.loadSeedData()).thenReturn(csvData);
    when(supabaseProvider.getKey()).thenReturn("supabase");
    when(supabaseProvider.loadSeedData()).thenReturn(supabaseData);

    SeedDataProviderSelectorService selector =
        new SeedDataProviderSelectorService(List.of(csvProvider, supabaseProvider));
    ReflectionTestUtils.setField(selector, "providerKey", "supabase");

    MockDataGeneratorService.MockDataSet result = selector.loadSeedData();

    assertThat(result).isSameAs(supabaseData);
  }

  @Test
  void fallsBackToCsvWhenProviderMissing() {
    SeedDataProvider csvProvider = mock(SeedDataProvider.class);
    MockDataGeneratorService.MockDataSet csvData = MockDataGeneratorService.MockDataSet.empty();

    when(csvProvider.getKey()).thenReturn("csv");
    when(csvProvider.loadSeedData()).thenReturn(csvData);

    SeedDataProviderSelectorService selector =
        new SeedDataProviderSelectorService(List.of(csvProvider));
    ReflectionTestUtils.setField(selector, "providerKey", "unknown");

    MockDataGeneratorService.MockDataSet result = selector.loadSeedData();

    assertThat(result).isSameAs(csvData);
  }
}
