package com.fortnite.pronos.service.alert;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;

import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job that checks for stale UNRESOLVED pipeline entries and triggers an email alert. Only
 * active when {@code alert.email.enabled=true} is configured.
 */
@Service
@ConditionalOnProperty(name = "alert.email.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class UnresolvedAlertSchedulerService {

  private final PlayerIdentityRepositoryPort identityRepository;
  private final EmailAlertService emailAlertService;
  private final long thresholdHours;
  private final Clock clock;

  @Autowired
  public UnresolvedAlertSchedulerService(
      PlayerIdentityRepositoryPort identityRepository,
      EmailAlertService emailAlertService,
      @Value("${alert.unresolved.threshold-hours:24}") long thresholdHours) {
    this(identityRepository, emailAlertService, thresholdHours, Clock.systemUTC());
  }

  UnresolvedAlertSchedulerService(
      PlayerIdentityRepositoryPort identityRepository,
      EmailAlertService emailAlertService,
      long thresholdHours,
      Clock clock) {
    this.identityRepository = identityRepository;
    this.emailAlertService = emailAlertService;
    this.thresholdHours = thresholdHours;
    this.clock = clock;
  }

  /**
   * Runs on a configurable cron schedule (default: daily at 06:00 UTC). Queries UNRESOLVED entries
   * older than {@code thresholdHours} and sends an email alert if any are found.
   *
   * @return number of stale entries found (0 if none)
   */
  @Scheduled(cron = "${alert.unresolved.cron:0 0 6 * * *}")
  public int checkAndAlert() {
    LocalDateTime threshold = LocalDateTime.now(clock).minusHours(thresholdHours);
    List<PlayerIdentityEntry> stale =
        identityRepository.findByStatus(IdentityStatus.UNRESOLVED).stream()
            .filter(e -> e.getCreatedAt().isBefore(threshold))
            .toList();
    if (stale.isEmpty()) {
      log.debug("No stale UNRESOLVED entries found — skipping email alert.");
      return 0;
    }
    log.warn(
        "[ALERT] {} UNRESOLVED entries > {}h — sending email alert.", stale.size(), thresholdHours);
    emailAlertService.sendUnresolvedAlert(stale.size(), thresholdHours);
    return stale.size();
  }
}
