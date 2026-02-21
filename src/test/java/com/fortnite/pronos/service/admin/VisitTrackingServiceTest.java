package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.dto.admin.VisitAnalyticsDto;

class VisitTrackingServiceTest {

  @Test
  void shouldComputeVisitMetricsForTrackedApiRequests() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock);
    service.recordRequest(request("/api/games", "thibaut", "S1"));
    clock.plusSeconds(120);
    service.recordRequest(request("/api/trades", "thibaut", "S1"));
    clock.plusSeconds(60);
    service.recordRequest(request("/api/games", "teddy", "S2"));

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getPageViews()).isEqualTo(3);
    assertThat(result.getUniqueVisitors()).isEqualTo(2);
    assertThat(result.getActiveSessions()).isEqualTo(2);
    assertThat(result.getAverageSessionDurationSeconds()).isEqualTo(60);
    assertThat(result.getBounceRatePercent()).isEqualTo(50);
    assertThat(result.getTopPages()).isNotEmpty();
    assertThat(result.getTopPages().get(0).getPath()).isEqualTo("/api/games");
    assertThat(result.getTopPages().get(0).getViews()).isEqualTo(2);
    assertThat(result.getTopNavigationFlows()).hasSize(1);
    assertThat(result.getTopNavigationFlows().get(0).getFromPath()).isEqualTo("/api/games");
    assertThat(result.getTopNavigationFlows().get(0).getToPath()).isEqualTo("/api/trades");
  }

  @Test
  void shouldIgnoreNonApiRequests() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock);
    MockHttpServletRequest request = request("/assets/i18n/fr.json", "thibaut", "S1");
    service.recordRequest(request);

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getPageViews()).isZero();
    assertThat(result.getBounceRatePercent()).isZero();
    assertThat(result.getTopPages()).isEqualTo(List.of());
    assertThat(result.getTopNavigationFlows()).isEqualTo(List.of());
  }

  @Test
  void shouldFilterEventsOutsideHoursWindow() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock);
    service.recordRequest(request("/api/games", "thibaut", "S1"));
    clock.plusSeconds(60 * 60 * 25L);
    service.recordRequest(request("/api/games", "thibaut", "S1"));

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getPageViews()).isEqualTo(1);
  }

  @Test
  void shouldComputeTransitionsAcrossMultiplePagesInSameSession() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock);
    service.recordRequest(request("/api/games", "thibaut", "S1"));
    clock.plusSeconds(10);
    service.recordRequest(request("/api/teams", "thibaut", "S1"));
    clock.plusSeconds(10);
    service.recordRequest(request("/api/trades", "thibaut", "S1"));

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getBounceRatePercent()).isZero();
    assertThat(result.getTopNavigationFlows()).hasSize(2);
    assertThat(result.getTopNavigationFlows())
        .anySatisfy(
            flow -> {
              assertThat(flow.getFromPath()).isEqualTo("/api/games");
              assertThat(flow.getToPath()).isEqualTo("/api/teams");
              assertThat(flow.getTransitions()).isEqualTo(1);
            });
    assertThat(result.getTopNavigationFlows())
        .anySatisfy(
            flow -> {
              assertThat(flow.getFromPath()).isEqualTo("/api/teams");
              assertThat(flow.getToPath()).isEqualTo("/api/trades");
              assertThat(flow.getTransitions()).isEqualTo(1);
            });
  }

  @Test
  void shouldRecordFrontendNavigationWithNormalizedPath() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock);

    service.recordFrontendNavigation(
        request("/api/analytics/navigation", "thibaut", "S1"), "/games/42?created=true#info");

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getPageViews()).isEqualTo(1);
    assertThat(result.getTopPages()).hasSize(1);
    assertThat(result.getTopPages().get(0).getPath()).isEqualTo("/games/42");
  }

  @Test
  void shouldIgnoreInvalidFrontendNavigationPath() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock);

    service.recordFrontendNavigation(
        request("/api/analytics/navigation", "thibaut", "S1"), "games/42");
    service.recordFrontendNavigation(request("/api/analytics/navigation", "thibaut", "S1"), "   ");

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getPageViews()).isZero();
  }

  private MockHttpServletRequest request(String uri, String userName, String sessionId) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(uri);
    request.setUserPrincipal(() -> userName);
    request.setRemoteAddr("127.0.0.1");
    request.setSession(new org.springframework.mock.web.MockHttpSession(null, sessionId));
    return request;
  }

  private static final class MutableClock extends Clock {
    private Instant current;

    private MutableClock(Instant initialInstant) {
      this.current = initialInstant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return current;
    }

    private void plusSeconds(long seconds) {
      current = current.plusSeconds(seconds);
    }
  }
}
