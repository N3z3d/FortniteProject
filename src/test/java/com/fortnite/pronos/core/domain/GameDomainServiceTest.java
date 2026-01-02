package com.fortnite.pronos.core.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.User;

class GameDomainServiceTest {

  private final GameDomainService gameDomainService = new GameDomainService();

  @Test
  void allowsUserAndAdminParticipants() {
    Game game = buildGame(buildUser("creator", User.UserRole.ADMIN));

    User user = buildUser("user", User.UserRole.USER);
    User admin = buildUser("admin", User.UserRole.ADMIN);

    assertThat(gameDomainService.canUserParticipate(user, game)).isTrue();
    assertThat(gameDomainService.canUserParticipate(admin, game)).isTrue();
  }

  @Test
  void rejectsSpectators() {
    Game game = buildGame(buildUser("creator", User.UserRole.ADMIN));
    User spectator = buildUser("spectator", User.UserRole.SPECTATOR);

    assertThat(gameDomainService.canUserParticipate(spectator, game)).isFalse();
  }

  @Test
  void rejectsCreatorAsParticipant() {
    User creator = buildUser("creator", User.UserRole.USER);
    Game game = buildGame(creator);

    assertThat(gameDomainService.canUserParticipate(creator, game)).isFalse();
  }

  private Game buildGame(User creator) {
    return Game.builder()
        .id(UUID.randomUUID())
        .name("Test Game")
        .creator(creator)
        .maxParticipants(4)
        .status(GameStatus.CREATING)
        .participants(new ArrayList<>())
        .regionRules(new ArrayList<>())
        .build();
  }

  private User buildUser(String username, User.UserRole role) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    user.setEmail(username + "@test.com");
    user.setPassword("password");
    user.setRole(role);
    user.setCurrentSeason(2025);
    return user;
  }
}
