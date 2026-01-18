package com.fortnite.pronos.service.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.model.Team;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaderboardDebugService - TDD Tests")
class LeaderboardDebugServiceTddTest {

  @Mock private TeamRepository teamRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private ScoreRepository scoreRepository;

  @InjectMocks private LeaderboardDebugService leaderboardDebugService;

  @Test
  @DisplayName("getDebugStats returns expected counts and samples")
  void getDebugStatsReturnsExpectedCountsAndSamples() {
    int season = 2025;
    UUID playerId = UUID.randomUUID();

    when(teamRepository.findBySeason(season)).thenReturn(List.of(new Team()));
    when(playerRepository.count()).thenReturn(2L);
    when(scoreRepository.count()).thenReturn(3L);
    when(scoreRepository.findAllBySeasonGroupedByPlayerRaw(season))
        .thenReturn(List.<Object[]>of(new Object[] {playerId, 100}));
    when(scoreRepository.findAllBySeasonGroupedByPlayer(season)).thenReturn(Map.of(playerId, 100));

    Map<String, Object> debug = leaderboardDebugService.getDebugStats(season);

    assertThat(debug)
        .containsKeys("totalTeams", "totalPlayers", "totalScores", "playerPointsMapSize");
    assertThat(debug.get("totalTeams")).isEqualTo(1L);
    assertThat(debug.get("totalPlayers")).isEqualTo(2L);
    assertThat(debug.get("totalScores")).isEqualTo(3L);
  }

  @Test
  @DisplayName("getDebugSimple returns basic samples")
  void getDebugSimpleReturnsBasicSamples() {
    Player player = buildPlayer();
    Score score = buildScore(player);

    when(playerRepository.count()).thenReturn(1L);
    when(teamRepository.count()).thenReturn(2L);
    when(scoreRepository.count()).thenReturn(3L);
    when(playerRepository.findAll()).thenReturn(List.of(player));
    when(scoreRepository.findAll()).thenReturn(List.of(score));

    Map<String, Object> debug = leaderboardDebugService.getDebugSimple();

    assertThat(debug).containsKeys("totalPlayers", "totalTeams", "totalScores");
    assertThat(debug.get("totalPlayers")).isEqualTo(1L);
    assertThat(debug.get("totalTeams")).isEqualTo(2L);
    assertThat(debug.get("totalScores")).isEqualTo(3L);
  }

  private Player buildPlayer() {
    Player player = new Player();
    player.setId(UUID.randomUUID());
    player.setUsername("player");
    player.setNickname("player");
    player.setRegion(Player.Region.EU);
    player.setTranche("1");
    player.setCurrentSeason(2025);
    return player;
  }

  private Score buildScore(Player player) {
    Score score = new Score();
    score.setPlayer(player);
    score.setSeason(2025);
    score.setPoints(100);
    return score;
  }
}
