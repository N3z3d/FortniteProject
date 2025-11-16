package com.fortnite.pronos.debug;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.PronosApplication;
import com.fortnite.pronos.service.TestSecurityConfigTestBackup;

/**
 * Test TDD simple pour vérifier le démarrage de l'application Principe : Red (tests qui échouent) →
 * Green (implémentation) → Refactor
 */
@SpringBootTest(classes = {PronosApplication.class, TestSecurityConfigTestBackup.class})
@ActiveProfiles("test")
@DisplayName("Test TDD Simple - Démarrage de l'Application")
class SimpleStartupTest {

  @Test
  @DisplayName("Devrait démarrer l'application sans erreur")
  void shouldStartApplicationWithoutError() {
    // Given - Application configurée
    // When & Then - Vérification que l'application démarre
    assertTrue(true, "L'application devrait démarrer sans erreur");
  }

  @Test
  @DisplayName("Devrait avoir un contexte Spring valide")
  void shouldHaveValidSpringContext() {
    // Given - Application démarrée
    // When & Then - Vérification du contexte Spring
    assertTrue(true, "Le contexte Spring devrait être valide");
  }
}
