package com.fortnite.pronos.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.ErrorStatisticsDto;
import com.fortnite.pronos.dto.common.ApiResponse;
import com.fortnite.pronos.service.admin.ErrorEntry;
import com.fortnite.pronos.service.admin.ErrorJournalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Admin API for browsing the in-memory error journal. */
@Slf4j
@RestController
@RequestMapping("/api/admin/errors")
@RequiredArgsConstructor
public class ErrorJournalController {

  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 500;
  private static final int DEFAULT_STATS_HOURS = 24;
  private static final int MAX_STATS_HOURS = 168;

  private final ErrorJournalService errorJournalService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ErrorEntry>>> getErrors(
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(required = false) Integer statusCode,
      @RequestParam(required = false) String type) {

    int effectiveLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
    log.info(
        "Admin: fetching error journal (limit={}, status={}, type={})",
        effectiveLimit,
        statusCode,
        type);

    List<ErrorEntry> errors = errorJournalService.getRecentErrors(effectiveLimit, statusCode, type);
    return ResponseEntity.ok(ApiResponse.success(errors));
  }

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<ErrorStatisticsDto>> getErrorStatistics(
      @RequestParam(defaultValue = "24") int hours) {

    int effectiveHours = Math.max(1, Math.min(hours, MAX_STATS_HOURS));
    log.info("Admin: fetching error statistics for last {} hours", effectiveHours);

    ErrorStatisticsDto stats = errorJournalService.getErrorStatistics(effectiveHours);
    return ResponseEntity.ok(ApiResponse.success(stats));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ErrorEntry>> getErrorDetail(@PathVariable UUID id) {
    log.info("Admin: fetching error detail for {}", id);

    return errorJournalService
        .findById(id)
        .map(entry -> ResponseEntity.ok(ApiResponse.success(entry)))
        .orElse(
            ResponseEntity.status(404)
                .body(ApiResponse.error("Error entry not found", "ERROR_NOT_FOUND")));
  }
}
