package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.dto.admin.RealTimeAnalyticsDto;
import com.fortnite.pronos.dto.admin.VisitAnalyticsDto;

class VisitTrackingServiceTest {

  @Test
  void shouldComputeVisitMetricsForTrackedApiRequests() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
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
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
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
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
    service.recordRequest(request("/api/games", "thibaut", "S1"));
    clock.plusSeconds(60 * 60 * 25L);
    service.recordRequest(request("/api/games", "thibaut", "S1"));

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getPageViews()).isEqualTo(1);
  }

  @Test
  void shouldComputeTransitionsAcrossMultiplePagesInSameSession() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
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
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());

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
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());

    service.recordFrontendNavigation(
        request("/api/analytics/navigation", "thibaut", "S1"), "games/42");
    service.recordFrontendNavigation(request("/api/analytics/navigation", "thibaut", "S1"), "   ");

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getPageViews()).isZero();
  }

  @Test
  void shouldTruncateLongFrontendNavigationPath() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
    String longPath = "/games/" + "a".repeat(400);

    service.recordFrontendNavigation(
        request("/api/analytics/navigation", "thibaut", "S1"), longPath);

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getPageViews()).isEqualTo(1);
    assertThat(result.getTopPages()).hasSize(1);
    assertThat(result.getTopPages().get(0).getPath()).hasSize(255);
  }

  @Test
  void shouldBuildTopCountriesFromCfIpCountryHeader() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());

    MockHttpServletRequest fr1 = request("/api/games", "userA", "S1");
    fr1.addHeader("CF-IPCountry", "FR");
    MockHttpServletRequest fr2 = request("/api/trades", "userB", "S2");
    fr2.addHeader("CF-IPCountry", "FR");
    MockHttpServletRequest us1 = request("/api/games", "userC", "S3");
    us1.addHeader("CF-IPCountry", "US");

    service.recordRequest(fr1);
    service.recordRequest(fr2);
    service.recordRequest(us1);

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getTopCountries()).hasSize(2);
    assertThat(result.getTopCountries().get(0).getCountry()).isEqualTo("FR");
    assertThat(result.getTopCountries().get(0).getVisitCount()).isEqualTo(2);
    assertThat(result.getTopCountries().get(1).getCountry()).isEqualTo("US");
    assertThat(result.getTopCountries().get(1).getVisitCount()).isEqualTo(1);
  }

  @Test
  void shouldClassifyPrivateIpAsLocal() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());

    MockHttpServletRequest localReq = request("/api/games", "userA", "S1");
    localReq.setRemoteAddr("127.0.0.1");
    service.recordRequest(localReq);

    VisitAnalyticsDto result = service.getVisitAnalytics(24);

    assertThat(result.getTopCountries()).hasSize(1);
    assertThat(result.getTopCountries().get(0).getCountry()).isEqualTo(GeoResolutionService.LOCAL);
  }

  @Test
  void shouldReturnZeroActiveUsersWhenNoEvents() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());

    RealTimeAnalyticsDto result = service.getRealTimeSnapshot();

    assertThat(result.getActiveUsersNow()).isZero();
    assertThat(result.getActiveSessionsNow()).isZero();
    assertThat(result.getActivePagesNow()).isEmpty();
  }

  @Test
  void shouldCountActiveUsersWithinFiveMinuteWindow() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
    service.recordRequest(request("/api/games", "alice", "S1"));
    service.recordRequest(request("/api/games", "bob", "S2"));

    RealTimeAnalyticsDto result = service.getRealTimeSnapshot();

    assertThat(result.getActiveUsersNow()).isEqualTo(2);
    assertThat(result.getActiveSessionsNow()).isEqualTo(2);
  }

  @Test
  void shouldExcludeEventsOlderThanFiveMinutesFromActiveUsers() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
    service.recordRequest(request("/api/games", "oldUser", "S0"));
    clock.plusSeconds(6 * 60);

    RealTimeAnalyticsDto result = service.getRealTimeSnapshot();

    assertThat(result.getActiveUsersNow()).isZero();
  }

  @Test
  void shouldListActivePagesFromLastTwoMinutes() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
    service.recordRequest(request("/api/games", "alice", "S1"));
    service.recordRequest(request("/api/games", "bob", "S2"));
    service.recordRequest(request("/api/trades", "alice", "S1"));

    RealTimeAnalyticsDto result = service.getRealTimeSnapshot();

    assertThat(result.getActivePagesNow()).hasSize(2);
    assertThat(result.getActivePagesNow().get(0).getPath()).isEqualTo("/api/games");
    assertThat(result.getActivePagesNow().get(0).getVisitorCount()).isEqualTo(2);
  }

  @Test
  void shouldExcludeOldEventsFromActivePagesButKeepThemInActiveUsers() {
    MutableClock clock = new MutableClock(Instant.parse("2026-02-21T09:00:00Z"));
    VisitTrackingService service = new VisitTrackingService(clock, new GeoResolutionService());
    service.recordRequest(request("/api/games", "alice", "S1"));
    clock.plusSeconds(3 * 60); // 3 min later: outside 2-min page window, within 5-min user window
    service.recordRequest(request("/api/trades", "bob", "S2"));

    RealTimeAnalyticsDto result = service.getRealTimeSnapshot();

    assertThat(result.getActivePagesNow()).hasSize(1);
    assertThat(result.getActivePagesNow().get(0).getPath()).isEqualTo("/api/trades");
    assertThat(result.getActiveUsersNow()).isEqualTo(2);
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
