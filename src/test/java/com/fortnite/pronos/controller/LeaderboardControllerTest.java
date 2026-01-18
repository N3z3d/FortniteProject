package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.service.leaderboard.LeaderboardDebugService;
import com.fortnite.pronos.service.leaderboard.LeaderboardStatsService;
import com.fortnite.pronos.service.leaderboard.PlayerLeaderboardService;
import com.fortnite.pronos.service.leaderboard.PronostiqueurLeaderboardService;
import com.fortnite.pronos.service.leaderboard.TeamLeaderboardService;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {

  @Mock private TeamLeaderboardService teamLeaderboardService;
  @Mock private PlayerLeaderboardService playerLeaderboardService;
  @Mock private PronostiqueurLeaderboardService pronostiqueurLeaderboardService;
  @Mock private LeaderboardStatsService statsService;
  @Mock private LeaderboardDebugService debugService;
  @InjectMocks private LeaderboardController leaderboardController;

  @Test
  void getLeaderboard_usesServiceResult_whenCacheIsEmpty() {
    int season = 2025;
    List<LeaderboardEntryDTO> serviceEntries = List.of(entryWithRegion(Player.Region.EU));

    when(teamLeaderboardService.getLeaderboard(season)).thenReturn(serviceEntries);

    ResponseEntity<List<LeaderboardEntryDTO>> response =
        leaderboardController.getLeaderboard(season, null, null);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(serviceEntries, response.getBody());
    verify(teamLeaderboardService).getLeaderboard(season);
  }

  @Test
  void getLeaderboard_filtersByRegion() {
    int season = 2025;
    LeaderboardEntryDTO euEntry = entryWithRegion(Player.Region.EU);
    LeaderboardEntryDTO nawEntry = entryWithRegion(Player.Region.NAW);

    when(teamLeaderboardService.getLeaderboard(season)).thenReturn(List.of(euEntry, nawEntry));

    ResponseEntity<List<LeaderboardEntryDTO>> response =
        leaderboardController.getLeaderboard(season, "EU", null);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
    assertEquals(List.of(euEntry), response.getBody());
  }

  private static LeaderboardEntryDTO entryWithRegion(Player.Region region) {
    return LeaderboardEntryDTO.builder()
        .teamId(UUID.randomUUID())
        .teamName("Team")
        .ownerId(UUID.randomUUID())
        .ownerUsername("Owner")
        .totalPoints(100L)
        .players(
            List.of(
                LeaderboardEntryDTO.PlayerInfo.builder()
                    .playerId(UUID.randomUUID())
                    .region(region)
                    .build()))
        .build();
  }
}
