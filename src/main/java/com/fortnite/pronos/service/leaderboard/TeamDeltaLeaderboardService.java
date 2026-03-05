package com.fortnite.pronos.service.leaderboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamScoreDeltaRepositoryPort;
import com.fortnite.pronos.domain.team.model.TeamScoreDelta;
import com.fortnite.pronos.dto.TeamDeltaLeaderboardEntryDto;
import com.fortnite.pronos.model.GameParticipant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Computes the leaderboard for a game based on pre-computed {@link TeamScoreDelta} entries. Returns
 * participants ordered by delta PR descending with standard rank assignment (tied entries share
 * rank).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamDeltaLeaderboardService {

  private final TeamScoreDeltaRepositoryPort deltaRepository;
  private final GameParticipantRepositoryPort participantRepository;

  /**
   * Returns the leaderboard for the given game, sorted by delta PR descending with rank assigned.
   */
  public List<TeamDeltaLeaderboardEntryDto> getLeaderboard(UUID gameId) {
    log.debug("TeamDeltaLeaderboard: computing leaderboard for game={}", gameId);

    List<GameParticipant> participants = participantRepository.findByGameIdWithUserFetch(gameId);
    Map<UUID, String> usernameByParticipantId = buildUsernameMap(participants);

    List<TeamScoreDelta> deltas = new ArrayList<>(deltaRepository.findByGameId(gameId));
    List<TeamScoreDelta> completedDeltas = ensureParticipantsCovered(gameId, participants, deltas);
    completedDeltas.sort(Comparator.comparingInt(TeamScoreDelta::getDeltaPr).reversed());

    List<TeamDeltaLeaderboardEntryDto> result =
        assignRanks(completedDeltas, usernameByParticipantId);
    log.debug("TeamDeltaLeaderboard: {} entries returned for game={}", result.size(), gameId);
    return result;
  }

  private Map<UUID, String> buildUsernameMap(List<GameParticipant> participants) {
    return participants.stream()
        .filter(gp -> gp.getId() != null && gp.getUsername() != null)
        .collect(
            Collectors.toMap(
                GameParticipant::getId, GameParticipant::getUsername, (left, right) -> left));
  }

  private List<TeamScoreDelta> ensureParticipantsCovered(
      UUID gameId, List<GameParticipant> participants, List<TeamScoreDelta> deltas) {
    if (participants.isEmpty()) {
      return deltas;
    }

    LocalDate fallbackStart = deltas.isEmpty() ? LocalDate.now() : deltas.get(0).getPeriodStart();
    LocalDate fallbackEnd = deltas.isEmpty() ? fallbackStart : deltas.get(0).getPeriodEnd();
    LocalDateTime fallbackComputedAt =
        deltas.isEmpty() ? LocalDateTime.now() : deltas.get(0).getComputedAt();

    Map<UUID, TeamScoreDelta> deltaByParticipant =
        deltas.stream()
            .collect(
                Collectors.toMap(
                    TeamScoreDelta::getParticipantId, delta -> delta, (left, right) -> left));

    List<TeamScoreDelta> completed = new ArrayList<>(deltas);
    for (GameParticipant participant : participants) {
      UUID participantId = participant.getId();
      if (participantId == null || deltaByParticipant.containsKey(participantId)) {
        continue;
      }
      completed.add(
          TeamScoreDelta.restore(
              UUID.randomUUID(),
              gameId,
              participantId,
              fallbackStart,
              fallbackEnd,
              0,
              fallbackComputedAt));
    }
    return completed;
  }

  private List<TeamDeltaLeaderboardEntryDto> assignRanks(
      List<TeamScoreDelta> sorted, Map<UUID, String> usernameByParticipantId) {
    List<TeamDeltaLeaderboardEntryDto> result = new ArrayList<>();
    int rank = 1;
    for (int i = 0; i < sorted.size(); i++) {
      TeamScoreDelta delta = sorted.get(i);
      if (i > 0 && delta.getDeltaPr() < sorted.get(i - 1).getDeltaPr()) {
        rank = i + 1;
      }
      result.add(
          new TeamDeltaLeaderboardEntryDto(
              rank,
              delta.getParticipantId(),
              usernameByParticipantId.getOrDefault(delta.getParticipantId(), "—"),
              delta.getDeltaPr(),
              delta.getPeriodStart(),
              delta.getPeriodEnd(),
              delta.getComputedAt()));
    }
    return result;
  }
}
