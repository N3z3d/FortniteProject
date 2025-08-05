package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.service.InvitationCodeService.InvitationCodeGenerationException;

/** Tests TDD pour InvitationCodeService Clean Code : tests clairs et focalisés */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests TDD - InvitationCodeService")
class InvitationCodeServiceTest {

  @Mock private GameRepository gameRepository;

  @InjectMocks private InvitationCodeService invitationCodeService;

  @Test
  @DisplayName("Devrait générer un code d'invitation unique de 6 caractères")
  void shouldGenerateUniqueCodeWith6Characters() {
    // Given
    when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

    // When
    String code = invitationCodeService.generateUniqueCode();

    // Then
    assertThat(code).isNotNull();
    assertThat(code).hasSize(6);
    assertThat(code).matches("^[A-Z0-9]{6}$");
    verify(gameRepository, times(1)).existsByInvitationCode(anyString());
  }

  @Test
  @DisplayName("Devrait réessayer si le code existe déjà")
  void shouldRetryIfCodeAlreadyExists() {
    // Given
    when(gameRepository.existsByInvitationCode(anyString()))
        .thenReturn(true) // Premier code existe
        .thenReturn(true) // Deuxième code existe
        .thenReturn(false); // Troisième code n'existe pas

    // When
    String code = invitationCodeService.generateUniqueCode();

    // Then
    assertThat(code).isNotNull();
    verify(gameRepository, times(3)).existsByInvitationCode(anyString());
  }

  @Test
  @DisplayName("Devrait lancer une exception après 100 tentatives")
  void shouldThrowExceptionAfter100Attempts() {
    // Given
    when(gameRepository.existsByInvitationCode(anyString())).thenReturn(true);

    // When/Then
    assertThatThrownBy(() -> invitationCodeService.generateUniqueCode())
        .isInstanceOf(InvitationCodeGenerationException.class)
        .hasMessageContaining("100 tentatives");

    verify(gameRepository, times(100)).existsByInvitationCode(anyString());
  }

  @Test
  @DisplayName("Devrait valider un code au format correct")
  void shouldValidateCorrectCodeFormat() {
    // Given
    String validCode = "ABC123";

    // When
    boolean isValid = invitationCodeService.isValidCodeFormat(validCode);

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Devrait rejeter un code null")
  void shouldRejectNullCode() {
    // When
    boolean isValid = invitationCodeService.isValidCodeFormat(null);

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Devrait rejeter un code trop court")
  void shouldRejectShortCode() {
    // Given
    String shortCode = "ABC12";

    // When
    boolean isValid = invitationCodeService.isValidCodeFormat(shortCode);

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Devrait rejeter un code trop long")
  void shouldRejectLongCode() {
    // Given
    String longCode = "ABC1234";

    // When
    boolean isValid = invitationCodeService.isValidCodeFormat(longCode);

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Devrait rejeter un code avec des caractères invalides")
  void shouldRejectCodeWithInvalidCharacters() {
    // Given
    String invalidCode = "ABC@23";

    // When
    boolean isValid = invitationCodeService.isValidCodeFormat(invalidCode);

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Performance - Devrait générer un code en moins de 50ms")
  void shouldGenerateCodeQuickly() {
    // Given
    when(gameRepository.existsByInvitationCode(anyString())).thenReturn(false);

    // When
    long startTime = System.currentTimeMillis();
    String code = invitationCodeService.generateUniqueCode();
    long endTime = System.currentTimeMillis();

    // Then
    assertThat(code).isNotNull();
    assertThat(endTime - startTime).isLessThan(50);
  }
}
