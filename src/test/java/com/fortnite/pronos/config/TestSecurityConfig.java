package com.fortnite.pronos.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fortnite.pronos.service.JwtService;

/**
 * Configuration de test simplifiee : on garde une securite minimale (stateless) et on permet
 * l'authentification via l'en-tete X-Test-User pour les suites d'integration. Aucun bypass global
 * de la securite : sans utilisateur, les endpoints proteges retournent 401.
 */
@org.springframework.context.annotation.Configuration
@EnableWebSecurity
@org.springframework.context.annotation.Profile("test")
public class TestSecurityConfig {

  private final ObjectProvider<TestFallbackAuthenticationFilter> testFallbackFilter;

  public TestSecurityConfig(ObjectProvider<TestFallbackAuthenticationFilter> testFallbackFilter) {
    this.testFallbackFilter = testFallbackFilter;
  }

  @Bean
  @Primary
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/draft/**")
                    .permitAll()
                    .requestMatchers("/api/trade-form-data")
                    .permitAll()
                    // Allow read-only game endpoints for integration tests
                    .requestMatchers("/api/games", "/api/games/available", "/api/leaderboard")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    TestFallbackAuthenticationFilter fallback = testFallbackFilter.getIfAvailable();
    if (fallback != null) {
      http.addFilterBefore(fallback, UsernamePasswordAuthenticationFilter.class);
    }

    return http.build();
  }

  @Bean
  @Primary
  public JwtService testJwtService() {
    JwtService mockJwtService = org.mockito.Mockito.mock(JwtService.class);

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

  /** PasswordEncoder pour les tests (necessaire pour l'initialisation des donnees) */
  @Bean
  @Primary
  public PasswordEncoder testPasswordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
