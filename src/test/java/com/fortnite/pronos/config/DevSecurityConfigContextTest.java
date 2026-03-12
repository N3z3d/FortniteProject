package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

class DevSecurityConfigContextTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withPropertyValues("spring.profiles.active=dev")
          .withUserConfiguration(
              SecurityConfig.class, DevSecurityConfig.class, TestBeansConfiguration.class);

  @Test
  void devProfileRegistersSingleSecurityFilterChain() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(SecurityFilterChain.class);
        });
  }

  @TestConfiguration
  static class TestBeansConfiguration {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
      return new JwtAuthenticationFilter(
          mock(com.fortnite.pronos.service.JwtService.class), userDetailsService);
    }

    @Bean
    UserDetailsService userDetailsService() {
      return username ->
          User.withUsername(username).password("{noop}password").authorities("ROLE_USER").build();
    }

    @Bean
    RateLimitingFilter rateLimitingFilter() {
      return new RateLimitingFilter();
    }

    @Bean
    TestFallbackAuthenticationFilter testFallbackAuthenticationFilter(
        UserDetailsService userDetailsService) {
      return new TestFallbackAuthenticationFilter(userDetailsService);
    }

    @Bean
    @SuppressWarnings("unchecked")
    ObjectProvider<TestFallbackAuthenticationFilter> testFallbackProvider(
        TestFallbackAuthenticationFilter filter) {
      ObjectProvider<TestFallbackAuthenticationFilter> provider = mock(ObjectProvider.class);
      org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(filter);
      return provider;
    }
  }
}
