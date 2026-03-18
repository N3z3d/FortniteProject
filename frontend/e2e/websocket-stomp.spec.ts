/**
 * @fileoverview WebSocket STOMP — Multi-Context E2E Test Suite
 *
 * ## Multi-Context Pattern
 *
 * Two independent browser contexts observe the same snake draft page simultaneously.
 * When the picker (Context A) confirms a player selection, the backend broadcasts a
 * STOMP event on `/topic/draft/{draftId}/snake`. The observer (Context B) receives
 * this event via its own WebSocket connection and reflects the update in the UI
 * (`.player-card--taken` class) WITHOUT a page reload.
 *
 * ### Why `browser.newContext()` and not `browser.newPage()`?
 *
 * `browser.newPage()` opens a new tab within the SAME browser context, sharing
 * sessionStorage, localStorage, and cookies. Both pages would share the same
 * authenticated session — impossible to be logged in as two different users.
 *
 * `browser.newContext()` creates a fully isolated browser session (separate
 * sessionStorage + localStorage + cookies). Each context has its own independent
 * login state, which is required to simulate two simultaneous user sessions.
 *
 * ### STOMP Architecture
 *
 * - Backend pick endpoint: `POST /api/games/{gameId}/draft/snake/pick`
 * - Service: `SnakeDraftService.validateAndAdvance()` → `messagingTemplate.convertAndSend()`
 * - STOMP topic: `/topic/draft/{draftId}/snake` (SockJS endpoint `/ws`)
 * - Frontend subscription: `WebSocketService.subscribeToDraft(draftId)` → `draftEvents$` Observable
 * - UI update: `SnakeDraftPageComponent` applies `.player-card--taken` on received events
 *
 * @see frontend/e2e/draft-flow.spec.ts for single-context draft flow tests
 * @see src/main/java/com/fortnite/pronos/service/draft/SnakeDraftService.java
 * @see frontend/src/app/core/services/websocket.service.ts
 */
import { APIRequestContext, BrowserContext, Page, expect, test } from '@playwright/test';

import {
  cleanupE2eGames,
  createQuickGame,
  forceLoginWithProfile,
  generateInvitationCode,
  joinWithInvitationCode,
} from './helpers/app-helpers';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const CREATOR_PROFILE = 'thibaut' as const;
const JOINER_PROFILE = 'teddy' as const;
const SUITE_PREFIX = 'E2E-WS-';

// ─── Local API type definitions ───────────────────────────────────────────────

type GameParticipantResponse = {
  userId: string;
  username: string;
};

type DraftTurnResponse = {
  draftId: string;
  region: string;
  participantId: string;
  round: number;
  pickNumber: number;
  reversed: boolean;
};

// ─── Local API helpers ────────────────────────────────────────────────────────

async function fetchGameStatus(
  request: APIRequestContext,
  gameId: string,
  username = CREATOR_PROFILE
): Promise<string> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}?user=${encodeURIComponent(username)}`,
    { headers: { 'X-Test-User': username } }
  );
  expect(response.ok()).toBeTruthy();
  const payload = (await response.json()) as { status: string };
  return payload.status;
}

/**
 * Returns the current snake turn, initializing it via API if not yet started.
 * Mirrors the same helper in draft-flow.spec.ts but is kept local to avoid
 * coupling the two test files.
 */
async function fetchCurrentTurn(
  request: APIRequestContext,
  gameId: string,
  username = CREATOR_PROFILE
): Promise<DraftTurnResponse> {
  const headers = { 'X-Test-User': username };
  const turnResponse = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/turn?region=GLOBAL`,
    { headers }
  );

  if (turnResponse.ok()) {
    const payload = (await turnResponse.json()) as { data: DraftTurnResponse };
    return payload.data;
  }

  if (turnResponse.status() === 404) {
    const initializeResponse = await request.post(
      `${BACKEND_URL}/api/games/${gameId}/draft/snake/initialize`,
      { headers }
    );
    expect(initializeResponse.ok()).toBeTruthy();
    const payload = (await initializeResponse.json()) as { data: DraftTurnResponse };
    return payload.data;
  }

  throw new Error(
    `Unable to resolve current snake turn: ${turnResponse.status()} ${turnResponse.statusText()}`
  );
}

/**
 * Resolves which of the two suite profiles (thibaut / teddy) is currently
 * the active picker, by cross-referencing the participants list with the
 * current turn's participantId.
 */
async function resolveCurrentPickerUsername(
  request: APIRequestContext,
  gameId: string
): Promise<typeof CREATOR_PROFILE | typeof JOINER_PROFILE> {
  const [participantsResp, turn] = await Promise.all([
    request.get(`${BACKEND_URL}/api/games/${gameId}/participants`, {
      headers: { 'X-Test-User': CREATOR_PROFILE },
    }),
    fetchCurrentTurn(request, gameId),
  ]);

  expect(participantsResp.ok()).toBeTruthy();
  const participantList = (await participantsResp.json()) as GameParticipantResponse[];
  const currentParticipant = participantList.find(p => p.userId === turn.participantId);

  if (!currentParticipant) {
    throw new Error(`Current participant ${turn.participantId} not found in game participants`);
  }

  const username = currentParticipant.username;
  if (username !== CREATOR_PROFILE && username !== JOINER_PROFILE) {
    throw new Error(`Unexpected picker username: ${username} (expected thibaut or teddy)`);
  }

  return username as typeof CREATOR_PROFILE | typeof JOINER_PROFILE;
}

// ─── Game setup helper (local copy using E2E-WS- prefix) ─────────────────────

/**
 * Creates a game, has teddy join, then starts the draft as thibaut.
 * Returns the gameId of the game now in DRAFTING status.
 *
 * Local copy of createStartedDraftGame from draft-flow.spec.ts, using
 * SUITE_PREFIX = 'E2E-WS-' so cleanup only touches this suite's games.
 */
async function createStartedDraftGame(
  page: Page,
  request: APIRequestContext
): Promise<string> {
  const gameName = `${SUITE_PREFIX}${Date.now()}`;

  await forceLoginWithProfile(page, CREATOR_PROFILE);
  const gameId = await createQuickGame(page, gameName);
  const invitationCode = await generateInvitationCode(page, request, CREATOR_PROFILE, gameId);

  await forceLoginWithProfile(page, JOINER_PROFILE);
  await joinWithInvitationCode(page, invitationCode);

  await expect
    .poll(() => fetchGameStatus(request, gameId, CREATOR_PROFILE), { timeout: 10_000 })
    .toBe('CREATING');

  await forceLoginWithProfile(page, CREATOR_PROFILE);
  await page.goto(`/games/${gameId}`);
  await expect(page.locator('button.start-draft-btn')).toBeEnabled({ timeout: 10_000 });
  await page.locator('button.start-draft-btn').click();
  await page
    .getByRole('dialog')
    .getByRole('button', { name: /Demarrer le draft/i })
    .click();

  await expect
    .poll(() => fetchGameStatus(request, gameId, CREATOR_PROFILE), { timeout: 10_000 })
    .toBe('DRAFTING');

  return gameId;
}

// ─── Test suite ───────────────────────────────────────────────────────────────

test.describe.serial('WS-STOMP: WebSocket STOMP multi-context propagation', () => {
  let gameId = '';
  let contextA: BrowserContext;
  let contextB: BrowserContext;
  let pageA: Page;
  let pageB: Page;
  let pickerUsername: typeof CREATOR_PROFILE | typeof JOINER_PROFILE;
  let observerUsername: typeof CREATOR_PROFILE | typeof JOINER_PROFILE;

  test.setTimeout(120_000);

  test.beforeAll(async ({ browser, request }) => {
    test.setTimeout(120_000);

    // Cleanup stale E2E-WS- games for both suite participants
    await cleanupE2eGames(request, CREATOR_PROFILE, SUITE_PREFIX);
    await cleanupE2eGames(request, JOINER_PROFILE, SUITE_PREFIX);

    // Create a fresh game with two participants in DRAFTING state
    const setupPage = await browser.newPage();
    gameId = await createStartedDraftGame(setupPage, request);
    await setupPage.close();

    // Determine who picks first (depends on snake turn order assigned at draft start)
    pickerUsername = await resolveCurrentPickerUsername(request, gameId);
    observerUsername = pickerUsername === CREATOR_PROFILE ? JOINER_PROFILE : CREATOR_PROFILE;
  });

  test.afterAll(async () => {
    await pageA?.close().catch(() => undefined);
    await pageB?.close().catch(() => undefined);
    await contextA?.close().catch(() => undefined);
    await contextB?.close().catch(() => undefined);
  });

  test(
    'WS-01: pick by Context A is reflected in Context B UI via STOMP without page reload',
    async ({ browser }) => {
      // ── Context A: the current picker ──────────────────────────────────────
      // Use browser.newContext() to get an isolated session (independent sessionStorage)
      contextA = await browser.newContext();
      pageA = await contextA.newPage();
      await forceLoginWithProfile(pageA, pickerUsername);
      await pageA.goto(`/games/${gameId}/draft/snake`);

      // ── Context B: the observer ────────────────────────────────────────────
      // Separate isolated session for the second participant
      contextB = await browser.newContext();
      pageB = await contextB.newPage();
      await forceLoginWithProfile(pageB, observerUsername);
      await pageB.goto(`/games/${gameId}/draft/snake`);

      // Both contexts must have the draft board fully rendered
      await expect(pageA.locator('#player-list')).toBeVisible({ timeout: 15_000 });
      await expect(pageB.locator('#player-list')).toBeVisible({ timeout: 15_000 });

      // M1 fix: wait for at least one player card to render in Context B before picking.
      // This ensures SnakeDraftPageComponent's ngOnInit STOMP subscription is active —
      // #player-list being visible does not guarantee the async WS handshake is complete.
      await expect(pageB.locator('.player-card').first()).toBeVisible({ timeout: 10_000 });

      // Context A must display the "your turn" badge before picking
      await expect(pageA.locator('.my-turn-badge')).toBeVisible({ timeout: 15_000 });

      // ── Context A: pick the first available player card ────────────────────
      const firstAvailableCard = pageA.locator('.player-card:not(.player-card--taken)').first();
      await expect(firstAvailableCard).toBeVisible({ timeout: 10_000 });
      const pickedPlayerName = (
        (await firstAvailableCard.locator('.player-name').textContent()) ?? ''
      ).trim();
      // M2 fix: guard against empty player name — { hasText: '' } would match anything
      expect(pickedPlayerName, 'picked player name must be non-empty').not.toBe('');

      await firstAvailableCard.click();
      await expect(pageA.locator('.confirm-zone .btn-confirm')).toBeVisible({ timeout: 10_000 });
      await pageA.locator('.confirm-zone .btn-confirm').click();

      // M3 fix: confirm Context A's pick cycle completed before polling Context B.
      // Without this, a silent confirm-click failure would appear as a WS propagation issue.
      await expect(pageA.locator('.confirm-zone')).not.toBeVisible({ timeout: 10_000 });

      // ── Context B: verify STOMP push — no reload ───────────────────────────
      // The backend broadcasts on /topic/draft/{draftId}/snake.
      // SnakeDraftPageComponent subscribes and applies .player-card--taken.
      // We poll without reloading — a .player-card--taken appearing proves WS delivery.
      await expect
        .poll(() => pageB.locator('.player-card--taken').count(), {
          timeout: 15_000,
          // L2 fix: explicit cadence for all 15s — avoid undefined behavior after array exhausts
          intervals: [500, 1_000, 2_000, 2_000, 2_000, 2_000, 2_000],
        })
        .toBeGreaterThan(0);

      // Bonus assertion: the exact picked player is marked taken in Context B
      const pickedCardInContextB = pageB
        .locator('.player-card', { hasText: pickedPlayerName })
        .first();
      await expect(pickedCardInContextB).toHaveClass(/player-card--taken/, { timeout: 5_000 });
    }
  );
});
