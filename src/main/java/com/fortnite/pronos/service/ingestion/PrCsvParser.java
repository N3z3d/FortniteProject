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

import org.springframework.stereotype.Component;

@Component
public class PrCsvParser {
  private static final List<String> REQUIRED_HEADERS =
      List.of("nickname", "region", "points", "rank", "snapshot_date");
  private static final List<String> ALLOWED_REGIONS =
      List.of("EU", "NAC", "NAW", "BR", "ASIA", "OCE", "ME", "GLOBAL");

  public ParseResult parse(Reader reader) {
    List<PrCsvRow> rows = new ArrayList<>();
    int errorCount = 0;
    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      String headerLine = bufferedReader.readLine();
      if (headerLine == null) {
        return ParseResult.failure("invalid_header", 0);
      }
      Map<String, Integer> index = headerIndex(headerLine);
      if (!hasRequiredHeaders(index)) {
        return ParseResult.failure("invalid_header", 0);
      }
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        if (line.isBlank() || line.trim().startsWith("#")) {
          continue;
        }
        PrCsvRow row = parseRow(line, index);
        if (row == null) {
          errorCount++;
          continue;
        }
        rows.add(row);
      }
    } catch (IOException e) {
      return ParseResult.failure("io_error", errorCount);
    }
    if (rows.isEmpty()) {
      return ParseResult.failure("no_rows", errorCount);
    }
    return ParseResult.success(rows, errorCount);
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
    String pointsRaw = clean(readValue(parts, index.get("points")));
    String rankRaw = clean(readValue(parts, index.get("rank")));
    String dateRaw = clean(readValue(parts, index.get("snapshot_date")));

    if (nickname.isEmpty() || regionRaw.isEmpty() || dateRaw.isEmpty()) {
      return null;
    }
    String region = regionRaw.toUpperCase(Locale.ROOT);
    if (!ALLOWED_REGIONS.contains(region)) {
      return null;
    }
    Integer points = parseInt(pointsRaw);
    Integer rank = parseInt(rankRaw);
    LocalDate snapshotDate = parseDate(dateRaw);
    if (points == null || rank == null || snapshotDate == null) {
      return null;
    }
    return new PrCsvRow(nickname, region, points, rank, snapshotDate);
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
}
