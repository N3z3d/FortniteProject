package com.fortnite.pronos.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fortnite.pronos.domain.team.model.TeamScoreDelta;

/** Output port for TeamScoreDelta persistence operations. */
public interface TeamScoreDeltaRepositoryPort {

  TeamScoreDelta save(TeamScoreDelta delta);

  List<TeamScoreDelta> findByGameId(UUID gameId);

  Optional<TeamScoreDelta> findByGameIdAndParticipantId(UUID gameId, UUID participantId);
}
