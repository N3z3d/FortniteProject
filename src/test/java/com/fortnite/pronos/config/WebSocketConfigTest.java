package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

class WebSocketConfigTest {

  private WebSocketConfig webSocketConfig;
  private WebSocketAuthInterceptor mockInterceptor;

  @BeforeEach
  void setUp() {
    mockInterceptor = mock(WebSocketAuthInterceptor.class);
    webSocketConfig = new WebSocketConfig(mockInterceptor);
  }

  @Test
  void configuresMessageBrokerWithCorrectPrefixes() {
    MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

    webSocketConfig.configureMessageBroker(registry);

    verify(registry).enableSimpleBroker("/topic", "/queue");
    verify(registry).setApplicationDestinationPrefixes("/app");
    verify(registry).setUserDestinationPrefix("/user");
  }

  @Test
  void registersStompEndpointWithSockJS() {
    StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
    StompWebSocketEndpointRegistration registration =
        mock(StompWebSocketEndpointRegistration.class);

    org.mockito.Mockito.when(registry.addEndpoint("/ws")).thenReturn(registration);
    org.mockito.Mockito.when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

    webSocketConfig.registerStompEndpoints(registry);

    verify(registry).addEndpoint("/ws");
    verify(registration).setAllowedOriginPatterns("*");
    verify(registration).withSockJS();
  }

  @Test
  void webSocketConfigIsNotNull() {
    assertThat(webSocketConfig).isNotNull();
  }
}
