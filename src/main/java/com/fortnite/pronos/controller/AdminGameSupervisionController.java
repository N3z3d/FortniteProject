package com.fortnite.pronos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.GameSupervisionDto;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.service.admin.AdminGameSupervisionService;

@RestController
@RequestMapping("/api/admin/supervision")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGameSupervisionController {

  private final AdminGameSupervisionService supervisionService;

  public AdminGameSupervisionController(AdminGameSupervisionService supervisionService) {
    this.supervisionService = supervisionService;
  }

  @GetMapping("/games")
  public ResponseEntity<List<GameSupervisionDto>> getGames(
      @RequestParam(required = false) GameStatus status) {
    List<GameSupervisionDto> games =
        status == null
            ? supervisionService.getAllActiveGames()
            : supervisionService.getActiveGamesByStatus(status);
    return ResponseEntity.ok(games);
  }
}
