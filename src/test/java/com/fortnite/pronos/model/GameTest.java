package com.fortnite.pronos.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests unitaires pour l'entité Game */
@DisplayName("Tests de l'entité Game")
class GameTest {

  private Game game;
  private User creator;
  private GameRegionRule regionRule;
  private GameParticipant participant;

  @BeforeEach
  void setUp() {
    // Création d'un utilisateur créateur
    creator = new User();
    creator.setId(UUID.randomUUID());
    creator.setUsername("TestCreator");
    creator.setEmail("creator@test.com");
    creator.setRole(User.UserRole.USER);

    // Création d'une game de test
    game = new Game();
    game.setId(UUID.randomUUID());
    game.setName("Test Game");
    game.setCreator(creator);
    game.setMaxParticipants(10);
    game.setStatus(GameStatus.CREATING);
    game.setCreatedAt(LocalDateTime.now());

    // Création d'une règle régionale
    regionRule = new GameRegionRule();
    regionRule.setId(UUID.randomUUID());
    regionRule.setRegion(Player.Region.EU);
    regionRule.setMaxPlayers(7);

    // Création d'un participant
    User participantUser = new User();
    participantUser.setId(UUID.randomUUID());
    participantUser.setUsername("TestParticipant");
    participantUser.setEmail("participant@test.com");
    participantUser.setRole(User.UserRole.USER);

    participant = new GameParticipant();
    participant.setId(UUID.randomUUID());
    participant.setUser(participantUser);
    participant.setDraftOrder(1);
  }

  @Test
  @DisplayName("Devrait créer une game valide")
  void shouldCreateValidGame() {
    // Given & When (déjà fait dans setUp)

    // Then
    assertNotNull(game);
    assertEquals("Test Game", game.getName());
    assertEquals(creator, game.getCreator());
    assertEquals(10, game.getMaxParticipants());
    assertEquals(GameStatus.CREATING, game.getStatus());
    assertNotNull(game.getCreatedAt());
  }

  @Test
  @DisplayName("Devrait accepter de nouveaux participants quand il y a de la place")
  void shouldAcceptParticipantsWhenSpaceAvailable() {
    // Given
    game.setMaxParticipants(5);

    // When & Then
    assertTrue(game.canAcceptParticipants());

    // Ajouter 4 participants
    for (int i = 0; i < 4; i++) {
      game.addParticipant(participant);
      assertTrue(game.canAcceptParticipants());
    }

    // Ajouter le 5ème participant
    game.addParticipant(participant);
    assertFalse(game.canAcceptParticipants());
  }

  @Test
  @DisplayName("Devrait ajouter et supprimer des règles régionales")
  void shouldAddAndRemoveRegionRules() {
    // Given
    int initialSize = game.getRegionRules().size();

    // When - Ajouter une règle
    game.addRegionRule(regionRule);

    // Then
    assertEquals(initialSize + 1, game.getRegionRules().size());
    assertTrue(game.getRegionRules().contains(regionRule));
    assertEquals(game, regionRule.getGame());

    // When - Supprimer la règle
    game.removeRegionRule(regionRule);

    // Then
    assertEquals(initialSize, game.getRegionRules().size());
    assertFalse(game.getRegionRules().contains(regionRule));
    assertNull(regionRule.getGame());
  }

  @Test
  @DisplayName("Devrait ajouter et supprimer des participants")
  void shouldAddAndRemoveParticipants() {
    // Given
    int initialSize = game.getParticipants().size();

    // When - Ajouter un participant
    game.addParticipant(participant);

    // Then
    assertEquals(initialSize + 1, game.getParticipants().size());
    assertTrue(game.getParticipants().contains(participant));
    assertEquals(game, participant.getGame());

    // When - Supprimer le participant
    game.removeParticipant(participant);

    // Then
    assertEquals(initialSize, game.getParticipants().size());
    assertFalse(game.getParticipants().contains(participant));
    assertNull(participant.getGame());
  }

  @Test
  @DisplayName("Devrait détecter correctement le statut DRAFTING")
  void shouldDetectDraftingStatus() {
    // Given
    game.setStatus(GameStatus.DRAFTING);

    // When & Then
    assertTrue(game.isDrafting());
    assertFalse(game.isActive());
  }

  @Test
  @DisplayName("Devrait détecter correctement le statut ACTIVE")
  void shouldDetectActiveStatus() {
    // Given
    game.setStatus(GameStatus.ACTIVE);

    // When & Then
    assertTrue(game.isActive());
    assertFalse(game.isDrafting());
  }

  @Test
  @DisplayName("Devrait gérer les statuts terminés")
  void shouldHandleFinishedStatuses() {
    // Given & When
    game.setStatus(GameStatus.FINISHED);

    // Then
    assertFalse(game.isDrafting());
    assertFalse(game.isActive());

    // Given & When
    game.setStatus(GameStatus.CANCELLED);

    // Then
    assertFalse(game.isDrafting());
    assertFalse(game.isActive());
  }
}
