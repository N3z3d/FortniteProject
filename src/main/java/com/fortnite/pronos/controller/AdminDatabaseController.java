package com.fortnite.pronos.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fortnite.pronos.dto.admin.DbTableInfoDto;
import com.fortnite.pronos.dto.admin.SqlQueryRequest;
import com.fortnite.pronos.dto.admin.SqlQueryResultDto;
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

  @PostMapping("/database/query")
  public ResponseEntity<ApiResponse<SqlQueryResultDto>> executeQuery(
      @Valid @RequestBody SqlQueryRequest request) {
    log.info("Admin: executing read-only SQL query");
    SqlQueryResultDto result = adminDatabaseService.executeReadOnlyQuery(request.query());
    return ResponseEntity.ok(ApiResponse.success(result));
  }
}
