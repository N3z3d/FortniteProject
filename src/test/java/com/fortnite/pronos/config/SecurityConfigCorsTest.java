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

  @Test
  void corsConfiguration_allowsXTestUserHeader() {
    JwtAuthenticationFilter jwtAuthFilter = mock(JwtAuthenticationFilter.class);
    UserDetailsService userDetailsService = mock(UserDetailsService.class);
    Environment environment = mock(Environment.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<TestFallbackAuthenticationFilter> fallbackProvider = mock(ObjectProvider.class);

    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

    SecurityConfig securityConfig =
        new SecurityConfig(jwtAuthFilter, userDetailsService, environment, fallbackProvider);
    CorsConfigurationSource source = securityConfig.corsConfigurationSource();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games/my-games");
    CorsConfiguration configuration = source.getCorsConfiguration(request);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedHeaders()).contains("X-Test-User");
  }
}
