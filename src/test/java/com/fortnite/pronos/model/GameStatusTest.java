package com.fortnite.pronos.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests unitaires pour l'enum GameStatus */
@DisplayName("Tests de l'enum GameStatus")
class GameStatusTest {

  @Test
  @DisplayName("Devrait avoir tous les statuts attendus")
  void shouldHaveAllExpectedStatuses() {
    // Given & When
    GameStatus[] statuses = GameStatus.values();

    // Then
    assertEquals(5, statuses.length);

    // Vérifier que tous les statuts sont présents
    assertTrue(containsStatus(statuses, "CREATING"));
    assertTrue(containsStatus(statuses, "DRAFTING"));
    assertTrue(containsStatus(statuses, "ACTIVE"));
    assertTrue(containsStatus(statuses, "FINISHED"));
    assertTrue(containsStatus(statuses, "CANCELLED"));
  }

  @Test
  @DisplayName("Devrait pouvoir récupérer un statut par son nom")
  void shouldGetStatusByName() {
    // Given & When & Then
    assertEquals(GameStatus.CREATING, GameStatus.valueOf("CREATING"));
    assertEquals(GameStatus.DRAFTING, GameStatus.valueOf("DRAFTING"));
    assertEquals(GameStatus.ACTIVE, GameStatus.valueOf("ACTIVE"));
    assertEquals(GameStatus.FINISHED, GameStatus.valueOf("FINISHED"));
    assertEquals(GameStatus.CANCELLED, GameStatus.valueOf("CANCELLED"));
  }

  @Test
  @DisplayName("Devrait avoir des noms cohérents")
  void shouldHaveConsistentNames() {
    // Given & When & Then
    assertEquals("CREATING", GameStatus.CREATING.name());
    assertEquals("DRAFTING", GameStatus.DRAFTING.name());
    assertEquals("ACTIVE", GameStatus.ACTIVE.name());
    assertEquals("FINISHED", GameStatus.FINISHED.name());
    assertEquals("CANCELLED", GameStatus.CANCELLED.name());
  }

  @Test
  @DisplayName("Devrait pouvoir comparer les statuts")
  void shouldCompareStatuses() {
    // Given & When & Then
    assertNotEquals(GameStatus.CREATING, GameStatus.DRAFTING);
    assertNotEquals(GameStatus.ACTIVE, GameStatus.FINISHED);
    assertEquals(GameStatus.CREATING, GameStatus.CREATING);
    assertEquals(GameStatus.DRAFTING, GameStatus.DRAFTING);
  }

  private boolean containsStatus(GameStatus[] statuses, String statusName) {
    for (GameStatus status : statuses) {
      if (status.name().equals(statusName)) {
        return true;
      }
    }
    return false;
  }
}
