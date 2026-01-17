package com.fortnite.pronos.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import jakarta.persistence.Column;

import org.hibernate.annotations.ColumnTransformer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JPA enum mapping")
class EnumMappingTest {

  @Test
  @DisplayName("Player.region should use region_enum")
  void playerRegionUsesNamedEnum() throws NoSuchFieldException {
    Field field = Player.class.getDeclaredField("region");
    assertEnumColumn(field, "region_enum");
  }

  @Test
  @DisplayName("User.role should use user_role")
  void userRoleUsesNamedEnum() throws NoSuchFieldException {
    Field field = User.class.getDeclaredField("role");
    assertEnumColumn(field, "user_role");
  }

  @Test
  @DisplayName("PrSnapshot.region should use pr_region")
  void prSnapshotRegionUsesNamedEnum() throws NoSuchFieldException {
    Field field = PrSnapshot.class.getDeclaredField("region");
    assertEnumColumn(field, "pr_region");
  }

  private void assertEnumColumn(Field field, String expectedColumnDefinition) {
    ColumnTransformer transformer = field.getAnnotation(ColumnTransformer.class);
    Column column = field.getAnnotation(Column.class);

    assertThat(transformer).as("ColumnTransformer is missing on %s", field.getName()).isNotNull();
    assertThat(transformer.write()).isEqualTo("CAST(? AS " + expectedColumnDefinition + ")");
    assertThat(column).as("Column is missing on %s", field.getName()).isNotNull();
    assertThat(column.columnDefinition()).isEqualTo(expectedColumnDefinition);
  }
}
