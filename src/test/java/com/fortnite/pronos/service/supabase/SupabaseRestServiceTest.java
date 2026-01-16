package com.fortnite.pronos.service.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.config.SupabaseProperties;

@ExtendWith(MockitoExtension.class)
class SupabaseRestServiceTest {

  @Mock private SupabaseProperties supabaseProperties;
  @Mock private RestTemplate restTemplate;
  @Captor private ArgumentCaptor<String> urlCaptor;
  @Captor private ArgumentCaptor<HttpEntity<Void>> entityCaptor;

  private SupabaseRestService supabaseRestService;

  @BeforeEach
  void setUp() {
    supabaseRestService = new SupabaseRestService(supabaseProperties, restTemplate);
  }

  @Test
  void returnsEmptyListWhenNotConfigured() {
    when(supabaseProperties.isConfigured()).thenReturn(false);

    List<TestDto> result = supabaseRestService.fetchAll("players", TestDto[].class);

    assertThat(result).isEmpty();
    verify(restTemplate, never()).exchange(any(), any(HttpMethod.class), any(), any(Class.class));
  }

  @Test
  void returnsEmptyListWhenUrlIsNull() {
    when(supabaseProperties.isConfigured()).thenReturn(true);
    when(supabaseProperties.getUrl()).thenReturn(null);

    List<TestDto> result = supabaseRestService.fetchAll("players", TestDto[].class);

    assertThat(result).isEmpty();
  }

  @Test
  void returnsEmptyListWhenUrlIsBlank() {
    when(supabaseProperties.isConfigured()).thenReturn(true);
    when(supabaseProperties.getUrl()).thenReturn("   ");

    List<TestDto> result = supabaseRestService.fetchAll("players", TestDto[].class);

    assertThat(result).isEmpty();
  }

  @Test
  void fetchAllBuildsCorrectUrl() {
    setupMocksForSuccessfulRequest();
    when(restTemplate.exchange(
            urlCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(), eq(TestDto[].class)))
        .thenReturn(new ResponseEntity<>(new TestDto[] {}, HttpStatus.OK));

    supabaseRestService.fetchAll("players", TestDto[].class);

    assertThat(urlCaptor.getValue())
        .isEqualTo("https://example.supabase.co/rest/v1/players?select=*");
  }

  @Test
  void fetchWithFiltersBuildsCorrectUrl() {
    setupMocksForSuccessfulRequest();
    when(restTemplate.exchange(
            urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(TestDto[].class)))
        .thenReturn(new ResponseEntity<>(new TestDto[] {}, HttpStatus.OK));

    supabaseRestService.fetch("players", "id,name", Map.of("region", "eq.EU"), TestDto[].class);

    String url = urlCaptor.getValue();
    assertThat(url).contains("rest/v1/players");
    assertThat(url).contains("select=id,name");
    assertThat(url).contains("region=eq.EU");
  }

  @Test
  void setsRequiredHeaders() {
    setupMocksForSuccessfulRequest();
    when(supabaseProperties.getAnonKey()).thenReturn("test-anon-key");
    when(supabaseProperties.getSchema()).thenReturn("custom_schema");
    when(restTemplate.exchange(
            any(String.class), eq(HttpMethod.GET), entityCaptor.capture(), eq(TestDto[].class)))
        .thenReturn(new ResponseEntity<>(new TestDto[] {}, HttpStatus.OK));

    supabaseRestService.fetchAll("players", TestDto[].class);

    HttpHeaders headers = entityCaptor.getValue().getHeaders();
    assertThat(headers.getFirst("apikey")).isEqualTo("test-anon-key");
    assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer test-anon-key");
    assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json");
    assertThat(headers.getFirst("Accept-Profile")).isEqualTo("custom_schema");
    assertThat(headers.getFirst("Content-Profile")).isEqualTo("custom_schema");
  }

  @Test
  void omitsSchemaHeadersWhenSchemaIsBlank() {
    when(supabaseProperties.isConfigured()).thenReturn(true);
    when(supabaseProperties.getUrl()).thenReturn("https://example.supabase.co/");
    when(supabaseProperties.getAnonKey()).thenReturn("test-key");
    when(supabaseProperties.getSchema()).thenReturn("");
    when(restTemplate.exchange(
            any(String.class), eq(HttpMethod.GET), entityCaptor.capture(), eq(TestDto[].class)))
        .thenReturn(new ResponseEntity<>(new TestDto[] {}, HttpStatus.OK));

    supabaseRestService.fetchAll("players", TestDto[].class);

    HttpHeaders headers = entityCaptor.getValue().getHeaders();
    assertThat(headers.getFirst("Accept-Profile")).isNull();
    assertThat(headers.getFirst("Content-Profile")).isNull();
  }

  @Test
  void returnsDataFromSuccessfulResponse() {
    setupMocksForSuccessfulRequest();
    TestDto dto1 = new TestDto("1", "Player1");
    TestDto dto2 = new TestDto("2", "Player2");
    when(restTemplate.exchange(
            any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(TestDto[].class)))
        .thenReturn(new ResponseEntity<>(new TestDto[] {dto1, dto2}, HttpStatus.OK));

    List<TestDto> result = supabaseRestService.fetchAll("players", TestDto[].class);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).id()).isEqualTo("1");
    assertThat(result.get(1).name()).isEqualTo("Player2");
  }

  @Test
  void returnsEmptyListWhenResponseBodyIsNull() {
    setupMocksForSuccessfulRequest();
    when(restTemplate.exchange(
            any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(TestDto[].class)))
        .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

    List<TestDto> result = supabaseRestService.fetchAll("players", TestDto[].class);

    assertThat(result).isEmpty();
  }

  @Test
  void returnsEmptyListOnRestClientException() {
    setupMocksForSuccessfulRequest();
    when(restTemplate.exchange(
            any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(TestDto[].class)))
        .thenThrow(new RestClientException("Connection refused"));

    List<TestDto> result = supabaseRestService.fetchAll("players", TestDto[].class);

    assertThat(result).isEmpty();
  }

  @Test
  void normalizesUrlWithoutTrailingSlash() {
    when(supabaseProperties.isConfigured()).thenReturn(true);
    when(supabaseProperties.getUrl()).thenReturn("https://example.supabase.co");
    when(supabaseProperties.getAnonKey()).thenReturn("key");
    when(supabaseProperties.getSchema()).thenReturn("public");
    when(restTemplate.exchange(
            urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(TestDto[].class)))
        .thenReturn(new ResponseEntity<>(new TestDto[] {}, HttpStatus.OK));

    supabaseRestService.fetchAll("players", TestDto[].class);

    assertThat(urlCaptor.getValue()).startsWith("https://example.supabase.co/rest/v1/");
  }

  @Test
  void usesDefaultSelectWhenBlank() {
    setupMocksForSuccessfulRequest();
    when(restTemplate.exchange(
            urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(TestDto[].class)))
        .thenReturn(new ResponseEntity<>(new TestDto[] {}, HttpStatus.OK));

    supabaseRestService.fetch("players", "  ", Map.of(), TestDto[].class);

    assertThat(urlCaptor.getValue()).contains("select=*");
  }

  private void setupMocksForSuccessfulRequest() {
    when(supabaseProperties.isConfigured()).thenReturn(true);
    when(supabaseProperties.getUrl()).thenReturn("https://example.supabase.co/");
    when(supabaseProperties.getAnonKey()).thenReturn("test-key");
    when(supabaseProperties.getSchema()).thenReturn("public");
  }

  record TestDto(String id, String name) {}
}
