package com.fortnite.pronos.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests TDD pour le modèle Game Valide les méthodes ajoutées et les comportements attendus */
@DisplayName("Tests TDD - Modèle Game")
class GameModelTest {

  private Game game;
  private User creator;
  private GameParticipant participant1;
  private GameParticipant participant2;

  @BeforeEach
  void setUp() {
    // Créer un utilisateur créateur
    creator = new User();
    creator.setId(UUID.randomUUID());
    creator.setUsername("TestCreator");
    creator.setEmail("creator@test.com");
    creator.setPassword("password123"); // Ajout du mot de passe requis

    // Créer une game de test
    game =
        Game.builder()
            .id(UUID.randomUUID())
            .name("Test Game")
            .creator(creator)
            .maxParticipants(10)
            .status(GameStatus.CREATING)
            .createdAt(LocalDateTime.now())
            .build();

    // Créer des participants de test
    User user1 = new User();
    user1.setId(UUID.randomUUID());
    user1.setUsername("User1");
    user1.setEmail("user1@test.com");
    user1.setPassword("password123");

    User user2 = new User();
    user2.setId(UUID.randomUUID());
    user2.setUsername("User2");
    user2.setEmail("user2@test.com");
    user2.setPassword("password123");

    participant1 =
        GameParticipant.builder()
            .id(UUID.randomUUID())
            .game(game)
            .user(user1)
            .draftOrder(1)
            .build();

    participant2 =
        GameParticipant.builder()
            .id(UUID.randomUUID())
            .game(game)
            .user(user2)
            .draftOrder(2)
            .build();
  }

  @Test
  @DisplayName("Devrait retourner le bon nombre de participants")
  void shouldReturnCorrectParticipantCount() {
    // Given: Game sans participants
    assertThat(game.getParticipantCount()).isEqualTo(0);

    // When: Ajout de participants
    game.addParticipant(participant1);
    game.addParticipant(participant2);

    // Then: Nombre correct de participants
    assertThat(game.getParticipantCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("Devrait accepter de nouveaux participants quand pas plein")
  void shouldAcceptNewParticipantsWhenNotFull() {
    // Given: Game avec 2 participants sur 10 max
    game.addParticipant(participant1);
    game.addParticipant(participant2);

    // When: Vérification si peut accepter
    boolean canAccept = game.canAcceptParticipants();

    // Then: Peut accepter
    assertThat(canAccept).isTrue();
    assertThat(game.getParticipantCount()).isEqualTo(2);
    assertThat(game.getMaxParticipants()).isEqualTo(10);
  }

  @Test
  @DisplayName("Ne devrait pas accepter de nouveaux participants quand plein")
  void shouldNotAcceptNewParticipantsWhenFull() {
    // Given: Game avec 10 participants (pleine)
    for (int i = 0; i < 10; i++) {
      User user = new User();
      user.setId(UUID.randomUUID());
      user.setUsername("User" + i);

      GameParticipant participant =
          GameParticipant.builder()
              .id(UUID.randomUUID())
              .game(game)
              .user(user)
              .draftOrder(i + 1)
              .build();

      game.addParticipant(participant);
    }

    // When: Vérification si peut accepter
    boolean canAccept = game.canAcceptParticipants();

    // Then: Ne peut pas accepter
    assertThat(canAccept).isFalse();
    assertThat(game.getParticipantCount()).isEqualTo(10);
  }

  @Test
  @DisplayName("Devrait détecter correctement l'état de draft")
  void shouldDetectDraftingState() {
    // Given: Game en création
    assertThat(game.isDrafting()).isFalse();

    // When: Changement vers DRAFTING
    game.setStatus(GameStatus.DRAFTING);

    // Then: État de draft détecté
    assertThat(game.isDrafting()).isTrue();
  }

  @Test
  @DisplayName("Devrait détecter correctement l'état actif")
  void shouldDetectActiveState() {
    // Given: Game en création
    assertThat(game.isActive()).isFalse();

    // When: Changement vers ACTIVE
    game.setStatus(GameStatus.ACTIVE);

    // Then: État actif détecté
    assertThat(game.isActive()).isTrue();
  }

  @Test
  @DisplayName("Devrait gérer correctement l'ajout et suppression de participants")
  void shouldHandleParticipantAdditionAndRemoval() {
    // Given: Game vide
    assertThat(game.getParticipantCount()).isEqualTo(0);

    // When: Ajout d'un participant
    game.addParticipant(participant1);

    // Then: Participant ajouté
    assertThat(game.getParticipantCount()).isEqualTo(1);
    assertThat(game.getParticipants()).contains(participant1);

    // When: Suppression du participant
    game.removeParticipant(participant1);

    // Then: Participant supprimé
    assertThat(game.getParticipantCount()).isEqualTo(0);
    assertThat(game.getParticipants()).doesNotContain(participant1);
  }

  @Test
  @DisplayName("Devrait gérer correctement les règles régionales")
  void shouldHandleRegionRules() {
    // Given: Game sans règles régionales
    assertThat(game.getRegionRules()).isEmpty();

    // When: Ajout d'une règle régionale
    GameRegionRule rule = new GameRegionRule();
    rule.setId(UUID.randomUUID());
    rule.setRegion(Player.Region.EU);
    rule.setMaxPlayers(5);

    game.addRegionRule(rule);

    // Then: Règle ajoutée
    assertThat(game.getRegionRules()).hasSize(1);
    assertThat(game.getRegionRules()).contains(rule);
    assertThat(rule.getGame()).isEqualTo(game);

    // When: Suppression de la règle
    game.removeRegionRule(rule);

    // Then: Règle supprimée
    assertThat(game.getRegionRules()).isEmpty();
    assertThat(rule.getGame()).isNull();
  }

  @Test
  @DisplayName("Devrait maintenir la cohérence des données")
  void shouldMaintainDataConsistency() {
    // Given: Game avec données de base
    assertThat(game.getName()).isEqualTo("Test Game");
    assertThat(game.getCreator()).isEqualTo(creator);
    assertThat(game.getStatus()).isEqualTo(GameStatus.CREATING);

    // When: Modification des données
    game.setName("Updated Game");
    game.setStatus(GameStatus.DRAFTING);

    // Then: Données mises à jour
    assertThat(game.getName()).isEqualTo("Updated Game");
    assertThat(game.getStatus()).isEqualTo(GameStatus.DRAFTING);
    assertThat(game.getCreator()).isEqualTo(creator); // Inchangé
  }
}
