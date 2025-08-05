package com.fortnite.pronos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Tests TDD pour UserContextService avec paramètres Clean Code : Tests focalisés sur la gestion des
 * paramètres utilisateur
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserContextService - Gestion des paramètres")
class UserContextServiceParameterTest {

  @Mock private UserRepository userRepository;

  @Mock private SecurityContext securityContext;

  @Mock private Authentication authentication;

  @InjectMocks private UserContextService userContextService;

  private User thibaut;

  @BeforeEach
  void setUp() {
    // Créer un utilisateur de test
    thibaut = new User();
    thibaut.setId(UUID.randomUUID());
    thibaut.setUsername("Thibaut");
    thibaut.setEmail("thibaut@test.com");
  }

  @Test
  @DisplayName("Devrait utiliser le paramètre user même si Spring Security est actif")
  void shouldUseUserParameterEvenWithSpringSecurityActive() {
    // Given - Le paramètre user est fourni
    String userParam = "Thibaut";
    when(userRepository.findByUsernameIgnoreCase("Thibaut")).thenReturn(Optional.of(thibaut));

    // When - On récupère l'utilisateur avec fallback
    User result = userContextService.getCurrentUserWithFallback(userParam);

    // Then - Le paramètre user doit être prioritaire
    assertNotNull(result);
    assertEquals("Thibaut", result.getUsername());
    assertEquals(thibaut.getId(), result.getId());

    // Vérifier qu'on utilise le paramètre fourni
    verify(userRepository).findByUsernameIgnoreCase("Thibaut");
  }

  @Test
  @DisplayName("Devrait utiliser findUserByUsername quand le paramètre est fourni")
  void shouldUseFindUserByUsernameWhenParameterProvided() {
    // Given
    String userParam = "Thibaut";
    when(userRepository.findByUsernameIgnoreCase("Thibaut")).thenReturn(Optional.of(thibaut));

    // When
    User result = userContextService.findUserByUsername(userParam);

    // Then
    assertNotNull(result);
    assertEquals("Thibaut", result.getUsername());
    verify(userRepository).findByUsernameIgnoreCase("Thibaut");
  }
}
