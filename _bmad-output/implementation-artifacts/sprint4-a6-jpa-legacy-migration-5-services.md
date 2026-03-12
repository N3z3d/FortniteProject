# Story sprint4-a6: JPA Legacy Migration - 5 Services

Status: done

<!-- METADATA
  story_key: sprint4-a6-jpa-legacy-migration-5-services
  branch: story/sprint4-a6-jpa-legacy-migration-5-services
  sprint: Sprint 4
  Note: Tech debt story — no new user-facing features. Pure hexagonal architecture cleanup.
-->

## Story

As a developer,
I want 5 legacy services that still use JPA repositories directly to be migrated to domain ports,
so that the service layer respects the hexagonal architecture (no JPA leakage into business logic).

## Acceptance Criteria

1. `AdminDashboardService` uses `GameRepositoryPort`, `UserRepositoryPort`, `TradeRepositoryPort` — no direct `GameRepository`, `UserRepository`, `TradeRepository` imports.
2. `AdminGameCatalogService` uses `GameRepositoryPort`, `UserRepositoryPort` — no direct JPA repo imports.
3. `AdminGameSupervisionService` uses `GameRepositoryPort` — no direct `GameRepository` import.
4. `AdminRecentActivityService` uses `GameRepositoryPort`, `TradeRepositoryPort`, `UserRepositoryPort` — no direct JPA repo imports.
5. `ScrapeLogService` uses a new `IngestionRunRepositoryPort` — no direct `IngestionRunRepository` import.
6. All tests impacted by this migration pass (zero regressions on the migrated scope). When the full suite is run, it must not introduce any failure beyond the documented pre-existing baseline. Each migrated service has updated tests using port mocks.
7. `TradeRepositoryPort` is extended with `count()` and `findAll()` methods needed by dashboard/activity services.
8. `GameRepositoryPort` is extended with `findByStatusInWithFetch(Collection<GameStatus>)` and `findAll()` needed by supervision/catalog services.
9. New `IngestionRunRepositoryPort` created in `domain/port/out/` with `findRecentLogs(int limit)` — no Spring types in domain layer. Implemented by `IngestionRunRepository` via default method.
10. `CouplingTest` (max 7 deps) still passes after migration.

## Tasks / Subtasks

- [x] Task 1: Extend existing ports with missing methods (AC: #7, #8)
  - [x] 1.1: Add `count()` and `findAll()` to `TradeRepositoryPort`
  - [x] 1.2: Add `findByStatusInWithFetch(Collection<GameStatus> statuses)` and `findAll()` to `GameRepositoryPort`
  - [x] 1.3: Verify `TradeRepository` and `GameRepository` implement new port methods (via JpaRepository inheritance — no explicit override needed)

- [x] Task 2: Create `IngestionRunRepositoryPort` (AC: #5, #9)
  - [x] 2.1: Create `domain/port/out/IngestionRunRepositoryPort.java` with `findRecentLogs(int limit)` returning `List<IngestionRun>` (pure Java — no Spring types in domain layer)
  - [x] 2.2: Add `extends IngestionRunRepositoryPort` to `IngestionRunRepository` with default method delegating to `findAll(PageRequest)`

- [x] Task 3: Migrate `AdminGameCatalogService` (AC: #2)
  - [x] 3.1: Replace `GameRepository` field with `GameRepositoryPort`
  - [x] 3.2: Replace `UserRepository` field with `UserRepositoryPort`
  - [x] 3.3: Update `AdminDashboardServiceTest` to mock ports instead of JPA repos
  - [x] 3.4: Add direct `AdminGameCatalogServiceTest` coverage with port mocks, including normalized status filter handling

- [x] Task 4: Migrate `AdminGameSupervisionService` (AC: #3)
  - [x] 4.1: Replace `GameRepository` field with `GameRepositoryPort`
  - [x] 4.2: Update `AdminGameSupervisionServiceTest` to mock `GameRepositoryPort`

- [x] Task 5: Migrate `AdminRecentActivityService` (AC: #4)
  - [x] 5.1: Replace `GameRepository` with `GameRepositoryPort`
  - [x] 5.2: Replace `TradeRepository` with `TradeRepositoryPort`
  - [x] 5.3: Replace `UserRepository` with `UserRepositoryPort`
  - [x] 5.4: Update `AdminDashboardServiceTest` (which creates `AdminRecentActivityService` inline)
  - [x] 5.5: Add direct `AdminRecentActivityServiceTest` coverage with port mocks and descending "recent first" assertions

- [x] Task 6: Migrate `AdminDashboardService` (AC: #1)
  - [x] 6.1: Replace inline `UserRepository`, `GameRepository`, `TradeRepository` fields with ports (also removed inline fully-qualified class names, added proper imports)
  - [x] 6.2: Update `AdminDashboardServiceTest` to use ports

- [x] Task 7: Migrate `ScrapeLogService` (AC: #5)
  - [x] 7.1: Replace `IngestionRunRepository` field with `IngestionRunRepositoryPort`, use `findRecentLogs(limit)` instead of Spring `findAll(PageRequest)`
  - [x] 7.2: Update `ScrapeLogServiceTest` to mock `IngestionRunRepositoryPort` and use `findRecentLogs(int)` expectations

- [x] Task 8: Run full test suite and verify (AC: #6, #10)
  - [x] 8.1: `mvn spotless:apply -q && mvn test -q` — 2372 run, 15 failures + 1 error (all pre-existing, unchanged from baseline)
  - [x] 8.2: `CouplingTest`, `DomainIsolationTest`, `HexagonalArchitectureTest`, `NamingConventionTest`, `LayeredArchitectureTest` — all 40 arch tests pass (0 failures)
  - [x] 8.3: No JPA repo imports remain in the 5 migrated services (verified by grep)
  - [x] 8.4: Code-review rerun targeted suite green: `AdminGameCatalogServiceTest`, `AdminRecentActivityServiceTest`, `AdminDashboardServiceTest`, `AdminGameSupervisionServiceTest`, `ScrapeLogServiceTest` + 5 arch tests

## Dev Notes

### Architecture target

```
Controller → Service → Domain Port (interface) → Adapter → JPA Repository
```

Services must only depend on `domain/port/out/*RepositoryPort` interfaces, not on `repository/*.java` JPA interfaces.

### Critical constraints

- `CouplingTest`: max 7 `@Service` dependencies. All 5 target services have <= 3 deps, safe.
- `NamingConventionTest`: service classes in `..service..` must end with `Service` — no rename needed.
- `DomainIsolationTest`: domain classes must be `final`, no JPA/Spring imports — port interfaces are fine.
- `LayeredArchitectureTest`: controllers cannot depend on repositories — already satisfied.
- NEVER modify domain models — only port interfaces and service layer.
- Run `mvn spotless:apply` before `mvn test` (CRLF/LF normalization).

### Key implementation decision: IngestionRunRepositoryPort

The port uses a domain-friendly `findRecentLogs(int limit)` method rather than Spring's `Page<T> findAll(Pageable)`. This keeps Spring types out of the domain layer. The `IngestionRunRepository` implements the port via a `default` method that delegates to Spring Data's `findAll(PageRequest)`.

### Pre-existing Gaps / Known Issues

- [KNOWN] ~16 pre-existing backend test failures (GameDataIntegrationTest 4, FortniteTrackerServiceTddTest 6, etc.) — untouched by this story.
- [KNOWN] `AdminGameCatalogService` and `AdminRecentActivityService` are package-private — access modifier preserved, but now covered by direct package-level unit tests.

### Project Structure Notes

- Ports: `src/main/java/com/fortnite/pronos/domain/port/out/`
- Services: `src/main/java/com/fortnite/pronos/service/admin/`
- Tests: `src/test/java/com/fortnite/pronos/service/admin/`

### References

- [Source: docs/audit/ARCHITECTURE_AUDIT.md#Service Layer - Port Migration Status]
- [Source: src/main/java/com/fortnite/pronos/domain/port/out/GameRepositoryPort.java]
- [Source: src/main/java/com/fortnite/pronos/domain/port/out/TradeRepositoryPort.java]
- [Source: src/main/java/com/fortnite/pronos/domain/port/out/UserRepositoryPort.java]
- [Source: src/main/java/com/fortnite/pronos/repository/GameRepository.java]
- [Source: src/main/java/com/fortnite/pronos/repository/TradeRepository.java]
- [Source: src/main/java/com/fortnite/pronos/repository/IngestionRunRepository.java]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

None — all changes compiled and passed on second attempt (first attempt failed due to Spring `Page`/`Pageable` in domain port, fixed by using `List<IngestionRun>` + `findRecentLogs(int)` pattern).

### Completion Notes List

- Task 1: Extended `TradeRepositoryPort` with `findAll()` + `count()`. Extended `GameRepositoryPort` with `findAll()` + `findByStatusInWithFetch(Collection<GameStatus>)`. No changes needed in `TradeRepository`/`GameRepository` — JpaRepository base already provides `findAll()` and `count()`.
- Task 2: Created `IngestionRunRepositoryPort` with `findRecentLogs(int limit)` using pure Java return type (no Spring Data). `IngestionRunRepository` implements via default method.
- Tasks 3-6: All 5 services migrated — JPA repo fields replaced with port interfaces. `AdminDashboardServiceTest` updated to mock all 3 ports.
- Task 7: `ScrapeLogService` now calls `ingestionRunRepository.findRecentLogs(clampedLimit)`. `ScrapeLogServiceTest` updated with 6 tests using `findRecentLogs(int)` mock.
- Task 8: Full suite: 2372 run, 15 F + 1 E (all pre-existing). Arch tests: 40/40 pass.
- Code review rerun: `AdminGameCatalogService` now normalizes `status` filters before `GameStatus.valueOf(...)`; `AdminRecentActivityService` sorts recent games and trades in descending chronological order before applying the top-10 limit.
- Direct unit tests added for the two previously only-indirectly-covered package-private services: `AdminGameCatalogServiceTest` and `AdminRecentActivityServiceTest`.
- Targeted rerun green: 5 admin service tests + 5 architecture tests.

### File List

- `src/main/java/com/fortnite/pronos/domain/port/out/TradeRepositoryPort.java` — added `findAll()`, `count()`
- `src/main/java/com/fortnite/pronos/domain/port/out/GameRepositoryPort.java` — added `findAll()`, `findByStatusInWithFetch(Collection<GameStatus>)`, `import java.util.Collection`
- `src/main/java/com/fortnite/pronos/domain/port/out/IngestionRunRepositoryPort.java` — NEW: `findRecentLogs(int limit)`
- `src/main/java/com/fortnite/pronos/repository/IngestionRunRepository.java` — extends `IngestionRunRepositoryPort`, implements `findRecentLogs` via default method
- `src/main/java/com/fortnite/pronos/service/admin/AdminGameCatalogService.java` — migrated to `GameRepositoryPort` + `UserRepositoryPort`
- `src/main/java/com/fortnite/pronos/service/admin/AdminGameSupervisionService.java` — migrated to `GameRepositoryPort`
- `src/main/java/com/fortnite/pronos/service/admin/AdminRecentActivityService.java` — migrated to `GameRepositoryPort` + `TradeRepositoryPort` + `UserRepositoryPort`
- `src/main/java/com/fortnite/pronos/service/admin/AdminDashboardService.java` — migrated to ports, removed inline fully-qualified class names
- `src/main/java/com/fortnite/pronos/service/admin/ScrapeLogService.java` — migrated to `IngestionRunRepositoryPort`, uses `findRecentLogs(limit)`
- `src/test/java/com/fortnite/pronos/service/admin/AdminGameCatalogServiceTest.java` — NEW: direct port-mock coverage for catalog service + status normalization
- `src/test/java/com/fortnite/pronos/service/admin/AdminRecentActivityServiceTest.java` — NEW: direct port-mock coverage for recent activity ordering/limits
- `src/test/java/com/fortnite/pronos/service/admin/AdminDashboardServiceTest.java` — mocks updated to port interfaces
- `src/test/java/com/fortnite/pronos/service/admin/AdminGameSupervisionServiceTest.java` — mock updated to `GameRepositoryPort`
- `src/test/java/com/fortnite/pronos/service/admin/ScrapeLogServiceTest.java` — rewritten for `IngestionRunRepositoryPort.findRecentLogs(int)`

## Change Log

- 2026-03-07: code review rerun — AC #6 clarified to zero-regression baseline semantics; added direct unit tests for `AdminGameCatalogService` and `AdminRecentActivityService`; hardened recent-activity ordering and status filter normalization; targeted admin + arch suites green.

## Code Review Closure

- 2026-03-07 rerun: no remaining `HIGH` or `MEDIUM` findings on the migrated-service scope.
