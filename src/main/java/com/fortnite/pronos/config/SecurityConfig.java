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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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

/** Security configuration for the application. */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "fortnite.security.enabled",
    havingValue = "true",
    matchIfMissing = true)
@org.springframework.context.annotation.Profile("!test")
@SuppressWarnings({"java:S1874", "java:S5738"})
public class SecurityConfig {

  private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;
  private static final long CORS_MAX_AGE_SECONDS = 3_600L;

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final UserDetailsService userDetailsService;
  private final Environment environment;
  private final org.springframework.beans.factory.ObjectProvider<TestFallbackAuthenticationFilter>
      testFallbackAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(this::configureAuthorizationRules)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .headers(this::configureSecurityHeaders);

    TestFallbackAuthenticationFilter fallbackFilter =
        testFallbackAuthenticationFilter.getIfAvailable();
    if (fallbackFilter != null) {
      http.addFilterBefore(fallbackFilter, JwtAuthenticationFilter.class);
    }

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(resolveAllowedOriginPatterns());
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
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
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(CORS_MAX_AGE_SECONDS);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private void configureAuthorizationRules(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          authorizations) {
    authorizations
        .requestMatchers("/api/auth/**")
        .permitAll()
        .requestMatchers("/actuator/health")
        .permitAll()
        .requestMatchers("/actuator/**")
        .hasRole("ADMIN")
        .requestMatchers("/h2-console/**")
        .permitAll()
        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
        .permitAll()
        .requestMatchers("/ws/**")
        .permitAll()
        .requestMatchers("/api/admin/**")
        .hasRole("ADMIN")
        .requestMatchers("/api/**")
        .authenticated()
        .anyRequest()
        .authenticated();
  }

  private void configureSecurityHeaders(HeadersConfigurer<HttpSecurity> headers) {
    headers
        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
        .contentTypeOptions(Customizer.withDefaults())
        .httpStrictTransportSecurity(this::configureHsts)
        .referrerPolicy(
            referrer ->
                referrer.policy(
                    org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
        .httpPublicKeyPinning(hpkp -> hpkp.includeSubDomains(true));
  }

  private void configureHsts(HeadersConfigurer<HttpSecurity>.HstsConfig hstsConfig) {
    hstsConfig.maxAgeInSeconds(HSTS_MAX_AGE_SECONDS).includeSubDomains(true).preload(true);
  }

  private List<String> resolveAllowedOriginPatterns() {
    boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    if (isProd) {
      return List.of("https://fortnitepronos.com", "https://*.fortnitepronos.com");
    }
    return List.of(
        "http://localhost:4200",
        "http://localhost:4201",
        "http://127.0.0.1:4200",
        "http://127.0.0.1:4201");
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }
}
