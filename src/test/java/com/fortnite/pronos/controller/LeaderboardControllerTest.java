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
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.service.LeaderboardService;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {

  @Mock private LeaderboardService leaderboardService;
  @Mock private ScoreRepository scoreRepository;
  @Mock private PlayerRepository playerRepository;
  @Mock private TeamRepository teamRepository;

  @InjectMocks private LeaderboardController leaderboardController;

  @Test
  void getLeaderboard_usesServiceResult_whenCacheIsEmpty() {
    int season = 2025;
    List<LeaderboardEntryDTO> serviceEntries = List.of(entryWithRegion(Player.Region.EU));

    when(leaderboardService.getLeaderboard(season)).thenReturn(serviceEntries);

    ResponseEntity<List<LeaderboardEntryDTO>> response =
        leaderboardController.getLeaderboard(season, null);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(serviceEntries, response.getBody());
    verify(leaderboardService).getLeaderboard(season);
  }

  @Test
  void getLeaderboard_filtersByRegion() {
    int season = 2025;
    LeaderboardEntryDTO euEntry = entryWithRegion(Player.Region.EU);
    LeaderboardEntryDTO nawEntry = entryWithRegion(Player.Region.NAW);

    when(leaderboardService.getLeaderboard(season)).thenReturn(List.of(euEntry, nawEntry));

    ResponseEntity<List<LeaderboardEntryDTO>> response =
        leaderboardController.getLeaderboard(season, "EU");

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
