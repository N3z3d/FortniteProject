package com.fortnite.pronos.adapter.out.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.model.PrRegion;

@DisplayName("InMemoryCsvCacheAdapter")
class InMemoryCsvCacheAdapterTest {

  private InMemoryCsvCacheAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InMemoryCsvCacheAdapter();
  }

  @Test
  @DisplayName("save then load returns the saved value")
  void save_thenLoad_returnsValue() {
    String csv = "nickname,region,points,rank,snapshot_date\nBugha,EU,12500,1,2026-03-17\n";

    adapter.save(PrRegion.EU, csv);
    Optional<String> result = adapter.load(PrRegion.EU);

    assertThat(result).isPresent().contains(csv);
  }

  @Test
  @DisplayName("load returns empty Optional when nothing saved for region")
  void load_returnsEmpty_whenNothingSaved() {
    Optional<String> result = adapter.load(PrRegion.NAC);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("save twice overwrites previous value")
  void save_twice_overwritesPrevious() {
    String first = "nickname,region,points,rank,snapshot_date\nPlayer1,EU,5000,1,2026-03-17\n";
    String second = "nickname,region,points,rank,snapshot_date\nPlayer2,EU,9000,1,2026-03-17\n";

    adapter.save(PrRegion.EU, first);
    adapter.save(PrRegion.EU, second);
    Optional<String> result = adapter.load(PrRegion.EU);

    assertThat(result).isPresent().contains(second);
  }
}
