package com.fortnite.pronos.adapter.out.persistence.draft;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.draft.model.Draft;
import com.fortnite.pronos.domain.draft.model.DraftStatus;
import com.fortnite.pronos.model.Game;

/** Bidirectional mapper between pure domain Draft and JPA Draft entity. */
@Component
public class DraftEntityMapper {

  public Draft toDomain(com.fortnite.pronos.model.Draft entity) {
    if (entity == null) {
      return null;
    }
    return Draft.restore(
        entity.getId(),
        entity.getGame() != null ? entity.getGame().getId() : null,
        toDomainStatus(entity.getStatus()),
        safeInt(entity.getCurrentRound()),
        safeInt(entity.getCurrentPick()),
        safeInt(entity.getTotalRounds()),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getStartedAt(),
        entity.getFinishedAt());
  }

  public com.fortnite.pronos.model.Draft toEntity(Draft domain, Game game) {
    if (domain == null) {
      return null;
    }
    com.fortnite.pronos.model.Draft entity = new com.fortnite.pronos.model.Draft();
    entity.setId(domain.getId());
    entity.setGame(game);
    entity.setStatus(toEntityStatus(domain.getStatus()));
    entity.setCurrentRound(domain.getCurrentRound());
    entity.setCurrentPick(domain.getCurrentPick());
    entity.setTotalRounds(domain.getTotalRounds());
    entity.setCreatedAt(domain.getCreatedAt());
    entity.setUpdatedAt(domain.getUpdatedAt());
    entity.setStartedAt(domain.getStartedAt());
    entity.setFinishedAt(domain.getFinishedAt());
    return entity;
  }

  public com.fortnite.pronos.model.Draft.Status toEntityStatus(DraftStatus status) {
    if (status == null) {
      return null;
    }
    return com.fortnite.pronos.model.Draft.Status.valueOf(status.name());
  }

  public DraftStatus toDomainStatus(com.fortnite.pronos.model.Draft.Status status) {
    if (status == null) {
      return null;
    }
    return DraftStatus.valueOf(status.name());
  }

  private int safeInt(Integer value) {
    return value != null && value > 0 ? value : 1;
  }
}
