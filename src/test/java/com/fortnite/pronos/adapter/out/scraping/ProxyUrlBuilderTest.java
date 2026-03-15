package com.fortnite.pronos.adapter.out.scraping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProxyUrlBuilder")
class ProxyUrlBuilderTest {

  private ProxyUrlBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new ProxyUrlBuilder();
  }

  @Nested
  @DisplayName("buildTarget()")
  class BuildTarget {

    @Test
    @DisplayName("builds correct FortniteTracker URL with region and page")
    void buildTarget_returnsCorrectUrl() {
      String url = builder.buildTarget("EU", 1, "pc", "year");
      assertThat(url).startsWith("https://fortnitetracker.com/events/powerrankings");
      assertThat(url).contains("platform=pc");
      assertThat(url).contains("region=EU");
      assertThat(url).contains("time=year");
      assertThat(url).contains("page=1");
    }

    @Test
    @DisplayName("encodes special characters in region code")
    void buildTarget_encodesSpecialChars() {
      String url = builder.buildTarget("NA W", 2, "pc", "year");
      assertThat(url).contains("region=NA+W");
      assertThat(url).contains("page=2");
    }
  }

  @Nested
  @DisplayName("build() — Scrapfly")
  class BuildScrapfly {

    @Test
    @DisplayName("builds correct Scrapfly proxy URL with all required params")
    void build_scrapfly_containsAllParams() {
      String url = builder.build("scrapfly", "https://fortnitetracker.com/test", "mykey", 20000);
      assertThat(url).startsWith("https://api.scrapfly.io/scrape");
      assertThat(url).contains("key=mykey");
      assertThat(url).contains("asp=true");
      assertThat(url).contains("render_js=true");
      assertThat(url).contains("country=us");
      assertThat(url).contains("url=");
    }
  }

  @Nested
  @DisplayName("build() — ScraperAPI")
  class BuildScraperapi {

    @Test
    @DisplayName("builds correct ScraperAPI proxy URL with timeout")
    void build_scraperapi_containsAllParams() {
      String url =
          builder.build("scraperapi", "https://fortnitetracker.com/test", "apikey123", 20000);
      assertThat(url).startsWith("https://api.scraperapi.com/");
      assertThat(url).contains("api_key=apikey123");
      assertThat(url).contains("render=false");
      assertThat(url).contains("wait_selector=tbody");
      assertThat(url).contains("timeout=20000");
      assertThat(url).contains("url=");
    }

    @Test
    @DisplayName("encodes the target URL in the proxy URL")
    void build_scraperapi_encodesTargetUrl() {
      String target = "https://fortnitetracker.com/events/powerrankings?platform=pc&region=EU";
      String url = builder.build("scraperapi", target, "key", 10000);
      assertThat(url).doesNotContain("region=EU&");
      assertThat(url).contains("url=");
    }
  }

  @Nested
  @DisplayName("build() — Scrape.do")
  class BuildScrapedo {

    @Test
    @DisplayName("builds correct Scrape.do proxy URL with token")
    void build_scrapedo_containsAllParams() {
      String url = builder.build("scrapedo", "https://fortnitetracker.com/test", "mytoken", 20000);
      assertThat(url).startsWith("http://api.scrape.do/");
      assertThat(url).contains("url=");
      assertThat(url).contains("token=mytoken");
    }
  }
}
