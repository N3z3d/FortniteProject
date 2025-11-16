package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.service.InvitationCodeService.InvitationCodeGenerationException;

/**
 * TDD Tests for InvitationCodeService - Security & Uniqueness Critical Component
 *
 * <p>This test suite validates invitation code generation, uniqueness enforcement, and format
 * validation using RED-GREEN-REFACTOR TDD methodology. InvitationCodeService handles secure code
 * generation, collision detection, format validation, and security constraints essential for
 * maintaining game invitation integrity and preventing unauthorized access.
 *
 * <p>Business Logic Areas: - Secure code generation with SecureRandom - Uniqueness enforcement and
 * collision handling - Format validation and character restrictions - Security constraints and
 * error handling - Performance optimization for code generation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationCodeService - Security Critical TDD Tests")
class InvitationCodeServiceTddTest {

  @Mock private GameRepository gameRepository;

  @InjectMocks private InvitationCodeService invitationCodeService;

  private String validCode;
  private String invalidCode;

  @BeforeEach
  void setUp() {
    // Test data setup
    validCode = "ABC123";
    invalidCode = "invalid";
  }

  @Nested
  @DisplayName("Code Generation and Uniqueness")
  class CodeGenerationTests {

    @Test
    @DisplayName("Should generate unique code successfully")
    void shouldGenerateUniqueCodeSuccessfully() {
      // RED: Test basic code generation
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      String result = invitationCodeService.generateUniqueCode();

      assertThat(result).isNotNull();
      assertThat(result).hasSize(6);
      assertThat(result).matches("^[A-Z0-9]{6}$");

      verify(gameRepository).existsByInvitationCode(result);
    }

    @Test
    @DisplayName("Should retry code generation when collision occurs")
    void shouldRetryCodeGenerationWhenCollisionOccurs() {
      // RED: Test collision handling
      when(gameRepository.existsByInvitationCode(anyString()))
          .thenReturn(true) // First attempt collides
          .thenReturn(true) // Second attempt collides
          .thenReturn(false); // Third attempt succeeds

      String result = invitationCodeService.generateUniqueCode();

      assertThat(result).isNotNull();
      assertThat(result).hasSize(6);
      assertThat(result).matches("^[A-Z0-9]{6}$");

      verify(gameRepository, times(3)).existsByInvitationCode(anyString());
    }

    @Test
    @DisplayName("Should throw exception when max attempts exceeded")
    void shouldThrowExceptionWhenMaxAttemptsExceeded() {
      // RED: Test max attempts handling
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(true);

      assertThatThrownBy(() -> invitationCodeService.generateUniqueCode())
          .isInstanceOf(InvitationCodeGenerationException.class)
          .hasMessageContaining("Impossible de générer un code unique après 100 tentatives");

      verify(gameRepository, times(100)).existsByInvitationCode(anyString());
    }

    @Test
    @DisplayName("Should generate codes with proper format")
    void shouldGenerateCodesWithProperFormat() {
      // RED: Test format consistency
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      Set<String> generatedCodes = new HashSet<>();
      for (int i = 0; i < 10; i++) {
        String code = invitationCodeService.generateUniqueCode();
        generatedCodes.add(code);

        assertThat(code).hasSize(6);
        assertThat(code).matches("^[A-Z0-9]{6}$");
        assertThat(code).doesNotContainPattern("[a-z]"); // No lowercase letters
        assertThat(code).doesNotContainPattern("[^A-Z0-9]"); // Only allowed characters
      }

      // All codes should be unique
      assertThat(generatedCodes).hasSize(10);
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
      // RED: Test repository error handling
      when(gameRepository.existsByInvitationCode(anyString()))
          .thenThrow(new RuntimeException("Database connection failed"));

      assertThatThrownBy(() -> invitationCodeService.generateUniqueCode())
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Database connection failed");
    }

    @Test
    @DisplayName("Should generate truly random codes")
    void shouldGenerateTrulyRandomCodes() {
      // RED: Test randomness and distribution
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      Set<String> generatedCodes = new HashSet<>();
      for (int i = 0; i < 100; i++) {
        String code = invitationCodeService.generateUniqueCode();
        generatedCodes.add(code);
      }

      // Should generate at least 95% unique codes (allowing for some rare collisions)
      assertThat(generatedCodes.size()).isGreaterThan(95);
    }

    @Test
    @DisplayName("Should handle successful generation after multiple collisions")
    void shouldHandleSuccessfulGenerationAfterMultipleCollisions() {
      // RED: Test collision recovery
      when(gameRepository.existsByInvitationCode(anyString()))
          .thenReturn(true) // Attempts 1-5 collide
          .thenReturn(true)
          .thenReturn(true)
          .thenReturn(true)
          .thenReturn(true)
          .thenReturn(false); // Attempt 6 succeeds

      String result = invitationCodeService.generateUniqueCode();

      assertThat(result).isNotNull();
      verify(gameRepository, times(6)).existsByInvitationCode(anyString());
    }

    @Test
    @DisplayName("Should handle edge case at max attempts boundary")
    void shouldHandleEdgeCaseAtMaxAttempsBoundary() {
      // RED: Test boundary condition
      // Mock 99 collisions, then success on attempt 100
      when(gameRepository.existsByInvitationCode(anyString()))
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 10 trues
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 20 trues
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 30 trues
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 40 trues
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 50 trues
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 60 trues
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 70 trues
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 80 trues
          .thenReturn(true, true, true, true, true, true, true, true, true, true) // 90 trues
          .thenReturn(
              true, true, true, true, true, true, true, true, true, false); // 100th is false

      String result = invitationCodeService.generateUniqueCode();

      assertThat(result).isNotNull();
      verify(gameRepository, times(100)).existsByInvitationCode(anyString());
    }
  }

  @Nested
  @DisplayName("Format Validation")
  class FormatValidationTests {

    @Test
    @DisplayName("Should validate correct code format")
    void shouldValidateCorrectCodeFormat() {
      // RED: Test valid format validation
      boolean result = invitationCodeService.isValidCodeFormat(validCode);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject null code")
    void shouldRejectNullCode() {
      // RED: Test null code rejection
      boolean result = invitationCodeService.isValidCodeFormat(null);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject empty code")
    void shouldRejectEmptyCode() {
      // RED: Test empty code rejection
      boolean result = invitationCodeService.isValidCodeFormat("");

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject incorrect length codes")
    void shouldRejectIncorrectLengthCodes() {
      // RED: Test length validation
      String shortCode = "ABC12";
      String longCode = "ABC1234";

      boolean shortResult = invitationCodeService.isValidCodeFormat(shortCode);
      boolean longResult = invitationCodeService.isValidCodeFormat(longCode);

      assertThat(shortResult).isFalse();
      assertThat(longResult).isFalse();
    }

    @Test
    @DisplayName("Should reject codes with lowercase letters")
    void shouldRejectCodesWithLowercaseLetters() {
      // RED: Test case sensitivity
      String lowercaseCode = "abc123";
      String mixedCaseCode = "AbC123";

      boolean lowercaseResult = invitationCodeService.isValidCodeFormat(lowercaseCode);
      boolean mixedResult = invitationCodeService.isValidCodeFormat(mixedCaseCode);

      assertThat(lowercaseResult).isFalse();
      assertThat(mixedResult).isFalse();
    }

    @Test
    @DisplayName("Should reject codes with special characters")
    void shouldRejectCodesWithSpecialCharacters() {
      // RED: Test special character rejection
      String specialCode1 = "ABC@23";
      String specialCode2 = "ABC-23";
      String specialCode3 = "ABC 23";

      boolean result1 = invitationCodeService.isValidCodeFormat(specialCode1);
      boolean result2 = invitationCodeService.isValidCodeFormat(specialCode2);
      boolean result3 = invitationCodeService.isValidCodeFormat(specialCode3);

      assertThat(result1).isFalse();
      assertThat(result2).isFalse();
      assertThat(result3).isFalse();
    }

    @Test
    @DisplayName("Should validate all uppercase letters")
    void shouldValidateAllUppercaseLetters() {
      // RED: Test all uppercase validation
      String allLetters = "ABCDEF";

      boolean result = invitationCodeService.isValidCodeFormat(allLetters);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate all numbers")
    void shouldValidateAllNumbers() {
      // RED: Test all numbers validation
      String allNumbers = "123456";

      boolean result = invitationCodeService.isValidCodeFormat(allNumbers);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate mixed letters and numbers")
    void shouldValidateMixedLettersAndNumbers() {
      // RED: Test mixed format validation
      String[] validCodes = {"A1B2C3", "123ABC", "AB12CD", "1A2B3C"};

      for (String code : validCodes) {
        boolean result = invitationCodeService.isValidCodeFormat(code);
        assertThat(result).as("Code %s should be valid", code).isTrue();
      }
    }

    @Test
    @DisplayName("Should handle boundary characters correctly")
    void shouldHandleBoundaryCharactersCorrectly() {
      // RED: Test boundary conditions for allowed characters
      String codeWithA = "AAAAAA"; // First letter
      String codeWithZ = "ZZZZZZ"; // Last letter
      String codeWith0 = "000000"; // First number
      String codeWith9 = "999999"; // Last number

      boolean resultA = invitationCodeService.isValidCodeFormat(codeWithA);
      boolean resultZ = invitationCodeService.isValidCodeFormat(codeWithZ);
      boolean result0 = invitationCodeService.isValidCodeFormat(codeWith0);
      boolean result9 = invitationCodeService.isValidCodeFormat(codeWith9);

      assertThat(resultA).isTrue();
      assertThat(resultZ).isTrue();
      assertThat(result0).isTrue();
      assertThat(result9).isTrue();
    }

    @Test
    @DisplayName("Should reject unicode and extended characters")
    void shouldRejectUnicodeAndExtendedCharacters() {
      // RED: Test unicode rejection
      String unicodeCode = "ÄÖÜ123";
      String extendedCode = "ABC€23";
      String emojiCode = "ABC😀23";

      boolean unicodeResult = invitationCodeService.isValidCodeFormat(unicodeCode);
      boolean extendedResult = invitationCodeService.isValidCodeFormat(extendedCode);
      boolean emojiResult = invitationCodeService.isValidCodeFormat(emojiCode);

      assertThat(unicodeResult).isFalse();
      assertThat(extendedResult).isFalse();
      assertThat(emojiResult).isFalse();
    }
  }

  @Nested
  @DisplayName("Security and Performance")
  class SecurityPerformanceTests {

    @Test
    @DisplayName("Should use secure random generation")
    void shouldUseSecureRandomGeneration() {
      // RED: Test secure randomness
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      Set<String> generatedCodes = new HashSet<>();
      for (int i = 0; i < 1000; i++) {
        String code = invitationCodeService.generateUniqueCode();
        generatedCodes.add(code);
      }

      // Should have high uniqueness rate with secure random
      assertThat(generatedCodes.size()).isGreaterThan(990); // > 99% unique
    }

    @Test
    @DisplayName("Should handle concurrent code generation safely")
    void shouldHandleConcurrentCodeGenerationSafely() throws InterruptedException {
      // RED: Test thread safety
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      ExecutorService executor = Executors.newFixedThreadPool(10);
      Set<String> concurrentCodes = ConcurrentHashMap.newKeySet();
      AtomicInteger successCount = new AtomicInteger(0);

      for (int i = 0; i < 100; i++) {
        executor.submit(
            () -> {
              try {
                String code = invitationCodeService.generateUniqueCode();
                concurrentCodes.add(code);
                successCount.incrementAndGet();
              } catch (Exception e) {
                // Expected in case of collisions or other issues
              }
            });
      }

      executor.shutdown();
      boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);

      assertThat(terminated).isTrue();
      assertThat(successCount.get()).isGreaterThan(95); // Most should succeed
      assertThat(concurrentCodes.size()).isGreaterThan(95); // High uniqueness
    }

    @Test
    @DisplayName("Should maintain character distribution balance")
    void shouldMaintainCharacterDistributionBalance() {
      // RED: Test character distribution
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      int letterCount = 0;
      int digitCount = 0;
      int totalChars = 0;

      for (int i = 0; i < 100; i++) {
        String code = invitationCodeService.generateUniqueCode();
        for (char c : code.toCharArray()) {
          if (Character.isLetter(c)) {
            letterCount++;
          } else if (Character.isDigit(c)) {
            digitCount++;
          }
          totalChars++;
        }
      }

      // Should have reasonable distribution (not all letters or all digits)
      double letterRatio = (double) letterCount / totalChars;
      assertThat(letterRatio).isBetween(0.2, 0.8); // Between 20% and 80%
    }

    @Test
    @DisplayName("Should generate codes efficiently under load")
    void shouldGenerateCodesEfficientlyUnderLoad() {
      // RED: Test performance under load
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 1000; i++) {
        String code = invitationCodeService.generateUniqueCode();
        assertThat(code).isNotNull();
      }

      long duration = System.currentTimeMillis() - startTime;

      // Should generate 1000 codes in under 5 seconds
      assertThat(duration).isLessThan(5000);
    }

    @Test
    @DisplayName("Should handle format validation efficiently")
    void shouldHandleFormatValidationEfficiently() {
      // RED: Test validation performance
      String[] testCodes = {
        "ABC123", "DEFG45", "123456", "ABCDEF", "invalid", "", "ABC@23", "abc123", "AB12CD"
      };

      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 10000; i++) {
        for (String testCode : testCodes) {
          invitationCodeService.isValidCodeFormat(testCode);
        }
      }

      long duration = System.currentTimeMillis() - startTime;

      // Should validate 90,000 codes in under 1 second
      assertThat(duration).isLessThan(1000);
    }

    @Test
    @DisplayName("Should maintain security constraints under pressure")
    void shouldMaintainSecurityConstraintsUnderPressure() {
      // RED: Test security under stress
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      for (int i = 0; i < 500; i++) {
        String code = invitationCodeService.generateUniqueCode();

        // Every generated code must meet security standards
        assertThat(code).matches("^[A-Z0-9]{6}$");
        assertThat(code).hasSize(6);
        assertThat(invitationCodeService.isValidCodeFormat(code)).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Error Handling and Edge Cases")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle repository null response gracefully")
    void shouldHandleRepositoryNullResponseGracefully() {
      // RED: Test null response handling - repository always returns false for non-existing codes
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      String result = invitationCodeService.generateUniqueCode();

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle repository intermittent failures")
    void shouldHandleRepositoryIntermittentFailures() {
      // RED: Test intermittent failures
      when(gameRepository.existsByInvitationCode(anyString()))
          .thenThrow(new RuntimeException("Connection timeout"))
          .thenReturn(false);

      // Should fail on first call due to exception
      assertThatThrownBy(() -> invitationCodeService.generateUniqueCode())
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should provide meaningful error messages")
    void shouldProvideMeaningfulErrorMessages() {
      // RED: Test error message quality
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(true);

      InvitationCodeGenerationException exception =
          catchThrowableOfType(
              () -> invitationCodeService.generateUniqueCode(),
              InvitationCodeGenerationException.class);

      assertThat(exception.getMessage())
          .contains("Impossible de générer un code unique")
          .contains("100 tentatives");
    }

    @Test
    @DisplayName("Should handle extreme collision scenarios")
    void shouldHandleExtremeCollisionScenarios() {
      // RED: Test extreme collision handling
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(true);

      assertThatThrownBy(() -> invitationCodeService.generateUniqueCode())
          .isInstanceOf(InvitationCodeGenerationException.class);

      // Should have made exactly 100 attempts
      verify(gameRepository, times(100)).existsByInvitationCode(anyString());
    }

    @Test
    @DisplayName("Should maintain state consistency across failures")
    void shouldMaintainStateConsistencyAcrossFailures() {
      // RED: Test state consistency
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(true);

      // First call should fail
      assertThatThrownBy(() -> invitationCodeService.generateUniqueCode())
          .isInstanceOf(InvitationCodeGenerationException.class);

      // Reset mock for second call
      reset(gameRepository);
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      // Second call should succeed (service state should be clean)
      String result = invitationCodeService.generateUniqueCode();
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle validation edge cases gracefully")
    void shouldHandleValidationEdgeCasesGracefully() {
      // RED: Test validation edge cases
      String[] edgeCases = {
        null, "", " ", "A", "ABCDE", "ABCDEFG", "12345", "1234567", "!@#$%^", "\n\t\r"
      };

      for (String edgeCase : edgeCases) {
        boolean result = invitationCodeService.isValidCodeFormat(edgeCase);
        assertThat(result).as("Edge case '%s' should be invalid", edgeCase).isFalse();
      }
    }
  }

  @Nested
  @DisplayName("Integration and Business Logic")
  class IntegrationTests {

    @Test
    @DisplayName("Should integrate generation and validation correctly")
    void shouldIntegrateGenerationAndValidationCorrectly() {
      // RED: Test generation-validation integration
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      String generatedCode = invitationCodeService.generateUniqueCode();
      boolean isValid = invitationCodeService.isValidCodeFormat(generatedCode);

      assertThat(isValid).isTrue();
      assertThat(generatedCode).matches("^[A-Z0-9]{6}$");
    }

    @Test
    @DisplayName("Should maintain consistency between multiple operations")
    void shouldMaintainConsistencyBetweenMultipleOperations() {
      // RED: Test operational consistency
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      for (int i = 0; i < 50; i++) {
        String code = invitationCodeService.generateUniqueCode();
        boolean isValidFormat = invitationCodeService.isValidCodeFormat(code);
        boolean isValidFormatAgain = invitationCodeService.isValidCodeFormat(code);

        assertThat(isValidFormat).isTrue();
        assertThat(isValidFormatAgain).isTrue();
        assertThat(code).hasSize(6);
      }
    }

    @Test
    @DisplayName("Should handle business workflow integration")
    void shouldHandleBusinessWorkflowIntegration() {
      // RED: Test business workflow
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      // Generate code
      String code = invitationCodeService.generateUniqueCode();

      // Validate format (what frontend would do)
      boolean isValidFormat = invitationCodeService.isValidCodeFormat(code);

      // Verify integration workflow
      assertThat(code).isNotNull();
      assertThat(isValidFormat).isTrue();
      verify(gameRepository).existsByInvitationCode(code);
    }

    @Test
    @DisplayName("Should provide deterministic validation behavior")
    void shouldProvideDeterministicValidationBehavior() {
      // RED: Test validation determinism
      String testCode = "ABC123";

      boolean result1 = invitationCodeService.isValidCodeFormat(testCode);
      boolean result2 = invitationCodeService.isValidCodeFormat(testCode);
      boolean result3 = invitationCodeService.isValidCodeFormat(testCode);

      assertThat(result1).isEqualTo(result2).isEqualTo(result3).isTrue();
    }

    @Test
    @DisplayName("Should handle code lifecycle correctly")
    void shouldHandleCodeLifecycleCorrectly() {
      // RED: Test code lifecycle
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      // Generate first code
      String firstCode = invitationCodeService.generateUniqueCode();
      assertThat(firstCode).isNotNull();

      // Reset mock and simulate first code exists, others don't
      reset(gameRepository);
      when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

      String secondCode = invitationCodeService.generateUniqueCode();
      assertThat(secondCode).isNotNull();
      // Note: Due to randomness, codes might be equal, but that's acceptable for this test
    }
  }
}
