package com.fortnite.pronos.util;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** Utility for structured logs used by backend services. */
@Component
@Slf4j
@SuppressWarnings({"java:S1118", "java:S1488", "java:S2629"})
public class LoggingUtils {

  private static final int TRACE_ID_LENGTH = 8;

  private static final String TRACE_ID_KEY = "traceId";
  private static final String USER_ID_KEY = "userId";
  private static final String ACTION_KEY = "action";
  private static final String DURATION_KEY = "duration";
  private static final String STATUS_KEY = "status";

  private static final String SUCCESS_STATUS = "SUCCESS";
  private static final String FAILURE_STATUS = "FAILURE";

  public static String initTraceContext() {
    String traceId = UUID.randomUUID().toString().substring(0, TRACE_ID_LENGTH);
    MDC.put(TRACE_ID_KEY, traceId);
    return traceId;
  }

  public static void clearContext() {
    MDC.clear();
  }

  public static void setUserContext(String userId) {
    MDC.put(USER_ID_KEY, userId);
  }

  public static void logAudit(String action, String userId, Map<String, Object> details) {
    MDC.put(ACTION_KEY, action);
    MDC.put(USER_ID_KEY, userId);

    StringBuilder message =
        new StringBuilder().append("AUDIT: ").append(action).append(" by user ").append(userId);

    if (details != null && !details.isEmpty()) {
      message.append(" - Details: ");
      details.forEach((key, value) -> message.append(key).append("=").append(value).append(" "));
    }

    AuditLogHolder.LOGGER.info(message.toString());
  }

  public static void logPerformance(String operation, long durationMs, String status) {
    MDC.put(ACTION_KEY, operation);
    MDC.put(DURATION_KEY, String.valueOf(durationMs));
    MDC.put(STATUS_KEY, status);
    PerformanceLogHolder.LOGGER.info(
        "PERFORMANCE: {} completed in {}ms with status {}", operation, durationMs, status);
  }

  public static void logHttpAccess(
      String method,
      String uri,
      int statusCode,
      long durationMs,
      String userAgent,
      String clientIp) {
    String message =
        String.format(
            "HTTP_ACCESS: %s %s - Status: %d - Duration: %dms - IP: %s - UserAgent: %s",
            method, uri, statusCode, durationMs, clientIp, userAgent);
    AccessLogHolder.LOGGER.info(message);
  }

  public static void logError(String operation, Exception exception, Map<String, Object> context) {
    MDC.put(ACTION_KEY, operation);
    MDC.put(STATUS_KEY, "ERROR");

    StringBuilder message =
        new StringBuilder()
            .append("ERROR in ")
            .append(operation)
            .append(": ")
            .append(exception.getMessage());

    if (context != null && !context.isEmpty()) {
      message.append(" - Context: ");
      context.forEach((key, value) -> message.append(key).append("=").append(value).append(" "));
    }

    log.error(message.toString(), exception);
  }

  public static void logBusinessTransaction(
      String transactionType, String entityId, String action, String userId, boolean success) {
    MDC.put(ACTION_KEY, transactionType + "_" + action);
    MDC.put(USER_ID_KEY, userId);
    MDC.put(STATUS_KEY, success ? SUCCESS_STATUS : FAILURE_STATUS);

    log.info(
        "BUSINESS_TRANSACTION: {} {} on entity {} by user {} - Status: {}",
        transactionType,
        action,
        entityId,
        userId,
        success ? SUCCESS_STATUS : FAILURE_STATUS);
  }

  public static void logSystemMetric(String metricName, double value, String unit) {
    PerformanceLogHolder.LOGGER.info("SYSTEM_METRIC: {} = {} {}", metricName, value, unit);
  }

  public static <T> T measureAndLog(String operation, Supplier<T> supplier) {
    long startTime = System.currentTimeMillis();
    String status = SUCCESS_STATUS;

    try {
      return supplier.get();
    } catch (Exception e) {
      status = "ERROR";
      logError(operation, e, null);
      throw e;
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      logPerformance(operation, duration, status);
    }
  }

  public static void logApplicationStart(String applicationName, String version, String profile) {
    log.info(
        "APPLICATION_START: {} v{} started with profile: {}", applicationName, version, profile);
  }

  public static void logApplicationShutdown(String applicationName) {
    log.info("APPLICATION_SHUTDOWN: {} is shutting down", applicationName);
  }

  public static void logDataImport(
      String importType, int totalRecords, int successCount, int errorCount, long durationMs) {
    MDC.put(ACTION_KEY, "DATA_IMPORT_" + importType);
    MDC.put(DURATION_KEY, String.valueOf(durationMs));

    log.info(
        "DATA_IMPORT: {} - Total: {}, Success: {}, Errors: {}, Duration: {}ms",
        importType,
        totalRecords,
        successCount,
        errorCount,
        durationMs);
  }

  public static void logValidation(
      String validationType, String entityId, boolean isValid, String errorMessage) {
    MDC.put(ACTION_KEY, "VALIDATION_" + validationType);
    MDC.put(STATUS_KEY, isValid ? "VALID" : "INVALID");

    if (isValid) {
      log.debug("VALIDATION: {} for entity {} - VALID", validationType, entityId);
    } else {
      log.warn(
          "VALIDATION: {} for entity {} - INVALID: {}", validationType, entityId, errorMessage);
    }
  }

  public static void logCacheOperation(String operation, String cacheKey, boolean hit) {
    MDC.put(ACTION_KEY, "CACHE_" + operation);
    MDC.put(STATUS_KEY, hit ? "HIT" : "MISS");
    PerformanceLogHolder.LOGGER.debug(
        "CACHE: {} for key {} - {}", operation, cacheKey, hit ? "HIT" : "MISS");
  }

  public static void logNotification(
      String notificationType, String recipient, boolean success, String errorMessage) {
    MDC.put(ACTION_KEY, "NOTIFICATION_" + notificationType);
    MDC.put(STATUS_KEY, success ? "SENT" : "FAILED");

    if (success) {
      log.info("NOTIFICATION: {} sent to {}", notificationType, recipient);
    } else {
      log.error(
          "NOTIFICATION: {} failed for {} - Error: {}", notificationType, recipient, errorMessage);
    }
  }

  private static final class PerformanceLogHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.fortnite.pronos.performance");

    private PerformanceLogHolder() {}
  }

  private static final class AccessLogHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.fortnite.pronos.access");

    private AccessLogHolder() {}
  }

  private static final class AuditLogHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.fortnite.pronos.audit");

    private AuditLogHolder() {}
  }
}
