package com.fortnite.pronos.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.NavigationTrackingRequestDto;
import com.fortnite.pronos.dto.common.ApiResponse;
import com.fortnite.pronos.service.admin.VisitTrackingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class VisitTrackingController {

  private final VisitTrackingService visitTrackingService;

  @PostMapping("/navigation")
  public ResponseEntity<ApiResponse<Void>> trackNavigation(
      @Valid @RequestBody NavigationTrackingRequestDto request,
      HttpServletRequest httpServletRequest) {
    visitTrackingService.recordFrontendNavigation(httpServletRequest, request.path());
    return ResponseEntity.ok(ApiResponse.success(null, "Navigation tracked"));
  }
}
