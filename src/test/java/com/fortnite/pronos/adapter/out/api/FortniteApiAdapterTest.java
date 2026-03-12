package com.fortnite.pronos.adapter.out.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.dto.FortniteApiStatsResponse;

@ExtendWith(MockitoExtension.class)
class FortniteApiAdapterTest {

  @Mock private RestTemplate restTemplate;

  private FortniteApiAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new FortniteApiAdapter(restTemplate);
  }

  private void setApiKey(String key) {
    ReflectionTestUtils.setField(adapter, "apiKey", key);
    ReflectionTestUtils.setField(adapter, "baseUrl", "https://fortnite-api.com/v2/stats/br/v2");
  }

  private FortniteApiStatsResponse buildResponse(
      String epicId, String displayName, int wins, int kills, int matches) {
    var overall =
        new FortniteApiStatsResponse.FortniteApiOverallStats(
            0, wins, kills, matches, 2.0, 5.0, 0, 0, 0, 0, 120, 0);
    var input = new FortniteApiStatsResponse.FortniteApiInputNode(overall);
    var stats = new FortniteApiStatsResponse.FortniteApiStatsNode(input);
    var account = new FortniteApiStatsResponse.FortniteApiAccountNode(epicId, displayName);
    var bp = new FortniteApiStatsResponse.FortniteApiBattlePassNode(100, 50);
    var data = new FortniteApiStatsResponse.FortniteApiDataNode(account, bp, stats);
    return new FortniteApiStatsResponse(200, data);
  }

  @Nested
  @DisplayName("searchByName")
  class SearchByName {

    @Test
    void shouldReturnEmptyWhenApiKeyNotConfigured() {
      setApiKey("");
      Optional<FortnitePlayerData> result = adapter.searchByName("PlayerOne");
      assertThat(result).isEmpty();
      verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldReturnPlayerDataWhenApiReturnsOk() {
      setApiKey("test-api-key");
      var response = buildResponse("epic-123", "PlayerOne", 50, 500, 300);
      when(restTemplate.exchange(
              argThat((URI uri) -> uri.toString().contains("name=PlayerOne")),
              any(),
              any(),
              eq(FortniteApiStatsResponse.class)))
          .thenReturn(ResponseEntity.ok(response));

      Optional<FortnitePlayerData> result = adapter.searchByName("PlayerOne");

      assertThat(result).isPresent();
      assertThat(result.get().displayName()).isEqualTo("PlayerOne");
      assertThat(result.get().epicAccountId()).isEqualTo("epic-123");
      assertThat(result.get().wins()).isEqualTo(50);
      assertThat(result.get().battlePassLevel()).isEqualTo(100);
    }

    @Test
    void shouldReturnEmptyWhenPlayerNotFound() {
      setApiKey("test-api-key");
      when(restTemplate.exchange(any(URI.class), any(), any(), eq(FortniteApiStatsResponse.class)))
          .thenThrow(
              HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

      Optional<FortnitePlayerData> result = adapter.searchByName("UnknownPlayer");

      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyOnUnexpectedError() {
      setApiKey("test-api-key");
      when(restTemplate.exchange(any(URI.class), any(), any(), eq(FortniteApiStatsResponse.class)))
          .thenThrow(new RuntimeException("network timeout"));

      Optional<FortnitePlayerData> result = adapter.searchByName("PlayerOne");

      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenHttp401Unauthorized() {
      setApiKey("invalid-key");
      when(restTemplate.exchange(any(URI.class), any(), any(), eq(FortniteApiStatsResponse.class)))
          .thenThrow(
              HttpClientErrorException.create(
                  HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

      Optional<FortnitePlayerData> result = adapter.searchByName("PlayerOne");

      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenResourceAccessTimeout() {
      setApiKey("test-api-key");
      when(restTemplate.exchange(any(URI.class), any(), any(), eq(FortniteApiStatsResponse.class)))
          .thenThrow(new ResourceAccessException("Read timed out"));

      Optional<FortnitePlayerData> result = adapter.searchByName("PlayerOne");

      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnPlayerDataWithZeroStatsWhenStatsNull() {
      setApiKey("test-api-key");
      var account = new FortniteApiStatsResponse.FortniteApiAccountNode("epic-null", "NullStats");
      var bp = new FortniteApiStatsResponse.FortniteApiBattlePassNode(5, 0);
      var data = new FortniteApiStatsResponse.FortniteApiDataNode(account, bp, null);
      var response = new FortniteApiStatsResponse(200, data);
      when(restTemplate.exchange(any(URI.class), any(), any(), eq(FortniteApiStatsResponse.class)))
          .thenReturn(ResponseEntity.ok(response));

      Optional<FortnitePlayerData> result = adapter.searchByName("NullStats");

      assertThat(result).isPresent();
      assertThat(result.get().epicAccountId()).isEqualTo("epic-null");
      assertThat(result.get().wins()).isEqualTo(0);
      assertThat(result.get().kd()).isEqualTo(0.0);
    }

    @Test
    void shouldEncodeUrlForPlayerNameWithSpaces() {
      setApiKey("test-api-key");
      var response = buildResponse("epic-m8", "M8 ak1.", 10, 100, 80);
      when(restTemplate.exchange(
              argThat((URI uri) -> uri.toString().contains("name=M8%20ak1.")),
              any(),
              any(),
              eq(FortniteApiStatsResponse.class)))
          .thenReturn(ResponseEntity.ok(response));

      Optional<FortnitePlayerData> result = adapter.searchByName("M8 ak1.");

      assertThat(result).isPresent();
      assertThat(result.get().displayName()).isEqualTo("M8 ak1.");
    }

    @Test
    void shouldReturnEmptyWhenHttp429RateLimited() {
      setApiKey("test-api-key");
      when(restTemplate.exchange(any(URI.class), any(), any(), eq(FortniteApiStatsResponse.class)))
          .thenThrow(
              HttpClientErrorException.create(
                  org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                  "Too Many Requests",
                  null,
                  null,
                  null));

      Optional<FortnitePlayerData> result = adapter.searchByName("PlayerOne");

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("fetchByEpicId")
  class FetchByEpicId {

    @Test
    void shouldReturnEmptyWhenApiKeyIsBlank() {
      setApiKey("   ");
      Optional<FortnitePlayerData> result = adapter.fetchByEpicId("epic-999");
      assertThat(result).isEmpty();
      verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldReturnPlayerDataWhenApiReturnsOk() {
      setApiKey("test-api-key");
      var response = buildResponse("epic-999", "Player999", 10, 100, 80);
      when(restTemplate.exchange(
              argThat((URI uri) -> uri.toString().contains("accountId=epic-999")),
              any(),
              any(),
              eq(FortniteApiStatsResponse.class)))
          .thenReturn(ResponseEntity.ok(response));

      Optional<FortnitePlayerData> result = adapter.fetchByEpicId("epic-999");

      assertThat(result).isPresent();
      assertThat(result.get().epicAccountId()).isEqualTo("epic-999");
    }

    @Test
    void shouldReturnEmptyWhenApiKeyIsNull() {
      ReflectionTestUtils.setField(adapter, "apiKey", null);
      Optional<FortnitePlayerData> result = adapter.fetchByEpicId("epic-123");
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEpicIdNotFound() {
      setApiKey("test-api-key");
      when(restTemplate.exchange(any(URI.class), any(), any(), eq(FortniteApiStatsResponse.class)))
          .thenThrow(
              HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

      Optional<FortnitePlayerData> result = adapter.fetchByEpicId("unknown-epic-id");

      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenTimeoutOnEpicIdFetch() {
      setApiKey("test-api-key");
      when(restTemplate.exchange(any(URI.class), any(), any(), eq(FortniteApiStatsResponse.class)))
          .thenThrow(new ResourceAccessException("Connection timed out"));

      Optional<FortnitePlayerData> result = adapter.fetchByEpicId("epic-123");

      assertThat(result).isEmpty();
    }

    @Test
    void shouldBuildUrlWithAccountIdParam() {
      setApiKey("test-api-key");
      var response = buildResponse("epic-abc", "PlayerAbc", 5, 50, 40);
      when(restTemplate.exchange(
              argThat(
                  (URI uri) ->
                      uri.toString().contains("accountId=epic-abc")
                          && !uri.toString().contains("timeWindow")),
              any(),
              any(),
              eq(FortniteApiStatsResponse.class)))
          .thenReturn(ResponseEntity.ok(response));

      Optional<FortnitePlayerData> result = adapter.fetchByEpicId("epic-abc");

      assertThat(result).isPresent();
      assertThat(result.get().epicAccountId()).isEqualTo("epic-abc");
    }
  }
}
