package com.fortnite.pronos.service.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.admin.ErrorStatisticsDto;

import lombok.extern.slf4j.Slf4j;

/**
 * In-memory circular buffer for recording handled exceptions. Thread-safe, no external
 * dependencies.
 */
@Slf4j
@Service
public class ErrorJournalService {

  static final int MAX_ENTRIES = 500;
  private static final int DEFAULT_TOP_ERRORS_LIMIT = 10;

  private final ConcurrentLinkedDeque<ErrorEntry> entries = new ConcurrentLinkedDeque<>();
  private final AtomicInteger size = new AtomicInteger(0);

  /**
   * Records an error entry, evicting the oldest entry if the buffer is full.
   *
   * @param entry the error to record
   */
  public void recordError(ErrorEntry entry) {
    entries.addFirst(entry);
    if (size.incrementAndGet() > MAX_ENTRIES) {
      entries.pollLast();
      size.decrementAndGet();
    }
    log.debug("Error recorded: {} - {}", entry.getExceptionType(), entry.getMessage());
  }

  /**
   * Returns recent errors, optionally filtered by status code and/or exception type.
   *
   * @param limit maximum number of entries to return
   * @param statusCode optional HTTP status code filter (null = no filter)
   * @param exceptionType optional exception type filter (null = no filter)
   * @return filtered list of error entries, most recent first
   */
  public List<ErrorEntry> getRecentErrors(int limit, Integer statusCode, String exceptionType) {
    int effectiveLimit = Math.max(1, Math.min(limit, MAX_ENTRIES));

    return entries.stream()
        .filter(e -> statusCode == null || e.getStatusCode() == statusCode)
        .filter(
            e ->
                exceptionType == null
                    || e.getExceptionType().toLowerCase().contains(exceptionType.toLowerCase()))
        .limit(effectiveLimit)
        .toList();
  }

  /**
   * Finds an error entry by its unique ID.
   *
   * @param id the error entry UUID
   * @return the error entry if found
   */
  public Optional<ErrorEntry> findById(UUID id) {
    return entries.stream().filter(e -> e.getId().equals(id)).findFirst();
  }

  /**
   * Computes error statistics for the given time window.
   *
   * @param hours number of hours to look back
   * @return aggregated error statistics
   */
  public ErrorStatisticsDto getErrorStatistics(int hours) {
    int effectiveHours = Math.max(1, hours);
    LocalDateTime since = LocalDateTime.now().minusHours(effectiveHours);
    String trendGranularity = resolveTrendGranularity(effectiveHours);

    List<ErrorEntry> recentEntries =
        entries.stream().filter(e -> e.getTimestamp().isAfter(since)).toList();

    Map<String, Long> byType =
        recentEntries.stream()
            .collect(Collectors.groupingBy(ErrorEntry::getExceptionType, Collectors.counting()));

    Map<Integer, Long> byStatus =
        recentEntries.stream()
            .collect(Collectors.groupingBy(ErrorEntry::getStatusCode, Collectors.counting()));

    List<ErrorStatisticsDto.TopErrorEntry> topErrors = buildTopErrors(recentEntries);
    List<ErrorStatisticsDto.TrendPoint> errorTrend =
        buildErrorTrend(recentEntries, since, trendGranularity);

    return ErrorStatisticsDto.builder()
        .totalErrors(recentEntries.size())
        .errorsByType(byType)
        .errorsByStatusCode(byStatus)
        .topErrors(topErrors)
        .trendGranularity(trendGranularity)
        .errorTrend(errorTrend)
        .build();
  }

  /** Clears all entries from the journal. */
  public void clearAll() {
    entries.clear();
    size.set(0);
    log.info("Error journal cleared");
  }

  /** Returns the current number of entries. */
  public int getCurrentSize() {
    return size.get();
  }

  private List<ErrorStatisticsDto.TopErrorEntry> buildTopErrors(List<ErrorEntry> recentEntries) {
    Map<String, List<ErrorEntry>> grouped =
        recentEntries.stream()
            .collect(Collectors.groupingBy(e -> e.getExceptionType() + ": " + e.getMessage()));

    List<ErrorStatisticsDto.TopErrorEntry> topErrors = new ArrayList<>();
    grouped.forEach(
        (key, group) -> {
          String[] parts = key.split(": ", 2);
          LocalDateTime lastOccurrence =
              group.stream()
                  .map(ErrorEntry::getTimestamp)
                  .max(Comparator.naturalOrder())
                  .orElse(null);

          topErrors.add(
              ErrorStatisticsDto.TopErrorEntry.builder()
                  .type(parts[0])
                  .message(parts.length > 1 ? parts[1] : "")
                  .count(group.size())
                  .lastOccurrence(lastOccurrence)
                  .build());
        });

    topErrors.sort(Comparator.comparingInt(ErrorStatisticsDto.TopErrorEntry::getCount).reversed());

    if (topErrors.size() > DEFAULT_TOP_ERRORS_LIMIT) {
      return new ArrayList<>(topErrors.subList(0, DEFAULT_TOP_ERRORS_LIMIT));
    }
    return topErrors;
  }

  private String resolveTrendGranularity(int hours) {
    return hours <= 48 ? "HOUR" : "DAY";
  }

  private List<ErrorStatisticsDto.TrendPoint> buildErrorTrend(
      List<ErrorEntry> recentEntries, LocalDateTime since, String granularity) {
    if (recentEntries.isEmpty()) {
      return List.of();
    }
    LocalDateTime now = LocalDateTime.now();
    Map<LocalDateTime, Integer> buckets = buildTrendBuckets(recentEntries, granularity);
    return toTrendPoints(since, now, granularity, buckets);
  }

  private Map<LocalDateTime, Integer> buildTrendBuckets(
      List<ErrorEntry> recentEntries, String granularity) {
    Map<LocalDateTime, Integer> buckets = new LinkedHashMap<>();
    for (ErrorEntry entry : recentEntries) {
      LocalDateTime bucket = toBucketStart(entry.getTimestamp(), granularity);
      buckets.merge(bucket, 1, Integer::sum);
    }
    return buckets;
  }

  private List<ErrorStatisticsDto.TrendPoint> toTrendPoints(
      LocalDateTime since,
      LocalDateTime now,
      String granularity,
      Map<LocalDateTime, Integer> buckets) {
    List<ErrorStatisticsDto.TrendPoint> trendPoints = new ArrayList<>();
    LocalDateTime cursor = toBucketStart(since, granularity);
    LocalDateTime lastBucket = toBucketStart(now, granularity);
    while (!cursor.isAfter(lastBucket)) {
      trendPoints.add(
          ErrorStatisticsDto.TrendPoint.builder()
              .periodStart(cursor)
              .count(buckets.getOrDefault(cursor, 0))
              .build());
      cursor = advanceBucket(cursor, granularity);
    }
    return trendPoints;
  }

  private LocalDateTime toBucketStart(LocalDateTime timestamp, String granularity) {
    if ("DAY".equals(granularity)) {
      return timestamp.toLocalDate().atStartOfDay();
    }
    return timestamp.withMinute(0).withSecond(0).withNano(0);
  }

  private LocalDateTime advanceBucket(LocalDateTime bucketStart, String granularity) {
    if ("DAY".equals(granularity)) {
      return bucketStart.plusDays(1);
    }
    return bucketStart.plusHours(1);
  }
}
