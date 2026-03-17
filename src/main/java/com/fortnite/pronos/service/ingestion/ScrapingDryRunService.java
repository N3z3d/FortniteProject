package com.fortnite.pronos.service.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.admin.DryRunResultDto;
import com.fortnite.pronos.model.PrRegion;

/**
 * Triggers a manual dry-run of the PR scraping adapter on a single region and validates the result.
 * Intended for admin use before enabling scheduled ingestion.
 */
@Service
public class ScrapingDryRunService {

  private static final Logger log = LoggerFactory.getLogger(ScrapingDryRunService.class);
  private static final int SMOKE_MIN_ROWS = 10;
  private static final int SAMPLE_MAX_ROWS = 5;
  private static final int SCORE_MIN = 1;
  private static final int SCORE_MAX = 9_999_999;
  // CSV header: nickname,region,points,rank,snapshot_date  →  points at index 2
  private static final int POINTS_COLUMN_INDEX = 2;

  private final PrRegionCsvSourcePort csvSourcePort;

  public ScrapingDryRunService(PrRegionCsvSourcePort csvSourcePort) {
    this.csvSourcePort = csvSourcePort;
  }

  public DryRunResultDto runDryRun(PrRegion region) {
    log.info("Starting scraping dry-run for region={}", region.name());
    Optional<String> csvOpt = csvSourcePort.fetchCsv(region);

    if (csvOpt.isEmpty()) {
      log.warn("Dry-run returned no data for region={}", region.name());
      return new DryRunResultDto(
          region.name(), 0, false, List.of(), List.of("No scraping providers configured"));
    }

    String csv = csvOpt.get();
    String[] lines = csv.split("\n");
    List<String> dataLines = new ArrayList<>();
    for (int i = 1; i < lines.length; i++) { // skip header at index 0
      String line = lines[i].trim();
      if (!line.isEmpty()) {
        dataLines.add(line);
      }
    }

    int rowCount = dataLines.size();
    List<String> errors = new ArrayList<>();

    // Smoke check: at least 10 rows expected
    if (rowCount < SMOKE_MIN_ROWS) {
      errors.add(
          "Smoke check failed: expected at least " + SMOKE_MIN_ROWS + " rows, got " + rowCount);
    }

    // Score validation: points must be in range [1, 9_999_999]
    for (String line : dataLines) {
      String[] fields = line.split(",");
      if (fields.length > POINTS_COLUMN_INDEX) {
        try {
          int points = Integer.parseInt(fields[POINTS_COLUMN_INDEX].trim());
          if (points < SCORE_MIN || points > SCORE_MAX) {
            errors.add(
                "Score validation failed: points="
                    + points
                    + " out of range ["
                    + SCORE_MIN
                    + ", "
                    + SCORE_MAX
                    + "] in row: "
                    + line);
            break; // report first offending row only
          }
        } catch (NumberFormatException e) {
          errors.add("Score validation failed: non-numeric points field in row: " + line);
          break;
        }
      }
    }

    boolean valid = errors.isEmpty();
    List<String> sampleRows = dataLines.subList(0, Math.min(SAMPLE_MAX_ROWS, dataLines.size()));

    log.info(
        "Dry-run complete for region={}: rowCount={}, valid={}, errors={}",
        region.name(),
        rowCount,
        valid,
        errors);
    return new DryRunResultDto(region.name(), rowCount, valid, List.copyOf(sampleRows), errors);
  }
}
