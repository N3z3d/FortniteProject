package com.fortnite.pronos.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.service.seed.PlayerSeedService;
import com.fortnite.pronos.service.seed.ReferenceUserSeedService;
import com.fortnite.pronos.service.seed.TeamSeedService;

/** Unit tests for ReferenceGameSeedService. Tests seed configuration behavior and game creation. */
@ExtendWith(MockitoExtension.class)
class ReferenceGameSeedServiceTest {

  // Configuration dependencies
  @Mock private Environment environment;
  @Mock private SeedProperties seedProperties;

  // Seed service dependencies
  @Mock private ReferenceUserSeedService referenceUserSeedService;
  @Mock private PlayerSeedService playerSeedService;
  @Mock private TeamSeedService teamSeedService;
  @Mock private CsvDataLoaderService csvDataLoaderService;

  // Repository
  @Mock private GameRepository gameRepository;

  @InjectMocks private ReferenceGameSeedService referenceGameSeedService;

  @Test
  void skipsWhenSeedDisabled() {
    when(seedProperties.isEnabled()).thenReturn(false);

    referenceGameSeedService.ensureReferenceGame();

    verify(gameRepository, never()).save(any(Game.class));
  }

  @Test
  void skipsWhenNotDevProfile() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

    referenceGameSeedService.ensureReferenceGame();

    verify(gameRepository, never()).save(any(Game.class));
  }

  @Test
  void seedsReferenceGameWithTeams() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(seedProperties.isResetMode()).thenReturn(false);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
    // Use mock provider instead of csv
    when(environment.getProperty("fortnite.data.provider", "csv")).thenReturn("mock");

    Map<String, User> users = buildUsers();
    when(referenceUserSeedService.ensureReferenceUsers()).thenReturn(users);

    // Mock players in database (needed for resolveAssignments)
    List<Player> persistedPlayers =
        List.of(
            buildPlayer("alpha"), buildPlayer("beta"), buildPlayer("gamma"), buildPlayer("delta"));
    when(playerSeedService.getAllPlayers()).thenReturn(persistedPlayers);
    when(playerSeedService.loadSeedData()).thenReturn(buildSeedData());

    when(gameRepository.existsByNameAndCreator(anyString(), any(User.class))).thenReturn(false);
    when(gameRepository.count()).thenReturn(0L);
    when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

    referenceGameSeedService.ensureReferenceGame();

    verify(gameRepository).save(any(Game.class));
    verify(teamSeedService).getAllTeams();
  }

  @Test
  void resetModeClearsDataBeforeSeed() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(seedProperties.isResetMode()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

    Map<String, User> users = buildUsers();
    when(referenceUserSeedService.ensureReferenceUsers()).thenReturn(users);
    when(gameRepository.existsByNameAndCreator(anyString(), any(User.class))).thenReturn(true);

    referenceGameSeedService.ensureReferenceGame();

    // Refactored service only clears games (teams/trades cascade via JPA)
    verify(gameRepository).deleteAll();
  }

  private Map<String, User> buildUsers() {
    return Map.of(
        "Thibaut", buildUser("Thibaut"),
        "Teddy", buildUser("Teddy"),
        "Marcel", buildUser("Marcel"));
  }

  private MockDataGeneratorService.MockDataSet buildSeedData() {
    return new MockDataGeneratorService.MockDataSet(
        Map.of(
            "Thibaut",
                List.of(buildSeedEntry("Thibaut", "alpha"), buildSeedEntry("Thibaut", "beta")),
            "Teddy", List.of(buildSeedEntry("Teddy", "gamma")),
            "Marcel", List.of(buildSeedEntry("Marcel", "delta"))),
        4);
  }

  private MockDataGeneratorService.PlayerWithScore buildSeedEntry(
      String pronostiqueur, String nickname) {
    Player player = buildPlayer(nickname);
    Score score = new Score();
    score.setPlayer(player);
    score.setPoints(100);
    score.setSeason(2025);
    score.setDate(LocalDate.now());
    score.setTimestamp(OffsetDateTime.now());
    return new MockDataGeneratorService.PlayerWithScore(pronostiqueur, player, score, 1);
  }

  private User buildUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(username.toLowerCase() + "@test.com");
    user.setPassword("password");
    user.setRole(User.UserRole.USER);
    user.setCurrentSeason(2025);
    return user;
  }

  private Player buildPlayer(String nickname) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setNickname(nickname);
    player.setUsername(nickname);
    player.setRegion(Player.Region.EU);
    player.setTranche("1-5");
    return player;
  }
}
