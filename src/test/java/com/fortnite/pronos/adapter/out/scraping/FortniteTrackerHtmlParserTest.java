package com.fortnite.pronos.adapter.out.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@DisplayName("FortniteTrackerHtmlParser")
class FortniteTrackerHtmlParserTest {

  private FortniteTrackerHtmlParser parser;

  @BeforeEach
  void setUp() {
    parser = new FortniteTrackerHtmlParser();
  }

  @Nested
  @DisplayName("parse()")
  class ParseTests {

    @Test
    @DisplayName("nominal: parses 3 rows from fixture HTML file")
    void parse_nominalFixture_returns3Rows() throws IOException {
      String html = loadFixture("scraping/ft-leaderboard-eu-page1.html");
      List<ScrapedRow> rows = parser.parse(html, "EU", 0);
      assertThat(rows).hasSize(3);
      assertThat(rows.get(0).nickname()).isEqualTo("Bugha");
      assertThat(rows.get(0).points()).isEqualTo(12500);
      assertThat(rows.get(0).rank()).isEqualTo(1);
      assertThat(rows.get(1).nickname()).isEqualTo("Aqua");
      assertThat(rows.get(1).points()).isEqualTo(11200);
      assertThat(rows.get(2).nickname()).isEqualTo("Clix");
      assertThat(rows.get(2).points()).isEqualTo(9800);
    }

    @Test
    @DisplayName("malformed row with missing nickname is skipped")
    void parse_missingNickname_rowIsSkipped() {
      String html =
          "<table><tbody>"
              + "<tr><td class='leaderboard-rank' placement='1'>1</td>"
              + "<td></td>"
              + "<td class='column--highlight'><div>5000</div></td></tr>"
              + "</tbody></table>";
      List<ScrapedRow> rows = parser.parse(html, "EU", 0);
      assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("empty tbody returns empty list")
    void parse_emptyTbody_returnsEmpty() {
      String html = "<table><tbody></tbody></table>";
      List<ScrapedRow> rows = parser.parse(html, "EU", 0);
      assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("uses ordinal offset when placement attribute is absent")
    void parse_noPlacementAttr_usesOrdinalOffset() {
      String html =
          "<table><tbody>"
              + "<tr><td class='leaderboard-rank'>1</td>"
              + "<td><span class='leaderboard-user__nickname'>Player1</span></td>"
              + "<td class='column--highlight'><div>3000</div></td></tr>"
              + "</tbody></table>";
      List<ScrapedRow> rows = parser.parse(html, "EU", 100);
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).rank()).isEqualTo(101);
    }

    @Test
    @DisplayName("fallback to column--right when column--highlight is absent")
    void parse_fallbackToColumnRight() {
      String html =
          "<table><tbody>"
              + "<tr><td class='leaderboard-rank' placement='5'>5</td>"
              + "<td><span class='leaderboard-user__nickname'>TestPlayer</span></td>"
              + "<td class='column--right'><div>7,500</div></td></tr>"
              + "</tbody></table>";
      List<ScrapedRow> rows = parser.parse(html, "NAW", 0);
      assertThat(rows).hasSize(1);
      assertThat(rows.get(0).points()).isEqualTo(7500);
    }
  }

  @Nested
  @DisplayName("normalizeName()")
  class NormalizeNameTests {

    @Test
    @DisplayName("lowercases and removes diacritics")
    void normalizeName_diacritics() {
      assertThat(FortniteTrackerHtmlParser.normalizeName("Ñoño")).isEqualTo("nono");
    }

    @Test
    @DisplayName("handles null gracefully")
    void normalizeName_null_returnsEmpty() {
      assertThat(FortniteTrackerHtmlParser.normalizeName(null)).isEmpty();
    }

    @Test
    @DisplayName("removes zero-width characters")
    void normalizeName_zeroWidth() {
      String withZeroWidth = "Player\u200BName";
      assertThat(FortniteTrackerHtmlParser.normalizeName(withZeroWidth)).isEqualTo("playername");
    }

    @Test
    @DisplayName("plain ascii stays unchanged (lowercased)")
    void normalizeName_plain_lowercases() {
      assertThat(FortniteTrackerHtmlParser.normalizeName("Bugha")).isEqualTo("bugha");
    }
  }

  @Nested
  @DisplayName("deduplicate()")
  class DeduplicateTests {

    @Test
    @DisplayName("keeps row with higher points when duplicate key")
    void deduplicate_keepsHigherPoints() {
      List<ScrapedRow> rows =
          List.of(new ScrapedRow("Bugha", "EU", 10000, 1), new ScrapedRow("BUGHA", "EU", 12000, 2));
      Map<String, ScrapedRow> result = parser.deduplicate(rows);
      assertThat(result).hasSize(1);
      assertThat(result.values().iterator().next().points()).isEqualTo(12000);
    }

    @Test
    @DisplayName("tie-break on rank: keeps lower rank (better placement)")
    void deduplicate_tieBreakOnRank() {
      List<ScrapedRow> rows =
          List.of(
              new ScrapedRow("Player", "EU", 5000, 10), new ScrapedRow("player", "EU", 5000, 3));
      Map<String, ScrapedRow> result = parser.deduplicate(rows);
      assertThat(result).hasSize(1);
      assertThat(result.values().iterator().next().rank()).isEqualTo(3);
    }

    @Test
    @DisplayName("different regions are not deduplicated against each other")
    void deduplicate_differentRegions_notMerged() {
      List<ScrapedRow> rows =
          List.of(new ScrapedRow("Bugha", "EU", 10000, 1), new ScrapedRow("Bugha", "NAW", 9000, 2));
      Map<String, ScrapedRow> result = parser.deduplicate(rows);
      assertThat(result).hasSize(2);
    }
  }

  private String loadFixture(String resourcePath) throws IOException {
    return new String(
        new ClassPathResource(resourcePath).getInputStream().readAllBytes(),
        StandardCharsets.UTF_8);
  }
}
