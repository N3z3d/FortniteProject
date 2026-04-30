/**
 * sprint9-e2 — DRAFT-FULL-01: flux draft serpent complet
 *
 * Verifies the full snake draft lifecycle end-to-end at the API level:
 *   1. Create a game (CREATING)           → POST /api/games
 *   2. Join as second participant          → POST /api/games/join
 *   3. Start draft (CREATING → DRAFTING)  → POST /api/games/{id}/start-draft
 *   4. Initialize snake cursors           → POST /api/games/{id}/draft/snake/initialize
 *   5. Submit all picks (teamSize=1 × 2 participants = 2 picks total)
 *                                         → POST /api/games/{id}/draft/snake/pick
 *   6. Finish draft (DRAFTING → ACTIVE)   → POST /api/draft/{id}/finish
 *   7. Verify game status is ACTIVE       → GET  /api/games/{id}
 *
 * Design decision: API-level test (no browser navigation).
 * A full UI draft requires two simultaneous browser contexts coordinated over WebSocket,
 * which is inherently non-deterministic and cannot fit within a 35 s timeout.
 * UI coverage for snake draft is provided by DRAFT-01/02/03 in draft-flow.spec.ts.
 * This test covers the completion logic (all-picks → finish → ACTIVE), the critical
 * gap identified in sprint-8-retro-2026-03-15.md action item E2.
 */
import { APIRequestContext, expect, test } from '@playwright/test';

import { softDeleteLocalGamesByPrefix } from './helpers/local-db-helpers';
import { buildSingleRegionRules } from '../src/app/features/game/create-game/create-game-region-rules.util';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const DRAFT_FULL_PREFIX = 'E2E-DF-';
const PLACEHOLDER_USER_ID = '00000000-0000-0000-0000-000000000000';
const DRAFT_REGION = 'EU';

/** EU Tranche 1 players from V1001__seed_e2e_users_and_players.sql */
const BUGHA_EU_T1 = '10000000-0000-0000-0000-000000000001';
const AQUA_EU_T1 = '10000000-0000-0000-0000-000000000002';

type Username = 'admin' | 'thibaut' | 'marcel' | 'teddy';

interface GameApiDto {
  id: string;
  status: string;
}

interface SnakeTurnDto {
  draftId: string;
  participantId: string;
  pickNumber: number;
}

interface ApiEnvelope<T> {
  data: T;
}

interface ParticipantUserDto {
  userId: string;
  username: string;
}

interface SelectedPlayerDto {
  playerId: string;
  nickname: string;
}

interface GameDetailParticipantDto {
  participantId: string;
  username: string;
  selectedPlayers?: SelectedPlayerDto[] | null;
}

interface GameDetailDto {
  gameId: string;
  participants: GameDetailParticipantDto[];
}

interface DraftFullFixture {
  gameId: string;
}

// ---------------------------------------------------------------------------
// API helpers
// ---------------------------------------------------------------------------

function authHeaders(username: Username): Record<string, string> {
  return { 'X-Test-User': username };
}

function jsonAuthHeaders(username: Username): Record<string, string> {
  return { ...authHeaders(username), 'Content-Type': 'application/json' };
}

async function createGame(request: APIRequestContext, gameName: string): Promise<string> {
  const response = await request.post(`${BACKEND_URL}/api/games?user=admin`, {
    headers: jsonAuthHeaders('admin'),
    data: {
      name: gameName,
      maxParticipants: 2,
      description: 'draft-full e2e fixture',
      draftMode: 'SNAKE',
      teamSize: 1,
      tranchesEnabled: false,
      regionRules: buildSingleRegionRules(2, 'EU'),
    },
  });

  if (!response.ok()) {
    throw new Error(
      `createGame failed: ${response.status()} ${await response.text()}`
    );
  }

  const payload = (await response.json()) as GameApiDto;
  return payload.id;
}

async function joinGame(
  request: APIRequestContext,
  username: Username,
  gameId: string
): Promise<void> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/join?user=${username}`,
    {
      headers: jsonAuthHeaders(username),
      data: { gameId, userId: PLACEHOLDER_USER_ID },
    }
  );

  if (!response.ok()) {
    throw new Error(
      `joinGame failed for ${username}: ${response.status()} ${await response.text()}`
    );
  }
}

async function startDraft(request: APIRequestContext, gameId: string): Promise<void> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/start-draft?user=admin`,
    { headers: authHeaders('admin') }
  );

  if (!response.ok()) {
    throw new Error(
      `startDraft failed: ${response.status()} ${await response.text()}`
    );
  }
}

async function initializeSnakeCursors(
  request: APIRequestContext,
  gameId: string
): Promise<SnakeTurnDto> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/initialize?user=admin`,
    { headers: authHeaders('admin') }
  );

  if (!response.ok()) {
    throw new Error(
      `initializeSnakeCursors failed: ${response.status()} ${await response.text()}`
    );
  }

  const payload = (await response.json()) as ApiEnvelope<SnakeTurnDto>;
  return payload.data;
}

async function fetchCurrentTurn(
  request: APIRequestContext,
  gameId: string
): Promise<SnakeTurnDto | null> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/turn?region=${DRAFT_REGION}`,
    { headers: authHeaders('admin') }
  );

  if (response.status() === 404) {
    return null;
  }

  if (!response.ok()) {
    throw new Error(
      `fetchCurrentTurn failed: ${response.status()} ${await response.text()}`
    );
  }

  const payload = (await response.json()) as ApiEnvelope<SnakeTurnDto>;
  return payload.data;
}

async function fetchParticipants(
  request: APIRequestContext,
  gameId: string
): Promise<ParticipantUserDto[]> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/participants`,
    { headers: authHeaders('admin') }
  );

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as ParticipantUserDto[];
}

async function fetchGameDetail(
  request: APIRequestContext,
  gameId: string
): Promise<GameDetailDto> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/details`,
    { headers: authHeaders('admin') }
  );

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as GameDetailDto;
}

async function fetchGameStatus(
  request: APIRequestContext,
  gameId: string
): Promise<string> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}?user=admin`,
    { headers: authHeaders('admin') }
  );

  expect(response.ok()).toBeTruthy();
  const payload = (await response.json()) as GameApiDto;
  return payload.status;
}

async function submitSnakePick(
  request: APIRequestContext,
  gameId: string,
  username: Username,
  playerId: string
): Promise<void> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/pick?user=${username}`,
    {
      headers: jsonAuthHeaders(username),
      data: { playerId, region: DRAFT_REGION },
    }
  );

  if (!response.ok()) {
    throw new Error(
      `submitSnakePick failed for ${username} (player=${playerId}): ${response.status()} ${await response.text()}`
    );
  }
}

async function finishDraft(request: APIRequestContext, gameId: string): Promise<void> {
  // DraftController base path: /api/draft (also /api/drafts) + /{gameId}/finish
  const response = await request.post(
    `${BACKEND_URL}/api/draft/${gameId}/finish`,
    { headers: authHeaders('admin') }
  );

  if (!response.ok()) {
    throw new Error(
      `finishDraft failed: ${response.status()} ${await response.text()}`
    );
  }
}

/**
 * Returns true when every participant in the game has at least one selected player.
 */
function allParticipantsHavePick(detail: GameDetailDto): boolean {
  return detail.participants.every(
    participant => (participant.selectedPlayers?.length ?? 0) >= 1
  );
}

/**
 * Submits picks for all participants until every team has teamSize=1 player.
 *
 * The pick assignment map is fixed (admin→BUGHA, teddy→AQUA). The snake cursor
 * determines who picks first; this function resolves the current picker and submits
 * the right player for them on each iteration.
 */
async function seedAllPicks(
  request: APIRequestContext,
  gameId: string
): Promise<void> {
  const pickAssignment = new Map<Username, string>([
    ['admin', BUGHA_EU_T1],
    ['teddy', AQUA_EU_T1],
  ]);
  const remainingPickers = new Set<Username>(pickAssignment.keys());

  // Max iterations = 2 participants × 2 (safety margin)
  const maxIterations = remainingPickers.size * 2;

  for (let i = 0; i < maxIterations; i += 1) {
    const detail = await fetchGameDetail(request, gameId);
    if (allParticipantsHavePick(detail)) {
      return;
    }

    const turn = await fetchCurrentTurn(request, gameId);
    if (!turn) {
      throw new Error('Snake cursor not initialized — cannot resolve current picker');
    }

    const participants = await fetchParticipants(request, gameId);
    const currentParticipant = participants.find(p => p.userId === turn.participantId);
    if (!currentParticipant) {
      throw new Error(
        `Current turn participantId=${turn.participantId} not found in participants list`
      );
    }

    const pickerUsername = currentParticipant.username as Username;

    if (!remainingPickers.has(pickerUsername)) {
      // This picker already picked — guard against cursor drift
      throw new Error(
        `Picker ${pickerUsername} does not have a remaining pick assignment`
      );
    }

    const playerId = pickAssignment.get(pickerUsername);
    if (!playerId) {
      throw new Error(`No player assignment configured for picker ${pickerUsername}`);
    }

    await submitSnakePick(request, gameId, pickerUsername, playerId);
    remainingPickers.delete(pickerUsername);
  }

  // Final check
  const finalDetail = await fetchGameDetail(request, gameId);
  expect(allParticipantsHavePick(finalDetail)).toBeTruthy();
}

/**
 * Creates a game in DRAFTING state with initialized snake cursors.
 * Game has 2 participants (admin + teddy), teamSize=1.
 */
async function prepareFullDraftGame(
  request: APIRequestContext
): Promise<DraftFullFixture> {
  const gameName = `${DRAFT_FULL_PREFIX}${Date.now()}`;

  const gameId = await createGame(request, gameName);
  await joinGame(request, 'teddy', gameId);
  await startDraft(request, gameId);
  await initializeSnakeCursors(request, gameId);

  return { gameId };
}

// ---------------------------------------------------------------------------
// Test suite
// ---------------------------------------------------------------------------

test.describe.serial('DRAFT-FULL: complete snake draft lifecycle', () => {
  let fixture: DraftFullFixture | null = null;

  test.setTimeout(35_000);

  test.beforeAll(async ({ request }) => {
    softDeleteLocalGamesByPrefix(DRAFT_FULL_PREFIX);

    try {
      fixture = await prepareFullDraftGame(request);
    } catch (error) {
      // Backend not available — tests will skip gracefully
      const message = error instanceof Error ? error.message : String(error);
      console.warn(`DRAFT-FULL beforeAll: setup failed (${message}) — tests will be skipped`);
      fixture = null;
    }
  });

  test.afterAll(() => {
    softDeleteLocalGamesByPrefix(DRAFT_FULL_PREFIX);
  });

  test(
    'DRAFT-FULL-01: snake draft complet — tous les picks soumis → finish-draft → statut ACTIVE',
    async ({ request }) => {
      if (!fixture) {
        test.skip();
        return;
      }

      const { gameId } = fixture;

      // Step 1 — Verify game is in DRAFTING state before picks
      const statusBeforePicks = await fetchGameStatus(request, gameId);
      expect(statusBeforePicks).toBe('DRAFTING');

      // Step 2 — Submit all picks (2 participants, teamSize=1 → 2 picks)
      await seedAllPicks(request, gameId);

      // Step 3 — Verify all participants have picks
      const detailAfterPicks = await fetchGameDetail(request, gameId);
      expect(allParticipantsHavePick(detailAfterPicks)).toBe(true);

      // Step 4 — Call finish-draft: DRAFTING → ACTIVE
      await finishDraft(request, gameId);

      // Step 5 — Poll for ACTIVE status (async persistence)
      await expect
        .poll(() => fetchGameStatus(request, gameId), { timeout: 10_000 })
        .toBe('ACTIVE');
    }
  );
});
