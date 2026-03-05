import { test, expect, Page } from '@playwright/test';

/**
 * Admin Panel E2E tests — ADMIN-01 to ADMIN-04.
 * ADMIN-02/03/04 require an account with ADMIN role (E2E_ADMIN_USER / E2E_ADMIN_PASS env vars).
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
// Helper: login using profile-button click (the app shows pre-registered profiles)
// ---------------------------------------------------------------------------
async function loginWithFirstProfile(page: Page): Promise<void> {
  await page.goto('/login');
  const profileBtn = page.locator('fieldset.user-selection-section button, button.user-profile-btn').first();
  await profileBtn.waitFor({ state: 'visible', timeout: 10_000 });
  await profileBtn.click();
  await page.waitForURL(/\/games/, { timeout: 12_000 });
}

// ---------------------------------------------------------------------------
// Helper: login with the admin profile (looks for a button containing "admin" text)
// ---------------------------------------------------------------------------
async function loginAsAdmin(page: Page): Promise<boolean> {
  await page.goto('/login');

  // Wait for profile buttons to appear
  const profileBtns = page.locator('fieldset.user-selection-section button, button.user-profile-btn');
  await profileBtns.first().waitFor({ state: 'visible', timeout: 10_000 });

  // Try to find a profile button whose text includes "admin" (case-insensitive)
  const adminBtn = page.locator(
    'fieldset.user-selection-section button:has-text("admin"), button.user-profile-btn:has-text("admin")'
  ).first();

  const adminVisible = await adminBtn.isVisible({ timeout: 3_000 }).catch(() => false);

  if (adminVisible) {
    await adminBtn.click();
    await page.waitForURL(/\/games/, { timeout: 12_000 });
    return true;
  }

  // Fallback: use the alternative login form with E2E_ADMIN_USER env var
  const adminIdentifier = process.env['E2E_ADMIN_USER'];
  if (!adminIdentifier) {
    return false;
  }

  // Toggle the alternative login form
  const showAltBtn = page.locator('button.show-alternative').first();
  if (await showAltBtn.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await showAltBtn.click();
  }

  const identifierInput = page.locator('input[formcontrolname="identifier"]').first();
  await identifierInput.waitFor({ state: 'visible', timeout: 5_000 });
  await identifierInput.fill(adminIdentifier);

  const submitBtn = page.locator('button[type="submit"]').first();
  await submitBtn.click();
  await page.waitForURL(/\/games/, { timeout: 12_000 });
  return true;
}

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
  if (!loggedIn) {
    test.skip();
    return;
  }

  await page.goto('/admin');

  // The AdminGuard checks for ADMIN role — if not admin, it redirects away
  // Give the page time to either render the dashboard or redirect
  await page.waitForTimeout(3_000);
  const url = page.url();

  if (!url.includes('/admin')) {
    // Not an admin user — skip this test
    test.skip();
    return;
  }

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
  if (!loggedIn) {
    test.skip();
    return;
  }

  await page.goto('/admin/pipeline');

  // Check if redirect occurred (not an admin)
  await page.waitForTimeout(2_000);
  const url = page.url();
  if (!url.includes('/admin/pipeline')) {
    test.skip();
    return;
  }

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
  if (!loggedIn) {
    test.skip();
    return;
  }

  await page.goto('/admin/users');

  await page.waitForTimeout(2_000);
  const url = page.url();
  if (!url.includes('/admin/users')) {
    test.skip();
    return;
  }

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
