package com.fortnite.pronos.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.JoinTable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests TDD pour le modèle GameParticipant Valide les méthodes ajoutées et les comportements
 * attendus
 */
@DisplayName("Tests TDD - Modèle GameParticipant")
class GameParticipantModelTest {

  private GameParticipant participant;
  private Game game;
  private User user;
  private Player player1;
  private Player player2;

  @BeforeEach
  void setUp() {
    // Créer une game de test
    User creator = new User();
    creator.setId(UUID.randomUUID());
    creator.setUsername("Creator");

    game =
        Game.builder()
            .id(UUID.randomUUID())
            .name("Test Game")
            .creator(creator)
            .maxParticipants(10)
            .status(GameStatus.CREATING)
            .build();

    // Créer un utilisateur participant
    user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername("TestUser");
    user.setEmail("user@test.com");

    // Créer des joueurs de test
    player1 = new Player();
    player1.setId(UUID.randomUUID());
    player1.setNickname("Player1");
    player1.setRegion(Player.Region.EU);
    player1.setUsername("player1");
    player1.setTranche("1");

    player2 = new Player();
    player2.setId(UUID.randomUUID());
    player2.setNickname("Player2");
    player2.setRegion(Player.Region.NAC);
    player2.setUsername("player2");
    player2.setTranche("1");

    // Créer un participant de test
    participant =
        GameParticipant.builder()
            .id(UUID.randomUUID())
            .game(game)
            .user(user)
            .draftOrder(1)
            .lastSelectionTime(LocalDateTime.now())
            .build();
  }

  @Test
  @DisplayName("Devrait retourner le nom d'utilisateur correct")
  void shouldReturnCorrectUsername() {
    // When: Récupération du nom d'utilisateur
    String username = participant.getUsername();

    // Then: Nom d'utilisateur correct
    assertThat(username).isEqualTo("TestUser");
  }

  @Test
  @DisplayName("Devrait retourner null si l'utilisateur est null")
  void shouldReturnNullWhenUserIsNull() {
    // Given: Participant sans utilisateur
    participant.setUser(null);

    // When: Récupération du nom d'utilisateur
    String username = participant.getUsername();

    // Then: Null retourné
    assertThat(username).isNull();
  }

  @Test
  @DisplayName("Devrait ajouter un joueur à la sélection")
  void shouldAddPlayerToSelection() {
    // Given: Participant sans joueurs sélectionnés
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(0);

    // When: Ajout d'un joueur
    participant.addSelectedPlayer(player1);

    // Then: Joueur ajouté
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(1);
    assertThat(participant.getSelectedPlayers()).contains(player1);
  }

  @Test
  @DisplayName("Ne devrait pas ajouter le même joueur deux fois")
  void shouldNotAddSamePlayerTwice() {
    // Given: Participant avec un joueur déjà sélectionné
    participant.addSelectedPlayer(player1);
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(1);

    // When: Ajout du même joueur
    participant.addSelectedPlayer(player1);

    // Then: Joueur non ajouté en double
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(1);
    assertThat(participant.getSelectedPlayers()).containsExactly(player1);
  }

  @Test
  @DisplayName("Devrait supprimer un joueur de la sélection")
  void shouldRemovePlayerFromSelection() {
    // Given: Participant avec des joueurs sélectionnés
    participant.addSelectedPlayer(player1);
    participant.addSelectedPlayer(player2);
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(2);

    // When: Suppression d'un joueur
    participant.removeSelectedPlayer(player1);

    // Then: Joueur supprimé
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(1);
    assertThat(participant.getSelectedPlayers()).contains(player2);
    assertThat(participant.getSelectedPlayers()).doesNotContain(player1);
  }

  @Test
  @DisplayName("Devrait vérifier si un joueur est sélectionné")
  void shouldCheckIfPlayerIsSelected() {
    // Given: Participant avec un joueur sélectionné
    participant.addSelectedPlayer(player1);

    // When: Vérification de la sélection
    boolean hasPlayer1 = participant.hasSelectedPlayer(player1);
    boolean hasPlayer2 = participant.hasSelectedPlayer(player2);

    // Then: Résultats corrects
    assertThat(hasPlayer1).isTrue();
    assertThat(hasPlayer2).isFalse();
  }

  @Test
  @DisplayName("Devrait retourner le bon nombre de joueurs sélectionnés")
  void shouldReturnCorrectSelectedPlayersCount() {
    // Given: Participant sans joueurs
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(0);

    // When: Ajout de joueurs
    participant.addSelectedPlayer(player1);
    participant.addSelectedPlayer(player2);

    // Then: Nombre correct
    assertThat(participant.getSelectedPlayersCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("Devrait mettre à jour le temps de dernière sélection")
  void shouldUpdateLastSelectionTime() {
    // Given: Temps initial
    LocalDateTime initialTime = participant.getLastSelectionTime();

    // When: Mise à jour du temps (avec petit délai pour garantir une différence)
    try {
      Thread.sleep(1); // 1ms delay to ensure different timestamp
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    participant.updateLastSelectionTime();

    // Then: Temps mis à jour
    assertThat(participant.getLastSelectionTime()).isAfter(initialTime);
  }

  @Test
  @DisplayName("Ne devrait pas détecter de timeout si pas de sélection")
  void shouldNotDetectTimeoutWhenNoSelection() {
    // Given: Participant sans temps de sélection
    participant.setLastSelectionTime(null);

    // When: Vérification du timeout
    boolean hasTimedOut = participant.hasTimedOut();

    // Then: Pas de timeout
    assertThat(hasTimedOut).isFalse();
  }

  @Test
  @DisplayName("Ne devrait pas détecter de timeout si sélection récente")
  void shouldNotDetectTimeoutWhenRecentSelection() {
    // Given: Sélection récente (maintenant)
    participant.setLastSelectionTime(LocalDateTime.now());

    // When: Vérification du timeout
    boolean hasTimedOut = participant.hasTimedOut();

    // Then: Pas de timeout
    assertThat(hasTimedOut).isFalse();
  }

  @Test
  @DisplayName("Devrait détecter le timeout après 12h")
  void shouldDetectTimeoutAfter12Hours() {
    // Given: Sélection ancienne (il y a 13h)
    LocalDateTime oldTime = LocalDateTime.now().minusHours(13);
    participant.setLastSelectionTime(oldTime);

    // When: Vérification du timeout
    boolean hasTimedOut = participant.hasTimedOut();

    // Then: Timeout détecté
    assertThat(hasTimedOut).isTrue();
  }

  @Test
  @DisplayName("Devrait maintenir la cohérence des données")
  void shouldMaintainDataConsistency() {
    // Given: Participant avec données de base
    assertThat(participant.getGame()).isEqualTo(game);
    assertThat(participant.getUser()).isEqualTo(user);
    assertThat(participant.getDraftOrder()).isEqualTo(1);

    // When: Modification des données
    participant.setDraftOrder(3);

    // Then: Données mises à jour
    assertThat(participant.getDraftOrder()).isEqualTo(3);
    assertThat(participant.getGame()).isEqualTo(game); // Inchangé
    assertThat(participant.getUser()).isEqualTo(user); // Inchangé
  }

  @Test
  @DisplayName("Devrait gérer correctement les relations bidirectionnelles")
  void shouldHandleBidirectionalRelationships() {
    // Given: Participant avec joueurs sélectionnés
    participant.addSelectedPlayer(player1);
    participant.addSelectedPlayer(player2);

    // When: Vérification des relations
    boolean hasPlayer1 = participant.hasSelectedPlayer(player1);
    boolean hasPlayer2 = participant.hasSelectedPlayer(player2);

    // Then: Relations correctes
    assertThat(hasPlayer1).isTrue();
    assertThat(hasPlayer2).isTrue();
    assertThat(participant.getSelectedPlayers()).hasSize(2);
    assertThat(participant.getSelectedPlayers()).contains(player1, player2);
  }

  @Test
  @DisplayName("Devrait mapper la table game_participant_players avec game_participant_id")
  void shouldMapJoinTableWithGameParticipantId() throws NoSuchFieldException {
    Field field = GameParticipant.class.getDeclaredField("selectedPlayers");
    JoinTable joinTable = field.getAnnotation(JoinTable.class);

    assertThat(joinTable).isNotNull();
    assertThat(joinTable.name()).isEqualTo("game_participant_players");
    assertThat(joinTable.joinColumns()).hasSize(1);
    assertThat(joinTable.joinColumns()[0].name()).isEqualTo("game_participant_id");
    assertThat(joinTable.inverseJoinColumns()).hasSize(1);
    assertThat(joinTable.inverseJoinColumns()[0].name()).isEqualTo("player_id");
  }
}
