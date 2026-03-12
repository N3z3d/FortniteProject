package com.fortnite.pronos.adapter.out.api;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.FortniteApiPort;
import com.fortnite.pronos.dto.FortniteApiStatsResponse;
import com.fortnite.pronos.dto.FortniteApiStatsResponse.FortniteApiDataNode;
import com.fortnite.pronos.dto.FortniteApiStatsResponse.FortniteApiOverallStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter for Fortnite-API.com /v2/stats/br/v2. Returns empty Optional when the API key is not
 * configured, the player is not found, or stats are private.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FortniteApiAdapter implements FortniteApiPort {

  private final RestTemplate restTemplate;

  @Value("${fortnite.api.key:}")
  private String apiKey;

  @Value("${fortnite.api.url:https://fortnite-api.com/v2/stats/br/v2}")
  private String baseUrl;

  @Override
  public Optional<FortnitePlayerData> searchByName(String playerName) {
    if (!isConfigured()) {
      log.debug("Fortnite API key not configured — skipping lookup for '{}'", playerName);
      return Optional.empty();
    }
    URI uri =
        UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("name", playerName)
            .queryParam("timeWindow", "lifetime")
            .build()
            .encode()
            .toUri();
    return fetchFromApi(uri, playerName);
  }

  @Override
  public Optional<FortnitePlayerData> fetchByEpicId(String epicAccountId) {
    if (!isConfigured()) {
      log.debug(
          "Fortnite API key not configured — skipping lookup for epic ID '{}'", epicAccountId);
      return Optional.empty();
    }
    URI uri =
        UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("accountId", epicAccountId)
            .build()
            .encode()
            .toUri();
    return fetchFromApi(uri, epicAccountId);
  }

  private boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
  }

  private Optional<FortnitePlayerData> fetchFromApi(URI uri, String identifier) {
    try {
      ResponseEntity<FortniteApiStatsResponse> response =
          restTemplate.exchange(
              uri, HttpMethod.GET, buildRequest(), FortniteApiStatsResponse.class);
      if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
        return Optional.ofNullable(mapToPlayerData(response.getBody()));
      }
      return Optional.empty();
    } catch (ResourceAccessException e) {
      log.warn("Fortnite API timeout or connection error for '{}': {}", identifier, e.getMessage());
      return Optional.empty();
    } catch (HttpClientErrorException e) {
      int status = e.getStatusCode().value();
      if (status == HttpStatus.NOT_FOUND.value()) {
        log.debug("Player not found in Fortnite API: {}", identifier);
      } else if (status == HttpStatus.UNAUTHORIZED.value()) {
        log.warn("Fortnite API key invalid or unauthorized for '{}'", identifier);
      } else if (status == 429) {
        log.warn("Fortnite API rate limit reached, skipping '{}'", identifier);
      } else {
        log.warn(
            "Fortnite API error for '{}': {} {}", identifier, e.getStatusCode(), e.getMessage());
      }
      return Optional.empty();
    } catch (Exception e) {
      log.warn("Unexpected Fortnite API error for '{}': {}", identifier, e.getMessage());
      return Optional.empty();
    }
  }

  private HttpEntity<Void> buildRequest() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", apiKey);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    return new HttpEntity<>(headers);
  }

  private FortnitePlayerData mapToPlayerData(FortniteApiStatsResponse response) {
    FortniteApiDataNode data = response.data();
    if (data == null || data.account() == null) {
      return null;
    }
    int battlePassLevel = data.battlePass() != null ? data.battlePass().level() : 0;
    FortniteApiOverallStats overall = extractOverall(data);
    if (overall == null) {
      return new FortnitePlayerData(
          data.account().id(), data.account().name(), battlePassLevel, 0, 0, 0, 0.0, 0.0, 0);
    }
    return new FortnitePlayerData(
        data.account().id(),
        data.account().name(),
        battlePassLevel,
        overall.wins(),
        overall.kills(),
        overall.matches(),
        overall.kd(),
        overall.winRate(),
        overall.minutesPlayed());
  }

  private FortniteApiOverallStats extractOverall(FortniteApiDataNode data) {
    if (data.stats() == null || data.stats().all() == null) {
      return null;
    }
    return data.stats().all().overall();
  }
}
