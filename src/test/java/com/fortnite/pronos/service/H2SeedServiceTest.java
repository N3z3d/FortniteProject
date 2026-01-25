package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
class H2SeedServiceTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private GameRepositoryPort gameRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private Environment environment;

  @InjectMocks private H2SeedService h2SeedService;

  @Captor private ArgumentCaptor<User> userCaptor;
  @Captor private ArgumentCaptor<List<Player>> playersCaptor;
  @Captor private ArgumentCaptor<Game> gameCaptor;

  @BeforeEach
  void setUp() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"h2"});
  }

  @Test
  void doesNotSeedWhenNotH2Profile() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

    h2SeedService.seedTestData();

    verify(userRepository, never()).save(any());
    verify(playerRepository, never()).saveAll(any());
    verify(gameRepository, never()).save(any());
  }

  @Test
  void createsUsersWhenH2Profile() {
    when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              u.setId(java.util.UUID.randomUUID());
              return u;
            });
    when(playerRepository.count()).thenReturn(0L);
    when(gameRepository.count()).thenReturn(1L);

    h2SeedService.seedTestData();

    verify(userRepository, times(4)).save(userCaptor.capture());
    List<User> allCaptured = userCaptor.getAllValues();
    User thibautUser =
        allCaptured.stream().filter(u -> "thibaut".equals(u.getUsername())).findFirst().get();
    assertThat(thibautUser.getEmail()).isEqualTo("thibaut@test.com");
    assertThat(thibautUser.getRole()).isEqualTo(User.UserRole.ADMIN);
  }

  @Test
  void doesNotRecreateExistingUser() {
    User existingUser = new User();
    existingUser.setId(java.util.UUID.randomUUID());
    existingUser.setUsername("thibaut");
    when(userRepository.findByUsername("thibaut")).thenReturn(Optional.of(existingUser));
    when(userRepository.findByUsername("teddy")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("marcel")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("sarah")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              u.setId(java.util.UUID.randomUUID());
              return u;
            });
    when(playerRepository.count()).thenReturn(10L);
    when(gameRepository.count()).thenReturn(1L);

    h2SeedService.seedTestData();

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(3)).save(captor.capture());

    List<String> usernames = captor.getAllValues().stream().map(User::getUsername).toList();
    assertThat(usernames).doesNotContain("thibaut");
    assertThat(usernames).containsExactlyInAnyOrder("teddy", "marcel", "sarah");
  }

  @Test
  void createsPlayersWhenNoneExist() {
    setupBasicMocks();
    when(playerRepository.count()).thenReturn(0L);
    when(gameRepository.count()).thenReturn(1L);

    h2SeedService.seedTestData();

    verify(playerRepository).saveAll(playersCaptor.capture());
    List<Player> players = playersCaptor.getValue();
    assertThat(players).hasSize(20);
    assertThat(players.get(0).getNickname()).isEqualTo("Player1");
    assertThat(players.get(0).getUsername()).isEqualTo("player1");
    assertThat(players.get(0).getCurrentSeason()).isEqualTo(2025);
  }

  @Test
  void doesNotRecreateExistingPlayers() {
    setupBasicMocks();
    when(playerRepository.count()).thenReturn(20L);
    when(gameRepository.count()).thenReturn(1L);

    h2SeedService.seedTestData();

    verify(playerRepository, never()).saveAll(any());
  }

  @Test
  void createsGameWithParticipants() {
    setupBasicMocks();
    when(playerRepository.count()).thenReturn(20L);
    when(gameRepository.count()).thenReturn(0L);
    when(gameRepository.save(any(Game.class)))
        .thenAnswer(
            inv -> {
              Game g = inv.getArgument(0);
              g.setId(java.util.UUID.randomUUID());
              return g;
            });
    when(playerRepository.findAll()).thenReturn(createTestPlayers(20));

    h2SeedService.seedTestData();

    verify(gameRepository).save(gameCaptor.capture());
    Game game = gameCaptor.getValue();
    assertThat(game.getName()).isEqualTo("H2 Test Game");
    assertThat(game.getParticipants()).hasSize(4);
    assertThat(game.getRegionRules()).isNotEmpty();
  }

  @Test
  void assignsDifferentRegionsToPlayers() {
    setupBasicMocks();
    when(playerRepository.count()).thenReturn(0L);
    when(gameRepository.count()).thenReturn(1L);

    h2SeedService.seedTestData();

    verify(playerRepository).saveAll(playersCaptor.capture());
    List<Player> players = playersCaptor.getValue();

    List<Player.Region> regions = players.stream().map(Player::getRegion).distinct().toList();
    assertThat(regions)
        .containsExactlyInAnyOrder(
            Player.Region.EU, Player.Region.NAC, Player.Region.BR, Player.Region.ASIA);
  }

  private void setupBasicMocks() {
    when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              u.setId(java.util.UUID.randomUUID());
              return u;
            });
  }

  private List<Player> createTestPlayers(int count) {
    return java.util.stream.IntStream.rangeClosed(1, count)
        .mapToObj(
            i -> {
              Player p = new Player();
              p.setId(java.util.UUID.randomUUID());
              p.setNickname("Player" + i);
              p.setUsername("player" + i);
              p.setRegion(Player.Region.EU);
              p.setTranche("1-5");
              p.setCurrentSeason(2025);
              return p;
            })
        .toList();
  }
}
