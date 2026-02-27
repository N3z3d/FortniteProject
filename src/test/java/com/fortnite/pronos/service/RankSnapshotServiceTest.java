package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.player.model.RankSnapshot;
import com.fortnite.pronos.domain.port.out.RankSnapshotRepositoryPort;
import com.fortnite.pronos.dto.player.RankSnapshotResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("RankSnapshotService")
class RankSnapshotServiceTest {

  @Mock private RankSnapshotRepositoryPort repository;

  private RankSnapshotService service;

  private static final UUID PLAYER_ID = UUID.randomUUID();
  private static final String REGION = "EU";

  @BeforeEach
  void setUp() {
    service = new RankSnapshotService(repository);
  }

  // ===== getSparkline =====

  @Nested
  @DisplayName("getSparkline")
  class GetSparkline {

    @Test
    @DisplayName("returns empty list when no snapshots available")
    void shouldReturnEmptyWhenNoSnapshots() {
      when(repository.findByPlayerAndRegion(PLAYER_ID, REGION, 14)).thenReturn(List.of());

      List<RankSnapshotResponse> result = service.getSparkline(PLAYER_ID, REGION, 14);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("returns snapshots sorted by date ascending")
    void shouldSortByDateAscending() {
      LocalDate today = LocalDate.now();
      RankSnapshot older =
          RankSnapshot.restore(UUID.randomUUID(), PLAYER_ID, REGION, 50, 1000, today.minusDays(2));
      RankSnapshot newer =
          RankSnapshot.restore(UUID.randomUUID(), PLAYER_ID, REGION, 45, 1100, today.minusDays(1));
      // Repository returns in arbitrary order
      when(repository.findByPlayerAndRegion(PLAYER_ID, REGION, 14))
          .thenReturn(List.of(newer, older));

      List<RankSnapshotResponse> result = service.getSparkline(PLAYER_ID, REGION, 14);

      assertThat(result).hasSize(2);
      assertThat(result.get(0).date()).isEqualTo(today.minusDays(2));
      assertThat(result.get(1).date()).isEqualTo(today.minusDays(1));
    }

    @Test
    @DisplayName("maps rank and date to response DTO")
    void shouldMapRankAndDate() {
      LocalDate date = LocalDate.of(2026, 2, 10);
      RankSnapshot snapshot =
          RankSnapshot.restore(UUID.randomUUID(), PLAYER_ID, REGION, 37, 1500, date);
      when(repository.findByPlayerAndRegion(PLAYER_ID, REGION, 14)).thenReturn(List.of(snapshot));

      List<RankSnapshotResponse> result = service.getSparkline(PLAYER_ID, REGION, 14);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).rank()).isEqualTo(37);
      assertThat(result.get(0).date()).isEqualTo(date);
    }

    @Test
    @DisplayName("caps days at MAX_DAYS (90) when value exceeds limit")
    void shouldCapDaysAt90() {
      when(repository.findByPlayerAndRegion(PLAYER_ID, REGION, 90)).thenReturn(List.of());

      service.getSparkline(PLAYER_ID, REGION, 365);

      verify(repository).findByPlayerAndRegion(PLAYER_ID, REGION, 90);
    }

    @Test
    @DisplayName("passes days unchanged when within limit")
    void shouldPassDaysWithinLimit() {
      when(repository.findByPlayerAndRegion(PLAYER_ID, REGION, 7)).thenReturn(List.of());

      service.getSparkline(PLAYER_ID, REGION, 7);

      verify(repository).findByPlayerAndRegion(PLAYER_ID, REGION, 7);
    }
  }

  // ===== recordSnapshot =====

  @Nested
  @DisplayName("recordSnapshot")
  class RecordSnapshot {

    @Test
    @DisplayName("saves a new snapshot with today's date")
    void shouldSaveSnapshotWithTodaysDate() {
      when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      RankSnapshot result = service.recordSnapshot(PLAYER_ID, REGION, 42, 1800);

      ArgumentCaptor<RankSnapshot> captor = ArgumentCaptor.forClass(RankSnapshot.class);
      verify(repository).save(captor.capture());
      assertThat(captor.getValue().getSnapshotDate()).isEqualTo(LocalDate.now());
      assertThat(captor.getValue().getRank()).isEqualTo(42);
      assertThat(captor.getValue().getPrValue()).isEqualTo(1800);
    }

    @Test
    @DisplayName("returns the persisted snapshot from repository")
    void shouldReturnPersistedSnapshot() {
      RankSnapshot saved =
          RankSnapshot.restore(UUID.randomUUID(), PLAYER_ID, REGION, 42, 1800, LocalDate.now());
      when(repository.save(any())).thenReturn(saved);

      RankSnapshot result = service.recordSnapshot(PLAYER_ID, REGION, 42, 1800);

      assertThat(result).isEqualTo(saved);
    }

    @Test
    @DisplayName("sets correct region on saved snapshot")
    void shouldSetCorrectRegion() {
      when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.recordSnapshot(PLAYER_ID, "NAE", 10, 5000);

      ArgumentCaptor<RankSnapshot> captor = ArgumentCaptor.forClass(RankSnapshot.class);
      verify(repository).save(captor.capture());
      assertThat(captor.getValue().getRegion()).isEqualTo("NAE");
    }
  }
}
