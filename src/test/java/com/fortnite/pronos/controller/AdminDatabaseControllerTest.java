package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.admin.DbTableInfoDto;
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
}
