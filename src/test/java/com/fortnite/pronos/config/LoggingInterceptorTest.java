package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoggingInterceptorTest {

  private final LoggingInterceptor interceptor = new LoggingInterceptor();

  @Test
  void preHandle_setsStartTimeAndReturnsTrue() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/leaderboard/top");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean result = interceptor.preHandle(request, response, new Object());

    assertThat(result).isTrue();
    assertThat(request.getAttribute("startTime")).isInstanceOf(Long.class);
  }

  @Test
  void afterCompletion_doesNotFailWhenStartTimeIsMissing() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertDoesNotThrow(() -> interceptor.afterCompletion(request, response, new Object(), null));
  }

  @Test
  void afterCompletion_doesNotFailWhenExceptionIsPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/players");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setAttribute("startTime", System.currentTimeMillis() - 1_200L);
    IllegalStateException exception = new IllegalStateException("boom");

    assertDoesNotThrow(
        () -> interceptor.afterCompletion(request, response, new Object(), exception));
  }
}
