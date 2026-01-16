package com.fortnite.pronos.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ingestion_runs")
public class IngestionRun {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 50)
  private String source;

  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(name = "total_rows_written")
  private Integer totalRowsWritten;

  @Column(name = "error_message")
  private String errorMessage;

  @PrePersist
  void applyDefaults() {
    if (source == null || source.isBlank()) {
      throw new IllegalArgumentException("Ingestion source is required.");
    }
    if (status == null) {
      status = Status.RUNNING;
    }
    if (startedAt == null) {
      startedAt = OffsetDateTime.now();
    }
  }

  public enum Status {
    RUNNING,
    PARTIAL,
    SUCCESS,
    FAILED
  }
}
