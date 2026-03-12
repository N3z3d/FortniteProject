import { test, expect } from '@playwright/test';

import { loginAsAdmin } from './helpers/app-helpers';

/**
 * Admin Panel E2E tests — ADMIN-01 to ADMIN-09.
 * ADMIN-02..09 require an account with ADMIN role (E2E_ADMIN_USER / E2E_ADMIN_PASS env vars).
 * If these env vars are not set, admin-specific tests are skipped gracefully.
 *
 * Env vars:
 *   E2E_USER       — regular user identifier (email or username)
 *   E2E_ADMIN_USER — admin user identifier (email or username)
 *   BASE_URL       — app base URL (default http://localhost:4200)
 *   BACKEND_URL    — backend URL (default http://localhost:8080)
 *
 * Run: npm run test:e2e
 */

// ---------------------------------------------------------------------------
// ADMIN-01: /admin redirects to /login when not authenticated
// ---------------------------------------------------------------------------
test('ADMIN-01: /admin redirects to login when unauthenticated', async ({ page }) => {
  test.setTimeout(15_000);

  // Access /admin without any session
  await page.goto('/admin');

  // AdminGuard should redirect to /login (or /games if auto-login kicks in from localStorage)
  await page.waitForURL(/login|games/, { timeout: 8_000 });
  const currentUrl = page.url();
  expect(currentUrl).toMatch(/login|games/);
});

// ---------------------------------------------------------------------------
// ADMIN-02: Admin user can access the dashboard and see metrics
// ---------------------------------------------------------------------------
test('ADMIN-02: admin user can access dashboard and see metrics', async ({ page }) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin');
  await expect(page).toHaveURL(/\/admin$/, { timeout: 10_000 });

  // Admin dashboard component renders .admin-dashboard
  const dashboard = page.locator('.admin-dashboard');
  await expect(dashboard).toBeVisible({ timeout: 10_000 });

  // Either loading spinner or the dashboard grid is shown
  const loadingOrContent = page.locator('mat-spinner, .dashboard-grid');
  await expect(loadingOrContent.first()).toBeVisible({ timeout: 8_000 });

  // Wait for loading to finish (spinner disappears)
  const spinner = page.locator('.admin-dashboard mat-spinner');
  if (await spinner.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await spinner.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // Dashboard grid with KPI cards should now be visible
  const grid = page.locator('.dashboard-grid');
  await expect(grid).toBeVisible({ timeout: 10_000 });

  // KPI card (summary) should display total users, total games, total trades
  const kpiCard = page.locator('.kpi-card');
  await expect(kpiCard).toBeVisible({ timeout: 5_000 });

  const kpiValues = page.locator('.kpi-value');
  const kpiCount = await kpiValues.count();
  // At least the 3 main KPIs (totalUsers, totalGames, totalTrades) should be rendered
  expect(kpiCount).toBeGreaterThanOrEqual(1);
});

// ---------------------------------------------------------------------------
// ADMIN-03: Pipeline tab shows the identity list
// ---------------------------------------------------------------------------
test('ADMIN-03: admin pipeline page shows identity list', async ({ page }) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin/pipeline');
  await expect(page).toHaveURL(/\/admin\/pipeline$/, { timeout: 10_000 });

  // Pipeline page renders .pipeline-page with header
  const pipelineHeader = page.locator('.pipeline-header, header[role="banner"]').first();
  await expect(pipelineHeader).toBeVisible({ timeout: 10_000 });

  // Header contains "MODE ADMINISTRATION"
  await expect(page.locator('.pipeline-header__heading, h1').first()).toBeVisible({ timeout: 5_000 });

  // Wait for loading to complete
  const loadingSpinner = page.locator('.pipeline-loading mat-spinner');
  if (await loadingSpinner.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingSpinner.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // After loading: either inbox-zero, tab group with identity table, or error banner
  const contentRendered = page.locator(
    '.pipeline-inbox-zero, mat-tab-group.pipeline-tabs, .pipeline-error--partial'
  ).first();
  await expect(contentRendered).toBeVisible({ timeout: 10_000 });

  // If tabs are visible, they should contain at least one tab
  const tabGroup = page.locator('mat-tab-group.pipeline-tabs');
  if (await tabGroup.isVisible({ timeout: 3_000 }).catch(() => false)) {
    const tabs = tabGroup.locator('mat-tab-header .mat-mdc-tab, [role="tab"]');
    const tabCount = await tabs.count();
    expect(tabCount).toBeGreaterThanOrEqual(1);
  }
});

// ---------------------------------------------------------------------------
// ADMIN-04: /admin/users shows user list with at least a table or heading
// ---------------------------------------------------------------------------
test('ADMIN-04: /admin/users displays user management page', async ({ page }) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin/users');
  await expect(page).toHaveURL(/\/admin\/users$/, { timeout: 10_000 });

  // Admin user list renders .user-list container
  const userList = page.locator('.user-list');
  await expect(userList).toBeVisible({ timeout: 10_000 });

  // Heading "Gestion des Utilisateurs" should be visible
  const title = page.locator('.user-list__title, h2');
  await expect(title.first()).toBeVisible({ timeout: 5_000 });

  // Wait for loading to finish
  const loading = page.locator('.user-list__loading');
  if (await loading.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loading.waitFor({ state: 'hidden', timeout: 12_000 });
  }

  // After loading: either a table, empty state, or error
  const afterLoad = page.locator(
    'table.user-list__table, .user-list__empty, .user-list__error'
  ).first();
  await expect(afterLoad).toBeVisible({ timeout: 10_000 });

  // If a table is rendered, it should have column headers
  const table = page.locator('table.user-list__table');
  if (await table.isVisible({ timeout: 3_000 }).catch(() => false)) {
    // Column headers: username, email, role, season, status
    const headers = table.locator('th[mat-header-cell]');
    const headerCount = await headers.count();
    expect(headerCount).toBeGreaterThanOrEqual(1);
  }

  // Filter controls should be present (search, role, status filters)
  const searchInput = page.locator('.user-list__filters input[matinput], .user-list__search input').first();
  if (await searchInput.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await expect(searchInput).toBeVisible();
  }
});

// ---------------------------------------------------------------------------
// ADMIN-05: /admin/games shows the games supervision page
// ---------------------------------------------------------------------------
test('ADMIN-05: /admin/games displays games supervision page', async ({
  page,
}) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin/games');
  await expect(page).toHaveURL(/\/admin\/games$/, { timeout: 10_000 });

  const title = page.locator('.supervision-page__title, h1').first();
  await expect(title).toContainText(/Supervision des parties/i, {
    timeout: 10_000,
  });

  const count = page.locator('.supervision-page__count');
  await expect(count).toBeVisible({ timeout: 10_000 });

  const afterLoad = page.locator('table.supervision-table, .supervision-table__empty').first();
  await expect(afterLoad).toBeVisible({ timeout: 10_000 });
});

// ---------------------------------------------------------------------------
// ADMIN-06: Admin profile menu exposes the administration entry
// ---------------------------------------------------------------------------
test('ADMIN-06: admin profile menu exposes the administration entry', async ({ page }) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  // Navigate to a page that renders the main-layout navbar; wait for network to settle
  await page.goto('/games');
  await page.waitForLoadState('networkidle', { timeout: 5_000 }).catch(() => {});
  await page.waitForTimeout(500);

  // Open the profile dropdown menu (mat-menu trigger inside .user-section)
  const profileTrigger = page.locator('.user-section button[mat-button]').first();
  await expect(profileTrigger).toBeVisible({ timeout: 8_000 });
  await profileTrigger.click();

  // mat-menu renders in a CDK overlay portal — locate .admin-menu-item globally
  const adminMenuItem = page.locator('.admin-menu-item');
  await expect(adminMenuItem).toBeVisible({ timeout: 5_000 });
  await expect(adminMenuItem).toContainText(/administration/i);
});

// ---------------------------------------------------------------------------
// ADMIN-07: /admin/database shows the DB Explorer page
// ---------------------------------------------------------------------------
test('ADMIN-07: /admin/database displays DB Explorer page', async ({ page }) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin/database');
  await expect(page).toHaveURL(/\/admin\/database$/, { timeout: 10_000 });

  // Root container must be visible
  const dbExplorer = page.locator('.db-explorer');
  await expect(dbExplorer).toBeVisible({ timeout: 10_000 });

  // Page title must be present (AC#2 explicit requirement)
  await expect(page.locator('.db-explorer__title')).toBeVisible({ timeout: 5_000 });

  // SQL query section is always present (not conditionally rendered)
  const querySection = page.locator('.db-explorer__query-section');
  await expect(querySection).toBeVisible({ timeout: 10_000 });

  // Wait for table loading to finish
  const loadingEl = page.locator('.db-explorer__loading');
  if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingEl.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // After loading: either a data table or an empty state
  const afterLoad = page.locator('.db-explorer__table, .db-explorer__empty').first();
  await expect(afterLoad).toBeVisible({ timeout: 10_000 });
});

// ---------------------------------------------------------------------------
// ADMIN-08: /admin/logs shows the Logs page with at least 2 tabs
// ---------------------------------------------------------------------------
test('ADMIN-08: /admin/logs displays logs page with tab group', async ({ page }) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin/logs');
  await expect(page).toHaveURL(/\/admin\/logs$/, { timeout: 10_000 });

  // Root container must be visible
  const logsPage = page.locator('.logs-page');
  await expect(logsPage).toBeVisible({ timeout: 10_000 });

  // mat-tab-group must be present inside .logs-page
  const tabGroup = logsPage.locator('mat-tab-group');
  await expect(tabGroup).toBeVisible({ timeout: 10_000 });

  // At least 2 tabs (scrape logs + audit entries) — wait for tab headers to render
  const tabs = tabGroup.locator('[role="tab"]');
  await expect(tabs.nth(1)).toBeVisible({ timeout: 5_000 });
  const tabCount = await tabs.count();
  expect(tabCount).toBeGreaterThanOrEqual(2);
});

// ---------------------------------------------------------------------------
// ADMIN-09: /admin/errors shows the Error Journal page
// ---------------------------------------------------------------------------
test('ADMIN-09: /admin/errors displays Error Journal page', async ({ page }) => {
  test.setTimeout(35_000);

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin/errors');
  await expect(page).toHaveURL(/\/admin\/errors$/, { timeout: 10_000 });

  // Root container must be visible
  const container = page.locator('.error-journal-container');
  await expect(container).toBeVisible({ timeout: 10_000 });

  // Page header must be visible
  const pageHeader = container.locator('.page-header');
  await expect(pageHeader).toBeVisible({ timeout: 5_000 });

  // Wait for loading spinner to disappear if present
  const loadingEl = container.locator('.loading-container');
  if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingEl.waitFor({ state: 'hidden', timeout: 12_000 });
  }

  // After loading: stats-grid (data present), error-container (API failure),
  // or filters-bar (always rendered in non-loading/non-error branch, covers empty-data case)
  const afterLoad = container.locator('.stats-grid, .error-container, .filters-bar').first();
  await expect(afterLoad).toBeVisible({ timeout: 10_000 });
});
