package com.fortnite.pronos.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;
import com.fortnite.pronos.dto.LeaderboardStatsDTO;
import com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO;
import com.fortnite.pronos.dto.PronostiqueurLeaderboardEntryDTO;
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

  // --- getLeaderboard with gameId ---

  @Test
  void getLeaderboard_filtersByGameId() {
    UUID gameId = UUID.randomUUID();
    List<LeaderboardEntryDTO> entries = List.of(entryWithRegion(Player.Region.EU));
    when(teamLeaderboardService.getLeaderboardByGame(gameId)).thenReturn(entries);

    ResponseEntity<List<LeaderboardEntryDTO>> response =
        leaderboardController.getLeaderboard(2025, null, gameId.toString());

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(1, response.getBody().size());
    verify(teamLeaderboardService).getLeaderboardByGame(gameId);
  }

  @Test
  void getLeaderboard_returnsInternalServerError_onException() {
    when(teamLeaderboardService.getLeaderboard(2025)).thenThrow(new RuntimeException("DB error"));

    ResponseEntity<List<LeaderboardEntryDTO>> response =
        leaderboardController.getLeaderboard(2025, null, null);

    assertEquals(500, response.getStatusCodeValue());
  }

  // --- getLeaderboardBySeason ---

  @Test
  void getLeaderboardBySeason_delegatesToGetLeaderboard() {
    List<LeaderboardEntryDTO> entries = List.of(entryWithRegion(Player.Region.EU));
    when(teamLeaderboardService.getLeaderboard(2024)).thenReturn(entries);

    ResponseEntity<List<LeaderboardEntryDTO>> response =
        leaderboardController.getLeaderboardBySeason(2024, null, null);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(1, response.getBody().size());
  }

  // --- getTeamRanking ---

  @Test
  void getTeamRanking_returnsOk() {
    String teamId = "team-1";
    LeaderboardEntryDTO entry = entryWithRegion(Player.Region.EU);
    when(teamLeaderboardService.getTeamRanking(teamId)).thenReturn(entry);

    ResponseEntity<LeaderboardEntryDTO> response = leaderboardController.getTeamRanking(teamId);

    assertEquals(200, response.getStatusCodeValue());
    assertNotNull(response.getBody());
  }

  @Test
  void getTeamRanking_returnsNotFound_onException() {
    when(teamLeaderboardService.getTeamRanking("missing"))
        .thenThrow(new RuntimeException("Not found"));

    ResponseEntity<LeaderboardEntryDTO> response = leaderboardController.getTeamRanking("missing");

    assertEquals(404, response.getStatusCodeValue());
  }

  // --- getLeaderboardStats ---

  @Test
  void getLeaderboardStats_returnsBySeason() {
    LeaderboardStatsDTO stats = LeaderboardStatsDTO.builder().build();
    when(statsService.getLeaderboardStats(2025)).thenReturn(stats);

    ResponseEntity<LeaderboardStatsDTO> response =
        leaderboardController.getLeaderboardStats(2025, null);

    assertEquals(200, response.getStatusCodeValue());
    assertSame(stats, response.getBody());
  }

  @Test
  void getLeaderboardStats_returnsByGameId() {
    UUID gameId = UUID.randomUUID();
    LeaderboardStatsDTO stats = LeaderboardStatsDTO.builder().build();
    when(statsService.getLeaderboardStatsByGame(gameId)).thenReturn(stats);

    ResponseEntity<LeaderboardStatsDTO> response =
        leaderboardController.getLeaderboardStats(2025, gameId.toString());

    assertEquals(200, response.getStatusCodeValue());
    verify(statsService).getLeaderboardStatsByGame(gameId);
  }

  @Test
  void getLeaderboardStats_returnsError_onException() {
    when(statsService.getLeaderboardStats(2025)).thenThrow(new RuntimeException("Error"));

    ResponseEntity<LeaderboardStatsDTO> response =
        leaderboardController.getLeaderboardStats(2025, null);

    assertEquals(500, response.getStatusCodeValue());
  }

  // --- getRegionDistribution ---

  @Test
  void getRegionDistribution_returnsGlobal() {
    Map<String, Integer> distribution = Map.of("EU", 10, "NA", 5);
    when(statsService.getRegionDistribution()).thenReturn(distribution);

    ResponseEntity<Map<String, Integer>> response =
        leaderboardController.getRegionDistribution(null);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(10, response.getBody().get("EU"));
  }

  @Test
  void getRegionDistribution_returnsByGameId() {
    UUID gameId = UUID.randomUUID();
    Map<String, Integer> distribution = Map.of("EU", 3);
    when(statsService.getRegionDistributionByGame(gameId)).thenReturn(distribution);

    ResponseEntity<Map<String, Integer>> response =
        leaderboardController.getRegionDistribution(gameId.toString());

    assertEquals(200, response.getStatusCodeValue());
    verify(statsService).getRegionDistributionByGame(gameId);
  }

  @Test
  void getRegionDistribution_returnsError_onException() {
    when(statsService.getRegionDistribution()).thenThrow(new RuntimeException("Error"));

    ResponseEntity<Map<String, Integer>> response =
        leaderboardController.getRegionDistribution(null);

    assertEquals(500, response.getStatusCodeValue());
  }

  // --- getTrancheDistribution ---

  @Test
  void getTrancheDistribution_returnsOk() {
    Map<String, Integer> distribution = Map.of("T1", 5, "T2", 8);
    when(statsService.getTrancheDistribution()).thenReturn(distribution);

    ResponseEntity<Map<String, Integer>> response = leaderboardController.getTrancheDistribution();

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(5, response.getBody().get("T1"));
  }

  @Test
  void getTrancheDistribution_returnsError_onException() {
    when(statsService.getTrancheDistribution()).thenThrow(new RuntimeException("Error"));

    ResponseEntity<Map<String, Integer>> response = leaderboardController.getTrancheDistribution();

    assertEquals(500, response.getStatusCodeValue());
  }

  // --- getPronostiqueurLeaderboard ---

  @Test
  void getPronostiqueurLeaderboard_returnsOk() {
    List<PronostiqueurLeaderboardEntryDTO> entries = List.of();
    when(pronostiqueurLeaderboardService.getPronostiqueurLeaderboard(2025)).thenReturn(entries);

    ResponseEntity<List<PronostiqueurLeaderboardEntryDTO>> response =
        leaderboardController.getPronostiqueurLeaderboard(2025);

    assertEquals(200, response.getStatusCodeValue());
  }

  @Test
  void getPronostiqueurLeaderboard_returnsError_onException() {
    when(pronostiqueurLeaderboardService.getPronostiqueurLeaderboard(2025))
        .thenThrow(new RuntimeException("Error"));

    ResponseEntity<List<PronostiqueurLeaderboardEntryDTO>> response =
        leaderboardController.getPronostiqueurLeaderboard(2025);

    assertEquals(500, response.getStatusCodeValue());
  }

  // --- getPlayerLeaderboard ---

  @Test
  void getPlayerLeaderboard_returnsBySeason() {
    PlayerLeaderboardEntryDTO entry = playerEntryWithRegion(Player.Region.EU);
    when(playerLeaderboardService.getPlayerLeaderboard(2025)).thenReturn(List.of(entry));

    ResponseEntity<List<PlayerLeaderboardEntryDTO>> response =
        leaderboardController.getPlayerLeaderboard(2025, null, null);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getPlayerLeaderboard_filtersByGameId() {
    UUID gameId = UUID.randomUUID();
    when(playerLeaderboardService.getPlayerLeaderboardByGame(gameId)).thenReturn(List.of());

    ResponseEntity<List<PlayerLeaderboardEntryDTO>> response =
        leaderboardController.getPlayerLeaderboard(2025, null, gameId.toString());

    assertEquals(200, response.getStatusCodeValue());
    verify(playerLeaderboardService).getPlayerLeaderboardByGame(gameId);
  }

  @Test
  void getPlayerLeaderboard_filtersByRegion() {
    PlayerLeaderboardEntryDTO euEntry = playerEntryWithRegion(Player.Region.EU);
    PlayerLeaderboardEntryDTO naEntry = playerEntryWithRegion(Player.Region.NAW);
    when(playerLeaderboardService.getPlayerLeaderboard(2025)).thenReturn(List.of(euEntry, naEntry));

    ResponseEntity<List<PlayerLeaderboardEntryDTO>> response =
        leaderboardController.getPlayerLeaderboard(2025, "EU", null);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void getPlayerLeaderboard_returnsError_onException() {
    when(playerLeaderboardService.getPlayerLeaderboard(2025))
        .thenThrow(new RuntimeException("Error"));

    ResponseEntity<List<PlayerLeaderboardEntryDTO>> response =
        leaderboardController.getPlayerLeaderboard(2025, null, null);

    assertEquals(500, response.getStatusCodeValue());
  }

  // --- Debug endpoints ---

  @Test
  void getDebugStats_returnsOk() {
    Map<String, Object> debug = Map.of("count", 42);
    when(debugService.getDebugStats(anyInt())).thenReturn(debug);

    ResponseEntity<Map<String, Object>> response = leaderboardController.getDebugStats();

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(42, response.getBody().get("count"));
  }

  @Test
  void getDebugStats_returnsErrorMap_onException() {
    when(debugService.getDebugStats(anyInt())).thenThrow(new RuntimeException("Debug error"));

    ResponseEntity<Map<String, Object>> response = leaderboardController.getDebugStats();

    assertEquals(200, response.getStatusCodeValue());
    assertEquals("Debug error", response.getBody().get("error"));
  }

  @Test
  void getDebugSimple_returnsOk() {
    Map<String, Object> debug = Map.of("simple", true);
    when(debugService.getDebugSimple()).thenReturn(debug);

    ResponseEntity<Map<String, Object>> response = leaderboardController.getDebugSimple();

    assertEquals(200, response.getStatusCodeValue());
  }

  @Test
  void getDebugSimple_returnsErrorMap_onException() {
    when(debugService.getDebugSimple()).thenThrow(new RuntimeException("Simple error"));

    ResponseEntity<Map<String, Object>> response = leaderboardController.getDebugSimple();

    assertEquals(200, response.getStatusCodeValue());
    assertEquals("Simple error", response.getBody().get("error"));
  }

  private static PlayerLeaderboardEntryDTO playerEntryWithRegion(Player.Region region) {
    PlayerLeaderboardEntryDTO dto = new PlayerLeaderboardEntryDTO();
    dto.setPlayerId(UUID.randomUUID().toString());
    dto.setRegion(region);
    return dto;
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
