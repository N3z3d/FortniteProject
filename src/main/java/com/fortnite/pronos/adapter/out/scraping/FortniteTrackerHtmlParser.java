package com.fortnite.pronos.adapter.out.scraping;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses FortniteTracker leaderboard HTML into {@link ScrapedRow} objects.
 *
 * <p>Not a Spring bean — instantiated by the adapter. Uses JSoup for HTML parsing.
 */
class FortniteTrackerHtmlParser {

  private static final Logger log = LoggerFactory.getLogger(FortniteTrackerHtmlParser.class);
  private static final Pattern DIACRITICS_PATTERN =
      Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
  private static final Pattern ZERO_WIDTH_PATTERN = Pattern.compile("[\\u200B-\\u200D\\uFEFF]");
  private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("[^0-9]");

  /**
   * Parses an HTML page from FortniteTracker into a list of scraped rows.
   *
   * @param html raw HTML content
   * @param region PR region code (e.g. "EU")
   * @param pageOrdinalOffset offset for rank calculation (e.g. 100 for page 2)
   * @return list of parsed rows; empty if tbody is missing or has no valid rows
   */
  List<ScrapedRow> parse(String html, String region, int pageOrdinalOffset) {
    Document doc = Jsoup.parse(html);
    Elements rows = doc.select("tbody tr");

    if (rows.isEmpty()) {
      log.warn(
          "No tbody rows found for region={} — HTML structure may have changed or page is empty",
          region);
      return List.of();
    }

    List<ScrapedRow> result = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      Element tr = rows.get(i);
      try {
        ScrapedRow row = parseRow(tr, region, pageOrdinalOffset, i);
        if (row != null) {
          result.add(row);
        }
      } catch (Exception e) {
        log.debug(
            "Skipping malformed row at index {} for region={}: {}", i, region, e.getMessage());
      }
    }
    return result;
  }

  private ScrapedRow parseRow(Element tr, String region, int pageOrdinalOffset, int index) {
    String rankAttr = tr.select("[class*=leaderboard-rank]").attr("placement");
    int rank = rankAttr.isEmpty() ? (pageOrdinalOffset + index + 1) : Integer.parseInt(rankAttr);

    String player = tr.select("[class*=leaderboard-user__nickname]").text().trim();

    Element highlightDiv = tr.selectFirst("td[class*=column--highlight] div");
    Element rightDiv = tr.selectFirst("td[class*=column--right] div");
    String pointsText =
        highlightDiv != null ? highlightDiv.text() : rightDiv != null ? rightDiv.text() : "";
    String pointsClean = DIGITS_ONLY_PATTERN.matcher(pointsText).replaceAll("");

    if (player.isEmpty() || pointsClean.isEmpty()) {
      return null;
    }
    return new ScrapedRow(player, region, Integer.parseInt(pointsClean), rank);
  }

  /**
   * Normalizes a player nickname: NFKD decomposition, diacritic removal, zero-width char removal,
   * collapse whitespace, lowercase.
   *
   * @param value raw nickname
   * @return normalized nickname
   */
  static String normalizeName(String value) {
    if (value == null) {
      return "";
    }
    String nfkd = Normalizer.normalize(value, Normalizer.Form.NFKD);
    String noDiacritics = DIACRITICS_PATTERN.matcher(nfkd).replaceAll("");
    String noZeroWidth = ZERO_WIDTH_PATTERN.matcher(noDiacritics).replaceAll("");
    return noZeroWidth.trim().toLowerCase();
  }

  /**
   * Deduplicates scraped rows by {@code (region|normalizedNickname)} key. When two rows have the
   * same key, keeps the one with the higher {@code points}. Tie-break: lower rank (better
   * placement).
   *
   * @param rows list of scraped rows (may contain duplicates)
   * @return map from dedup key to best row
   */
  Map<String, ScrapedRow> deduplicate(List<ScrapedRow> rows) {
    Map<String, ScrapedRow> result = new HashMap<>();
    for (ScrapedRow row : rows) {
      String key = row.region() + "|" + normalizeName(row.nickname());
      ScrapedRow existing = result.get(key);
      if (existing == null || isBetter(row, existing)) {
        result.put(key, row);
      }
    }
    return result;
  }

  private boolean isBetter(ScrapedRow candidate, ScrapedRow current) {
    if (candidate.points() != current.points()) {
      return candidate.points() > current.points();
    }
    return candidate.rank() < current.rank();
  }
}
