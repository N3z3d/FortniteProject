package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fortnite.pronos.dto.admin.AdminAuditEntryDto;
import com.fortnite.pronos.service.admin.AdminAuditEntry;
import com.fortnite.pronos.service.admin.AdminAuditLogService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuditController")
class AdminAuditControllerTest {

  @Mock private AdminAuditLogService auditLogService;

  private AdminAuditController controller;

  @BeforeEach
  void setUp() {
    controller = new AdminAuditController(auditLogService);
  }

  @Nested
  @DisplayName("GET /api/admin/audit-log")
  class GetAuditLog {

    @Test
    @DisplayName("returns 200 with mapped DTOs")
    void returns200WithDtos() {
      AdminAuditEntry entry =
          new AdminAuditEntry("alice", "CORRECT_METADATA", "PLAYER_IDENTITY", "id-1", "detail");
      when(auditLogService.getRecentActions(50)).thenReturn(List.of(entry));

      ResponseEntity<List<AdminAuditEntryDto>> response = controller.getAuditLog(50);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).hasSize(1);
      AdminAuditEntryDto dto = response.getBody().get(0);
      assertThat(dto.actor()).isEqualTo("alice");
      assertThat(dto.action()).isEqualTo("CORRECT_METADATA");
      assertThat(dto.entityType()).isEqualTo("PLAYER_IDENTITY");
      assertThat(dto.entityId()).isEqualTo("id-1");
      assertThat(dto.details()).isEqualTo("detail");
      assertThat(dto.id()).isNotNull();
      assertThat(dto.timestamp()).isInstanceOf(LocalDateTime.class);
    }

    @Test
    @DisplayName("returns empty list when no audit entries")
    void returnsEmptyList() {
      when(auditLogService.getRecentActions(50)).thenReturn(List.of());

      ResponseEntity<List<AdminAuditEntryDto>> response = controller.getAuditLog(50);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("passes limit to service")
    void passesLimitToService() {
      when(auditLogService.getRecentActions(10)).thenReturn(List.of());

      ResponseEntity<List<AdminAuditEntryDto>> response = controller.getAuditLog(10);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
  }
}
