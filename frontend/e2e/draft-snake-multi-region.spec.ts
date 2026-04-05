import { APIRequestContext, BrowserContext, Page, expect, test } from '@playwright/test';

import { forceLoginWithProfile } from './helpers/app-helpers';
import { softDeleteLocalGamesByPrefix } from './helpers/local-db-helpers';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const SUITE_PREFIX = 'E2E-DMR-';
const PLACEHOLDER_USER_ID = '00000000-0000-0000-0000-000000000000';
const CREATOR_USERNAME: Username = 'teddy';
const SECOND_USERNAME: Username = 'thibaut';

type Username = 'thibaut' | 'teddy';

interface GameApiDto {
  id: string;
}

interface SnakeTurnDto {
  region: string;
  participantId: string;
}

interface ApiEnvelope<T> {
  data: T;
}

interface ParticipantUserDto {
  userId: string;
  username: string;
}

interface GameDetailParticipantDto {
  selectedPlayers?: { id?: string | null }[] | null;
}

interface GameDetailDto {
  regions?: string[];
  participants?: GameDetailParticipantDto[];
}

function authHeaders(username: Username): Record<string, string> {
  return { 'X-Test-User': username };
}

function jsonAuthHeaders(username: Username): Record<string, string> {
  return { ...authHeaders(username), 'Content-Type': 'application/json' };
}

async function createGame(
  request: APIRequestContext,
  gameName: string
): Promise<{ gameId: string; configuredRegions: string[] }> {
  const response = await request.post(`${BACKEND_URL}/api/games?user=${CREATOR_USERNAME}`, {
    headers: jsonAuthHeaders(CREATOR_USERNAME),
    data: {
      name: gameName,
      maxParticipants: 3,
      description: 'snake multi-region e2e fixture',
      draftMode: 'SNAKE',
      teamSize: 3,
      tranchesEnabled: false,
      regionRules: {
        EU: 1,
        NAW: 1,
        ASIA: 1,
      },
    },
  });

  if (!response.ok()) {
    throw new Error(`createGame failed: ${response.status()} ${await response.text()}`);
  }

  const payload = (await response.json()) as GameApiDto;
  const detail = await fetchGameDetail(request, payload.id);
  return { gameId: payload.id, configuredRegions: detail.regions ?? [] };
}

async function joinGame(
  request: APIRequestContext,
  username: Username,
  gameId: string
): Promise<void> {
  const response = await request.post(`${BACKEND_URL}/api/games/join?user=${username}`, {
    headers: jsonAuthHeaders(username),
    data: { gameId, userId: PLACEHOLDER_USER_ID },
  });

  if (!response.ok()) {
    throw new Error(`joinGame failed for ${username}: ${response.status()} ${await response.text()}`);
  }
}

async function startDraft(request: APIRequestContext, gameId: string): Promise<void> {
  const response = await request.post(`${BACKEND_URL}/api/games/${gameId}/start-draft?user=${CREATOR_USERNAME}`, {
    headers: authHeaders(CREATOR_USERNAME),
  });

  if (!response.ok()) {
    throw new Error(`startDraft failed: ${response.status()} ${await response.text()}`);
  }
}

async function initializeSnakeCursors(
  request: APIRequestContext,
  gameId: string
): Promise<SnakeTurnDto> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/initialize?user=${CREATOR_USERNAME}`,
    { headers: authHeaders(CREATOR_USERNAME) }
  );

  if (!response.ok()) {
    throw new Error(`initializeSnakeCursors failed: ${response.status()} ${await response.text()}`);
  }

  const payload = (await response.json()) as ApiEnvelope<SnakeTurnDto>;
  return payload.data;
}

async function fetchCurrentTurn(
  request: APIRequestContext,
  gameId: string,
  region: string
): Promise<SnakeTurnDto> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/turn?region=${region}`,
    { headers: authHeaders(CREATOR_USERNAME) }
  );

  if (!response.ok()) {
    throw new Error(`fetchCurrentTurn failed: ${response.status()} ${await response.text()}`);
  }

  const payload = (await response.json()) as ApiEnvelope<SnakeTurnDto>;
  return payload.data;
}

async function fetchParticipants(
  request: APIRequestContext,
  gameId: string
): Promise<ParticipantUserDto[]> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}/participants`, {
    headers: authHeaders(CREATOR_USERNAME),
  });

  if (!response.ok()) {
    throw new Error(`fetchParticipants failed: ${response.status()} ${await response.text()}`);
  }

  return (await response.json()) as ParticipantUserDto[];
}

async function fetchGameDetail(
  request: APIRequestContext,
  gameId: string
): Promise<GameDetailDto> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}/details`, {
    headers: authHeaders(CREATOR_USERNAME),
  });

  if (!response.ok()) {
    throw new Error(`fetchGameDetail failed: ${response.status()} ${await response.text()}`);
  }

  return (await response.json()) as GameDetailDto;
}

async function resolveCurrentPickerUsername(
  request: APIRequestContext,
  gameId: string,
  region: string
): Promise<Username> {
  const [participants, turn] = await Promise.all([
    fetchParticipants(request, gameId),
    fetchCurrentTurn(request, gameId, region),
  ]);

  const current = participants.find(participant => participant.userId === turn.participantId);
  if (!current) {
    throw new Error(`Participant ${turn.participantId} not found for region ${region}`);
  }

  if (current.username !== 'thibaut' && current.username !== 'teddy') {
    throw new Error(`Unexpected picker username: ${current.username}`);
  }

  return current.username as Username;
}

function totalPicks(detail: GameDetailDto): number {
  return detail.participants?.reduce((sum, participant) => sum + (participant.selectedPlayers?.length ?? 0), 0) ?? 0;
}

async function openDraftPage(page: Page, username: Username, gameId: string): Promise<void> {
  await forceLoginWithProfile(page, username);
  await page.goto(`/games/${gameId}/draft/snake`);
  await expect(page.locator('body')).toBeVisible({ timeout: 15_000 });
  await expect(page.locator('.region-switcher')).toBeVisible({ timeout: 15_000 });
}

async function switchToRegion(page: Page, region: string): Promise<void> {
  const button = page.locator('.region-switcher .region-chip').filter({ hasText: region }).first();
  await expect(button).toBeVisible({ timeout: 10_000 });

  const isActive = (await button.getAttribute('aria-pressed')) === 'true';
  if (!isActive) {
    await button.click();
  }

  await expect(page.locator('.current-region-label')).toContainText(region, { timeout: 10_000 });
}

async function pickFirstAvailablePlayer(page: Page): Promise<void> {
  const playerList = page.locator('#player-list');
  await expect(playerList).toBeVisible({ timeout: 10_000 });

  const availableCard = page.locator('app-player-card .player-card:not(.player-card--taken)').first();
  await expect(availableCard).toBeVisible({ timeout: 10_000 });
  await availableCard.click();

  const confirmButton = page.locator('.confirm-zone .btn-confirm');
  await expect(confirmButton).toBeVisible({ timeout: 10_000 });
  await confirmButton.click();
  await expect(confirmButton).not.toBeVisible({ timeout: 15_000 });
}

test.describe.serial('Snake draft multi-region UI flow', () => {
  let gameId = '';
  let regionOrder: string[] = [];
  let thibautContext: BrowserContext | undefined;
  let teddyContext: BrowserContext | undefined;
  let thibautPage: Page | undefined;
  let teddyPage: Page | undefined;

  test.beforeAll(async ({ browser, request }) => {
    softDeleteLocalGamesByPrefix(SUITE_PREFIX);

    const gameName = `${SUITE_PREFIX}${Date.now()}`;
    const created = await createGame(request, gameName);
    gameId = created.gameId;

      await joinGame(request, SECOND_USERNAME, gameId);
      await startDraft(request, gameId);
      const firstTurn = await initializeSnakeCursors(request, gameId);

    regionOrder = [
      firstTurn.region,
      ...created.configuredRegions.filter(region => region !== firstTurn.region),
    ];

    thibautContext = await browser.newContext();
    teddyContext = await browser.newContext();
    thibautPage = await thibautContext.newPage();
    teddyPage = await teddyContext.newPage();

    await openDraftPage(thibautPage, SECOND_USERNAME, gameId);
    await openDraftPage(teddyPage, CREATOR_USERNAME, gameId);
  });

  test.afterAll(async () => {
    await Promise.all([
      thibautContext?.close().catch(() => {}),
      teddyContext?.close().catch(() => {}),
    ]);
    softDeleteLocalGamesByPrefix(SUITE_PREFIX);
  });

  test('DMR-01: 2 users can play 6 UI picks across 3 configured regions without crash', async ({
    request,
  }) => {
    test.setTimeout(120_000);

    if (!thibautPage || !teddyPage) {
      throw new Error('Draft pages are not initialized');
    }

    const pages: Record<Username, Page> = {
      thibaut: thibautPage,
      teddy: teddyPage,
    };

    let expectedPickCount = 0;

    for (const region of regionOrder) {
      await Promise.all([switchToRegion(thibautPage, region), switchToRegion(teddyPage, region)]);

      for (let pickInRegion = 0; pickInRegion < 2; pickInRegion += 1) {
        const picker = await resolveCurrentPickerUsername(request, gameId, region);
        const waiting = picker === 'thibaut' ? 'teddy' : 'thibaut';

        await expect(pages[picker].locator('.my-turn-badge')).toBeVisible({ timeout: 10_000 });
        await pickFirstAvailablePlayer(pages[picker]);

        expectedPickCount += 1;

        await expect
          .poll(async () => totalPicks(await fetchGameDetail(request, gameId)), {
            timeout: 15_000,
            intervals: [500, 1_000, 2_000],
          })
          .toBe(expectedPickCount);

        if (pickInRegion === 0) {
          await expect(pages[waiting].locator('.current-region-label')).toContainText(region, {
            timeout: 10_000,
          });
          await expect(pages[waiting].locator('.my-turn-badge')).toBeVisible({ timeout: 10_000 });
        }
      }
    }

    const detail = await fetchGameDetail(request, gameId);
    const picksPerParticipant =
      detail.participants?.map(participant => participant.selectedPlayers?.length ?? 0) ?? [];

    expect(totalPicks(detail)).toBe(6);
    expect(picksPerParticipant).toEqual([3, 3]);
  });
});
