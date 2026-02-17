package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigCorsTest {

  private SecurityConfig buildSecurityConfigForProfiles(String... activeProfiles) {
    JwtAuthenticationFilter jwtAuthFilter = mock(JwtAuthenticationFilter.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    Environment environment = mock(Environment.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<TestFallbackAuthenticationFilter> fallbackProvider = mock(ObjectProvider.class);

    when(environment.getActiveProfiles()).thenReturn(activeProfiles);

    return new SecurityConfig(jwtAuthFilter, userDetailsService, environment, fallbackProvider);
  }

  private CorsConfiguration loadCorsConfiguration(SecurityConfig securityConfig) {
    CorsConfigurationSource source = securityConfig.corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games/my-games");
    return source.getCorsConfiguration(request);
  }

  @Test
  void corsConfiguration_allowsXTestUserHeader() {
    SecurityConfig securityConfig = buildSecurityConfigForProfiles("dev");
    CorsConfiguration configuration = loadCorsConfiguration(securityConfig);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedHeaders()).contains("X-Test-User");
  }

  @Test
  void corsConfiguration_doesNotUseWildcardMethodsOrHeaders() {
    SecurityConfig securityConfig = buildSecurityConfigForProfiles("dev");
    CorsConfiguration configuration = loadCorsConfiguration(securityConfig);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedMethods()).doesNotContain("*");
    assertThat(configuration.getAllowedHeaders()).doesNotContain("*");
  }

  @Test
  void corsConfiguration_usesLocalOriginsInDevProfile() {
    SecurityConfig securityConfig = buildSecurityConfigForProfiles("dev");
    CorsConfiguration configuration = loadCorsConfiguration(securityConfig);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOriginPatterns())
        .contains("http://localhost:4200", "http://localhost:4201");
  }

  @Test
  void corsConfiguration_usesStrictOriginsInProdProfile() {
    SecurityConfig securityConfig = buildSecurityConfigForProfiles("prod");
    CorsConfiguration configuration = loadCorsConfiguration(securityConfig);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOriginPatterns())
        .contains("https://fortnitepronos.com", "https://*.fortnitepronos.com");
    assertThat(configuration.getAllowedOriginPatterns()).doesNotContain("http://localhost:4200");
  }
}
