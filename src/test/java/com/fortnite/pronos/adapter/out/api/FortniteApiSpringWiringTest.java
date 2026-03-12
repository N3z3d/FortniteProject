package com.fortnite.pronos.adapter.out.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.config.RestTemplateConfig;
import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.service.catalogue.FortnitePlayerSearchService;

/**
 * Spring wiring test — validates that FortniteApiAdapter is correctly wired:
 * RestTemplateConfig @Bean → @Value injection into FortniteApiAdapter → FortnitePlayerSearchService
 * delegates through the port. Uses a minimal Spring context (only the relevant beans) +
 * MockRestServiceServer.
 */
@SpringJUnitConfig(
    classes = {
      RestTemplateConfig.class,
      FortniteApiAdapter.class,
      FortnitePlayerSearchService.class
    })
@TestPropertySource(
    properties = {
      "fortnite.api.key=test-spring-wiring-key",
      "fortnite.api.url=https://fortnite-api.com/v2/stats/br/v2"
    })
@DisplayName("FortniteApiAdapter — Spring context wiring")
class FortniteApiSpringWiringTest {

  private static final String BUGHA_JSON =
      """
      {
        "status": 200,
        "data": {
          "account": { "id": "33f85e8ed7124d15ae29cfaf53340239", "name": "Bugha" },
          "battlePass": { "level": 20, "progress": 76 },
          "stats": {
            "all": {
              "overall": {
                "score": 1871653, "wins": 373, "kills": 28502, "matches": 8911,
                "kd": 3.338, "winRate": 4.186, "top3": 0, "top5": 0,
                "top10": 0, "top25": 0, "minutesPlayed": 124111, "playersOutlived": 334400
              }
            }
          }
        }
      }
      """;

  @Autowired private FortnitePlayerSearchService fortnitePlayerSearchService;

  @Autowired private RestTemplate restTemplate;

  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  @DisplayName("Spring-wired adapter returns player data through FortnitePlayerSearchService")
  void shouldReturnPlayerData_whenSpringContextWired() {
    mockServer
        .expect(requestTo(Matchers.containsString("name=Bugha")))
        .andExpect(requestTo(Matchers.containsString("timeWindow=lifetime")))
        .andRespond(withSuccess(BUGHA_JSON, MediaType.APPLICATION_JSON));

    Optional<FortnitePlayerData> result = fortnitePlayerSearchService.searchByName("Bugha");

    assertThat(result).isPresent();
    assertThat(result.get().epicAccountId()).isEqualTo("33f85e8ed7124d15ae29cfaf53340239");
    assertThat(result.get().displayName()).isEqualTo("Bugha");
    assertThat(result.get().wins()).isEqualTo(373);
    assertThat(result.get().kd()).isEqualTo(3.338);
    mockServer.verify();
  }

  @Test
  @DisplayName("Spring-wired adapter encodes spaces in player name")
  void shouldEncodeSpaces_whenPlayerNameHasSpaces() {
    mockServer
        .expect(requestTo(Matchers.containsString("name=M8%20ak1.")))
        .andRespond(withSuccess(BUGHA_JSON, MediaType.APPLICATION_JSON));

    Optional<FortnitePlayerData> result = fortnitePlayerSearchService.searchByName("M8 ak1.");

    assertThat(result).isPresent();
    mockServer.verify();
  }
}
