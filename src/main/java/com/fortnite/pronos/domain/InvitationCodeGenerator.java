package com.fortnite.pronos.domain;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Domain service for generating game invitation codes. Pure domain logic without persistence
 * dependencies.
 */
public final class InvitationCodeGenerator {

  private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int DEFAULT_CODE_LENGTH = 8;

  private final Random random;
  private final int codeLength;

  /** Creates generator with default settings (secure random, 8-char codes). */
  public InvitationCodeGenerator() {
    this(new SecureRandom(), DEFAULT_CODE_LENGTH);
  }

  /**
   * Creates generator with custom random source (useful for testing).
   *
   * @param random random number generator
   * @param codeLength length of generated codes
   */
  public InvitationCodeGenerator(Random random, int codeLength) {
    if (codeLength < 4 || codeLength > 20) {
      throw new IllegalArgumentException("Code length must be between 4 and 20");
    }
    this.random = random;
    this.codeLength = codeLength;
  }

  /**
   * Generates a random alphanumeric invitation code.
   *
   * @return generated code
   */
  public String generate() {
    StringBuilder code = new StringBuilder(codeLength);
    for (int i = 0; i < codeLength; i++) {
      code.append(ALPHANUMERIC_CHARS.charAt(random.nextInt(ALPHANUMERIC_CHARS.length())));
    }
    return code.toString();
  }

  /**
   * Validates an invitation code format.
   *
   * @param code code to validate
   * @return true if valid format
   */
  public static boolean isValidFormat(String code) {
    if (code == null || code.length() < 4 || code.length() > 20) {
      return false;
    }
    return code.chars().allMatch(c -> ALPHANUMERIC_CHARS.indexOf(c) >= 0);
  }
}
