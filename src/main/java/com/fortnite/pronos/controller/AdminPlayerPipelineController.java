package com.fortnite.pronos.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.CorrectMetadataRequest;
import com.fortnite.pronos.dto.admin.PipelineCountResponse;
import com.fortnite.pronos.dto.admin.PipelineRegionalStatsDto;
import com.fortnite.pronos.dto.admin.PlayerIdentityEntryResponse;
import com.fortnite.pronos.dto.admin.RejectPlayerRequest;
import com.fortnite.pronos.dto.admin.ResolvePlayerRequest;
import com.fortnite.pronos.service.admin.AdminAuditLogService;
import com.fortnite.pronos.service.admin.PlayerIdentityPipelineService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/admin/players")
@RequiredArgsConstructor
public class AdminPlayerPipelineController {

  private final PlayerIdentityPipelineService pipelineService;
  private final AdminAuditLogService auditLogService;

  @GetMapping("/unresolved")
  public ResponseEntity<List<PlayerIdentityEntryResponse>> getUnresolved(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(pipelineService.getUnresolved(page, size));
  }

  @GetMapping("/resolved")
  public ResponseEntity<List<PlayerIdentityEntryResponse>> getResolved(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(pipelineService.getResolved(page, size));
  }

  @GetMapping("/pipeline/count")
  public ResponseEntity<PipelineCountResponse> getCount() {
    return ResponseEntity.ok(pipelineService.getCount());
  }

  @GetMapping("/pipeline/regional-status")
  public ResponseEntity<List<PipelineRegionalStatsDto>> getRegionalStatus() {
    return ResponseEntity.ok(pipelineService.getRegionalStats());
  }

  @PostMapping("/resolve")
  public ResponseEntity<PlayerIdentityEntryResponse> resolve(
      @Valid @RequestBody ResolvePlayerRequest request, Principal principal) {
    String resolvedBy = principal != null ? principal.getName() : "admin";
    PlayerIdentityEntryResponse result =
        pipelineService.resolve(request.playerId(), request.epicId(), resolvedBy);
    auditLogService.recordAction(
        resolvedBy,
        "RESOLVE_PLAYER",
        "PLAYER_IDENTITY",
        request.playerId().toString(),
        "epicId=" + request.epicId());
    return ResponseEntity.ok(result);
  }

  @PostMapping("/reject")
  public ResponseEntity<PlayerIdentityEntryResponse> reject(
      @Valid @RequestBody RejectPlayerRequest request, Principal principal) {
    String rejectedBy = principal != null ? principal.getName() : "admin";
    PlayerIdentityEntryResponse result =
        pipelineService.reject(request.playerId(), request.reason(), rejectedBy);
    auditLogService.recordAction(
        rejectedBy,
        "REJECT_PLAYER",
        "PLAYER_IDENTITY",
        request.playerId().toString(),
        "reason=" + request.reason());
    return ResponseEntity.ok(result);
  }

  @PatchMapping("/{playerId}/metadata")
  public ResponseEntity<PlayerIdentityEntryResponse> correctMetadata(
      @PathVariable UUID playerId,
      @Valid @RequestBody CorrectMetadataRequest request,
      Principal principal) {
    String correctedBy = principal != null ? principal.getName() : "admin";
    PlayerIdentityEntryResponse result =
        pipelineService.correctMetadata(
            playerId, request.newUsername(), request.newRegion(), correctedBy);
    auditLogService.recordAction(
        correctedBy,
        "CORRECT_METADATA",
        "PLAYER_IDENTITY",
        playerId.toString(),
        "username=" + request.newUsername() + ", region=" + request.newRegion());
    return ResponseEntity.ok(result);
  }
}
