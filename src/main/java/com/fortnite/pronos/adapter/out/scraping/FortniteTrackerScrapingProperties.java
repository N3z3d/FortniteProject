package com.fortnite.pronos.adapter.out.scraping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for FortniteTracker scraping via proxy services (Scrapfly, ScraperAPI,
 * Scrape.do).
 *
 * <p>Env vars: SCRAPING_FORTNITETRACKER_SCRAPFLY_KEYS, SCRAPING_FORTNITETRACKER_SCRAPERAPI_KEYS,
 * SCRAPING_FORTNITETRACKER_SCRAPEDO_TOKEN
 */
@Component
@ConfigurationProperties(prefix = "scraping.fortnitetracker")
public class FortniteTrackerScrapingProperties {

  private static final int DEFAULT_MAX_ATTEMPTS = 8;
  private static final int DEFAULT_REQUEST_TIMEOUT_MS = 20_000;
  private static final int DEFAULT_PAGES_PER_REGION = 4;

  /** Comma-separated Scrapfly API keys. */
  private String scrapflyKeys = "";

  /** Comma-separated ScraperAPI API keys. */
  private String scraperapiKeys = "";

  /** Single Scrape.do token. */
  private String scrapedoToken = "";

  /** Maximum retry attempts per page. */
  private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

  /** HTTP request timeout in milliseconds. */
  private int requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;

  /** Number of pages to scrape per region. */
  private int pagesPerRegion = DEFAULT_PAGES_PER_REGION;

  /** Platform filter (e.g. "pc"). */
  private String platform = "pc";

  /** Timeframe filter (e.g. "year"). */
  private String timeframe = "year";

  /** Comma-separated User-Agent strings for rotation. Empty = use default. */
  private String userAgents = "";

  public List<String> getScrapflyKeyList() {
    return parseKeys(scrapflyKeys);
  }

  public List<String> getScraperapiKeyList() {
    return parseKeys(scraperapiKeys);
  }

  public List<String> getScrapedoTokenList() {
    return parseKeys(scrapedoToken);
  }

  private List<String> parseKeys(String value) {
    if (value == null || value.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  public String getScrapflyKeys() {
    return scrapflyKeys;
  }

  public void setScrapflyKeys(String scrapflyKeys) {
    this.scrapflyKeys = scrapflyKeys;
  }

  public String getScraperapiKeys() {
    return scraperapiKeys;
  }

  public void setScraperapiKeys(String scraperapiKeys) {
    this.scraperapiKeys = scraperapiKeys;
  }

  public String getScrapedoToken() {
    return scrapedoToken;
  }

  public void setScrapedoToken(String scrapedoToken) {
    this.scrapedoToken = scrapedoToken;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public int getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public void setRequestTimeoutMs(int requestTimeoutMs) {
    this.requestTimeoutMs = requestTimeoutMs;
  }

  public int getPagesPerRegion() {
    return pagesPerRegion;
  }

  public void setPagesPerRegion(int pagesPerRegion) {
    this.pagesPerRegion = pagesPerRegion;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getTimeframe() {
    return timeframe;
  }

  public void setTimeframe(String timeframe) {
    this.timeframe = timeframe;
  }

  public String getUserAgents() {
    return userAgents;
  }

  public void setUserAgents(String userAgents) {
    this.userAgents = userAgents;
  }

  public List<String> getUserAgentList() {
    return parseKeys(userAgents);
  }
}
