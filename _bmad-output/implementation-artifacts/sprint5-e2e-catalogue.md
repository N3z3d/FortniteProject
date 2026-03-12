# Story sprint5-e2e-catalogue: E2E coverage for the Player Catalogue

Status: done  <!-- review pass applied: H1 CAT-02 expect.poll+skip-on-zero, M1 CAT-05 toBeAttached with timeout, M2 CAT-03 isVisible guard before click, L1 CAT-02 fixed timeout→expect.poll, L2 CAT-04 not.toBeAttached -->

<!-- METADATA
  story_key: sprint5-e2e-catalogue
  branch: story/sprint5-e2e-catalogue
  sprint: Sprint 5
  Note: Validation is optional. Run validate-create-story for quality check before dev-story.
-->

## Story

As an authenticated user,
I want the `/catalogue` route to be fully covered by Playwright E2E tests,
so that regressions in the player list, search, region filter, comparison panel, and accessible list are caught automatically.

## Acceptance Criteria

1. A test `CAT-01` verifies `/catalogue` loads and renders the player list: `.catalogue-page` is visible, loading spinner disappears, and either `cdk-virtual-scroll-viewport.catalogue-viewport` (with players) or `.catalogue-empty` is visible.
2. A test `CAT-02` types a search term in the search input (inside `.search-field input`), waits for debounce (≥ 300 ms), and asserts that `.result-counter` text updates (changes from initial value). If the backend returns no data, the test skips gracefully.
3. A test `CAT-03` selects a region from `[data-testid="region-select"]`, waits for the filter to apply, and asserts `.result-counter` is visible with an updated count or `.catalogue-empty` is shown.
4. A test `CAT-04` selects up to 2 player cards (click `.catalogue-row app-player-card` twice), asserts `.comparison-panel` appears with 2 `.comparison-card` elements, then clicks `.comparison-panel__clear` and asserts the panel disappears. If fewer than 2 players are available, the test skips gracefully.
5. A test `CAT-05` verifies the accessible list `#accessible-list` is present in the DOM and (when players are loaded) contains at least one `.catalogue-accessible-list__item`.
6. All tests use `forceLoginWithProfile(page, 'thibaut')` for authentication and `test.skip()` gracefully when backend returns empty data or Docker is not running.
7. All tests follow the same defensive-selector pattern as existing E2E tests (guard URL check, `test.setTimeout(35_000)`, no assertions on specific data values — only structure).
8. The existing test suites (`admin.spec.ts`, `trade-*.spec.ts`) remain untouched and continue to pass.

## Tasks / Subtasks

- [x] Task 1: Add CAT-01 — catalogue loads and renders list (AC: #1)
  - [x] 1.1: `forceLoginWithProfile(page, 'thibaut')` then `page.goto('/catalogue')`
  - [x] 1.2: Guard check: after `waitForTimeout(2_000)`, skip if URL doesn't include `/catalogue`
  - [x] 1.3: Assert `.catalogue-page` visible (`timeout: 10_000`)
  - [x] 1.4: Wait for `.catalogue-loading` to disappear if present (`isVisible` + `waitFor hidden`, `timeout: 15_000`)
  - [x] 1.5: Assert `cdk-virtual-scroll-viewport.catalogue-viewport` or `.catalogue-empty` is visible (`timeout: 10_000`)

- [x] Task 2: Add CAT-02 — search by username updates result counter (AC: #2)
  - [x] 2.1: `forceLoginWithProfile(page, 'thibaut')` then navigate to `/catalogue`, wait for list
  - [x] 2.2: Read initial `.result-counter` text
  - [x] 2.3: Fill `page.locator('.search-field input').first()` with search term (e.g., `"a"`)
  - [x] 2.4: `await page.waitForTimeout(400)` (debounce is 200 ms + render margin)
  - [x] 2.5: Assert `.result-counter` is visible; if initial count was 0, skip gracefully

- [x] Task 3: Add CAT-03 — region filter applies correctly (AC: #3)
  - [x] 3.1: `forceLoginWithProfile(page, 'thibaut')` then navigate to `/catalogue`, wait for list
  - [x] 3.2: Click `[data-testid="region-select"]` to open the mat-select dropdown
  - [x] 3.3: Wait for `mat-option` panel to appear, click first non-null option
  - [x] 3.4: `await page.waitForTimeout(400)` for filter debounce
  - [x] 3.5: Assert `.result-counter` is visible OR `.catalogue-empty` is visible (`timeout: 8_000`)

- [x] Task 4: Add CAT-04 — comparison panel with 2 players (AC: #4)
  - [x] 4.1: `forceLoginWithProfile(page, 'thibaut')` then navigate to `/catalogue`, wait for list
  - [x] 4.2: Count `.catalogue-row` items; if count < 2, `test.skip()` (not enough data)
  - [x] 4.3: Click first `.catalogue-row app-player-card`
  - [x] 4.4: Click second `.catalogue-row app-player-card`
  - [x] 4.5: Assert `.comparison-panel` is visible (`timeout: 5_000`)
  - [x] 4.6: Assert `.comparison-card` count equals 2
  - [x] 4.7: Click `.comparison-panel__clear`
  - [x] 4.8: Assert `.comparison-panel` is NOT visible (or gone from DOM)

- [x] Task 5: Add CAT-05 — accessible list is present (AC: #5)
  - [x] 5.1: `forceLoginWithProfile(page, 'thibaut')` then navigate to `/catalogue`, wait for list
  - [x] 5.2: Assert `#accessible-list` is attached to DOM (`toBeAttached()`)
  - [x] 5.3: If catalogue has players, assert at least one `.catalogue-accessible-list__item` is present

## Dev Notes

### Implementation approach

Create a new file `frontend/e2e/catalogue.spec.ts`. Do NOT modify any existing spec files.

All 5 tests use this scaffold:

```typescript
test('CAT-0N: description', async ({ page }) => {
  test.setTimeout(35_000);

  await forceLoginWithProfile(page, 'thibaut');
  await page.goto('/catalogue');
  await page.waitForTimeout(2_000);

  if (!page.url().includes('/catalogue')) {
    test.skip();
    return;
  }

  // ... assertions
});
```

### Key DOM selectors (verified against template)

| Selector | Element | Notes |
|----------|---------|-------|
| `.catalogue-page` | root div | Always rendered |
| `.catalogue-filters` | filter section | Always rendered |
| `.search-field input` | search input | Inside PlayerSearchFilter |
| `[data-testid="region-select"]` | mat-select | data-testid attribute |
| `[data-testid="tranche-select"]` | mat-select | data-testid attribute |
| `.result-counter` | span[role="status"] | aria-live="polite" |
| `.catalogue-loading` | loading div | aria-busy="true" |
| `.catalogue-empty` | empty state div | When 0 results |
| `.catalogue-empty__cta` | signaler button | Inside empty state |
| `cdk-virtual-scroll-viewport.catalogue-viewport` | virtual scroll | When players present |
| `.catalogue-row` | each player row | Inside virtual scroll |
| `app-player-card` | player card | Inside .catalogue-row |
| `.comparison-panel` | comparison panel | Only when ≥1 player selected |
| `.comparison-card` | individual comparison | 1..2 per comparison-panel |
| `.comparison-panel__clear` | clear button | Inside comparison-panel |
| `#accessible-list` | sr-only list | Always in DOM |
| `.catalogue-accessible-list__item` | list items | Max 50 items |

### mat-select interaction pattern

`[data-testid="region-select"]` is a `mat-select` inside `mat-form-field`. To interact:

```typescript
// Click the trigger to open the dropdown
await page.locator('[data-testid="region-select"]').click();

// Wait for the mat-option panel (renders in CDK overlay at document.body level)
const options = page.locator('mat-option');
await expect(options.first()).toBeVisible({ timeout: 5_000 });

// Click first non-null option (index 1, index 0 is "Toutes")
const firstRealOption = options.nth(1);
if (await firstRealOption.isVisible({ timeout: 2_000 }).catch(() => false)) {
  await firstRealOption.click();
} else {
  test.skip(); // No regions available
  return;
}
```

Note: `mat-option` renders in a CDK overlay portal at `document.body`, NOT scoped inside the mat-select. Use `page.locator('mat-option')` globally.

### PlayerCard click interaction

`app-player-card` renders in browse mode with a cardSelected EventEmitter. In the template:
```html
<app-player-card [player]="player" mode="browse" [selected]="isCompared(player)" (cardSelected)="onCardSelected($event)">
```
Click the card element itself — `page.locator('.catalogue-row').nth(0).locator('app-player-card')`.

### Loading state handling

The component sets `this.loading = true` before the HTTP call and `false` in subscribe. Use:
```typescript
const loadingEl = page.locator('.catalogue-loading');
if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
  await loadingEl.waitFor({ state: 'hidden', timeout: 15_000 });
}
```

### Filter debounce

`FILTER_DEBOUNCE_MS = 200` in the component. After any search input or filter change, wait at minimum `400ms` to allow debounce + HTTP call + render cycle. Use `page.waitForTimeout(400)`.

### Backend API reference

- `GET /api/players/catalogue` — list (params: `region`, `available=true`)
- `GET /api/players/catalogue/search?q=xxx` — search
- `GET /api/players/{id}/sparkline?region=EU&days=14` — sparkline (not tested in this story)
- Response type: `CataloguePlayerDto[]` → mapped to `AvailablePlayer[]`

### Auth pattern

Route `/catalogue` has NO auth guard (accessible to all authenticated users, no admin required). Use `forceLoginWithProfile(page, 'thibaut')`. Do NOT use `loginAsAdmin`.

### Project Structure Notes

```
frontend/e2e/
├── admin.spec.ts           ← DO NOT TOUCH
├── trade-dashboard.spec.ts ← DO NOT TOUCH
├── trade-swap-flow.spec.ts ← DO NOT TOUCH
├── trade-multi-flow.spec.ts ← DO NOT TOUCH
├── catalogue.spec.ts       ← CREATE THIS FILE
├── smoke.spec.ts
├── game-lifecycle.spec.ts
├── draft-flow.spec.ts
└── helpers/
    └── app-helpers.ts      ← import forceLoginWithProfile from here
```

### Pre-existing Gaps / Known Issues

- [KNOWN] 21 Vitest unit test failures (Zone.js debounce timing) — pre-existing, unrelated to this story.
- [KNOWN] Catalogue API returns empty array if Docker local backend has no seeded players. Tests must skip gracefully (not fail) when catalogue is empty.
- [KNOWN] `cdk-virtual-scroll-viewport` requires sufficient height in the browser viewport to render items. Playwright default viewport (1280×720) should be sufficient.
- [KNOWN] `mat-select` panel renders as a CDK overlay at `document.body` level — do NOT scope the `mat-option` locator inside the `mat-form-field`.
- [KNOWN] Comparison panel: clicking `app-player-card` triggers `(cardSelected)` event. The card itself is a standalone component — click the `app-player-card` element or its inner `.player-card` element.

### Testing standards summary

- `test.setTimeout(35_000)` on every test
- `forceLoginWithProfile(page, 'thibaut')` for authentication (NOT `loginAsAdmin`)
- Guard URL check after `page.waitForTimeout(2_000)` → `test.skip()` if redirected
- Wait for loading: `isVisible({ timeout: 3_000 }).catch(() => false)` before `.waitFor({ state: 'hidden' })`
- `expect(...).toBeVisible({ timeout: 10_000 })` for primary container
- Do NOT assert specific player names, counts, or data values — only structure
- Use `page.locator(...).count()` checks with graceful skips when no data

### References

- Route definition: [Source: frontend/src/app/app.routes.ts — `path: 'catalogue'`]
- Component template: [Source: frontend/src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.html]
- Component logic: [Source: frontend/src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.ts]
- Service: [Source: frontend/src/app/features/catalogue/services/player-catalogue.service.ts]
- PlayerSearchFilter template (selectors): [Source: frontend/src/app/shared/components/player-search-filter/player-search-filter.component.html]
- Auth helper: [Source: frontend/e2e/helpers/app-helpers.ts — `forceLoginWithProfile`]
- Playwright config: [Source: frontend/playwright.config.ts] — baseURL=http://localhost:4200, workers=1, timeout=30_000
- Existing E2E pattern reference: [Source: frontend/e2e/admin.spec.ts — ADMIN-02..09 pattern]
- Filter debounce: [Source: frontend/src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.ts:27 — `FILTER_DEBOUNCE_MS = 200`]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ **Task 1 — CAT-01**: `forceLoginWithProfile(page, 'thibaut')` → `/catalogue` → guard URL → `.catalogue-page` visible → loading wait → `cdk-virtual-scroll-viewport.catalogue-viewport` or `.catalogue-empty`.
- ✅ **Task 2 — CAT-02**: Search input `.search-field input` filled with `"a"`, `waitForTimeout(400)` debounce, `.result-counter` visibility asserted. Graceful skip if no initial data.
- ✅ **Task 3 — CAT-03**: `[data-testid="region-select"]` clicked → `mat-option` CDK overlay → `nth(1)` non-null option clicked → 400ms wait → `.result-counter` or `.catalogue-empty` asserted. Graceful skip if no regions.
- ✅ **Task 4 — CAT-04**: `.catalogue-row` count guard (< 2 → skip) → click 2 `app-player-card` → `.comparison-panel` visible → `.comparison-card` count = 2 → `.comparison-panel__clear` → panel gone.
- ✅ **Task 5 — CAT-05**: `#accessible-list` `toBeAttached()` → if `cdk-virtual-scroll-viewport` visible → `.catalogue-accessible-list__item` count > 0.
- ✅ **ACs 1–8 all satisfied**: 5 tests (CAT-01..05) with `forceLoginWithProfile`, `test.setTimeout(35_000)`, guard URL check, graceful skips, no data value assertions.
- ✅ **Existing specs untouched**: `admin.spec.ts`, `trade-*.spec.ts` not modified.
- ✅ **TypeScript**: `npx tsc --noEmit` clean on both `tsconfig.app.json` and `tsconfig.spec.json` (0 errors).
- ✅ **Vitest baseline unchanged**: 2212-2213/2234 (21-22 pre-existing Zone.js failures — flaky ±1, no regression caused by this story).
- ✅ **Code Review pass** — 5 findings fixed: H1 (CAT-02 `initialText` dead code → skip on 0 + `expect.poll` for AC#2 compliance), M1 (CAT-05 immediate `.count()` → `.first().toBeAttached({ timeout: 5_000 })` to wait for async list population), M2 (CAT-03 blind click → `isVisible` guard before `regionSelect.click()`), L1 (CAT-02 fixed 400ms → `expect.poll` stabilisation), L2 (CAT-04 `not.toBeVisible` → `not.toBeAttached` for DOM removal assertion).

### File List

- `_bmad-output/implementation-artifacts/sprint5-e2e-catalogue.md`
- `frontend/e2e/catalogue.spec.ts` — **rewritten** (CAT-01..CAT-05, full story spec compliance)
