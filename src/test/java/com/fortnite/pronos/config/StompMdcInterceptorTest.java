package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
class StompMdcInterceptorTest {

  private StompMdcInterceptor interceptor;

  @Mock private MessageChannel channel;

  @BeforeEach
  void setUp() {
    interceptor = new StompMdcInterceptor();
  }

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void shouldSetMdcKeysForStompSendCommand() {
    Principal principal = () -> "thibaut@test.com";
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setDestination("/app/draft/abc123/pick");
    accessor.setUser(principal);
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    Message<?> result = interceptor.preSend(message, channel);

    assertThat(result).isNotNull();
    assertThat(MDC.get(StompMdcInterceptor.MDC_STOMP_COMMAND)).isEqualTo("SEND");
    assertThat(MDC.get(StompMdcInterceptor.MDC_STOMP_DESTINATION))
        .isEqualTo("/app/draft/abc123/pick");
    assertThat(MDC.get(StompMdcInterceptor.MDC_STOMP_USER)).isEqualTo("thibaut@test.com");
    assertThat(MDC.get(StompMdcInterceptor.MDC_CORRELATION_ID)).isNotNull();
  }

  @Test
  void shouldReturnMessageUnchangedWhenAccessorIsNull() {
    // GenericMessage has no StompHeaderAccessor embedded — getAccessor returns null
    Message<byte[]> message = new GenericMessage<>(new byte[0]);

    Message<?> result = interceptor.preSend(message, channel);

    assertThat(result).isSameAs(message);
  }

  @Test
  void shouldClearAllMdcKeysInAfterSendCompletion() {
    MDC.put(StompMdcInterceptor.MDC_CORRELATION_ID, "test-corr");
    MDC.put(StompMdcInterceptor.MDC_STOMP_COMMAND, "SEND");
    MDC.put(StompMdcInterceptor.MDC_STOMP_DESTINATION, "/app/test");
    MDC.put(StompMdcInterceptor.MDC_STOMP_USER, "user@test.com");

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    interceptor.afterSendCompletion(message, channel, true, null);

    assertThat(MDC.get(StompMdcInterceptor.MDC_CORRELATION_ID)).isNull();
    assertThat(MDC.get(StompMdcInterceptor.MDC_STOMP_COMMAND)).isNull();
    assertThat(MDC.get(StompMdcInterceptor.MDC_STOMP_DESTINATION)).isNull();
    assertThat(MDC.get(StompMdcInterceptor.MDC_STOMP_USER)).isNull();
  }

  @Test
  void preSendShouldNeverReturnNullToAvoidDroppingStompFrame() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/draft/abc123/picks");
    Message<byte[]> message =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    Message<?> result = interceptor.preSend(message, channel);

    assertThat(result).isNotNull();
  }
}
