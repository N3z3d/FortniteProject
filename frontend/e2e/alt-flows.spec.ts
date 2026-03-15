/**
 * sprint8-e1 — E2E flux alternatifs critiques
 *
 * Tests the 3 rejection scenarios that complement the happy-path E2E suite:
 *   ALT-01: swap with player from a different region           → 400 INVALID_SWAP
 *   ALT-02: trade proposal offering a player not in roster     → 400 INVALID_SWAP
 *   ALT-03: snake draft pick for a player already drafted      → 409 PLAYER_ALREADY_SELECTED
 *
 * All tests are purely API-level (no browser required) to keep them fast
 * and resilient. The backend is exercised directly via Playwright `request`.
 */
import { expect, test } from '@playwright/test';

import {
  cleanupTradeFixtureUsers,
  ErrorResponseDto,
  TradeReadyGameFixture,
  postSwapSolo,
  prepareTradeReadyGame,
  softDeleteLocalGamesByPrefix,
} from './helpers/trade-swap-helpers';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';

// ---------------------------------------------------------------------------
// Player IDs from V1001__seed_e2e_users_and_players.sql
// ---------------------------------------------------------------------------

/** EU Tranche 1 — thibaut's opening pick */
const BUGHA_EU_T1 = '10000000-0000-0000-0000-000000000001';
/** EU Tranche 1 — not in any roster after seeding */
const MONGRAAL_EU_T1 = '10000000-0000-0000-0000-000000000003';
/** NAW Tranche 3 — wrong region for an EU player swap */
const NAW_LOW1_T3 = '10000000-0000-0000-0000-000000000017';

const ALT_PREFIX = 'E2E-ALT-';

test.describe.serial('ALT-FLOWS: alternative / rejection flows', () => {
  let fixture: TradeReadyGameFixture;

  test.setTimeout(180_000);

  test.beforeAll(async ({ request }) => {
    softDeleteLocalGamesByPrefix(ALT_PREFIX);
    await cleanupTradeFixtureUsers(request);
    // One game is enough — all 3 scenarios use independent fixtures or
    // make assertions that do NOT mutate state.
    fixture = await prepareTradeReadyGame(request, 'alt-flows', ALT_PREFIX);
  });

  test.afterAll(() => {
    softDeleteLocalGamesByPrefix(ALT_PREFIX);
  });

  // -------------------------------------------------------------------------
  // ALT-01: swap with player from a different region
  // -------------------------------------------------------------------------
  test('ALT-01: swap-solo with wrong region is rejected (INVALID_SWAP)', async ({ request }) => {
    // thibaut owns Bugha_EU (EU, T1).
    // Trying to swap for NAWLow1 (NAW, T3) — different region → must be rejected.
    const response = await postSwapSolo(
      request,
      fixture.gameId,
      'thibaut',
      BUGHA_EU_T1,
      NAW_LOW1_T3
    );

    expect(response.status()).toBe(400);
    const body = (await response.json()) as ErrorResponseDto;
    expect(body.code).toBe('INVALID_SWAP');
    expect(body.message?.toLowerCase()).toContain('region');
  });

  // -------------------------------------------------------------------------
  // ALT-02: trade proposal offering a player not in proposer's roster
  // -------------------------------------------------------------------------
  test('ALT-02: trade proposal with un-owned player is rejected (INVALID_SWAP)', async ({
    request,
  }) => {
    // thibaut owns BUGHA_EU_T1 after seeding.
    // He tries to offer MONGRAAL_EU_T1 (0003) which he did NOT draft → must be rejected.
    const response = await request.post(
      `${BACKEND_URL}/api/games/${fixture.gameId}/draft/trade?user=thibaut`,
      {
        headers: { 'X-Test-User': 'thibaut', 'Content-Type': 'application/json' },
        data: {
          targetParticipantId: fixture.participants.teddy,
          playerFromProposerId: MONGRAAL_EU_T1, // not in thibaut's roster
          playerFromTargetId: fixture.players.teddy,
        },
      }
    );

    expect(response.status()).toBe(400);
    const body = (await response.json()) as ErrorResponseDto;
    expect(body.code).toBe('INVALID_SWAP');
    expect(body.message?.toLowerCase()).toContain('not in your team');
  });

  // -------------------------------------------------------------------------
  // ALT-03: snake draft pick for a player already picked
  // -------------------------------------------------------------------------
  test('ALT-03: snake pick of already-drafted player is rejected (PLAYER_ALREADY_SELECTED)', async ({
    request,
  }) => {
    // BUGHA_EU_T1 was already drafted by thibaut during fixture setup.
    // Submitting the same player again in the same draft must be rejected.
    const response = await request.post(
      `${BACKEND_URL}/api/games/${fixture.gameId}/draft/snake/pick?user=thibaut`,
      {
        headers: { 'X-Test-User': 'thibaut', 'Content-Type': 'application/json' },
        data: { playerId: BUGHA_EU_T1, region: 'GLOBAL' },
      }
    );

    // Duplicate pick → 409 PLAYER_ALREADY_SELECTED (DraftTrancheService guard)
    expect(response.status()).toBe(409);
    const body = (await response.json()) as ErrorResponseDto;
    expect(body.code).toBe('PLAYER_ALREADY_SELECTED');
  });
});
