package com.fortnite.pronos.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.fortnite.pronos.service.JwtService;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketAuthInterceptor Tests")
class WebSocketAuthInterceptorTest {

  @Mock private JwtService jwtService;
  @Mock private UserDetailsService userDetailsService;
  @Mock private Environment environment;

  private WebSocketAuthInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new WebSocketAuthInterceptor(jwtService, userDetailsService, environment);
  }

  private UserDetails createTestUser(String username) {
    return User.builder()
        .username(username)
        .password("password")
        .authorities(Collections.emptyList())
        .build();
  }

  @Nested
  @DisplayName("JWT Authentication")
  class JwtAuthentication {

    @Test
    @DisplayName("Should authenticate with valid JWT token")
    void shouldAuthenticateWithValidJwt() {
      // Given
      String token = "valid.jwt.token";
      String userEmail = "test@example.com";
      UserDetails userDetails = createTestUser(userEmail);

      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setLeaveMutable(true);
      accessor.addNativeHeader("Authorization", "Bearer " + token);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      when(jwtService.extractUsername(token)).thenReturn(userEmail);
      when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);
      when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

      // When
      Message<?> result = interceptor.preSend(message, null);

      // Then
      assertNotNull(result);
      verify(jwtService).extractUsername(token);
      verify(jwtService).isTokenValid(token, userDetails);
    }

    @Test
    @DisplayName("Should reject invalid JWT token")
    void shouldRejectInvalidJwt() {
      // Given
      String token = "invalid.jwt.token";
      String userEmail = "test@example.com";
      UserDetails userDetails = createTestUser(userEmail);

      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setLeaveMutable(true);
      accessor.addNativeHeader("Authorization", "Bearer " + token);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      when(jwtService.extractUsername(token)).thenReturn(userEmail);
      when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);
      when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);
      when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

      // When
      Message<?> result = interceptor.preSend(message, null);

      // Then
      assertNotNull(result);
      verify(jwtService).isTokenValid(token, userDetails);
    }
  }

  @Nested
  @DisplayName("X-Test-User Authentication (Dev Mode)")
  class TestUserAuthentication {

    @Test
    @DisplayName("Should authenticate with X-Test-User in dev environment")
    void shouldAuthenticateWithTestUserInDev() {
      // Given
      String testUser = "devuser";
      UserDetails userDetails = createTestUser(testUser);

      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setLeaveMutable(true);
      accessor.addNativeHeader("X-Test-User", testUser);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
      when(userDetailsService.loadUserByUsername(testUser)).thenReturn(userDetails);

      // When
      Message<?> result = interceptor.preSend(message, null);

      // Then
      assertNotNull(result);
      verify(userDetailsService).loadUserByUsername(testUser);
    }

    @Test
    @DisplayName("Should NOT authenticate with X-Test-User in prod environment")
    void shouldNotAuthenticateWithTestUserInProd() {
      // Given
      String testUser = "devuser";

      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setLeaveMutable(true);
      accessor.addNativeHeader("X-Test-User", testUser);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

      // When
      Message<?> result = interceptor.preSend(message, null);

      // Then
      assertNotNull(result);
      verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Should authenticate with X-Test-User in h2 environment")
    void shouldAuthenticateWithTestUserInH2() {
      // Given
      String testUser = "h2user";
      UserDetails userDetails = createTestUser(testUser);

      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setLeaveMutable(true);
      accessor.addNativeHeader("X-Test-User", testUser);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      when(environment.getActiveProfiles()).thenReturn(new String[] {"h2"});
      when(userDetailsService.loadUserByUsername(testUser)).thenReturn(userDetails);

      // When
      Message<?> result = interceptor.preSend(message, null);

      // Then
      assertNotNull(result);
      verify(userDetailsService).loadUserByUsername(testUser);
    }
  }

  @Nested
  @DisplayName("Non-CONNECT Commands")
  class NonConnectCommands {

    @Test
    @DisplayName("Should pass through non-CONNECT messages")
    void shouldPassThroughNonConnectMessages() {
      // Given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // When
      Message<?> result = interceptor.preSend(message, null);

      // Then
      assertNotNull(result);
      verifyNoInteractions(jwtService);
      verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Should pass through SEND messages")
    void shouldPassThroughSendMessages() {
      // Given
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      // When
      Message<?> result = interceptor.preSend(message, null);

      // Then
      assertNotNull(result);
      verifyNoInteractions(jwtService);
    }
  }
}
