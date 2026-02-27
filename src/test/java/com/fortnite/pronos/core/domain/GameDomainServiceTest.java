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

  @Test
  void calculatesMinimumParticipantsUsingHalfOrTwo() {
    Game smallGame = buildGame(buildUser("creator", User.UserRole.ADMIN));
    smallGame.setMaxParticipants(3);

    Game biggerGame = buildGame(buildUser("creator2", User.UserRole.ADMIN));
    biggerGame.setMaxParticipants(10);

    assertThat(gameDomainService.calculateMinimumParticipants(smallGame)).isEqualTo(2);
    assertThat(gameDomainService.calculateMinimumParticipants(biggerGame)).isEqualTo(5);
  }

  @Test
  void validatesGameConfigurationRules() {
    Game validGame = buildGame(buildUser("creator", User.UserRole.ADMIN));
    validGame.setMaxParticipants(50);
    validGame.setName("Valid name");

    Game tooSmall = buildGame(buildUser("creator2", User.UserRole.ADMIN));
    tooSmall.setMaxParticipants(1);

    Game tooLarge = buildGame(buildUser("creator3", User.UserRole.ADMIN));
    tooLarge.setMaxParticipants(51);

    Game blankName = buildGame(buildUser("creator4", User.UserRole.ADMIN));
    blankName.setName(" ");

    assertThat(gameDomainService.isValidGameConfiguration(validGame)).isTrue();
    assertThat(gameDomainService.isValidGameConfiguration(tooSmall)).isFalse();
    assertThat(gameDomainService.isValidGameConfiguration(tooLarge)).isFalse();
    assertThat(gameDomainService.isValidGameConfiguration(blankName)).isFalse();
  }

  @Test
  void keepsCurrentStatusForCreatingDraftingAndActive() {
    Game creating = buildGame(buildUser("creator", User.UserRole.ADMIN));
    creating.setStatus(GameStatus.CREATING);

    Game drafting = buildGame(buildUser("creator2", User.UserRole.ADMIN));
    drafting.setStatus(GameStatus.DRAFTING);

    Game active = buildGame(buildUser("creator3", User.UserRole.ADMIN));
    active.setStatus(GameStatus.ACTIVE);

    assertThat(gameDomainService.determineNextStatus(creating, 1)).isEqualTo(GameStatus.CREATING);
    assertThat(gameDomainService.determineNextStatus(drafting, 4)).isEqualTo(GameStatus.DRAFTING);
    assertThat(gameDomainService.determineNextStatus(active, 4)).isEqualTo(GameStatus.ACTIVE);
  }

  @Test
  void allowsGameStartOnlyWhenStatusAndParticipantsAreValid() {
    Game game = buildGame(buildUser("creator", User.UserRole.ADMIN));
    game.setMaxParticipants(8);
    game.setStatus(GameStatus.CREATING);

    assertThat(gameDomainService.canStartGame(game, 4)).isTrue();
    assertThat(gameDomainService.canStartGame(game, 3)).isFalse();

    game.setStatus(GameStatus.ACTIVE);
    assertThat(gameDomainService.canStartGame(game, 8)).isFalse();
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
