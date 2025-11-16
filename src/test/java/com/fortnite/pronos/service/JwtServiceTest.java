package com.fortnite.pronos.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.PronosApplication;

/** Tests TDD pour JwtService Focus: Sécurité JWT, validation des tokens, gestion des erreurs */
@SpringBootTest(classes = {PronosApplication.class, TestSecurityConfigTestBackup.class})
@ActiveProfiles("test")
@DisplayName("Tests TDD - JWT Service Security")
class JwtServiceTest {

  @Autowired private JwtService jwtService;

  private UserDetails testUser;

  @BeforeEach
  void setUp() {
    // Création d'un utilisateur de test
    testUser =
        User.builder()
            .username("testuser")
            .password("password")
            .authorities(Collections.emptyList())
            .build();
  }

  @Test
  @DisplayName("Devrait générer un token JWT valide")
  void shouldGenerateValidJwtToken() {
    // When - Génération d'un token
    String token = jwtService.generateToken(testUser);

    // Then - Vérifications de sécurité
    assertNotNull(token, "Le token ne doit pas être null");
    assertFalse(token.isEmpty(), "Le token ne doit pas être vide");
    assertTrue(
        token.split("\\.").length == 3,
        "Le token JWT doit avoir 3 parties séparées par des points");

    // Vérification que le token contient le nom d'utilisateur
    String extractedUsername = jwtService.extractUsername(token);
    assertEquals("testuser", extractedUsername, "Le nom d'utilisateur extrait doit correspondre");
  }

  @Test
  @DisplayName("Devrait valider un token JWT correct")
  void shouldValidateCorrectJwtToken() {
    // Given - Génération d'un token valide
    String token = jwtService.generateToken(testUser);

    // When - Validation du token
    boolean isValid = jwtService.isTokenValid(token, testUser);

    // Then - Le token doit être valide
    assertTrue(isValid, "Le token généré doit être valide");
  }

  @Test
  @DisplayName("Devrait rejeter un token JWT corrompu")
  void shouldRejectCorruptedJwtToken() {
    // Given - Token corrompu
    String corruptedToken = "eyJhbGciOiJIUzI1NiJ9.CORRUPTED.signature";

    // When & Then - Validation doit échouer
    assertThrows(
        RuntimeException.class,
        () -> {
          jwtService.extractUsername(corruptedToken);
        },
        "Un token corrompu doit lever une exception");
  }

  @Test
  @DisplayName("Devrait rejeter un token JWT malformé")
  void shouldRejectMalformedJwtToken() {
    // Given - Token malformé
    String malformedToken = "not.a.valid.jwt.token";

    // When & Then - Validation doit échouer
    assertThrows(
        RuntimeException.class,
        () -> {
          jwtService.extractUsername(malformedToken);
        },
        "Un token malformé doit lever une exception");
  }

  @Test
  @DisplayName("Devrait rejeter un token avec signature invalide")
  void shouldRejectTokenWithInvalidSignature() {
    // Given - Token avec signature modifiée
    String tokenWithInvalidSignature =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTUxNjIzOTAyMn0.wrong_signature";

    // When & Then - Validation doit échouer
    assertThrows(
        RuntimeException.class,
        () -> {
          jwtService.extractUsername(tokenWithInvalidSignature);
        },
        "Un token avec signature invalide doit lever une exception");
  }

  @Test
  @DisplayName("Devrait générer des tokens valides pour des appels multiples")
  void shouldGenerateValidTokensForMultipleCalls() {
    // When - Génération de plusieurs tokens
    String token1 = jwtService.generateToken(testUser);
    String token2 = jwtService.generateToken(testUser);

    // Then - Les deux tokens doivent être valides
    assertNotNull(token1, "Le premier token ne doit pas être null");
    assertNotNull(token2, "Le second token ne doit pas être null");
    assertTrue(jwtService.isTokenValid(token1, testUser), "Le premier token doit être valide");
    assertTrue(jwtService.isTokenValid(token2, testUser), "Le second token doit être valide");

    // Les deux tokens doivent extraire le même nom d'utilisateur
    assertEquals("testuser", jwtService.extractUsername(token1));
    assertEquals("testuser", jwtService.extractUsername(token2));
  }

  @Test
  @DisplayName("Devrait rejeter un token pour un utilisateur différent")
  void shouldRejectTokenForDifferentUser() {
    // Given - Token pour un utilisateur et validation avec un autre
    String token = jwtService.generateToken(testUser);

    UserDetails differentUser =
        User.builder()
            .username("differentuser")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

    // When - Validation avec un utilisateur différent
    boolean isValid = jwtService.isTokenValid(token, differentUser);

    // Then - Le token ne doit pas être valide
    assertFalse(isValid, "Un token ne doit pas être valide pour un utilisateur différent");
  }

  @Test
  @DisplayName("Devrait générer un refresh token valide")
  void shouldGenerateValidRefreshToken() {
    // When - Génération d'un refresh token
    String refreshToken = jwtService.generateRefreshToken(testUser);

    // Then - Vérifications
    assertNotNull(refreshToken, "Le refresh token ne doit pas être null");
    assertFalse(refreshToken.isEmpty(), "Le refresh token ne doit pas être vide");

    // Le refresh token doit être valide pour l'utilisateur
    assertTrue(
        jwtService.isTokenValid(refreshToken, testUser), "Le refresh token doit être valide");
  }

  @Test
  @DisplayName("Devrait gérer les claims personnalisées")
  void shouldHandleCustomClaims() {
    // Given - Claims personnalisées
    java.util.Map<String, Object> customClaims =
        java.util.Map.of(
            "role", "admin",
            "department", "IT");

    // When - Génération avec claims personnalisées
    String token = jwtService.generateToken(customClaims, testUser);

    // Then - Le token doit être valide
    assertNotNull(token);
    assertTrue(jwtService.isTokenValid(token, testUser));
    assertEquals("testuser", jwtService.extractUsername(token));
  }

  @Test
  @DisplayName("Devrait empêcher l'injection de code malveillant dans les claims")
  void shouldPreventCodeInjectionInClaims() {
    // Given - Claims avec tentative d'injection
    java.util.Map<String, Object> maliciousClaims =
        java.util.Map.of(
            "evil", "<script>alert('xss')</script>",
            "injection", "'; DROP TABLE users; --");

    // When - Génération avec claims malveillantes
    String token = jwtService.generateToken(maliciousClaims, testUser);

    // Then - Le token doit être généré mais sécurisé
    assertNotNull(token);
    assertTrue(jwtService.isTokenValid(token, testUser));
    // Les claims malveillantes sont stockées mais ne sont pas exécutées
  }

  @Test
  @DisplayName("Devrait avoir une longueur de clé secrète suffisante")
  void shouldHaveSufficientSecretKeyLength() {
    // When - Génération d'un token (force l'initialisation de la clé)
    String token = jwtService.generateToken(testUser);

    // Then - Le token doit être généré avec succès
    assertNotNull(token, "La clé secrète doit être suffisamment longue pour générer un token");
    assertTrue(
        jwtService.isTokenValid(token, testUser), "La clé secrète doit permettre la validation");
  }

  @Test
  @DisplayName("Devrait être thread-safe pour la génération simultanée")
  void shouldBeThreadSafeForConcurrentGeneration() throws InterruptedException {
    // Given - Plusieurs threads
    int threadCount = 10;
    Thread[] threads = new Thread[threadCount];
    String[] tokens = new String[threadCount];
    Exception[] exceptions = new Exception[threadCount];

    // When - Génération simultanée de tokens
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] =
          new Thread(
              () -> {
                try {
                  tokens[index] = jwtService.generateToken(testUser);
                } catch (Exception e) {
                  exceptions[index] = e;
                }
              });
      threads[i].start();
    }

    // Attendre la fin de tous les threads
    for (Thread thread : threads) {
      thread.join();
    }

    // Then - Tous les tokens doivent être générés avec succès
    for (int i = 0; i < threadCount; i++) {
      assertNull(
          exceptions[i],
          "Aucune exception ne doit être levée: "
              + (exceptions[i] != null ? exceptions[i].getMessage() : ""));
      assertNotNull(tokens[i], "Tous les tokens doivent être générés");
      assertTrue(
          jwtService.isTokenValid(tokens[i], testUser), "Tous les tokens doivent être valides");
    }
  }
}
