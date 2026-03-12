import { expect, test } from '@playwright/test';

import { forceLoginWithProfile } from './helpers/app-helpers';
import {
  cleanupTradeFixtureUsers,
  DEFAULT_TRADE_SWAP_PREFIX,
  DraftAuditEntryDto,
  ErrorResponseDto,
  GameDetailDto,
  TRADE_SWAP_PLAYER_IDS,
  TradeReadyGameFixture,
  acceptTrade,
  executeSwapSolo,
  fetchDraftAudit,
  fetchGameDetail,
  postSwapSolo,
  prepareTradeReadyGame,
  proposeTrade,
  rejectTrade,
  softDeleteLocalGamesByPrefix,
} from './helpers/trade-swap-helpers';

test.describe.serial('TRADE-SWAP-FLOW: local runtime contract', () => {
  let swapFixture: TradeReadyGameFixture;
  let acceptFixture: TradeReadyGameFixture;
  let rejectFixture: TradeReadyGameFixture;

  test.setTimeout(180_000);

  test.beforeAll(async ({ request }) => {
    await cleanupTradeFixtureUsers(request);
    softDeleteLocalGamesByPrefix(DEFAULT_TRADE_SWAP_PREFIX);
    swapFixture = await prepareTradeReadyGame(request, 'swap');
    acceptFixture = await prepareTradeReadyGame(request, 'trade-accept');
    rejectFixture = await prepareTradeReadyGame(request, 'trade-reject');
  });

  test.afterAll(() => {
    softDeleteLocalGamesByPrefix(DEFAULT_TRADE_SWAP_PREFIX);
  });

  test('TS-01: invalid swap is rejected explicitly without mutating the roster', async ({
    request,
  }) => {
    const invalidSwapResponse = await postSwapSolo(
      request,
      swapFixture.gameId,
      'thibaut',
      swapFixture.players.thibaut,
      TRADE_SWAP_PLAYER_IDS.invalidSwapIn
    );

    expect(invalidSwapResponse.status()).toBe(400);
    const errorPayload = (await invalidSwapResponse.json()) as ErrorResponseDto;
    expect(errorPayload.code).toBe('INVALID_SWAP');
    expect(errorPayload.message).toContain('strictly worse rank');

    const detail = await fetchGameDetail(request, swapFixture.gameId, 'thibaut');
    expect(firstSelectedPlayerId(detail, 'thibaut')).toBe(swapFixture.players.thibaut);

    const auditEntries = await fetchDraftAudit(request, swapFixture.gameId, 'thibaut');
    expect(auditEntries).toHaveLength(0);
  });

  test('TS-02: swap-solo persists the roster mutation and the audit trace', async ({
    page,
    request,
  }) => {
    const swapResponse = await executeSwapSolo(
      request,
      swapFixture.gameId,
      'thibaut',
      swapFixture.players.thibaut,
      TRADE_SWAP_PLAYER_IDS.happySwapIn
    );

    expect(swapResponse.playerInId).toBe(TRADE_SWAP_PLAYER_IDS.happySwapIn);

    await expect
      .poll(() => selectedPlayerIdsFor(request, swapFixture.gameId, 'thibaut'), {
        timeout: 15_000,
      })
      .toEqual([TRADE_SWAP_PLAYER_IDS.happySwapIn]);

    const auditEntries = await fetchDraftAudit(request, swapFixture.gameId, 'thibaut');
    expect(findAuditEntry(auditEntries, 'SWAP_SOLO')).toMatchObject({
      playerOutId: swapFixture.players.thibaut,
      playerInId: TRADE_SWAP_PLAYER_IDS.happySwapIn,
    });

    await forceLoginWithProfile(page, 'thibaut');
    await page.goto(`/games/${swapFixture.gameId}/draft/audit`);
    await expect(page.locator('.audit-page')).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('.type-chip--swap')).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('.audit-table')).toContainText(
      TRADE_SWAP_PLAYER_IDS.happySwapIn
    );
  });

  test('TS-03: trade propose then accept swaps both rosters and writes the audit', async ({
    page,
    request,
  }) => {
    const proposedTrade = await proposeTrade(
      request,
      acceptFixture.gameId,
      'thibaut',
      acceptFixture.participants.teddy,
      acceptFixture.players.thibaut,
      acceptFixture.players.teddy
    );

    expect(proposedTrade.status).toBe('PENDING');

    const acceptedTrade = await acceptTrade(
      request,
      acceptFixture.gameId,
      'teddy',
      proposedTrade.tradeId
    );

    expect(acceptedTrade.status).toBe('ACCEPTED');

    await expect
      .poll(() => selectedPlayerIdsFor(request, acceptFixture.gameId, 'thibaut'), {
        timeout: 15_000,
      })
      .toEqual([acceptFixture.players.teddy]);
    await expect
      .poll(() => selectedPlayerIdsFor(request, acceptFixture.gameId, 'teddy'), {
        timeout: 15_000,
      })
      .toEqual([acceptFixture.players.thibaut]);

    const auditEntries = await fetchDraftAudit(request, acceptFixture.gameId, 'teddy');
    expect(findAuditEntry(auditEntries, 'TRADE_PROPOSED')).toMatchObject({
      playerOutId: acceptFixture.players.thibaut,
      playerInId: acceptFixture.players.teddy,
    });
    expect(findAuditEntry(auditEntries, 'TRADE_ACCEPTED')).toMatchObject({
      playerOutId: acceptFixture.players.thibaut,
      playerInId: acceptFixture.players.teddy,
    });

    await forceLoginWithProfile(page, 'teddy');
    await page.goto(`/games/${acceptFixture.gameId}/draft/audit`);
    await expect(page.locator('.audit-page')).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('.type-chip--trade-accepted')).toBeVisible({
      timeout: 10_000,
    });
  });

  test('TS-04: trade propose then reject keeps both rosters unchanged and writes the audit', async ({
    page,
    request,
  }) => {
    const proposedTrade = await proposeTrade(
      request,
      rejectFixture.gameId,
      'thibaut',
      rejectFixture.participants.teddy,
      rejectFixture.players.thibaut,
      rejectFixture.players.teddy
    );

    expect(proposedTrade.status).toBe('PENDING');

    const rejectedTrade = await rejectTrade(
      request,
      rejectFixture.gameId,
      'teddy',
      proposedTrade.tradeId
    );

    expect(rejectedTrade.status).toBe('REJECTED');

    await expect
      .poll(() => selectedPlayerIdsFor(request, rejectFixture.gameId, 'thibaut'), {
        timeout: 15_000,
      })
      .toEqual([rejectFixture.players.thibaut]);
    await expect
      .poll(() => selectedPlayerIdsFor(request, rejectFixture.gameId, 'teddy'), {
        timeout: 15_000,
      })
      .toEqual([rejectFixture.players.teddy]);

    const auditEntries = await fetchDraftAudit(request, rejectFixture.gameId, 'teddy');
    expect(findAuditEntry(auditEntries, 'TRADE_PROPOSED')).toMatchObject({
      playerOutId: rejectFixture.players.thibaut,
      playerInId: rejectFixture.players.teddy,
    });
    expect(findAuditEntry(auditEntries, 'TRADE_REJECTED')).toMatchObject({
      playerOutId: rejectFixture.players.thibaut,
      playerInId: rejectFixture.players.teddy,
    });

    await forceLoginWithProfile(page, 'teddy');
    await page.goto(`/games/${rejectFixture.gameId}/draft/audit`);
    await expect(page.locator('.audit-page')).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('.type-chip--trade-rejected')).toBeVisible({
      timeout: 10_000,
    });
  });
});

async function selectedPlayerIdsFor(
  request: Parameters<typeof fetchGameDetail>[0],
  gameId: string,
  username: 'thibaut' | 'teddy'
): Promise<string[]> {
  const detail = await fetchGameDetail(request, gameId, username);
  const participant = findParticipant(detail, username);
  return (participant.selectedPlayers ?? []).map(player => player.playerId);
}

function firstSelectedPlayerId(
  detail: GameDetailDto,
  username: 'thibaut' | 'teddy'
): string {
  const playerId = findParticipant(detail, username).selectedPlayers?.[0]?.playerId;
  if (!playerId) {
    throw new Error(`Participant ${username} has no roster entry in ${detail.gameId}`);
  }
  return playerId;
}

function findParticipant(
  detail: GameDetailDto,
  username: 'thibaut' | 'teddy'
): GameDetailDto['participants'][number] {
  const participant = detail.participants.find(entry => entry.username === username);
  if (!participant) {
    throw new Error(`Participant ${username} not found in ${detail.gameId}`);
  }
  return participant;
}

function findAuditEntry(
  entries: DraftAuditEntryDto[],
  type: DraftAuditEntryDto['type']
): DraftAuditEntryDto {
  const entry = entries.find(candidate => candidate.type === type);
  if (!entry) {
    throw new Error(`Audit entry ${type} not found`);
  }
  return entry;
}
