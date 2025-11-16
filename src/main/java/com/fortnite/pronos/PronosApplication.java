package com.fortnite.pronos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.fortnite.pronos.util.LoggingUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class PronosApplication {

  private final Environment environment;

  public static void main(String[] args) {
    SpringApplication.run(PronosApplication.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    String version = getClass().getPackage().getImplementationVersion();
    String[] activeProfiles = environment.getActiveProfiles();
    String profile = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";

    LoggingUtils.logApplicationStart(
        "Fortnite Pronos API", version != null ? version : "DEVELOPMENT", profile);

    log.info("🚀 Application démarrée avec succès sur le profil: {}", profile);
    log.info("📊 Base de données: {}", environment.getProperty("spring.datasource.url"));
    log.info("🌐 Port: {}", environment.getProperty("server.port", "8080"));
  }

  @EventListener(ContextClosedEvent.class)
  public void onApplicationShutdown() {
    LoggingUtils.logApplicationShutdown("Fortnite Pronos API");
    log.info("🛑 Application arrêtée proprement");
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
