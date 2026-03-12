package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.admin.DbTableInfoDto;
import com.fortnite.pronos.dto.admin.SqlQueryRequest;
import com.fortnite.pronos.dto.admin.SqlQueryResultDto;
import com.fortnite.pronos.service.admin.AdminDatabaseService;

@ExtendWith(MockitoExtension.class)
class AdminDatabaseControllerTest {

  @Mock private AdminDatabaseService adminDatabaseService;

  private AdminDatabaseController controller;

  @BeforeEach
  void setUp() {
    controller = new AdminDatabaseController(adminDatabaseService);
  }

  @Nested
  @DisplayName("Get Database Tables")
  class GetDatabaseTables {

    @Test
    void shouldReturnTableList() {
      var tableInfo =
          DbTableInfoDto.builder()
              .tableName("games")
              .entityName("Game")
              .rowCount(42L)
              .sizeDescription("8 KB")
              .build();
      when(adminDatabaseService.getTableInfo()).thenReturn(List.of(tableInfo));

      var response = controller.getDatabaseTables();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getData()).hasSize(1);
      assertThat(response.getBody().getData().get(0).getTableName()).isEqualTo("games");
      assertThat(response.getBody().getData().get(0).getRowCount()).isEqualTo(42L);
      assertThat(response.getBody().getData().get(0).getSizeDescription()).isEqualTo("8 KB");
    }

    @Test
    void shouldReturnEmptyListWhenNoTables() {
      when(adminDatabaseService.getTableInfo()).thenReturn(List.of());

      var response = controller.getDatabaseTables();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    void shouldReturnMultipleTables() {
      var table1 =
          DbTableInfoDto.builder()
              .tableName("games")
              .entityName("Game")
              .rowCount(100L)
              .sizeDescription("16 KB")
              .build();
      var table2 =
          DbTableInfoDto.builder()
              .tableName("users")
              .entityName("User")
              .rowCount(50L)
              .sizeDescription("8 KB")
              .build();
      when(adminDatabaseService.getTableInfo()).thenReturn(List.of(table1, table2));

      var response = controller.getDatabaseTables();

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData()).hasSize(2);
      assertThat(response.getBody().getData().get(0).getEntityName()).isEqualTo("Game");
      assertThat(response.getBody().getData().get(1).getEntityName()).isEqualTo("User");
    }
  }

  @Nested
  @DisplayName("Execute SQL Query")
  class ExecuteQuery {

    @Test
    void shouldReturn200WithQueryResult() {
      var result =
          new SqlQueryResultDto(
              List.of("id", "name"), List.of(Map.of("id", 1, "name", "Alice")), 1, false);
      when(adminDatabaseService.executeReadOnlyQuery("SELECT id, name FROM users"))
          .thenReturn(result);

      var response = controller.executeQuery(new SqlQueryRequest("SELECT id, name FROM users"));

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getData().columns()).containsExactly("id", "name");
      assertThat(response.getBody().getData().rows()).hasSize(1);
      assertThat(response.getBody().getData().truncated()).isFalse();
    }

    @Test
    void shouldPropagateExceptionFromServiceForForbiddenQuery() {
      when(adminDatabaseService.executeReadOnlyQuery(anyString()))
          .thenThrow(new IllegalArgumentException("Only SELECT queries are allowed"));

      assertThatThrownBy(() -> controller.executeQuery(new SqlQueryRequest("DELETE FROM users")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Only SELECT queries are allowed");
    }

    @Test
    void shouldReturnTruncatedResultWhenOver100Rows() {
      var result = new SqlQueryResultDto(List.of("id"), List.of(), 100, true);
      when(adminDatabaseService.executeReadOnlyQuery(anyString())).thenReturn(result);

      var response = controller.executeQuery(new SqlQueryRequest("SELECT id FROM big_table"));

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody().getData().truncated()).isTrue();
    }
  }
}
