package com.fortnite.pronos.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.domain.port.out.IngestionRunRepositoryPort;
import com.fortnite.pronos.model.IngestionRun;

@Repository
public interface IngestionRunRepository
    extends JpaRepository<IngestionRun, UUID>, IngestionRunRepositoryPort {

  @Override
  default List<IngestionRun> findRecentLogs(int limit) {
    return findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt")))
        .getContent();
  }
}
