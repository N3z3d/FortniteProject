package com.fortnite.pronos.service.admin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.admin.VisitAnalyticsDto;

@Service
public class VisitTrackingService {

  private static final int MAX_EVENTS_IN_MEMORY = 50_000;
  private static final int TOP_PAGE_LIMIT = 5;
  private static final int TOP_FLOW_LIMIT = 5;

  private final Deque<VisitEvent> events = new ConcurrentLinkedDeque<>();
  private final Clock clock;

  public VisitTrackingService() {
    this(Clock.systemUTC());
  }

  VisitTrackingService(Clock clock) {
    this.clock = clock;
  }

  public void recordRequest(HttpServletRequest request) {
    if (request == null || !isTrackableRequest(request)) {
      return;
    }
    recordEvent(request, request.getRequestURI());
  }

  public void recordFrontendNavigation(HttpServletRequest request, String rawPath) {
    if (request == null) {
      return;
    }
    String normalizedPath = normalizeFrontendPath(rawPath);
    if (normalizedPath == null) {
      return;
    }
    recordEvent(request, normalizedPath);
  }

  public VisitAnalyticsDto getVisitAnalytics(int hours) {
    Instant cutoff = clock.instant().minus(Duration.ofHours(Math.max(hours, 1)));
    List<VisitEvent> windowEvents =
        events.stream().filter(event -> !event.timestamp().isBefore(cutoff)).toList();
    Map<String, List<VisitEvent>> eventsBySession =
        windowEvents.stream().collect(Collectors.groupingBy(VisitEvent::sessionId));

    return VisitAnalyticsDto.builder()
        .pageViews(windowEvents.size())
        .uniqueVisitors((int) windowEvents.stream().map(VisitEvent::visitorId).distinct().count())
        .activeSessions(eventsBySession.size())
        .averageSessionDurationSeconds(calculateAverageSessionDuration(eventsBySession))
        .bounceRatePercent(calculateBounceRate(eventsBySession))
        .topPages(buildTopPages(windowEvents))
        .topNavigationFlows(buildTopNavigationFlows(eventsBySession))
        .build();
  }

  private boolean isTrackableRequest(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null || !uri.startsWith("/api/")) {
      return false;
    }
    return !uri.startsWith("/api/admin/dashboard/visits")
        && !uri.startsWith("/api/analytics/navigation");
  }

  private String resolveVisitorId(HttpServletRequest request) {
    if (request.getUserPrincipal() != null && request.getUserPrincipal().getName() != null) {
      return request.getUserPrincipal().getName();
    }
    String remoteAddress = Objects.toString(request.getRemoteAddr(), "unknown-ip");
    String userAgent = Objects.toString(request.getHeader("User-Agent"), "unknown-agent");
    return remoteAddress + "|" + userAgent;
  }

  private String resolveSessionId(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      return session.getId();
    }
    return resolveVisitorId(request);
  }

  private void trimOldestIfNeeded() {
    while (events.size() > MAX_EVENTS_IN_MEMORY) {
      events.pollFirst();
    }
  }

  private void recordEvent(HttpServletRequest request, String path) {
    VisitEvent event =
        new VisitEvent(clock.instant(), resolveVisitorId(request), resolveSessionId(request), path);
    events.addLast(event);
    trimOldestIfNeeded();
  }

  private String normalizeFrontendPath(String rawPath) {
    if (rawPath == null) {
      return null;
    }
    String trimmedPath = rawPath.trim();
    if (trimmedPath.isEmpty()) {
      return null;
    }
    int fragmentIndex = trimmedPath.indexOf('#');
    if (fragmentIndex >= 0) {
      trimmedPath = trimmedPath.substring(0, fragmentIndex);
    }
    int queryIndex = trimmedPath.indexOf('?');
    if (queryIndex >= 0) {
      trimmedPath = trimmedPath.substring(0, queryIndex);
    }
    if (!trimmedPath.startsWith("/")) {
      return null;
    }
    return trimmedPath.length() > 255 ? trimmedPath.substring(0, 255) : trimmedPath;
  }

  private double calculateAverageSessionDuration(Map<String, List<VisitEvent>> eventsBySession) {
    if (eventsBySession.isEmpty()) {
      return 0;
    }
    double totalSeconds =
        eventsBySession.values().stream()
            .mapToDouble(
                sessionEvents -> {
                  Instant min =
                      sessionEvents.stream()
                          .map(VisitEvent::timestamp)
                          .min(Comparator.naturalOrder())
                          .orElse(clock.instant());
                  Instant max =
                      sessionEvents.stream()
                          .map(VisitEvent::timestamp)
                          .max(Comparator.naturalOrder())
                          .orElse(min);
                  return Duration.between(min, max).toSeconds();
                })
            .sum();
    return totalSeconds / eventsBySession.size();
  }

  private List<VisitAnalyticsDto.PageViewDto> buildTopPages(List<VisitEvent> eventsWindow) {
    Map<String, Long> viewsByPath =
        eventsWindow.stream()
            .collect(Collectors.groupingBy(VisitEvent::path, Collectors.counting()));
    return viewsByPath.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(TOP_PAGE_LIMIT)
        .map(
            entry ->
                VisitAnalyticsDto.PageViewDto.builder()
                    .path(entry.getKey())
                    .views(entry.getValue())
                    .build())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private double calculateBounceRate(Map<String, List<VisitEvent>> eventsBySession) {
    if (eventsBySession.isEmpty()) {
      return 0;
    }
    long bouncedSessions =
        eventsBySession.values().stream()
            .filter(
                sessionEvents ->
                    sessionEvents.stream().map(VisitEvent::path).distinct().count() <= 1)
            .count();
    return (bouncedSessions * 100.0) / eventsBySession.size();
  }

  private List<VisitAnalyticsDto.NavigationFlowDto> buildTopNavigationFlows(
      Map<String, List<VisitEvent>> eventsBySession) {
    Map<String, Long> transitions = new java.util.HashMap<>();
    eventsBySession
        .values()
        .forEach(sessionEvents -> countSessionTransitions(sessionEvents, transitions));
    return transitions.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(TOP_FLOW_LIMIT)
        .map(entry -> toNavigationFlow(entry.getKey(), entry.getValue()))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private void countSessionTransitions(
      List<VisitEvent> sessionEvents, Map<String, Long> transitionsCounter) {
    List<VisitEvent> sortedEvents =
        sessionEvents.stream().sorted(Comparator.comparing(VisitEvent::timestamp)).toList();
    for (int index = 1; index < sortedEvents.size(); index++) {
      String previousPath = sortedEvents.get(index - 1).path();
      String currentPath = sortedEvents.get(index).path();
      if (previousPath.equals(currentPath)) {
        continue;
      }
      String key = previousPath + "->" + currentPath;
      transitionsCounter.merge(key, 1L, Long::sum);
    }
  }

  private VisitAnalyticsDto.NavigationFlowDto toNavigationFlow(String key, long transitions) {
    String[] paths = key.split("->", 2);
    String fromPath = paths.length > 0 ? paths[0] : "";
    String toPath = paths.length > 1 ? paths[1] : "";
    return VisitAnalyticsDto.NavigationFlowDto.builder()
        .fromPath(fromPath)
        .toPath(toPath)
        .transitions(transitions)
        .build();
  }

  private record VisitEvent(Instant timestamp, String visitorId, String sessionId, String path) {}
}
