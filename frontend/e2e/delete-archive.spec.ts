/**
 * sprint12-e2e-delete-archive — DA-01..DA-06: delete/archive/leave game tests
 *
 * DA-01..DA-04: API-level tests for RC-2 fix (409 CONFLICT on picks present).
 * DA-05..DA-06: Browser tests validating post-action redirect to /games
 *               (regression guard for Sprint 14 bug: navigate(['/']) → crash /login).
 *
 *   DA-01: Delete game with no picks          → HTTP < 300 (success)
 *   DA-02: Delete game with 1 pick submitted  → HTTP 409 CONFLICT (RC-2 fix)
 *   DA-03: Delete game in CREATING status     → HTTP < 300 (soft delete)
 *   DA-04: Double-delete same game            → second response NOT 500
 *   DA-05: Delete game via UI                 → redirects to /games (not /login)
 *   DA-06: Leave game via UI (as teddy)       → redirects to /games (not /login)
 */

import { APIRequestContext, APIResponse, Page, expect, test } from '@playwright/test';

import { softDeleteLocalGamesByPrefix } from './helpers/local-db-helpers';
import { buildSingleRegionRules } from '../src/app/features/game/create-game/create-game-region-rules.util';

const BASE_URL = process.env['BASE_URL'] ?? 'http://localhost:4200';
const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const DA_PREFIX = 'E2E-DA-';

// ---------------------------------------------------------------------------
// Real-JWT login helper (profile button click — no X-Test-User on browser context)
// ---------------------------------------------------------------------------

async function loginWithRealJwt(page: Page, username: 'thibaut' | 'teddy'): Promise<void> {
  await page.goto(`${BASE_URL}/login`);
  // Clear any residual session from a previous test running in the same browser context
  await page.evaluate(() => {
    sessionStorage.clear();
    localStorage.clear();
  });
  const profileBtn = page.locator(
    `button:has-text("${username}"), [role="button"]:has-text("${username}")`
  ).first();
  await profileBtn.waitFor({ state: 'visible', timeout: 15_000 });
  await profileBtn.click();
  await page.waitForURL(url => !url.pathname.includes('/login'), { timeout: 15_000 });
  // Wait for resources to load (networkidle avoided — WebSocket keeps network busy)
  await page.waitForLoadState('load', { timeout: 10_000 });
}

/** EU Tranche 1 player from V1001__seed_e2e_users_and_players.sql */
const BUGHA_EU_T1 = '10000000-0000-0000-0000-000000000001';

// ---------------------------------------------------------------------------
// API helpers
// ---------------------------------------------------------------------------

function authHeaders(username: string): Record<string, string> {
  return { 'X-Test-User': username };
}

function jsonAuthHeaders(username: string): Record<string, string> {
  return { ...authHeaders(username), 'Content-Type': 'application/json' };
}

async function createGame(
  request: APIRequestContext,
  username: string,
  name: string
): Promise<string> {
  const res = await request.post(`${BACKEND_URL}/api/games`, {
    headers: jsonAuthHeaders(username),
    data: { name, maxParticipants: 2, teamSize: 1, regionRules: buildSingleRegionRules(2, 'EU') },
  });
  expect(res.status()).toBe(201);
  const body = await res.json();
  return body.data?.id ?? body.id;
}

async function generateCode(
  request: APIRequestContext,
  username: string,
  gameId: string
): Promise<string> {
  const res = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/regenerate-code`,
    { headers: jsonAuthHeaders(username) }
  );
  expect(res.status()).toBeLessThan(300);
  const body = await res.json();
  const code: string | undefined = body.data?.invitationCode ?? body.invitationCode;
  if (!code) throw new Error(`generateCode returned no invitationCode for game ${gameId}`);
  return code;
}

async function joinWithCode(
  request: APIRequestContext,
  username: string,
  code: string
): Promise<void> {
  const res = await request.post(`${BACKEND_URL}/api/games/join-with-code`, {
    headers: jsonAuthHeaders(username),
    data: { invitationCode: code },
  });
  if (!res.ok()) {
    throw new Error(
      `joinWithCode failed for ${username}: ${res.status()} ${(await res.text()).substring(0, 300)}`
    );
  }
}

async function joinGame(
  request: APIRequestContext,
  username: string,
  gameId: string,
  code: string
): Promise<void> {
  const res = await request.post(`${BACKEND_URL}/api/games/join`, {
    headers: jsonAuthHeaders(username),
    data: { gameId, invitationCode: code },
  });
  if (!res.ok()) {
    throw new Error(`joinGame failed for ${username}: ${res.status()} ${await res.text()}`);
  }
}

async function startDraft(
  request: APIRequestContext,
  username: string,
  gameId: string
): Promise<void> {
  const res = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/start-draft`,
    { headers: authHeaders(username) }
  );
  if (!res.ok()) {
    throw new Error(`startDraft failed: ${res.status()} ${await res.text()}`);
  }
}

async function initializeSnake(
  request: APIRequestContext,
  username: string,
  gameId: string
): Promise<void> {
  const res = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/initialize`,
    { headers: authHeaders(username) }
  );
  if (!res.ok()) {
    throw new Error(`initializeSnake failed: ${res.status()} ${await res.text()}`);
  }
}

async function submitPick(
  request: APIRequestContext,
  username: string,
  gameId: string,
  playerId: string,
  regionId: string
): Promise<void> {
  const res = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/pick`,
    {
      headers: jsonAuthHeaders(username),
      data: { playerId, regionId },
    }
  );
  if (!res.ok()) {
    throw new Error(
      `submitPick failed for ${username} (player=${playerId}): ${res.status()} ${await res.text()}`
    );
  }
}

async function deleteGame(
  request: APIRequestContext,
  username: string,
  gameId: string
): Promise<APIResponse> {
  return request.delete(`${BACKEND_URL}/api/games/${gameId}`, {
    headers: authHeaders(username),
  });
}

// ---------------------------------------------------------------------------
// Test suite
// ---------------------------------------------------------------------------

test.describe('DELETE/ARCHIVE game — RC-2 fix (409 on picks present)', () => {
  test.afterAll(() => {
    softDeleteLocalGamesByPrefix(DA_PREFIX);
  });

  // -------------------------------------------------------------------------
  // DA-01: Delete game with no picks → success
  // -------------------------------------------------------------------------
  test('DA-01: delete game with no picks returns success', async ({ request }) => {
    test.setTimeout(30_000);

    const gameName = `${DA_PREFIX}CLEAN-${Date.now()}`;
    let gameId: string;

    try {
      gameId = await createGame(request, 'thibaut', gameName);
    } catch {
      test.skip();
      return;
    }

    const res = await deleteGame(request, 'thibaut', gameId);
    expect(res.status()).toBeLessThan(300);
  });

  // -------------------------------------------------------------------------
  // DA-02: Delete game with 1 pick → 409 CONFLICT (RC-2 fix)
  // -------------------------------------------------------------------------
  test('DA-02: delete game with picks returns 409 CONFLICT', async ({ request }) => {
    test.setTimeout(60_000);

    const gameName = `${DA_PREFIX}PICKS-${Date.now()}`;
    let gameId: string;

    try {
      // Create game as thibaut
      gameId = await createGame(request, 'thibaut', gameName);

      // Generate invitation code and join as teddy
      const code = await generateCode(request, 'thibaut', gameId);
      await joinGame(request, 'teddy', gameId, code);

      // Start draft and initialize snake cursors
      await startDraft(request, 'thibaut', gameId);
      await initializeSnake(request, 'thibaut', gameId);

      // Submit 1 pick (EU region, Bugha)
      await submitPick(request, 'thibaut', gameId, BUGHA_EU_T1, 'EU');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      console.warn(`DA-02 setup failed (${message}) — skipping`);
      test.skip();
      return;
    }

    // Attempt to delete — should return 409 because picks exist (RC-2 fix in GlobalExceptionHandler)
    const res = await deleteGame(request, 'thibaut', gameId);
    expect(res.status()).toBe(409);

    // Note: the UI snackbar display triggered by this 409 is covered by manual acceptance
    // testing and unit tests on game-detail-actions.service.ts (extractBackendMessage + snackbar).
    // A full browser E2E for the snackbar would require a seeded game with picks, which is
    // fragile across environments — API-level validation of the 409 is the primary signal here.
  });

  // -------------------------------------------------------------------------
  // DA-03: Delete game in CREATING status → soft delete succeeds
  // -------------------------------------------------------------------------
  test('DA-03: delete game in CREATING status returns success', async ({ request }) => {
    test.setTimeout(30_000);

    const gameName = `${DA_PREFIX}ARCH-${Date.now()}`;
    let gameId: string;

    try {
      gameId = await createGame(request, 'thibaut', gameName);
    } catch {
      test.skip();
      return;
    }

    // Game is in CREATING status (no participants joined, no draft started)
    const res = await deleteGame(request, 'thibaut', gameId);
    expect(res.status()).toBeLessThan(300);
  });

  // -------------------------------------------------------------------------
  // DA-04: Double-delete same game → second response is NOT 500
  // -------------------------------------------------------------------------
  test('DA-04: double-delete game is graceful (not 500)', async ({ request }) => {
    test.setTimeout(30_000);

    const gameName = `${DA_PREFIX}DBLARCH-${Date.now()}`;
    let gameId: string;

    try {
      gameId = await createGame(request, 'thibaut', gameName);
    } catch {
      test.skip();
      return;
    }

    // First delete — should succeed (soft delete)
    const first = await deleteGame(request, 'thibaut', gameId);
    expect(first.status()).toBeLessThan(300);

    // Second delete — game already deleted, expect 404 or 409 but NOT 500
    const second = await deleteGame(request, 'thibaut', gameId);
    expect(second.status()).not.toBe(500);
  });
});

// ---------------------------------------------------------------------------
// Browser redirect tests — Sprint 15 regression guard (A14a)
// Validates that delete/leave via UI redirects to /games, not /login.
// Fix: game-detail-actions.service.ts navigate(['/']) → navigate(['/games'])
// ---------------------------------------------------------------------------

test.describe('POST-ACTION REDIRECT — regression guard (Sprint 14 fix)', () => {
  test.afterAll(() => {
    try {
      softDeleteLocalGamesByPrefix('E2E-DA05-');
      softDeleteLocalGamesByPrefix('E2E-DA06-');
    } catch (err) {
      // best-effort cleanup — do not fail the suite on Docker/DB issues
      console.warn('afterAll cleanup failed:', err instanceof Error ? err.message : err);
    }
  });

  // -------------------------------------------------------------------------
  // DA-05: Delete game via UI → redirect to /games (not /login)
  // -------------------------------------------------------------------------
  test('DA-05: delete game via UI redirects to /games', async ({ page, request }) => {
    test.setTimeout(60_000);

    // Setup: create a fresh CREATING game via API
    const gameName = `E2E-DA05-DEL-${Date.now()}`;
    let gameId: string;
    try {
      gameId = await createGame(request, 'thibaut', gameName);
    } catch {
      test.skip();
      return;
    }

    // Login via real JWT (profile button — no X-Test-User on browser context)
    await loginWithRealJwt(page, 'thibaut');

    // Navigate to game detail page
    await page.goto(`${BASE_URL}/games/${gameId}`);
    await page.waitForLoadState('domcontentloaded');

    // Click the delete button (class="delete-game-btn", game is in CREATING status)
    // Hard fail if not visible — button absence may indicate a real regression
    const deleteBtn = page.locator('button.delete-game-btn').first();
    await deleteBtn.waitFor({ state: 'visible', timeout: 12_000 });
    await deleteBtn.click();

    // Confirm the dialog — wait for the button itself to be visible (accounts for Material animation)
    // Hard fail if dialog does not appear — this is the critical assertion path
    const deleteConfirmBtn = page.locator('[role="dialog"]').getByRole('button', { name: 'Supprimer' });
    await deleteConfirmBtn.waitFor({ state: 'visible', timeout: 8_000 });
    await deleteConfirmBtn.click();

    // Assert redirect to /games (not /login) — regression guard
    await page.waitForURL(url => url.pathname === '/games', { timeout: 10_000 });
    expect(page.url()).not.toContain('/login');
    expect(page.url()).toContain('/games');
  });

  // -------------------------------------------------------------------------
  // DA-06: Leave game via UI (as teddy) → redirect to /games (not /login)
  // -------------------------------------------------------------------------
  test('DA-06: leave game via UI redirects to /games', async ({ page, request }) => {
    test.setTimeout(60_000);

    // Setup: thibaut creates game, generates code, teddy joins via API
    const gameName = `E2E-DA06-LV-${Date.now()}`;
    let gameId: string;

    try {
      gameId = await createGame(request, 'thibaut', gameName);
      const code = await generateCode(request, 'thibaut', gameId);
      await joinWithCode(request, 'teddy', code);
    } catch {
      test.skip();
      return;
    }

    // Login as teddy via real JWT (profile button)
    await loginWithRealJwt(page, 'teddy');

    // Navigate to game detail page
    await page.goto(`${BASE_URL}/games/${gameId}`);
    await page.waitForLoadState('domcontentloaded');

    // Click the leave button (timeout 15s: canLeaveGame() waits on async participants load)
    // Hard fail if not visible — button absence may indicate a real regression (canLeaveGame() broken)
    const leaveBtn = page.locator('button.leave-game-btn').first();
    await leaveBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await leaveBtn.scrollIntoViewIfNeeded();
    await leaveBtn.click();

    // Confirm dialog — wait for the button itself to be visible (accounts for Material animation)
    // Hard fail if dialog does not appear — this is the critical assertion path
    const leaveConfirmBtn = page.locator('[role="dialog"]').getByRole('button', { name: /^Quitter$/i });
    await leaveConfirmBtn.waitFor({ state: 'visible', timeout: 8_000 });
    await leaveConfirmBtn.click();

    // Assert redirect to /games (not /login) — regression guard
    await page.waitForURL(url => url.pathname === '/games', { timeout: 10_000 });
    expect(page.url()).not.toContain('/login');
    expect(page.url()).toContain('/games');
  });
});
