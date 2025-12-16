package com.fortnite.pronos.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fortnite.pronos.dto.CreateGameRequest;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

/*
 * TODO: Performance tests skipped - Require dedicated performance testing environment
 *
 * CONTEXT:
 * - Ces tests de performance mesurent les temps de réponse (200-500ms) et la charge concurrente
 * - Les résultats sont fortement dépendants de l'environnement d'exécution:
 *   * Machine de dev (CPU, RAM, disque)
 *   * Charge système actuelle
 *   * Database en mémoire (H2) vs production (PostgreSQL)
 *   * Configuration JVM et Spring Boot
 * - Les tests échouent souvent en CI/CD ou sur machines moins performantes
 * - Les seuils de performance (200ms, 300ms, 500ms) sont arbitraires et non validés en production
 *
 * PROBLÈMES ACTUELS:
 * 1. shouldCreateGameUnder200ms - Dépend de l'authentification et validation complexe
 * 2. shouldHandle10ConcurrentGameCreations - Nécessite pool de connexions DB optimisé
 * 3. shouldHandle50ConcurrentReadRequests - Taux de succès > 95% difficile sur H2
 * 4. shouldHandleDatabaseTimeouts - Crée 1000+ games, pollue la base de test
 * 5. shouldMaintainPerformanceUnderLoad - Test de charge progressive (10-100 requêtes)
 *
 * ACTION REQUIRED:
 * 1. Créer un profil de test dédié "performance" séparé des tests fonctionnels:
 *    - @Tag("performance") sur la classe
 *    - Configuration Maven: <groups>!performance</groups> par défaut
 *    - Exécution manuelle: mvn test -Dgroups=performance
 *
 * 2. Environnement de test performance:
 *    - Utiliser Testcontainers avec PostgreSQL (plus proche de la prod)
 *    - Configurer pool de connexions HikariCP (ex: maximumPoolSize=20)
 *    - JVM avec -Xmx2g -XX:+UseG1GC pour stabilité
 *    - Exécuter sur CI dédiée avec ressources garanties
 *
 * 3. Ajuster les seuils de performance:
 *    - Faire un baseline sur environnement de référence
 *    - Mesurer P50, P95, P99 plutôt que des assertions strictes
 *    - Utiliser JMH (Java Microbenchmark Harness) pour précision
 *    - Comparer les résultats entre runs (détection de régression)
 *
 * 4. Alternative recommandée - Tests de charge externes:
 *    - Utiliser JMeter, Gatling ou K6 pour load testing
 *    - Déployer app sur environnement staging
 *    - Générer des rapports de performance détaillés
 *    - Intégrer dans pipeline CD (non bloquant)
 *
 * FICHIERS À CRÉER:
 * - src/test/java/com/fortnite/pronos/performance/ (package dédié)
 * - pom.xml: configuration <groups> pour exclure/inclure tests performance
 * - .github/workflows/performance-tests.yml (CI séparée, weekly)
 * - docs/PERFORMANCE_TESTING.md (guide pour exécuter et interpréter les tests)
 *
 * ESTIMATION:
 * - Setup Testcontainers + profil performance: 3-4h
 * - Ajuster seuils et ajouter métriques P95/P99: 2h
 * - Alternative JMeter/Gatling: 4-6h
 * - Documentation et CI: 2h
 * TOTAL: 11-14h (2 jours)
 *
 * PRIORITÉ: BASSE (tests fonctionnels d'abord, puis optimisation)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Disabled(
    "Performance tests require dedicated environment and profiling setup. See TODO above for details.")
@DisplayName("Tests de Performance TDD - Intégration")
class PerformanceIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;

  @Autowired private GameRepository gameRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;
  private List<User> testUsers;
  private List<Game> testGames;
  private static final String TEST_USERNAME = "TestUser0";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    testUsers = new ArrayList<>();
    testGames = new ArrayList<>();

    // Création de données de test pour les tests de performance
    createTestData();
  }

  private void createTestData() {
    // Création de 100 utilisateurs de test
    for (int i = 0; i < 100; i++) {
      User user = new User();
      user.setId(UUID.randomUUID());
      user.setUsername("TestUser" + i);
      user.setEmail("user" + i + "@test.com");
      user.setPassword("password123");
      user.setRole(User.UserRole.USER);
      user.setCurrentSeason(2025);
      testUsers.add(userRepository.save(user));
    }

    // Création de 50 games de test
    for (int i = 0; i < 50; i++) {
      Game game = new Game();
      game.setId(UUID.randomUUID());
      game.setName("Performance Test Game " + i);
      game.setCreator(testUsers.get(i % testUsers.size()));
      game.setMaxParticipants(10);
      game.setStatus(GameStatus.CREATING);
      game.setCreatedAt(LocalDateTime.now());
      testGames.add(gameRepository.save(game));
    }
  }

  @Test
  @DisplayName("Devrait récupérer toutes les games en moins de 500ms")
  void shouldGetAllGamesUnder500ms() throws Exception {
    // Given - Données de test créées dans setUp()

    // When & Then - Test de performance
    long startTime = System.currentTimeMillis();

    performWithUser(get("/api/games"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // Assertion de performance
    assert duration < 500
        : "La récupération de toutes les games prend trop de temps: " + duration + "ms";
  }

  @Test
  @DisplayName("Devrait récupérer les games disponibles en moins de 300ms")
  void shouldGetAvailableGamesUnder300ms() throws Exception {
    // Given - Données de test créées dans setUp()

    // When & Then - Test de performance
    long startTime = System.currentTimeMillis();

    performWithUser(get("/api/games/available"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // Assertion de performance
    assert duration < 300
        : "La récupération des games disponibles prend trop de temps: " + duration + "ms";
  }

  @Test
  @DisplayName("Devrait créer une game en moins de 200ms")
  void shouldCreateGameUnder200ms() throws Exception {
    // Given - Requête de création
    CreateGameRequest createRequest = new CreateGameRequest();
    createRequest.setName("Performance Test Game");
    createRequest.setMaxParticipants(5);
    createRequest.setRegionRules(new HashMap<>());
    createRequest.getRegionRules().put(Player.Region.EU, 3);
    createRequest.getRegionRules().put(Player.Region.NAC, 2);

    // When & Then - Test de performance
    long startTime = System.currentTimeMillis();

    performWithUser(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated());

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // Assertion de performance
    assert duration < 200 : "La création de game prend trop de temps: " + duration + "ms";
  }

  @Test
  @DisplayName("Devrait gérer 10 requêtes concurrentes de création de games")
  void shouldHandle10ConcurrentGameCreations() throws Exception {
    // Given - 10 requêtes de création
    List<CreateGameRequest> requests = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      CreateGameRequest request = new CreateGameRequest();
      request.setName("Concurrent Game " + i);
      request.setMaxParticipants(5);
      request.setRegionRules(new HashMap<>());
      request.getRegionRules().put(Player.Region.EU, 3);
      request.getRegionRules().put(Player.Region.NAC, 2);
      requests.add(request);
    }

    // When - Exécution concurrente
    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<CompletableFuture<Long>> futures = new ArrayList<>();

    for (CreateGameRequest request : requests) {
      CompletableFuture<Long> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  long startTime = System.currentTimeMillis();
                  performWithUser(
                          post("/api/games")
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
                      .andExpect(status().isCreated());
                  long endTime = System.currentTimeMillis();
                  return endTime - startTime;
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              },
              executor);
      futures.add(future);
    }

    // Then - Attendre la fin de toutes les requêtes
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    // Vérification des performances
    long totalTime = 0;
    for (CompletableFuture<Long> future : futures) {
      long duration = future.get();
      totalTime += duration;
      assert duration < 1000 : "Une requête concurrente prend trop de temps: " + duration + "ms";
    }

    long averageTime = totalTime / futures.size();
    assert averageTime < 500 : "Le temps moyen est trop élevé: " + averageTime + "ms";
  }

  @Test
  @DisplayName("Devrait gérer 50 requêtes concurrentes de lecture")
  void shouldHandle50ConcurrentReadRequests() throws Exception {
    // Given - 50 requêtes de lecture
    int requestCount = 50;

    // When - Exécution concurrente
    ExecutorService executor = Executors.newFixedThreadPool(20);
    List<CompletableFuture<Long>> futures = new ArrayList<>();

    for (int i = 0; i < requestCount; i++) {
      CompletableFuture<Long> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  long startTime = System.currentTimeMillis();
                  performWithUser(get("/api/games")).andExpect(status().isOk());
                  long endTime = System.currentTimeMillis();
                  return endTime - startTime;
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              },
              executor);
      futures.add(future);
    }

    // Then - Attendre la fin de toutes les requêtes
    executor.shutdown();
    executor.awaitTermination(15, TimeUnit.SECONDS);

    // Vérification des performances
    long totalTime = 0;
    int successCount = 0;
    for (CompletableFuture<Long> future : futures) {
      try {
        long duration = future.get();
        totalTime += duration;
        successCount++;
        assert duration < 1000 : "Une requête de lecture prend trop de temps: " + duration + "ms";
      } catch (Exception e) {
        // Ignorer les échecs pour ce test de performance
      }
    }

    // Vérification du taux de succès
    double successRate = (double) successCount / requestCount;
    assert successRate > 0.95 : "Le taux de succès est trop faible: " + (successRate * 100) + "%";

    if (successCount > 0) {
      long averageTime = totalTime / successCount;
      assert averageTime < 300 : "Le temps moyen de lecture est trop élevé: " + averageTime + "ms";
    }
  }

  @Test
  @DisplayName("Devrait gérer les requêtes mixtes (lecture/écriture) en moins de 1 seconde")
  void shouldHandleMixedRequestsUnder1Second() throws Exception {
    // Given - Mélange de requêtes de lecture et d'écriture
    int totalRequests = 20;
    int writeRequests = 5;
    int readRequests = totalRequests - writeRequests;

    // When - Exécution des requêtes
    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<CompletableFuture<Long>> futures = new ArrayList<>();

    // Requêtes d'écriture
    for (int i = 0; i < writeRequests; i++) {
      CreateGameRequest request = new CreateGameRequest();
      request.setName("Mixed Test Game " + i);
      request.setMaxParticipants(5);
      request.setRegionRules(new HashMap<>());
      request.getRegionRules().put(Player.Region.EU, 3);
      request.getRegionRules().put(Player.Region.NAC, 2);

      CompletableFuture<Long> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  long startTime = System.currentTimeMillis();
                  performWithUser(
                          post("/api/games")
                              .contentType(MediaType.APPLICATION_JSON)
                              .content(objectMapper.writeValueAsString(request)))
                      .andExpect(status().isCreated());
                  long endTime = System.currentTimeMillis();
                  return endTime - startTime;
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              },
              executor);
      futures.add(future);
    }

    // Requêtes de lecture
    for (int i = 0; i < readRequests; i++) {
      CompletableFuture<Long> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  long startTime = System.currentTimeMillis();
                  performWithUser(get("/api/games/available")).andExpect(status().isOk());
                  long endTime = System.currentTimeMillis();
                  return endTime - startTime;
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              },
              executor);
      futures.add(future);
    }

    // Then - Attendre la fin de toutes les requêtes
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    // Vérification des performances
    long totalTime = 0;
    int successCount = 0;
    for (CompletableFuture<Long> future : futures) {
      try {
        long duration = future.get();
        totalTime += duration;
        successCount++;
        assert duration < 1000 : "Une requête mixte prend trop de temps: " + duration + "ms";
      } catch (Exception e) {
        // Ignorer les échecs pour ce test de performance
      }
    }

    // Vérification du taux de succès
    double successRate = (double) successCount / totalRequests;
    assert successRate > 0.9 : "Le taux de succès est trop faible: " + (successRate * 100) + "%";

    if (successCount > 0) {
      long averageTime = totalTime / successCount;
      assert averageTime < 500
          : "Le temps moyen des requêtes mixtes est trop élevé: " + averageTime + "ms";
    }
  }

  @Test
  @DisplayName("Devrait maintenir les performances sous charge")
  void shouldMaintainPerformanceUnderLoad() throws Exception {
    // Given - Test de charge progressive
    int[] requestCounts = {10, 25, 50, 100};
    List<Long> averageTimes = new ArrayList<>();

    for (int requestCount : requestCounts) {
      // When - Exécution du nombre de requêtes
      ExecutorService executor = Executors.newFixedThreadPool(Math.min(requestCount, 20));
      List<CompletableFuture<Long>> futures = new ArrayList<>();

      for (int i = 0; i < requestCount; i++) {
        CompletableFuture<Long> future =
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    long startTime = System.currentTimeMillis();
                    performWithUser(get("/api/games")).andExpect(status().isOk());
                    long endTime = System.currentTimeMillis();
                    return endTime - startTime;
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                },
                executor);
        futures.add(future);
      }

      // Then - Calcul du temps moyen
      executor.shutdown();
      executor.awaitTermination(30, TimeUnit.SECONDS);

      long totalTime = 0;
      int successCount = 0;
      for (CompletableFuture<Long> future : futures) {
        try {
          long duration = future.get();
          totalTime += duration;
          successCount++;
        } catch (Exception e) {
          // Ignorer les échecs
        }
      }

      if (successCount > 0) {
        long averageTime = totalTime / successCount;
        averageTimes.add(averageTime);
      }
    }

    // Vérification que les performances ne se dégradent pas trop
    if (averageTimes.size() >= 2) {
      long firstAverage = averageTimes.get(0);
      long lastAverage = averageTimes.get(averageTimes.size() - 1);

      // La dégradation ne doit pas dépasser 300%
      double degradation = (double) lastAverage / firstAverage;
      assert degradation < 3.0
          : "La dégradation des performances est trop importante: " + degradation + "x";
    }
  }

  @Test
  @DisplayName("Devrait gérer les timeouts de base de données")
  void shouldHandleDatabaseTimeouts() throws Exception {
    // Given - Test avec beaucoup de données
    // Création de 1000 games pour tester les limites
    for (int i = 0; i < 1000; i++) {
      Game game = new Game();
      game.setId(UUID.randomUUID());
      game.setName("Timeout Test Game " + i);
      game.setCreator(testUsers.get(i % testUsers.size()));
      game.setMaxParticipants(10);
      game.setStatus(GameStatus.CREATING);
      game.setCreatedAt(LocalDateTime.now());
      gameRepository.save(game);
    }

    // When & Then - Test de récupération avec beaucoup de données
    long startTime = System.currentTimeMillis();

    performWithUser(get("/api/games"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // Assertion de performance avec beaucoup de données
    assert duration < 2000
        : "La récupération avec beaucoup de données prend trop de temps: " + duration + "ms";
  }

  private org.springframework.test.web.servlet.ResultActions performWithUser(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
      throws Exception {
    return mockMvc.perform(builder.header("X-Test-User", TEST_USERNAME));
  }
}
