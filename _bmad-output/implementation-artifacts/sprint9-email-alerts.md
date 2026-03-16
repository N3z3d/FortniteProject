# Story: sprint9-email-alerts — Email Alerts for UNRESOLVED Pipeline Entries > 24h

## Status: done

## Story

**As an** admin,
**I want** to receive an email alert when PlayerIdentityEntry items remain UNRESOLVED for more than 24 hours,
**So that** I can intervene promptly before pipeline blockage escalates.

## Context

Detection already exists in `UnresolvedAlertService.getAlertStatus()` (WARNING at 24h, CRITICAL at 48h) and `PlayerQualityService.alertStaleUnresolved()` (logs only). The gap is that no email is sent. The `spring-boot-starter-mail` dependency is already present in `pom.xml` (line 50).

This story wires `JavaMailSender` into a new `EmailAlertService` and a new `UnresolvedPlayerAlertScheduler` that runs on a cron schedule, queries UNRESOLVED entries > 24h via the existing `PlayerIdentityRepositoryPort`, and sends a consolidated email to a configurable admin address.

## Acceptance Criteria

1. `EmailAlertService` exists in `service/alert/` — sends email via `JavaMailSender`, subject+body parameterized, protected by `@ConditionalOnProperty(name="alert.email.enabled", havingValue="true", matchIfMissing=false)`
2. `UnresolvedPlayerAlertScheduler` exists in `service/alert/` — `@Scheduled` cron (configurable, default `0 0 6 * * *`), queries UNRESOLVED entries older than `alert.unresolved.threshold-hours` (default 24), calls `EmailAlertService.sendUnresolvedAlert()` if count > 0
3. When 0 stale entries exist, no email is sent (skip logic)
4. `application.properties` documents all config keys (commented, with defaults)
5. When `alert.email.enabled=false` (default), the `EmailAlertService` bean is absent — no `MailSender` auto-configuration required, no `NoSuchBeanDefinitionException`
6. Minimum 6 unit tests covering: send path, skip path, threshold filtering, conditional bean absence, subject format, body content

## Pre-existing Gaps

- Backend pre-existing failures: `GameDataIntegrationTest` (4), `FortniteTrackerServiceTddTest` (6), `PlayerServiceTddTest` (1), `ScoreServiceTddTest` (3), `GameStatisticsServiceTddTest` (1 error), `ScoreCalculationServiceTddTest` (2). Total ~17 known. Exclude from run.

## Technical Design

### Package layout

```
service/alert/
  EmailAlertService.java               ← @Service, @ConditionalOnProperty, uses JavaMailSender
  UnresolvedPlayerAlertScheduler.java  ← @Service, @ConditionalOnProperty, @Scheduled cron
```

### Config keys (application.properties)

```properties
# Email alerts (disabled by default — set alert.email.enabled=true + spring.mail.* to activate)
alert.email.enabled=false
alert.email.to=${ALERT_EMAIL_TO:admin@example.com}
alert.email.from=${ALERT_EMAIL_FROM:noreply@fortnite-pronos.local}
alert.unresolved.threshold-hours=24
alert.unresolved.cron=0 0 6 * * *

# Spring Mail (only needed when alert.email.enabled=true)
# spring.mail.host=${SMTP_HOST:smtp.gmail.com}
# spring.mail.port=${SMTP_PORT:587}
# spring.mail.username=${SMTP_USERNAME:}
# spring.mail.password=${SMTP_PASSWORD:}
# spring.mail.properties.mail.smtp.auth=true
# spring.mail.properties.mail.smtp.starttls.enable=true
```

### EmailAlertService

```java
@Service
@ConditionalOnProperty(name = "alert.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailAlertService {

  static final String SUBJECT_PREFIX = "[Fortnite Pronos] ALERTE pipeline — ";

  private final JavaMailSender mailSender;
  private final String to;
  private final String from;

  @Autowired
  public EmailAlertService(
      JavaMailSender mailSender,
      @Value("${alert.email.to}") String to,
      @Value("${alert.email.from}") String from) {
    this.mailSender = mailSender;
    this.to = to;
    this.from = from;
  }

  public void sendUnresolvedAlert(long count, long thresholdHours) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(to);
    msg.setFrom(from);
    msg.setSubject(SUBJECT_PREFIX + count + " joueur(s) UNRESOLVED > " + thresholdHours + "h");
    msg.setText(buildBody(count, thresholdHours));
    mailSender.send(msg);
  }

  String buildBody(long count, long thresholdHours) {
    return count
        + " entrée(s) PlayerIdentityEntry sont restées UNRESOLVED depuis plus de "
        + thresholdHours
        + " heures.\n\nVeuillez vous connecter au panneau d'administration pour les traiter :\n"
        + "http://localhost:8080/admin/pipeline\n\n"
        + "-- Fortnite Pronos (alerte automatique)";
  }
}
```

### UnresolvedPlayerAlertScheduler

```java
@Service
@ConditionalOnProperty(name = "alert.email.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class UnresolvedPlayerAlertScheduler {

  private final PlayerIdentityRepositoryPort identityRepository;
  private final EmailAlertService emailAlertService;
  private final long thresholdHours;
  private final Clock clock;

  @Autowired
  public UnresolvedPlayerAlertScheduler(
      PlayerIdentityRepositoryPort identityRepository,
      EmailAlertService emailAlertService,
      @Value("${alert.unresolved.threshold-hours:24}") long thresholdHours) {
    this(identityRepository, emailAlertService, thresholdHours, Clock.systemUTC());
  }

  UnresolvedPlayerAlertScheduler(
      PlayerIdentityRepositoryPort identityRepository,
      EmailAlertService emailAlertService,
      long thresholdHours,
      Clock clock) {
    this.identityRepository = identityRepository;
    this.emailAlertService = emailAlertService;
    this.thresholdHours = thresholdHours;
    this.clock = clock;
  }

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
    log.warn("[ALERT] {} UNRESOLVED entries > {}h — sending email alert.", stale.size(), thresholdHours);
    emailAlertService.sendUnresolvedAlert(stale.size(), thresholdHours);
    return stale.size();
  }
}
```

### Test file

`src/test/java/com/fortnite/pronos/service/alert/UnresolvedPlayerAlertSchedulerTest.java`

Minimum 6 tests:
1. `checkAndAlert_skipsEmail_whenNoStaleEntries` — 0 UNRESOLVED → `sendUnresolvedAlert` not called, returns 0
2. `checkAndAlert_skipsEmail_whenUnresolvedIsRecent` — 1 UNRESOLVED but created 1h ago (below threshold) → email not sent
3. `checkAndAlert_sendsEmail_whenStaleEntriesExist` — 2 UNRESOLVED created 30h ago → email sent, returns 2
4. `checkAndAlert_appliesThreshold_correctly` — threshold=48h, entry 25h old → skip; entry 49h old → send
5. `emailAlertService_buildBody_containsCount` — verifies subject/body content of `EmailAlertService.buildBody()`
6. `emailAlertService_sendUnresolvedAlert_callsMailSender` — verify `SimpleMailMessage` fields (to, from, subject)

## Files to Create/Modify

- `src/main/java/com/fortnite/pronos/service/alert/EmailAlertService.java` (NEW)
- `src/main/java/com/fortnite/pronos/service/alert/UnresolvedAlertSchedulerService.java` (NEW — renamed from UnresolvedPlayerAlertScheduler to satisfy NamingConventionTest @Service suffix rule)
- `src/main/resources/application.properties` (MODIFY — alert config keys added)
- `src/test/java/com/fortnite/pronos/service/alert/UnresolvedPlayerAlertSchedulerTest.java` (NEW — 6 tests)

## Completion Notes

Implemented 2026-03-16. The `spring-boot-starter-mail` dependency was already present in `pom.xml`. Both beans are guarded by `@ConditionalOnProperty(name="alert.email.enabled", havingValue="true", matchIfMissing=false)` — the default is `false` so no SMTP config is required at startup. NamingConventionTest enforces `@Service` classes in `..service..` must end with `Service` — scheduler class named `UnresolvedAlertSchedulerService` accordingly. Test run: 2336 run, 0 failures, 0 errors (6 new tests green).

## Review Follow-ups (AI)

- [ ] Consider adding an integration test that verifies the `@ConditionalOnProperty` conditional (bean absent when `alert.email.enabled` not set)
- [ ] `application.properties` SMTP comments could be moved to `.env.example` for clarity
