package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.port.out.EpicIdValidatorPort;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.dto.admin.PipelineCountResponse;
import com.fortnite.pronos.dto.admin.PlayerIdentityEntryResponse;
import com.fortnite.pronos.exception.InvalidEpicIdException;
import com.fortnite.pronos.exception.PlayerIdentityNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerIdentityPipelineService")
class PlayerIdentityPipelineServiceTest {

  @Mock private PlayerIdentityRepositoryPort identityRepository;
  @Mock private SimpMessagingTemplate messagingTemplate;
  @Mock private ConfidenceScoreService confidenceScoreService;
  @Mock private EpicIdValidatorPort epicIdValidator;

  @InjectMocks private PlayerIdentityPipelineService service;

  // ===== Helpers =====

  private PlayerIdentityEntry buildUnresolved(UUID playerId, String username) {
    return new PlayerIdentityEntry(playerId, username, "EU", LocalDateTime.now());
  }

  private PlayerIdentityEntry buildRestored(
      UUID playerId, String username, IdentityStatus status, String epicId) {
    return PlayerIdentityEntry.restore(
        UUID.randomUUID(),
        playerId,
        username,
        "EU",
        epicId,
        status,
        85,
        "admin",
        LocalDateTime.now(),
        null,
        null,
        LocalDateTime.now().minusDays(1));
  }

  @Nested
  @DisplayName("getUnresolved")
  class GetUnresolved {

    @Test
    @DisplayName("returns mapped responses for UNRESOLVED entries")
    void returnsUnresolvedMapped() {
      UUID pid = UUID.randomUUID();
      when(identityRepository.findByStatusPaged(IdentityStatus.UNRESOLVED, 0, 50))
          .thenReturn(List.of(buildUnresolved(pid, "Bughaboo")));

      List<PlayerIdentityEntryResponse> result = service.getUnresolved();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).playerUsername()).isEqualTo("Bughaboo");
      assertThat(result.get(0).status()).isEqualTo("UNRESOLVED");
    }

    @Test
    @DisplayName("returns empty list when pipeline is empty")
    void returnsEmptyWhenNone() {
      when(identityRepository.findByStatusPaged(IdentityStatus.UNRESOLVED, 0, 50))
          .thenReturn(List.of());

      assertThat(service.getUnresolved()).isEmpty();
    }

    @Test
    @DisplayName("sanitizes negative page and non-positive size")
    void sanitizesPagingParameters() {
      when(identityRepository.findByStatusPaged(IdentityStatus.UNRESOLVED, 0, 50))
          .thenReturn(List.of());

      service.getUnresolved(-4, 0);

      verify(identityRepository).findByStatusPaged(IdentityStatus.UNRESOLVED, 0, 50);
    }
  }

  @Nested
  @DisplayName("getResolved")
  class GetResolved {

    @Test
    @DisplayName("returns RESOLVED entries with confidence score")
    void returnsResolvedWithScore() {
      UUID pid = UUID.randomUUID();
      when(identityRepository.findByStatusPaged(IdentityStatus.RESOLVED, 0, 50))
          .thenReturn(List.of(buildRestored(pid, "Alpha", IdentityStatus.RESOLVED, "alpha_fn")));

      List<PlayerIdentityEntryResponse> result = service.getResolved();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).confidenceScore()).isEqualTo(85);
      assertThat(result.get(0).epicId()).isEqualTo("alpha_fn");
    }
  }

  @Test
  @DisplayName("caps page size to 200")
  void capsPageSizeToMax() {
    when(identityRepository.findByStatusPaged(IdentityStatus.RESOLVED, 2, 200))
        .thenReturn(List.of());

    service.getResolved(2, 999);

    verify(identityRepository).findByStatusPaged(IdentityStatus.RESOLVED, 2, 200);
  }

  @Nested
  @DisplayName("getCount")
  class GetCount {

    @Test
    @DisplayName("returns aggregated unresolved and resolved counts")
    void returnsCorrectCounts() {
      when(identityRepository.countByStatus(IdentityStatus.UNRESOLVED)).thenReturn(5L);
      when(identityRepository.countByStatus(IdentityStatus.RESOLVED)).thenReturn(12L);

      var result = service.getCount();

      assertThat(result.unresolvedCount()).isEqualTo(5L);
      assertThat(result.resolvedCount()).isEqualTo(12L);
    }
  }

  @Nested
  @DisplayName("resolve")
  class Resolve {

    @Test
    @DisplayName("resolves entry, saves it, and broadcasts count")
    void resolvesAndBroadcasts() {
      UUID pid = UUID.randomUUID();
      PlayerIdentityEntry entry = buildUnresolved(pid, "Bughaboo");
      when(identityRepository.findByPlayerId(pid)).thenReturn(Optional.of(entry));
      when(epicIdValidator.validate("bughaboo_fn_1204")).thenReturn(true);
      when(confidenceScoreService.compute(entry, "bughaboo_fn_1204")).thenReturn(90);
      when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(identityRepository.countByStatus(any())).thenReturn(0L);

      PlayerIdentityEntryResponse result = service.resolve(pid, "bughaboo_fn_1204", "admin");

      assertThat(result.status()).isEqualTo("RESOLVED");
      assertThat(result.epicId()).isEqualTo("bughaboo_fn_1204");
      assertThat(result.resolvedBy()).isEqualTo("admin");
      verify(identityRepository).save(any());
      verify(messagingTemplate)
          .convertAndSend(
              eq(PlayerIdentityPipelineService.WS_TOPIC), any(PipelineCountResponse.class));
    }

    @Test
    @DisplayName("throws InvalidEpicIdException when Epic ID is invalid")
    void throwsOnInvalidEpicId() {
      UUID pid = UUID.randomUUID();
      when(epicIdValidator.validate("bad_id")).thenReturn(false);

      assertThatThrownBy(() -> service.resolve(pid, "bad_id", "admin"))
          .isInstanceOf(InvalidEpicIdException.class)
          .hasMessageContaining("bad_id");

      verify(identityRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws PlayerIdentityNotFoundException when entry does not exist")
    void throwsWhenEntryMissing() {
      UUID pid = UUID.randomUUID();
      when(epicIdValidator.validate(any())).thenReturn(true);
      when(identityRepository.findByPlayerId(pid)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.resolve(pid, "any_id", "admin"))
          .isInstanceOf(PlayerIdentityNotFoundException.class);
    }

    @Test
    @DisplayName("uses confidence score from ConfidenceScoreService")
    void usesConfidenceScore() {
      UUID pid = UUID.randomUUID();
      PlayerIdentityEntry entry = buildUnresolved(pid, "Player");
      when(identityRepository.findByPlayerId(pid)).thenReturn(Optional.of(entry));
      when(epicIdValidator.validate(any())).thenReturn(true);
      when(confidenceScoreService.compute(any(), any())).thenReturn(75);
      when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(identityRepository.countByStatus(any())).thenReturn(0L);

      PlayerIdentityEntryResponse result = service.resolve(pid, "player_epic", "admin");

      assertThat(result.confidenceScore()).isEqualTo(75);
    }
  }

  @Nested
  @DisplayName("reject")
  class Reject {

    @Test
    @DisplayName("rejects entry with reason and broadcasts count")
    void rejectsAndBroadcasts() {
      UUID pid = UUID.randomUUID();
      PlayerIdentityEntry entry = buildUnresolved(pid, "Unknown");
      when(identityRepository.findByPlayerId(pid)).thenReturn(Optional.of(entry));
      when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(identityRepository.countByStatus(any())).thenReturn(0L);

      PlayerIdentityEntryResponse result = service.reject(pid, "Joueur introuvable", "admin");

      assertThat(result.status()).isEqualTo("REJECTED");
      assertThat(result.rejectionReason()).isEqualTo("Joueur introuvable");
      verify(messagingTemplate)
          .convertAndSend(
              eq(PlayerIdentityPipelineService.WS_TOPIC), any(PipelineCountResponse.class));
    }

    @Test
    @DisplayName("accepts null reason on reject")
    void acceptsNullReason() {
      UUID pid = UUID.randomUUID();
      PlayerIdentityEntry entry = buildUnresolved(pid, "Unknown");
      when(identityRepository.findByPlayerId(pid)).thenReturn(Optional.of(entry));
      when(identityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(identityRepository.countByStatus(any())).thenReturn(0L);

      PlayerIdentityEntryResponse result = service.reject(pid, null, "admin");

      assertThat(result.status()).isEqualTo("REJECTED");
      assertThat(result.rejectionReason()).isNull();
    }

    @Test
    @DisplayName("throws PlayerIdentityNotFoundException when entry does not exist")
    void throwsWhenEntryMissing() {
      UUID pid = UUID.randomUUID();
      when(identityRepository.findByPlayerId(pid)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.reject(pid, null, "admin"))
          .isInstanceOf(PlayerIdentityNotFoundException.class);
    }
  }
}
