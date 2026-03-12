package com.fortnite.pronos.service.admin;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.port.out.IngestionRunRepositoryPort;
import com.fortnite.pronos.dto.admin.ScrapeLogDto;
import com.fortnite.pronos.model.IngestionRun;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScrapeLogService {

  static final int MIN_LIMIT = 1;
  static final int MAX_LIMIT = 200;

  private final IngestionRunRepositoryPort ingestionRunRepository;

  public List<ScrapeLogDto> getRecentLogs(int limit) {
    int clampedLimit = Math.max(MIN_LIMIT, Math.min(limit, MAX_LIMIT));
    return ingestionRunRepository.findRecentLogs(clampedLimit).stream().map(this::toDto).toList();
  }

  private ScrapeLogDto toDto(IngestionRun run) {
    return new ScrapeLogDto(
        run.getId(),
        run.getSource(),
        run.getStartedAt(),
        run.getFinishedAt(),
        run.getStatus() != null ? run.getStatus().name() : null,
        run.getTotalRowsWritten(),
        run.getErrorMessage());
  }
}
