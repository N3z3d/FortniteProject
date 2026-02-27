package com.fortnite.pronos.service.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
class GameSeedServiceTest {

  @Mock private GameRepositoryPort gameRepository;

  private GameSeedService gameSeedService;

  @BeforeEach
  void setUp() {
    gameSeedService = new GameSeedService(gameRepository);
  }

  @Test
  void createTestGamesWithRealTeams_skipsWhenNotEnoughParticipants() {
    List<User> users = List.of(createUser("Thibaut"), createUser("Teddy"));
    List<Team> teams = List.of(new Team(), new Team(), new Team());

    gameSeedService.createTestGamesWithRealTeams(users, teams);

    verify(gameRepository, never()).save(any(Game.class));
  }

  @Test
  void createTestGamesWithRealTeams_savesActiveGameWithThreeParticipants() {
    List<User> users = List.of(createUser("Thibaut"), createUser("Teddy"), createUser("Marcel"));
    List<Team> teams = List.of(new Team(), new Team(), new Team());
    when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

    gameSeedService.createTestGamesWithRealTeams(users, teams);

    ArgumentCaptor<Game> savedGameCaptor = ArgumentCaptor.forClass(Game.class);
    verify(gameRepository).save(savedGameCaptor.capture());

    Game savedGame = savedGameCaptor.getValue();
    assertThat(savedGame.getStatus()).isEqualTo(GameStatus.ACTIVE);
    assertThat(savedGame.getParticipants()).hasSize(3);
    assertThat(savedGame.getRegionRules()).hasSize(3);
  }

  @Test
  void createTestGames_savesThreeGamesWhenEnoughParticipants() {
    List<User> users = List.of(createUser("Thibaut"), createUser("Teddy"), createUser("Marcel"));
    when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(gameRepository.count()).thenReturn(3L);

    gameSeedService.createTestGames(users);

    verify(gameRepository, times(3)).save(any(Game.class));
  }

  @Test
  void createGame_addsExpectedRegionRules() {
    Game game =
        gameSeedService.createGame(
            "Test game", "Description", createUser("Thibaut"), 3, GameStatus.ACTIVE);

    assertThat(game.getRegionRules()).hasSize(3);
    assertThat(game.getRegionRules()).allMatch(regionRule -> regionRule.getMaxPlayers() == 2);
  }

  private User createUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setRole(User.UserRole.USER);
    user.setEmail(username.toLowerCase() + "@test.com");
    user.setPassword("password");
    return user;
  }
}
