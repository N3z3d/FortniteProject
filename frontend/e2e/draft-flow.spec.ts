import { APIRequestContext, Page, expect, test } from '@playwright/test';

import {
  cleanupE2eGames,
  createQuickGame,
  forceLoginWithProfile,
  generateInvitationCode,
  joinWithInvitationCode,
} from './helpers/app-helpers';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
const CREATOR_PROFILE = 'thibaut';
const JOINER_PROFILE = 'teddy';
const SUITE_PREFIX = 'E2E-DRAFT-82-';

type DraftTurnResponse = {
  draftId: string;
  region: string;
  participantId: string;
  round: number;
  pickNumber: number;
  reversed: boolean;
};

type GameDetailParticipant = {
  participantId: string;
  username: string;
  selectedPlayers?: Array<{ nickname: string }>;
};

type GameParticipantResponse = {
  userId: string;
  username: string;
};

type GameDetailResponse = {
  participants: GameDetailParticipant[];
};

async function fetchGameDetail(
  request: APIRequestContext,
  gameId: string,
  username = CREATOR_PROFILE
): Promise<GameDetailResponse> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}/details`, {
    headers: { 'X-Test-User': username },
  });

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as GameDetailResponse;
}

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

async function fetchGameStatus(
  request: APIRequestContext,
  gameId: string,
  username = CREATOR_PROFILE
): Promise<string> {
  const response = await request.get(
    `${BACKEND_URL}/api/games/${gameId}?user=${encodeURIComponent(username)}`,
    {
      headers: { 'X-Test-User': username },
    }
  );

  expect(response.ok()).toBeTruthy();
  const payload = (await response.json()) as { status: string };
  return payload.status;
}

async function resolveCurrentPickerUsername(
  request: APIRequestContext,
  gameId: string
): Promise<string> {
  const [participants, turn] = await Promise.all([
    request.get(`${BACKEND_URL}/api/games/${gameId}/participants`, {
      headers: { 'X-Test-User': CREATOR_PROFILE },
    }),
    fetchCurrentTurn(request, gameId),
  ]);

  expect(participants.ok()).toBeTruthy();
  const participantList = (await participants.json()) as GameParticipantResponse[];
  const currentParticipant = participantList.find(participant => participant.userId === turn.participantId);

  if (!currentParticipant) {
    throw new Error(`Current participant ${turn.participantId} not found in game detail`);
  }

  return currentParticipant.username;
}

async function fetchSelectedPlayerCount(
  request: APIRequestContext,
  gameId: string,
  username: string
): Promise<number> {
  const detail = await fetchGameDetail(request, gameId, username);
  const participant = detail.participants.find(entry => entry.username === username);
  return participant?.selectedPlayers?.length ?? 0;
}

async function fetchSelectedPlayerNames(
  request: APIRequestContext,
  gameId: string,
  username: string
): Promise<string[]> {
  const detail = await fetchGameDetail(request, gameId, username);
  const participant = detail.participants.find(entry => entry.username === username);
  return (participant?.selectedPlayers ?? []).map(player => player.nickname);
}

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

test.describe.serial('DRAFT-FLOW: dedicated deep draft suite', () => {
  let gameId = '';
  let currentPickerUsername = '';
  let pickedPlayerName = '';

  test.setTimeout(120_000);

  test.beforeAll(async ({ browser, request }) => {
    test.setTimeout(120_000);
    await cleanupE2eGames(request, CREATOR_PROFILE, SUITE_PREFIX);
    await cleanupE2eGames(request, JOINER_PROFILE, SUITE_PREFIX);

    const page = await browser.newPage();
    gameId = await createStartedDraftGame(page, request);
    await page.close();
  });

  test('DRAFT-01: creator can start the snake draft from game detail', async ({ browser }) => {
    const page = await browser.newPage();

    await forceLoginWithProfile(page, CREATOR_PROFILE);
    await page.goto(`/games/${gameId}`);
    await expect(page.locator('.draft-section button')).toBeVisible({ timeout: 10_000 });

    await page.locator('.draft-section button').click();

    await page.waitForURL(new RegExp(`/games/${gameId}/draft/snake$`), {
      timeout: 15_000,
    });
    await expect(page.locator('#player-list')).toBeVisible({ timeout: 10_000 });

    await page.close();
  });

  test('DRAFT-02: current picker can select a player on the snake board', async ({
    browser,
    request,
  }) => {
    await expect
      .poll(() => resolveCurrentPickerUsername(request, gameId), { timeout: 10_000 })
      .not.toBe('');
    currentPickerUsername = await resolveCurrentPickerUsername(request, gameId);

    const page = await browser.newPage();

    await forceLoginWithProfile(page, currentPickerUsername as 'thibaut' | 'teddy');
    await page.goto(`/games/${gameId}/draft/snake`);
    await expect(page.locator('.my-turn-badge')).toBeVisible({ timeout: 10_000 });

    const firstAvailableCard = page.locator('.player-card:not(.player-card--taken)').first();
    await expect(firstAvailableCard).toBeVisible({ timeout: 10_000 });
    pickedPlayerName =
      ((await firstAvailableCard.locator('.player-name').textContent()) ?? '').trim();

    await firstAvailableCard.click();
    await expect(page.locator('.confirm-zone .btn-confirm')).toBeVisible({ timeout: 10_000 });
    await page.locator('.confirm-zone .btn-confirm').click();

    await expect
      .poll(() => fetchSelectedPlayerCount(request, gameId, currentPickerUsername), {
        timeout: 10_000,
      })
      .toBe(1);

    await page.close();
  });

  test('DRAFT-03: a confirmed pick is reflected in the draft board state', async ({
    browser,
    request,
  }) => {
    const page = await browser.newPage();

    await forceLoginWithProfile(page, currentPickerUsername as 'thibaut' | 'teddy');
    await page.goto(`/games/${gameId}/draft/snake`);
    await expect(page.locator('#player-list')).toBeVisible({ timeout: 10_000 });

    await page.reload();
    const pickedCard = page.locator('.player-card', { hasText: pickedPlayerName }).first();
    await expect(pickedCard).toBeVisible({ timeout: 10_000 });
    await expect(pickedCard).toHaveClass(/player-card--taken/);

    await expect
      .poll(
        async () => (await fetchSelectedPlayerNames(request, gameId, currentPickerUsername)).join(','),
        {
        timeout: 10_000,
        }
      )
      .toContain(pickedPlayerName);

    await page.close();
  });
});
