package com.fortnite.pronos.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("LoggingUtils")
class LoggingUtilsTest {

  @AfterEach
  void clearContext() {
    LoggingUtils.clearContext();
  }

  @Test
  @DisplayName("initializes trace context with 8 chars and stores it in MDC")
  void initializesTraceContextWithExpectedLength() {
    String traceId = LoggingUtils.initTraceContext();

    assertThat(traceId).hasSize(8);
    assertThat(MDC.get("traceId")).isEqualTo(traceId);
  }

  @Test
  @DisplayName("defines a dedicated trace id length constant")
  void definesTraceIdLengthConstant() {
    assertThatCode(() -> LoggingUtils.class.getDeclaredField("TRACE_ID_LENGTH"))
        .doesNotThrowAnyException();
  }
}
