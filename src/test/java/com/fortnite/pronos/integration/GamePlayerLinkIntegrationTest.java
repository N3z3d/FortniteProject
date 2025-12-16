package com.fortnite.pronos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

/**
 * Tests d'intégration TDD pour valider la liaison joueurs-pronostiqueurs dans la game Valide que
 * les 147 joueurs sont correctement liés aux pronostiqueurs
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'Intégration TDD - Liaison Joueurs-Pronostiqueurs")
class GamePlayerLinkIntegrationTest {

  @Autowired private GameRepository gameRepository;

  @Autowired private GameParticipantRepository gameParticipantRepository;

  @Autowired private PlayerRepository playerRepository;

  @Autowired private UserRepository userRepository;

  private Game testGame;
  private User thibaut;
  private User marcel;
  private User teddy;

  @BeforeEach
  void setUp() {
    // Récupérer la game de test
    testGame =
        gameRepository
            .findById(UUID.fromString("880e8400-e29b-41d4-a716-446655440000"))
            .orElse(null);

    // Récupérer les utilisateurs
    thibaut =
        userRepository
            .findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
            .orElse(null);

    marcel =
        userRepository
            .findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440002"))
            .orElse(null);

    teddy =
        userRepository
            .findById(UUID.fromString("550e8400-e29b-41d4-a716-446655440003"))
            .orElse(null);
  }

  @Test
  @DisplayName("Devrait avoir 147 joueurs dans la base de données")
  void shouldHave147PlayersInDatabase() {
    // When: Compter tous les joueurs
    long totalPlayers = playerRepository.count();

    // Then: 147 joueurs présents
    assertThat(totalPlayers).isEqualTo(147);
  }

  @Test
  @DisplayName("Devrait avoir 3 participants dans la game de test")
  void shouldHave3ParticipantsInTestGame() {
    // Given: Game de test
    assertThat(testGame).isNotNull();

    // When: Récupérer les participants
    List<GameParticipant> participants =
        gameParticipantRepository.findByGameOrderByDraftOrderAsc(testGame);

    // Then: 3 participants
    assertThat(participants).hasSize(3);
    assertThat(participants.get(0).getUser().getUsername()).isEqualTo("Thibaut");
    assertThat(participants.get(1).getUser().getUsername()).isEqualTo("Marcel");
    assertThat(participants.get(2).getUser().getUsername()).isEqualTo("Teddy");
  }

  @Test
  @DisplayName("Devrait avoir les pronostiqueurs avec leurs rôles corrects")
  void shouldHavePronostiqueursWithCorrectRoles() {
    // Then: Rôles corrects
    assertThat(thibaut).isNotNull();
    assertThat(thibaut.getRole()).isEqualTo(User.UserRole.USER);

    assertThat(marcel).isNotNull();
    assertThat(marcel.getRole()).isEqualTo(User.UserRole.USER);

    assertThat(teddy).isNotNull();
    assertThat(teddy.getRole()).isEqualTo(User.UserRole.USER);
  }

  @Test
  @DisplayName("Devrait avoir des joueurs répartis par région")
  void shouldHavePlayersDistributedByRegion() {
    // When: Récupérer tous les joueurs
    List<Player> players = playerRepository.findAll();

    // Then: Joueurs répartis par région
    assertThat(players).isNotEmpty();

    // Vérifier que chaque région a des joueurs
    long euCount = players.stream().filter(p -> p.getRegion() == Player.Region.EU).count();
    long nacCount = players.stream().filter(p -> p.getRegion() == Player.Region.NAC).count();
    long brCount = players.stream().filter(p -> p.getRegion() == Player.Region.BR).count();

    assertThat(euCount).isGreaterThan(0);
    assertThat(nacCount).isGreaterThan(0);
    assertThat(brCount).isGreaterThan(0);
  }

  @Test
  @DisplayName("Devrait avoir des joueurs avec des usernames uniques")
  void shouldHavePlayersWithUniqueUsernames() {
    // When: Récupérer tous les joueurs
    List<Player> players = playerRepository.findAll();

    // Then: Usernames uniques
    long uniqueUsernames = players.stream().map(Player::getUsername).distinct().count();

    assertThat(uniqueUsernames).isEqualTo(147);
  }

  @Test
  @DisplayName("Devrait avoir des joueurs avec des nicknames")
  void shouldHavePlayersWithNicknames() {
    // When: Récupérer tous les joueurs
    List<Player> players = playerRepository.findAll();

    // Then: Tous ont des nicknames
    assertThat(players)
        .allMatch(player -> player.getNickname() != null && !player.getNickname().trim().isEmpty());
  }

  @Test
  @DisplayName("Devrait avoir des joueurs avec des régions valides")
  void shouldHavePlayersWithValidRegions() {
    // When: Récupérer tous les joueurs
    List<Player> players = playerRepository.findAll();

    // Then: Régions valides
    assertThat(players)
        .allMatch(
            player ->
                player.getRegion() != null
                    && (player.getRegion() == Player.Region.EU
                        || player.getRegion() == Player.Region.NAC
                        || player.getRegion() == Player.Region.BR
                        || player.getRegion() == Player.Region.ASIA
                        || player.getRegion() == Player.Region.OCE
                        || player.getRegion() == Player.Region.NAW
                        || player.getRegion() == Player.Region.ME));
  }

  @Test
  @DisplayName("Devrait avoir des joueurs avec des tranches valides")
  void shouldHavePlayersWithValidTranches() {
    // When: Récupérer tous les joueurs
    List<Player> players = playerRepository.findAll();

    // Then: Tranches valides
    assertThat(players)
        .allMatch(player -> player.getTranche() != null && !player.getTranche().trim().isEmpty());
  }

  @Test
  @DisplayName("Devrait avoir des joueurs avec des saisons valides")
  void shouldHavePlayersWithValidSeasons() {
    // When: Récupérer tous les joueurs
    List<Player> players = playerRepository.findAll();

    // Then: Saisons valides
    assertThat(players)
        .allMatch(player -> player.getCurrentSeason() != null && player.getCurrentSeason() > 0);
  }

  @Test
  @DisplayName("Devrait maintenir la cohérence des données")
  void shouldMaintainDataConsistency() {
    // Given: Données de base
    assertThat(testGame).isNotNull();
    assertThat(thibaut).isNotNull();
    assertThat(marcel).isNotNull();
    assertThat(teddy).isNotNull();

    // When: Vérifier les relations
    List<GameParticipant> participants =
        gameParticipantRepository.findByGameOrderByDraftOrderAsc(testGame);

    // Then: Cohérence maintenue
    assertThat(participants).hasSize(3);
    assertThat(participants.get(0).getGame()).isEqualTo(testGame);
    assertThat(participants.get(1).getGame()).isEqualTo(testGame);
    assertThat(participants.get(2).getGame()).isEqualTo(testGame);

    assertThat(participants.get(0).getUser()).isEqualTo(thibaut);
    assertThat(participants.get(1).getUser()).isEqualTo(marcel);
    assertThat(participants.get(2).getUser()).isEqualTo(teddy);
  }
}
