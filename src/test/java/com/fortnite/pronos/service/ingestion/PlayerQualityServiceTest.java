package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.game.model.PlayerRegion;
import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.player.model.Player;
import com.fortnite.pronos.domain.port.out.PlayerDomainRepositoryPort;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.PrSnapshotQueryPort;
import com.fortnite.pronos.service.ingestion.PlayerQualityService.PlayerQualityJobResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerQualityService — daily quality job")
class PlayerQualityServiceTest {

  @Mock private PlayerDomainRepositoryPort playerRepository;
  @Mock private PlayerIdentityRepositoryPort identityRepository;
  @Mock private PrSnapshotQueryPort snapshotQuery;

  private PlayerQualityService service;

  @BeforeEach
  void setUp() {
    service = new PlayerQualityService(playerRepository, identityRepository, snapshotQuery);
  }

  private Player playerWithRegion(PlayerRegion region) {
    return Player.restore(
        UUID.randomUUID(), null, "user" + region, "Nick" + region, region, "1-5", 2025, false);
  }

  private PlayerIdentityEntry resolvedEntry(UUID playerId, String epicId) {
    PlayerIdentityEntry entry =
        new PlayerIdentityEntry(playerId, "nick", "EU", LocalDateTime.now().minusHours(1));
    entry.resolve(epicId, 100, "AUTO");
    return entry;
  }

  private PlayerIdentityEntry unresolvedEntry(int hoursAgo) {
    LocalDateTime createdAt = LocalDateTime.now().minusHours(hoursAgo);
    return new PlayerIdentityEntry(UUID.randomUUID(), "nick", "EU", createdAt);
  }

  @Nested
  @DisplayName("FR-07: Main region computation")
  class MainRegionComputation {

    @Test
    @DisplayName("Updates player region when snapshot data shows different main region")
    void updatesPlayerRegionFromSnapshotData() {
      Player player = playerWithRegion(PlayerRegion.UNKNOWN);
      when(playerRepository.findAll()).thenReturn(List.of(player));
      when(snapshotQuery.findMainRegionNameForPlayer(eq(player.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of("EU"));
      when(identityRepository.findByStatus(any())).thenReturn(List.of());

      PlayerQualityJobResult result = service.runDailyQualityJob();

      assertThat(result.mainRegionsUpdated()).isEqualTo(1);
      verify(playerRepository, times(1)).save(player);
    }

    @Test
    @DisplayName("Does not update player region when it matches computed main region")
    void noUpdateWhenRegionUnchanged() {
      Player player = playerWithRegion(PlayerRegion.EU);
      when(playerRepository.findAll()).thenReturn(List.of(player));
      when(snapshotQuery.findMainRegionNameForPlayer(eq(player.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of("EU"));
      when(identityRepository.findByStatus(any())).thenReturn(List.of());

      PlayerQualityJobResult result = service.runDailyQualityJob();

      assertThat(result.mainRegionsUpdated()).isZero();
      verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Skips player with no snapshot data (no region update)")
    void skipsPlayerWithNoSnapshotData() {
      Player player = playerWithRegion(PlayerRegion.EU);
      when(playerRepository.findAll()).thenReturn(List.of(player));
      when(snapshotQuery.findMainRegionNameForPlayer(any(), any())).thenReturn(Optional.empty());
      when(identityRepository.findByStatus(any())).thenReturn(List.of());

      PlayerQualityJobResult result = service.runDailyQualityJob();

      assertThat(result.mainRegionsUpdated()).isZero();
    }
  }

  @Nested
  @DisplayName("FR-08: Duplicate epicId detection")
  class DuplicateEpicIdDetection {

    @Test
    @DisplayName("Detects players sharing the same epicId")
    void detectsDuplicateEpicId() {
      UUID player1 = UUID.randomUUID();
      UUID player2 = UUID.randomUUID();
      when(playerRepository.findAll()).thenReturn(List.of());
      when(identityRepository.findByStatus(IdentityStatus.RESOLVED))
          .thenReturn(
              List.of(resolvedEntry(player1, "EPIC-001"), resolvedEntry(player2, "EPIC-001")));
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of());

      PlayerQualityJobResult result = service.runDailyQualityJob();

      assertThat(result.duplicateEpicIdsDetected()).isEqualTo(1);
    }

    @Test
    @DisplayName("Reports zero duplicates when all epicIds are unique")
    void noDuplicatesWhenEpicIdsAreUnique() {
      UUID player1 = UUID.randomUUID();
      UUID player2 = UUID.randomUUID();
      when(playerRepository.findAll()).thenReturn(List.of());
      when(identityRepository.findByStatus(IdentityStatus.RESOLVED))
          .thenReturn(
              List.of(resolvedEntry(player1, "EPIC-001"), resolvedEntry(player2, "EPIC-002")));
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of());

      PlayerQualityJobResult result = service.runDailyQualityJob();

      assertThat(result.duplicateEpicIdsDetected()).isZero();
    }
  }

  @Nested
  @DisplayName("FR-09: Stale UNRESOLVED alert")
  class StaleUnresolvedAlert {

    @Test
    @DisplayName("Alerts for UNRESOLVED entries older than 24h")
    void alertsForStaleUnresolvedEntries() {
      when(playerRepository.findAll()).thenReturn(List.of());
      when(identityRepository.findByStatus(IdentityStatus.RESOLVED)).thenReturn(List.of());
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(List.of(unresolvedEntry(25), unresolvedEntry(48)));

      PlayerQualityJobResult result = service.runDailyQualityJob();

      assertThat(result.staleUnresolvedAlerted()).isEqualTo(2);
    }

    @Test
    @DisplayName("Does not alert for UNRESOLVED entries younger than 24h")
    void noAlertForRecentUnresolvedEntries() {
      when(playerRepository.findAll()).thenReturn(List.of());
      when(identityRepository.findByStatus(IdentityStatus.RESOLVED)).thenReturn(List.of());
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(List.of(unresolvedEntry(1), unresolvedEntry(23)));

      PlayerQualityJobResult result = service.runDailyQualityJob();

      assertThat(result.staleUnresolvedAlerted()).isZero();
    }
  }
}
