package com.fortnite.pronos.service.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.fortnite.pronos.domain.player.identity.model.IdentityStatus;
import com.fortnite.pronos.domain.player.identity.model.MetadataCorrection;
import com.fortnite.pronos.domain.player.identity.model.PlayerIdentityEntry;
import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnresolvedAlertSchedulerService")
class UnresolvedAlertSchedulerServiceTest {

  @Mock private PlayerIdentityRepositoryPort identityRepository;
  @Mock private EmailAlertService emailAlertService;
  @Mock private JavaMailSender mailSender;

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-03-16T10:00:00Z"), ZoneOffset.UTC);

  private static final long THRESHOLD_HOURS = 24L;

  private UnresolvedAlertSchedulerService scheduler;

  @BeforeEach
  void setUp() {
    scheduler =
        new UnresolvedAlertSchedulerService(
            identityRepository, emailAlertService, THRESHOLD_HOURS, FIXED_CLOCK);
  }

  private PlayerIdentityEntry unresolvedEntryCreatedAt(LocalDateTime createdAt) {
    return PlayerIdentityEntry.restore(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "TestPlayer",
        "EU",
        null,
        IdentityStatus.UNRESOLVED,
        50,
        null,
        null,
        null,
        null,
        createdAt,
        new MetadataCorrection(null, null, null, null));
  }

  @Nested
  @DisplayName("checkAndAlert")
  class CheckAndAlert {

    @Test
    @DisplayName("skips email when no UNRESOLVED entries exist")
    void skipsEmail_whenNoUnresolvedEntries() {
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of());

      int result = scheduler.checkAndAlert();

      assertThat(result).isZero();
      verify(emailAlertService, never()).sendUnresolvedAlert(anyLong(), anyLong());
    }

    @Test
    @DisplayName("skips email when UNRESOLVED entry is too recent (below threshold)")
    void skipsEmail_whenEntryIsRecent() {
      // Created 1 hour ago — below 24h threshold
      LocalDateTime recentCreatedAt = LocalDateTime.now(FIXED_CLOCK).minusHours(1);
      PlayerIdentityEntry recent = unresolvedEntryCreatedAt(recentCreatedAt);
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED)).thenReturn(List.of(recent));

      int result = scheduler.checkAndAlert();

      assertThat(result).isZero();
      verify(emailAlertService, never()).sendUnresolvedAlert(anyLong(), anyLong());
    }

    @Test
    @DisplayName("sends email when stale UNRESOLVED entries exist (> threshold hours)")
    void sendsEmail_whenStaleEntriesExist() {
      // Created 30 hours ago — above 24h threshold
      LocalDateTime staleCreatedAt = LocalDateTime.now(FIXED_CLOCK).minusHours(30);
      PlayerIdentityEntry stale1 = unresolvedEntryCreatedAt(staleCreatedAt);
      PlayerIdentityEntry stale2 = unresolvedEntryCreatedAt(staleCreatedAt.minusHours(5));
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(List.of(stale1, stale2));

      int result = scheduler.checkAndAlert();

      assertThat(result).isEqualTo(2);
      verify(emailAlertService).sendUnresolvedAlert(2L, THRESHOLD_HOURS);
    }

    @Test
    @DisplayName("applies threshold correctly — filters mixed stale and recent entries")
    void appliesThreshold_correctly_withMixedEntries() {
      LocalDateTime staleCreatedAt = LocalDateTime.now(FIXED_CLOCK).minusHours(49);
      LocalDateTime recentCreatedAt = LocalDateTime.now(FIXED_CLOCK).minusHours(23);
      PlayerIdentityEntry stale = unresolvedEntryCreatedAt(staleCreatedAt);
      PlayerIdentityEntry recent = unresolvedEntryCreatedAt(recentCreatedAt);
      when(identityRepository.findByStatus(IdentityStatus.UNRESOLVED))
          .thenReturn(List.of(stale, recent));

      int result = scheduler.checkAndAlert();

      assertThat(result).isEqualTo(1);
      verify(emailAlertService).sendUnresolvedAlert(1L, THRESHOLD_HOURS);
    }
  }

  @Nested
  @DisplayName("EmailAlertService")
  class EmailAlertServiceTests {

    private EmailAlertService alertService;

    @BeforeEach
    void setUpAlertService() {
      alertService = new EmailAlertService(mailSender, "admin@example.com", "noreply@example.com");
    }

    @Test
    @DisplayName("buildBody contains count and threshold hours")
    void buildBody_containsCountAndThreshold() {
      String body = alertService.buildBody(5L, 24L);

      assertThat(body).contains("5");
      assertThat(body).contains("24");
      assertThat(body).contains("UNRESOLVED");
      assertThat(body).contains("/admin/pipeline");
    }

    @Test
    @DisplayName("sendUnresolvedAlert sets correct To, From, Subject on SimpleMailMessage")
    void sendUnresolvedAlert_setsCorrectEmailFields() {
      ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

      alertService.sendUnresolvedAlert(3L, 24L);

      verify(mailSender).send(captor.capture());
      SimpleMailMessage msg = captor.getValue();
      assertThat(msg.getTo()).containsExactly("admin@example.com");
      assertThat(msg.getFrom()).isEqualTo("noreply@example.com");
      assertThat(msg.getSubject()).contains("3").contains("UNRESOLVED").contains("24h");
    }
  }
}
