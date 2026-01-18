package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationService - Email Validation TDD Tests")
class ValidationServiceEmailTddTest {

  @InjectMocks private ValidationService validationService;

  private String validEmail;
  private String invalidEmail;

  @BeforeEach
  void setUp() {
    validEmail = "champion@fortnite.com";
    invalidEmail = "invalid-email";
  }

  @Test
  @DisplayName("Should validate correct email format")
  void shouldValidateCorrectEmailFormat() {
    boolean result = validationService.isValidEmail(validEmail);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should reject invalid email format")
  void shouldRejectInvalidEmailFormat() {
    boolean result = validationService.isValidEmail(invalidEmail);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should reject null email")
  void shouldRejectNullEmail() {
    boolean result = validationService.isValidEmail(null);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should validate various email formats")
  void shouldValidateVariousEmailFormats() {
    String[] validEmails = {
      "user@domain.com",
      "test.email@example.org",
      "long.email.name@subdomain.domain.co.uk",
      "user+tag@domain.com"
    };

    for (String email : validEmails) {
      boolean result = validationService.isValidEmail(email);
      assertThat(result).as("Email %s should be valid", email).isTrue();
    }
  }

  @Test
  @DisplayName("Should reject malformed emails")
  void shouldRejectMalformedEmails() {
    String[] invalidEmails = {
      "invalid", "invalid@", "@domain.com", "invalid.domain", "user@@domain.com", "user@.com", ""
    };

    for (String email : invalidEmails) {
      boolean result = validationService.isValidEmail(email);
      assertThat(result).as("Email %s should be invalid", email).isFalse();
    }
  }

  @Test
  @DisplayName("Should handle edge case email formats")
  void shouldHandleEdgeCaseEmailFormats() {
    String minimalValidEmail = "a@b.c";
    String longValidEmail = "very.long.email.address@very.long.domain.name.example.com";

    boolean minimalResult = validationService.isValidEmail(minimalValidEmail);
    boolean longResult = validationService.isValidEmail(longValidEmail);

    assertThat(minimalResult).isTrue();
    assertThat(longResult).isTrue();
  }
}
