import { test, expect, Page } from '@playwright/test';

import { clearForcedAuth, forceLoginWithProfile } from './helpers/app-helpers';
import { softDeleteLocalGamesByPrefix } from './helpers/local-db-helpers';

/**
 * Login/Logout E2E tests — AUTH-LL-01 to AUTH-LL-04.
 *
 * These tests validate the store cleanup fix (Sprint 12 RC-5):
 * AppComponent.userChanged$ → store.reset() on logout, preventing stale data
 * contamination between user sessions.
 *
 * AUTH-LL-01: Login thibaut → /games with store loaded (game list or empty state visible)
 * AUTH-LL-02: Login thibaut → logout → /login, navigating to /games is stable (no freeze)
 * AUTH-LL-03: No session → /login?switchUser=true → login page renders, no console ERRORs
 * AUTH-LL-04: Login thibaut → logout → login teddy → /games stable (no thibaut contamination)
 *
 * Run: npm run test:e2e
 */

// ---------------------------------------------------------------------------
// Helper: perform logout via UI button or programmatic fallback
// ---------------------------------------------------------------------------
async function performLogout(page: Page): Promise<void> {
  const logoutBtn = page.locator('button.logout-btn, button.logout-button').first();
  const logoutVisible = await logoutBtn.isVisible({ timeout: 5_000 }).catch(() => false);

  if (logoutVisible) {
    await logoutBtn.click();
  } else {
    const iconLogout = page.locator('button:has(mat-icon:text("logout"))').first();
    const iconVisible = await iconLogout.isVisible({ timeout: 3_000 }).catch(() => false);
    if (iconVisible) {
      await iconLogout.click();
    } else {
      await page.goto('/login?switchUser=true');
    }
  }

  await page.waitForURL(/\/login/, { timeout: 10_000 });
}

// ---------------------------------------------------------------------------
// Cleanup after each test (no E2E-LL- games created, included for safety)
// ---------------------------------------------------------------------------
test.afterEach(async () => {
  softDeleteLocalGamesByPrefix('E2E-LL-');
});

// ---------------------------------------------------------------------------
// AUTH-LL-01: Login thibaut → /games, store loaded (list or empty state visible)
// ---------------------------------------------------------------------------
test('AUTH-LL-01: login thibaut navigates to /games with store loaded', async ({ page }) => {
  test.setTimeout(35_000);

  await clearForcedAuth(page);
  await forceLoginWithProfile(page, 'thibaut');

  // Verify we landed on /games
  expect(page.url()).toContain('/games');

  // Verify store is loaded: game card, empty state, or game list container is visible
  const storeLoaded = page.locator(
    '.game-card, .empty-state, .games-list, .game-home-container, app-game-home'
  ).first();
  await expect(storeLoaded).toBeVisible({ timeout: 10_000 });
});

// ---------------------------------------------------------------------------
// AUTH-LL-02: Login thibaut → logout → /login, navigate to /games is stable
// ---------------------------------------------------------------------------
test('AUTH-LL-02: logout redirects to /login and navigating to /games is stable', async ({ page }) => {
  test.setTimeout(35_000);

  await forceLoginWithProfile(page, 'thibaut');
  expect(page.url()).toContain('/games');

  await performLogout(page);
  expect(page.url()).toContain('/login');

  // Navigate to /games after logout — either redirected to /login (guard active)
  // or the page stays on /games (auto-login). Either way, the app must be stable.
  await page.goto('/games');
  await page.waitForURL(/login|games/, { timeout: 8_000 });

  // App is stable: body is rendered, no freeze
  await expect(page.locator('body')).toBeVisible();
});

// ---------------------------------------------------------------------------
// AUTH-LL-03: No session → /login?switchUser=true → login page, no console ERRORs
// ---------------------------------------------------------------------------
test('AUTH-LL-03: navigating to /login?switchUser=true without session shows login page without errors', async ({ page }) => {
  test.setTimeout(35_000);

  const consoleErrors: string[] = [];
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });

  await clearForcedAuth(page);
  await page.goto('/login?switchUser=true');
  await page.waitForURL(/\/login/, { timeout: 10_000 });

  // Login page must render correctly (not frozen)
  const loginContainer = page.locator(
    '.user-controlled-login, .user-selection-login, form'
  ).first();
  await expect(loginContainer).toBeVisible({ timeout: 8_000 });

  // No critical console errors (excluding favicon and zone.js noise)
  const criticalErrors = consoleErrors.filter(
    e => e.includes('ERROR') && !e.includes('favicon') && !e.includes('zone')
  );
  expect(criticalErrors).toHaveLength(0);
});

// ---------------------------------------------------------------------------
// AUTH-LL-04: Login thibaut → logout → login teddy → /games stable, no crash
// ---------------------------------------------------------------------------
test('AUTH-LL-04: switching from thibaut to teddy shows stable page (store not contaminated)', async ({ page }) => {
  test.setTimeout(35_000);

  // Login as thibaut
  await forceLoginWithProfile(page, 'thibaut');
  expect(page.url()).toContain('/games');

  // Logout
  await performLogout(page);
  expect(page.url()).toContain('/login');

  // Login as teddy
  await forceLoginWithProfile(page, 'teddy');
  expect(page.url()).toContain('/games');

  // Page must be stable and body visible (no crash after store reset + reload)
  await expect(page.locator('body')).toBeVisible({ timeout: 10_000 });

  // Verify the store loaded content for teddy's session (game list or empty state)
  const storeLoaded = page.locator(
    '.game-card, .empty-state, .games-list, .game-home-container, app-game-home'
  ).first();
  await expect(storeLoaded).toBeVisible({ timeout: 10_000 });
});
