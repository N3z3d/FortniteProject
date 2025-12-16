package com.fortnite.pronos.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests TDD - JWT Service Security (unit)")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class JwtServiceTest {

  @Mock private org.springframework.core.env.Environment environment;

  private JwtService jwtService;
  private UserDetails testUser;

  @BeforeEach
  void setUp() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"test"});

    jwtService = new JwtService(environment);
    // Inject test-friendly configuration
    ReflectionTestUtils.setField(
        jwtService, "secretKey", "test-secret-key-32-characters-long-123456");
    ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
    ReflectionTestUtils.setField(jwtService, "refreshExpiration", 7_200_000L);

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
    String token = jwtService.generateToken(testUser);

    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertEquals(3, token.split("\\.").length);
    assertEquals("testuser", jwtService.extractUsername(token));
  }

  @Test
  @DisplayName("Devrait valider un token JWT correct")
  void shouldValidateCorrectJwtToken() {
    String token = jwtService.generateToken(testUser);
    assertTrue(jwtService.isTokenValid(token, testUser));
  }

  @Test
  @DisplayName("Devrait rejeter un token JWT corrompu")
  void shouldRejectCorruptedJwtToken() {
    String corruptedToken = "eyJhbGciOiJIUzI1NiJ9.CORRUPTED.signature";
    assertThrows(RuntimeException.class, () -> jwtService.extractUsername(corruptedToken));
  }

  @Test
  @DisplayName("Devrait rejeter un token JWT malformé")
  void shouldRejectMalformedJwtToken() {
    String malformedToken = "not.a.valid.jwt.token";
    assertThrows(RuntimeException.class, () -> jwtService.extractUsername(malformedToken));
  }

  @Test
  @DisplayName("Devrait rejeter un token avec signature invalide")
  void shouldRejectTokenWithInvalidSignature() {
    String tokenWithInvalidSignature =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTUxNjIzOTAyMn0.wrong_signature";
    assertThrows(
        RuntimeException.class, () -> jwtService.extractUsername(tokenWithInvalidSignature));
  }

  @Test
  @DisplayName("Devrait générer des tokens valides pour des appels multiples")
  void shouldGenerateValidTokensForMultipleCalls() {
    String token1 = jwtService.generateToken(testUser);
    String token2 = jwtService.generateToken(testUser);

    assertNotNull(token1);
    assertNotNull(token2);
    assertTrue(jwtService.isTokenValid(token1, testUser));
    assertTrue(jwtService.isTokenValid(token2, testUser));
    assertEquals("testuser", jwtService.extractUsername(token1));
    assertEquals("testuser", jwtService.extractUsername(token2));
  }

  @Test
  @DisplayName("Devrait rejeter un token pour un utilisateur différent")
  void shouldRejectTokenForDifferentUser() {
    String token = jwtService.generateToken(testUser);
    UserDetails differentUser =
        User.builder()
            .username("differentuser")
            .password("password")
            .authorities(Collections.emptyList())
            .build();

    assertFalse(jwtService.isTokenValid(token, differentUser));
  }

  @Test
  @DisplayName("Devrait générer un refresh token valide")
  void shouldGenerateValidRefreshToken() {
    String refreshToken = jwtService.generateRefreshToken(testUser);
    assertNotNull(refreshToken);
    assertFalse(refreshToken.isEmpty());
    assertTrue(jwtService.isTokenValid(refreshToken, testUser));
  }

  @Test
  @DisplayName("Devrait gérer les claims personnalisées")
  void shouldHandleCustomClaims() {
    Map<String, Object> customClaims = Map.of("role", "admin", "department", "IT");
    String token = jwtService.generateToken(customClaims, testUser);

    assertNotNull(token);
    assertTrue(jwtService.isTokenValid(token, testUser));
    assertEquals("testuser", jwtService.extractUsername(token));
  }

  @Test
  @DisplayName("Devrait empêcher l'injection de code malveillant dans les claims")
  void shouldPreventCodeInjectionInClaims() {
    Map<String, Object> maliciousClaims =
        Map.of("evil", "<script>alert('xss')</script>", "injection", "'; DROP TABLE users; --");

    String token = jwtService.generateToken(maliciousClaims, testUser);
    assertNotNull(token);
    assertTrue(jwtService.isTokenValid(token, testUser));
  }

  @Test
  @DisplayName("Devrait avoir une longueur de clé secrète suffisante")
  void shouldHaveSufficientSecretKeyLength() {
    String token = jwtService.generateToken(testUser);
    assertNotNull(token);
    assertTrue(jwtService.isTokenValid(token, testUser));
  }

  @Test
  @DisplayName("Devrait être thread-safe pour la génération simultanée")
  void shouldBeThreadSafeForConcurrentGeneration() throws InterruptedException {
    int threadCount = 10;
    Thread[] threads = new Thread[threadCount];
    String[] tokens = new String[threadCount];
    Exception[] exceptions = new Exception[threadCount];

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

    for (Thread thread : threads) {
      thread.join();
    }

    for (int i = 0; i < threadCount; i++) {
      assertNull(exceptions[i], "Aucune exception ne doit être levée");
      assertNotNull(tokens[i]);
      assertTrue(jwtService.isTokenValid(tokens[i], testUser));
    }
  }
}
