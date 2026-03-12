package com.fortnite.pronos.service.scoring;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.game.model.Game;
import com.fortnite.pronos.domain.player.model.RankSnapshot;
import com.fortnite.pronos.domain.port.out.DraftPickRepositoryPort;
import com.fortnite.pronos.domain.port.out.GameParticipantRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.RankSnapshotRepositoryPort;
import com.fortnite.pronos.domain.port.out.TeamScoreDeltaRepositoryPort;
import com.fortnite.pronos.domain.team.model.TeamScoreDelta;
import com.fortnite.pronos.model.GameParticipant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Processes PR delta computation for a single game within its own transaction. Extracted from
 * {@link TeamScoreDeltaBatchService} to avoid Spring AOP self-invocation bypass on
 * {@code @Transactional}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamScoreDeltaGameService {

  private final GameParticipantRepositoryPort participantRepository;
  private final DraftPickRepositoryPort draftPickRepository;
  private final PlayerDomainRepositoryPort playerRepository;
  private final RankSnapshotRepositoryPort snapshotRepository;
  private final TeamScoreDeltaRepositoryPort deltaRepository;

  @Transactional
  public void computeDeltasForGame(Game game, LocalDate today) {
    LocalDate periodStart = game.getCompetitionStart();
    LocalDate periodEnd =
        game.getCompetitionEnd().isBefore(today) ? game.getCompetitionEnd() : today;

    List<GameParticipant> participants =
        participantRepository.findByGameIdOrderByJoinedAt(game.getId());

    for (GameParticipant participant : participants) {
      List<UUID> playerIds =
          draftPickRepository.findPlayerIdsByDraftIdAndParticipantId(
              game.getDraftId(), participant.getId());
      int teamDelta = computeTeamDelta(playerIds, periodStart, periodEnd);
      upsertDelta(
          game.getId(), participant.getId(), periodStart, game.getCompetitionEnd(), teamDelta);
    }
    log.debug(
        "TeamScoreDeltaBatch: processed game={} participants={}",
        game.getId(),
        participants.size());
  }

  private int computeTeamDelta(List<UUID> playerIds, LocalDate periodStart, LocalDate periodEnd) {
    int total = 0;
    for (UUID playerId : playerIds) {
      if (playerRepository.findById(playerId).isEmpty()) {
        continue;
      }
      Optional<RankSnapshot> startSnap =
          snapshotRepository.findLatestOnOrBefore(playerId, periodStart);
      Optional<RankSnapshot> endSnap = snapshotRepository.findLatestOnOrBefore(playerId, periodEnd);

      if (startSnap.isPresent() && endSnap.isPresent()) {
        total += endSnap.get().getPrValue() - startSnap.get().getPrValue();
      }
    }
    return total;
  }

  private void upsertDelta(
      UUID gameId, UUID participantId, LocalDate periodStart, LocalDate periodEnd, int deltaPr) {
    Optional<TeamScoreDelta> existing =
        deltaRepository.findByGameIdAndParticipantId(gameId, participantId);
    if (existing.isPresent()) {
      TeamScoreDelta updated =
          TeamScoreDelta.restore(
              existing.get().getId(),
              gameId,
              participantId,
              periodStart,
              periodEnd,
              deltaPr,
              java.time.LocalDateTime.now());
      deltaRepository.save(updated);
    } else {
      deltaRepository.save(
          new TeamScoreDelta(gameId, participantId, periodStart, periodEnd, deltaPr));
    }
    log.debug(
        "TeamScoreDeltaBatch: upserted delta game={} participant={} delta={}",
        gameId,
        participantId,
        deltaPr);
  }
}
