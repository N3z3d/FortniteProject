package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.model.GameRegionRule;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.RegionRule;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.model.TeamPlayer;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationService - Team Composition TDD Tests")
class ValidationServiceTeamCompositionTddTest {

  @InjectMocks private ValidationService validationService;

  @Test
  @DisplayName("Should accept when no region rules provided")
  void shouldAcceptWhenNoRegionRulesProvided() {
    Team team = buildTeamWithPlayers(Player.Region.EU);

    assertThatCode(() -> validationService.validateTeamComposition(team, null))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should reject null team")
  void shouldRejectNullTeam() {
    assertThatThrownBy(() -> validationService.validateTeamComposition(null, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Team is required");
  }

  @Test
  @DisplayName("Should reject null region rule")
  void shouldRejectNullRegionRule() {
    Team team = buildTeamWithPlayers(Player.Region.EU);

    assertThatThrownBy(() -> validationService.validateTeamComposition(team, List.of((Object) null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Region rule cannot be null");
  }

  @Test
  @DisplayName("Should reject missing maxPlayers")
  void shouldRejectMissingMaxPlayers() {
    Team team = buildTeamWithPlayers(Player.Region.EU);
    GameRegionRule rule = new GameRegionRule();
    rule.setRegion(Player.Region.EU);
    rule.setMaxPlayers(null);

    assertThatThrownBy(() -> validationService.validateTeamComposition(team, List.of(rule)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("maxPlayers");
  }

  @Test
  @DisplayName("Should reject when team exceeds region limit")
  void shouldRejectWhenTeamExceedsRegionLimit() {
    Team team = buildTeamWithPlayers(Player.Region.EU, Player.Region.EU);
    GameRegionRule rule = new GameRegionRule();
    rule.setRegion(Player.Region.EU);
    rule.setMaxPlayers(1);

    assertThatThrownBy(() -> validationService.validateTeamComposition(team, List.of(rule)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("exceeds regional limit");
  }

  @Test
  @DisplayName("Should ignore inactive players")
  void shouldIgnoreInactivePlayers() {
    Team team = new Team();
    team.setName("InactiveTeam");
    TeamPlayer active = buildTeamPlayer(Player.Region.EU, null, 1);
    TeamPlayer inactive =
        buildTeamPlayer(Player.Region.EU, OffsetDateTime.now().minusDays(1), 2);
    team.setPlayers(Arrays.asList(active, inactive));

    GameRegionRule rule = new GameRegionRule();
    rule.setRegion(Player.Region.EU);
    rule.setMaxPlayers(1);

    assertThatCode(() -> validationService.validateTeamComposition(team, List.of(rule)))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Should reject missing region in RegionRule")
  void shouldRejectMissingRegionInRegionRule() {
    Team team = buildTeamWithPlayers(Player.Region.EU);
    RegionRule rule = new RegionRule();
    rule.setRegion(null);
    rule.setMaxPlayers(2);

    assertThatThrownBy(() -> validationService.validateTeamComposition(team, List.of(rule)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("requires a region");
  }

  private Team buildTeamWithPlayers(Player.Region... regions) {
    Team team = new Team();
    team.setName("TestTeam");
    List<TeamPlayer> players = new ArrayList<>();
    int position = 1;
    for (Player.Region region : regions) {
      players.add(buildTeamPlayer(region, null, position++));
    }
    team.setPlayers(players);
    return team;
  }

  private TeamPlayer buildTeamPlayer(Player.Region region, OffsetDateTime until, int position) {
    Player player = new Player();
    player.setRegion(region);
    TeamPlayer teamPlayer = new TeamPlayer();
    teamPlayer.setPlayer(player);
    teamPlayer.setPosition(position);
    teamPlayer.setUntil(until);
    return teamPlayer;
  }
}
