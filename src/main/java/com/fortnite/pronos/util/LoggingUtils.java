package com.fortnite.pronos.util;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** Utilitaire pour les logs structurés et professionnels */
@Component
@Slf4j
public class LoggingUtils {

  private static final Logger PERFORMANCE_LOGGER =
      LoggerFactory.getLogger("com.fortnite.pronos.performance");
  private static final Logger ACCESS_LOGGER = LoggerFactory.getLogger("com.fortnite.pronos.access");
  private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("com.fortnite.pronos.audit");

  private static final String TRACE_ID_KEY = "traceId";
  private static final String USER_ID_KEY = "userId";
  private static final String ACTION_KEY = "action";
  private static final String DURATION_KEY = "duration";
  private static final String STATUS_KEY = "status";

  /** Initialise un contexte de trace pour une requête */
  public static String initTraceContext() {
    String traceId = UUID.randomUUID().toString().substring(0, 8);
    MDC.put(TRACE_ID_KEY, traceId);
    return traceId;
  }

  /** Nettoie le contexte MDC */
  public static void clearContext() {
    MDC.clear();
  }

  /** Ajoute l'utilisateur au contexte */
  public static void setUserContext(String userId) {
    MDC.put(USER_ID_KEY, userId);
  }

  /** Log une action d'audit avec contexte */
  public static void logAudit(String action, String userId, Map<String, Object> details) {
    MDC.put(ACTION_KEY, action);
    MDC.put(USER_ID_KEY, userId);

    StringBuilder message =
        new StringBuilder().append("AUDIT: ").append(action).append(" by user ").append(userId);

    if (details != null && !details.isEmpty()) {
      message.append(" - Details: ");
      details.forEach((key, value) -> message.append(key).append("=").append(value).append(" "));
    }

    AUDIT_LOGGER.info(message.toString());
  }

  /** Log une métrique de performance */
  public static void logPerformance(String operation, long durationMs, String status) {
    MDC.put(ACTION_KEY, operation);
    MDC.put(DURATION_KEY, String.valueOf(durationMs));
    MDC.put(STATUS_KEY, status);

    PERFORMANCE_LOGGER.info(
        "PERFORMANCE: {} completed in {}ms with status {}", operation, durationMs, status);
  }

  /** Log un accès HTTP */
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

    ACCESS_LOGGER.info(message);
  }

  /** Log une erreur avec contexte enrichi */
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

  /** Log une transaction métier */
  public static void logBusinessTransaction(
      String transactionType, String entityId, String action, String userId, boolean success) {
    MDC.put(ACTION_KEY, transactionType + "_" + action);
    MDC.put(USER_ID_KEY, userId);
    MDC.put(STATUS_KEY, success ? "SUCCESS" : "FAILURE");

    log.info(
        "BUSINESS_TRANSACTION: {} {} on entity {} by user {} - Status: {}",
        transactionType,
        action,
        entityId,
        userId,
        success ? "SUCCESS" : "FAILURE");
  }

  /** Log des métriques système */
  public static void logSystemMetric(String metricName, double value, String unit) {
    PERFORMANCE_LOGGER.info("SYSTEM_METRIC: {} = {} {}", metricName, value, unit);
  }

  /** Wrapper pour mesurer et logger le temps d'exécution */
  public static <T> T measureAndLog(String operation, java.util.function.Supplier<T> supplier) {
    long startTime = System.currentTimeMillis();
    String status = "SUCCESS";

    try {
      T result = supplier.get();
      return result;
    } catch (Exception e) {
      status = "ERROR";
      logError(operation, e, null);
      throw e;
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      logPerformance(operation, duration, status);
    }
  }

  /** Log de démarrage d'application */
  public static void logApplicationStart(String applicationName, String version, String profile) {
    log.info(
        "APPLICATION_START: {} v{} started with profile: {}", applicationName, version, profile);
  }

  /** Log de fin d'application */
  public static void logApplicationShutdown(String applicationName) {
    log.info("APPLICATION_SHUTDOWN: {} is shutting down", applicationName);
  }

  /** Log pour les imports de données */
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

  /** Log pour les validations métier */
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

  /** Log pour les opérations de cache */
  public static void logCacheOperation(String operation, String cacheKey, boolean hit) {
    MDC.put(ACTION_KEY, "CACHE_" + operation);
    MDC.put(STATUS_KEY, hit ? "HIT" : "MISS");

    PERFORMANCE_LOGGER.debug(
        "CACHE: {} for key {} - {}", operation, cacheKey, hit ? "HIT" : "MISS");
  }

  /** Log pour les notifications */
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
}
