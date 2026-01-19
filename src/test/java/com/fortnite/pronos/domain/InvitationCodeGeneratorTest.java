package com.fortnite.pronos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for InvitationCodeGenerator. Pure domain tests - no Spring context required. */
@DisplayName("InvitationCodeGenerator")
class InvitationCodeGeneratorTest {

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("creates generator with default settings")
    void createsWithDefaults() {
      InvitationCodeGenerator generator = new InvitationCodeGenerator();
      String code = generator.generate();

      assertThat(code).hasSize(8);
    }

    @Test
    @DisplayName("creates generator with custom length")
    void createsWithCustomLength() {
      InvitationCodeGenerator generator = new InvitationCodeGenerator(new Random(42), 12);
      String code = generator.generate();

      assertThat(code).hasSize(12);
    }

    @Test
    @DisplayName("rejects code length below 4")
    void rejectsLengthBelow4() {
      assertThatThrownBy(() -> new InvitationCodeGenerator(new Random(), 3))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("between 4 and 20");
    }

    @Test
    @DisplayName("rejects code length above 20")
    void rejectsLengthAbove20() {
      assertThatThrownBy(() -> new InvitationCodeGenerator(new Random(), 21))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("between 4 and 20");
    }
  }

  @Nested
  @DisplayName("generate")
  class Generate {

    @Test
    @DisplayName("generates alphanumeric code")
    void generatesAlphanumericCode() {
      InvitationCodeGenerator generator = new InvitationCodeGenerator();
      String code = generator.generate();

      assertThat(code).matches("[A-Z0-9]+");
    }

    @Test
    @DisplayName("generates deterministic code with seeded random")
    void generatesDeterministicWithSeed() {
      InvitationCodeGenerator generator1 = new InvitationCodeGenerator(new Random(42), 8);
      InvitationCodeGenerator generator2 = new InvitationCodeGenerator(new Random(42), 8);

      assertThat(generator1.generate()).isEqualTo(generator2.generate());
    }

    @Test
    @DisplayName("generates different codes on subsequent calls")
    void generatesDifferentCodes() {
      InvitationCodeGenerator generator = new InvitationCodeGenerator();
      String code1 = generator.generate();
      String code2 = generator.generate();

      assertThat(code1).isNotEqualTo(code2);
    }
  }

  @Nested
  @DisplayName("isValidFormat")
  class IsValidFormat {

    @Test
    @DisplayName("accepts valid 8-character code")
    void acceptsValid8CharCode() {
      assertThat(InvitationCodeGenerator.isValidFormat("ABCD1234")).isTrue();
    }

    @Test
    @DisplayName("accepts minimum 4-character code")
    void acceptsMin4CharCode() {
      assertThat(InvitationCodeGenerator.isValidFormat("AB12")).isTrue();
    }

    @Test
    @DisplayName("accepts maximum 20-character code")
    void acceptsMax20CharCode() {
      assertThat(InvitationCodeGenerator.isValidFormat("ABCDEFGHIJ1234567890")).isTrue();
    }

    @Test
    @DisplayName("rejects null code")
    void rejectsNull() {
      assertThat(InvitationCodeGenerator.isValidFormat(null)).isFalse();
    }

    @Test
    @DisplayName("rejects code shorter than 4 characters")
    void rejectsTooShort() {
      assertThat(InvitationCodeGenerator.isValidFormat("ABC")).isFalse();
    }

    @Test
    @DisplayName("rejects code longer than 20 characters")
    void rejectsTooLong() {
      assertThat(InvitationCodeGenerator.isValidFormat("ABCDEFGHIJ12345678901")).isFalse();
    }

    @Test
    @DisplayName("rejects code with lowercase letters")
    void rejectsLowercase() {
      assertThat(InvitationCodeGenerator.isValidFormat("abcd1234")).isFalse();
    }

    @Test
    @DisplayName("rejects code with special characters")
    void rejectsSpecialChars() {
      assertThat(InvitationCodeGenerator.isValidFormat("ABCD-123")).isFalse();
    }
  }
}
