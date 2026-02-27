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
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.domain.port.out.ResolutionPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResolutionQueueService")
class ResolutionQueueServiceTest {

  @Mock private ResolutionPort resolutionPort;
  @Mock private PlayerIdentityRepositoryPort identityRepository;

  private ResolutionQueueService service;

  @BeforeEach
  void setUp() {
    service = new ResolutionQueueService(resolutionPort, identityRepository);
  }

  private PlayerIdentityEntry unresolvedEntry(String pseudo, String region) {
    return new PlayerIdentityEntry(UUID.randomUUID(), pseudo, region, LocalDateTime.now());
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
      when(resolutionPort.resolveFortniteId(any(), any()))
          .thenReturn(Optional.of("EPIC-001"))
          .thenReturn(Optional.of("EPIC-002"))
          .thenReturn(Optional.of("EPIC-003"));

      int resolved = service.processUnresolvedBatch();

      assertThat(resolved).isEqualTo(3);
      verify(identityRepository, times(3)).save(any());
      assertThat(e1.getStatus()).isEqualTo(IdentityStatus.RESOLVED);
      assertThat(e1.getEpicId()).isEqualTo("EPIC-001");
      assertThat(e2.getStatus()).isEqualTo(IdentityStatus.RESOLVED);
      assertThat(e3.getStatus()).isEqualTo(IdentityStatus.RESOLVED);
    }

    @Test
    @DisplayName("Returns 0 when no UNRESOLVED entries exist")
    void shouldReturnZeroWhenNothingToProcess() {
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of());

      int resolved = service.processUnresolvedBatch();

      assertThat(resolved).isZero();
      verify(resolutionPort, never()).resolveFortniteId(any(), any());
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
      when(resolutionPort.resolveFortniteId("ResolvedPlayer", "EU"))
          .thenReturn(Optional.of("EPIC-XYZ"));
      when(resolutionPort.resolveFortniteId("UnknownPlayer", "ME")).thenReturn(Optional.empty());

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
      when(resolutionPort.resolveFortniteId("FailPlayer", "OCE"))
          .thenThrow(new RuntimeException("timeout"));
      when(resolutionPort.resolveFortniteId("OkPlayer", "ASIA")).thenReturn(Optional.of("EPIC-OK"));

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
      verify(resolutionPort, never()).resolveFortniteId(any(), any());
      verify(identityRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Port swappability (DIP)")
  class PortSwappability {

    @Test
    @DisplayName("Service works identically with any ResolutionPort implementation")
    void shouldWorkWithAnyResolutionPortImplementation() {
      ResolutionPort alternativeAdapter = (pseudo, region) -> Optional.of("ALT-" + pseudo);
      var svc = new ResolutionQueueService(alternativeAdapter, identityRepository);
      var entry = unresolvedEntry("TestPlayer", "NAW");
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of(entry));

      int count = svc.processUnresolvedBatch();

      assertThat(count).isEqualTo(1);
      assertThat(entry.getEpicId()).isEqualTo("ALT-TestPlayer");
      verify(identityRepository, times(1)).save(entry);
    }
  }
}
