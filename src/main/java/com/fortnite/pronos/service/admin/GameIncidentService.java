package com.fortnite.pronos.service.admin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * In-memory circular buffer for recording participant-submitted game incidents. Thread-safe, no
 * external dependencies.
 */
@Slf4j
@Service
public class GameIncidentService {

  static final int MAX_ENTRIES = 500;
  private static final int DEFAULT_LIMIT = 50;

  private final ConcurrentLinkedDeque<IncidentEntry> entries = new ConcurrentLinkedDeque<>();
  private final AtomicInteger size = new AtomicInteger(0);

  /**
   * Records an incident, evicting the oldest entry if the buffer is full.
   *
   * @param entry the incident to record
   */
  public void recordIncident(IncidentEntry entry) {
    entries.addFirst(entry);
    if (size.incrementAndGet() > MAX_ENTRIES) {
      entries.pollLast();
      size.decrementAndGet();
    }
    log.info(
        "Incident recorded: type={}, game={}, reporter={}",
        entry.getIncidentType(),
        entry.getGameId(),
        entry.getReporterUsername());
  }

  /**
   * Returns recent incidents, optionally filtered by game.
   *
   * @param limit maximum number of entries to return (capped at MAX_ENTRIES)
   * @param gameId optional game ID filter (null = all games)
   * @return list of incident entries, most recent first
   */
  public List<IncidentEntry> getRecentIncidents(int limit, UUID gameId) {
    int effectiveLimit = Math.max(1, Math.min(limit, MAX_ENTRIES));
    return entries.stream()
        .filter(e -> gameId == null || e.getGameId().equals(gameId))
        .limit(effectiveLimit)
        .toList();
  }

  /** Returns the current number of entries. */
  public int getCurrentSize() {
    return size.get();
  }

  /** Clears all entries (test/reset use). */
  public void clearAll() {
    entries.clear();
    size.set(0);
    log.info("Incident journal cleared");
  }
}
