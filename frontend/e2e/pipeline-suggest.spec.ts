import { test, expect } from '@playwright/test';

import { loginAsAdmin } from './helpers/app-helpers';

/**
 * Pipeline suggest-epic-id E2E tests — PIPELINE-SUGGEST-01/02.
 *
 * Tests the auto-suggest flow: when the admin opens the UNRESOLVED tab,
 * each entry's Epic ID field is pre-filled automatically via the
 * suggest-epic-id API (works for both stub and fortnite-api adapter modes).
 *
 * Test strategy:
 *   1. Pre-call the suggest API for each UNRESOLVED player to know which ones
 *      will receive a suggestion (found=true) and which won't (found=false).
 *   2. Assert on a player that IS expected to receive a suggestion.
 *   3. If no player receives a suggestion, skip gracefully.
 *
 * These tests require:
 *   - Docker stack running on BASE_URL (default http://localhost:8080)
 *   - At least one UNRESOLVED player in player_identity_pipeline table
 *     for which the active ResolutionAdapter returns found=true
 *
 * Run: npm run test:e2e
 */

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';

interface SuggestionResponse {
  found: boolean;
  suggestedEpicId: string | null;
  displayName: string | null;
  confidenceScore: number;
}

interface PipelineEntry {
  id: string;
  playerId: string;
  playerUsername: string;
  playerRegion: string;
  status: string;
}

interface EntryWithSuggestion {
  entry: PipelineEntry;
  suggestion: SuggestionResponse;
}

interface SuggestionPreflightResult {
  target?: EntryWithSuggestion;
  skipReason?: string;
}

/** Pre-calls suggest-epic-id until the first suggestable player or a temporary API incident. */
async function findFirstSuggestableEntry(
  request: import('@playwright/test').APIRequestContext,
  entries: PipelineEntry[]
): Promise<SuggestionPreflightResult> {
  for (const entry of entries) {
    const resp = await request.get(
      `${BACKEND_URL}/api/admin/players/${entry.playerId}/suggest-epic-id`,
      { headers: { 'X-Test-User': 'admin' } }
    );
    if (resp.status() === 429 || resp.status() === 503) {
      return {
        skipReason: `suggest-epic-id preflight returned HTTP ${resp.status()} for ${entry.playerUsername}`
      };
    }
    if (resp.ok()) {
      const suggestion = (await resp.json()) as SuggestionResponse;
      if (suggestion.found && suggestion.suggestedEpicId) {
        return { target: { entry, suggestion } };
      }
    }
  }
  return {};
}

function playerRow(
  page: import('@playwright/test').Page,
  entry: PipelineEntry,
  allEntries: PipelineEntry[]
): Promise<import('@playwright/test').Locator> {
  return (async () => {
    const byPlayerId = page.locator(`.pipeline-row[data-player-id="${entry.playerId}"]`);
    if ((await byPlayerId.count()) > 0) {
      return byPlayerId.first();
    }

    const rowIndex = allEntries.findIndex(candidate => candidate.playerId === entry.playerId);
    if (rowIndex < 0) {
      throw new Error(`Cannot locate pipeline row for playerId=${entry.playerId}`);
    }

    const fallbackRow = page.locator('.pipeline-row').nth(rowIndex);
    await expect(fallbackRow).toContainText(entry.playerUsername, { timeout: 10_000 });
    await expect(fallbackRow).toContainText(entry.playerRegion, { timeout: 10_000 });
    return fallbackRow;
  })();
}

// ---------------------------------------------------------------------------
// PIPELINE-SUGGEST-01: Auto-suggest fills the Epic ID field on page load
// ---------------------------------------------------------------------------
test('PIPELINE-SUGGEST-01: auto-suggest pre-fills Epic ID for a known UNRESOLVED player', async ({
  page,
  request,
}) => {
  test.setTimeout(60_000);

  // Preflight 1: check that UNRESOLVED entries exist
  const unresolvedResp = await request.get(`${BACKEND_URL}/api/admin/players/unresolved`, {
    headers: { 'X-Test-User': 'admin' },
  });
  if (!unresolvedResp.ok()) {
    test.skip(true, 'Cannot reach pipeline API — Docker not running');
    return;
  }
  const entries = (await unresolvedResp.json()) as PipelineEntry[];
  if (!Array.isArray(entries) || entries.length === 0) {
    test.skip(true, 'No UNRESOLVED players in DB — seed entries to run this test');
    return;
  }

  // Preflight 2: find at least one player for which the adapter returns a suggestion
  const preflight = await findFirstSuggestableEntry(request, entries);
  if (preflight.skipReason) {
    test.skip(true, preflight.skipReason);
    return;
  }
  if (!preflight.target) {
    test.skip(
      true,
      `The active ResolutionAdapter returned found=false for all ${entries.length} UNRESOLVED ` +
        'players. Use RESOLUTION_ADAPTER=stub or seed a player with a resolvable username.'
    );
    return;
  }

  const { entry: target, suggestion: expected } = preflight.target;

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin/pipeline');
  await expect(page).toHaveURL(/\/admin\/pipeline$/, { timeout: 10_000 });

  // Wait for initial loading spinner to disappear
  const loadingSpinner = page.locator('.pipeline-loading mat-spinner');
  if (await loadingSpinner.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingSpinner.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  // Pipeline tabs must be visible
  const tabGroup = page.locator('mat-tab-group.pipeline-tabs');
  await expect(tabGroup).toBeVisible({ timeout: 10_000 });

  // Find the specific row for the target player (whose API returns found=true)
  const targetRow = await playerRow(page, target, entries);
  await expect(targetRow).toBeVisible({ timeout: 10_000 });

  // The epic-id-input within this row should be pre-filled by auto-suggest
  const targetInput = targetRow.locator('.epic-id-input').first();
  await expect(targetInput).toBeVisible({ timeout: 10_000 });

  await expect
    .poll(
      async () => {
        const value = await targetInput.inputValue();
        return value.trim().length > 0;
      },
      { timeout: 20_000, intervals: [500] }
    )
    .toBe(true);

  // Value must match the API-returned suggestion
  const actualValue = await targetInput.inputValue();
  expect(actualValue.trim()).toBe(expected.suggestedEpicId);

  // Confidence badge must appear
  const confidenceBadge = targetRow.locator('.confidence-badge').first();
  await expect(confidenceBadge).toBeVisible({ timeout: 10_000 });
  await expect(confidenceBadge).toContainText(`${expected.confidenceScore}%`);
});

// ---------------------------------------------------------------------------
// PIPELINE-SUGGEST-02: Manual suggest button re-triggers the suggestion
// Strategy: targeted preflight for a single suggestable row, then assert the
//           post-click network request and its response explicitly.
// ---------------------------------------------------------------------------
test('PIPELINE-SUGGEST-02: manual suggest button fills Epic ID field on demand', async ({
  page,
  request,
}) => {
  test.setTimeout(60_000);

  // Preflight 1: check UNRESOLVED entries exist
  const unresolvedResp = await request.get(`${BACKEND_URL}/api/admin/players/unresolved`, {
    headers: { 'X-Test-User': 'admin' },
  });
  if (!unresolvedResp.ok()) {
    test.skip(true, 'Cannot reach pipeline API — Docker not running');
    return;
  }
  const entries = (await unresolvedResp.json()) as PipelineEntry[];
  if (!Array.isArray(entries) || entries.length === 0) {
    test.skip(true, 'No UNRESOLVED players in DB — seed entries to run this test');
    return;
  }

  // Preflight 2: identify one row that should resolve, or skip explicitly on 429/503
  const preflight = await findFirstSuggestableEntry(request, entries);
  if (preflight.skipReason) {
    test.skip(true, preflight.skipReason);
    return;
  }
  if (!preflight.target) {
    test.skip(
      true,
      `The active ResolutionAdapter returned found=false for all ${entries.length} UNRESOLVED ` +
        'players. Use RESOLUTION_ADAPTER=stub or seed a player with a resolvable username.'
    );
    return;
  }

  const { entry: target, suggestion: expected } = preflight.target;

  const loggedIn = await loginAsAdmin(page);
  expect(loggedIn).toBe(true);

  await page.goto('/admin/pipeline');
  await expect(page).toHaveURL(/\/admin\/pipeline$/, { timeout: 10_000 });

  // Wait for initial loading spinner to disappear
  const loadingSpinner = page.locator('.pipeline-loading mat-spinner');
  if (await loadingSpinner.isVisible({ timeout: 3_000 }).catch(() => false)) {
    await loadingSpinner.waitFor({ state: 'hidden', timeout: 15_000 });
  }

  const targetRow = await playerRow(page, target, entries);
  await expect(targetRow).toBeVisible({ timeout: 10_000 });

  // Scope all locators to the specific row that received a suggestion
  const targetInput = targetRow.locator('.epic-id-input').first();
  const suggestButton = targetRow.locator('.btn-suggest').first();
  const confidenceBadge = targetRow.locator('.confidence-badge').first();

  // Wait for auto-suggest to fill the exact player selected by preflight
  await expect(targetInput).toHaveValue(expected.suggestedEpicId ?? '', { timeout: 20_000 });
  await expect(suggestButton).toBeEnabled({ timeout: 5_000 });

  // Badge must be visible (auto-suggest completed)
  await expect(confidenceBadge).toBeVisible({ timeout: 5_000 });
  await expect(confidenceBadge).toContainText(`${expected.confidenceScore}%`);

  // Click suggest button manually and prove a new request was emitted for this row
  const manualSuggestResponsePromise = page.waitForResponse(response => {
    return (
      response.request().method() === 'GET' &&
      response.url().includes(`/api/admin/players/${target.playerId}/suggest-epic-id`)
    );
  });
  await suggestButton.click();
  const manualSuggestResponse = await manualSuggestResponsePromise;

  if (manualSuggestResponse.status() === 429 || manualSuggestResponse.status() === 503) {
    test.skip(
      true,
      `manual suggest returned HTTP ${manualSuggestResponse.status()} for ${target.playerUsername}`
    );
    return;
  }

  expect(manualSuggestResponse.ok()).toBe(true);
  const manualSuggestion = (await manualSuggestResponse.json()) as SuggestionResponse;
  expect(manualSuggestion.found).toBe(true);
  expect(manualSuggestion.suggestedEpicId).toBe(expected.suggestedEpicId);

  // Input and badge stay aligned with the manual response after re-suggest
  await expect(targetInput).toHaveValue(expected.suggestedEpicId ?? '');
  await expect(confidenceBadge).toBeVisible({ timeout: 10_000 });
});
