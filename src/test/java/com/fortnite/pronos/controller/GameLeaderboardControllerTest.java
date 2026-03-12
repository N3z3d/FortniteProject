package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fortnite.pronos.dto.TeamDeltaLeaderboardEntryDto;
import com.fortnite.pronos.service.leaderboard.TeamDeltaLeaderboardService;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameLeaderboardController")
class GameLeaderboardControllerTest {

  @Mock private TeamDeltaLeaderboardService leaderboardService;

  private GameLeaderboardController controller;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID PARTICIPANT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    controller = new GameLeaderboardController(leaderboardService);
  }

  private TeamDeltaLeaderboardEntryDto buildEntry(int rank, String username, int deltaPr) {
    return new TeamDeltaLeaderboardEntryDto(
        rank,
        PARTICIPANT_ID,
        username,
        deltaPr,
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 12, 31),
        LocalDateTime.of(2025, 12, 31, 8, 0));
  }

  @Nested
  @DisplayName("getLeaderboard")
  class GetLeaderboard {

    @Test
    @DisplayName("whenDeltas_returns200WithSortedEntries")
    void whenDeltas_returns200WithSortedEntries() {
      List<TeamDeltaLeaderboardEntryDto> entries =
          List.of(buildEntry(1, "alice", 500), buildEntry(2, "bob", 300));

      when(leaderboardService.getLeaderboard(GAME_ID)).thenReturn(entries);

      ResponseEntity<List<TeamDeltaLeaderboardEntryDto>> response =
          controller.getLeaderboard(GAME_ID);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).hasSize(2);
      assertThat(response.getBody().get(0).username()).isEqualTo("alice");
      assertThat(response.getBody().get(0).rank()).isEqualTo(1);
      assertThat(response.getBody().get(1).username()).isEqualTo("bob");
    }

    @Test
    @DisplayName("whenNoDeltas_returns200WithEmptyList")
    void whenNoDeltas_returns200WithEmptyList() {
      when(leaderboardService.getLeaderboard(GAME_ID)).thenReturn(List.of());

      ResponseEntity<List<TeamDeltaLeaderboardEntryDto>> response =
          controller.getLeaderboard(GAME_ID);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEmpty();
    }
  }
}
