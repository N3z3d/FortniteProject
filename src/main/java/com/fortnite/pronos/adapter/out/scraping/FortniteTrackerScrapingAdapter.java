package com.fortnite.pronos.adapter.out.scraping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.ingestion.PrRegionCsvSourcePort;

/**
 * Scraping adapter that fetches FortniteTracker PR leaderboards via proxy services (Scrapfly,
 * ScraperAPI, Scrape.do) and returns the data as CSV for the ingestion pipeline.
 */
@Component
public class FortniteTrackerScrapingAdapter implements PrRegionCsvSourcePort {

  private static final Logger log = LoggerFactory.getLogger(FortniteTrackerScrapingAdapter.class);
  private static final String CSV_HEADER = "nickname,region,points,rank,snapshot_date";
  private static final double BACKOFF_BASE_MS = 300.0;
  private static final double BACKOFF_MULTIPLIER = 1.9;
  private static final int BACKOFF_JITTER_MS = 300;

  private final FortniteTrackerScrapingProperties props;
  private final RestTemplate restTemplate;
  private final FortniteTrackerHtmlParser htmlParser;
  private final ProxyUrlBuilder urlBuilder;

  public FortniteTrackerScrapingAdapter(
      FortniteTrackerScrapingProperties props,
      @Qualifier("restTemplateForScraping") RestTemplate restTemplate) {
    this.props = props;
    this.restTemplate = restTemplate;
    this.htmlParser = new FortniteTrackerHtmlParser();
    this.urlBuilder = new ProxyUrlBuilder();
  }

  @Override
  public Optional<String> fetchCsv(PrRegion region) {
    List<String> providers = getAvailableProviders();
    if (providers.isEmpty()) {
      log.warn("No scraping providers configured — skipping scraping for region={}", region.name());
      return Optional.empty();
    }

    String regionCode = region.name();
    List<ScrapedRow> allRows = new ArrayList<>();

    for (int page = 1; page <= props.getPagesPerRegion(); page++) {
      Optional<List<ScrapedRow>> pageRows = fetchPageWithRetry(regionCode, page);
      if (pageRows.isPresent()) {
        allRows.addAll(pageRows.get());
      } else {
        log.warn("All retries exhausted for region={} page={} — skipping page", regionCode, page);
      }
    }

    if (allRows.isEmpty()) {
      log.warn("No rows scraped for region={} — returning empty", regionCode);
      return Optional.empty();
    }

    Map<String, ScrapedRow> deduped = htmlParser.deduplicate(allRows);
    String csv = assembleCsv(deduped.values());
    log.info(
        "Scraped region={}: {} unique rows (from {} raw)",
        regionCode,
        deduped.size(),
        allRows.size());
    return Optional.of(csv);
  }

  Optional<List<ScrapedRow>> fetchPageWithRetry(String region, int page) {
    String targetUrl =
        urlBuilder.buildTarget(region, page, props.getPlatform(), props.getTimeframe());
    int ordinalOffset = (page - 1) * 100;

    for (int attempt = 0; attempt < props.getMaxAttempts(); attempt++) {
      String provider = pickProvider(region, page, attempt);
      String key = pickKey(provider, attempt);
      String proxyUrl = urlBuilder.build(provider, targetUrl, key, props.getRequestTimeoutMs());

      try {
        ResponseEntity<String> resp = restTemplate.getForEntity(proxyUrl, String.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
          List<ScrapedRow> parsed = htmlParser.parse(resp.getBody(), region, ordinalOffset);
          if (!parsed.isEmpty()) {
            return Optional.of(parsed);
          }
          log.warn(
              "Scraping attempt {}/{} returned 0 rows for region={} page={} via {}",
              attempt + 1,
              props.getMaxAttempts(),
              region,
              page,
              provider);
        }
      } catch (Exception e) {
        log.warn(
            "Scraping attempt {}/{} failed for region={} page={} via {}: {}",
            attempt + 1,
            props.getMaxAttempts(),
            region,
            page,
            provider,
            e.getMessage());
      }

      if (attempt < props.getMaxAttempts() - 1) {
        sleepBackoff(attempt);
      }
    }
    return Optional.empty();
  }

  String pickProvider(String region, int page, int attempt) {
    List<String> available = getAvailableProviders();
    if (available.isEmpty()) {
      return "scraperapi";
    }
    int slot = Math.abs((region + "-" + page).hashCode()) % available.size();
    int index = (slot + (attempt % available.size())) % available.size();
    return available.get(index);
  }

  String pickKey(String provider, int attempt) {
    List<String> keys = getKeysForProvider(provider);
    if (keys.isEmpty()) {
      return "";
    }
    return keys.get(attempt % keys.size());
  }

  List<String> getAvailableProviders() {
    List<String> available = new ArrayList<>();
    if (!props.getScrapflyKeyList().isEmpty()) {
      available.add("scrapfly");
    }
    if (!props.getScraperapiKeyList().isEmpty()) {
      available.add("scraperapi");
    }
    if (!props.getScrapedoTokenList().isEmpty()) {
      available.add("scrapedo");
    }
    return available;
  }

  private List<String> getKeysForProvider(String provider) {
    return switch (provider.toLowerCase()) {
      case "scrapfly" -> props.getScrapflyKeyList();
      case "scraperapi" -> props.getScraperapiKeyList();
      case "scrapedo" -> props.getScrapedoTokenList();
      default -> List.of();
    };
  }

  String assembleCsv(Collection<ScrapedRow> rows) {
    String today = LocalDate.now().toString();
    StringBuilder sb = new StringBuilder(CSV_HEADER).append('\n');
    for (ScrapedRow row : rows) {
      sb.append(row.nickname())
          .append(',')
          .append(row.region())
          .append(',')
          .append(row.points())
          .append(',')
          .append(row.rank())
          .append(',')
          .append(today)
          .append('\n');
    }
    return sb.toString();
  }

  private void sleepBackoff(int attempt) {
    long millis =
        (long) (BACKOFF_BASE_MS * Math.pow(BACKOFF_MULTIPLIER, attempt))
            + (long) (Math.random() * BACKOFF_JITTER_MS);
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
}
