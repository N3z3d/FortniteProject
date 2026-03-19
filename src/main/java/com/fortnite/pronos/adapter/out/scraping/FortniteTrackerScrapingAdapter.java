package com.fortnite.pronos.adapter.out.scraping;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
    Set<String> exhaustedKeys = new HashSet<>();

    for (int attempt = 0; attempt < props.getMaxAttempts(); attempt++) {
      List<String> activeProviders = getActiveProviders(exhaustedKeys);
      if (activeProviders.isEmpty()) {
        log.warn(
            "All keys exhausted for region={} page={} — stopping early after {} attempts",
            region,
            page,
            attempt);
        break;
      }

      String provider = pickProviderFrom(activeProviders, region, page, attempt);
      String key = pickActiveKey(provider, attempt, exhaustedKeys);
      if (key == null) {
        continue; // safety: all keys for this provider just became exhausted
      }
      String proxyUrl = urlBuilder.build(provider, targetUrl, key, props.getRequestTimeoutMs());

      try {
        ResponseEntity<String> resp =
            restTemplate.exchange(
                URI.create(proxyUrl), HttpMethod.GET, buildRequestEntity(attempt), String.class);
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
        if (isQuotaError(e)) {
          exhaustedKeys.add(provider + ":" + key);
          log.warn(
              "Key quota exhausted for provider={} — blacklisting for this session ({} keys blacklisted)",
              provider,
              exhaustedKeys.size());
          continue; // no backoff for quota errors — switch key immediately
        }
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

  List<String> getActiveProviders(Set<String> exhaustedKeys) {
    return getAvailableProviders().stream()
        .filter(
            p -> getKeysForProvider(p).stream().anyMatch(k -> !exhaustedKeys.contains(p + ":" + k)))
        .collect(Collectors.toList());
  }

  String pickProviderFrom(List<String> providers, String region, int page, int attempt) {
    if (providers.isEmpty()) {
      return "scraperapi";
    }
    if (attempt == 0 && providers.contains("scrapedo")) {
      return "scrapedo";
    }
    int slot = Math.abs((region + "-" + page).hashCode()) % providers.size();
    int index = (slot + (attempt % providers.size())) % providers.size();
    return providers.get(index);
  }

  String pickActiveKey(String provider, int attempt, Set<String> exhaustedKeys) {
    List<String> activeKeys =
        getKeysForProvider(provider).stream()
            .filter(k -> !exhaustedKeys.contains(provider + ":" + k))
            .collect(Collectors.toList());
    if (activeKeys.isEmpty()) {
      return null;
    }
    return activeKeys.get(attempt % activeKeys.size());
  }

  boolean isQuotaError(Exception e) {
    String msg = e.getMessage();
    return msg != null
        && (msg.contains("429")
            || msg.contains("QUOTA_LIMIT_REACHED")
            || msg.toLowerCase().contains("quota"));
  }

  String pickProvider(String region, int page, int attempt) {
    List<String> available = getAvailableProviders();
    if (available.isEmpty()) {
      return "scraperapi";
    }
    // First attempt always uses Scrape.do if available (preferred provider)
    if (attempt == 0 && available.contains("scrapedo")) {
      return "scrapedo";
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

  private HttpEntity<Void> buildRequestEntity(int attempt) {
    List<String> uas = props.getUserAgentList();
    if (uas.isEmpty()) {
      return new HttpEntity<>(HttpHeaders.EMPTY);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", uas.get(attempt % uas.size()));
    return new HttpEntity<>(headers);
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
