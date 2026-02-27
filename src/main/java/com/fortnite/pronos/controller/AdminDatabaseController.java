package com.fortnite.pronos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.DbTableInfoDto;
import com.fortnite.pronos.dto.common.ApiResponse;
import com.fortnite.pronos.service.admin.AdminDatabaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDatabaseController {

  private final AdminDatabaseService adminDatabaseService;

  @GetMapping("/database/tables")
  public ResponseEntity<ApiResponse<List<DbTableInfoDto>>> getDatabaseTables() {
    log.info("Admin: fetching database table info");
    return ResponseEntity.ok(ApiResponse.success(adminDatabaseService.getTableInfo()));
  }
}
