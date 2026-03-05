package com.fortnite.pronos.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.fortnite.pronos.service.JwtService;

/**
 * Shared {@code @TestConfiguration} that provides a no-op {@link JwtAuthenticationFilter} bean.
 * Extracted from the duplicated inner {@code SecurityTestBeans} class present in every {@code
 * SecurityConfig*AuthorizationTest}. Import with {@code @Import(SecurityTestBeansConfig.class)}.
 */
@TestConfiguration
public class SecurityTestBeansConfig {

  @Bean
  JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
    JwtService jwtService = Mockito.mock(JwtService.class);
    return new JwtAuthenticationFilter(jwtService, userDetailsService) {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
        filterChain.doFilter(request, response);
      }
    };
  }
}
