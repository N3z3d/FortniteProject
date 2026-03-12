# Story 7.3: Alerting escalant et logs scraping détaillés

Status: done

## Story

As an admin,
I want to see detailed scraping run logs and receive escalating alerts when UNRESOLVED entries persist too long,
so that I can detect pipeline problems early and act before data quality degrades.

## Acceptance Criteria

1. **Given** the admin calls `GET /api/admin/scraping/logs?limit=50`, **Then** the server returns up to 50 `IngestionRun` records sorted by `startedAt DESC` (200 OK), each with `id`, `source`, `startedAt`, `finishedAt`, `status` (RUNNING|PARTIAL|SUCCESS|FAILED), `totalRowsWritten`, `errorMessage`.

2. **Given** the admin calls `GET /api/admin/scraping/alert`, **Then** the server returns a `PipelineAlertDto` with `level` (NONE|WARNING|CRITICAL), `unresolvedCount`, `oldestUnresolvedAt` (null if none), `elapsedHours` (0 if none): `NONE` when 0 UNRESOLVED or oldest < 24h, `WARNING` when oldest ≥ 24h, `CRITICAL` when oldest ≥ 48h.

3. **Given** a non-admin user calls either endpoint, **Then** the server responds 403 Forbidden.

4. **Given** the admin opens `/admin/pipeline`, **When** the pipeline alert level is WARNING or CRITICAL, **Then** a prominent alert banner is shown above the tabs with the level, unresolved count, and elapsed hours.

5. **Given** the admin opens `/admin/pipeline`, **Then** a 4th "Logs scraping" tab appears with a table showing all `ScrapeLogEntry` columns (source, status chip with color coding, startedAt, finishedAt, totalRowsWritten, errorMessage).

6. **Given** a non-admin accesses the frontend admin routes, **Then** the existing `AdminGuard` redirects them (no change needed to routing).

## Tasks / Subtasks

- [x] Task 1: Backend — ScrapeLog endpoint (AC: #1, #3)
  - [x] 1.1: Create `ScrapeLogDto` record in `dto/admin/` — fields: `id` (UUID), `source` (String), `startedAt` (OffsetDateTime), `finishedAt` (OffsetDateTime nullable), `status` (String), `totalRowsWritten` (Integer nullable), `errorMessage` (String nullable)
  - [x] 1.2: Create `ScrapeLogService` in `service/admin/` — 1 dep: `IngestionRunRepository`; method `getRecentLogs(int limit)` returns `List<ScrapeLogDto>` sorted by `startedAt DESC` using `PageRequest.of(0, clampedLimit, Sort.by("startedAt").descending())`; clamp limit to [1, 200]
  - [x] 1.3: Create `AdminScrapeController` at `/api/admin/scraping` — `GET /logs?limit=50` delegates to `ScrapeLogService.getRecentLogs(limit)` (admin-restricted via SecurityConfig `/api/admin/**`)
  - [x] 1.4: Write tests: `ScrapeLogServiceTest` (6 tests), `AdminScrapeControllerTest` (4 tests for `/logs`)

- [x] Task 2: Backend — UnresolvedAlert endpoint (AC: #2, #3)
  - [x] 2.1: Add `Optional<LocalDateTime> findOldestCreatedAtByStatus(IdentityStatus status)` to `PlayerIdentityRepositoryPort`; implement in adapter + JPA repo (`@Query("SELECT MIN(e.createdAt)...")`)
  - [x] 2.2: Create `PipelineAlertDto` record in `dto/admin/` with nested `AlertLevel` enum (NONE|WARNING|CRITICAL)
  - [x] 2.3: Create `UnresolvedAlertService` in `service/admin/` — 2 deps: `PlayerIdentityRepositoryPort` + `Clock` (double-constructor pattern); escalation: 24h→WARNING, 48h→CRITICAL
  - [x] 2.4: Add `GET /alert` endpoint to `AdminScrapeController` — delegates to `UnresolvedAlertService.getAlertStatus()`
  - [x] 2.5: Write tests: `UnresolvedAlertServiceTest` (6 tests), `AdminScrapeControllerTest` (2 additional tests for `/alert`)

- [x] Task 3: Frontend — models + service (AC: #4, #5)
  - [x] 3.1: Add to `admin.models.ts`: `ScrapeLogEntry` interface + `PipelineAlertStatus` interface + `AlertLevel` type
  - [x] 3.2: Add to `pipeline.service.ts`: `getScrapeLog(limit = 50)` + `getUnresolvedAlertStatus()` methods (with `scrapeBaseUrl`)
  - [x] 3.3: Write tests in `pipeline.service.spec.ts`: 4 tests (getScrapeLog success/error, getUnresolvedAlertStatus success/error)

- [x] Task 4: Frontend — AdminPipelinePageComponent wiring (AC: #4, #5)
  - [x] 4.1: Add `scrapeLog: ScrapeLogEntry[] = []`, `pipelineAlert: PipelineAlertStatus | null = null`; extended `forkJoin` to include `getScrapeLog()` + `getUnresolvedAlertStatus()`
  - [x] 4.2: Alert banner in template above `<mat-tab-group>`: shown when `pipelineAlert.level !== 'NONE'`; WARNING=orange, CRITICAL=red
  - [x] 4.3: 4th mat-tab "Logs scraping" with table (source, status chip, startedAt, finishedAt, totalRowsWritten, errorMessage)
  - [x] 4.4: 6 new tests in `admin-pipeline-page.component.spec.ts` (scrapeLog populated, banner CRITICAL, banner WARNING, banner hidden for NONE, banner shows elapsed hours, empty scrapeLog)

### Review Follow-ups (AI)

- [x] [AI-Review][MEDIUM] M1 — `SecurityConfigAdminScrapeAuthorizationTest.java` already in File List (7 tests) — RESOLVED (file already documented)
- [x] [AI-Review][MEDIUM] M2 — Spec file is 323 lines (well under 500) — RESOLVED (concern no longer applies)
- [x] [AI-Review][MEDIUM] M3 — Added `limitZeroReturnsBadRequest` + `limitAboveMaxReturnsBadRequest` to `SecurityConfigAdminScrapeAuthorizationTest` — 8/8 tests green
- [x] [AI-Review][MEDIUM] M4 — Added `alertError` boolean + `alertError = false` reset in `loadData()` + `.pipeline-alert-error` banner in template + SCSS class + 2 tests (`sets alertError`, `shows indicator`)
- [x] [AI-Review][LOW] L5 — Added `aria-atomic="true"` to the alert banner element

## Dev Notes

### Key Infrastructure (Already Exists)

- **`IngestionRun`** entity at `model/IngestionRun.java`: `@Table("ingestion_runs")`, fields: `id` (UUID PK), `source` (VARCHAR 50 NOT NULL), `startedAt` (OffsetDateTime NOT NULL), `finishedAt` (nullable), `status` (RUNNING|PARTIAL|SUCCESS|FAILED), `totalRowsWritten` (Integer nullable), `errorMessage` (String nullable).
- **`IngestionRunRepository`** at `repository/IngestionRunRepository.java`: plain `JpaRepository<IngestionRun, UUID>` — no custom queries yet. Add `findAll(Pageable)` usage from parent.
- **`PlayerIdentityRepositoryPort`** at `domain/port/out/PlayerIdentityRepositoryPort.java`: already has `countByStatus(IdentityStatus)`, `findByStatus(IdentityStatus)`, `findByPlayerId(UUID)`, `save()`, `countByRegionAndStatus()`, `findLastIngestedAtByRegion()`.
- **`PlayerIdentityRepositoryAdapter`** at `adapter/out/persistence/player/identity/PlayerIdentityRepositoryAdapter.java`: implements the port, delegates to `PlayerIdentityJpaRepository`.
- **`PlayerIdentityJpaRepository`** at `adapter/out/persistence/player/identity/PlayerIdentityJpaRepository.java`: add `@Query("SELECT MIN(e.createdAt) FROM PlayerIdentityEntity e WHERE e.status = :status") Optional<OffsetDateTime> findOldestCreatedAtByStatus(@Param("status") IdentityStatus status)`.
- **`AdminAlertService`** pattern (already in `service/admin/`): reference for @Service structure.
- **Latest DB migration**: V41 — **no new migration needed** (ingestion_runs table already exists, no schema changes required).
- **`AdminScrapeController`** is new — place in `controller/` package, annotated `@RestController @RequestMapping("/api/admin/scraping") @PreAuthorize("hasRole('ADMIN')")`.

### Architecture Guardrails

- **CouplingTest** (max 7 deps): `AdminScrapeController` = 2 deps (`ScrapeLogService` + `UnresolvedAlertService`) ✓; `ScrapeLogService` = 1 dep (`IngestionRunRepository`) ✓; `UnresolvedAlertService` = 2 deps (`PlayerIdentityRepositoryPort` + `Clock`) ✓.
- **NamingConventionTest**: `@Service` classes must end with `Service` — `ScrapeLogService` ✓, `UnresolvedAlertService` ✓.
- **DomainIsolationTest**: `PlayerIdentityEntry` is in `domain/player/identity/model/` — no changes to domain model needed.
- **DependencyInversionTest**: Controllers must NOT inject repositories. `AdminScrapeController` → services only. `ScrapeLogService` injects `IngestionRunRepository` directly (pragmatic: `IngestionRun` lives in legacy `model/` layer, not hexagonal; consistent with existing ingestion service pattern).
- **Spotless**: run `mvn spotless:apply` before `mvn test`.
- **Clock pattern**: `UnresolvedAlertService` uses double-constructor pattern (same as `VisitTrackingService`): public constructor calls `this(dep, Clock.systemUTC())`, package-private constructor takes `Clock` for tests. No `@Bean Clock` needed.
- **Security**: `/api/admin/**` already restricted to ADMIN role via `SecurityConfig.configureAuthorizationRules()` — no `@PreAuthorize` needed on controller.

### Frontend Patterns

- **`forkJoin` extension**: current `loadData()` uses `forkJoin({ unresolved, resolved, count })` + separate `getRegionalStatus()` call. Extend the `forkJoin` to include `scrapeLog` + `pipelineAlert` for atomic loading.
- **Status chip colors**: SCSS `.status--success` (green), `.status--failed` (red), `.status--partial` (orange), `.status--running` (blue) in the page's SCSS.
- **Alert banner CSS class**: `.pipeline-alert-banner` — use `mat-icon` + text; WARNING = orange, CRITICAL = red.
- **Test spy**: extended `makePipelineServiceSpy` in the page spec to include `getScrapeLog` + `getUnresolvedAlertStatus` methods.
- **Test baselines**: Backend 2211 run (19 failures + 1 error pre-existing). Frontend 2104/2104 all green.

### Project Structure Notes

- New backend files:
  - `src/main/java/com/fortnite/pronos/dto/admin/ScrapeLogDto.java`
  - `src/main/java/com/fortnite/pronos/dto/admin/PipelineAlertDto.java`
  - `src/main/java/com/fortnite/pronos/service/admin/ScrapeLogService.java`
  - `src/main/java/com/fortnite/pronos/service/admin/UnresolvedAlertService.java`
  - `src/main/java/com/fortnite/pronos/controller/AdminScrapeController.java`
  - `src/test/java/com/fortnite/pronos/service/admin/ScrapeLogServiceTest.java`
  - `src/test/java/com/fortnite/pronos/service/admin/UnresolvedAlertServiceTest.java`
  - `src/test/java/com/fortnite/pronos/controller/AdminScrapeControllerTest.java`
- Modified backend files:
  - `src/main/java/com/fortnite/pronos/domain/port/out/PlayerIdentityRepositoryPort.java` (add `findOldestCreatedAtByStatus`)
  - `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/identity/PlayerIdentityJpaRepository.java` (add `@Query` method)
  - `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/identity/PlayerIdentityRepositoryAdapter.java` (implement new port method)
- New/modified frontend files:
  - `frontend/src/app/features/admin/models/admin.models.ts` (add 2 interfaces + AlertLevel type)
  - `frontend/src/app/features/admin/services/pipeline.service.ts` (add scrapeBaseUrl + 2 methods)
  - `frontend/src/app/features/admin/services/pipeline.service.spec.ts` (add 4 tests)
  - `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.ts` (extend forkJoin + new fields)
  - `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.html` (alert banner + scraping tab)
  - `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.scss` (status chip + banner styles)
  - `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.spec.ts` (extended spy + 6 new tests)

### References

- [Source: model/IngestionRun.java] — existing entity structure
- [Source: repository/IngestionRunRepository.java] — plain JpaRepository, no custom queries
- [Source: domain/port/out/PlayerIdentityRepositoryPort.java] — existing port methods
- [Source: service/admin/AdminAlertService.java] — alert service pattern with AlertDto
- [Source: service/admin/ErrorJournalService.java] — in-memory service pattern with Clock/time logic
- [Source: service/ingestion/PrIngestionOrchestrationService.java] — @ConditionalOnProperty + @Scheduled pattern
- [Source: adapter/out/persistence/player/identity/] — JPA adapter + @Query pattern

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- TypeScript type error in spec spy factory: `of(overrides.pipelineAlert)` inferred as `Observable<PipelineAlertStatus | null | undefined>`. Fixed with explicit cast to `PipelineAlertStatus | null`.
- `PlayerIdentityEntity.createdAt` is `LocalDateTime` (not `OffsetDateTime`) — adjusted port method signature and DTO field accordingly.

### Completion Notes List

- Task 1 (Backend ScrapeLog): `ScrapeLogDto` record + `ScrapeLogService` (clamps limit [1,200], sorted DESC) + `AdminScrapeController` `/logs` endpoint. 6 + 4 = 10 tests all green.
- Task 2 (Backend Alert): `findOldestCreatedAtByStatus` added to port + JPA repo + adapter. `PipelineAlertDto` record with nested `AlertLevel` enum. `UnresolvedAlertService` with double-constructor Clock pattern, escalation thresholds 24h→WARNING / 48h→CRITICAL. 6 + 2 = 8 tests all green.
- Task 3 (Frontend models/service): `ScrapeLogEntry` + `PipelineAlertStatus` interfaces + `AlertLevel` type added to `admin.models.ts`. `getScrapeLog()` + `getUnresolvedAlertStatus()` added to `PipelineService` using `scrapeBaseUrl`. 4 new service tests.
- Task 4 (Frontend page): `scrapeLog` + `pipelineAlert` fields added. `forkJoin` extended to 5 observables. Alert banner (orange/red by level) + 4th "Logs scraping" tab with status chips. SCSS styles for chips + banner. 6 new component tests.
- Runtime follow-up (2026-03-07): validation admin JWT reelle + correction `CustomUserDetailsService`/`UnresolvedAlertService` + revalidation avec une vraie ingestion locale `POST /api/ingestion/pr/csv` ; `/api/admin/scraping/logs` expose maintenant une entree `LOCAL_PR SUCCESS`. Voir `7-3-alerting-escalant-et-logs-scraping-detailles-runtime-validation-2026-03-07.md`.
- **Backend total**: 2229 run, 19 failures + 1 error (all pre-existing, +18 new passing).
- **Frontend total**: 2114/2114 SUCCESS (+10 new tests vs 2104 baseline).

### File List

- `src/main/java/com/fortnite/pronos/dto/admin/ScrapeLogDto.java` (new)
- `src/main/java/com/fortnite/pronos/dto/admin/PipelineAlertDto.java` (new)
- `src/main/java/com/fortnite/pronos/service/admin/ScrapeLogService.java` (new)
- `src/main/java/com/fortnite/pronos/service/admin/UnresolvedAlertService.java` (new)
- `src/main/java/com/fortnite/pronos/controller/AdminScrapeController.java` (new)
- `src/test/java/com/fortnite/pronos/service/admin/ScrapeLogServiceTest.java` (new)
- `src/test/java/com/fortnite/pronos/service/admin/UnresolvedAlertServiceTest.java` (new)
- `src/test/java/com/fortnite/pronos/controller/AdminScrapeControllerTest.java` (new)
- `src/test/java/com/fortnite/pronos/config/SecurityConfigAdminScrapeAuthorizationTest.java` (new)
- `src/main/java/com/fortnite/pronos/domain/port/out/PlayerIdentityRepositoryPort.java` (modified)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/identity/PlayerIdentityJpaRepository.java` (modified)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/identity/PlayerIdentityRepositoryAdapter.java` (modified)
- `frontend/src/app/features/admin/models/admin.models.ts` (modified)
- `frontend/src/app/features/admin/services/pipeline.service.ts` (modified)
- `frontend/src/app/features/admin/services/pipeline.service.spec.ts` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.ts` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.html` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.scss` (modified)
- `frontend/src/app/features/admin/pipeline/admin-pipeline-page/admin-pipeline-page.component.spec.ts` (modified)
