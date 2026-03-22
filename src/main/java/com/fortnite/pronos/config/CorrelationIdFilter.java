package com.fortnite.pronos.config;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that assigns a correlation ID to every HTTP request. The ID is taken from the
 * incoming {@code X-Correlation-ID} header (distributed tracing) or generated as a short UUID
 * otherwise. The ID is propagated via MDC so that every log line for the request includes it, and
 * returned to the caller via the {@code X-Correlation-ID} response header.
 *
 * <p>MDC keys set by this filter: {@code correlationId}, {@code requestId}. Both are removed in a
 * {@code finally} block — never via {@code MDC.clear()} — to avoid wiping context added by upstream
 * components such as {@code LoggingUtils}.
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

  static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  static final String MDC_CORRELATION_ID = "correlationId";
  static final String MDC_REQUEST_ID = "requestId";

  private static final int SHORT_UUID_LENGTH = 16;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = resolveCorrelationId(request);
    String requestId = generateShortId();

    MDC.put(MDC_CORRELATION_ID, correlationId);
    MDC.put(MDC_REQUEST_ID, requestId);
    response.setHeader(CORRELATION_ID_HEADER, correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_CORRELATION_ID);
      MDC.remove(MDC_REQUEST_ID);
    }
  }

  private String resolveCorrelationId(HttpServletRequest request) {
    String incoming = request.getHeader(CORRELATION_ID_HEADER);
    return (incoming != null && !incoming.isBlank()) ? incoming : generateShortId();
  }

  private String generateShortId() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, SHORT_UUID_LENGTH);
  }
}
