package com.fortnite.pronos.service.leaderboard;

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

import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamScoreDeltaRepositoryPort;
import com.fortnite.pronos.domain.team.model.TeamScoreDelta;
import com.fortnite.pronos.dto.TeamDeltaLeaderboardEntryDto;
import com.fortnite.pronos.model.GameParticipant;
import com.fortnite.pronos.model.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamDeltaLeaderboardService")
class TeamDeltaLeaderboardServiceTest {

  @Mock private TeamScoreDeltaRepositoryPort deltaRepository;
  @Mock private GameParticipantRepositoryPort participantRepository;

  private TeamDeltaLeaderboardService service;

  private static final UUID GAME_ID = UUID.randomUUID();
  private static final UUID PARTICIPANT_A = UUID.randomUUID();
  private static final UUID PARTICIPANT_B = UUID.randomUUID();
  private static final UUID PARTICIPANT_C = UUID.randomUUID();

  private static final LocalDate PERIOD_START = LocalDate.of(2025, 1, 1);
  private static final LocalDate PERIOD_END = LocalDate.of(2025, 12, 31);

  @BeforeEach
  void setUp() {
    service = new TeamDeltaLeaderboardService(deltaRepository, participantRepository);
  }

  private TeamScoreDelta buildDelta(UUID participantId, int deltaPr) {
    return TeamScoreDelta.restore(
        UUID.randomUUID(),
        GAME_ID,
        participantId,
        PERIOD_START,
        PERIOD_END,
        deltaPr,
        LocalDateTime.of(2025, 12, 31, 8, 0));
  }

  private GameParticipant buildParticipant(UUID participantId, String username) {
    User user = new User();
    user.setUsername(username);
    GameParticipant gp = new GameParticipant();
    gp.setId(participantId);
    gp.setUser(user);
    return gp;
  }

  @Nested
  @DisplayName("getLeaderboard")
  class GetLeaderboard {

    @Test
    @DisplayName("whenMultipleDeltas_sortedByDeltaPrDescWithRank")
    void whenMultipleDeltas_sortedByDeltaPrDescWithRank() {
      when(deltaRepository.findByGameId(GAME_ID))
          .thenReturn(
              List.of(
                  buildDelta(PARTICIPANT_A, 300),
                  buildDelta(PARTICIPANT_B, 500),
                  buildDelta(PARTICIPANT_C, 100)));

      when(participantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(
              List.of(
                  buildParticipant(PARTICIPANT_A, "alice"),
                  buildParticipant(PARTICIPANT_B, "bob"),
                  buildParticipant(PARTICIPANT_C, "carol")));

      List<TeamDeltaLeaderboardEntryDto> result = service.getLeaderboard(GAME_ID);

      assertThat(result).hasSize(3);
      // Sorted DESC: bob(500) rank1, alice(300) rank2, carol(100) rank3
      assertThat(result.get(0).username()).isEqualTo("bob");
      assertThat(result.get(0).deltaPr()).isEqualTo(500);
      assertThat(result.get(0).rank()).isEqualTo(1);

      assertThat(result.get(1).username()).isEqualTo("alice");
      assertThat(result.get(1).deltaPr()).isEqualTo(300);
      assertThat(result.get(1).rank()).isEqualTo(2);

      assertThat(result.get(2).username()).isEqualTo("carol");
      assertThat(result.get(2).deltaPr()).isEqualTo(100);
      assertThat(result.get(2).rank()).isEqualTo(3);
    }

    @Test
    @DisplayName("whenNoDeltasButParticipantsExist_returnsZeroDeltaRows")
    void whenNoDeltasButParticipantsExist_returnsZeroDeltaRows() {
      when(deltaRepository.findByGameId(GAME_ID)).thenReturn(List.of());
      when(participantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(
              List.of(
                  buildParticipant(PARTICIPANT_A, "alice"),
                  buildParticipant(PARTICIPANT_B, "bob")));

      List<TeamDeltaLeaderboardEntryDto> result = service.getLeaderboard(GAME_ID);

      assertThat(result).hasSize(2);
      assertThat(result).extracting(TeamDeltaLeaderboardEntryDto::deltaPr).containsOnly(0);
      assertThat(result).extracting(TeamDeltaLeaderboardEntryDto::rank).containsOnly(1);
      assertThat(result)
          .extracting(TeamDeltaLeaderboardEntryDto::username)
          .containsExactly("alice", "bob");
    }

    @Test
    @DisplayName("whenNoDeltasAndNoParticipants_returnsEmptyList")
    void whenNoDeltasAndNoParticipants_returnsEmptyList() {
      when(deltaRepository.findByGameId(GAME_ID)).thenReturn(List.of());
      when(participantRepository.findByGameIdWithUserFetch(GAME_ID)).thenReturn(List.of());

      List<TeamDeltaLeaderboardEntryDto> result = service.getLeaderboard(GAME_ID);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("whenSomeParticipantsMissingDelta_addsZeroDeltaRows")
    void whenSomeParticipantsMissingDelta_addsZeroDeltaRows() {
      when(deltaRepository.findByGameId(GAME_ID))
          .thenReturn(List.of(buildDelta(PARTICIPANT_A, 400)));
      when(participantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(
              List.of(
                  buildParticipant(PARTICIPANT_A, "alice"),
                  buildParticipant(PARTICIPANT_B, "bob")));

      List<TeamDeltaLeaderboardEntryDto> result = service.getLeaderboard(GAME_ID);

      assertThat(result).hasSize(2);
      assertThat(result.get(0).participantId()).isEqualTo(PARTICIPANT_A);
      assertThat(result.get(0).deltaPr()).isEqualTo(400);
      assertThat(result.get(0).rank()).isEqualTo(1);
      assertThat(result.get(1).participantId()).isEqualTo(PARTICIPANT_B);
      assertThat(result.get(1).deltaPr()).isEqualTo(0);
      assertThat(result.get(1).rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("whenTiedDeltaPr_sameRankAssigned")
    void whenTiedDeltaPr_sameRankAssigned() {
      // A and B tied at 400, C at 100
      when(deltaRepository.findByGameId(GAME_ID))
          .thenReturn(
              List.of(
                  buildDelta(PARTICIPANT_A, 400),
                  buildDelta(PARTICIPANT_B, 400),
                  buildDelta(PARTICIPANT_C, 100)));

      when(participantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(
              List.of(
                  buildParticipant(PARTICIPANT_A, "alice"),
                  buildParticipant(PARTICIPANT_B, "bob"),
                  buildParticipant(PARTICIPANT_C, "carol")));

      List<TeamDeltaLeaderboardEntryDto> result = service.getLeaderboard(GAME_ID);

      assertThat(result).hasSize(3);
      // A and B both rank 1 (tied), C gets rank 3
      assertThat(result.get(0).rank()).isEqualTo(1);
      assertThat(result.get(1).rank()).isEqualTo(1);
      assertThat(result.get(2).rank()).isEqualTo(3);
      assertThat(result.get(2).username()).isEqualTo("carol");
    }

    @Test
    @DisplayName("whenParticipantNotInMap_usernameIsDash")
    void whenParticipantNotInMap_usernameIsDash() {
      when(deltaRepository.findByGameId(GAME_ID))
          .thenReturn(List.of(buildDelta(PARTICIPANT_A, 200)));
      // No participant returned — username fallback to "—"
      when(participantRepository.findByGameIdWithUserFetch(GAME_ID)).thenReturn(List.of());

      List<TeamDeltaLeaderboardEntryDto> result = service.getLeaderboard(GAME_ID);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).username()).isEqualTo("—");
      assertThat(result.get(0).rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("whenSingleParticipant_rank1")
    void whenSingleParticipant_rank1() {
      when(deltaRepository.findByGameId(GAME_ID))
          .thenReturn(List.of(buildDelta(PARTICIPANT_A, 750)));
      when(participantRepository.findByGameIdWithUserFetch(GAME_ID))
          .thenReturn(List.of(buildParticipant(PARTICIPANT_A, "alice")));

      List<TeamDeltaLeaderboardEntryDto> result = service.getLeaderboard(GAME_ID);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).rank()).isEqualTo(1);
      assertThat(result.get(0).deltaPr()).isEqualTo(750);
      assertThat(result.get(0).participantId()).isEqualTo(PARTICIPANT_A);
    }
  }
}
