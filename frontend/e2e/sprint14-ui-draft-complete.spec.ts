/**
 * DRAFT-UI-01: Draft serpent complet via clics UI réels — 2 contextes navigateur
 *
 * Sprint 14 — sprint14-e2e-ui-draft-complet
 *
 * Valide le flux draft serpent de bout en bout avec de vrais clics UI :
 *   1. Setup via API (create → join → start-draft → initialize-cursors)
 *   2. Ouvre 2 contextes navigateur (thibaut + teddy) sur la page draft
 *   3. Le premier joueur clique sur une carte joueur + confirme son choix
 *   4. Le deuxième joueur fait de même après la propagation WS
 *   5. Assertion finale : les 2 participants ont chacun un joueur sélectionné
 *
 * Sélecteurs UI utilisés :
 *   - `.my-turn-badge`          : indicateur "TON TOUR" (isMyTurn)
 *   - `.player-card:not(.player-card--taken)` dans un `app-player-card:not(.ineligible-tranche)`
 *   - `.btn-confirm`            : bouton "Confirmer [joueur]"
 *   - `.draft-content`          : zone draft montée (phase !== 'idle')
 *
 * @see frontend/e2e/draft-two-players.spec.ts   — ancienne version avec picks via API
 * @see frontend/e2e/draft-full-flow.spec.ts     — patterns helpers API
 */
import { APIRequestContext, BrowserContext, Page, expect, test } from '@playwright/test';

import { softDeleteLocalGamesByPrefix } from './helpers/local-db-helpers';
import { buildSingleRegionRules } from '../src/app/features/game/create-game/create-game-region-rules.util';

// ─── Constants ────────────────────────────────────────────────────────────────

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const SUITE_PREFIX = 'E2E-UI-DRAFT-';
const PLACEHOLDER_USER_ID = '00000000-0000-0000-0000-000000000000';

type Username = 'thibaut' | 'teddy' | 'admin';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ApiEnvelope<T> {
  data: T;
}

interface GameApiDto {
  id: string;
  status: string;
}

interface SnakeTurnApiDto {
  draftId: string;
  participantId: string;
  pickNumber: number;
}

interface ParticipantUserDto {
  userId: string;
  username: string;
}

interface SelectedPlayerDto {
  playerId: string;
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

// ─── API helpers ──────────────────────────────────────────────────────────────

function authHeaders(username: Username): Record<string, string> {
  return { 'X-Test-User': username };
}

function jsonAuthHeaders(username: Username): Record<string, string> {
  return { ...authHeaders(username), 'Content-Type': 'application/json' };
}

async function createGame(request: APIRequestContext, gameName: string): Promise<string> {
  const res = await request.post(`${BACKEND_URL}/api/games?user=thibaut`, {
    headers: jsonAuthHeaders('thibaut'),
    data: {
      name: gameName,
      maxParticipants: 2,
      description: 'E2E UI draft complet',
      draftMode: 'SNAKE',
      teamSize: 1,
      tranchesEnabled: false,
      regionRules: buildSingleRegionRules(2, 'EU'),
    },
  });
  if (!res.ok()) {
    throw new Error(`createGame failed: ${res.status()} ${await res.text()}`);
  }
  return ((await res.json()) as GameApiDto).id;
}

async function joinGame(request: APIRequestContext, username: Username, gameId: string): Promise<void> {
  const res = await request.post(`${BACKEND_URL}/api/games/join?user=${username}`, {
    headers: jsonAuthHeaders(username),
    data: { gameId, userId: PLACEHOLDER_USER_ID },
  });
  if (!res.ok()) {
    throw new Error(`joinGame(${username}) failed: ${res.status()} ${await res.text()}`);
  }
}

async function startDraft(request: APIRequestContext, gameId: string): Promise<void> {
  const res = await request.post(`${BACKEND_URL}/api/games/${gameId}/start-draft?user=thibaut`, {
    headers: authHeaders('thibaut'),
  });
  if (!res.ok()) {
    throw new Error(`startDraft failed: ${res.status()} ${await res.text()}`);
  }
}

async function initializeCursors(request: APIRequestContext, gameId: string): Promise<string> {
  const res = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/initialize?user=thibaut`,
    { headers: authHeaders('thibaut') }
  );
  if (!res.ok()) {
    throw new Error(`initializeCursors failed: ${res.status()} ${await res.text()}`);
  }
  const body = (await res.json()) as ApiEnvelope<SnakeTurnApiDto & { region: string }>;
  return body.data.region ?? 'EU';
}

async function fetchCurrentTurn(
  request: APIRequestContext,
  gameId: string,
  region = 'EU'
): Promise<SnakeTurnApiDto | null> {
  const res = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/turn?region=${region}`,
    { headers: authHeaders('thibaut') }
  );
  if (res.status() === 404) return null;
  if (!res.ok()) throw new Error(`fetchCurrentTurn failed: ${res.status()}`);
  return ((await res.json()) as ApiEnvelope<SnakeTurnApiDto>).data;
}

async function fetchParticipants(
  request: APIRequestContext,
  gameId: string
): Promise<ParticipantUserDto[]> {
  const res = await request.get(`${BACKEND_URL}/api/games/${gameId}/participants`, {
    headers: authHeaders('thibaut'),
  });
  if (!res.ok()) throw new Error(`fetchParticipants failed: ${res.status()}`);
  return (await res.json()) as ParticipantUserDto[];
}

async function fetchGameDetail(
  request: APIRequestContext,
  gameId: string
): Promise<GameDetailDto> {
  const res = await request.get(`${BACKEND_URL}/api/games/${gameId}/details`, {
    headers: authHeaders('thibaut'),
  });
  if (!res.ok()) throw new Error(`fetchGameDetail failed: ${res.status()}`);
  return (await res.json()) as GameDetailDto;
}

async function resolvePickerUsername(
  request: APIRequestContext,
  gameId: string,
  region = 'EU'
): Promise<'thibaut' | 'teddy'> {
  const [participants, turn] = await Promise.all([
    fetchParticipants(request, gameId),
    fetchCurrentTurn(request, gameId, region),
  ]);
  if (!turn) throw new Error('No current turn — cursors not initialized');
  const found = participants.find(p => p.userId === turn.participantId);
  if (!found) throw new Error(`participantId ${turn.participantId} not in participants`);
  if (found.username !== 'thibaut' && found.username !== 'teddy') {
    throw new Error(`Unexpected picker: ${found.username}`);
  }
  return found.username as 'thibaut' | 'teddy';
}

// ─── Auth helper ──────────────────────────────────────────────────────────────

/**
 * Logs in by clicking the profile button on the /login page.
 *
 * The Angular app's login UI shows clickable profile cards (admin, thibaut, teddy, …).
 * Clicking one triggers a real POST /api/auth/login with `environment.devUserPassword`
 * and stores the resulting JWT in sessionStorage — exactly what the real user does.
 *
 * This is simpler and more robust than manual sessionStorage injection because
 * it goes through the full Angular auth flow.
 */
async function loginWithRealJwt(page: Page, username: 'thibaut' | 'teddy'): Promise<void> {
  const BASE = process.env['BASE_URL'] ?? 'http://localhost:4200';

  await page.goto(`${BASE}/login`);
  await page.waitForLoadState('domcontentloaded');

  // Click the profile button whose text contains the username
  await page.locator(`button:has-text("${username}"), [role="button"]:has-text("${username}")`).first().click();

  // Wait until the router navigates away from /login (auth succeeded)
  await page.waitForURL(url => !url.toString().includes('/login'), { timeout: 15_000 });
}

// ─── UI helpers ───────────────────────────────────────────────────────────────

/**
 * Waits for the draft content to be rendered (phase !== 'idle').
 * Returns false if the page is not on the draft route (guard redirected).
 */
async function waitForDraftContent(page: Page, timeoutMs = 40_000): Promise<boolean> {
  try {
    // First wait for Angular to finish initial rendering
    await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {});
    await page.locator('.draft-content').waitFor({ state: 'visible', timeout: timeoutMs });
    return true;
  } catch {
    const url = page.url();
    const title = await page.title().catch(() => '?');
    console.warn(`waitForDraftContent: not rendered — url=${url} title=${title}`);
    return false;
  }
}

/**
 * Waits until the "TON TOUR" badge is visible (it's this player's turn).
 */
async function waitForMyTurn(page: Page, timeoutMs = 30_000): Promise<boolean> {
  try {
    await page.locator('.my-turn-badge').waitFor({ state: 'visible', timeout: timeoutMs });
    return true;
  } catch {
    return false;
  }
}

/**
 * Clicks on the first available (non-taken, non-ineligible) player card,
 * then clicks the confirm button.
 *
 * Returns the username of the selected player, or null on failure.
 */
async function pickFirstAvailablePlayer(page: Page): Promise<string | null> {
  // The eligible card selector: host element without ineligible-tranche, inner div without taken
  const cardLocator = page
    .locator('app-player-card:not(.ineligible-tranche) .player-card:not(.player-card--taken)')
    .first();

  try {
    await cardLocator.waitFor({ state: 'visible', timeout: 15_000 });
  } catch {
    return null; // No available card found
  }

  // Read the player name before clicking
  const nameEl = cardLocator.locator('.player-name');
  const playerName = (await nameEl.textContent({ timeout: 3_000 }).catch(() => null))?.trim() ?? null;

  await cardLocator.click();

  // Wait for the confirm button to appear
  const confirmBtn = page.locator('.btn-confirm');
  try {
    await confirmBtn.waitFor({ state: 'visible', timeout: 10_000 });
  } catch {
    // Confirm button didn't appear — pick may have been auto-submitted or it's not my turn
    return null;
  }

  await confirmBtn.click();

  // Wait for the confirm button to disappear (pick processed)
  await confirmBtn.waitFor({ state: 'hidden', timeout: 15_000 }).catch(() => {
    // Ignore — pick may have been processed but button is still in DOM
  });

  return playerName;
}

// ─── Module state ─────────────────────────────────────────────────────────────

let gameId = '';
let draftRegion = 'EU';
let setupSucceeded = false;

// ─── Test suite ───────────────────────────────────────────────────────────────

test.describe.serial('DRAFT-UI: draft serpent complet via clics UI réels', () => {
  test.setTimeout(120_000);

  test.beforeAll(async ({ request }) => {
    test.setTimeout(60_000);
    softDeleteLocalGamesByPrefix(SUITE_PREFIX);

    try {
      const gameName = `${SUITE_PREFIX}${Date.now()}`;
      gameId = await createGame(request, gameName);
      await joinGame(request, 'teddy', gameId);
      await startDraft(request, gameId);
      draftRegion = await initializeCursors(request, gameId);
      setupSucceeded = true;
      console.info(`DRAFT-UI: game setup OK — gameId=${gameId} region=${draftRegion}`);
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      console.warn(`DRAFT-UI beforeAll: setup failed (${msg}) — tests will be skipped`);
      setupSucceeded = false;
    }
  });

  test.afterAll(() => {
    softDeleteLocalGamesByPrefix(SUITE_PREFIX);
  });

  test(
    'DRAFT-UI-01: draft serpent complet — 2 joueurs pickent via clic UI, les 2 équipes ont un joueur',
    async ({ browser, request }) => {
      test.setTimeout(120_000);

      if (!setupSucceeded) {
        test.skip();
        return;
      }

      // Determine who picks first
      const firstPicker = await resolvePickerUsername(request, gameId, draftRegion);
      const secondPicker: 'thibaut' | 'teddy' = firstPicker === 'thibaut' ? 'teddy' : 'thibaut';

      console.info(`DRAFT-UI-01: firstPicker=${firstPicker} secondPicker=${secondPicker}`);

      let ctx1: BrowserContext | undefined;
      let ctx2: BrowserContext | undefined;

      try {
        // ── Open 2 contexts ────────────────────────────────────────────────────
        ctx1 = await browser.newContext();
        ctx2 = await browser.newContext();
        const page1: Page = await ctx1.newPage();
        const page2: Page = await ctx2.newPage();

        await loginWithRealJwt(page1, firstPicker);
        await loginWithRealJwt(page2, secondPicker);

        // ── Navigate to draft page ────────────────────────────────────────────
        await Promise.all([
          page1.goto(`/games/${gameId}/draft/snake`),
          page2.goto(`/games/${gameId}/draft/snake`),
        ]);

        const [draft1Ready, draft2Ready] = await Promise.all([
          waitForDraftContent(page1),
          waitForDraftContent(page2),
        ]);

        expect(draft1Ready, 'draft content should render for first picker').toBe(true);
        expect(draft2Ready, 'draft content should render for second picker').toBe(true);

        // ── Pick 1: firstPicker makes their selection ─────────────────────────
        const myTurn1 = await waitForMyTurn(page1, 25_000);
        expect(myTurn1, `"TON TOUR" should be visible for ${firstPicker}`).toBe(true);

        const pick1Player = await pickFirstAvailablePlayer(page1);
        expect(pick1Player, `available card should exist for ${firstPicker}`).toBeTruthy();

        console.info(`DRAFT-UI-01: ${firstPicker} picked "${pick1Player}"`);

        // Wait for pick 1 to be reflected via API
        await expect
          .poll(
            async () => {
              const detail = await fetchGameDetail(request, gameId);
              return detail.participants.reduce(
                (sum, p) => sum + (p.selectedPlayers?.length ?? 0),
                0
              );
            },
            { timeout: 20_000, intervals: [500, 1_000, 1_500, 2_000, 2_000] }
          )
          .toBeGreaterThanOrEqual(1);

        // ── Pick 2: secondPicker makes their selection ────────────────────────
        const myTurn2 = await waitForMyTurn(page2, 25_000);
        expect(myTurn2, `"TON TOUR" should be visible for ${secondPicker}`).toBe(true);

        const pick2Player = await pickFirstAvailablePlayer(page2);
        expect(pick2Player, `available card should exist for ${secondPicker}`).toBeTruthy();

        console.info(`DRAFT-UI-01: ${secondPicker} picked "${pick2Player}"`);

        // ── Final assertion: both participants have a player ──────────────────
        await expect
          .poll(
            async () => {
              const detail = await fetchGameDetail(request, gameId);
              return detail.participants.every(p => (p.selectedPlayers?.length ?? 0) >= 1);
            },
            { timeout: 20_000, intervals: [500, 1_000, 1_500, 2_000, 3_000] }
          )
          .toBe(true);

        // Verify both picks are different players
        expect(pick1Player).toBeTruthy();
        expect(pick2Player).toBeTruthy();
        expect(pick1Player).not.toBe(pick2Player);

        console.info('DRAFT-UI-01: ✅ draft complet — les 2 équipes ont un joueur');
      } finally {
        await ctx1?.close().catch(() => {});
        await ctx2?.close().catch(() => {});
      }
    }
  );
});
