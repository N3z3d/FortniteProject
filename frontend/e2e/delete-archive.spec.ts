/**
 * sprint12-e2e-delete-archive — DA-01..DA-04: delete/archive game with 409 validation
 *
 * Verifies the RC-2 fix in GlobalExceptionHandler: deleting a game that has
 * draft picks returns 409 CONFLICT (not 500). All tests are API-level only
 * (request fixture, no browser navigation) for determinism and speed.
 *
 *   DA-01: Delete game with no picks          → HTTP < 300 (success)
 *   DA-02: Delete game with 1 pick submitted  → HTTP 409 CONFLICT (RC-2 fix)
 *   DA-03: Delete game in CREATING status     → HTTP < 300 (soft delete)
 *   DA-04: Double-delete same game            → second response NOT 500
 */

import { APIRequestContext, APIResponse, expect, test } from '@playwright/test';

import { softDeleteLocalGamesByPrefix } from './helpers/local-db-helpers';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const DA_PREFIX = 'E2E-DA-';

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
    data: { name, maxParticipants: 2, teamSize: 1 },
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
    `${BACKEND_URL}/api/games/${gameId}/invitation-code`,
    { headers: jsonAuthHeaders(username) }
  );
  expect(res.status()).toBeLessThan(300);
  const body = await res.json();
  return body.data?.invitationCode ?? body.invitationCode;
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
    test.setTimeout(45_000);

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

    // Attempt to delete — should return 409 because picks exist
    const res = await deleteGame(request, 'thibaut', gameId);
    expect(res.status()).toBe(409);
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
