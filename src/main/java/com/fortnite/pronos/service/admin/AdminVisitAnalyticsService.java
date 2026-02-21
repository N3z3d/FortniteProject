package com.fortnite.pronos.service.admin;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.admin.VisitAnalyticsDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminVisitAnalyticsService {

  private static final int DEFAULT_HOURS = 24;

  private final VisitTrackingService visitTrackingService;

  public VisitAnalyticsDto getVisitAnalytics(int hours) {
    int effectiveHours = hours > 0 ? hours : DEFAULT_HOURS;
    return visitTrackingService.getVisitAnalytics(effectiveHours);
  }
}
