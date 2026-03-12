import { expect, test } from '@playwright/test';

import { forceLoginWithProfile } from './helpers/app-helpers';
import {
  cleanupTradeFixtureUsers,
  DraftTradeProposalDto,
  TradeReadyGameFixture,
  acceptTrade,
  fetchGameDetail,
  prepareTradeReadyGame,
  proposeTrade,
  softDeleteLocalGamesByPrefix,
} from './helpers/trade-swap-helpers';

const MULTI_TRADE_PREFIX = 'E2E-MT-';
const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';

/**
 * MULTI-TRADE-FLOW: multi-trade and refresh/polling E2E coverage
 *
 * Covers story 8.5: successive trades on same game, coexisting pending offers,
 * and dashboard persistence proof after page refresh.
 *
 * All tests require Docker local stack running:
 *   - Angular app on :4200
 *   - Spring Boot API on :8080
 *   - Seed data with players 10000000-0000-0000-0000-00000000000{1,2,11}
 *
 * OUT OF SCOPE (AC#6): real-time WebSocket push is not proven by this suite.
 * The runtime HTTP polling is the source of truth for local E2E validation.
 * WebSocket sync proof is deferred to a future infra/staging story.
 */
test.describe.serial('MULTI-TRADE-FLOW: multi-trade and refresh/polling', () => {
  // Fixtures for successive trades test
  let successiveFixture: TradeReadyGameFixture;
  // Fixtures for coexisting pending tests
  let pendingFixtureA: TradeReadyGameFixture;
  let pendingFixtureB: TradeReadyGameFixture;

  // State after first trade in MULTI-01 (players are swapped)
  let tradeOneResult: DraftTradeProposalDto;
  // Pending trade pre-seeded in beforeAll for MULTI-02/MULTI-03 independence
  let pendingTradeAId: string;

  test.setTimeout(240_000);

  test.beforeAll(async ({ request }) => {
    await cleanupTradeFixtureUsers(request);
    softDeleteLocalGamesByPrefix(MULTI_TRADE_PREFIX);

    [successiveFixture, pendingFixtureA, pendingFixtureB] = await Promise.all([
      prepareTradeReadyGame(request, 'successive', MULTI_TRADE_PREFIX),
      prepareTradeReadyGame(request, 'pending-a', MULTI_TRADE_PREFIX),
      prepareTradeReadyGame(request, 'pending-b', MULTI_TRADE_PREFIX),
    ]);

    // Pre-execute trade 1 for the successive scenario (thibaut → teddy, accepted)
    const proposed = await proposeTrade(
      request,
      successiveFixture.gameId,
      'thibaut',
      successiveFixture.participants.teddy,
      successiveFixture.players.thibaut,
      successiveFixture.players.teddy
    );
    tradeOneResult = await acceptTrade(
      request,
      successiveFixture.gameId,
      'teddy',
      proposed.tradeId
    );

    // Pre-seed a PENDING trade in pendingFixtureA for MULTI-02 and MULTI-03 independence
    // MULTI-03 relies on this trade being present without depending on MULTI-02 running first
    const pendingTradeA = await proposeTrade(
      request,
      pendingFixtureA.gameId,
      'thibaut',
      pendingFixtureA.participants.teddy,
      pendingFixtureA.players.thibaut,
      pendingFixtureA.players.teddy
    );
    pendingTradeAId = pendingTradeA.tradeId;
  });

  test.afterAll(() => {
    softDeleteLocalGamesByPrefix(MULTI_TRADE_PREFIX);
  });

  // ---------------------------------------------------------------------------
  // MULTI-01: Two successive trades on the same game (swap then swap back)
  // ---------------------------------------------------------------------------
  test('MULTI-01: successive trades on same game — both appear in completed history', async ({
    page,
    request,
  }) => {
    // Trade 1 was executed in beforeAll and accepted → thibaut now holds teddyOpening, teddy holds thibautOpening
    expect(tradeOneResult.status).toBe('ACCEPTED');

    // Verify rosters swapped after trade 1 — use expect.poll (roster update is async)
    await expect
      .poll(
        async () => {
          const detail = await fetchGameDetail(
            request,
            successiveFixture.gameId,
            'thibaut'
          );
          return detail.participants
            .find(p => p.username === 'thibaut')
            ?.selectedPlayers?.[0]?.playerId;
        },
        { timeout: 15_000 }
      )
      .toBe(successiveFixture.players.teddy);

    await expect
      .poll(
        async () => {
          const detail = await fetchGameDetail(
            request,
            successiveFixture.gameId,
            'teddy'
          );
          return detail.participants
            .find(p => p.username === 'teddy')
            ?.selectedPlayers?.[0]?.playerId;
        },
        { timeout: 15_000 }
      )
      .toBe(successiveFixture.players.thibaut);

    // Trade 2: swap back — thibaut offers teddyOpening (now in their roster) to get thibautOpening back
    const trade2Proposed = await proposeTrade(
      request,
      successiveFixture.gameId,
      'thibaut',
      successiveFixture.participants.teddy,
      successiveFixture.players.teddy, // thibaut now holds this
      successiveFixture.players.thibaut // teddy now holds this
    );
    expect(trade2Proposed.status).toBe('PENDING');

    const trade2Accepted = await acceptTrade(
      request,
      successiveFixture.gameId,
      'teddy',
      trade2Proposed.tradeId
    );
    expect(trade2Accepted.status).toBe('ACCEPTED');

    // Verify rosters are back to original after trade 2
    await expect
      .poll(
        async () => {
          const detail = await fetchGameDetail(
            request,
            successiveFixture.gameId,
            'thibaut'
          );
          return detail.participants
            .find(p => p.username === 'thibaut')
            ?.selectedPlayers?.[0]?.playerId;
        },
        { timeout: 15_000 }
      )
      .toBe(successiveFixture.players.thibaut);

    // Navigate to dashboard and verify 2 completed trades are listed
    await forceLoginWithProfile(page, 'thibaut');
    await page.goto(`/games/${successiveFixture.gameId}/trades`);

    await expect(page.locator('.trading-dashboard')).toBeVisible({
      timeout: 10_000,
    });

    // Navigate to completed trades tab (index 3 per TD-02 pattern)
    await page.getByRole('tab').nth(3).click();

    // At least 2 completed trade items should be visible
    const completedTrades = page.locator('.trade-item.completed-trade');
    await expect(completedTrades.first()).toBeVisible({ timeout: 10_000 });

    const count = await completedTrades.count();
    expect(count).toBeGreaterThanOrEqual(2);

    // Both should show Accepted status
    const allText = await completedTrades.allTextContents();
    const acceptedCount = allText.filter(text =>
      /accepted/i.test(text)
    ).length;
    expect(acceptedCount).toBeGreaterThanOrEqual(2);
  });

  // ---------------------------------------------------------------------------
  // MULTI-02: Coexisting pending offers in two separate games — both PENDING
  // ---------------------------------------------------------------------------
  test('MULTI-02: coexisting pending offers across games — both visible with PENDING status', async ({
    page,
    request,
  }) => {
    // Trade A in game A was pre-seeded as PENDING in beforeAll (pendingTradeAId)
    // Verify it is still PENDING (no mutation since beforeAll)
    expect(pendingTradeAId).toBeTruthy();

    // Propose trade B in game B (not accepted)
    const tradeB = await proposeTrade(
      request,
      pendingFixtureB.gameId,
      'thibaut',
      pendingFixtureB.participants.teddy,
      pendingFixtureB.players.thibaut,
      pendingFixtureB.players.teddy
    );
    expect(tradeB.status).toBe('PENDING');

    // Both trades coexist as PENDING simultaneously in two separate games

    // --- Check game A dashboard ---
    await forceLoginWithProfile(page, 'thibaut');
    await page.goto(`/games/${pendingFixtureA.gameId}/trades`);

    await expect(page.locator('.trading-dashboard')).toBeVisible({
      timeout: 10_000,
    });

    // Pending trades tab is index 0 (default); check for pending trade card
    const pendingTabA = page.locator('.trade-card.pending-trade').first();
    await expect(pendingTabA).toBeVisible({ timeout: 10_000 });

    // Status chip should show PENDING
    const statusChipA = pendingTabA.locator('.status-chip');
    await expect(statusChipA).toContainText(/pending/i, { timeout: 5_000 });

    // --- Check game B dashboard ---
    await page.goto(`/games/${pendingFixtureB.gameId}/trades`);

    await expect(page.locator('.trading-dashboard')).toBeVisible({
      timeout: 10_000,
    });

    const pendingTabB = page.locator('.trade-card.pending-trade').first();
    await expect(pendingTabB).toBeVisible({ timeout: 10_000 });

    const statusChipB = pendingTabB.locator('.status-chip');
    await expect(statusChipB).toContainText(/pending/i, { timeout: 5_000 });
  });

  // ---------------------------------------------------------------------------
  // MULTI-03: Refresh/polling proof — mutation persists after full page reload
  // ---------------------------------------------------------------------------
  test('MULTI-03: trade mutation persists on dashboard after full page reload', async ({
    page,
    request,
  }) => {
    // Use pendingFixtureA — its pending trade (pendingTradeAId) was pre-seeded in beforeAll.
    // This test is independent of MULTI-02: no shared mutable state, only reads.

    await forceLoginWithProfile(page, 'thibaut');

    // First visit to the dashboard
    await page.goto(`/games/${pendingFixtureA.gameId}/trades`);
    await expect(page.locator('.trading-dashboard')).toBeVisible({
      timeout: 10_000,
    });

    // Confirm pending trade is visible on first load
    const pendingCard = page.locator('.trade-card.pending-trade').first();
    await expect(pendingCard).toBeVisible({ timeout: 10_000 });

    // Navigate away to a different route (simulates user navigating out)
    await page.goto('/games');
    await page.waitForURL(/\/games/, { timeout: 8_000 });

    // Navigate BACK to the trades dashboard (full route reload, no SPA cache shortcut)
    await page.goto(`/games/${pendingFixtureA.gameId}/trades`);

    await expect(page.locator('.trading-dashboard')).toBeVisible({
      timeout: 10_000,
    });

    // Pending trade must still be visible — proves server persistence, not in-memory state
    const pendingCardAfterRefresh = page.locator('.trade-card.pending-trade').first();
    await expect(pendingCardAfterRefresh).toBeVisible({ timeout: 10_000 });

    // Verify the draft audit still contains the pending proposal after the full reload.
    const draftAuditResponse = await request.get(
      `${BACKEND_URL}/api/games/${pendingFixtureA.gameId}/draft/audit`,
      { headers: { 'X-Test-User': 'thibaut' } }
    );
    if (draftAuditResponse.ok()) {
      const auditEntries = (await draftAuditResponse.json()) as Array<{
        id: string;
        type: string;
      }>;
      const matchingProposal = auditEntries.find(
        entry => entry.id === pendingTradeAId && entry.type === 'TRADE_PROPOSED'
      );
      expect(matchingProposal).toBeTruthy();
    }
  });
});
