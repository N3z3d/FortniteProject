package com.fortnite.pronos.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Fournit un PasswordEncoder pour les profils de test lorsque la configuration de sǩcuritǩ est
 * dǩsactivǩe. Évite les erreurs d'injection dans les services (DataInitializationService, etc.).
 */
@Configuration
@Profile("test")
public class TestPasswordEncoderConfig {

  @Bean
  @ConditionalOnMissingBean(PasswordEncoder.class)
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
