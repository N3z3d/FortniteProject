package com.fortnite.pronos.config;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/** Filter that limits requests to /api/auth/** to 5 per 60 seconds per IP address. */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final String AUTH_PATH_PREFIX = "/api/auth";
  private static final int MAX_REQUESTS = 5;
  private static final int WINDOW_SECONDS = 60;
  private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;
  private static final String RETRY_AFTER_VALUE = String.valueOf(WINDOW_SECONDS);
  private static final String TOO_MANY_REQUESTS_BODY =
      "{\"error\":\"Too Many Requests\","
          + "\"message\":\"Rate limit exceeded. Try again in "
          + WINDOW_SECONDS
          + " seconds.\","
          + "\"retryAfterSeconds\":"
          + WINDOW_SECONDS
          + "}";

  private final ConcurrentHashMap<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!isAuthRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = request.getRemoteAddr();
    Bucket bucket = bucketsByIp.computeIfAbsent(clientIp, ip -> createNewBucket());

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      writeRateLimitResponse(response);
    }
  }

  private boolean isAuthRequest(HttpServletRequest request) {
    return request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
  }

  private Bucket createNewBucket() {
    Bandwidth limit =
        Bandwidth.classic(
            MAX_REQUESTS, Refill.greedy(MAX_REQUESTS, Duration.ofSeconds(WINDOW_SECONDS)));
    return Bucket.builder().addLimit(limit).build();
  }

  private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
    response.setStatus(HTTP_STATUS_TOO_MANY_REQUESTS);
    response.setContentType("application/json");
    response.setHeader("Retry-After", RETRY_AFTER_VALUE);
    response.getWriter().write(TOO_MANY_REQUESTS_BODY);
  }
}
