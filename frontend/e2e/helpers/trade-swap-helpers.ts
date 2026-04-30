import { APIRequestContext, APIResponse, expect } from '@playwright/test';

import { cleanupE2eGames } from './app-helpers';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';

export { softDeleteLocalGamesByPrefix } from './local-db-helpers';

export const DEFAULT_TRADE_SWAP_PREFIX = 'E2E-TS-';
const TRADE_RELATED_GAME_PREFIXES = [DEFAULT_TRADE_SWAP_PREFIX, 'E2E-FF-', 'E2E-GL-'] as const;
const PLACEHOLDER_USER_ID = '00000000-0000-0000-0000-000000000000';
const DRAFT_REGION = 'EU';

export const TRADE_SWAP_PLAYER_IDS = {
  adminOpening: '10000000-0000-0000-0000-000000000003',
  thibautOpening: '10000000-0000-0000-0000-000000000001',
  teddyOpening: '10000000-0000-0000-0000-000000000002',
  happySwapIn: '10000000-0000-0000-0000-000000000005',
  invalidSwapIn: '10000000-0000-0000-0000-000000000004',
} as const;

const CREATOR_USERNAME = 'admin';
const TRADER_A_USERNAME = 'thibaut';
const TRADER_B_USERNAME = 'teddy';
const SUPPORTING_USERNAME = 'marcel';

type Username =
  | typeof CREATOR_USERNAME
  | typeof TRADER_A_USERNAME
  | typeof TRADER_B_USERNAME
  | typeof SUPPORTING_USERNAME;

interface GameApiDto {
  id: string;
  invitationCode?: string | null;
}

interface SnakeTurnDto {
  draftId: string;
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

interface SelectedPlayerDto {
  playerId: string;
  nickname: string;
}

interface GameDetailParticipantDto {
  participantId: string;
  username: string;
  selectedPlayers?: SelectedPlayerDto[] | null;
}

export interface GameDetailDto {
  gameId: string;
  draftInfo?: {
    draftId: string;
  } | null;
  participants: GameDetailParticipantDto[];
}

export interface DraftAuditEntryDto {
  id: string;
  type: 'SWAP_SOLO' | 'TRADE_PROPOSED' | 'TRADE_ACCEPTED' | 'TRADE_REJECTED';
  playerOutId: string;
  playerInId: string;
}

export interface DraftTradeProposalDto {
  tradeId: string;
  proposerParticipantId: string;
  targetParticipantId: string;
  playerFromProposerId: string;
  playerFromTargetId: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
}

export interface SwapSoloDto {
  draftId: string;
  participantId: string;
  playerOutId: string;
  playerInId: string;
}

export interface TradeReadyGameFixture {
  gameId: string;
  draftId: string;
  participants: {
    admin: string;
    thibaut: string;
    teddy: string;
  };
  players: {
    admin?: string;
    thibaut: string;
    teddy: string;
  };
}

export interface ErrorResponseDto {
  code?: string;
  message?: string;
}

export async function prepareTradeReadyGame(
  request: APIRequestContext,
  scenario: string,
  prefix = DEFAULT_TRADE_SWAP_PREFIX
): Promise<TradeReadyGameFixture> {
  const gameId = await createGame(request, `${prefix}${scenario}-${Date.now()}`);
  await joinGameById(request, TRADER_A_USERNAME, gameId);
  await joinGameById(request, TRADER_B_USERNAME, gameId);
  await startDraft(request, gameId, CREATOR_USERNAME);
  await seedOpeningRosters(request, gameId);

  const detail = await fetchGameDetail(request, gameId, CREATOR_USERNAME);
  const admin = findParticipant(detail, CREATOR_USERNAME);
  const thibaut = findParticipant(detail, TRADER_A_USERNAME);
  const teddy = findParticipant(detail, TRADER_B_USERNAME);
  const draftId = detail.draftInfo?.draftId;
  if (!draftId) {
    throw new Error(`Draft was started for ${gameId} but game details returned no draftId`);
  }

  return {
    gameId,
    draftId,
    participants: {
      admin: admin.participantId,
      thibaut: thibaut.participantId,
      teddy: teddy.participantId,
    },
    players: {
      admin: admin.selectedPlayers?.[0]?.playerId,
      thibaut: firstSelectedPlayerId(thibaut),
      teddy: firstSelectedPlayerId(teddy),
    },
  };
}

export async function cleanupTradeFixtureUsers(request: APIRequestContext): Promise<void> {
  const usernames: Username[] = [
    CREATOR_USERNAME,
    TRADER_A_USERNAME,
    TRADER_B_USERNAME,
    SUPPORTING_USERNAME,
  ];

  for (const username of usernames) {
    for (const prefix of TRADE_RELATED_GAME_PREFIXES) {
      await cleanupE2eGames(request, username, prefix);
    }
  }
}

export async function fetchGameDetail(
  request: APIRequestContext,
  gameId: string,
  username: Username
): Promise<GameDetailDto> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}/details`, {
    headers: authHeaders(username),
  });

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as GameDetailDto;
}

export async function fetchDraftAudit(
  request: APIRequestContext,
  gameId: string,
  username: Username
): Promise<DraftAuditEntryDto[]> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}/draft/audit`, {
    headers: authHeaders(username),
  });

  expect(response.ok()).toBeTruthy();
  const payload = (await response.json()) as DraftAuditEntryDto[] | DraftAuditEntryDto;
  return Array.isArray(payload) ? payload : [payload];
}

export async function postSwapSolo(
  request: APIRequestContext,
  gameId: string,
  username: Username,
  playerOutId: string,
  playerInId: string
): Promise<APIResponse> {
  return request.post(`${BACKEND_URL}/api/games/${gameId}/draft/swap-solo?user=${username}`, {
    headers: jsonAuthHeaders(username),
    data: { playerOutId, playerInId },
  });
}

export async function executeSwapSolo(
  request: APIRequestContext,
  gameId: string,
  username: Username,
  playerOutId: string,
  playerInId: string
): Promise<SwapSoloDto> {
  const response = await postSwapSolo(request, gameId, username, playerOutId, playerInId);

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as SwapSoloDto;
}

export async function proposeTrade(
  request: APIRequestContext,
  gameId: string,
  username: Username,
  targetParticipantId: string,
  playerFromProposerId: string,
  playerFromTargetId: string
): Promise<DraftTradeProposalDto> {
  const response = await request.post(`${BACKEND_URL}/api/games/${gameId}/draft/trade?user=${username}`, {
    headers: jsonAuthHeaders(username),
    data: { targetParticipantId, playerFromProposerId, playerFromTargetId },
  });

  expect(response.status()).toBe(201);
  return (await response.json()) as DraftTradeProposalDto;
}

export async function acceptTrade(
  request: APIRequestContext,
  gameId: string,
  username: Username,
  tradeId: string
): Promise<DraftTradeProposalDto> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/trade/${tradeId}/accept?user=${username}`,
    { headers: authHeaders(username) }
  );

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as DraftTradeProposalDto;
}

export async function rejectTrade(
  request: APIRequestContext,
  gameId: string,
  username: Username,
  tradeId: string
): Promise<DraftTradeProposalDto> {
  const response = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/trade/${tradeId}/reject?user=${username}`,
    { headers: authHeaders(username) }
  );

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as DraftTradeProposalDto;
}

function authHeaders(username: Username): Record<string, string> {
  return { 'X-Test-User': username };
}

function jsonAuthHeaders(username: Username): Record<string, string> {
  return { ...authHeaders(username), 'Content-Type': 'application/json' };
}

async function createGame(request: APIRequestContext, gameName: string): Promise<string> {
  const response = await request.post(`${BACKEND_URL}/api/games?user=${CREATOR_USERNAME}`, {
    headers: jsonAuthHeaders(CREATOR_USERNAME),
    data: {
      name: gameName,
      maxParticipants: 3,
      description: 'trade-swap e2e fixture',
      draftMode: 'SNAKE',
      teamSize: 2,
      tranchesEnabled: false,
      regionRules: { EU: 3 },
    },
  });

  expect(response.status()).toBe(201);
  const payload = (await response.json()) as GameApiDto;
  return payload.id;
}

async function joinGameById(
  request: APIRequestContext,
  username: Username,
  gameId: string
): Promise<void> {
  const response = await request.post(`${BACKEND_URL}/api/games/join?user=${username}`, {
    headers: jsonAuthHeaders(username),
    // The controller resolves the effective user from auth, but bean validation still requires a UUID.
    data: { gameId, userId: PLACEHOLDER_USER_ID },
  });

  if (!response.ok()) {
    throw new Error(
      `joinGameById failed for ${username} on ${gameId}: ${response.status()} ${await response.text()}`
    );
  }
}

async function startDraft(
  request: APIRequestContext,
  gameId: string,
  username: Username
): Promise<void> {
  const response = await request.post(`${BACKEND_URL}/api/games/${gameId}/start-draft?user=${username}`, {
    headers: authHeaders(username),
  });

  if (!response.ok()) {
    throw new Error(
      `startDraft failed for ${username} on ${gameId}: ${response.status()} ${await response.text()}`
    );
  }
}

async function seedOpeningRosters(
  request: APIRequestContext,
  gameId: string
): Promise<void> {
  const picksByUsername = new Map<Username, string>([
    [CREATOR_USERNAME, TRADE_SWAP_PLAYER_IDS.adminOpening],
    [TRADER_A_USERNAME, TRADE_SWAP_PLAYER_IDS.thibautOpening],
    [TRADER_B_USERNAME, TRADE_SWAP_PLAYER_IDS.teddyOpening],
  ]);

  for (let attempt = 0; attempt < 6; attempt += 1) {
    const detail = await fetchGameDetail(request, gameId, CREATOR_USERNAME);
    if (hasOpeningRosters(detail)) {
      return;
    }

    const currentPicker = await resolveCurrentPickerUsername(request, gameId);
    const playerId = picksByUsername.get(currentPicker);
    if (!playerId) {
      throw new Error(`No opening pick configured for ${currentPicker}`);
    }

    await submitSnakePick(request, gameId, currentPicker, playerId);
    picksByUsername.delete(currentPicker);
  }

  const detail = await fetchGameDetail(request, gameId, CREATOR_USERNAME);
  expect(hasOpeningRosters(detail)).toBeTruthy();
}

async function resolveCurrentPickerUsername(
  request: APIRequestContext,
  gameId: string
): Promise<Username> {
  const [turn, participants] = await Promise.all([
    fetchCurrentTurn(request, gameId),
    fetchParticipantUsers(request, gameId),
  ]);
  const currentPicker = participants.find(participant => participant.userId === turn.participantId);
  if (!currentPicker) {
    throw new Error(`Current picker ${turn.participantId} not found`);
  }
  return currentPicker.username as Username;
}

async function fetchCurrentTurn(
  request: APIRequestContext,
  gameId: string
): Promise<SnakeTurnDto> {
  const turnResponse = await request.get(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/turn?region=${DRAFT_REGION}`,
    { headers: authHeaders(CREATOR_USERNAME) }
  );

  if (turnResponse.ok()) {
    const payload = (await turnResponse.json()) as ApiEnvelope<SnakeTurnDto>;
    return payload.data;
  }

  expect(turnResponse.status()).toBe(404);
  const initializeResponse = await request.post(
    `${BACKEND_URL}/api/games/${gameId}/draft/snake/initialize?user=${CREATOR_USERNAME}`,
    { headers: authHeaders(CREATOR_USERNAME) }
  );

  expect(initializeResponse.status()).toBe(201);
  const payload = (await initializeResponse.json()) as ApiEnvelope<SnakeTurnDto>;
  return payload.data;
}

async function fetchParticipantUsers(
  request: APIRequestContext,
  gameId: string
): Promise<ParticipantUserDto[]> {
  const response = await request.get(`${BACKEND_URL}/api/games/${gameId}/participants`, {
    headers: authHeaders(CREATOR_USERNAME),
  });

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as ParticipantUserDto[];
}

async function submitSnakePick(
  request: APIRequestContext,
  gameId: string,
  username: Username,
  playerId: string
): Promise<void> {
  const response = await request.post(`${BACKEND_URL}/api/games/${gameId}/draft/snake/pick?user=${username}`, {
    headers: jsonAuthHeaders(username),
    data: { playerId, region: DRAFT_REGION },
  });

  expect(response.ok()).toBeTruthy();
}

function hasOpeningRosters(detail: GameDetailDto): boolean {
  return Boolean(
    findParticipant(detail, TRADER_A_USERNAME).selectedPlayers?.length &&
      findParticipant(detail, TRADER_B_USERNAME).selectedPlayers?.length
  );
}

function findParticipant(detail: GameDetailDto, username: Username): GameDetailParticipantDto {
  const participant = detail.participants.find(entry => entry.username === username);
  if (!participant) {
    throw new Error(`Participant ${username} not found in ${detail.gameId}`);
  }
  return participant;
}

function firstSelectedPlayerId(participant: GameDetailParticipantDto): string {
  const playerId = participant.selectedPlayers?.[0]?.playerId;
  if (!playerId) {
    throw new Error(`Participant ${participant.username} has no selected player`);
  }
  return playerId;
}
