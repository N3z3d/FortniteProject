package com.fortnite.pronos.adapter.out.persistence.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MappingUtils")
class MappingUtilsTest {

  @Nested
  @DisplayName("safeInt")
  class SafeInt {

    @Test
    @DisplayName("returns value when non-null")
    void returnsValueWhenNonNull() {
      assertThat(MappingUtils.safeInt(42, 0)).isEqualTo(42);
    }

    @Test
    @DisplayName("returns defaultValue when null")
    void returnsDefaultWhenNull() {
      assertThat(MappingUtils.safeInt(null, 5)).isEqualTo(5);
    }

    @Test
    @DisplayName("returns defaultValue when null, zero default")
    void returnsZeroDefaultWhenNull() {
      assertThat(MappingUtils.safeInt(null, 0)).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("safeBool")
  class SafeBool {

    @Test
    @DisplayName("returns false when null")
    void returnsFalseWhenNull() {
      assertThat(MappingUtils.safeBool(null)).isFalse();
    }

    @Test
    @DisplayName("returns true when Boolean.TRUE")
    void returnsTrueWhenTrue() {
      assertThat(MappingUtils.safeBool(Boolean.TRUE)).isTrue();
    }

    @Test
    @DisplayName("returns false when Boolean.FALSE")
    void returnsFalseWhenFalse() {
      assertThat(MappingUtils.safeBool(Boolean.FALSE)).isFalse();
    }
  }
}
