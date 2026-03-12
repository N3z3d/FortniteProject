package com.fortnite.pronos.adapter.out.persistence.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.port.out.TeamScoreDeltaRepositoryPort;
import com.fortnite.pronos.domain.team.model.TeamScoreDelta;
import com.fortnite.pronos.model.TeamScoreDeltaEntity;
import com.fortnite.pronos.repository.TeamScoreDeltaJpaRepository;

import lombok.RequiredArgsConstructor;

/** Persistence adapter for TeamScoreDelta, bridging the domain port and JPA layer. */
@Component
@RequiredArgsConstructor
public class TeamScoreDeltaRepositoryAdapter implements TeamScoreDeltaRepositoryPort {

  private final TeamScoreDeltaJpaRepository jpaRepository;

  @Override
  public TeamScoreDelta save(TeamScoreDelta delta) {
    TeamScoreDeltaEntity entity = toEntity(delta);
    return toDomain(jpaRepository.save(entity));
  }

  @Override
  public List<TeamScoreDelta> findByGameId(UUID gameId) {
    return jpaRepository.findByGameId(gameId).stream().map(this::toDomain).toList();
  }

  @Override
  public Optional<TeamScoreDelta> findByGameIdAndParticipantId(UUID gameId, UUID participantId) {
    return jpaRepository.findByGameIdAndParticipantId(gameId, participantId).map(this::toDomain);
  }

  private TeamScoreDeltaEntity toEntity(TeamScoreDelta delta) {
    TeamScoreDeltaEntity entity = new TeamScoreDeltaEntity();
    entity.setId(delta.getId());
    entity.setGameId(delta.getGameId());
    entity.setParticipantId(delta.getParticipantId());
    entity.setPeriodStart(delta.getPeriodStart());
    entity.setPeriodEnd(delta.getPeriodEnd());
    entity.setDeltaPr(delta.getDeltaPr());
    entity.setComputedAt(delta.getComputedAt());
    return entity;
  }

  private TeamScoreDelta toDomain(TeamScoreDeltaEntity entity) {
    return TeamScoreDelta.restore(
        entity.getId(),
        entity.getGameId(),
        entity.getParticipantId(),
        entity.getPeriodStart(),
        entity.getPeriodEnd(),
        entity.getDeltaPr(),
        entity.getComputedAt());
  }
}
