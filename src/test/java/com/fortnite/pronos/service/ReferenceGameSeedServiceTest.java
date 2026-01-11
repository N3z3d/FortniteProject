package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.TradeRepository;
import com.fortnite.pronos.repository.UserRepository;
import com.fortnite.pronos.service.seed.SeedDataProviderSelectorService;

@ExtendWith(MockitoExtension.class)
class ReferenceGameSeedServiceTest {

  @Mock private Environment environment;
  @Mock private SeedProperties seedProperties;
  @Mock private CsvDataLoaderService csvDataLoaderService;
  @Mock private SeedDataProviderSelectorService seedDataProviderSelector;
  @Mock private UserRepository userRepository;
  @Mock private GameRepository gameRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TradeRepository tradeRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private ReferenceGameSeedService referenceGameSeedService;

  @Test
  void skipsWhenSeedDisabled() {
    when(seedProperties.isEnabled()).thenReturn(false);

    referenceGameSeedService.ensureReferenceGame();

    verify(gameRepository, never()).save(any(Game.class));
    verify(teamRepository, never()).saveAll(any());
  }

  @Test
  void skipsWhenNotDevProfile() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

    referenceGameSeedService.ensureReferenceGame();

    verify(gameRepository, never()).save(any(Game.class));
    verify(teamRepository, never()).saveAll(any());
  }

  @Test
  void seedsReferenceGameWithTeams() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(seedProperties.isResetMode()).thenReturn(false);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

    Map<String, User> users = buildUsers();
    when(userRepository.findByUsernameIgnoreCase(anyString()))
        .thenAnswer(invocation -> Optional.of(users.get(invocation.getArgument(0))));
    when(playerRepository.findAll()).thenReturn(List.of());
    when(gameRepository.existsByNameAndCreator(anyString(), any(User.class))).thenReturn(false);
    when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(teamRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(seedDataProviderSelector.loadSeedData()).thenReturn(buildSeedData());

    referenceGameSeedService.ensureReferenceGame();

    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());
    assertThat(teamsCaptor.getValue()).hasSize(3);
    verify(gameRepository).save(any(Game.class));
  }

  @Test
  void resetModeClearsDataBeforeSeed() {
    when(seedProperties.isEnabled()).thenReturn(true);
    when(seedProperties.isResetMode()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

    User thibaut = buildUser("Thibaut");
    when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(thibaut));
    when(gameRepository.existsByNameAndCreator(anyString(), any(User.class))).thenReturn(true);

    referenceGameSeedService.ensureReferenceGame();

    verify(tradeRepository).deleteAll();
    verify(teamRepository).deleteAll();
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
