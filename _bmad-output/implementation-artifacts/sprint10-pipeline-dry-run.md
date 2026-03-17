# Story: sprint10-pipeline-dry-run — Admin Dry-Run Endpoint for FortniteTrackerScrapingAdapter

Status: ready-for-dev

<!-- METADATA
  story_key: sprint10-pipeline-dry-run
  branch: story/sprint10-pipeline-dry-run
  sprint: Sprint 10
  Note: Validation is optional. Run validate-create-story for quality check before dev-story.
-->

## Story

**As an** admin,
**I want** to trigger a manual dry-run of the FortniteTracker scraping adapter on a single region,
**So that** I can validate that proxy keys are correctly configured and real data is returned before enabling the scheduled cron ingestion.

## Context

`FortniteTrackerScrapingAdapter` (Sprint 9) implements `PrRegionCsvSourcePort` and fetches PR leaderboard data via 3 proxy providers (Scrapfly, ScraperAPI, Scrape.do) with 8-attempt exponential backoff. It is already wired into Spring context but gated by env vars — no proxy keys = no scraping. Before activating `ingestion.pr.scheduled.enabled=true`, the admin needs a safe, on-demand validation path that:

1. Calls the real adapter with the configured keys
2. Returns a structured result (row count, sample rows, validation status)
3. Does NOT activate the scheduler or modify any production state

The endpoint is behind `ROLE_ADMIN` (same security contract as all `/api/admin/**` routes).

**Important**: configure `scraping.fortnitetracker.pages-per-region=1` in the runtime environment when running dry-run tests to fetch only 1 page (≈100 rows) instead of the default 4 pages.

## Acceptance Criteria

1. `ScrapingDryRunService` exists in `service/ingestion/` — injects `PrRegionCsvSourcePort`, calls `fetchCsv(region)`, parses the CSV result, and returns `DryRunResult` (rowCount, valid, sampleRows[≤5], errors list)
2. `ScrapingAdminController` exists in `controller/` — `POST /api/admin/scraping/dry-run` accepts `?region=EU` (default EU), requires `ROLE_ADMIN`, returns `DryRunResultDto` (HTTP 200 even when validation fails — errors are in the body)
3. `DryRunResultDto` record contains: `region` (String), `rowCount` (int), `valid` (boolean), `sampleRows` (List\<String\>), `errors` (List\<String\>)
4. Validation logic in `ScrapingDryRunService`: (a) `rowCount >= 10` → smoke check, (b) all data rows have `points` field parseable as integer between 1 and 9_999_999, (c) `valid=true` only when both checks pass
5. When no proxy keys are configured (empty `getAvailableProviders()`), `fetchCsv()` returns `Optional.empty()` — service returns `DryRunResult` with `rowCount=0`, `valid=false`, `errors=["No scraping providers configured"]`
6. `application.properties` documents the relevant env var pattern for dry-run (commented)
7. Minimum 6 unit tests in `ScrapingDryRunServiceTest`: send path (valid data), skip path (empty Optional), smoke check failure (rowCount < 10), score validation failure (out-of-range points), sampleRows capped at 5, region parameter forwarded correctly
8. `SecurityConfigScrapingAdminAuthorizationTest` covers `POST /api/admin/scraping/dry-run`: anonymous → 401, `ROLE_USER` → 403, `ROLE_ADMIN` → 200

## Tasks / Subtasks

- [ ] Task 1: Create `DryRunResultDto` record (AC: #3)
  - [ ] 1.1: Create `src/main/java/com/fortnite/pronos/controller/dto/DryRunResultDto.java` — record with fields: `region`, `rowCount`, `valid`, `sampleRows`, `errors`

- [ ] Task 2: Create `ScrapingDryRunService` (AC: #1, #4, #5)
  - [ ] 2.1: Create `src/main/java/com/fortnite/pronos/service/ingestion/ScrapingDryRunService.java`
  - [ ] 2.2: Inject `PrRegionCsvSourcePort` (port, not adapter) via constructor
  - [ ] 2.3: Implement `DryRunResult runDryRun(PrRegion region)` — calls `fetchCsv(region)`, parses CSV lines, runs smoke + score validation, builds result
  - [ ] 2.4: Inner record or separate class `DryRunResult` for internal use (or reuse `DryRunResultDto` directly if no mapping needed)
  - [ ] 2.5: Parse CSV: skip header line, count data rows, extract `points` field (index 2 per header `nickname,region,points,rank,snapshot_date`)
  - [ ] 2.6: Build `sampleRows` as first 5 raw CSV lines (excluding header) for human inspection

- [ ] Task 3: Create `ScrapingAdminController` (AC: #2)
  - [ ] 3.1: Create `src/main/java/com/fortnite/pronos/controller/ScrapingAdminController.java`
  - [ ] 3.2: `@RestController`, `@RequestMapping("/api/admin/scraping")`, `@PreAuthorize("hasRole('ADMIN')")`
  - [ ] 3.3: `POST /dry-run` with `@RequestParam(defaultValue = "EU") String region` — parse to `PrRegion`, call service, return `ResponseEntity<DryRunResultDto>`
  - [ ] 3.4: Handle unknown region string → `ResponseEntity.badRequest()` with error message

- [ ] Task 4: Update `application.properties` (AC: #6)
  - [ ] 4.1: Add commented section `# Dry-run tip: set scraping.fortnitetracker.pages-per-region=1 for single-page validation`

- [ ] Task 5: Write `ScrapingDryRunServiceTest` (AC: #7)
  - [ ] 5.1: Test `runDryRun_returnsValid_whenAdapterReturnsGoodCsv` — mock port returns well-formed CSV with 15 rows, valid scores → `valid=true`, `rowCount=15`
  - [ ] 5.2: Test `runDryRun_returnsInvalid_whenAdapterReturnsEmpty` — mock port returns `Optional.empty()` → `rowCount=0`, `valid=false`, error message present
  - [ ] 5.3: Test `runDryRun_failsSmoke_whenRowCountBelow10` — 7 rows → `valid=false`, error contains "smoke"
  - [ ] 5.4: Test `runDryRun_failsScore_whenPointsOutOfRange` — row with `points=0` or `points=10_000_001` → `valid=false`, error mentions score
  - [ ] 5.5: Test `runDryRun_capsSampleRowsAt5` — 15 rows returned → `sampleRows.size() == 5`
  - [ ] 5.6: Test `runDryRun_forwardsRegionToPort` — verify `fetchCsv(PrRegion.NAC)` called when `region=NAC`

- [ ] Task 6: Security authorization test (AC: #8)
  - [ ] 6.1: Create `src/test/java/com/fortnite/pronos/config/SecurityConfigScrapingAdminAuthorizationTest.java`
  - [ ] 6.2: `@WebMvcTest(controllers = ScrapingAdminController.class)`, `@Import({SecurityConfig.class, SecurityTestBeans.class})`
  - [ ] 6.3: `@MockBean ScrapingDryRunService` — return dummy result
  - [ ] 6.4: Test anonymous `POST /api/admin/scraping/dry-run` → 401
  - [ ] 6.5: Test `ROLE_USER` → 403
  - [ ] 6.6: Test `ROLE_ADMIN` → 200

## Dev Notes

### Architecture Constraints

- `FortniteTrackerScrapingAdapter` is `@Component` (not `@Service`) — this is intentional to satisfy `CouplingTest.servicesShouldNotHaveMoreThanSevenDependencies` (adapters don't count toward service coupling limit). Do NOT change it.
- `ScrapingDryRunService` must be `@Service` and must end in `Service` (NamingConventionTest rule for `..service..` packages).
- `ScrapingAdminController` is a new `@RestController` → security test is **mandatory** (see Task 6). Pattern: `SecurityConfig<ControllerName>AuthorizationTest`.
- Inject `PrRegionCsvSourcePort` (not `FortniteTrackerScrapingAdapter`) to stay hexagonal.
- `PrRegionCsvSourcePort` is in `service/ingestion/` — already accessible from `service/ingestion/ScrapingDryRunService`.

### CSV Format

The adapter produces CSV with header: `nickname,region,points,rank,snapshot_date`
Example row: `Bugha,EU,12500,1,2026-03-17`
Parse `points` from index 2 (0-based after split on `,`).

### PrRegion Enum

`PrRegion` is in `com.fortnite.pronos.model.PrRegion`. Values include: `EU`, `NAC`, `NAW`, `BR`, `ASIA`, `OCE`, `ME`, `GLOBAL`. Parse from String via `PrRegion.valueOf(region.toUpperCase())` — throws `IllegalArgumentException` for unknown regions (catch and return 400).

### FortniteTrackerScrapingAdapter Key Facts

- `fetchCsv(PrRegion region)` loops `props.getPagesPerRegion()` pages (default 4)
- For dry-run: set env `SCRAPING_FORTNITETRACKER_PAGES_PER_REGION=1` (or `scraping.fortnitetracker.pages-per-region=1`) to fetch only 1 page
- If no provider keys configured → logs warn + returns `Optional.empty()` immediately
- Exponential backoff: `300 * 1.9^n` ms, 8 attempts max, jitter 300ms

### Env Vars Needed for Real Dry-Run

At least one of:
- `SCRAPING_FORTNITETRACKER_SCRAPFLY_KEYS=<key1>,<key2>`
- `SCRAPING_FORTNITETRACKER_SCRAPERAPI_KEYS=<key>`
- `SCRAPING_FORTNITETRACKER_SCRAPEDO_TOKEN=<token>`

These are NOT stored in `application.properties` — environment-only.

### `SecurityTestBeans.class`

Pattern from `SecurityConfigGameLeaderboardAuthorizationTest`: `@Import({SecurityConfig.class, SecurityTestBeans.class})`. `SecurityTestBeans` provides mock `JwtUtil` etc. for `@WebMvcTest` context.

### Pre-existing Gaps / Known Issues

- [KNOWN] `FortniteTrackerServiceTddTest`: 6 failures — French error message mismatch (pre-existing, unrelated to this story)
- [KNOWN] `GameDataIntegrationTest`: 4 failures — test data issues (pre-existing)
- [KNOWN] `PlayerServiceTddTest`: 1 failure (pre-existing)
- [KNOWN] `PlayerServiceTest`: 1 failure (pre-existing)
- [KNOWN] `ScoreCalculationServiceTddTest`: 2 failures (pre-existing)
- [KNOWN] `ScoreCalculationServiceTest`: 2 failures (pre-existing)
- [KNOWN] `ScoreServiceTddTest`: 3 failures (pre-existing)
- [KNOWN] `GameStatisticsServiceTddTest`: 1 NPE error (pre-existing)
- Total: ~20 pre-existing backend failures — do not attempt to fix them as part of this story

### Project Structure Notes

```
src/main/java/com/fortnite/pronos/
  controller/
    ScrapingAdminController.java          ← NEW
    dto/
      DryRunResultDto.java                ← NEW (record)
  service/
    ingestion/
      ScrapingDryRunService.java          ← NEW (@Service)
      PrRegionCsvSourcePort.java          ← existing port (inject this)
  adapter/out/scraping/
    FortniteTrackerScrapingAdapter.java   ← existing (no changes)
    FortniteTrackerScrapingProperties.java ← existing (no changes)

src/test/java/com/fortnite/pronos/
  config/
    SecurityConfigScrapingAdminAuthorizationTest.java  ← NEW
  service/ingestion/
    ScrapingDryRunServiceTest.java                     ← NEW
```

- No Flyway migration needed (no DB changes)
- No frontend changes
- No i18n changes
- Always run `mvn spotless:apply` before `mvn test` for new files

### References

- [Source: `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingAdapter.java` — `fetchCsv()`, `assembleCsv()`, `getAvailableProviders()`]
- [Source: `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingProperties.java` — `getPagesPerRegion()`, key list getters]
- [Source: `src/main/java/com/fortnite/pronos/adapter/out/scraping/ScrapedRow.java` — CSV row structure]
- [Source: `src/main/java/com/fortnite/pronos/service/ingestion/PrRegionCsvSourcePort.java` — port interface]
- [Source: `src/main/java/com/fortnite/pronos/controller/AdminPlayerPipelineController.java` — @PreAuthorize pattern]
- [Source: `src/test/java/com/fortnite/pronos/config/SecurityConfigGameLeaderboardAuthorizationTest.java` — @WebMvcTest security test pattern]
- [Source: `_bmad-output/implementation-artifacts/sprint-9-retro-2026-03-17.md` — smoke test ≥10 rows, score 0<points<10M, dry-run rationale]
- [Source: MEMORY.md — NamingConventionTest @Service suffix rule, CouplingTest max 7 deps, Spotless apply before test]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
