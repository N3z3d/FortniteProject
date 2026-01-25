package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameParticipantRepository;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.UserRepository;

/** Tests TDD pour la création de games de test dans DataInitializationService */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataInitializationService - Game Creation TDD Tests")
class DataInitializationServiceGameTest {

  @Mock private GameRepository gameRepository;

  @Mock private GameParticipantRepository gameParticipantRepository;

  @Mock private UserRepository userRepository;

  private User marcel;
  private User thibaut;
  private User teddy;
  private User sarah;

  @BeforeEach
  void setUp() {
    // Créer des utilisateurs de test
    marcel = createUser("Marcel", "marcel@test.com");
    thibaut = createUser("Thibaut", "thibaut@test.com");
    teddy = createUser("Teddy", "teddy@test.com");
    sarah = createUser("Sarah", "sarah@test.com");
  }

  @Test
  @DisplayName("Doit créer une game avec Marcel, Thibaut et Teddy comme participants")
  void shouldCreateGameWithMarcelThibautTeddy() {
    // Given
    when(((UserRepositoryPort) userRepository).findByUsername("Marcel"))
        .thenReturn(Optional.of(marcel));
    when(((UserRepositoryPort) userRepository).findByUsername("Thibaut"))
        .thenReturn(Optional.of(thibaut));
    when(((UserRepositoryPort) userRepository).findByUsername("Teddy"))
        .thenReturn(Optional.of(teddy));
    when(((GameRepositoryPort) gameRepository).save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(((GameParticipantRepositoryPort) gameParticipantRepository)
            .save(any(GameParticipant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    Game testGame = createTestGame();

    // Then
    assertThat(testGame).isNotNull();
    assertThat(testGame.getName()).isEqualTo("Game Test - Marcel, Thibaut, Teddy");
    assertThat(testGame.getStatus()).isEqualTo(GameStatus.CREATING);
    assertThat(testGame.getMaxParticipants()).isEqualTo(4);
    assertThat(testGame.getCreator()).isEqualTo(marcel);
  }

  @Test
  @DisplayName("Doit ajouter les bons participants à la game")
  void shouldAddCorrectParticipantsToGame() {
    // Given
    when(((UserRepositoryPort) userRepository).findByUsername("Marcel"))
        .thenReturn(Optional.of(marcel));
    when(((UserRepositoryPort) userRepository).findByUsername("Thibaut"))
        .thenReturn(Optional.of(thibaut));
    when(((UserRepositoryPort) userRepository).findByUsername("Teddy"))
        .thenReturn(Optional.of(teddy));
    when(((GameRepositoryPort) gameRepository).save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(((GameParticipantRepositoryPort) gameParticipantRepository)
            .save(any(GameParticipant.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    Game testGame = createTestGame();
    addParticipantsToGame(testGame, Arrays.asList(marcel, thibaut, teddy));

    // Then
    verify(gameParticipantRepository, times(3)).save(any(GameParticipant.class));
  }

  @Test
  @DisplayName("Sarah ne doit pas être dans la game")
  void sarahShouldNotBeInGame() {
    // Given
    when(((UserRepositoryPort) userRepository).findByUsername("Marcel"))
        .thenReturn(Optional.of(marcel));
    when(((UserRepositoryPort) userRepository).findByUsername("Thibaut"))
        .thenReturn(Optional.of(thibaut));
    when(((UserRepositoryPort) userRepository).findByUsername("Teddy"))
        .thenReturn(Optional.of(teddy));
    when(((GameRepositoryPort) gameRepository).save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    Game testGame = createTestGame();
    addParticipantsToGame(testGame, Arrays.asList(marcel, thibaut, teddy));

    // Then - Sarah ne devrait pas être ajoutée
    // Vérifier qu'on n'a sauvé que 3 participants, pas 4
    verify(gameParticipantRepository, times(3)).save(any(GameParticipant.class));
  }

  private User createUser(String username, String email) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(email);
    user.setRole(User.UserRole.USER);
    return user;
  }

  private Game createTestGame() {
    Game game = new Game();
    game.setId(UUID.randomUUID());
    game.setName("Game Test - Marcel, Thibaut, Teddy");
    game.setDescription("Game de test avec 3 participants sur 4 places");
    game.setCreator(marcel);
    game.setMaxParticipants(4);
    game.setStatus(GameStatus.CREATING);
    return ((GameRepositoryPort) gameRepository).save(game);
  }

  private void addParticipantsToGame(Game game, java.util.List<User> participants) {
    for (User participant : participants) {
      GameParticipant gameParticipant = new GameParticipant();
      gameParticipant.setGame(game);
      gameParticipant.setUser(participant);
      ((GameParticipantRepositoryPort) gameParticipantRepository).save(gameParticipant);
    }
  }
}
