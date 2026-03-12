package com.fortnite.pronos.service.admin;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;
import com.fortnite.pronos.dto.admin.PipelineAlertDto;
import com.fortnite.pronos.dto.admin.PipelineAlertDto.AlertLevel;

@Service
public class UnresolvedAlertService {

  static final long WARNING_THRESHOLD_HOURS = 24L;
  static final long CRITICAL_THRESHOLD_HOURS = 48L;

  private final PlayerIdentityRepositoryPort identityRepository;
  private final Clock clock;

  @Autowired
  public UnresolvedAlertService(PlayerIdentityRepositoryPort identityRepository) {
    this(identityRepository, Clock.systemUTC());
  }

  UnresolvedAlertService(PlayerIdentityRepositoryPort identityRepository, Clock clock) {
    this.identityRepository = identityRepository;
    this.clock = clock;
  }

  public PipelineAlertDto getAlertStatus() {
    OffsetDateTime checkedAt = OffsetDateTime.now(clock);
    long unresolvedCount = identityRepository.countByStatus(IdentityStatus.UNRESOLVED);

    if (unresolvedCount == 0) {
      return new PipelineAlertDto(AlertLevel.NONE, 0L, null, 0L, checkedAt);
    }

    Optional<LocalDateTime> oldest =
        identityRepository.findOldestCreatedAtByStatus(IdentityStatus.UNRESOLVED);
    if (oldest.isEmpty()) {
      return new PipelineAlertDto(AlertLevel.NONE, unresolvedCount, null, 0L, checkedAt);
    }

    LocalDateTime oldestAt = oldest.get();
    LocalDateTime now = LocalDateTime.now(clock);
    long elapsedHours = Duration.between(oldestAt, now).toHours();

    AlertLevel level;
    if (elapsedHours >= CRITICAL_THRESHOLD_HOURS) {
      level = AlertLevel.CRITICAL;
    } else if (elapsedHours >= WARNING_THRESHOLD_HOURS) {
      level = AlertLevel.WARNING;
    } else {
      level = AlertLevel.NONE;
    }

    return new PipelineAlertDto(
        level, unresolvedCount, oldestAt.atOffset(ZoneOffset.UTC), elapsedHours, checkedAt);
  }
}
