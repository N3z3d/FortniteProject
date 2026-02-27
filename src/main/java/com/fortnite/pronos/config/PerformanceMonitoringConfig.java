package com.fortnite.pronos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/** SPRINT 4 - ADVANCED PERFORMANCE MONITORING FOR 500+ USERS. */
@Configuration
@EnableScheduling
@Slf4j
public class PerformanceMonitoringConfig {

  @Bean
  public PerformanceMonitor performanceMonitor(MeterRegistry meterRegistry) {
    return new PerformanceMonitor(meterRegistry);
  }
}
