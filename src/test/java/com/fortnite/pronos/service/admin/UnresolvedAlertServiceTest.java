package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.dto.admin.PipelineAlertDto;
import com.fortnite.pronos.dto.admin.PipelineAlertDto.AlertLevel;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnresolvedAlertService")
class UnresolvedAlertServiceTest {

  @Mock private PlayerIdentityRepositoryPort identityRepository;

  private static final Instant FIXED_NOW = Instant.parse("2026-03-01T12:00:00Z");
  private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

  private UnresolvedAlertService service() {
    return new UnresolvedAlertService(identityRepository, FIXED_CLOCK);
  }

  private LocalDateTime hoursAgo(long hours) {
    return LocalDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC).minusHours(hours);
  }

  @Nested
  @DisplayName("getAlertStatus")
  class GetAlertStatus {

    @Test
    @DisplayName("returns NONE when no unresolved entries")
    void noUnresolved() {
      when(identityRepository.countByStatus(IdentityStatus.UNRESOLVED)).thenReturn(0L);

      PipelineAlertDto result = service().getAlertStatus();

      assertThat(result.level()).isEqualTo(AlertLevel.NONE);
      assertThat(result.unresolvedCount()).isZero();
      assertThat(result.oldestUnresolvedAt()).isNull();
      assertThat(result.elapsedHours()).isZero();
      assertThat(result.checkedAt()).isNotNull();
    }

    @Test
    @DisplayName("returns NONE when oldest unresolved is less than 24h ago")
    void recentUnresolved() {
      when(identityRepository.countByStatus(IdentityStatus.UNRESOLVED)).thenReturn(3L);
      when(identityRepository.findOldestCreatedAtByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(Optional.of(hoursAgo(10)));

      PipelineAlertDto result = service().getAlertStatus();

      assertThat(result.level()).isEqualTo(AlertLevel.NONE);
      assertThat(result.unresolvedCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("returns WARNING when oldest unresolved is exactly 24h ago")
    void exactly24Hours() {
      when(identityRepository.countByStatus(IdentityStatus.UNRESOLVED)).thenReturn(2L);
      when(identityRepository.findOldestCreatedAtByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(Optional.of(hoursAgo(24)));

      PipelineAlertDto result = service().getAlertStatus();

      assertThat(result.level()).isEqualTo(AlertLevel.WARNING);
      assertThat(result.elapsedHours()).isEqualTo(24L);
      assertThat(result.oldestUnresolvedAt()).isInstanceOf(OffsetDateTime.class);
    }

    @Test
    @DisplayName("returns CRITICAL when oldest unresolved is 48h or more ago")
    void criticalAfter48Hours() {
      when(identityRepository.countByStatus(IdentityStatus.UNRESOLVED)).thenReturn(5L);
      when(identityRepository.findOldestCreatedAtByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(Optional.of(hoursAgo(72)));

      PipelineAlertDto result = service().getAlertStatus();

      assertThat(result.level()).isEqualTo(AlertLevel.CRITICAL);
      assertThat(result.elapsedHours()).isEqualTo(72L);
      assertThat(result.checkedAt()).isEqualTo(OffsetDateTime.ofInstant(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("returns NONE when 23h59m elapsed (just under threshold)")
    void justUnderWarningThreshold() {
      when(identityRepository.countByStatus(IdentityStatus.UNRESOLVED)).thenReturn(1L);
      LocalDateTime almostThreshold = hoursAgo(23).minusMinutes(59);
      when(identityRepository.findOldestCreatedAtByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(Optional.of(almostThreshold));

      PipelineAlertDto result = service().getAlertStatus();

      assertThat(result.level()).isEqualTo(AlertLevel.NONE);
    }

    @Test
    @DisplayName("returns NONE when oldest is absent despite non-zero count")
    void emptyOldestFallback() {
      when(identityRepository.countByStatus(IdentityStatus.UNRESOLVED)).thenReturn(1L);
      when(identityRepository.findOldestCreatedAtByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(Optional.empty());

      PipelineAlertDto result = service().getAlertStatus();

      assertThat(result.level()).isEqualTo(AlertLevel.NONE);
      assertThat(result.unresolvedCount()).isEqualTo(1L);
    }
  }
}
