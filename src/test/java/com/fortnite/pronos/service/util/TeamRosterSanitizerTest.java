package com.fortnite.pronos.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;

@DisplayName("TeamRosterSanitizer")
class TeamRosterSanitizerTest {

  @Test
  @DisplayName("Devrait filtrer les doublons, inactifs et joueurs incomplets")
  void shouldFilterDuplicatesInactiveAndMissingPlayers() {
    Team team = new Team();
    team.setId(UUID.randomUUID());

    Player player1 = new Player();
    player1.setId(UUID.randomUUID());
    player1.setNickname("Player1");
    player1.setUsername("player1");
    player1.setRegion(Player.Region.EU);
    player1.setTranche("1-5");

    Player player2 = new Player();
    player2.setId(UUID.randomUUID());
    player2.setNickname("Player2");
    player2.setUsername("player2");
    player2.setRegion(Player.Region.NAW);
    player2.setTranche("1-5");

    Player missingIdPlayer = new Player();
    missingIdPlayer.setNickname("Missing");
    missingIdPlayer.setUsername("missing");
    missingIdPlayer.setRegion(Player.Region.EU);
    missingIdPlayer.setTranche("1-5");

    TeamPlayer active = new TeamPlayer();
    active.setTeam(team);
    active.setPlayer(player1);
    active.setPosition(1);

    TeamPlayer duplicate = new TeamPlayer();
    duplicate.setTeam(team);
    duplicate.setPlayer(player1);
    duplicate.setPosition(2);

    TeamPlayer inactive = new TeamPlayer();
    inactive.setTeam(team);
    inactive.setPlayer(player2);
    inactive.setPosition(3);
    inactive.setUntil(OffsetDateTime.now().minusDays(1));

    TeamPlayer missing = new TeamPlayer();
    missing.setTeam(team);
    missing.setPlayer(missingIdPlayer);
    missing.setPosition(4);

    team.setPlayers(List.of(active, duplicate, inactive, missing));

    List<TeamPlayer> result = TeamRosterSanitizer.sanitize(team, "test");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPlayer().getId()).isEqualTo(player1.getId());
  }

  @Test
  @DisplayName("Devrait retourner une liste vide pour une equipe nulle")
  void shouldReturnEmptyForNullTeam() {
    List<TeamPlayer> result = TeamRosterSanitizer.sanitize(null, "test");
    assertThat(result).isEmpty();
  }
}
