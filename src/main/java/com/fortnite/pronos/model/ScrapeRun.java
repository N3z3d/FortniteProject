package com.fortnite.pronos.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Data;

@Data
@Entity
@Table(name = "scrape_runs")
public class ScrapeRun {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.PENDING;

  @CreationTimestamp
  @Column(name = "started_at", nullable = false, updatable = false)
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "players_processed")
  private Integer playersProcessed = 0;

  @Column(name = "players_failed")
  private Integer playersFailed = 0;

  @Column(name = "errors", columnDefinition = "TEXT")
  private String errors;

  public enum Status {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
  }

  public void markAsRunning() {
    this.status = Status.RUNNING;
  }

  public void markAsCompleted() {
    this.status = Status.SUCCESS;
    this.completedAt = OffsetDateTime.now();
    if (this.startedAt != null) {
      this.durationMs = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
    }
  }

  public void markAsFailed(String errorMessage) {
    this.status = Status.FAILED;
    this.completedAt = OffsetDateTime.now();
    this.errorMessage = errorMessage;
    if (this.startedAt != null) {
      this.durationMs = java.time.Duration.between(this.startedAt, this.completedAt).toMillis();
    }
  }

  public boolean isCompleted() {
    return status == Status.SUCCESS || status == Status.FAILED;
  }

  public boolean isSuccessful() {
    return status == Status.SUCCESS;
  }

  public boolean isFailed() {
    return status == Status.FAILED;
  }
}
