package com.fortnite.pronos.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;

import com.fortnite.pronos.model.IngestionRun;

@DataJpaTest
@ActiveProfiles("test")
class IngestionRunRepositoryTest {

  @Autowired private IngestionRunRepository ingestionRunRepository;

  @Test
  void savesRunWithDefaults() {
    IngestionRun run = new IngestionRun();
    run.setSource("FORTNITE_TRACKER");

    IngestionRun saved = ingestionRunRepository.saveAndFlush(run);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(IngestionRun.Status.RUNNING);
    assertThat(saved.getStartedAt()).isNotNull();
  }

  @Test
  void preservesExplicitStatusAndStartTime() {
    IngestionRun run = new IngestionRun();
    run.setSource("FORTNITE_TRACKER");
    OffsetDateTime startedAt = OffsetDateTime.parse("2025-01-10T10:15:30Z");
    run.setStartedAt(startedAt);
    run.setStatus(IngestionRun.Status.PARTIAL);

    IngestionRun saved = ingestionRunRepository.saveAndFlush(run);

    assertThat(saved.getStatus()).isEqualTo(IngestionRun.Status.PARTIAL);
    assertThat(saved.getStartedAt()).isEqualTo(startedAt);
  }

  @Test
  void defaultsStatusWhenNullButKeepsStartedAt() {
    IngestionRun run = new IngestionRun();
    run.setSource("FORTNITE_TRACKER");
    OffsetDateTime startedAt = OffsetDateTime.parse("2025-01-10T10:15:30Z");
    run.setStartedAt(startedAt);

    IngestionRun saved = ingestionRunRepository.saveAndFlush(run);

    assertThat(saved.getStatus()).isEqualTo(IngestionRun.Status.RUNNING);
    assertThat(saved.getStartedAt()).isEqualTo(startedAt);
  }

  @Test
  void rejectsBlankSource() {
    IngestionRun run = new IngestionRun();
    run.setSource(" ");

    Throwable thrown = catchThrowable(() -> ingestionRunRepository.saveAndFlush(run));

    assertThat(thrown).isInstanceOf(InvalidDataAccessApiUsageException.class);
    assertThat(thrown.getCause())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Ingestion source is required.");
  }
}
