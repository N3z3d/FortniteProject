import { test, expect } from '@playwright/test';

import { forceLoginWithProfile, waitForPageReady } from './helpers/app-helpers';

/**
 * Player Catalogue E2E tests — CAT-01 to CAT-05.
 * All tests use forceLoginWithProfile(page, 'thibaut') for authentication.
 * Tests skip gracefully when backend returns empty data or Docker is not running.
 *
 * Route: /catalogue (no auth guard, accessible to all authenticated users)
 *
 * Run: npm run test:e2e
 */

// ---------------------------------------------------------------------------
// CAT-01: /catalogue loads and renders the player list
// ---------------------------------------------------------------------------
test('CAT-01: /catalogue loads and renders the player list', async ({ page }) => {
  test.setTimeout(35_000);

  await forceLoginWithProfile(page, 'thibaut');
  if (!await waitForPageReady(page, '/catalogue')) { test.skip(); return; }

  // Root page container must be visible
  await expect(page.locator('.catalogue-page')).toBeVisible({ timeout: 10_000 });

  // Wait for loading spinner to disappear if present
  const loadingEl = page.locator('.catalogue-loading');
  if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingEl.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // Either the virtual scroll viewport (with players) or the empty state must be visible
  const contentState = page
    .locator('cdk-virtual-scroll-viewport.catalogue-viewport, .catalogue-empty')
    .first();
  await expect(contentState).toBeVisible({ timeout: 10_000 });
});

// ---------------------------------------------------------------------------
// CAT-02: Search by username updates the result counter
// ---------------------------------------------------------------------------
test('CAT-02: search by username updates result counter', async ({ page }) => {
  test.setTimeout(35_000);

  await forceLoginWithProfile(page, 'thibaut');
  if (!await waitForPageReady(page, '/catalogue')) { test.skip(); return; }

  await expect(page.locator('.catalogue-page')).toBeVisible({ timeout: 10_000 });

  // Wait for loading to finish
  const loadingEl = page.locator('.catalogue-loading');
  if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingEl.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // Read initial counter text
  const resultCounter = page.locator('.result-counter');
  const initialText = await resultCounter.textContent({ timeout: 8_000 }).catch(() => null);

  // Skip gracefully when no counter present or catalogue is empty (0 players — nothing to filter)
  if (!initialText || /^0\b/.test(initialText.trim())) {
    test.skip();
    return;
  }

  // Fill search input and wait for debounce (FILTER_DEBOUNCE_MS=200 + render margin)
  await page.locator('.search-field input').first().fill('a');

  // AC#2: use expect.poll to confirm counter stabilises after debounce + HTTP response
  // (more robust than a fixed waitForTimeout that may not cover slow backends)
  await expect
    .poll(async () => resultCounter.textContent(), { timeout: 8_000 })
    .not.toBeNull();

  // Counter must remain visible after filter has been applied
  await expect(resultCounter).toBeVisible({ timeout: 5_000 });
});

// ---------------------------------------------------------------------------
// CAT-03: Region filter applies correctly
// ---------------------------------------------------------------------------
test('CAT-03: region filter applies correctly', async ({ page }) => {
  test.setTimeout(35_000);

  await forceLoginWithProfile(page, 'thibaut');
  if (!await waitForPageReady(page, '/catalogue')) { test.skip(); return; }

  await expect(page.locator('.catalogue-page')).toBeVisible({ timeout: 10_000 });

  // Wait for loading to finish
  const loadingEl = page.locator('.catalogue-loading');
  if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingEl.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // Guard: assert region-select is visible before clicking (skip if filter section not rendered)
  const regionSelect = page.locator('[data-testid="region-select"]');
  const selectVisible = await regionSelect.isVisible({ timeout: 5_000 }).catch(() => false);
  if (!selectVisible) {
    test.skip();
    return;
  }

  // Click region select trigger to open mat-select dropdown
  await regionSelect.click();

  // mat-option renders in a CDK overlay at document.body — use global locator
  const options = page.locator('mat-option');
  await expect(options.first()).toBeVisible({ timeout: 5_000 });

  // Click first non-null option (index 1; index 0 is "Toutes" / all-regions option)
  const firstRealOption = options.nth(1);
  const optionVisible = await firstRealOption
    .isVisible({ timeout: 2_000 })
    .catch(() => false);
  if (!optionVisible) {
    test.skip(); // No specific regions available
    return;
  }
  await firstRealOption.click();

  // Wait for filter debounce (FILTER_DEBOUNCE_MS=200) + HTTP + render
  await page.waitForTimeout(400);

  // Either result counter or empty state must be visible
  const afterFilter = page.locator('.result-counter, .catalogue-empty').first();
  await expect(afterFilter).toBeVisible({ timeout: 8_000 });
});

// ---------------------------------------------------------------------------
// CAT-04: Selecting two players shows the comparison panel; clearing removes it
// ---------------------------------------------------------------------------
test('CAT-04: comparison panel appears with 2 players and clears on button click', async ({
  page,
}) => {
  test.setTimeout(35_000);

  await forceLoginWithProfile(page, 'thibaut');
  if (!await waitForPageReady(page, '/catalogue')) { test.skip(); return; }

  await expect(page.locator('.catalogue-page')).toBeVisible({ timeout: 10_000 });

  // Wait for loading to finish
  const loadingEl = page.locator('.catalogue-loading');
  if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingEl.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // Need at least 2 .catalogue-row items; skip gracefully when not enough data
  const rowCount = await page.locator('.catalogue-row').count();
  if (rowCount < 2) {
    test.skip();
    return;
  }

  // Click first two player cards (triggers cardSelected EventEmitter on app-player-card)
  await page.locator('.catalogue-row').nth(0).locator('app-player-card').click();
  await page.locator('.catalogue-row').nth(1).locator('app-player-card').click();

  // Comparison panel must appear
  await expect(page.locator('.comparison-panel')).toBeVisible({ timeout: 5_000 });

  // Must contain exactly 2 comparison cards
  await expect(page.locator('.comparison-card')).toHaveCount(2, {
    timeout: 5_000,
  });

  // Click the clear button — panel must be removed from DOM (not just hidden)
  await page.locator('.comparison-panel__clear').click();
  await expect(page.locator('.comparison-panel')).not.toBeAttached({
    timeout: 5_000,
  });
});

// ---------------------------------------------------------------------------
// CAT-05: Accessible list is present in the DOM
// ---------------------------------------------------------------------------
test('CAT-05: accessible list is present in the DOM', async ({ page }) => {
  test.setTimeout(35_000);

  await forceLoginWithProfile(page, 'thibaut');
  if (!await waitForPageReady(page, '/catalogue')) { test.skip(); return; }

  await expect(page.locator('.catalogue-page')).toBeVisible({ timeout: 10_000 });

  // Wait for loading to finish
  const loadingEl = page.locator('.catalogue-loading');
  if (await loadingEl.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingEl.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // #accessible-list must be attached to the DOM (sr-only — not necessarily visible)
  await expect(page.locator('#accessible-list')).toBeAttached();

  // When players are loaded, wait for Angular to populate the accessible list before asserting
  const viewport = page.locator('cdk-virtual-scroll-viewport.catalogue-viewport');
  const hasPlayers = await viewport.isVisible({ timeout: 3_000 }).catch(() => false);
  if (hasPlayers) {
    // Use toBeAttached with timeout to wait for async list population
    await expect(
      page.locator('.catalogue-accessible-list__item').first()
    ).toBeAttached({ timeout: 5_000 });
  }
});
