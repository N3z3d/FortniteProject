package com.fortnite.pronos.controller;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.AdminAuditEntryDto;
import com.fortnite.pronos.service.admin.AdminAuditEntry;
import com.fortnite.pronos.service.admin.AdminAuditLogService;

import lombok.RequiredArgsConstructor;

/** Exposes the in-memory admin audit trail to the admin panel. */
@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
@Validated
public class AdminAuditController {

  private final AdminAuditLogService auditLogService;

  @GetMapping
  public ResponseEntity<List<AdminAuditEntryDto>> getAuditLog(
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
    List<AdminAuditEntryDto> dtos =
        auditLogService.getRecentActions(limit).stream().map(this::toDto).toList();
    return ResponseEntity.ok(dtos);
  }

  private AdminAuditEntryDto toDto(AdminAuditEntry entry) {
    return new AdminAuditEntryDto(
        entry.getId(),
        entry.getActor(),
        entry.getAction(),
        entry.getEntityType(),
        entry.getEntityId(),
        entry.getDetails(),
        entry.getTimestamp());
  }
}
