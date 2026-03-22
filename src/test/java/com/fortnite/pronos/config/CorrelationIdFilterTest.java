package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

  private CorrelationIdFilter filter;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain chain;

  @BeforeEach
  void setUp() {
    filter = new CorrelationIdFilter();
  }

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void shouldGenerateCorrelationIdWhenNoHeaderPresent() throws Exception {
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);

    filter.doFilterInternal(request, response, chain);

    ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
    verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), idCaptor.capture());
    assertThat(idCaptor.getValue()).hasSize(16);
    verify(chain).doFilter(request, response);
  }

  @Test
  void shouldReuseCorrelationIdFromIncomingHeader() throws Exception {
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("my-trace-id");

    filter.doFilterInternal(request, response, chain);

    verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "my-trace-id");
    verify(chain).doFilter(request, response);
  }

  @Test
  void shouldClearMdcInFinallyEvenWhenChainThrows() throws Exception {
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);
    doThrow(new ServletException("simulated chain failure")).when(chain).doFilter(any(), any());

    assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
        .isInstanceOf(ServletException.class)
        .hasMessage("simulated chain failure");

    assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID)).isNull();
    assertThat(MDC.get(CorrelationIdFilter.MDC_REQUEST_ID)).isNull();
  }

  @Test
  void shouldAlwaysCallFilterChainOnceRegardlessOfHeader() throws Exception {
    when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("any-id");

    filter.doFilterInternal(request, response, chain);

    verify(chain, times(1)).doFilter(request, response);
    verify(response).setHeader(anyString(), anyString());
  }
}
