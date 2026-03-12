import { test, expect } from '@playwright/test';

import {
  cleanupE2eGames,
  createQuickGame,
  generateInvitationCode,
  joinWithInvitationCode,
  loginWithProfile,
  waitForInvitationCodePersistence,
} from './helpers/app-helpers';

/**
 * Game Lifecycle E2E tests - GAME-01 to GAME-05.
 * Requires: app running on port 4200, backend on port 8080.
 * Uses profile-based login with the current quick-create flow.
 *
 * Run: npm run test:e2e
 */

const GAME_CREATOR = 'teddy';
const GAME_JOINER = 'marcel';
const GAME_PREFIX = 'E2E-GL-';

test.beforeEach(async ({ request }) => {
  await cleanupE2eGames(request, GAME_CREATOR, GAME_PREFIX);
  await cleanupE2eGames(request, GAME_JOINER, GAME_PREFIX);
});

// ---------------------------------------------------------------------------
// GAME-01: Authenticated user can create a game and see it in the detail page
// ---------------------------------------------------------------------------
test('GAME-01: authenticated user can create a game and see it in the detail page', async ({
  page,
}) => {
  test.setTimeout(40_000);

  await loginWithProfile(page, GAME_CREATOR);

  const gameName = `${GAME_PREFIX}Test-${Date.now()}`;
  await createQuickGame(page, gameName);

  const bodyText = await page.locator('body').textContent();
  expect(bodyText).toContain(gameName);
});

// ---------------------------------------------------------------------------
// GAME-02: Creator can generate an invitation code from the game detail page
// ---------------------------------------------------------------------------
test('GAME-02: creator can generate an invitation code', async ({
  page,
  request,
}) => {
  test.setTimeout(40_000);

  await loginWithProfile(page, GAME_CREATOR);

  const gameName = `${GAME_PREFIX}CodeGen-${Date.now()}`;
  const gameId = await createQuickGame(page, gameName);

  const code = await generateInvitationCode(page, request, GAME_CREATOR, gameId);
  await waitForInvitationCodePersistence(request, GAME_CREATOR, gameId, code);
  expect(code.length).toBeGreaterThan(0);
  await expect(page.locator('.code-value').first()).toContainText(code, {
    timeout: 5_000,
  });
});

// ---------------------------------------------------------------------------
// GAME-03: Navigating to /games/{id} displays game details
// ---------------------------------------------------------------------------
test('GAME-03: /games/{id} displays game details', async ({ page }) => {
  test.setTimeout(40_000);

  await loginWithProfile(page, GAME_CREATOR);
  const gameName = `${GAME_PREFIX}Detail-${Date.now()}`;
  const gameId = await createQuickGame(page, gameName);
  await page.goto(`/games/${gameId}`);

  await expect(page).toHaveURL(/\/games\/[a-z0-9-]+/, { timeout: 8_000 });

  const container = page.locator('.game-detail-container, .game-content').first();
  await expect(container).toBeVisible({ timeout: 10_000 });
  await expect(page.locator('.game-header, h1').first()).toBeVisible({
    timeout: 5_000,
  });
  await expect(
    page.locator('.status-card, .game-status-section').first()
  ).toBeVisible({ timeout: 5_000 });
});

// ---------------------------------------------------------------------------
// GAME-04: Leave button is visible for participants who are not the creator
// ---------------------------------------------------------------------------
test('GAME-04: leave button is visible for non-creator participants', async ({
  browser,
  request,
}) => {
  test.setTimeout(60_000);

  const creatorContext = await browser.newContext();
  const creatorPage = await creatorContext.newPage();
  await loginWithProfile(creatorPage, GAME_CREATOR);

  const gameName = `${GAME_PREFIX}Leave-${Date.now()}`;
  const gameId = await createQuickGame(creatorPage, gameName);
  const invitationCode = await generateInvitationCode(
    creatorPage,
    request,
    GAME_CREATOR,
    gameId
  );
  await waitForInvitationCodePersistence(
    request,
    GAME_CREATOR,
    gameId,
    invitationCode
  );

  const joinerContext = await browser.newContext();
  const joinerPage = await joinerContext.newPage();

  try {
    await loginWithProfile(joinerPage, GAME_JOINER);
    await joinWithInvitationCode(joinerPage, invitationCode);
    await joinerPage.goto(`/games/${gameId}`);

    await expect(joinerPage).toHaveURL(new RegExp(`/games/${gameId}`), {
      timeout: 10_000,
    });

    const leaveBtn = joinerPage
      .locator('button:has(mat-icon:text("exit_to_app"))')
      .first();
    await expect(leaveBtn).toBeVisible({ timeout: 8_000 });
  } finally {
    await creatorContext.close();
    await joinerContext.close();
  }
});

// ---------------------------------------------------------------------------
// GAME-05: Advanced options panel opens from the quick-create page
// ---------------------------------------------------------------------------
test('GAME-05: advanced create options open inline from the quick-create page', async ({
  page,
}) => {
  test.setTimeout(25_000);

  await loginWithProfile(page, GAME_CREATOR);
  await page.goto('/games/create');

  await expect(page.locator('.ultra-fast-create')).toBeVisible({
    timeout: 10_000,
  });

  const advancedBtn = page.locator('button.show-advanced').first();
  await expect(advancedBtn).toBeVisible({ timeout: 5_000 });
  await advancedBtn.click();

  await expect(page.locator('.advanced-panel')).toBeVisible({ timeout: 5_000 });
  await expect(page.locator('.advanced-panel mat-select').first()).toBeVisible({
    timeout: 5_000,
  });
});
