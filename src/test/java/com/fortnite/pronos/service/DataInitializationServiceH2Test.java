package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Tests TDD pour DataInitializationService Focus sur la correction des erreurs de création de games
 * avec H2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataInitializationService TDD - H2 Game Creation Fix")
class DataInitializationServiceH2Test {

  @Mock private UserRepository userRepository;
  @Mock private GameRepository gameRepository;
  @Mock private GameParticipantRepository gameParticipantRepository;
  @Mock private Environment environment;
  @Mock private PasswordEncoder passwordEncoder;

  private User marcel, thibaut, teddy;

  @BeforeEach
  void setUp() {
    // Mock environment pour être en mode dev avec lenient
    lenient().when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

    // Mock des utilisateurs
    marcel = createUser("Marcel", "marcel@test.com");
    thibaut = createUser("Thibaut", "thibaut@test.com");
    teddy = createUser("Teddy", "teddy@test.com");

    lenient().when(userRepository.findByUsername("Marcel")).thenReturn(Optional.of(marcel));
    lenient().when(userRepository.findByUsername("Thibaut")).thenReturn(Optional.of(thibaut));
    lenient().when(userRepository.findByUsername("Teddy")).thenReturn(Optional.of(teddy));

    // Mock des saves avec lenient
    lenient()
        .when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(gameRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(gameParticipantRepository.save(any(GameParticipant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");

    // Mock pour éviter l'initialisation complète
    lenient().when(userRepository.count()).thenReturn(3L); // Simuler que les users existent déjà
  }

  @Test
  @DisplayName("Doit créer une game sans erreur avec Builder pattern")
  void shouldCreateGameWithoutErrorUsingBuilder() {
    // When
    Game testGame = createGameUsingBuilder();

    // Then
    assertThat(testGame).isNotNull();
    assertThat(testGame.getName()).isEqualTo("Fantasy League Pro 2025");
    assertThat(testGame.getCreator()).isEqualTo(thibaut);
    assertThat(testGame.getStatus()).isEqualTo(GameStatus.ACTIVE);
    assertThat(testGame.getMaxParticipants()).isEqualTo(3);
    assertThat(testGame.getParticipants()).isNotNull();
    assertThat(testGame.getRegionRules()).isNotNull();
  }

  @Test
  @DisplayName("Doit créer un GameParticipant sans erreur avec Builder pattern")
  void shouldCreateGameParticipantWithoutErrorUsingBuilder() {
    // Given
    Game game = createGameUsingBuilder();

    // When
    GameParticipant participant = createGameParticipantUsingBuilder(game, thibaut, 1);

    // Then
    assertThat(participant).isNotNull();
    assertThat(participant.getGame()).isEqualTo(game);
    assertThat(participant.getUser()).isEqualTo(thibaut);
    assertThat(participant.getDraftOrder()).isEqualTo(1);
    assertThat(participant.getSelectedPlayers()).isNotNull();
    assertThat(participant.getSelectedPlayers()).isEmpty();
  }

  @Test
  @DisplayName("Doit valider la structure des relations Game-GameParticipant")
  void shouldValidateGameParticipantRelationship() {
    // Given
    Game game = createGameUsingBuilder();
    GameParticipant participant1 = createGameParticipantUsingBuilder(game, thibaut, 1);
    GameParticipant participant2 = createGameParticipantUsingBuilder(game, marcel, 2);
    GameParticipant participant3 = createGameParticipantUsingBuilder(game, teddy, 3);

    // When - Ajouter les participants à la game
    game.addParticipant(participant1);
    game.addParticipant(participant2);
    game.addParticipant(participant3);

    // Then
    assertThat(game.getParticipants()).hasSize(3);
    assertThat(game.getParticipants()).contains(participant1, participant2, participant3);

    // Vérifier les back-references
    assertThat(participant1.getGame()).isEqualTo(game);
    assertThat(participant2.getGame()).isEqualTo(game);
    assertThat(participant3.getGame()).isEqualTo(game);
  }

  /** Clean Code: Méthode utilitaire pour créer un utilisateur */
  private User createUser(String username, String email) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setRole(User.UserRole.USER);
    user.setPassword("$2a$10$encodedPassword");
    return user;
  }

  /**
   * Clean Code TDD: Méthode pour créer une Game avec le Builder pattern Ceci devrait remplacer la
   * création manuelle dans DataInitializationService
   */
  private Game createGameUsingBuilder() {
    return Game.builder()
        .name("Fantasy League Pro 2025")
        .description("Championnat principal avec Thibaut, Teddy et Marcel - Saison 2025")
        .creator(thibaut)
        .maxParticipants(3)
        .status(GameStatus.ACTIVE)
        .participants(new ArrayList<>()) // Initialiser la liste
        .regionRules(new ArrayList<>()) // Initialiser la liste
        .build();
  }

  /** Clean Code TDD: Méthode pour créer un GameParticipant avec le Builder pattern */
  private GameParticipant createGameParticipantUsingBuilder(Game game, User user, int draftOrder) {
    return GameParticipant.builder()
        .game(game)
        .user(user)
        .draftOrder(draftOrder)
        .selectedPlayers(new ArrayList<>()) // Initialiser la liste
        .build();
  }
}
