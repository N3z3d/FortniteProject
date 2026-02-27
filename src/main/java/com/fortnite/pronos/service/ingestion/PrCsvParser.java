package com.fortnite.pronos.service.ingestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings({"java:S135"})
public class PrCsvParser {
  private static final Logger LOG = LoggerFactory.getLogger(PrCsvParser.class);
  private static final List<String> REQUIRED_HEADERS =
      List.of("nickname", "region", "points", "rank", "snapshot_date");
  private static final List<String> ALLOWED_REGIONS =
      List.of("EU", "NAC", "NAW", "BR", "ASIA", "OCE", "ME", "GLOBAL");

  public ParseResult parse(Reader reader) {
    List<PrCsvRow> rows = new ArrayList<>();
    int errorCount = 0;
    String failureReason = null;
    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      String headerLine = bufferedReader.readLine();
      Map<String, Integer> index = headerLine == null ? Map.of() : headerIndex(headerLine);
      if (headerLine == null || !hasRequiredHeaders(index)) {
        failureReason = "invalid_header";
      } else {
        RowParsingResult rowParsingResult = parseRows(bufferedReader, index);
        rows.addAll(rowParsingResult.rows());
        errorCount = rowParsingResult.errorCount();
      }
    } catch (IOException e) {
      LOG.warn("I/O error while parsing PR CSV input", e);
      failureReason = "io_error";
    }
    if (failureReason == null && rows.isEmpty()) {
      failureReason = "no_rows";
    }
    if (failureReason != null) {
      return ParseResult.failure(failureReason, errorCount);
    }
    return ParseResult.success(rows, errorCount);
  }

  private RowParsingResult parseRows(BufferedReader bufferedReader, Map<String, Integer> index)
      throws IOException {
    List<PrCsvRow> rows = new ArrayList<>();
    int errorCount = 0;
    String line;
    while ((line = bufferedReader.readLine()) != null) {
      if (line.isBlank() || line.trim().startsWith("#")) {
        continue;
      }
      PrCsvRow row = parseRow(line, index);
      if (row == null) {
        errorCount += 1;
      } else {
        rows.add(row);
      }
    }
    return new RowParsingResult(rows, errorCount);
  }

  private Map<String, Integer> headerIndex(String headerLine) {
    String[] headers = headerLine.split(",", -1);
    Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < headers.length; i++) {
      String key = clean(headers[i]).toLowerCase(Locale.ROOT);
      if (!key.isEmpty()) {
        index.putIfAbsent(key, i);
      }
    }
    return index;
  }

  private boolean hasRequiredHeaders(Map<String, Integer> index) {
    for (String required : REQUIRED_HEADERS) {
      if (!index.containsKey(required)) {
        return false;
      }
    }
    return true;
  }

  private PrCsvRow parseRow(String line, Map<String, Integer> index) {
    String[] parts = line.split(",", -1);
    String nickname = clean(readValue(parts, index.get("nickname")));
    String regionRaw = clean(readValue(parts, index.get("region")));
    String dateRaw = clean(readValue(parts, index.get("snapshot_date")));

    PrCsvRow parsedRow = null;
    if (!nickname.isEmpty() && !regionRaw.isEmpty() && !dateRaw.isEmpty()) {
      String region = regionRaw.toUpperCase(Locale.ROOT);
      if (ALLOWED_REGIONS.contains(region)) {
        LocalDate snapshotDate = parseDate(dateRaw);
        String pointsRaw = clean(readValue(parts, index.get("points")));
        String rankRaw = clean(readValue(parts, index.get("rank")));
        Integer points = parseInt(pointsRaw);
        Integer rank = parseInt(rankRaw);
        if (points != null && rank != null && snapshotDate != null) {
          parsedRow = new PrCsvRow(nickname, region, points, rank, snapshotDate);
        }
      }
    }
    return parsedRow;
  }

  private String readValue(String[] parts, Integer index) {
    if (index == null || index < 0 || index >= parts.length) {
      return "";
    }
    return parts[index];
  }

  private String clean(String value) {
    return value == null ? "" : value.trim().replace("\"", "");
  }

  private Integer parseInt(String value) {
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private LocalDate parseDate(String value) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public record PrCsvRow(
      String nickname, String region, int points, int rank, LocalDate snapshotDate) {}

  public record ParseResult(List<PrCsvRow> rows, int errorCount, String failureReason) {
    public static ParseResult success(List<PrCsvRow> rows, int errorCount) {
      return new ParseResult(rows, errorCount, null);
    }

    public static ParseResult failure(String reason, int errorCount) {
      return new ParseResult(List.of(), errorCount, reason);
    }
  }

  private record RowParsingResult(List<PrCsvRow> rows, int errorCount) {}
}
