import { test, expect } from '@playwright/test';

import { forceLoginWithProfile } from './helpers/app-helpers';

/**
 * Smoke tests — 8 critical user flows.
 * These tests require the app (port 4200) and backend (port 8080) to be running.
 *
 * Run: npm run test:e2e
 * CI:  BASE_URL=https://staging.example.com npx playwright test
 */

// ---------------------------------------------------------------------------
// SMOKE-01: Login page loads and shows the profile-selection UI
// The app uses a profile-button login (not a traditional email/password form).
// Primary flow: click a .user-profile-btn to login.
// Alternative flow: click .show-alternative then fill formControlName="identifier".
// ---------------------------------------------------------------------------
test('SMOKE-01: login page renders correctly', async ({ page }) => {
  await page.goto('/login');

  // Page title
  await expect(page).toHaveTitle(/Fortnite/i);

  // The user-selection-login container must be present
  await page.waitForSelector('.user-selection-login', { timeout: 10_000 });
  await expect(page.locator('.user-selection-login')).toBeVisible();

  // At least one profile button OR the alternative toggle must be visible
  const profileBtns = page.locator('.user-profile-btn');
  const altToggle = page.locator('button.show-alternative');
  const hasBtns = (await profileBtns.count()) > 0;
  const hasToggle = await altToggle.isVisible().catch(() => false);
  expect(hasBtns || hasToggle).toBe(true);
});

// ---------------------------------------------------------------------------
// SMOKE-02: Alternative identifier form is operational
// The app uses profile-button login as primary. The alternative form accepts
// an identifier and falls back to the first available profile when no match is found.
// ---------------------------------------------------------------------------
test('SMOKE-02: alternative identifier form falls back to a playable session', async ({
  page,
}) => {
  await page.goto('/login');
  await page.waitForSelector('.user-selection-login', { timeout: 10_000 });

  // Open the alternative login form
  const altToggle = page.locator('button.show-alternative');
  await altToggle.click();

  // Fill with an unknown identifier
  const identifierInput = page.locator('input[formControlName="identifier"]');
  await identifierInput.fill('unknown-user-xyz-does-not-exist@example.com');

  const submitBtn = page.locator('.minimal-form button[type="submit"]');
  await submitBtn.click();

  // The current login component falls back to the first available profile.
  await page.waitForURL(/\/games/, { timeout: 15_000 });
  await expect(page).not.toHaveURL(/login/);
});

// ---------------------------------------------------------------------------
// SMOKE-03: Unauthenticated access to /games redirects to /login
// ---------------------------------------------------------------------------
test('SMOKE-03: /games redirects to /login when unauthenticated', async ({
  page,
}) => {
  await page.goto('/games');
  await page.waitForURL('**/login**', { timeout: 5_000 });
  await expect(page).toHaveURL(/login/);
});

// ---------------------------------------------------------------------------
// SMOKE-04: Unauthenticated access to /admin redirects to /login or /games
// ---------------------------------------------------------------------------
test('SMOKE-04: /admin redirects when unauthenticated', async ({ page }) => {
  await page.goto('/admin');
  await page.waitForURL(/login|games/, { timeout: 5_000 });
  const url = page.url();
  expect(url).toMatch(/login|games/);
});

// ---------------------------------------------------------------------------
// SMOKE-05: /catalogue page is reachable after login
// ---------------------------------------------------------------------------
test('SMOKE-05: player catalogue page is reachable after login', async ({
  page,
}) => {
  await forceLoginWithProfile(page, 'thibaut');

  await page.goto('/catalogue');
  await expect(page).not.toHaveURL(/login/, { timeout: 5_000 });
  await expect(page.locator('.catalogue-page')).toBeVisible({
    timeout: 10_000,
  });
});

// ---------------------------------------------------------------------------
// SMOKE-06: Page not found returns a 404-like view (no white screen)
// ---------------------------------------------------------------------------
test('SMOKE-06: unknown route does not show a blank page', async ({ page }) => {
  await page.goto('/this-does-not-exist-xyz');
  const body = page.locator('body');
  const text = await body.textContent();
  // The page should render SOMETHING (not completely blank)
  expect(text?.trim().length).toBeGreaterThan(0);
});

// ---------------------------------------------------------------------------
// SMOKE-07: Backend health endpoint responds 200
// ---------------------------------------------------------------------------
test('SMOKE-07: backend /actuator/health returns 200', async ({ request }) => {
  const backendUrl = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
  const response = await request.get(`${backendUrl}/actuator/health`);
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.status).toBe('UP');
});

// ---------------------------------------------------------------------------
// SMOKE-08: Login flow succeeds via profile button and lands on /games
// The app uses a profile-button login (click your username card).
// E2E_USER / E2E_PASS are kept in env for API-level usage.
// The profile button at index 0 corresponds to the first seeded user.
// ---------------------------------------------------------------------------
test('SMOKE-08: valid login navigates to /games', async ({ page }) => {
  await page.goto('/login');

  // Wait for the user-selection panel to load
  await page.waitForSelector('.user-selection-login', { timeout: 10_000 });

  // Click the first available profile button (seeded user: thibaut / admin)
  const profileBtns = page.locator('.user-profile-btn');
  const count = await profileBtns.count();

  if (count > 0) {
    await profileBtns.first().click();
  } else {
    // Fallback: use the alternative form with the first seeded email
    const testUser = process.env['E2E_USER'] ?? 'thibaut@fortnite-pronos.com';
    const altToggle = page.locator('button.show-alternative');
    await altToggle.click();
    const identifierInput = page.locator('input[formControlName="identifier"]');
    await identifierInput.fill(testUser);
    await page.locator('.minimal-form button[type="submit"]').click();
  }

  // After login, expect redirect to /games
  await page.waitForURL(/\/games/, { timeout: 15_000 });
  await expect(page).not.toHaveURL(/login/);
});
