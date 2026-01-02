package com.fortnite.pronos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.User;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryRoleTest {

  @Autowired private UserRepository userRepository;
  @Autowired private TeamRepository teamRepository;

  @Test
  void filtersUsersByRoleForTeamParticipationQueries() {
    User userWithTeam = buildUser("user1", User.UserRole.USER);
    User userWithoutTeam = buildUser("user2", User.UserRole.USER);
    User admin = buildUser("admin", User.UserRole.ADMIN);

    userRepository.saveAll(List.of(userWithTeam, userWithoutTeam, admin));

    Team team = new Team();
    team.setId(UUID.randomUUID());
    team.setName("Team user1");
    team.setOwner(userWithTeam);
    team.setSeason(2025);
    teamRepository.save(team);

    List<User> withTeam = userRepository.findParticipantsWithTeam(2025);
    List<User> withoutTeam = userRepository.findParticipantsWithoutTeam(2025);

    assertThat(withTeam).extracting(User::getUsername).containsExactly("user1");
    assertThat(withoutTeam).extracting(User::getUsername).containsExactly("user2");
    assertThat(userRepository.countParticipants()).isEqualTo(2);
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
