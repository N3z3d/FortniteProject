package com.fortnite.pronos.adapter.out.persistence.support;

/** Shared null-safe mapping utilities for JPA adapter mappers. */
public final class MappingUtils {

  private MappingUtils() {}

  /**
   * Returns {@code value} if non-null, otherwise {@code defaultValue}.
   *
   * @param value the nullable Integer
   * @param defaultValue fallback primitive int
   * @return safe int value
   */
  public static int safeInt(Integer value, int defaultValue) {
    return value != null ? value : defaultValue;
  }

  /**
   * Returns {@code true} only if {@code value} is non-null and {@code true}.
   *
   * @param value the nullable Boolean
   * @return safe boolean value
   */
  public static boolean safeBool(Boolean value) {
    return value != null && value;
  }
}
