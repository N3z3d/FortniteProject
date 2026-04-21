package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.player.model.FortnitePlayerData;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.ResolutionPort;
import com.fortnite.pronos.service.admin.ConfidenceScoreService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResolutionQueueService")
class ResolutionQueueServiceTest {

  @Mock private ResolutionPort resolutionPort;
  @Mock private PlayerIdentityRepositoryPort identityRepository;
  @Mock private ConfidenceScoreService confidenceScoreService;

  private ResolutionQueueService service;

  @BeforeEach
  void setUp() {
    service =
        new ResolutionQueueService(resolutionPort, identityRepository, confidenceScoreService);
  }

  private PlayerIdentityEntry unresolvedEntry(String pseudo, String region) {
    return new PlayerIdentityEntry(UUID.randomUUID(), pseudo, region, LocalDateTime.now());
  }

  private FortnitePlayerData playerData(String epicId, String displayName) {
    return new FortnitePlayerData(epicId, displayName, 0, 0, 0, 0, 0.0, 0.0, 0);
  }

  @Nested
  @DisplayName("Nominal batch processing")
  class NominalBatch {

    @Test
    @DisplayName("Resolves all UNRESOLVED entries and saves them")
    void shouldResolveAllEntriesInBatch() {
      var e1 = unresolvedEntry("PlayerA", "EU");
      var e2 = unresolvedEntry("PlayerB", "NAC");
      var e3 = unresolvedEntry("PlayerC", "BR");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(List.of(e1, e2, e3));
      when(resolutionPort.resolvePlayer(any(), any()))
          .thenReturn(Optional.of(playerData("EPIC-001", "PlayerA")))
          .thenReturn(Optional.of(playerData("EPIC-002", "PlayerB")))
          .thenReturn(Optional.of(playerData("EPIC-003", "PlayerC")));
      when(confidenceScoreService.compute(any(), any(), any())).thenReturn(85);

      int resolved = service.processUnresolvedBatch();

      assertThat(resolved).isEqualTo(3);
      verify(identityRepository, times(3)).save(any());
      assertThat(e1.getStatus()).isEqualTo(IdentityStatus.RESOLVED);
      assertThat(e1.getEpicId()).isEqualTo("EPIC-001");
      assertThat(e2.getStatus()).isEqualTo(IdentityStatus.RESOLVED);
      assertThat(e3.getStatus()).isEqualTo(IdentityStatus.RESOLVED);
    }

    @Test
    @DisplayName("Uses real confidence score from ConfidenceScoreService (not hardcoded 100)")
    void shouldUseRealConfidenceScore() {
      var entry = unresolvedEntry("Bugha", "EU");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of(entry));
      when(resolutionPort.resolvePlayer("Bugha", "EU"))
          .thenReturn(Optional.of(playerData("bugha-epic-123", "Bugha")));
      when(confidenceScoreService.compute(entry, "bugha-epic-123", "Bugha")).thenReturn(95);

      service.processUnresolvedBatch();

      assertThat(entry.getConfidenceScore()).isEqualTo(95);
      verify(confidenceScoreService).compute(entry, "bugha-epic-123", "Bugha");
    }

    @Test
    @DisplayName("Returns 0 when no UNRESOLVED entries exist")
    void shouldReturnZeroWhenNothingToProcess() {
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of());

      int resolved = service.processUnresolvedBatch();

      assertThat(resolved).isZero();
      verify(resolutionPort, never()).resolvePlayer(any(), any());
      verify(identityRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Partial resolution")
  class PartialResolution {

    @Test
    @DisplayName("Continues batch when one entry stays unresolved")
    void shouldContinueBatchWhenOneEntryUnresolved() {
      var resolved = unresolvedEntry("ResolvedPlayer", "EU");
      var unresolved = unresolvedEntry("UnknownPlayer", "ME");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(List.of(resolved, unresolved));
      when(resolutionPort.resolvePlayer("ResolvedPlayer", "EU"))
          .thenReturn(Optional.of(playerData("EPIC-XYZ", "ResolvedPlayer")));
      when(resolutionPort.resolvePlayer("UnknownPlayer", "ME")).thenReturn(Optional.empty());
      when(confidenceScoreService.compute(any(), any(), any())).thenReturn(80);

      int count = service.processUnresolvedBatch();

      assertThat(count).isEqualTo(1);
      verify(identityRepository, times(1)).save(resolved);
      verify(identityRepository, never()).save(unresolved);
      assertThat(resolved.getStatus()).isEqualTo(IdentityStatus.RESOLVED);
      assertThat(unresolved.getStatus()).isEqualTo(IdentityStatus.UNRESOLVED);
    }
  }

  @Nested
  @DisplayName("Error resilience")
  class ErrorResilience {

    @Test
    @DisplayName("Continues batch when ResolutionPort throws a technical exception")
    void shouldContinueBatchOnTechnicalException() {
      var failing = unresolvedEntry("FailPlayer", "OCE");
      var ok = unresolvedEntry("OkPlayer", "ASIA");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(List.of(failing, ok));
      when(resolutionPort.resolvePlayer("FailPlayer", "OCE"))
          .thenThrow(new RuntimeException("timeout"));
      when(resolutionPort.resolvePlayer("OkPlayer", "ASIA"))
          .thenReturn(Optional.of(playerData("EPIC-OK", "OkPlayer")));
      when(confidenceScoreService.compute(any(), any(), any())).thenReturn(75);

      int count = service.processUnresolvedBatch();

      assertThat(count).isEqualTo(1);
      verify(identityRepository, times(1)).save(ok);
      assertThat(failing.getStatus()).isEqualTo(IdentityStatus.UNRESOLVED);
    }

    @Test
    @DisplayName("Skips entry with blank pseudo without calling ResolutionPort")
    void shouldSkipEntryWithBlankPseudo() {
      var invalid = unresolvedEntry("  ", "EU");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of(invalid));

      int count = service.processUnresolvedBatch();

      assertThat(count).isZero();
      verify(resolutionPort, never()).resolvePlayer(any(), any());
      verify(identityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Keeps entry unresolved when resolved player data has null Epic Account ID")
    void shouldSkipResolvedDataWithNullEpicAccountId() {
      var entry = unresolvedEntry("Bugha", "EU");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of(entry));
      when(resolutionPort.resolvePlayer("Bugha", "EU"))
          .thenReturn(Optional.of(playerData(null, "Bugha")));

      int count = service.processUnresolvedBatch();

      assertThat(count).isZero();
      assertThat(entry.getStatus()).isEqualTo(IdentityStatus.UNRESOLVED);
      assertThat(entry.getEpicId()).isNull();
      verifyNoInteractions(confidenceScoreService);
      verify(identityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Keeps entry unresolved when resolved player data has blank Epic Account ID")
    void shouldSkipResolvedDataWithBlankEpicAccountId() {
      var entry = unresolvedEntry("Aqua", "EU");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of(entry));
      when(resolutionPort.resolvePlayer("Aqua", "EU"))
          .thenReturn(Optional.of(playerData("  ", "Aqua")));

      int count = service.processUnresolvedBatch();

      assertThat(count).isZero();
      assertThat(entry.getStatus()).isEqualTo(IdentityStatus.UNRESOLVED);
      assertThat(entry.getEpicId()).isNull();
      verifyNoInteractions(confidenceScoreService);
      verify(identityRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Port swappability (DIP)")
  class PortSwappability {

    @Test
    @DisplayName("Service works identically with any ResolutionPort implementation")
    void shouldWorkWithAnyResolutionPortImplementation() {
      ConfidenceScoreService realScoreService = new ConfidenceScoreService();
      ResolutionPort alternativeAdapter =
          (pseudo, region) ->
              Optional.of(new FortnitePlayerData("ALT-" + pseudo, pseudo, 0, 0, 0, 0, 0.0, 0.0, 0));
      var svc =
          new ResolutionQueueService(alternativeAdapter, identityRepository, realScoreService);
      var entry = unresolvedEntry("TestPlayer", "NAW");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of(entry));

      int count = svc.processUnresolvedBatch();

      assertThat(count).isEqualTo(1);
      assertThat(entry.getEpicId()).isEqualTo("ALT-TestPlayer");
      verify(identityRepository, times(1)).save(entry);
    }
  }
}
