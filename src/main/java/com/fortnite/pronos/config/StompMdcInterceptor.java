package com.fortnite.pronos.config;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * STOMP channel interceptor that propagates MDC context for every inbound WebSocket frame. This
 * makes STOMP events (especially draft picks) visible in structured logs, enabling post-hoc
 * diagnosis of state-desync bugs such as BUG-06.
 *
 * <p>MDC keys set: {@code correlationId}, {@code stompCommand}, {@code stompDestination}, {@code
 * stompUser}. All keys are removed in {@code afterSendCompletion} — never via {@code MDC.clear()}.
 *
 * <p>NEVER return {@code null} from {@code preSend}: a null return silently drops the STOMP frame.
 */
@Component
@Slf4j
public class StompMdcInterceptor implements ChannelInterceptor {

  static final String MDC_CORRELATION_ID = "correlationId";
  static final String MDC_STOMP_COMMAND = "stompCommand";
  static final String MDC_STOMP_DESTINATION = "stompDestination";
  static final String MDC_STOMP_USER = "stompUser";

  private static final int SHORT_UUID_LENGTH = 16;

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    StompCommand command = accessor.getCommand();
    String destination = accessor.getDestination();
    String user = accessor.getUser() != null ? accessor.getUser().getName() : "anonymous";
    String correlationId = resolveCorrelationId(accessor);

    MDC.put(MDC_CORRELATION_ID, correlationId);
    MDC.put(MDC_STOMP_COMMAND, command != null ? command.name() : "UNKNOWN");
    MDC.put(MDC_STOMP_DESTINATION, destination != null ? destination : "");
    MDC.put(MDC_STOMP_USER, user);

    if (StompCommand.SEND.equals(command)) {
      log.debug("STOMP SEND -> {} [user={}]", destination, user);
    }

    return message;
  }

  @Override
  public void afterSendCompletion(
      @NonNull Message<?> message, @NonNull MessageChannel channel, boolean sent, Exception ex) {
    MDC.remove(MDC_CORRELATION_ID);
    MDC.remove(MDC_STOMP_COMMAND);
    MDC.remove(MDC_STOMP_DESTINATION);
    MDC.remove(MDC_STOMP_USER);
  }

  private String resolveCorrelationId(StompHeaderAccessor accessor) {
    String incoming = accessor.getFirstNativeHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
    return (incoming != null && !incoming.isBlank())
        ? incoming
        : UUID.randomUUID().toString().replace("-", "").substring(0, SHORT_UUID_LENGTH);
  }
}
