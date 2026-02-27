package com.fortnite.pronos.adapter.out.persistence.draft;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fortnite.pronos.domain.draft.model.DraftRegionCursor;
import com.fortnite.pronos.domain.port.out.DraftRegionCursorRepositoryPort;

/** Persistence adapter that bridges the domain port to Spring Data JPA. */
@Component
public class DraftRegionCursorRepositoryAdapter implements DraftRegionCursorRepositoryPort {

  private final DraftRegionCursorJpaRepository jpaRepository;
  private final DraftRegionCursorEntityMapper mapper;

  public DraftRegionCursorRepositoryAdapter(
      DraftRegionCursorJpaRepository jpaRepository, DraftRegionCursorEntityMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Optional<DraftRegionCursor> findByDraftIdAndRegion(UUID draftId, String region) {
    return jpaRepository.findByIdDraftIdAndIdRegion(draftId, region).map(mapper::toDomain);
  }

  @Override
  public DraftRegionCursor save(DraftRegionCursor cursor) {
    DraftRegionCursorEntity entity = mapper.toEntity(cursor);
    DraftRegionCursorEntity saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
  }
}
