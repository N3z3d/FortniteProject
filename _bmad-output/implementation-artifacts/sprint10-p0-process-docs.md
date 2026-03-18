# Story 10.5: Process Documentation — §E2E Limitations, §Config Production, Adapter DoD, @ConditionalOnProperty

Status: ready-for-dev

<!-- METADATA
  story_key: sprint10-p0-process-docs
  branch: story/sprint10-p0-process-docs
  sprint: Sprint 10
  Note: Documentation-only story — no production code changes. All changes are to project-context.md.
-->

## Story

As a developer,
I want project-context.md to document E2E test limitations, production configuration env vars, the adapter DoD checklist, and the @ConditionalOnProperty pattern,
so that future AI agents and developers have complete context when working with these areas without rediscovering them from scratch.

## Acceptance Criteria

1. **§E2E Limitations section** added to `project-context.md` documenting:
   - E2E tests require Docker stack running (`docker-compose.local.yml`) — app on :4200, backend on :8080
   - `browser.newContext()` vs `browser.newPage()` distinction (multi-user session isolation)
   - STOMP subscription timing caveat: wait for `.player-card` (data rendered) not just `#player-list` (DOM visible) before triggering WS events in tests
   - `X-Test-User` header must be set per-context via `page.context().setExtraHTTPHeaders()`
   - `SUITE_PREFIX` convention: each suite uses its own prefix for cleanup isolation (`E2E-WS-`, `E2E-DRAFT-82-`, etc.)

2. **§Config Production section** added to `project-context.md` documenting all env vars required to activate the pipeline in production:
   - `INGESTION_PR_SCHEDULED_ENABLED=true` — activates `PrIngestionOrchestrationService` bean (`@ConditionalOnProperty`)
   - `INGESTION_PR_SCHEDULED_CRON` — optional override, default `0 0 5 * * *` (05:00 UTC daily)
   - `SCRAPING_FORTNITETRACKER_PAGES_PER_REGION` — default 1 (dev/dry-run), set to 4 for full production scrape
   - `SCRAPING_FORTNITETRACKER_SCRAPFLY_KEYS`, `SCRAPING_FORTNITETRACKER_SCRAPERAPI_KEYS`, `SCRAPING_FORTNITETRACKER_SCRAPEDO_TOKEN` — proxy API keys (comma-separated, never commit)
   - Activation prerequisite: dry-run must pass (`POST /api/admin/scraping/dry-run?region=EU`) before enabling scheduler

3. **Adapter DoD checklist** added to §6 Definition of Done documenting the mandatory steps when adding a new external adapter (e.g., scraping adapter):
   - Define port interface in `service/ingestion/` or appropriate domain port package
   - Mock adapter for tests / local dev (no real external calls in CI)
   - Dry-run endpoint (`/dry-run`) BEFORE enabling scheduled production use
   - Smoke check validation (rows >= threshold) implemented in adapter or orchestrator
   - Env vars documented in `application.properties` as commented-out examples (never default to real keys)
   - `@ConditionalOnProperty` used for any bean that should be OFF by default in prod

4. **@ConditionalOnProperty pattern** added to §3 Backend Critical Rules documenting:
   - When to use: any `@Service` that should be OFF by default (scheduled jobs, external adapters, feature flags)
   - Pattern: `@ConditionalOnProperty(name = "feature.enabled", havingValue = "true", matchIfMissing = false)`
   - Spring DI with missing conditional bean: inject as `Optional<MyService>` — Spring injects `Optional.empty()` when bean absent
   - In `@WebMvcTest` context: conditional bean is absent → `Optional.empty()` auto-injected, no `@MockBean` needed
   - Two-constructor `@Autowired` rule: if `@Service` has 2 constructors (prod + test), annotate prod constructor with `@Autowired`
   - Reference: `PrIngestionOrchestrationService` + `AdminScrapeController` as canonical example

5. **§FortniteTracker CGU/ToS status** added to §Config Production documenting the legal status of scraping:
   - Document whether FortniteTracker.com ToS explicitly prohibits automated scraping
   - Document the proxy architecture rationale (Scrapfly/ScraperAPI/Scrape.do) — rate limiting, IP rotation
   - Note that scraping is done via proxy services (not direct), limiting direct liability exposure
   - Recommend periodic ToS review (quarterly) as FortniteTracker may update terms

6. **`_Last Updated` line** in `project-context.md` updated to reflect Sprint 10 changes.

## Tasks / Subtasks

- [ ] Task 1: Add §E2E Limitations section to `project-context.md` (AC: #1)
  - [ ] 1.1: Add new section `## §E2E Tests — Limitations et Patterns` after the existing §WebSocket Security Pattern section
  - [ ] 1.2: Document Docker stack prerequisite, multi-context pattern, STOMP timing caveat, X-Test-User header, SUITE_PREFIX convention

- [ ] Task 2: Add §Config Production section to `project-context.md` (AC: #2)
  - [ ] 2.1: Add new section `## §Config Production — Pipeline FortniteTracker` after §E2E Limitations
  - [ ] 2.2: Document all env vars with defaults, activation prerequisite (dry-run first), `application.properties` commented reference

- [ ] Task 3: Add adapter DoD checklist to §6 Definition of Done (AC: #3)
  - [ ] 3.1: Add "Adapter externe" row in the DoD table, OR add a dedicated sub-section below the DoD table listing the 6 checklist items

- [ ] Task 4: Add @ConditionalOnProperty pattern to §3 Backend Critical Rules (AC: #4)
  - [ ] 4.1: Add sub-section `### @ConditionalOnProperty — Beans optionnels` in §3
  - [ ] 4.2: Document the pattern, Optional injection, @WebMvcTest behavior, two-constructor rule, and canonical reference

- [ ] Task 5: Add §FortniteTracker CGU/ToS status (AC: #5)
  - [ ] 5.1: Research FortniteTracker.com ToS (check `tracker.gg/fortnite` — parent site `tracker.gg`) for automated access clauses
  - [ ] 5.2: Add `### §FortniteTracker ToS Status` sub-section inside §Config Production documenting findings and quarterly review recommendation

- [ ] Task 6: Update `_Last Updated` line (AC: #6)
  - [ ] 6.1: Update the last line of `project-context.md` to mention Sprint 10 additions

## Dev Notes

- **Pure documentation story**: only `project-context.md` is modified. No Java, TypeScript, SQL, or test files.
- **project-context.md location**: `_bmad-output/project-context.md` [Source: `_bmad-output/project-context.md`]
- **Current last section**: `## §WebSocket Security Pattern (Sprint 8 — SEC-R2)` followed by `## Usage Guidelines` — new sections go BETWEEN these two. [Source: `_bmad-output/project-context.md:327–391`]
- **Canonical @ConditionalOnProperty example**: `PrIngestionOrchestrationService` uses `@ConditionalOnProperty(name = "ingestion.pr.scheduled.enabled", havingValue = "true", matchIfMissing = false)`. `AdminScrapeController` injects `Optional<PrIngestionOrchestrationService>`. [Source: `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionOrchestrationService.java:27-30`, `src/main/java/com/fortnite/pronos/controller/AdminScrapeController.java:39`]
- **All env vars**: documented in `application.properties` lines 33-45. [Source: `src/main/resources/application.properties`]
- **Proxy keys**: `SCRAPING_FORTNITETRACKER_SCRAPFLY_KEYS`, `SCRAPING_FORTNITETRACKER_SCRAPERAPI_KEYS`, `SCRAPING_FORTNITETRACKER_SCRAPEDO_TOKEN` [Source: `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingProperties.java`]
- **FortniteTracker ToS**: tracker.gg is the parent platform. Search for `tracker.gg/legal` or `tracker.gg/terms-of-service`. Expected finding: no explicit prohibition on reading public leaderboard pages, but automated scraping is typically in a grey zone. Document factually without legal advice.
- **E2E patterns**: documented from sprint10-e2e-websocket-minimal review findings — WS timing (`player-card` before pick), `browser.newContext()` isolation, SUITE_PREFIX per suite. [Source: `frontend/e2e/websocket-stomp.spec.ts`]
- **No code review needed**: documentation-only stories with no logic → code review is not meaningful. Story can go directly to `done` after dev.

### Pre-existing Gaps / Known Issues

- [KNOWN] Backend pre-existing failures: ~15 failures + 1 error — none related to this documentation story.
- [KNOWN] Frontend Vitest: 2185/2206 passing (21 Zone.js pre-existing) — not affected.
- [KNOWN] `project-context.md` mentions baseline "2243 tests, 0 failure" (Sprint 7) — actual current baseline is 2206 run, 2185 passing (21 Zone.js). Update this baseline figure in §4 Tests Frontend while editing the file.

### Project Structure Notes

- Only file modified: `_bmad-output/project-context.md`
- No backend, frontend, test, or migration files touched
- No spotless, no TypeScript compilation needed

### References

- [Source: `_bmad-output/project-context.md`] — file to modify, current state
- [Source: `src/main/resources/application.properties:33-45`] — env vars for scraping + scheduler
- [Source: `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionOrchestrationService.java:27-30`] — @ConditionalOnProperty usage
- [Source: `src/main/java/com/fortnite/pronos/controller/AdminScrapeController.java:39-49`] — Optional<T> injection pattern
- [Source: `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingProperties.java`] — all scraping config keys
- [Source: `frontend/e2e/websocket-stomp.spec.ts`] — E2E multi-context patterns just implemented
- [Source: `frontend/e2e/draft-flow.spec.ts`] — SUITE_PREFIX convention + existing E2E patterns

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
