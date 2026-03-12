import { expect, test } from '@playwright/test';

import { forceLoginWithProfile } from './helpers/app-helpers';
import {
  cleanupTradeFixtureUsers,
  DEFAULT_TRADE_SWAP_PREFIX,
  TradeReadyGameFixture,
  acceptTrade,
  prepareTradeReadyGame,
  proposeTrade,
  rejectTrade,
  softDeleteLocalGamesByPrefix,
} from './helpers/trade-swap-helpers';

const TRADE_DASHBOARD_PREFIX = `${DEFAULT_TRADE_SWAP_PREFIX}dashboard-`;

test.describe.serial('TRADE-DASHBOARD: real runtime UI', () => {
  let acceptedFixture: TradeReadyGameFixture;
  let rejectedFixture: TradeReadyGameFixture;

  test.setTimeout(180_000);

  test.beforeAll(async ({ request }) => {
    await cleanupTradeFixtureUsers(request);
    softDeleteLocalGamesByPrefix(TRADE_DASHBOARD_PREFIX);

    acceptedFixture = await prepareTradeReadyGame(request, 'ui-accept', TRADE_DASHBOARD_PREFIX);
    rejectedFixture = await prepareTradeReadyGame(request, 'ui-reject', TRADE_DASHBOARD_PREFIX);

    const acceptedTrade = await proposeTrade(
      request,
      acceptedFixture.gameId,
      'thibaut',
      acceptedFixture.participants.teddy,
      acceptedFixture.players.thibaut,
      acceptedFixture.players.teddy
    );
    await acceptTrade(request, acceptedFixture.gameId, 'teddy', acceptedTrade.tradeId);

    const rejectedTrade = await proposeTrade(
      request,
      rejectedFixture.gameId,
      'thibaut',
      rejectedFixture.participants.teddy,
      rejectedFixture.players.thibaut,
      rejectedFixture.players.teddy
    );
    await rejectTrade(request, rejectedFixture.gameId, 'teddy', rejectedTrade.tradeId);
  });

  test.afterAll(() => {
    softDeleteLocalGamesByPrefix(TRADE_DASHBOARD_PREFIX);
  });

  test('TD-01: legacy list route redirects to the canonical trades dashboard', async ({ page }) => {
    await forceLoginWithProfile(page, 'thibaut');
    await page.goto(`/games/${acceptedFixture.gameId}/trades/list`);

    await page.waitForURL(`**/games/${acceptedFixture.gameId}/trades`, { timeout: 10_000 });
    await expect(page.locator('.trading-dashboard')).toBeVisible({ timeout: 10_000 });
  });

  test('TD-02: accepted trade is visible on the real trades dashboard UI', async ({ page }) => {
    await forceLoginWithProfile(page, 'thibaut');
    await page.goto(`/games/${acceptedFixture.gameId}/trades`);

    await expect(page.locator('.trading-dashboard')).toBeVisible({ timeout: 10_000 });
    await page.getByRole('tab').nth(3).click();

    const completedTrade = page.locator('.trade-item.completed-trade').first();
    await expect(completedTrade).toBeVisible({ timeout: 10_000 });
    await expect(completedTrade).toContainText(/Accepted/i);
    await expect(completedTrade).toContainText(/teddy/i);
  });

  test('TD-03: rejected trade is visible on the real trades dashboard UI', async ({ page }) => {
    await forceLoginWithProfile(page, 'thibaut');
    await page.goto(`/games/${rejectedFixture.gameId}/trades`);

    await expect(page.locator('.trading-dashboard')).toBeVisible({ timeout: 10_000 });
    await page.getByRole('tab').nth(3).click();

    const completedTrade = page.locator('.trade-item.completed-trade').first();
    await expect(completedTrade).toBeVisible({ timeout: 10_000 });
    await expect(completedTrade).toContainText(/Rejected/i);
    await expect(completedTrade).toContainText(/teddy/i);
  });
});
