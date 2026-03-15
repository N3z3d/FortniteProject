package com.fortnite.pronos.adapter.out.scraping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds proxy URLs for FortniteTracker scraping.
 *
 * <p>Supports: Scrapfly, ScraperAPI, Scrape.do. Not a Spring bean — instantiated by the adapter.
 */
class ProxyUrlBuilder {

  private static final String FT_BASE_URL = "https://fortnitetracker.com/events/powerrankings";
  private static final String SCRAPFLY_BASE = "https://api.scrapfly.io/scrape";
  private static final String SCRAPERAPI_BASE = "https://api.scraperapi.com/";
  private static final String SCRAPEDO_BASE = "http://api.scrape.do/";

  /**
   * Builds the FortniteTracker target URL for a given region and page.
   *
   * @param region PR region code (e.g. "EU")
   * @param page page number (1-indexed)
   * @param platform platform filter (e.g. "pc")
   * @param timeframe timeframe filter (e.g. "year")
   * @return the full FortniteTracker leaderboard URL
   */
  String buildTarget(String region, int page, String platform, String timeframe) {
    return FT_BASE_URL
        + "?platform="
        + encode(platform)
        + "&region="
        + encode(region)
        + "&time="
        + encode(timeframe)
        + "&page="
        + page;
  }

  /**
   * Builds a proxy URL for the given provider.
   *
   * @param provider one of "scrapfly", "scraperapi", "scrapedo"
   * @param targetUrl the FortniteTracker URL to scrape
   * @param key the API key or token for this provider
   * @param timeoutMs request timeout in milliseconds (used by ScraperAPI)
   * @return the full proxy URL
   */
  String build(String provider, String targetUrl, String key, int timeoutMs) {
    return switch (provider.toLowerCase()) {
      case "scrapfly" -> buildScrapfly(targetUrl, key);
      case "scraperapi" -> buildScraperapi(targetUrl, key, timeoutMs);
      case "scrapedo" -> buildScrapedo(targetUrl, key);
      default -> buildScraperapi(targetUrl, key, timeoutMs);
    };
  }

  private String buildScrapfly(String targetUrl, String key) {
    return SCRAPFLY_BASE
        + "?key="
        + encode(key)
        + "&asp=true&render_js=true&country=us"
        + "&url="
        + encode(targetUrl);
  }

  private String buildScraperapi(String targetUrl, String key, int timeoutMs) {
    return SCRAPERAPI_BASE
        + "?api_key="
        + encode(key)
        + "&render=false"
        + "&wait_selector=tbody"
        + "&timeout="
        + timeoutMs
        + "&url="
        + encode(targetUrl);
  }

  private String buildScrapedo(String targetUrl, String key) {
    return SCRAPEDO_BASE + "?url=" + encode(targetUrl) + "&token=" + encode(key);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
