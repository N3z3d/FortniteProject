package com.fortnite.pronos.service.admin;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * In-memory circular buffer for admin write actions. Thread-safe, survives application restarts
 * with the last {@value #MAX_ENTRIES} entries.
 */
@Slf4j
@Service
public class AdminAuditLogService {

  static final int MAX_ENTRIES = 200;

  private final ConcurrentLinkedDeque<AdminAuditEntry> entries = new ConcurrentLinkedDeque<>();
  private final AtomicInteger size = new AtomicInteger(0);

  /**
   * Records an admin action, evicting the oldest entry when the buffer is full.
   *
   * @param actor username of the admin performing the action
   * @param action short verb describing the operation (e.g. "CORRECT_METADATA")
   * @param entityType type of the entity affected (e.g. "PLAYER_IDENTITY")
   * @param entityId identifier of the affected entity (may be null)
   * @param details human-readable summary of the change
   */
  public void recordAction(
      String actor, String action, String entityType, String entityId, String details) {
    AdminAuditEntry entry = new AdminAuditEntry(actor, action, entityType, entityId, details);
    entries.addFirst(entry);
    if (size.incrementAndGet() > MAX_ENTRIES) {
      entries.pollLast();
      size.decrementAndGet();
    }
    log.info("Admin audit: [{}] {} on {}:{} — {}", actor, action, entityType, entityId, details);
  }

  /**
   * Returns the most recent admin actions.
   *
   * @param limit maximum number of entries to return (clamped to [{@value #MAX_ENTRIES}])
   * @return list of entries, most recent first
   */
  public List<AdminAuditEntry> getRecentActions(int limit) {
    int effectiveLimit = Math.max(1, Math.min(limit, MAX_ENTRIES));
    return entries.stream().limit(effectiveLimit).toList();
  }

  /** Returns the current number of entries in the buffer. */
  public int getCurrentSize() {
    return size.get();
  }
}
