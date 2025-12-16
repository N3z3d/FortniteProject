package com.fortnite.pronos.config;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import lombok.RequiredArgsConstructor;

/**
 * Expose un endpoint Prometheus minimal en profil test pour satisfaire les vérifications
 * d'intégration.
 */
@RestController
@Profile("test")
@RequiredArgsConstructor
@RequestMapping("/actuator/prometheus")
public class TestPrometheusEndpointController {

  private final PrometheusMeterRegistry prometheusMeterRegistry;

  @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> scrape() {
    return ResponseEntity.ok(prometheusMeterRegistry.scrape());
  }
}
