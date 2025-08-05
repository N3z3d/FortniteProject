package com.fortnite.pronos.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fortnite.pronos.model.ScrapeRun;

@Repository
public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, UUID> {
  List<ScrapeRun> findByStatus(ScrapeRun.Status status);

  @Query("SELECT sr FROM ScrapeRun sr WHERE sr.startedAt BETWEEN :start AND :end")
  List<ScrapeRun> findByStartedAtBetween(
      @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

  @Query("SELECT sr FROM ScrapeRun sr ORDER BY sr.startedAt DESC")
  Optional<ScrapeRun> findTopByOrderByStartedAtDesc();

  @Query("SELECT COUNT(sr) FROM ScrapeRun sr WHERE sr.status = :status")
  long countByStatus(@Param("status") ScrapeRun.Status status);

  @Query(
      "SELECT COUNT(sr) FROM ScrapeRun sr WHERE sr.status = :status AND sr.startedAt BETWEEN :start AND :end")
  long countByStatusAndStartedAtBetween(
      @Param("status") ScrapeRun.Status status,
      @Param("start") OffsetDateTime start,
      @Param("end") OffsetDateTime end);

  @Query("SELECT sr FROM ScrapeRun sr WHERE sr.startedAt >= :since ORDER BY sr.startedAt DESC")
  Page<ScrapeRun> findRecentRuns(@Param("since") OffsetDateTime since, Pageable pageable);

  @Query("SELECT sr FROM ScrapeRun sr WHERE sr.status = 'FAILURE' ORDER BY sr.startedAt DESC")
  List<ScrapeRun> findFailedRuns();

  @Query("SELECT sr FROM ScrapeRun sr WHERE sr.status = 'PARTIAL' ORDER BY sr.startedAt DESC")
  List<ScrapeRun> findPartialRuns();

  @Query("SELECT AVG(sr.durationMs) FROM ScrapeRun sr WHERE sr.status = 'SUCCESS'")
  Double averageSuccessfulDuration();

  @Query("SELECT MAX(sr.durationMs) FROM ScrapeRun sr")
  Long maxDuration();

  @Query("SELECT MIN(sr.durationMs) FROM ScrapeRun sr WHERE sr.status = 'SUCCESS'")
  Long minSuccessfulDuration();

  @Query(
      "SELECT sr FROM ScrapeRun sr WHERE sr.startedAt >= :since AND sr.status = 'SUCCESS' "
          + "ORDER BY sr.startedAt DESC")
  Optional<ScrapeRun> findLastSuccessfulRun(@Param("since") OffsetDateTime since);

  @Query("SELECT sr FROM ScrapeRun sr WHERE sr.startedAt >= :since " + "ORDER BY sr.startedAt DESC")
  Optional<ScrapeRun> findLastRun(@Param("since") OffsetDateTime since);

  @Query(
      "SELECT COUNT(sr) FROM ScrapeRun sr WHERE sr.startedAt >= :since AND sr.status = 'FAILURE'")
  long countFailedRunsSince(@Param("since") OffsetDateTime since);
}
