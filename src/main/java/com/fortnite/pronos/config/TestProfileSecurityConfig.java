package com.fortnite.pronos.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration de securite pour le profil test. Permet l'authentification via l'en-tete
 * X-Test-User pour les tests d'integration.
 */
@Configuration
@EnableWebSecurity
@Profile("test")
public class TestProfileSecurityConfig {

  private final ObjectProvider<TestFallbackAuthenticationFilter> testFallbackFilter;

  public TestProfileSecurityConfig(
      ObjectProvider<TestFallbackAuthenticationFilter> testFallbackFilter) {
    this.testFallbackFilter = testFallbackFilter;
  }

  @Bean
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
                    .requestMatchers("/api/games", "/api/games/**", "/api/leaderboard")
                    .permitAll()
                    .requestMatchers("/api/players/**")
                    .permitAll()
                    .requestMatchers("/api/teams/**")
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
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
