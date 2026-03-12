package com.fortnite.pronos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.TeamScoreDeltaEntity;

/** JPA repository for the team_score_deltas table. */
@Repository
public interface TeamScoreDeltaJpaRepository extends JpaRepository<TeamScoreDeltaEntity, UUID> {

  List<TeamScoreDeltaEntity> findByGameId(UUID gameId);

  Optional<TeamScoreDeltaEntity> findByGameIdAndParticipantId(UUID gameId, UUID participantId);
}
