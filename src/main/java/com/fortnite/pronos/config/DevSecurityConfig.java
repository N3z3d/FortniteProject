package com.fortnite.pronos.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration de sécurité pour le développement Permet l'accès non authentifié aux API pour
 * faciliter le développement
 */
@Configuration
@EnableWebSecurity
@Profile({"dev", "h2", "test"})
@Order(1)
public class DevSecurityConfig {

  /** Configuration de sécurité permissive pour le développement */
  @Bean
  public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
    http
        // Désactiver CSRF pour les API REST
        .csrf(AbstractHttpConfigurer::disable)

        // Configuration CORS
        .cors(cors -> cors.configurationSource(devCorsConfigurationSource()))

        // Permettre tous les accès en développement
        .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())

        // Configuration des sessions
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Headers de sécurité moins restrictifs pour le développement
        .headers(
            headers ->
                headers
                    .frameOptions()
                    .sameOrigin() // Permet H2 console
                    .httpStrictTransportSecurity()
                    .disable() // Pas de HTTPS obligatoire en dev
            );

    return http.build();
  }

  /** Configuration CORS permissive pour le développement */
  @Bean
  public CorsConfigurationSource devCorsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Origins autorisés en développement
    configuration.setAllowedOriginPatterns(
        Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*"));

    // Toutes les méthodes autorisées
    configuration.setAllowedMethods(Arrays.asList("*"));

    // Tous les headers autorisés
    configuration.setAllowedHeaders(Arrays.asList("*"));

    // Credentials autorisés
    configuration.setAllowCredentials(true);

    // Max age
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
