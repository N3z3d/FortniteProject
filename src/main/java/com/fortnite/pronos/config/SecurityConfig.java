package com.fortnite.pronos.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

/** Configuration de sécurité pour l'application */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "fortnite.security.enabled",
    havingValue = "true",
    matchIfMissing = true)
@org.springframework.context.annotation.Profile("!test")
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final UserDetailsService userDetailsService;
  private final Environment environment;
  private final org.springframework.beans.factory.ObjectProvider<TestFallbackAuthenticationFilter>
      testFallbackAuthenticationFilter;

  /** Configuration du filtre de sécurité */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Désactiver CSRF pour les API REST
        .csrf(AbstractHttpConfigurer::disable)

        // Configuration CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // Configuration des autorisations - Sécurité rétablie
        .authorizeHttpRequests(
            authz ->
                authz
                    // Endpoints publics
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/h2-console/**")
                    .permitAll() // Pour H2 en dev
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    // WebSocket endpoints - SockJS requires multiple paths
                    .requestMatchers("/ws/**")
                    .permitAll()

                    // Endpoints API protégés
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/**")
                    .authenticated()

                    // Toutes les autres requêtes nécessitent une authentification
                    .anyRequest()
                    .authenticated())

        // Configuration des sessions
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Fournisseur d'authentification
        .authenticationProvider(authenticationProvider())

        // Ajouter le filtre JWT (le fallback de test est injecté plus bas si présent)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

        // PHASE 1A: ENHANCED SECURITY HEADERS for production hardening
        .headers(
            headers ->
                headers
                    .frameOptions()
                    .deny()
                    .contentTypeOptions()
                    .and()
                    .httpStrictTransportSecurity(
                        hstsConfig ->
                            hstsConfig
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                                .preload(true))
                    .referrerPolicy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                            .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    .and()
                    .httpPublicKeyPinning(hpkp -> hpkp.includeSubDomains(true)));

    TestFallbackAuthenticationFilter fallbackFilter =
        testFallbackAuthenticationFilter.getIfAvailable();
    if (fallbackFilter != null) {
      http.addFilterBefore(fallbackFilter, JwtAuthenticationFilter.class);
    }

    return http.build();
  }

  /** Configuration CORS */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // PHASE 1A: ENHANCED CORS - More restrictive for production security
    boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    if (isProd) {
      // Production: strict domain restrictions
      configuration.setAllowedOriginPatterns(
          List.of("https://fortnitepronos.com", "https://*.fortnitepronos.com"));
    } else {
      // Development: allow local development
      configuration.setAllowedOriginPatterns(
          List.of(
              "http://localhost:4200",
              "http://localhost:4201",
              "http://127.0.0.1:4200",
              "http://127.0.0.1:4201"));
    }

    // Méthodes autorisées
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // Headers autorisés - restrictifs pour la sécurité
    configuration.setAllowedHeaders(
        Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-Test-User",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"));

    // Credentials autorisés
    configuration.setAllowCredentials(true);

    // Max age
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  /** Encodeur de mots de passe */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** Fournisseur d'authentification */
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  /** Gestionnaire d'authentification */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }
}
