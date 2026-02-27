package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fortnite.pronos.dto.admin.DbTableInfoDto;
import com.fortnite.pronos.model.Game;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class AdminDatabaseServiceTest {

  @Mock private EntityManager entityManager;
  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private Metamodel metamodel;
  @Mock private EntityType entityType;
  @Mock private Query query;

  private AdminDatabaseService service;

  static class IdentityEntity {}

  @BeforeEach
  void setUp() {
    service = new AdminDatabaseService(entityManager, jdbcTemplate);
  }

  private void configureEntityType(String entityName, Class<?> javaType) {
    when(entityManager.getMetamodel()).thenReturn(metamodel);
    when(metamodel.getEntities()).thenReturn(Set.of(entityType));
    when(entityType.getName()).thenReturn(entityName);
    when(entityType.getJavaType()).thenReturn(javaType);
  }

  @Nested
  class GetTableInfo {

    @Test
    void shouldReturnTableInfoForEntityWithTableAnnotation() {
      configureEntityType("Game", Game.class);
      when(entityManager.createQuery(anyString())).thenReturn(query);
      when(query.getSingleResult()).thenReturn(10L);
      when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(8192L);

      List<DbTableInfoDto> result = service.getTableInfo();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTableName()).isEqualTo("games");
      assertThat(result.get(0).getEntityName()).isEqualTo("Game");
      assertThat(result.get(0).getRowCount()).isEqualTo(10L);
      assertThat(result.get(0).getSizeDescription()).isEqualTo("8 KB");
    }

    @Test
    void shouldReturnNAForSizeWhenJdbcFails() {
      configureEntityType("Game", Game.class);
      when(entityManager.createQuery(anyString())).thenReturn(query);
      when(query.getSingleResult()).thenReturn(5L);
      when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
          .thenThrow(new RuntimeException("pg_relation_size not supported"));

      List<DbTableInfoDto> result = service.getTableInfo();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getSizeDescription()).isEqualTo("N/A");
    }

    @Test
    void shouldReturnMinusOneRowCountWhenCountQueryFails() {
      configureEntityType("Game", Game.class);
      when(entityManager.createQuery(anyString())).thenReturn(query);
      when(query.getSingleResult()).thenThrow(new RuntimeException("JPQL error"));
      when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(0L);

      List<DbTableInfoDto> result = service.getTableInfo();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getRowCount()).isEqualTo(-1L);
    }

    @Test
    void shouldFallbackToSimpleNameWhenNoTableAnnotation() {
      configureEntityType("SomeEntity", String.class);
      when(entityManager.createQuery(anyString())).thenReturn(query);
      when(query.getSingleResult()).thenReturn(0L);
      when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(0L);

      List<DbTableInfoDto> result = service.getTableInfo();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTableName()).isEqualTo("string");
    }

    @Test
    void shouldSortByRowCountDescending() {
      EntityType entityType2 = mock(EntityType.class);
      Query query2 = mock(Query.class);

      when(entityManager.getMetamodel()).thenReturn(metamodel);
      when(metamodel.getEntities()).thenReturn(Set.of(entityType, entityType2));
      when(entityType.getName()).thenReturn("Game");
      when(entityType.getJavaType()).thenReturn(Game.class);
      when(entityType2.getName()).thenReturn("Other");
      when(entityType2.getJavaType()).thenReturn(String.class);

      when(entityManager.createQuery("SELECT COUNT(e) FROM Game e")).thenReturn(query);
      when(entityManager.createQuery("SELECT COUNT(e) FROM Other e")).thenReturn(query2);
      when(query.getSingleResult()).thenReturn(3L);
      when(query2.getSingleResult()).thenReturn(100L);
      when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(0L);

      List<DbTableInfoDto> result = service.getTableInfo();

      assertThat(result).hasSize(2);
      assertThat(result.get(0).getRowCount()).isEqualTo(100L);
      assertThat(result.get(1).getRowCount()).isEqualTo(3L);
    }

    @Test
    void shouldReturnEmptyListWhenNoEntities() {
      when(entityManager.getMetamodel()).thenReturn(metamodel);
      when(metamodel.getEntities()).thenReturn(Set.of());

      List<DbTableInfoDto> result = service.getTableInfo();

      assertThat(result).isEmpty();
    }

    @Test
    void shouldUseLocaleRootForFallbackTableName() {
      Locale initialLocale = Locale.getDefault();
      try {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        configureEntityType("IdentityEntity", IdentityEntity.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(0L);

        List<DbTableInfoDto> result = service.getTableInfo();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTableName()).isEqualTo("identityentity");
      } finally {
        Locale.setDefault(initialLocale);
      }
    }

    @Test
    void shouldReturnUnmodifiableResultList() {
      configureEntityType("Game", Game.class);
      when(entityManager.createQuery(anyString())).thenReturn(query);
      when(query.getSingleResult()).thenReturn(10L);
      when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(0L);

      List<DbTableInfoDto> result = service.getTableInfo();

      assertThatThrownBy(() -> result.add(DbTableInfoDto.builder().tableName("x").build()))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
