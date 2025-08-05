package com.fortnite.pronos.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import lombok.extern.slf4j.Slf4j;

/** Intercepteur de logging pour les performances API Optimisé pour 147+ joueurs simultanés */
@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    long startTime = System.currentTimeMillis();
    request.setAttribute("startTime", startTime);

    // Log uniquement les endpoints critiques pour réduire le bruit
    String uri = request.getRequestURI();
    if (isCriticalEndpoint(uri)) {
      log.debug("START {} {}", request.getMethod(), uri);
    }

    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    Long startTime = (Long) request.getAttribute("startTime");
    if (startTime != null) {
      long executionTime = System.currentTimeMillis() - startTime;
      String uri = request.getRequestURI();

      // Performance monitoring - log lent uniquement
      if (executionTime > 100 || isCriticalEndpoint(uri)) {
        log.info(
            "COMPLETED {} {} - {}ms - Status: {}",
            request.getMethod(),
            uri,
            executionTime,
            response.getStatus());
      }

      // Alert pour les requêtes très lentes
      if (executionTime > 1000) {
        log.warn(
            "SLOW REQUEST {} {} - {}ms - Potential performance issue",
            request.getMethod(),
            uri,
            executionTime);
      }
    }

    if (ex != null) {
      log.error("Request failed: {} {}", request.getMethod(), request.getRequestURI(), ex);
    }
  }

  /** Identifier les endpoints critiques nécessitant un monitoring spécial */
  private boolean isCriticalEndpoint(String uri) {
    return uri.contains("/leaderboard")
        || uri.contains("/games")
        || uri.contains("/players")
        || uri.contains("/draft");
  }
}
