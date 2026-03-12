package com.fortnite.pronos.domain.port.out;

import java.util.List;

import com.fortnite.pronos.model.IngestionRun;

/**
 * Output port for IngestionRun persistence operations. Implemented by the persistence adapter
 * (IngestionRunRepository). Uses domain Pagination to avoid Spring Data dependency in domain layer.
 */
public interface IngestionRunRepositoryPort {

  List<IngestionRun> findRecentLogs(int limit);
}
