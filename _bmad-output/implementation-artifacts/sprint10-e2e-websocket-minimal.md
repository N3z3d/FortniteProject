# Story 10.4: E2E WebSocket STOMP — propagation PICK_MADE multi-context

Status: ready-for-dev

<!-- METADATA
  story_key: sprint10-e2e-websocket-minimal
  branch: story/sprint10-e2e-websocket-minimal
  sprint: Sprint 10
  Note: Pure E2E test addition — no backend changes, no frontend changes.
-->

## Story

As a developer,
I want a Playwright E2E test that opens two independent browser contexts simultaneously on the snake draft page,
so that I can verify STOMP event propagation (PICK_MADE) is correctly reflected in a second browser session without a page reload.

## Acceptance Criteria

1. A new file `frontend/e2e/websocket-stomp.spec.ts` exists with a single test `WS-01`.
2. The test opens two independent browser contexts (Context A = thibaut/creator, Context B = teddy/joiner) on the same `/games/{gameId}/draft/snake` route using `browser.newContext()`.
3. Context A picks the first available player card via UI (click card → click `.btn-confirm`) — confirming the pick.
4. Within 15 seconds of Context A confirming the pick, Context B's UI reflects the same player card as taken (CSS class `.player-card--taken`) **without a page reload** — verified via STOMP WebSocket push.
5. The test uses `SUITE_PREFIX = 'E2E-WS-'` for cleanup (via `cleanupE2eGames`).
6. The test has JSDoc at the file level documenting the multi-context pattern for future reference.
7. The `createStartedDraftGame` helper is adapted locally (using `E2E-WS-` prefix) or extracted to `app-helpers.ts` — whichever is simpler — so that `draft-flow.spec.ts` still works unchanged.
8. Test timeout is at least `120_000 ms` (game setup + two WS connections + pick propagation can take time).
9. The `browser.newContext()` pattern is used — NOT `browser.newPage()` on the same context — so that the two sessions have independent cookies/localStorage (independent login state).

## Tasks / Subtasks

- [ ] Task 1: Extract or duplicate `createStartedDraftGame` for `E2E-WS-` suite (AC: #5, #7)
  - [ ] 1.1: Decide between local duplication vs. extracting to `app-helpers.ts`
  - [ ] 1.2: Implement local `createStartedDraftGame` in `websocket-stomp.spec.ts` with `SUITE_PREFIX = 'E2E-WS-'` (simplest approach — avoids touching shared helpers)
  - [ ] 1.3: Verify `draft-flow.spec.ts` still compiles and runs unchanged

- [ ] Task 2: Implement `WS-01` test — two-context STOMP propagation (AC: #1, #2, #3, #4, #8, #9)
  - [ ] 2.1: Create `frontend/e2e/websocket-stomp.spec.ts` with file-level JSDoc describing multi-context pattern
  - [ ] 2.2: `test.beforeAll` — cleanup + `createStartedDraftGame` → `gameId`, initialize snake turn via API if needed
  - [ ] 2.3: Open Context A (thibaut): `browser.newContext()`, `forceLoginWithProfile`, navigate to `/games/{gameId}/draft/snake`, wait for `.my-turn-badge` (thibaut must be first picker, else skip)
  - [ ] 2.4: Open Context B (teddy): `browser.newContext()`, `forceLoginWithProfile`, navigate to `/games/{gameId}/draft/snake`, wait for `#player-list` to be visible (just needs to show the board)
  - [ ] 2.5: Context A: read name of first available card (`.player-card:not(.player-card--taken)`), click it, click `.btn-confirm`
  - [ ] 2.6: Context B: `expect.poll(() => page.locator('.player-card--taken').count(), { timeout: 15_000 }).toBeGreaterThan(0)` — verifies STOMP push reflected in UI
  - [ ] 2.7: `test.afterAll` — close both contexts
  - [ ] 2.8: Set `test.setTimeout(120_000)` at describe level

- [ ] Task 3: Verify test is runnable (AC: #1–#9)
  - [ ] 3.1: Confirm test appears in `npm run test:e2e -- --list` output
  - [ ] 3.2: (Optional, if Docker stack is running) Run `WS-01` alone via `npm run test:e2e -- --grep "WS-01"` and confirm pass

## Dev Notes

- **No backend changes**: backend already broadcasts STOMP events on `/topic/draft/{draftId}/snake` via `SnakeDraftService.validateAndAdvance()` [Source: `src/main/java/com/fortnite/pronos/service/draft/SnakeDraftService.java`]
- **No frontend changes**: `WebSocketService.subscribeToDraft(draftId)` + `draftEvents$` Observable already wires STOMP events to the UI [Source: `frontend/src/app/core/services/websocket.service.ts`]
- **WebSocket config**: SockJS endpoint `/ws`, app prefix `/app`, topics `/topic` + `/queue` [Source: `src/main/java/com/fortnite/pronos/config/WebSocketConfig.java`]
- **`browser.newContext()` is critical**: Each context has its own session storage + cookies → independent login state. Using `browser.newPage()` on the same context shares session → only one user would be authenticated.
- **Context A must be current picker**: The `.my-turn-badge` guard ensures Context A's thibaut is actually the current picker. If the game was already initialized and the turn order started with teddy, the test must handle that. Simplest: initialize the game fresh in `beforeAll`, force the turn order check via API (re-use `resolveCurrentPickerUsername` pattern from `draft-flow.spec.ts`).
- **`X-Test-User` header per context**: `page.context().setExtraHTTPHeaders({ 'X-Test-User': username })` — must be called on each context's page separately.
- **`createStartedDraftGame` from `draft-flow.spec.ts`**: The function uses `SUITE_PREFIX` (file-scoped constant). Duplicate it locally in `websocket-stomp.spec.ts` with its own `SUITE_PREFIX = 'E2E-WS-'` — no extraction needed. [Source: `frontend/e2e/draft-flow.spec.ts:143–174`]
- **`fetchCurrentTurn` / `resolveCurrentPickerUsername`**: Duplicate from `draft-flow.spec.ts` as needed, or use API directly to ensure thibaut is the first picker before proceeding.
- **Playwright config**: 1 worker, chromium, baseURL 4200, test timeout 30s (override to 120s at describe level). [Source: `frontend/playwright.config.ts`]
- **STOMP propagation SLA**: In local Docker, WS round-trip is <2s. Setting `timeout: 15_000` on `expect.poll` is ample. If the test runs against a remote server, consider 30s.
- **`#player-list` selector**: used in `DRAFT-01` test as the landmark for the draft board being rendered. [Source: `frontend/e2e/draft-flow.spec.ts:205`]
- **`.my-turn-badge` selector**: used in `DRAFT-02` to detect it's the current picker's turn. [Source: `frontend/e2e/draft-flow.spec.ts:223`]
- **`.player-card--taken`**: CSS class applied by SnakeDraftPageComponent when a player has been picked. [Source: `frontend/e2e/draft-flow.spec.ts:256`]
- **`.btn-confirm` selector**: confirm button in the pick zone. [Source: `frontend/e2e/draft-flow.spec.ts:231`]
- **`.confirm-zone .btn-confirm`**: Full selector for confirm button. [Source: `frontend/e2e/draft-flow.spec.ts:231`]

### Pre-existing Gaps / Known Issues

- [KNOWN] Backend pre-existing failures: ~15 failures + 1 error in GameDataIntegrationTest, FortniteTrackerServiceTddTest, GameStatisticsServiceTddTest — none related to this story.
- [KNOWN] Frontend Vitest: 2206 run, 2185 passing (21 Zone.js pre-existing — unrelated to E2E Playwright).
- [KNOWN] E2E tests require app running on :4200 + backend on :8080 (Docker stack `docker-compose.local.yml`). Without the stack, the test cannot run but the file compiles cleanly.
- [KNOWN] `createStartedDraftGame` in `draft-flow.spec.ts` uses `SUITE_PREFIX = 'E2E-DRAFT-82-'`. This story's local copy uses `'E2E-WS-'` — separate cleanup buckets, no conflict.

### Project Structure Notes

- New file: `frontend/e2e/websocket-stomp.spec.ts`
- Reuses helpers from: `frontend/e2e/helpers/app-helpers.ts` (`forceLoginWithProfile`, `cleanupE2eGames`, `createQuickGame`, `generateInvitationCode`, `joinWithInvitationCode`)
- No new helper files needed
- No changes to `frontend/playwright.config.ts`, `frontend/e2e/helpers/app-helpers.ts`, or any backend file

### References

- [Source: `frontend/e2e/draft-flow.spec.ts`] — `createStartedDraftGame`, `fetchCurrentTurn`, `resolveCurrentPickerUsername`, `fetchGameStatus`, selectors
- [Source: `frontend/e2e/helpers/app-helpers.ts`] — `forceLoginWithProfile`, `cleanupE2eGames`, `createQuickGame`, `generateInvitationCode`, `joinWithInvitationCode`
- [Source: `frontend/playwright.config.ts`] — playwright config (1 worker, chromium, baseURL 4200, timeout 30s)
- [Source: `src/main/java/com/fortnite/pronos/config/WebSocketConfig.java`] — SockJS endpoint `/ws`, topics `/topic`, app prefix `/app`
- [Source: `src/main/java/com/fortnite/pronos/service/draft/SnakeDraftService.java`] — `messagingTemplate.convertAndSend("/topic/draft/{draftId}/snake", ...)` in `validateAndAdvance()`
- [Source: `frontend/src/app/core/services/websocket.service.ts`] — `subscribeToDraft(draftId)`, `draftEvents$` Subject, `DraftEventMessage` interface
- [Source: `frontend/e2e/draft-full-flow.spec.ts`] — comment: "A full UI draft requires two simultaneous browser contexts coordinated over WebSocket"

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
