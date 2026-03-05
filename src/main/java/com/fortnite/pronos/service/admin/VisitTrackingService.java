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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.admin.RealTimeAnalyticsDto;
import com.fortnite.pronos.dto.admin.VisitAnalyticsDto;

@Service
public class VisitTrackingService {

  private static final int MAX_EVENTS_IN_MEMORY = 50_000;
  private static final int TOP_PAGE_LIMIT = 5;
  private static final int TOP_FLOW_LIMIT = 5;
  private static final int TOP_COUNTRY_LIMIT = 5;
  private static final int MAX_FRONTEND_PATH_LENGTH = 255;
  private static final double PERCENT_MULTIPLIER = 100.0;
  private static final String NAVIGATION_SEPARATOR = "->";
  private static final int NAVIGATION_SPLIT_LIMIT = 2;
  private static final Pattern NAVIGATION_SEPARATOR_PATTERN =
      Pattern.compile(Pattern.quote(NAVIGATION_SEPARATOR));
  private static final long ACTIVE_USER_WINDOW_MINUTES = 5;
  private static final long ACTIVE_PAGE_WINDOW_MINUTES = 2;

  private final Deque<VisitEvent> events = new ConcurrentLinkedDeque<>();
  private final AtomicInteger eventCount = new AtomicInteger();
  private final Clock clock;
  private final GeoResolutionService geoResolutionService;

  @Autowired
  public VisitTrackingService(GeoResolutionService geoResolutionService) {
    this(Clock.systemUTC(), geoResolutionService);
  }

  VisitTrackingService(Clock clock, GeoResolutionService geoResolutionService) {
    this.clock = clock;
    this.geoResolutionService = geoResolutionService;
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

  public RealTimeAnalyticsDto getRealTimeSnapshot() {
    Instant fiveMinutesAgo = clock.instant().minus(Duration.ofMinutes(ACTIVE_USER_WINDOW_MINUTES));
    Instant twoMinutesAgo = clock.instant().minus(Duration.ofMinutes(ACTIVE_PAGE_WINDOW_MINUTES));

    List<VisitEvent> activeEvents =
        events.stream().filter(e -> !e.timestamp().isBefore(fiveMinutesAgo)).toList();
    List<VisitEvent> recentPageEvents =
        events.stream().filter(e -> !e.timestamp().isBefore(twoMinutesAgo)).toList();

    return RealTimeAnalyticsDto.builder()
        .activeUsersNow((int) activeEvents.stream().map(VisitEvent::visitorId).distinct().count())
        .activeSessionsNow(
            (int) activeEvents.stream().map(VisitEvent::sessionId).distinct().count())
        .activePagesNow(buildActivePages(recentPageEvents))
        .build();
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
        .topCountries(buildTopCountries(windowEvents))
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

  private void trimOldestIfNeeded(int currentEventCount) {
    int remainingCount = currentEventCount;
    while (remainingCount > MAX_EVENTS_IN_MEMORY) {
      VisitEvent removed = events.pollFirst();
      if (removed == null) {
        eventCount.set(0);
        return;
      }
      remainingCount = eventCount.decrementAndGet();
    }
  }

  private void recordEvent(HttpServletRequest request, String path) {
    String country = geoResolutionService.resolveCountry(request);
    VisitEvent event =
        new VisitEvent(
            clock.instant(), resolveVisitorId(request), resolveSessionId(request), path, country);
    events.addLast(event);
    trimOldestIfNeeded(eventCount.incrementAndGet());
  }

  private List<VisitAnalyticsDto.GeoDistributionDto> buildTopCountries(
      List<VisitEvent> eventsWindow) {
    Map<String, Long> visitsByCountry =
        eventsWindow.stream()
            .collect(Collectors.groupingBy(VisitEvent::country, Collectors.counting()));
    return visitsByCountry.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(TOP_COUNTRY_LIMIT)
        .map(
            entry ->
                VisitAnalyticsDto.GeoDistributionDto.builder()
                    .country(entry.getKey())
                    .visitCount(entry.getValue())
                    .build())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private String normalizeFrontendPath(String rawPath) {
    String cleanedPath = sanitizeNavigationPath(rawPath);
    if (cleanedPath == null || !cleanedPath.startsWith("/")) {
      return null;
    }
    if (cleanedPath.length() <= MAX_FRONTEND_PATH_LENGTH) {
      return cleanedPath;
    }
    return cleanedPath.substring(0, MAX_FRONTEND_PATH_LENGTH);
  }

  private String sanitizeNavigationPath(String rawPath) {
    if (rawPath == null) {
      return null;
    }
    String trimmedPath = rawPath.trim();
    if (trimmedPath.isEmpty()) {
      return null;
    }
    String noFragment = removeSuffixFromCharacter(trimmedPath, '#');
    return removeSuffixFromCharacter(noFragment, '?');
  }

  private String removeSuffixFromCharacter(String value, char separator) {
    int separatorIndex = value.indexOf(separator);
    if (separatorIndex < 0) {
      return value;
    }
    return value.substring(0, separatorIndex);
  }

  private double calculateAverageSessionDuration(Map<String, List<VisitEvent>> eventsBySession) {
    if (eventsBySession.isEmpty()) {
      return 0;
    }
    double totalSeconds =
        eventsBySession.values().stream()
            .mapToDouble(this::calculateSessionDurationInSeconds)
            .sum();
    return totalSeconds / eventsBySession.size();
  }

  private double calculateSessionDurationInSeconds(List<VisitEvent> sessionEvents) {
    Instant minTimestamp =
        sessionEvents.stream()
            .map(VisitEvent::timestamp)
            .min(Comparator.naturalOrder())
            .orElse(clock.instant());
    Instant maxTimestamp =
        sessionEvents.stream()
            .map(VisitEvent::timestamp)
            .max(Comparator.naturalOrder())
            .orElse(minTimestamp);
    return Duration.between(minTimestamp, maxTimestamp).toSeconds();
  }

  private List<RealTimeAnalyticsDto.ActivePageDto> buildActivePages(List<VisitEvent> recentEvents) {
    Map<String, Long> viewsByPath =
        recentEvents.stream()
            .collect(Collectors.groupingBy(VisitEvent::path, Collectors.counting()));
    return viewsByPath.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(TOP_PAGE_LIMIT)
        .map(
            entry ->
                RealTimeAnalyticsDto.ActivePageDto.builder()
                    .path(entry.getKey())
                    .visitorCount(entry.getValue().intValue())
                    .build())
        .collect(Collectors.toCollection(ArrayList::new));
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
    return (bouncedSessions * PERCENT_MULTIPLIER) / eventsBySession.size();
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
    String[] paths = NAVIGATION_SEPARATOR_PATTERN.split(key, NAVIGATION_SPLIT_LIMIT);
    String fromPath = paths.length > 0 ? paths[0] : "";
    String toPath = paths.length > 1 ? paths[1] : "";
    return VisitAnalyticsDto.NavigationFlowDto.builder()
        .fromPath(fromPath)
        .toPath(toPath)
        .transitions(transitions)
        .build();
  }

  private record VisitEvent(
      Instant timestamp, String visitorId, String sessionId, String path, String country) {}
}
