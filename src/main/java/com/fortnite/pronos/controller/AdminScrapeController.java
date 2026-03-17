package com.fortnite.pronos.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.DryRunResultDto;
import com.fortnite.pronos.dto.admin.IngestionTriggerResultDto;
import com.fortnite.pronos.dto.admin.PipelineAlertDto;
import com.fortnite.pronos.dto.admin.ScrapeLogDto;
import com.fortnite.pronos.model.PrRegion;
import com.fortnite.pronos.service.admin.ScrapeLogService;
import com.fortnite.pronos.service.admin.UnresolvedAlertService;
import com.fortnite.pronos.service.ingestion.PrIngestionOrchestrationService;
import com.fortnite.pronos.service.ingestion.PrIngestionOrchestrationService.MultiRegionIngestionResult;
import com.fortnite.pronos.service.ingestion.ScrapingDryRunService;

@RestController
@RequestMapping("/api/admin/scraping")
@Validated
public class AdminScrapeController {

  private final ScrapeLogService scrapeLogService;
  private final UnresolvedAlertService unresolvedAlertService;
  private final ScrapingDryRunService scrapingDryRunService;
  private final Optional<PrIngestionOrchestrationService> orchestrationService;

  public AdminScrapeController(
      ScrapeLogService scrapeLogService,
      UnresolvedAlertService unresolvedAlertService,
      ScrapingDryRunService scrapingDryRunService,
      Optional<PrIngestionOrchestrationService> orchestrationService) {
    this.scrapeLogService = scrapeLogService;
    this.unresolvedAlertService = unresolvedAlertService;
    this.scrapingDryRunService = scrapingDryRunService;
    this.orchestrationService = orchestrationService;
  }

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
  public ResponseEntity<Object> dryRun(@RequestParam(defaultValue = "EU") String region) {
    PrRegion prRegion;
    try {
      prRegion = PrRegion.valueOf(region.toUpperCase());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("Unknown region: " + region);
    }
    DryRunResultDto result = scrapingDryRunService.runDryRun(prRegion);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/trigger")
  public ResponseEntity<Object> triggerIngestion() {
    if (orchestrationService.isEmpty()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              Map.of(
                  "error",
                  "Scheduled ingestion is disabled (ingestion.pr.scheduled.enabled=false)"));
    }
    MultiRegionIngestionResult result = orchestrationService.get().runAllRegions();
    Map<String, String> failures =
        Map.copyOf(
            result.regionFailures().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)));
    IngestionTriggerResultDto dto =
        new IngestionTriggerResultDto(
            result.status().name(), result.regionsProcessed(), failures, result.durationMs());
    return ResponseEntity.ok(dto);
  }
}
