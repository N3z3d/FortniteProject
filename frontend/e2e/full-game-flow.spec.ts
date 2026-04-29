import { test, expect, BrowserContext, Page } from '@playwright/test';

import {
  cleanupE2eGames,
  createQuickGame,
  generateInvitationCode,
  joinWithInvitationCode,
  loginWithProfile,
  waitForInvitationCodePersistence,
  waitForInvitationCodeRemoval,
} from './helpers/app-helpers';

const USER_A = 'marcel';
const USER_B = 'teddy';
const USER_C = 'thibaut';
const FULL_FLOW_PREFIX = 'E2E-FF-';

/**
 * FULL-FLOW: Complete game lifecycle test
 *
 * Covers: login -> create game -> generate invitation code -> second user joins
 *         via code -> game detail visibility
 *
 * Requires:
 *   - App on http://localhost:8080 (Docker) or http://localhost:4200 (dev)
 *   - Backend API healthy on the same origin or BACKEND_URL
 *   - Distinct local profiles for thibaut and marcel
 *   - Deep draft coverage lives in draft-flow.spec.ts to keep this pack rerunnable
 *
 * Run: npm run test:e2e -- --grep "FULL-FLOW"
 */

test.describe('FULL-FLOW: Complete game lifecycle', () => {
  test.describe.configure({ mode: 'serial' });
  test.setTimeout(120_000);

  let contextA: BrowserContext;
  let contextB: BrowserContext;
  let contextC: BrowserContext;
  let pageA: Page;
  let pageB: Page;
  let pageC: Page;
  let gameId: string;
  let invitationCode: string;

  test('FULL-FLOW-00: backend health check and E2E cleanup', async ({
    request,
  }) => {
    const backendUrl = process.env['BACKEND_URL'] ?? 'http://localhost:8080';
    const response = await request.get(`${backendUrl}/actuator/health`);
    expect(response.status()).toBe(200);

    const body = await response.json();
    expect(body.status).toBe('UP');

    await cleanupE2eGames(request, USER_A, FULL_FLOW_PREFIX);
    await cleanupE2eGames(request, USER_B, FULL_FLOW_PREFIX);
    await cleanupE2eGames(request, USER_C, FULL_FLOW_PREFIX);
  });

  test('FULL-FLOW-01: User A logs in', async ({ browser }) => {
    contextA = await browser.newContext();
    pageA = await contextA.newPage();

    await loginWithProfile(pageA, USER_A);

    await expect(pageA).toHaveURL(/\/games/);
    await expect(pageA.locator('.gaming-dashboard-container')).toBeVisible({
      timeout: 10_000,
    });
  });

  test('FULL-FLOW-02: User A creates a game', async ({ browser }) => {
    if (!pageA) {
      contextA = await browser.newContext();
      pageA = await contextA.newPage();
      await loginWithProfile(pageA, USER_A);
    }

    const gameName = `${FULL_FLOW_PREFIX}Game-${Date.now()}`;
    gameId = await createQuickGame(pageA, gameName);

    await expect(pageA.locator('.game-header h1')).toContainText(gameName, {
      timeout: 10_000,
    });
    expect(gameId).toMatch(/^[0-9a-f-]{36}$/);
  });

  test('FULL-FLOW-03: User A generates an invitation code', async ({
    request,
  }) => {
    if (!pageA) {
      test.fixme(
        true,
        'Depends on FULL-FLOW-02 - run the full suite together, not this test in isolation.'
      );
      return;
    }

    await pageA.goto(`/games/${gameId}`);
    await pageA.waitForSelector('.game-content', { timeout: 10_000 });

    invitationCode = await generateInvitationCode(
      pageA,
      request,
      USER_A,
      gameId
    );
    await waitForInvitationCodePersistence(request, USER_A, gameId, invitationCode);
    expect(invitationCode.length).toBeGreaterThan(0);
    expect(invitationCode).not.toBe('-');

    const codeEl = pageA.locator('span.code-value').first();
    await expect(codeEl).toContainText(invitationCode, { timeout: 5_000 });
  });

  test('FULL-FLOW-04: User B logs in in an isolated context', async ({
    browser,
  }) => {
    contextB = await browser.newContext();
    pageB = await contextB.newPage();

    await loginWithProfile(pageB, USER_B);

    await expect(pageB).toHaveURL(/\/games/);
    await expect(pageB.locator('.gaming-dashboard-container')).toBeVisible({
      timeout: 10_000,
    });
  });

  test('FULL-FLOW-05: User B joins the game with the invitation code', async () => {
    if (!pageB || !invitationCode) {
      test.fixme(
        true,
        'Depends on FULL-FLOW-03 and FULL-FLOW-04 - run the full suite together.'
      );
      return;
    }

    await joinWithInvitationCode(pageB, invitationCode);
    await pageB.goto(`/games/${gameId}`);

    await expect(pageB).toHaveURL(new RegExp(`/games/${gameId}`), {
      timeout: 10_000,
    });
    await expect(pageB.locator('.game-detail-container, .dashboard-container').first()).toBeVisible({
      timeout: 10_000,
    });
    await expect(pageB.locator('text=2/5').first()).toBeVisible({
      timeout: 10_000,
    });
  });

  test('FULL-FLOW-06: creator no longer sees the invitation code after successful join', async ({
    request,
  }) => {
    if (!pageA || !invitationCode) {
      test.fixme(
        true,
        'Depends on FULL-FLOW-03 and FULL-FLOW-05 - run the full suite together.'
      );
      return;
    }

    await waitForInvitationCodeRemoval(request, USER_A, gameId);
    await pageA.goto(`/games/${gameId}`);
    await pageA.waitForSelector('.game-content', { timeout: 10_000 });

    await expect(pageA.locator('.generate-code-btn')).toBeVisible({ timeout: 10_000 });
    await expect(pageA.locator('.delete-code-btn')).toHaveCount(0);
    await expect(pageA.locator('.expiry-chip, .expired-chip')).toHaveCount(0);
    await expect(pageA.locator('span.code-value').first()).toContainText('-', {
      timeout: 5_000,
    });
  });

  test('FULL-FLOW-07: a second user cannot reuse the consumed invitation code', async ({
    browser,
  }) => {
    if (!invitationCode) {
      test.fixme(
        true,
        'Depends on FULL-FLOW-03 and FULL-FLOW-05 - run the full suite together.'
      );
      return;
    }

    contextC = await browser.newContext();
    pageC = await contextC.newPage();

    await loginWithProfile(pageC, USER_C);
    await pageC.goto('/games/join');
    await pageC.locator('.join-game-card').first().waitFor({
      state: 'visible',
      timeout: 10_000,
    });

    const codeInput = pageC.locator('input[name="invitationCode"]').first();
    await codeInput.fill(invitationCode);
    await codeInput.press('Enter');

    const errorFeedback = pageC.locator('.join-feedback--error, [role="alert"]').first();
    await expect(errorFeedback).toBeVisible({ timeout: 10_000 });
    await expect(errorFeedback).toContainText(
      /code invalide|invalid code|partie indisponible|unavailable game|partida no disponible|partida indispon[ií]vel/i
    );
    await expect(pageC).toHaveURL(/\/games\/join$/, { timeout: 5_000 });
  });

  test.afterAll(async () => {
    if (contextA) {
      await contextA.close().catch(() => undefined);
    }
    if (contextB) {
      await contextB.close().catch(() => undefined);
    }
    if (contextC) {
      await contextC.close().catch(() => undefined);
    }
  });
});

test.describe('FULL-FLOW: Login page contract', () => {
  test('login page renders profile buttons or alternative form', async ({
    page,
  }) => {
    await page.goto('/login');
    await page.waitForSelector('.user-selection-login', { timeout: 10_000 });

    const profileBtns = page.locator('.user-profile-btn');
    const altToggle = page.locator('button.show-alternative');

    const hasBtns = (await profileBtns.count()) > 0;
    const hasToggle = await altToggle.isVisible().catch(() => false);

    expect(hasBtns || hasToggle).toBe(true);
  });

  test('login page shows game title hero', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('.game-title')).toBeVisible({ timeout: 5_000 });
    await expect(page.locator('.game-title')).toContainText(/Fortnite/i);
  });
});

test.describe('FULL-FLOW: Game creation page structure', () => {
  test('create page exposes quick-create and advanced options toggle', async ({
    browser,
  }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();

    await loginWithProfile(page, USER_A);
    await page.goto('/games/create');
    await page.waitForSelector('.ultra-fast-create', { timeout: 10_000 });

    await expect(page.locator('button.mega-create-btn')).toBeVisible();
    await expect(
      page.locator('input[formcontrolname="name"], input[formControlName="name"]')
    ).toBeVisible();

    const advancedBtn = page.locator('button.show-advanced').first();
    await expect(advancedBtn).toBeVisible({ timeout: 5_000 });
    await advancedBtn.click();

    await expect(page.locator('.advanced-panel')).toBeVisible({
      timeout: 5_000,
    });
    await expect(page.locator('.advanced-panel mat-select').first()).toBeVisible({
      timeout: 5_000,
    });

    await ctx.close();
  });

  test('join page renders code input', async ({ browser }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();

    await loginWithProfile(page, USER_A);
    await page.goto('/games/join');
    await page.waitForSelector('.join-game-card', { timeout: 10_000 });

    await expect(page.locator('input[name="invitationCode"]')).toBeVisible();
    await expect(page.locator('button.join-button[type="submit"]')).toBeVisible();

    await ctx.close();
  });
});
