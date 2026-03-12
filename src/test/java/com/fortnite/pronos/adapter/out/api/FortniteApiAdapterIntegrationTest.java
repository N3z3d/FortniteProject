package com.fortnite.pronos.adapter.out.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;

/**
 * Integration test verifying FortniteApiAdapter wiring: RestTemplate → URL building → response
 * mapping. Uses MockRestServiceServer (Spring Test built-in, no additional deps).
 */
@DisplayName("FortniteApiAdapter Integration — Spring RestTemplate wiring")
class FortniteApiAdapterIntegrationTest {

  private static final String BASE_URL = "https://fortnite-api.com/v2/stats/br/v2";
  private static final String TEST_KEY = "test-integration-key";

  private static final String BUGHA_RESPONSE =
      """
      {
        "status": 200,
        "data": {
          "account": {
            "id": "33f85e8ed7124d15ae29cfaf53340239",
            "name": "Bugha"
          },
          "battlePass": { "level": 20, "progress": 76 },
          "stats": {
            "all": {
              "overall": {
                "score": 1871653,
                "wins": 373,
                "kills": 28502,
                "matches": 8911,
                "kd": 3.338,
                "winRate": 4.186,
                "top3": 0, "top5": 0, "top10": 0, "top25": 0,
                "minutesPlayed": 124111,
                "playersOutlived": 334400
              }
            }
          }
        }
      }
      """;

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;
  private FortniteApiAdapter adapter;

  @BeforeEach
  void setUp() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(10_000);
    factory.setReadTimeout(10_000);
    restTemplate = new RestTemplate(factory);
    mockServer = MockRestServiceServer.createServer(restTemplate);
    adapter = new FortniteApiAdapter(restTemplate);
    ReflectionTestUtils.setField(adapter, "apiKey", TEST_KEY);
    ReflectionTestUtils.setField(adapter, "baseUrl", BASE_URL);
  }

  @Nested
  @DisplayName("URL encoding")
  class UrlEncoding {

    @Test
    @DisplayName("simple name builds correct URL with timeWindow=lifetime")
    void shouldBuildUrlWithTimeWindowForSimpleName() {
      mockServer
          .expect(requestTo(Matchers.containsString("name=Bugha")))
          .andExpect(requestTo(Matchers.containsString("timeWindow=lifetime")))
          .andRespond(withSuccess(BUGHA_RESPONSE, MediaType.APPLICATION_JSON));

      Optional<FortnitePlayerData> result = adapter.searchByName("Bugha");

      assertThat(result).isPresent();
      assertThat(result.get().epicAccountId()).isEqualTo("33f85e8ed7124d15ae29cfaf53340239");
      assertThat(result.get().displayName()).isEqualTo("Bugha");
      assertThat(result.get().wins()).isEqualTo(373);
      mockServer.verify();
    }

    @Test
    @DisplayName("name with spaces is percent-encoded in URL")
    void shouldPercentEncodeSpacesInPlayerName() {
      mockServer
          .expect(requestTo(Matchers.containsString("name=M8%20ak1.")))
          .andRespond(withSuccess(BUGHA_RESPONSE, MediaType.APPLICATION_JSON));

      adapter.searchByName("M8 ak1.");

      mockServer.verify();
    }

    @Test
    @DisplayName("accountId builds URL without timeWindow param")
    void shouldBuildUrlWithAccountIdOnly() {
      mockServer
          .expect(requestTo(Matchers.containsString("accountId=33f85e8ed7124d15ae29cfaf53340239")))
          .andRespond(withSuccess(BUGHA_RESPONSE, MediaType.APPLICATION_JSON));

      adapter.fetchByEpicId("33f85e8ed7124d15ae29cfaf53340239");

      mockServer.verify();
    }
  }

  @Nested
  @DisplayName("Response mapping")
  class ResponseMapping {

    @Test
    @DisplayName("maps full Bugha response to FortnitePlayerData")
    void shouldMapFullResponseToPlayerData() {
      mockServer
          .expect(requestTo(Matchers.containsString("name=Bugha")))
          .andRespond(withSuccess(BUGHA_RESPONSE, MediaType.APPLICATION_JSON));

      Optional<FortnitePlayerData> result = adapter.searchByName("Bugha");

      assertThat(result).isPresent();
      FortnitePlayerData data = result.get();
      assertThat(data.epicAccountId()).isEqualTo("33f85e8ed7124d15ae29cfaf53340239");
      assertThat(data.displayName()).isEqualTo("Bugha");
      assertThat(data.wins()).isEqualTo(373);
      assertThat(data.kills()).isEqualTo(28502);
      assertThat(data.matches()).isEqualTo(8911);
      assertThat(data.kd()).isEqualTo(3.338);
      assertThat(data.winRate()).isEqualTo(4.186);
      assertThat(data.battlePassLevel()).isEqualTo(20);
    }

    @Test
    @DisplayName("returns empty on 404 from mock server")
    void shouldReturnEmptyOn404() {
      mockServer
          .expect(requestTo(Matchers.containsString("name=Unknown")))
          .andRespond(withStatus(HttpStatus.NOT_FOUND));

      Optional<FortnitePlayerData> result = adapter.searchByName("Unknown");

      assertThat(result).isEmpty();
      mockServer.verify();
    }

    @Test
    @DisplayName("returns empty on 401 from mock server")
    void shouldReturnEmptyOn401() {
      mockServer
          .expect(requestTo(Matchers.containsString("name=Bugha")))
          .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

      Optional<FortnitePlayerData> result = adapter.searchByName("Bugha");

      assertThat(result).isEmpty();
      mockServer.verify();
    }
  }
}
