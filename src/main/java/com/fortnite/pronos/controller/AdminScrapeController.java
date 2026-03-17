package com.fortnite.pronos.controller;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.DryRunResultDto;
import com.fortnite.pronos.dto.admin.PipelineAlertDto;
import com.fortnite.pronos.dto.admin.ScrapeLogDto;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.admin.ScrapeLogService;
import com.fortnite.pronos.service.admin.UnresolvedAlertService;
import com.fortnite.pronos.service.ingestion.ScrapingDryRunService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/scraping")
@RequiredArgsConstructor
@Validated
public class AdminScrapeController {

  private final ScrapeLogService scrapeLogService;
  private final UnresolvedAlertService unresolvedAlertService;
  private final ScrapingDryRunService scrapingDryRunService;

  @GetMapping("/logs")
  public ResponseEntity<List<ScrapeLogDto>> getLogs(
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
    return ResponseEntity.ok(scrapeLogService.getRecentLogs(limit));
  }

  @GetMapping("/alert")
  public ResponseEntity<PipelineAlertDto> getAlert() {
    return ResponseEntity.ok(unresolvedAlertService.getAlertStatus());
  }

  @PostMapping("/dry-run")
  public ResponseEntity<?> dryRun(@RequestParam(defaultValue = "EU") String region) {
    PrRegion prRegion;
    try {
      prRegion = PrRegion.valueOf(region.toUpperCase());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("Unknown region: " + region);
    }
    DryRunResultDto result = scrapingDryRunService.runDryRun(prRegion);
    return ResponseEntity.ok(result);
  }
}
