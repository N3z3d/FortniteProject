package com.fortnite.pronos.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.fortnite.pronos.service.JwtService;

/**
 * Configuration de test qui désactive complètement Spring Security Utilisée uniquement pour les
 * tests unitaires et d'intégration
 *
 * <p>IMPORTANT: Cette configuration remplace JwtService par un mock pour éviter les problèmes
 * de @PostConstruct avec Environment null dans les tests.
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfigTestBackup {

  @Bean
  @Primary
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }

  /**
   * JwtService de test qui remplace complètement l'implémentation originale Évite tous les
   * problèmes de @PostConstruct et de configuration.
   *
   * <p>IMPORTANT: Utilise un mock Mockito pour éviter complètement les problèmes d'héritage et
   * de @PostConstruct de la classe JwtService originale.
   */
  @Bean
  @Primary
  public JwtService testJwtService() {
    JwtService mockJwtService = org.mockito.Mockito.mock(JwtService.class);

    // Configure le comportement basique pour tous les tests
    org.mockito.Mockito.when(mockJwtService.extractUsername(org.mockito.Mockito.anyString()))
        .thenReturn("testuser");
    org.mockito.Mockito.when(
            mockJwtService.generateToken(org.mockito.Mockito.any(UserDetails.class)))
        .thenReturn("test-token");
    org.mockito.Mockito.when(
            mockJwtService.isTokenValid(
                org.mockito.Mockito.anyString(), org.mockito.Mockito.any(UserDetails.class)))
        .thenReturn(true);
    org.mockito.Mockito.when(
            mockJwtService.generateRefreshToken(org.mockito.Mockito.any(UserDetails.class)))
        .thenReturn("test-refresh-token");
    org.mockito.Mockito.when(
            mockJwtService.generateToken(
                org.mockito.Mockito.anyMap(), org.mockito.Mockito.any(UserDetails.class)))
        .thenReturn("test-token-with-claims");

    return mockJwtService;
  }

  /** PasswordEncoder pour les tests Nécessaire pour DataInitializationService */
  @Bean
  @Primary
  public PasswordEncoder testPasswordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
