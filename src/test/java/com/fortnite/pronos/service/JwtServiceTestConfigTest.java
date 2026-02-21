package com.fortnite.pronos.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de test minimale pour JwtServiceTest Désactive la sécurité mais permet au vrai
 * JwtService de fonctionner
 */
@TestConfiguration
@EnableWebSecurity
class JwtServiceTestConfigTest {

  @Bean
  @Primary
  SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }

  /** PasswordEncoder pour les tests */
  @Bean
  @Primary
  PasswordEncoder testPasswordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Test
  void shouldDeclareTestConfigurationAnnotation() {
    assertTrue(JwtServiceTestConfigTest.class.isAnnotationPresent(TestConfiguration.class));
  }
}
