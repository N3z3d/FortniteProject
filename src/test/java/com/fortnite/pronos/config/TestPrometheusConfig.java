package com.fortnite.pronos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

@Configuration
@Profile("test")
public class TestPrometheusConfig {

  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry() {
    PrometheusRegistry coreRegistry = new PrometheusRegistry();
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, coreRegistry, Clock.SYSTEM);
  }
}
