package com.fortnite.pronos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** HTTP client configuration — RestTemplate with connect and read timeouts. */
@Configuration
public class RestTemplateConfig {

  private static final int HTTP_TIMEOUT_MS = 10_000;

  @Bean
  public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(HTTP_TIMEOUT_MS);
    factory.setReadTimeout(HTTP_TIMEOUT_MS);
    return new RestTemplate(factory);
  }
}
