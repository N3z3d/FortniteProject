package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("RateLimitingFilter")
class RateLimitingFilterTest {

  private RateLimitingFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RateLimitingFilter();
  }

  @Test
  @DisplayName("should allow requests under the rate limit (5 requests pass through)")
  void shouldAllowRequestsUnderLimit() throws Exception {
    FilterChain chain = mock(FilterChain.class);

    for (int i = 0; i < 5; i++) {
      MockHttpServletRequest request = authRequest("192.168.1.1");
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilterInternal(request, response, chain);
      assertThat(response.getStatus()).isNotEqualTo(429);
    }

    verify(chain, times(5))
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("should return 429 on the 6th request from the same IP")
  void shouldReturn429OnExcessRequests() throws Exception {
    FilterChain chain = mock(FilterChain.class);
    String ip = "10.0.0.1";

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(authRequest(ip), new MockHttpServletResponse(), chain);
    }

    MockHttpServletResponse sixthResponse = new MockHttpServletResponse();
    filter.doFilterInternal(authRequest(ip), sixthResponse, chain);

    assertThat(sixthResponse.getStatus()).isEqualTo(429);
    assertThat(sixthResponse.getContentAsString()).contains("Too Many Requests");
  }

  @Test
  @DisplayName("should include Retry-After header on 429 response")
  void shouldIncludeRetryAfterHeader() throws Exception {
    FilterChain chain = mock(FilterChain.class);
    String ip = "10.0.0.2";

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(authRequest(ip), new MockHttpServletResponse(), chain);
    }

    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilterInternal(authRequest(ip), response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("60");
  }

  @Test
  @DisplayName("should not limit non-auth endpoints like /api/games")
  void shouldNotLimitNonAuthEndpoints() throws Exception {
    FilterChain chain = mock(FilterChain.class);
    String ip = "10.0.0.3";

    for (int i = 0; i < 10; i++) {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games");
      request.setRemoteAddr(ip);
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilterInternal(request, response, chain);
      assertThat(response.getStatus()).isNotEqualTo(429);
    }

    verify(chain, times(10))
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("should maintain independent buckets per IP address")
  void shouldResetBucketPerIp() throws Exception {
    FilterChain chain = mock(FilterChain.class);
    String ip1 = "192.168.0.1";
    String ip2 = "192.168.0.2";

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(authRequest(ip1), new MockHttpServletResponse(), chain);
    }

    MockHttpServletResponse ip2Response = new MockHttpServletResponse();
    filter.doFilterInternal(authRequest(ip2), ip2Response, chain);

    assertThat(ip2Response.getStatus()).isNotEqualTo(429);
  }

  private MockHttpServletRequest authRequest(String remoteAddr) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr(remoteAddr);
    return request;
  }
}
