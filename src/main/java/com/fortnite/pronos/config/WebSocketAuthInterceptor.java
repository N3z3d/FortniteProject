package com.fortnite.pronos.config;

import java.util.Arrays;

import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.fortnite.pronos.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor for WebSocket STOMP connections to handle authentication. Supports both JWT tokens
 * (production) and X-Test-User header (development).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String TEST_USER_HEADER = "X-Test-User";

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final Environment environment;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }

    // Try JWT authentication first
    if (tryJwtAuthentication(accessor)) {
      return message;
    }

    // Fall back to X-Test-User in non-production environments
    if (isDevEnvironment()) {
      tryTestUserAuthentication(accessor);
    }

    return message;
  }

  private boolean tryJwtAuthentication(StompHeaderAccessor accessor) {
    String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      return false;
    }

    String jwt = authHeader.substring(BEARER_PREFIX.length());

    try {
      String userEmail = jwtService.extractUsername(jwt);
      if (userEmail == null) {
        return false;
      }

      UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
      if (!jwtService.isTokenValid(jwt, userDetails)) {
        log.warn("Invalid JWT token for WebSocket connection");
        return false;
      }

      setAuthentication(accessor, userDetails);
      log.debug("WebSocket JWT authenticated for user: {}", userEmail);
      return true;

    } catch (Exception e) {
      log.error("WebSocket JWT authentication failed: {}", e.getMessage());
      return false;
    }
  }

  private void tryTestUserAuthentication(StompHeaderAccessor accessor) {
    String testUser = accessor.getFirstNativeHeader(TEST_USER_HEADER);

    if (testUser == null || testUser.isBlank()) {
      log.debug("No X-Test-User header in WebSocket CONNECT frame");
      return;
    }

    try {
      UserDetails userDetails = userDetailsService.loadUserByUsername(testUser);
      setAuthentication(accessor, userDetails);
      log.debug("WebSocket X-Test-User authenticated for user: {}", testUser);
    } catch (Exception e) {
      log.warn("WebSocket X-Test-User authentication failed for {}: {}", testUser, e.getMessage());
    }
  }

  private void setAuthentication(StompHeaderAccessor accessor, UserDetails userDetails) {
    UsernamePasswordAuthenticationToken authToken =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    SecurityContextHolder.getContext().setAuthentication(authToken);
    accessor.setUser(authToken);
  }

  private boolean isDevEnvironment() {
    String[] activeProfiles = environment.getActiveProfiles();
    return Arrays.stream(activeProfiles)
        .anyMatch(
            profile -> profile.equals("dev") || profile.equals("h2") || profile.equals("test"));
  }
}
