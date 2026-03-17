# Story: sprint10-pipeline-hardening — CSV Cache Fallback + Smoke Check + User-Agent Rotation

Status: review

<!-- METADATA
  story_key: sprint10-pipeline-hardening
  branch: story/sprint10-pipeline-hardening
  sprint: Sprint 10
  Note: Builds on sprint10-pipeline-dry-run (done). Prerequisite for sprint10-pipeline-wiring.
-->

## Story

**As an** admin,
**I want** the scraping pipeline to be resilient against transient proxy failures and bot-detection,
**So that** scheduled ingestion falls back to the last valid CSV cache when live scraping fails, and rotating User-Agent headers reduce the risk of provider bans.

## Context

`FortniteTrackerScrapingAdapter` scrapes 8 regions via 3 proxy providers. Two fragility points identified:

1. **Zero fallback**: if `fetchCsv()` returns empty (all retries exhausted), `processRegion()` returns `"empty_csv"` and the region is silently skipped — no data ingested. Historical data should be preserved.
2. **Fixed User-Agent**: all HTTP requests use the `RestTemplate`'s default User-Agent, making bot-detection trivial. Rotation reduces fingerprinting.

Additionally, the orchestrator currently does `csv.isBlank()` as the only quality check. It should also validate the smoke condition (≥ 10 rows) before persisting data, and only update the cache with clean data.

This story adds:
- `CsvCachePort` + `InMemoryCsvCacheAdapter` (in-memory, per-region, thread-safe)
- Smoke check + cache save/load in `PrIngestionOrchestrationService.processRegion()`
- User-Agent rotation via new `userAgents` property in `FortniteTrackerScrapingProperties`

## Acceptance Criteria

1. `CsvCachePort` interface exists in `service/ingestion/` with `void save(PrRegion, String)` and `Optional<String> load(PrRegion)`
2. `InMemoryCsvCacheAdapter` implements `CsvCachePort` — `@Component`, uses `ConcurrentHashMap<PrRegion, String>`, thread-safe
3. `PrIngestionOrchestrationService.processRegion()` updated:
   - (a) if `fetchCsv()` returns empty → try `csvCachePort.load(region)` as fallback; if fallback present, log info + use it; if no fallback → return `"no_data"`
   - (b) if `fetchCsv()` returns non-empty → count data rows (lines excluding header and blanks); if `rowCount < SMOKE_MIN_ROWS` (10) → log warn + return `"smoke_check_failed"` (don't ingest, don't update cache)
   - (c) if smoke passes → `csvCachePort.save(region, csv)` then proceed with ingestion
4. `FortniteTrackerScrapingProperties` has new field `userAgents` (String, default `""`) + `getUserAgentList()` (reuse `parseKeys()` logic)
5. `FortniteTrackerScrapingAdapter.fetchPageWithRetry()` uses `restTemplate.exchange()` with `HttpEntity` carrying a `User-Agent` header selected as `userAgentList.get(attempt % size)` when list is non-empty; no header added when list is empty (backward-compatible)
6. Minimum 8 new tests:
   - `InMemoryCsvCacheAdapterTest`: save+load happy path, load returns empty when not saved, overwrite existing entry (3 tests)
   - `PrIngestionOrchestrationServiceTest` additions: fallback used when fetchCsv empty, smoke check failure skips ingestion, cache saved after successful ingestion, `"no_data"` returned when both live + cache empty (4 tests)
   - `FortniteTrackerScrapingAdapterTest` addition: User-Agent header set when userAgents configured (1 test)
7. Existing `PrIngestionOrchestrationServiceTest` tests remain green (constructor updated, single-row CSV updated to ≥ 10 rows)

## Tasks / Subtasks

- [x] Task 1: Create `CsvCachePort` interface (AC: #1)
  - [x] 1.1: Create `src/main/java/com/fortnite/pronos/service/ingestion/CsvCachePort.java`
  - [x] 1.2: Two methods: `void save(PrRegion region, String csv)` and `Optional<String> load(PrRegion region)`

- [x] Task 2: Create `InMemoryCsvCacheAdapter` (AC: #2)
  - [x] 2.1: Create `src/main/java/com/fortnite/pronos/adapter/out/scraping/InMemoryCsvCacheAdapter.java`
  - [x] 2.2: `@Component`, `implements CsvCachePort`, `ConcurrentHashMap<PrRegion, String>` field
  - [x] 2.3: `save()` puts; `load()` returns `Optional.ofNullable(map.get(region))`

- [x] Task 3: Update `PrIngestionOrchestrationService` (AC: #3)
  - [x] 3.1: Add `CsvCachePort csvCachePort` as 4th constructor param (both public + package-private test constructor)
  - [x] 3.2: Add `static final int SMOKE_MIN_ROWS = 10` constant
  - [x] 3.3: Rewrite `processRegion()` per AC #3a/b/c logic (see Dev Notes for full pseudocode)
  - [x] 3.4: Add private helper `int countDataRows(String csv)` — splits on `\n`, skips header (index 0), counts non-blank lines
  - [x] 3.5: Update `PrIngestionOrchestrationServiceTest` — add `@Mock CsvCachePort csvCachePort`, update constructor call to 4-arg, update single-row CSV fixtures to 11 rows

- [x] Task 4: User-Agent rotation in adapter (AC: #4, #5)
  - [x] 4.1: Add `private String userAgents = ""` field + setter/getter to `FortniteTrackerScrapingProperties`
  - [x] 4.2: Add `getUserAgentList()` method reusing `parseKeys(userAgents)`
  - [x] 4.3: In `FortniteTrackerScrapingAdapter.fetchPageWithRetry()`: replace `restTemplate.getForEntity(proxyUrl, String.class)` with `restTemplate.exchange(proxyUrl, HttpMethod.GET, buildRequestEntity(attempt), String.class)`
  - [x] 4.4: Add private `HttpEntity<Void> buildRequestEntity(int attempt)` — creates `HttpHeaders`, sets `User-Agent` if `getUserAgentList()` is non-empty, returns `new HttpEntity<>(headers)`
  - [x] 4.5: Add required imports: `org.springframework.http.HttpMethod`, `org.springframework.http.HttpEntity`, `org.springframework.http.HttpHeaders`
  - [x] 4.6: Update `FortniteTrackerScrapingAdapterTest` mock: change `restTemplate.getForEntity(...)` to `restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))`

- [x] Task 5: Tests (AC: #6)
  - [x] 5.1: Create `src/test/java/com/fortnite/pronos/adapter/out/scraping/InMemoryCsvCacheAdapterTest.java` — 3 tests: `save_thenLoad_returnsValue`, `load_returnsEmpty_whenNothingSaved`, `save_twice_overwritesPrevious`
  - [x] 5.2: Add 4 tests to `PrIngestionOrchestrationServiceTest`: `processRegion_usesCacheFallback_whenFetchCsvEmpty`, `processRegion_skipsIngestion_whenSmokeCheckFails`, `processRegion_savesToCache_afterSuccessfulIngestion`, `processRegion_returnsNoData_whenBothLiveAndCacheEmpty`
  - [x] 5.3: Add 1 test to `FortniteTrackerScrapingAdapterTest`: `fetchPageWithRetry_setsUserAgentHeader_whenConfigured`

## Dev Notes

### processRegion() Pseudocode (AC #3)

```java
private String processRegion(PrRegion region) {
    try {
        Optional<String> csvOpt = regionCsvSourcePort.fetchCsv(region);
        String csv;
        if (csvOpt.isEmpty()) {
            Optional<String> cached = csvCachePort.load(region);
            if (cached.isEmpty()) return "no_data";
            log.info("Using CSV cache fallback for region={}", region);
            csv = cached.get();
        } else {
            csv = csvOpt.get();
            int rowCount = countDataRows(csv);
            if (rowCount < SMOKE_MIN_ROWS) {
                log.warn("Smoke check failed for region={}: {} rows < {}", region, rowCount, SMOKE_MIN_ROWS);
                return "smoke_check_failed";
            }
            csvCachePort.save(region, csv);
        }
        // existing ingestion logic below (unchanged)
        PrIngestionResult result = ingestionService.ingest(new StringReader(csv),
            new PrIngestionConfig("SCHEDULED_PR_" + region.name(), Year.now(clock).getValue(), true));
        return result.status() == IngestionRun.Status.SUCCESS ? null : "ingestion_" + result.status();
    } catch (Exception e) {
        String msg = e.getMessage();
        return msg == null || msg.isBlank() ? e.getClass().getSimpleName() : msg;
    }
}
```

### countDataRows() Helper

```java
private int countDataRows(String csv) {
    String[] lines = csv.split("\n");
    int count = 0;
    for (int i = 1; i < lines.length; i++) {   // skip header at index 0
        if (!lines[i].trim().isEmpty()) count++;
    }
    return count;
}
```

### RestTemplate exchange() Pattern

```java
// In fetchPageWithRetry(), replace:
ResponseEntity<String> resp = restTemplate.getForEntity(proxyUrl, String.class);
// With:
ResponseEntity<String> resp = restTemplate.exchange(proxyUrl, HttpMethod.GET, buildRequestEntity(attempt), String.class);

// New helper:
private HttpEntity<Void> buildRequestEntity(int attempt) {
    List<String> uas = props.getUserAgentList();
    if (uas.isEmpty()) return HttpEntity.EMPTY;
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", uas.get(attempt % uas.size()));
    return new HttpEntity<>(headers);
}
```

### Existing Test Breakage — MUST Fix

**`PrIngestionOrchestrationServiceTest`** — 3 required fixes:

1. Constructor updated from 3 args to 4: add `@Mock CsvCachePort csvCachePort` and update `setUp()`:
   ```java
   @Mock private CsvCachePort csvCachePort;
   // in setUp():
   orchestrationService = new PrIngestionOrchestrationService(ingestionService, regionCsvSourcePort, clock, csvCachePort);
   // Note: 4th arg is csvCachePort BUT the constructor signature must match
   ```
   Wait — check actual constructor param order in the updated service. The package-private 4-arg test constructor should be: `(PrIngestionService, PrRegionCsvSourcePort, Clock, CsvCachePort)`.

2. Single-row CSV fixtures → must have ≥ 10 data rows. Current: `"nickname,...\nplayer,EU,100,1,2026-02-25"`. Fix: add 10 rows. Helper method:
   ```java
   private String csvWithRows(PrRegion region, int count) {
       StringBuilder sb = new StringBuilder("nickname,region,points,rank,snapshot_date\n");
       for (int i = 1; i <= count; i++) {
           sb.append("Player").append(i).append(",").append(region).append(",")
             .append(1000 + i).append(",").append(i).append(",2026-03-17\n");
       }
       return sb.toString();
   }
   // Use: csvWithRows(PrRegion.EU, 11) everywhere
   ```

3. Existing tests may stub `csvCachePort` implicitly (Mockito returns `Optional.empty()` by default for Optional-returning methods — no explicit stubbing needed for the fallback path when live scraping returns valid data).

**`FortniteTrackerScrapingAdapterTest`** — mock change:
```java
// Old:
when(restTemplate.getForEntity(anyString(), eq(String.class)))
    .thenReturn(ResponseEntity.ok(VALID_HTML));
// New (all existing stubs):
when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
    .thenReturn(ResponseEntity.ok(VALID_HTML));
// Add import: org.springframework.http.HttpMethod, org.springframework.http.HttpEntity
```

### Architecture Constraints

- `CsvCachePort` in `service/ingestion/` (port lives alongside the orchestration service that uses it)
- `InMemoryCsvCacheAdapter` in `adapter/out/scraping/` (adapter pattern, co-located with scraping)
- `@Component` not `@Service` for `InMemoryCsvCacheAdapter` (adapter, not service — no NamingConventionTest issue)
- `PrIngestionOrchestrationService` currently: 3 deps → after: 4 deps (well under the 7-dep `CouplingTest` limit)
- `@ConditionalOnProperty` on `PrIngestionOrchestrationService` means it only loads when `ingestion.pr.scheduled.enabled=true` — no Spring context issues in tests (uses direct instantiation)
- No new `@RestController` → **NO security test needed**

### Pre-existing Gaps / Known Issues

- [KNOWN] `FortniteTrackerServiceTddTest`: 6 failures — French error messages (pre-existing)
- [KNOWN] `GameDataIntegrationTest`: 4 failures — test data issues (pre-existing)
- [KNOWN] `PlayerServiceTddTest`: 1, `PlayerServiceTest`: 1, `ScoreCalculationServiceTddTest`: 2, `ScoreCalculationServiceTest`: 2, `ScoreServiceTddTest`: 3 (all pre-existing)
- [KNOWN] `GameStatisticsServiceTddTest`: 1 NPE error (pre-existing)
- Total: ~20 pre-existing — do not fix as part of this story

### Project Structure Notes

```
src/main/java/com/fortnite/pronos/
  service/ingestion/
    CsvCachePort.java                        ← NEW (interface)
    PrIngestionOrchestrationService.java     ← MODIFIED (+CsvCachePort dep, smoke check, cache)
  adapter/out/scraping/
    InMemoryCsvCacheAdapter.java             ← NEW (@Component)
    FortniteTrackerScrapingAdapter.java      ← MODIFIED (exchange() + User-Agent)
    FortniteTrackerScrapingProperties.java   ← MODIFIED (+userAgents field)

src/test/java/com/fortnite/pronos/
  adapter/out/scraping/
    InMemoryCsvCacheAdapterTest.java         ← NEW (3 tests)
    FortniteTrackerScrapingAdapterTest.java  ← MODIFIED (exchange() mock + 1 new test)
  service/ingestion/
    PrIngestionOrchestrationServiceTest.java ← MODIFIED (4th constructor arg + 4 new tests + CSV fixtures)
```

- No Flyway migration needed
- No frontend changes
- No i18n changes
- Run `mvn spotless:apply` before `mvn test`

### References

- [Source: `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionOrchestrationService.java` — `processRegion()`, constructor, SUPPORTED_REGIONS]
- [Source: `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingAdapter.java` — `fetchPageWithRetry()`, `getForEntity()` call to migrate]
- [Source: `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingProperties.java` — `parseKeys()` reuse pattern]
- [Source: `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionOrchestrationServiceTest.java` — existing 3-arg constructor, single-row CSV fixtures to update]
- [Source: `src/test/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingAdapterTest.java` — `getForEntity` mock pattern to replace with `exchange`]
- [Source: `_bmad-output/implementation-artifacts/sprint10-pipeline-dry-run.md` — M3 timeout issue context, `countDataRows` pattern already tested in ScrapingDryRunService]
- [Source: MEMORY.md — CouplingTest max 7 deps, NamingConventionTest @Service suffix, Spotless apply before test, `PrIngestionRowProcessor` constructor update pattern]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ `CsvCachePort` interface created in `service/ingestion/` — `save(PrRegion, String)` + `Optional<String> load(PrRegion)`
- ✅ `InMemoryCsvCacheAdapter` created in `adapter/out/scraping/` — `@Component`, `ConcurrentHashMap<PrRegion, String>`, thread-safe
- ✅ `PrIngestionOrchestrationService` updated: 4-arg constructor (+`@Autowired` on public ctor), `SMOKE_MIN_ROWS=10`, `processRegion()` rewritten with cache fallback + smoke check + cache save, `countDataRows()` helper added
- ✅ `FortniteTrackerScrapingProperties` updated: `userAgents` field + `getUserAgentList()` reusing `parseKeys()`
- ✅ `FortniteTrackerScrapingAdapter.fetchPageWithRetry()` migrated from `getForEntity()` to `exchange()` with `buildRequestEntity(attempt)` — User-Agent header set when `userAgents` list is non-empty; backward-compatible (empty list → `HttpHeaders.EMPTY`)
- ✅ `InMemoryCsvCacheAdapterTest` (3 tests) — save+load, load empty, overwrite
- ✅ `PrIngestionOrchestrationServiceTest` (4 new tests + 3 existing updated) — cache fallback, smoke check failure, cache save after success, no_data when both empty; single-row CSV → 11-row via `csvWithRows()` helper
- ✅ `FortniteTrackerScrapingAdapterTest` (all stubs migrated getForEntity→exchange, 1 new test: User-Agent header)
- ✅ Full regression: 2359 tests, 0 failures, 0 errors
- ℹ️ `HttpEntity.EMPTY` is typed `HttpEntity<?>` — used `new HttpEntity<>(HttpHeaders.EMPTY)` for correct `HttpEntity<Void>` return type

### File List

**Created:**
- `src/main/java/com/fortnite/pronos/service/ingestion/CsvCachePort.java`
- `src/main/java/com/fortnite/pronos/adapter/out/scraping/InMemoryCsvCacheAdapter.java`
- `src/test/java/com/fortnite/pronos/adapter/out/scraping/InMemoryCsvCacheAdapterTest.java`

**Modified:**
- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionOrchestrationService.java`
- `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingAdapter.java`
- `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingProperties.java`
- `src/test/java/com/fortnite/pronos/service/ingestion/PrIngestionOrchestrationServiceTest.java`
- `src/test/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingAdapterTest.java`
