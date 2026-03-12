# Story sprint5-e2e-admin-panel: E2E coverage for all admin panel routes

Status: done  <!-- review pass applied: F1+F2 HIGH fixed, F3+F4+F5 MEDIUM fixed, F6-F8 LOW fixed -->

<!-- METADATA
  story_key: sprint5-e2e-admin-panel
  branch: story/sprint5-e2e-admin-panel
  sprint: Sprint 5
  Note: Validation is optional. Run validate-create-story for quality check before dev-story.
-->

## Story

As an admin user,
I want full E2E coverage of every admin panel route,
so that regressions are caught automatically whenever any admin component changes.

## Acceptance Criteria

1. A new test `ADMIN-06` verifies that an admin user can reach `/admin` via the **profile dropdown menu** in the top navbar (`.user-section button[mat-button]` → `.admin-menu-item`), implementing the navigation path added in `sprint5-ux-navbar-globale`.
2. A new test `ADMIN-07` navigates to `/admin/database` and asserts that the DB Explorer page renders (`.db-explorer` container, `.db-explorer__title`, `.db-explorer__query-section`).
3. A new test `ADMIN-08` navigates to `/admin/logs` and asserts that the Logs page renders (`.logs-page` container with a `mat-tab-group` containing at least 2 tabs).
4. A new test `ADMIN-09` navigates to `/admin/errors` and asserts that the Error Journal page renders (`.error-journal-container` container, `.page-header`, `.stats-grid` or loading state).
5. All new tests use `loginAsAdmin(page)` + `test.skip()` gracefully when no admin credentials are available.
6. All new tests follow the same defensive-selector pattern as ADMIN-02..05 (check URL after nav, skip if redirected away from admin).
7. The existing ADMIN-01..05 tests remain untouched and continue to pass.

## Tasks / Subtasks

- [x] Task 1: Add ADMIN-06 — navbar profile dropdown → admin access (AC: #1)
  - [x] 1.1: Login as admin with `loginAsAdmin(page)`, navigate to `/games` (page that renders the navbar)
  - [x] 1.2: Click the profile dropdown trigger (`button[mat-button]` inside `.user-section`)
  - [x] 1.3: Wait for the mat-menu to appear, then click the `.admin-menu-item` button
  - [x] 1.4: Assert URL contains `/admin` and `.admin-dashboard` is visible

- [x] Task 2: Add ADMIN-07 — DB Explorer page (AC: #2)
  - [x] 2.1: Login as admin, navigate directly to `/admin/database`
  - [x] 2.2: Guard check: if URL doesn't contain `/admin/database`, skip
  - [x] 2.3: Assert `.db-explorer` container is visible (timeout 10 000 ms)
  - [x] 2.4: Assert `.db-explorer__query-section` is visible (SQL read-only section)
  - [x] 2.5: Wait for loading to finish (`.db-explorer__loading` disappears), then assert either `.db-explorer__table` or `.db-explorer__empty` is visible

- [x] Task 3: Add ADMIN-08 — Logs page (AC: #3)
  - [x] 3.1: Login as admin, navigate directly to `/admin/logs`
  - [x] 3.2: Guard check: if URL doesn't contain `/admin/logs`, skip
  - [x] 3.3: Assert `.logs-page` container is visible (timeout 10 000 ms)
  - [x] 3.4: Assert `mat-tab-group` inside `.logs-page` is visible
  - [x] 3.5: Assert tab count ≥ 2 (scrape logs tab + audit entries tab)

- [x] Task 4: Add ADMIN-09 — Error Journal page (AC: #4)
  - [x] 4.1: Login as admin, navigate directly to `/admin/errors`
  - [x] 4.2: Guard check: if URL doesn't contain `/admin/errors`, skip
  - [x] 4.3: Assert `.error-journal-container` is visible (timeout 10 000 ms)
  - [x] 4.4: Assert `.page-header` is visible
  - [x] 4.5: Wait for loading state: if `.loading-container` visible, wait for it to disappear (timeout 12 000 ms); then assert `.stats-grid` or `.error-container` is visible

## Dev Notes

### Implementation approach

Extend `frontend/e2e/admin.spec.ts` by appending 4 new `test(...)` blocks after the existing ADMIN-05 block. Do NOT modify the existing tests.

All 4 tests follow this exact scaffold:

```typescript
test('ADMIN-0N: description', async ({ page }) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  if (!loggedIn) {
    test.skip();
    return;
  }

  await page.goto('/admin/<route>');

  await page.waitForTimeout(2_000);
  const url = page.url();
  if (!url.includes('/admin/<route>')) {
    test.skip();
    return;
  }

  // ... assertions
});
```

### ADMIN-06 specifics — navbar dropdown navigation

The profile dropdown is rendered by `main-layout.component.html`:

```html
<!-- trigger -->
<button mat-button [matMenuTriggerFor]="profileMenu" ...>
  <mat-icon>account_circle</mat-icon>
  <span class="nav-label">{{ currentUser?.username }}</span>
  <mat-icon>arrow_drop_down</mat-icon>
</button>

<!-- menu item (admin only) -->
<button mat-menu-item [routerLink]="'/admin'" class="admin-menu-item">
  <mat-icon>admin_panel_settings</mat-icon>
  Administration
</button>
```

Playwright locator strategy:

```typescript
// Open the profile dropdown
const profileTrigger = page.locator('.user-section button[mat-button]').first();
await profileTrigger.click();

// Wait for the mat-menu overlay to appear
const adminMenuItem = page.locator('.admin-menu-item');
await expect(adminMenuItem).toBeVisible({ timeout: 5_000 });
await adminMenuItem.click();

// Assert landing on /admin
await page.waitForURL(/\/admin/, { timeout: 10_000 });
await expect(page.locator('.admin-dashboard')).toBeVisible({ timeout: 10_000 });
```

Note: `mat-menu` renders items in a CDK overlay portal outside the nav DOM. Use `page.locator('.admin-menu-item')` (not scoped inside `.user-section`) after the trigger click.

### Source tree components to touch

| File | Action |
|------|--------|
| `frontend/e2e/admin.spec.ts` | **Extend** — append ADMIN-06..09 after line 227 |

No other files need modification.

### Key DOM selectors by route

| Route | Root container | Loading | Success state |
|-------|---------------|---------|---------------|
| `/admin/database` | `.db-explorer` | `.db-explorer__loading` | `.db-explorer__table` or `.db-explorer__empty` |
| `/admin/logs` | `.logs-page` | implicit (mat-spinner) | `mat-tab-group` with 2+ tabs |
| `/admin/errors` | `.error-journal-container` | `.loading-container` | `.stats-grid` or `.error-container` |

### Testing standards summary

- `test.setTimeout(35_000)` on every test (admin APIs can be slow)
- `loginAsAdmin(page)` → `test.skip()` if returns `false`
- Guard URL check after `page.waitForTimeout(2_000)` → `test.skip()` if redirected
- Spinner/loading checks: `isVisible({ timeout: 3_000 }).catch(() => false)` before `.waitFor({ state: 'hidden' })`
- `expect(...).toBeVisible({ timeout: 10_000 })` for primary container
- Do NOT assert specific data values — only structure (containers, tables, headers)

### Pre-existing Gaps / Known Issues

- [KNOWN] 22 Vitest unit test failures (Zone.js debounce timing) — pre-existing, unrelated to this story. Source: vitest.config.mts migration.
- [KNOWN] `ADMIN-02..05` require `E2E_ADMIN_USER` env var or `DEV_PROFILE_MAP.admin` (id=1) in local seed. Without it, tests are gracefully skipped.
- [KNOWN] `loginAsAdmin()` uses `.user-profile-btn` selector or falls back to `E2E_ADMIN_USER` env var. Source: `frontend/e2e/helpers/app-helpers.ts`.
- [KNOWN] Playwright `workers: 1`, `fullyParallel: false` — tests run sequentially. Do NOT add `test.describe.parallel`.

### Project Structure Notes

```
frontend/e2e/
├── admin.spec.ts          ← target file (extend only)
├── helpers/
│   └── app-helpers.ts     ← loginAsAdmin() already defined
├── smoke.spec.ts
├── game-lifecycle.spec.ts
└── draft-flow.spec.ts
```

Admin Angular routes (from `frontend/src/app/features/admin/admin.routes.ts`):
- `''` → AdminDashboardComponent
- `errors` → ErrorJournalComponent (`.error-journal-container`)
- `pipeline` → AdminPipelinePageComponent (`.pipeline-header`)
- `incidents` → AdminIncidentListComponent
- `games` → AdminGamesSupervisionComponent (`.supervision-page__title`)
- `database` → AdminDbExplorerComponent (`.db-explorer`)
- `users` → AdminUserListComponent (`.user-list`)
- `logs` → AdminLogsComponent (`.logs-page`)

### References

- Existing tests pattern: [Source: frontend/e2e/admin.spec.ts#1-227]
- Navbar dropdown HTML: [Source: frontend/src/app/shared/components/main-layout/main-layout.component.html#60-89]
- DB Explorer selectors: [Source: frontend/src/app/features/admin/db-explorer/admin-db-explorer.component.html#1-92]
- Logs page selectors: [Source: frontend/src/app/features/admin/logs/admin-logs.component.html#1-114]
- Error Journal selectors: [Source: frontend/src/app/features/admin/error-journal/error-journal.component.html#1-60]
- Admin routes: [Source: frontend/src/app/features/admin/admin.routes.ts]
- Playwright config: [Source: frontend/playwright.config.ts] — baseURL=http://localhost:4200, workers=1, timeout=30_000
- loginAsAdmin helper: [Source: frontend/e2e/helpers/app-helpers.ts] — DEV_PROFILE_MAP.admin = {id: 1}

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- ✅ **Task 1 — ADMIN-06** : test Playwright ajouté dans `frontend/e2e/admin.spec.ts`. Login admin → `/games` → clic sur `.user-section button[mat-button]` → clic `.admin-menu-item` (CDK overlay) → `waitForURL(/\/admin/)` → assert `.admin-dashboard` visible.
- ✅ **Task 2 — ADMIN-07** : test `/admin/database` — assert `.db-explorer`, `.db-explorer__query-section`, wait loading, assert `.db-explorer__table` ou `.db-explorer__empty`.
- ✅ **Task 3 — ADMIN-08** : test `/admin/logs` — assert `.logs-page`, `mat-tab-group`, count `[role="tab"]` ≥ 2.
- ✅ **Task 4 — ADMIN-09** : test `/admin/errors` — assert `.error-journal-container`, `.page-header`, wait `.loading-container`, assert `.stats-grid` ou `.error-container`.
- ✅ **ACs 1–7 tous satisfaits** : tests ADMIN-06..09 ajoutés, scaffold défensif identique à ADMIN-02..05, ADMIN-01..05 intacts, `loginAsAdmin()` + `test.skip()` gracieux.
- ✅ **0 régression Vitest** : 2213/2234 (21 pré-existants Zone.js inchangés).
- ✅ **TypeScript** : compilation sans erreur (`npx tsc --noEmit`).

### File List

- `frontend/e2e/admin.spec.ts` — modifié (ajout tests ADMIN-06..09 après ligne 228)
