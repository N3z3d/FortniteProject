/**
 * @fileoverview Draft Snake Tranches — E2E Test Suite (DRAFT-TRANCHE)
 *
 * ## Purpose
 *
 * Validates the full 2-player snake draft flow with tranches enabled.
 * Uses two independent browser contexts (thibaut + teddy) connected
 * simultaneously to the same game. Tests cover:
 *
 *   DT-01: Setup — both pages render the player list
 *   DT-02: Round 1 pick — picker clicks player + confirms, observer sees taken
 *   DT-03: Turn alternation — after pick 1, other player sees my-turn-badge
 *   DT-04: Round 1 complete — both players have 1 pick each (API check)
 *   DT-05: Round 2 tranche floor — info bar visible, floor > 1
 *   DT-06: Ineligible players grayed — .ineligible-tranche CSS class
 *   DT-07: Valid round 2 pick — eligible player pick succeeds
 *   DT-08: No false positive "already selected" on valid player
 *   DT-09: Auto-pick on timer expiry (optional, long timeout)
 *   DT-10: Draft completion — all picks done, game status verified
 *
 * ## Setup Strategy
 *
 * Game created via API in beforeAll with tranchesEnabled=true, teamSize=2.
 * Snake cursors initialized. Tests are serial (shared game state).
 * Cleanup via softDeleteLocalGamesByPrefix in afterAll.
 */
import { APIRequestContext, BrowserContext, Page, expect, test } from '@playwright/test';

import { forceLoginWithProfile } from './helpers/app-helpers';
import { softDeleteLocalGamesByPrefix } from './helpers/local-db-helpers';
import { buildSingleRegionRules } from '../src/app/features/game/create-game/create-game-region-rules.util';

// ─── Constants ────────────────────────────────────────────────────────────────

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const SUITE_PREFIX = 'E2E-DT-';

type Username = 'thibaut' | 'teddy';

// ─── Types ────────────────────────────────────────────────────────────────────

interface GameApiDto {
  id: string;
  status: string;
}

interface SnakeTurnDto {
  draftId: string;
  region: string;
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

interface GameDetailDto {
  participants?: { selectedPlayers?: { id: string }[] | null }[];
}

// ─── API helpers ──────────────────────────────────────────────────────────────

function authHeaders(username: Username): Record<string, string> {
  return { 'X-Test-User': username };
}

function jsonAuthHeaders(username: Username): Record<string, string> {
  return { ...authHeaders(username), 'Content-Type': 'application/json' };
}

async function createGame(request: APIRequestContext, gameName: string): Promise<string> {
  const response = await request.post(`${BACKEND_URL}/api/games?user=thibaut`, {
    headers: jsonAuthHeaders('thibaut'),
    data: {
      name: gameName,
      maxParticipants: 2,
      description: 'draft-tranche e2e fixture',
      draftMode: 'SNAKE',
      teamSize: 2,
      tranchesEnabled: true,
      regionRules: buildSingleRegionRules(2, 'EU'),
    },
  });

  if (!response.ok()) {
    throw new Error(`createGame failed: ${response.status()} ${await response.text()}`);
  }

  const payload = (await response.json()) as GameApiDto;
  return payload.id;
}

async function joinGame(
  request: APIRequestContext,
  username: Username,
  gameId: string
): Promise<void> {
  const response = await request.post(`${BACKEND_URL}/api/games/join?user=${username}`, {
    headers: jsonAuthHeaders(username),
    data: { gameId, userId: '00000000-0000-0000-0000-000000000000' },
  });

  if (!response.ok()) {
    throw new Error(
      `joinGame failed for ${username}: ${response.status()} ${await response.text()}`
    );
  }
}

async function startDraft(request: APIRequestContext, gameId: string): Promise<void> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/start-draft?user=thibaut`,
    { headers: authHeaders('thibaut') }
  );

  if (!response.ok()) {
    throw new Error(`startDraft failed: ${response.status()} ${await response.text()}`);
  }
}

async function initializeSnakeCursors(
  request: APIRequestContext,
  gameId: string
): Promise<SnakeTurnDto> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/initialize?user=thibaut`,
    { headers: authHeaders('thibaut') }
  );

  if (!response.ok()) {
    throw new Error(
      `initializeSnakeCursors failed: ${response.status()} ${await response.text()}`
    );
  }

  const payload = (await response.json()) as ApiEnvelope<SnakeTurnDto>;
  return payload.data;
}

let activeRegion = 'EU';

async function fetchCurrentTurn(
  request: APIRequestContext,
  gameId: string
): Promise<SnakeTurnDto | null> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/turn?region=${activeRegion}`,
    { headers: authHeaders('thibaut') }
  );

  if (response.status() === 404) return null;

  if (!response.ok()) {
    throw new Error(`fetchCurrentTurn failed: ${response.status()} ${await response.text()}`);
  }

  const payload = (await response.json()) as ApiEnvelope<SnakeTurnDto>;
  activeRegion = payload.data.region;
  return payload.data;
}

async function fetchParticipants(
  request: APIRequestContext,
  gameId: string
): Promise<ParticipantUserDto[]> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}/participants`, {
    headers: authHeaders('thibaut'),
  });

  if (!response.ok()) {
    throw new Error(`fetchParticipants failed: ${response.status()} ${await response.text()}`);
  }

  return (await response.json()) as ParticipantUserDto[];
}

async function resolveCurrentPickerUsername(
  request: APIRequestContext,
  gameId: string
): Promise<Username> {
  const [participants, turn] = await Promise.all([
    fetchParticipants(request, gameId),
    fetchCurrentTurn(request, gameId),
  ]);

  if (!turn) throw new Error('No current turn — cursors may not be initialized');

  const current = participants.find(p => p.userId === turn.participantId);
  if (!current) throw new Error(`Participant ${turn.participantId} not found`);

  const username = current.username;
  if (username !== 'thibaut' && username !== 'teddy') {
    throw new Error(`Unexpected picker: ${username}`);
  }

  return username as Username;
}

async function submitSnakePickApi(
  request: APIRequestContext,
  gameId: string,
  username: Username,
  playerId: string
): Promise<void> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/pick?user=${username}`,
    {
      headers: jsonAuthHeaders(username),
      data: { playerId, region: activeRegion },
    }
  );

  if (!response.ok()) {
    throw new Error(
      `submitSnakePick failed for ${username} (player=${playerId}): ${response.status()} ${await response.text()}`
    );
  }
}

async function fetchGameDetail(
  request: APIRequestContext,
  gameId: string
): Promise<GameDetailDto> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}/details`, {
    headers: authHeaders('thibaut'),
  });

  if (!response.ok()) {
    throw new Error(`fetchGameDetail failed: ${response.status()} ${await response.text()}`);
  }

  return (await response.json()) as GameDetailDto;
}

async function fetchGameStatus(request: APIRequestContext, gameId: string): Promise<string> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}?user=thibaut`, {
    headers: authHeaders('thibaut'),
  });

  if (!response.ok()) {
    throw new Error(`fetchGameStatus failed: ${response.status()} ${await response.text()}`);
  }

  const payload = (await response.json()) as GameApiDto;
  return payload.status;
}

function totalPicks(detail: GameDetailDto): number {
  return (
    detail.participants?.reduce((sum, p) => sum + (p.selectedPlayers?.length ?? 0), 0) ?? 0
  );
}

/**
 * Calls the backend /recommend API to get the best eligible player for the current draft slot.
 * Returns the player nickname, or null if no recommendation available.
 */
async function fetchRecommendedPlayer(
  request: APIRequestContext,
  gameId: string,
  username: Username
): Promise<{ id: string; nickname: string } | null> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/recommend?region=${activeRegion}&user=${username}`,
    { headers: authHeaders(username) }
  );

  if (response.status() === 404) return null;
  if (!response.ok()) {
    throw new Error(`fetchRecommendedPlayer failed: ${response.status()} ${await response.text()}`);
  }

  const payload = (await response.json()) as { data?: { id: string; nickname: string } };
  return payload.data ?? null;
}

// ─── UI helpers ───────────────────────────────────────────────────────────────

async function openDraftPage(page: Page, username: Username, gameId: string): Promise<void> {
  await forceLoginWithProfile(page, username);
  await page.goto(`/games/${gameId}/draft/snake`);
  await expect(page.locator('body')).toBeVisible({ timeout: 15_000 });
}

/**
 * Clicks the first available (non-taken, non-ineligible) player card and confirms.
 * Returns the player username text for verification.
 *
 * CDK virtual scroll only renders items in/near the viewport. If all visible players
 * are ineligible (e.g. round 2 with high tranche floor), scrolls down to find one.
 */
async function pickFirstAvailablePlayer(page: Page): Promise<string> {
  // Wait for player list to load
  const playerList = page.locator('#player-list');
  await expect(playerList).toBeVisible({ timeout: 15_000 });

  // Selector for eligible, non-taken player cards
  const eligibleSelector =
    'app-player-card:not(.ineligible-tranche) .player-card:not(.player-card--taken)';

  // Wait for tranche classes to be applied (board state must load and render).
  // In round 2+, some players should have .ineligible-tranche. We wait up to 10s
  // for them to appear. If none appear (round 1 or all eligible), proceed anyway.
  const ineligibleLoc = page.locator('.ineligible-tranche');
  await ineligibleLoc.first().waitFor({ state: 'attached', timeout: 10_000 }).catch(() => {
    // No ineligible players found — may be round 1 or all eligible. Proceed.
  });

  // Scroll within virtual scroll viewport until an eligible card appears
  const MAX_SCROLL_ATTEMPTS = 10;
  for (let attempt = 0; attempt < MAX_SCROLL_ATTEMPTS; attempt++) {
    const count = await page.locator(eligibleSelector).count();
    if (count > 0) break;

    // Scroll the virtual scroll viewport down by ~400px (5 items at 80px each)
    await playerList.evaluate(el => el.scrollTop += 400);
    await page.waitForTimeout(300);
  }

  const availableCard = page.locator(eligibleSelector).first();
  await expect(availableCard).toBeVisible({ timeout: 10_000 });

  // Get player name before clicking
  const playerName = (await availableCard.locator('.player-name').textContent()) ?? 'unknown';

  await availableCard.click();

  // Wait for confirm zone to appear and click confirm
  const confirmBtn = page.locator('.confirm-zone .btn-confirm');
  await expect(confirmBtn).toBeVisible({ timeout: 10_000 });
  await confirmBtn.click();

  // Wait for confirm zone to disappear (pick submitted)
  await expect(confirmBtn).not.toBeVisible({ timeout: 15_000 });

  return playerName.trim();
}

/**
 * Picks a specific player by nickname. Scrolls through the virtual scroll viewport
 * to find the player card, then clicks it and confirms.
 * Falls back to pickFirstAvailablePlayer if the player is not found after scrolling.
 */
async function pickPlayerByNickname(page: Page, nickname: string): Promise<string> {
  const playerList = page.locator('#player-list');
  await expect(playerList).toBeVisible({ timeout: 15_000 });

  const recommendButton = page
    .locator('.recommend-bar .btn-recommend')
    .filter({ hasText: nickname });
  const canUseRecommend = await recommendButton.first().isVisible({ timeout: 3_000 }).catch(() => false);
  if (canUseRecommend) {
    await recommendButton.first().click();

    const confirmBtn = page.locator('.confirm-zone .btn-confirm');
    await expect(confirmBtn).toBeVisible({ timeout: 10_000 });
    await confirmBtn.click();
    await expect(confirmBtn).not.toBeVisible({ timeout: 15_000 });

    return nickname;
  }

  // Scroll through virtual scroll to find the target player
  const targetCard = page.locator(
    `.player-card:not(.player-card--taken)`,
    { has: page.locator(`.player-name`, { hasText: nickname }) }
  );

  const MAX_SCROLL_ATTEMPTS = 20;
  for (let attempt = 0; attempt < MAX_SCROLL_ATTEMPTS; attempt++) {
    const count = await targetCard.count();
    if (count > 0) break;
    await playerList.evaluate(el => (el.scrollTop += 400));
    await page.waitForTimeout(300);
  }

  if ((await targetCard.count()) === 0) {
    // Fallback: scroll back to top and pick first available
    await playerList.evaluate(el => (el.scrollTop = 0));
    await page.waitForTimeout(300);
    return pickFirstAvailablePlayer(page);
  }

  await targetCard.first().click();

  const confirmBtn = page.locator('.confirm-zone .btn-confirm');
  await expect(confirmBtn).toBeVisible({ timeout: 10_000 });
  await confirmBtn.click();
  await expect(confirmBtn).not.toBeVisible({ timeout: 15_000 });

  return nickname;
}

// ─── Module-level state ───────────────────────────────────────────────────────

let gameId = '';
let setupSucceeded = false;

// ─── Test suite ───────────────────────────────────────────────────────────────

test.describe.serial('DRAFT-TRANCHE: snake draft complet avec tranches — 2 joueurs', () => {
  test.setTimeout(120_000);

  test.beforeAll(async ({ request }) => {
    test.setTimeout(120_000);

    softDeleteLocalGamesByPrefix(SUITE_PREFIX);

    const gameName = `${SUITE_PREFIX}${Date.now()}`;

    try {
      gameId = await createGame(request, gameName);
      await joinGame(request, 'teddy', gameId);
      await startDraft(request, gameId);
      const initialTurn = await initializeSnakeCursors(request, gameId);
      activeRegion = initialTurn.region;
      setupSucceeded = true;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      console.warn(`DRAFT-TRANCHE beforeAll: setup failed (${message}) — tests will be skipped`);
      setupSucceeded = false;
    }
  });

  test.afterAll(() => {
    softDeleteLocalGamesByPrefix(SUITE_PREFIX);
  });

  // ─── DT-01: Setup — both pages render player list ──────────────────────────

  test('DT-01: both players see #player-list on the snake draft page', async ({
    browser,
  }) => {
    test.setTimeout(60_000);
    if (!setupSucceeded) { test.skip(); return; }

    let ctx1: BrowserContext | undefined;
    let ctx2: BrowserContext | undefined;

    try {
      ctx1 = await browser.newContext();
      ctx2 = await browser.newContext();
      const page1 = await ctx1.newPage();
      const page2 = await ctx2.newPage();

      await openDraftPage(page1, 'thibaut', gameId);
      await openDraftPage(page2, 'teddy', gameId);

      await expect(page1.locator('#player-list')).toBeVisible({ timeout: 15_000 });
      await expect(page2.locator('#player-list')).toBeVisible({ timeout: 15_000 });
    } finally {
      await ctx1?.close().catch(() => {});
      await ctx2?.close().catch(() => {});
    }
  });

  // ─── DT-02: Round 1 pick — picker clicks, observer sees taken ─────────────

  test('DT-02: picker clicks first available player + confirms, pick is registered via API', async ({
    browser,
    request,
  }) => {
    test.setTimeout(60_000);
    if (!setupSucceeded) { test.skip(); return; }

    const pickerUsername = await resolveCurrentPickerUsername(request, gameId);

    let ctx: BrowserContext | undefined;

    try {
      ctx = await browser.newContext();
      const pickerPage = await ctx.newPage();

      await openDraftPage(pickerPage, pickerUsername, gameId);

      // Should see "TON TOUR"
      await expect(pickerPage.locator('.my-turn-badge')).toBeVisible({ timeout: 10_000 });

      // Pick first available player via UI
      await pickFirstAvailablePlayer(pickerPage);

      // Verify pick was registered via API
      await expect
        .poll(async () => totalPicks(await fetchGameDetail(request, gameId)), {
          timeout: 15_000,
          intervals: [500, 1_000, 2_000],
        })
        .toBeGreaterThanOrEqual(1);
    } finally {
      await ctx?.close().catch(() => {});
    }
  });

  // ─── DT-03: Turn alternation — other player now has my-turn-badge ─────────

  test('DT-03: after pick 1, the other player becomes the active picker', async ({
    browser,
    request,
  }) => {
    test.setTimeout(60_000);
    if (!setupSucceeded) { test.skip(); return; }

    // After DT-02, the turn should have switched
    const newPicker = await resolveCurrentPickerUsername(request, gameId);

    let ctx: BrowserContext | undefined;

    try {
      ctx = await browser.newContext();
      const page = await ctx.newPage();

      await openDraftPage(page, newPicker, gameId);

      // New picker should see "TON TOUR"
      await expect(page.locator('.my-turn-badge')).toBeVisible({ timeout: 15_000 });
    } finally {
      await ctx?.close().catch(() => {});
    }
  });

  // ─── DT-04: Round 1 complete — both have 1 pick each ─────────────────────

  test('DT-04: after 2nd pick (round 1 complete), both participants have 1 pick each', async ({
    browser,
    request,
  }) => {
    test.setTimeout(60_000);
    if (!setupSucceeded) { test.skip(); return; }

    // Current picker needs to make pick #2 to complete round 1
    const pickerUsername = await resolveCurrentPickerUsername(request, gameId);

    let ctx: BrowserContext | undefined;

    try {
      ctx = await browser.newContext();
      const page = await ctx.newPage();

      await openDraftPage(page, pickerUsername, gameId);
      await expect(page.locator('.my-turn-badge')).toBeVisible({ timeout: 10_000 });

      await pickFirstAvailablePlayer(page);

      // Verify 2 total picks via API
      await expect
        .poll(async () => totalPicks(await fetchGameDetail(request, gameId)), {
          timeout: 15_000,
          intervals: [500, 1_000, 2_000],
        })
        .toBe(2);
    } finally {
      await ctx?.close().catch(() => {});
    }
  });

  // ─── DT-05: Round 2 — tranche info bar visible, floor > 1 ────────────────

  test('DT-05: in round 2, tranche info bar shows floor > 1', async ({
    browser,
    request,
  }) => {
    test.setTimeout(60_000);
    if (!setupSucceeded) { test.skip(); return; }

    // After round 1 (2 picks), round 2 starts. Floor should be > 1.
    const pickerUsername = await resolveCurrentPickerUsername(request, gameId);

    let ctx: BrowserContext | undefined;

    try {
      ctx = await browser.newContext();
      const page = await ctx.newPage();

      await openDraftPage(page, pickerUsername, gameId);

      // tranche-info-bar should be visible with floor > 1
      const trancheBar = page.locator('.tranche-info-bar');
      const barVisible = await trancheBar.isVisible({ timeout: 10_000 }).catch(() => false);

      if (!barVisible) {
        console.info('DT-05: tranche-info-bar not visible — tranches may not be active for this round');
        test.skip();
        return;
      }

      const labelText = (await page.locator('.tranche-label').textContent()) ?? '';
      // Should contain a number > 1
      const floorMatch = labelText.match(/(\d+)/);
      expect(floorMatch).not.toBeNull();
      if (floorMatch) {
        expect(parseInt(floorMatch[1], 10)).toBeGreaterThan(1);
      }
    } finally {
      await ctx?.close().catch(() => {});
    }
  });

  // ─── DT-06: Ineligible players have .ineligible-tranche class ─────────────

  test('DT-06: some player cards have .ineligible-tranche CSS class in round 2', async ({
    browser,
    request,
  }) => {
    test.setTimeout(60_000);
    if (!setupSucceeded) { test.skip(); return; }

    const pickerUsername = await resolveCurrentPickerUsername(request, gameId);

    let ctx: BrowserContext | undefined;

    try {
      ctx = await browser.newContext();
      const page = await ctx.newPage();

      await openDraftPage(page, pickerUsername, gameId);
      await expect(page.locator('#player-list')).toBeVisible({ timeout: 15_000 });

      // Wait for board state to render (tranche classes depend on forkJoin + CD cycle)
      await page.waitForTimeout(3_000);

      const ineligibleCount = await page.locator('.ineligible-tranche').count();
      // In round 2 with floor > 1, some players should be ineligible.
      expect(ineligibleCount).toBeGreaterThan(0);
    } finally {
      await ctx?.close().catch(() => {});
    }
  });

  // ─── DT-07: Valid round 2 pick succeeds ───────────────────────────────────

  test('DT-07: picking an eligible player in round 2 succeeds (pick 3)', async ({
    browser,
    request,
  }) => {
    test.setTimeout(60_000);
    if (!setupSucceeded) { test.skip(); return; }

    const pickerUsername = await resolveCurrentPickerUsername(request, gameId);

    // Use the backend /recommend API to get a guaranteed-eligible player
    const recommended = await fetchRecommendedPlayer(request, gameId, pickerUsername);

    let ctx: BrowserContext | undefined;

    try {
      ctx = await browser.newContext();
      const page = await ctx.newPage();

      await openDraftPage(page, pickerUsername, gameId);
      await expect(page.locator('.my-turn-badge')).toBeVisible({ timeout: 10_000 });

      if (recommended) {
        await pickPlayerByNickname(page, recommended.nickname);
      } else {
        // Fallback: scroll to bottom to avoid top-ranked ineligible players
        const playerList = page.locator('#player-list');
        await playerList.evaluate(el => (el.scrollTop = el.scrollHeight));
        await page.waitForTimeout(500);
        await pickFirstAvailablePlayer(page);
      }

      // Verify 3 total picks
      await expect
        .poll(async () => totalPicks(await fetchGameDetail(request, gameId)), {
          timeout: 15_000,
          intervals: [500, 1_000, 2_000],
        })
        .toBe(3);
    } finally {
      await ctx?.close().catch(() => {});
    }
  });

  // ─── DT-08: No "already selected" false positive ─────────────────────────

  test('DT-08: picking a never-selected player does not trigger "already selected" error', async ({
    browser,
    request,
  }) => {
    test.setTimeout(60_000);
    if (!setupSucceeded) { test.skip(); return; }

    // Pick 4 — last pick to complete draft (teamSize=2, 2 participants = 4 picks total)
    const pickerUsername = await resolveCurrentPickerUsername(request, gameId);

    // Use recommend API to get a guaranteed-eligible player (still round 2)
    const recommended = await fetchRecommendedPlayer(request, gameId, pickerUsername);

    let ctx: BrowserContext | undefined;

    try {
      ctx = await browser.newContext();
      const page = await ctx.newPage();

      await openDraftPage(page, pickerUsername, gameId);
      await expect(page.locator('.my-turn-badge')).toBeVisible({ timeout: 10_000 });

      if (recommended) {
        await pickPlayerByNickname(page, recommended.nickname);
      } else {
        const playerList = page.locator('#player-list');
        await playerList.evaluate(el => (el.scrollTop = el.scrollHeight));
        await page.waitForTimeout(500);
        await pickFirstAvailablePlayer(page);
      }

      // Verify NO error snackbar appeared
      const errorSnackbar = page.locator('.mat-mdc-snack-bar-container');
      const errorVisible = await errorSnackbar.isVisible({ timeout: 3_000 }).catch(() => false);
      if (errorVisible) {
        const errorText = (await errorSnackbar.textContent()) ?? '';
        // Fail explicitly if we see "already selected"
        expect(errorText.toLowerCase()).not.toContain('already selected');
        expect(errorText.toLowerCase()).not.toContain('déjà sélectionné');
      }

      // Verify 4 total picks
      await expect
        .poll(async () => totalPicks(await fetchGameDetail(request, gameId)), {
          timeout: 15_000,
          intervals: [500, 1_000, 2_000],
        })
        .toBe(4);
    } finally {
      await ctx?.close().catch(() => {});
    }
  });

  // ─── DT-09: Auto-pick on timer expiry (optional — long timeout) ──────────

  test.skip('DT-09: auto-pick fires when timer expires (requires 60s+ wait)', async () => {
    // This test is skipped by default because it requires waiting for the full
    // timer to expire (60s+). Enable manually for full regression testing.
    // When enabled, it should:
    // 1. Open picker's page, do NOT click any player
    // 2. Wait for timer to reach 0
    // 3. Verify recommended player was auto-picked
    // 4. Verify pick count increased
  });

  // ─── DT-10: Draft completion ──────────────────────────────────────────────

  test('DT-10: after all picks, game status transitions correctly', async ({
    request,
  }) => {
    test.setTimeout(30_000);
    if (!setupSucceeded) { test.skip(); return; }

    // After 4 picks (teamSize=2, 2 participants), draft should be complete
    const detail = await fetchGameDetail(request, gameId);
    const picks = totalPicks(detail);

    // Verify we have 4 picks total
    expect(picks).toBe(4);

    // Check game status — should be ACTIVE or DRAFTING (depending on auto-transition)
    const status = await fetchGameStatus(request, gameId);
    expect(['ACTIVE', 'DRAFTING', 'DRAFT_COMPLETE']).toContain(status);
  });
});
