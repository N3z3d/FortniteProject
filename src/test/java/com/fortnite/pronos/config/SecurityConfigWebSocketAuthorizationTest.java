package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
@DisplayName("SecurityConfigWebSocketAuthorizationTest")
class SecurityConfigWebSocketAuthorizationTest {

  @Mock private JwtService jwtService;
  @Mock private UserDetailsService userDetailsService;
  @Mock private Environment environment;

  private WebSocketAuthInterceptor interceptor;

  @BeforeEach
  void setUp() {
    interceptor = new WebSocketAuthInterceptor(jwtService, userDetailsService, environment);
  }

  private Message<?> buildConnectMessage() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<?> buildConnectMessageWithJwt(String token) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setLeaveMutable(true);
    accessor.addNativeHeader("Authorization", "Bearer " + token);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private UserDetails testUser(String username) {
    return User.builder()
        .username(username)
        .password("pw")
        .authorities(Collections.emptyList())
        .build();
  }

  @Nested
  @DisplayName("Production environment")
  class ProductionEnvironment {

    @Test
    @DisplayName("Anonymous connection is rejected with IllegalStateException")
    void anonymousConnectionIsRejectedInProduction() {
      when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

      assertThatThrownBy(() -> interceptor.preSend(buildConnectMessage(), null))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("Valid JWT connection is accepted")
    void validJwtConnectionIsAcceptedInProduction() {
      String token = "valid.jwt.token";
      String email = "user@example.com";
      UserDetails user = testUser(email);

      when(jwtService.extractUsername(token)).thenReturn(email);
      when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
      when(jwtService.isTokenValid(token, user)).thenReturn(true);

      Message<?> result = interceptor.preSend(buildConnectMessageWithJwt(token), null);

      assertNotNull(result);
      verify(jwtService).isTokenValid(token, user);
    }

    @Test
    @DisplayName("Invalid JWT connection is rejected")
    void invalidJwtConnectionIsRejectedInProduction() {
      String token = "expired.jwt.token";
      String email = "user@example.com";
      UserDetails user = testUser(email);

      when(jwtService.extractUsername(token)).thenReturn(email);
      when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
      when(jwtService.isTokenValid(token, user)).thenReturn(false);
      when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

      assertThatThrownBy(() -> interceptor.preSend(buildConnectMessageWithJwt(token), null))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Authentication required");
    }
  }

  @Nested
  @DisplayName("Development environment")
  class DevelopmentEnvironment {

    @Test
    @DisplayName("Anonymous connection is allowed in dev (no auth set, no rejection)")
    void anonymousConnectionIsAllowedInDev() {
      when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

      Message<?> result = interceptor.preSend(buildConnectMessage(), null);

      assertNotNull(result);
    }

    @Test
    @DisplayName("X-Test-User connection is authenticated in dev")
    void testUserConnectionIsAuthenticatedInDev() {
      String testUser = "devuser";
      UserDetails user = testUser(testUser);

      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setLeaveMutable(true);
      accessor.addNativeHeader("X-Test-User", testUser);
      Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

      when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
      when(userDetailsService.loadUserByUsername(testUser)).thenReturn(user);

      Message<?> result = interceptor.preSend(message, null);

      assertNotNull(result);
      verify(userDetailsService).loadUserByUsername(testUser);
    }
  }
}
