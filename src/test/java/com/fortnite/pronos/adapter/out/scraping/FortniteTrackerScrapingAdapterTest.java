package com.fortnite.pronos.adapter.out.scraping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.model.PrRegion;

@DisplayName("FortniteTrackerScrapingAdapter")
class FortniteTrackerScrapingAdapterTest {

  private static final String VALID_HTML =
      "<table><tbody>"
          + "<tr><td class='leaderboard-rank' placement='1'>1</td>"
          + "<td><span class='leaderboard-user__nickname'>Bugha</span></td>"
          + "<td class='column--highlight'><div>12500</div></td></tr>"
          + "<tr><td class='leaderboard-rank' placement='2'>2</td>"
          + "<td><span class='leaderboard-user__nickname'>Aqua</span></td>"
          + "<td class='column--highlight'><div>11200</div></td></tr>"
          + "</tbody></table>";

  private RestTemplate restTemplate;
  private FortniteTrackerScrapingProperties props;
  private FortniteTrackerScrapingAdapter adapter;

  @BeforeEach
  void setUp() {
    restTemplate = mock(RestTemplate.class);
    props = new FortniteTrackerScrapingProperties();
    props.setScraperapiKeys("testkey");
    props.setMaxAttempts(3);
    props.setPagesPerRegion(1);
    props.setPlatform("pc");
    props.setTimeframe("year");
    props.setRequestTimeoutMs(20000);
    adapter = new FortniteTrackerScrapingAdapter(props, restTemplate);
  }

  @Nested
  @DisplayName("fetchCsv()")
  class FetchCsvTests {

    @Test
    @DisplayName("returns empty Optional when no providers configured")
    void fetchCsv_noProviders_returnsEmpty() {
      FortniteTrackerScrapingProperties emptyProps = new FortniteTrackerScrapingProperties();
      FortniteTrackerScrapingAdapter noProviderAdapter =
          new FortniteTrackerScrapingAdapter(emptyProps, restTemplate);

      Optional<String> result = noProviderAdapter.fetchCsv(PrRegion.EU);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns CSV with header when scraping succeeds")
    void fetchCsv_success_returnsCsvWithHeader() {
      when(restTemplate.exchange(
              anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
          .thenReturn(new ResponseEntity<>(VALID_HTML, HttpStatus.OK));

      Optional<String> result = adapter.fetchCsv(PrRegion.EU);
      assertThat(result).isPresent();
      assertThat(result.get()).startsWith("nickname,region,points,rank,snapshot_date");
      assertThat(result.get()).contains("Bugha,EU,12500,1,");
      assertThat(result.get()).contains("Aqua,EU,11200,2,");
    }

    @Test
    @DisplayName("returns empty Optional when all pages fail")
    void fetchCsv_allPagesFail_returnsEmpty() {
      when(restTemplate.exchange(
              anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
          .thenThrow(new RuntimeException("Network error"));

      props.setMaxAttempts(1);
      Optional<String> result = adapter.fetchCsv(PrRegion.EU);
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("fetchPageWithRetry()")
  class FetchPageWithRetryTests {

    @Test
    @DisplayName("returns rows after 2 failures then success on 3rd attempt")
    void fetchPageWithRetry_twoFailuresThenSuccess() {
      when(restTemplate.exchange(
              anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
          .thenThrow(new RuntimeException("fail"))
          .thenThrow(new RuntimeException("fail"))
          .thenReturn(new ResponseEntity<>(VALID_HTML, HttpStatus.OK));

      Optional<List<ScrapedRow>> result = adapter.fetchPageWithRetry("EU", 1);
      assertThat(result).isPresent();
      assertThat(result.get()).hasSize(2);
    }

    @Test
    @DisplayName("returns empty Optional when all attempts exhausted")
    void fetchPageWithRetry_allAttemptsFail_returnsEmpty() {
      when(restTemplate.exchange(
              anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
          .thenThrow(new RuntimeException("Network failure"));

      props.setMaxAttempts(2);
      Optional<List<ScrapedRow>> result = adapter.fetchPageWithRetry("EU", 1);
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("sets User-Agent header when userAgents configured")
    void fetchPageWithRetry_setsUserAgentHeader_whenConfigured() {
      props.setUserAgents("Mozilla/5.0 TestAgent/1.0");
      when(restTemplate.exchange(
              anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
          .thenReturn(new ResponseEntity<>(VALID_HTML, HttpStatus.OK));

      adapter.fetchPageWithRetry("EU", 1);

      verify(restTemplate)
          .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }
  }

  @Nested
  @DisplayName("pickProvider()")
  class PickProviderTests {

    @Test
    @DisplayName("returns single available provider consistently")
    void pickProvider_singleProvider_returnsThatProvider() {
      String provider = adapter.pickProvider("EU", 1, 0);
      assertThat(provider).isEqualTo("scraperapi");
    }

    @Test
    @DisplayName("rotates provider on subsequent attempts")
    void pickProvider_withMultipleProviders_rotates() {
      props.setScrapflyKeys("sfkey");
      FortniteTrackerScrapingAdapter multiAdapter =
          new FortniteTrackerScrapingAdapter(props, restTemplate);
      List<String> providers = multiAdapter.getAvailableProviders();
      assertThat(providers).hasSize(2);

      String attempt0 = multiAdapter.pickProvider("EU", 1, 0);
      String attempt1 = multiAdapter.pickProvider("EU", 1, 1);
      assertThat(attempt0).isNotEqualTo(attempt1);
    }
  }

  @Nested
  @DisplayName("assembleCsv()")
  class AssembleCsvTests {

    @Test
    @DisplayName("assembles CSV with correct header and rows")
    void assembleCsv_correctFormat() {
      List<ScrapedRow> rows = List.of(new ScrapedRow("TestPlayer", "EU", 5000, 1));
      String csv = adapter.assembleCsv(rows);
      assertThat(csv).startsWith("nickname,region,points,rank,snapshot_date\n");
      assertThat(csv).contains("TestPlayer,EU,5000,1,");
    }
  }
}
