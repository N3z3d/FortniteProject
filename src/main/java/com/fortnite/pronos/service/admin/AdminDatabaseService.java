package com.fortnite.pronos.service.admin;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.admin.DbTableInfoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDatabaseService {

  private static final long BYTES_IN_KILOBYTE = 1024L;
  private static final long BYTES_IN_MEGABYTE = BYTES_IN_KILOBYTE * BYTES_IN_KILOBYTE;
  private static final long BYTES_IN_GIGABYTE = BYTES_IN_MEGABYTE * BYTES_IN_KILOBYTE;

  private final EntityManager entityManager;
  private final JdbcTemplate jdbcTemplate;

  @Transactional(readOnly = true)
  public List<DbTableInfoDto> getTableInfo() {
    return entityManager.getMetamodel().getEntities().stream()
        .map(this::toTableInfo)
        .sorted(Comparator.comparingLong(DbTableInfoDto::getRowCount).reversed())
        .toList();
  }

  private DbTableInfoDto toTableInfo(EntityType<?> entity) {
    String entityName = entity.getName();
    String tableName = resolveTableName(entity.getJavaType());
    long rowCount = countRows(entityName);
    String sizeDescription = resolveTableSize(tableName);
    return DbTableInfoDto.builder()
        .tableName(tableName)
        .entityName(entityName)
        .rowCount(rowCount)
        .sizeDescription(sizeDescription)
        .build();
  }

  private long countRows(String entityName) {
    try {
      Object result =
          entityManager.createQuery("SELECT COUNT(e) FROM " + entityName + " e").getSingleResult();
      return result instanceof Long l ? l : 0L;
    } catch (Exception e) {
      log.warn("Could not count rows for entity {}: {}", entityName, e.getMessage());
      return -1L;
    }
  }

  private String resolveTableName(Class<?> javaType) {
    jakarta.persistence.Table tableAnnotation =
        javaType.getAnnotation(jakarta.persistence.Table.class);
    if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
      return tableAnnotation.name();
    }
    return javaType.getSimpleName().toLowerCase(Locale.ROOT);
  }

  private String resolveTableSize(String tableName) {
    try {
      Long sizeBytes =
          jdbcTemplate.queryForObject("SELECT pg_relation_size(?)", Long.class, tableName);
      return sizeBytes != null ? formatBytes(sizeBytes) : "N/A";
    } catch (Exception e) {
      log.debug(
          "Could not resolve size for table {} (non-PostgreSQL?): {}", tableName, e.getMessage());
      return "N/A";
    }
  }

  private String formatBytes(long bytes) {
    long value = bytes;
    String unit = "B";
    if (bytes >= BYTES_IN_GIGABYTE) {
      value = bytes / BYTES_IN_GIGABYTE;
      unit = "GB";
    } else if (bytes >= BYTES_IN_MEGABYTE) {
      value = bytes / BYTES_IN_MEGABYTE;
      unit = "MB";
    } else if (bytes >= BYTES_IN_KILOBYTE) {
      value = bytes / BYTES_IN_KILOBYTE;
      unit = "KB";
    }
    return value + " " + unit;
  }
}
