package com.fortnite.pronos.service.supabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fortnite.pronos.config.SupabaseProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseRestService {

  private static final String REST_PREFIX = "rest/v1/";

  private final SupabaseProperties supabaseProperties;
  private final RestTemplate restTemplate;

  public <T> List<T> fetchAll(String table, Class<T[]> responseType) {
    return fetch(table, "*", Collections.emptyMap(), responseType);
  }

  public <T> List<T> fetch(
      String table, String select, Map<String, String> filters, Class<T[]> responseType) {
    if (!supabaseProperties.isConfigured()) {
      log.warn("Supabase not configured; skip fetching {}", table);
      return List.of();
    }

    String baseUrl = normalizeBaseUrl(supabaseProperties.getUrl());
    if (baseUrl == null) {
      log.warn("Supabase url is empty; skip fetching {}", table);
      return List.of();
    }

    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(baseUrl + REST_PREFIX + table)
            .queryParam("select", select == null || select.isBlank() ? "*" : select);
    for (Map.Entry<String, String> entry : filters.entrySet()) {
      builder.queryParam(entry.getKey(), entry.getValue());
    }

    String url = builder.build(true).toUriString();
    HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());

    try {
      ResponseEntity<T[]> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
      T[] body = response.getBody();
      if (body == null) {
        return List.of();
      }
      return Arrays.asList(body);
    } catch (RestClientException ex) {
      log.warn("Supabase fetch failed for {}: {}", table, ex.getMessage());
      return List.of();
    }
  }

  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return null;
    }
    return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
  }

  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("apikey", supabaseProperties.getAnonKey());
    headers.set("Authorization", "Bearer " + supabaseProperties.getAnonKey());
    headers.set("Content-Type", "application/json");

    String schema = supabaseProperties.getSchema();
    if (schema != null && !schema.isBlank()) {
      headers.set("Accept-Profile", schema);
      headers.set("Content-Profile", schema);
    }

    return headers;
  }
}
