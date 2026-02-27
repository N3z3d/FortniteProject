package com.fortnite.pronos.controller;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.PipelineCountResponse;
import com.fortnite.pronos.dto.admin.PlayerIdentityEntryResponse;
import com.fortnite.pronos.dto.admin.RejectPlayerRequest;
import com.fortnite.pronos.dto.admin.ResolvePlayerRequest;
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

  @PostMapping("/resolve")
  public ResponseEntity<PlayerIdentityEntryResponse> resolve(
      @Valid @RequestBody ResolvePlayerRequest request, Principal principal) {
    String resolvedBy = principal != null ? principal.getName() : "admin";
    return ResponseEntity.ok(
        pipelineService.resolve(request.playerId(), request.epicId(), resolvedBy));
  }

  @PostMapping("/reject")
  public ResponseEntity<PlayerIdentityEntryResponse> reject(
      @Valid @RequestBody RejectPlayerRequest request, Principal principal) {
    String rejectedBy = principal != null ? principal.getName() : "admin";
    return ResponseEntity.ok(
        pipelineService.reject(request.playerId(), request.reason(), rejectedBy));
  }
}
