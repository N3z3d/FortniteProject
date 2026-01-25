package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.fortnite.pronos.config.SeedProperties;
import com.fortnite.pronos.domain.port.out.GameRepositoryPort;
import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FakeGameSeedServiceTest {

  @Mock private GameRepository gameRepository;
  @Mock private UserRepository userRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private Environment environment;
  @Mock private SeedProperties seedProperties;

  @InjectMocks private FakeGameSeedService fakeGameSeedService;

  @Test
  void skipsSeedWhenDisabled() {
    when(seedProperties.isFakeGameEnabled()).thenReturn(false);

    fakeGameSeedService.seedFakeGame();

    verify(gameRepository, never()).save(any(Game.class));
    verify(teamRepository, never()).saveAll(any());
  }

  @Test
  void skipsSeedOutsideDevProfiles() {
    when(seedProperties.isFakeGameEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});

    fakeGameSeedService.seedFakeGame();

    verify(gameRepository, never()).save(any(Game.class));
    verify(teamRepository, never()).saveAll(any());
  }

  @Test
  void skipsSeedWhenCreatorMissing() {
    when(seedProperties.isFakeGameEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});
    when(userRepository.findByUsername("Thibaut")).thenReturn(Optional.empty());

    fakeGameSeedService.seedFakeGame();

    verify(gameRepository, never()).save(any(Game.class));
    verify(teamRepository, never()).saveAll(any());
  }

  @Test
  void seedsGameAndTeamsForDevProfile() {
    when(seedProperties.isFakeGameEnabled()).thenReturn(true);
    when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

    User creator = buildUser("Thibaut", User.UserRole.USER);
    User teddy = buildUser("Teddy", User.UserRole.USER);
    User marcel = buildUser("Marcel", User.UserRole.USER);

    when(((UserRepositoryPort) userRepository).findByUsername("Thibaut"))
        .thenReturn(Optional.of(creator));
    when(((UserRepositoryPort) userRepository).findByUsername("Teddy"))
        .thenReturn(Optional.of(teddy));
    when(((UserRepositoryPort) userRepository).findByUsername("Marcel"))
        .thenReturn(Optional.of(marcel));
    when(gameRepository.existsByNameAndCreator(eq("Fake League - 4 Players"), eq(creator)))
        .thenReturn(false);

    List<Player> players =
        List.of(buildPlayer("A"), buildPlayer("B"), buildPlayer("C"), buildPlayer("D"));
    when(playerRepository.findAll(PageRequest.of(0, 4, Sort.by("nickname").ascending())))
        .thenReturn(new PageImpl<>(players));

    when(((GameRepositoryPort) gameRepository).save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(teamRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    fakeGameSeedService.seedFakeGame();

    ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
    verify(teamRepository).saveAll(teamsCaptor.capture());
    assertThat(teamsCaptor.getValue()).hasSize(3);
  }

  private User buildUser(String username, User.UserRole role) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(username.toLowerCase() + "@test.com");
    user.setPassword("password");
    user.setRole(role);
    user.setCurrentSeason(2025);
    return user;
  }

  private Player buildPlayer(String nickname) {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setNickname(nickname);
    player.setUsername("user_" + nickname);
    player.setRegion(Player.Region.EU);
    player.setTranche("1");
    player.setCurrentSeason(2025);
    return player;
  }
}
