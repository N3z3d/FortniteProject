package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AdminAuditLogService")
class AdminAuditLogServiceTest {

  private AdminAuditLogService service;

  @BeforeEach
  void setUp() {
    service = new AdminAuditLogService();
  }

  @Nested
  @DisplayName("recordAction")
  class RecordAction {

    @Test
    @DisplayName("stores entry with all fields set")
    void storesEntryWithAllFields() {
      service.recordAction("alice", "CORRECT_METADATA", "PLAYER_IDENTITY", "pid-1", "username=Foo");

      List<AdminAuditEntry> entries = service.getRecentActions(10);
      assertThat(entries).hasSize(1);
      AdminAuditEntry entry = entries.get(0);
      assertThat(entry.getActor()).isEqualTo("alice");
      assertThat(entry.getAction()).isEqualTo("CORRECT_METADATA");
      assertThat(entry.getEntityType()).isEqualTo("PLAYER_IDENTITY");
      assertThat(entry.getEntityId()).isEqualTo("pid-1");
      assertThat(entry.getDetails()).isEqualTo("username=Foo");
      assertThat(entry.getId()).isNotNull();
      assertThat(entry.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("inserts most recent entry first")
    void insertsMostRecentFirst() {
      service.recordAction("a", "ACTION_1", "E", null, null);
      service.recordAction("b", "ACTION_2", "E", null, null);

      List<AdminAuditEntry> entries = service.getRecentActions(10);
      assertThat(entries.get(0).getActor()).isEqualTo("b");
      assertThat(entries.get(1).getActor()).isEqualTo("a");
    }

    @Test
    @DisplayName("evicts oldest entry when buffer exceeds MAX_ENTRIES")
    void evictsOldestWhenFull() {
      for (int i = 0; i < AdminAuditLogService.MAX_ENTRIES; i++) {
        service.recordAction("admin", "ACT", "T", String.valueOf(i), null);
      }
      assertThat(service.getCurrentSize()).isEqualTo(AdminAuditLogService.MAX_ENTRIES);

      service.recordAction("admin", "ACT_NEW", "T", "new", null);

      assertThat(service.getCurrentSize()).isEqualTo(AdminAuditLogService.MAX_ENTRIES);
      List<AdminAuditEntry> entries = service.getRecentActions(AdminAuditLogService.MAX_ENTRIES);
      assertThat(entries.get(0).getAction()).isEqualTo("ACT_NEW");
      // Oldest entry (entityId=0) should have been evicted
      assertThat(entries.stream().map(AdminAuditEntry::getEntityId)).doesNotContain("0");
    }

    @Test
    @DisplayName("increments size counter on each record")
    void incrementsSize() {
      assertThat(service.getCurrentSize()).isZero();
      service.recordAction("a", "A", "T", null, null);
      assertThat(service.getCurrentSize()).isEqualTo(1);
      service.recordAction("b", "B", "T", null, null);
      assertThat(service.getCurrentSize()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("getRecentActions")
  class GetRecentActions {

    @Test
    @DisplayName("returns empty list when no entries")
    void returnsEmptyWhenNoEntries() {
      assertThat(service.getRecentActions(10)).isEmpty();
    }

    @Test
    @DisplayName("clamps limit below 1 to 1")
    void clampsLimitBelow1() {
      service.recordAction("a", "A", "T", null, null);
      service.recordAction("b", "B", "T", null, null);

      List<AdminAuditEntry> result = service.getRecentActions(0);
      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("clamps limit above MAX_ENTRIES to MAX_ENTRIES")
    void clampsLimitAboveMax() {
      for (int i = 0; i < 5; i++) {
        service.recordAction("a", "A", "T", null, null);
      }
      List<AdminAuditEntry> result = service.getRecentActions(AdminAuditLogService.MAX_ENTRIES + 1);
      assertThat(result).hasSize(5);
    }

    @Test
    @DisplayName("each entry has unique UUID id")
    void eachEntryHasUniqueId() {
      service.recordAction("a", "A", "T", null, null);
      service.recordAction("b", "B", "T", null, null);

      List<UUID> ids = service.getRecentActions(10).stream().map(AdminAuditEntry::getId).toList();
      assertThat(ids).doesNotHaveDuplicates();
    }
  }
}
