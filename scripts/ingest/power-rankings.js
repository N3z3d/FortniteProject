#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '..', '..');
const PLAYER_REGIONS = new Set(['EU', 'NAW', 'BR', 'ASIA', 'OCE', 'NAC', 'ME', 'NA']);
const PR_REGIONS = new Set(['EU', 'NAW', 'BR', 'ASIA', 'OCE', 'NAC', 'ME', 'GLOBAL']);

loadEnvFromRoot(ROOT_DIR);

const args = parseArgs(process.argv.slice(2));
const config = buildConfig(args);

if (typeof fetch !== 'function') {
  console.error('This script requires Node 18+ (global fetch).');
  process.exit(1);
}

main(config)
  .then(() => process.exit(0))
  .catch((err) => {
    console.error('Ingestion failed:', err && err.message ? err.message : err);
    process.exit(1);
  });

async function main(config) {
  let run = null;
  let totalSnapshots = 0;
  let totalScores = 0;
  let skippedSnapshots = 0;
  let ignoredGlobalSnapshots = 0;
  let cleanupSummary = null;

  try {
    if (config.cleanupGlobalOnly) {
      cleanupSummary = await cleanupGlobalOnlyPlayers(config);
      console.log(
        `Cleanup: global-only players=${cleanupSummary.players}, snapshots=${cleanupSummary.snapshots}, scores=${cleanupSummary.scores}`
      );
      if (config.cleanupOnly) {
        return;
      }
    }

    if (!config.dryRun) {
      run = await createIngestionRun(config);
    }

    const fetchResult = await fetchAllRegions(config);
    const prepared = prepareRows(fetchResult.rows, config);
    const buildResult = buildUpsertPayloads(prepared.rows, config);

    if (config.dryRun) {
      printDryRunSummary(fetchResult, prepared, buildResult);
      return;
    }

    const playerIdByNormalized = await upsertPlayers(config, buildResult.playerUpserts);
    if (config.includeGlobal) {
      await addExistingPlayersForGlobalRows(config, buildResult.snapshotRows, playerIdByNormalized);
    }
    const snapshotMap = mapSnapshots(
      buildResult.snapshotRows,
      playerIdByNormalized,
      config,
      run ? run.id : null
    );

    skippedSnapshots = snapshotMap.skipped;
    ignoredGlobalSnapshots = snapshotMap.ignoredGlobal;
    totalSnapshots = await upsertSnapshots(config, snapshotMap.rows);

    if (config.writeScores) {
      const scoreMap = mapScores(buildResult.scoreRows, playerIdByNormalized, config);
      totalScores = await upsertScores(config, scoreMap.rows);
    }

    const status = computeStatus(fetchResult.pageStats, skippedSnapshots);
    await finalizeRun(config, run, status, totalSnapshots, skippedSnapshots);

    console.log(
      `Ingestion done: status=${status}, players=${buildResult.playerUpserts.length}, snapshots=${totalSnapshots}, scores=${totalScores}, skipped=${skippedSnapshots}, ignoredGlobal=${ignoredGlobalSnapshots}`
    );
  } catch (err) {
    const message = err && err.message ? err.message : String(err);
    if (run && !config.dryRun) {
      await finalizeRun(config, run, 'FAILED', totalSnapshots, skippedSnapshots, message);
    }
    throw err;
  }
}

function buildConfig(args) {
  const supabaseUrl = process.env.SUPABASE_URL;
  const supabaseKey =
    process.env.SUPABASE_SERVICE_ROLE_KEY ||
    process.env.SUPABASE_API_KEY ||
    process.env.SUPABASE_ANON_KEY;

  if (!supabaseUrl) {
    throw new Error('SUPABASE_URL is required.');
  }
  if (!supabaseKey) {
    throw new Error('SUPABASE_SERVICE_ROLE_KEY (or SUPABASE_API_KEY) is required.');
  }

  const scrapflyKeys = parseEnvList('SCRAPFLY_KEYS');
  const scraperapiKeys = parseEnvList('SCRAPERAPI_KEYS');
  const scrapedoTokens = mergeTokens(parseEnvList('SCRAPEDO_TOKENS'), process.env.SCRAPEDO_TOKEN);
  if (!scrapflyKeys.length && !scraperapiKeys.length && !scrapedoTokens.length) {
    throw new Error('Missing SCRAPFLY_KEYS, SCRAPERAPI_KEYS, or SCRAPEDO_TOKEN.');
  }

  const includeGlobal =
    args.includeGlobal !== undefined
      ? args.includeGlobal
      : parseBool(process.env.SCRAPE_INCLUDE_GLOBAL, false);

  const globalPages = parseInt(process.env.SCRAPE_GLOBAL_PAGES || '21', 10);
  const regionPages = parseInt(process.env.SCRAPE_REGION_PAGES || '3', 10);
  const regionFilter = args.regions || process.env.SCRAPE_REGIONS || '';

  const regions = buildRegions(globalPages, regionPages, includeGlobal, regionFilter);

  const weights = {
    scrapfly: parseInt(process.env.SCRAPFLY_WEIGHT || '1', 10),
    scraperapi: parseInt(process.env.SCRAPERAPI_WEIGHT || '1', 10),
    scrapedo: parseInt(process.env.SCRAPEDO_WEIGHT || '1', 10)
  };
  if (!scrapflyKeys.length) {
    weights.scrapfly = 0;
  }
  if (!scraperapiKeys.length) {
    weights.scraperapi = 0;
  }
  if (!scrapedoTokens.length) {
    weights.scrapedo = 0;
  }
  if (weights.scrapfly + weights.scraperapi + weights.scrapedo === 0) {
    throw new Error('At least one provider must have keys.');
  }

  const snapshotDate = parseSnapshotDate(process.env.SNAPSHOT_DATE);
  const scoreStrategy = (args.scoreStrategy || process.env.SCORE_STRATEGY || 'MAX').toUpperCase();
  const writeScores =
    args.writeScores !== undefined
      ? args.writeScores
      : parseBool(process.env.INGESTION_WRITE_SCORES, true);

  return {
    supabaseUrl: supabaseUrl.replace(/\/$/, ''),
    supabaseKey,
    supabaseSchema: process.env.SUPABASE_SCHEMA || 'public',
    platform: process.env.SCRAPE_PLATFORM || 'pc',
    timeframe: process.env.SCRAPE_TIMEFRAME || 'year',
    pageSize: parseInt(process.env.SCRAPE_PAGE_SIZE || '100', 10),
    scraperapiRender: parseBool(process.env.SCRAPERAPI_RENDER, false),
    chunkSize: parseInt(process.env.SCRAPE_CHUNK_SIZE || '40', 10),
    maxAttempts: parseInt(process.env.SCRAPE_MAX_ATTEMPTS || '8', 10),
    requestTimeoutMs: parseInt(process.env.SCRAPE_TIMEOUT_MS || '20000', 10),
    debug: parseBool(process.env.SCRAPE_DEBUG, false),
    weights,
    keys: {
      scrapfly: scrapflyKeys,
      scraperapi: scraperapiKeys,
      scrapedo: scrapedoTokens
    },
    scrapedoBaseUrl: process.env.SCRAPEDO_BASE_URL || 'http://api.scrape.do/',
    regions,
    includeGlobal,
    ingestionSource: process.env.INGESTION_SOURCE || 'FORTNITE_TRACKER',
    currentSeason: parseInt(process.env.INGESTION_SEASON || '2025', 10),
    snapshotDate,
    collectedAt: new Date().toISOString(),
    dbBatchSize: parseInt(process.env.INGESTION_BATCH_SIZE || '500', 10),
    lookupBatchSize: parseInt(process.env.INGESTION_LOOKUP_BATCH_SIZE || '50', 10),
    scoreStrategy,
    writeScores,
    cleanupGlobalOnly: !!args.cleanupGlobalOnly,
    cleanupOnly: !!args.cleanupOnly,
    dryRun: !!args.dryRun
  };
}

async function fetchAllRegions(config) {
  const rows = [];
  const pageStats = [];
  for (const region of config.regions) {
    const result = await fetchRegion(region, config);
    rows.push(...result.rows);
    pageStats.push(...result.pageStats);
  }
  return { rows, pageStats };
}

async function fetchRegion(region, config) {
  let pending = [];
  for (let page = region.first; page <= region.last; page += 1) {
    pending.push({
      region: region.code,
      page,
      provider: pickPrimaryProvider(region.code, page, config),
      attempts: 0
    });
  }

  const rows = [];
  const pageStats = [];

  while (pending.length > 0) {
    const nextRound = [];
    for (let i = 0; i < pending.length; i += config.chunkSize) {
      const batch = pending.slice(i, i + config.chunkSize);
      const reqs = batch.map((item) => {
        const key = pickKeyFor(item.provider, item.region, item.page, item.attempts, config.keys);
        const targetUrl = buildTarget(item.region, item.page, config);
        const url = buildProviderUrl(item.provider, targetUrl, key, config);
        return {
          region: item.region,
          page: item.page,
          provider: item.provider,
          attempts: item.attempts + 1,
          url,
          targetUrl
        };
      });

      const responses = await fetchBatch(reqs, config);
      for (const response of responses) {
        const { meta, status, body } = response;
        const html = extractHtml(body);
        const hasTbody = html ? /<tbody[^>]*>/i.test(html) : false;
        const success = status === 200 && hasTbody;

        if (success) {
          const parsedRows = parseHtmlRows(html, meta, config);
          rows.push(...parsedRows);
        }

        pageStats.push({
          region: meta.region,
          page: meta.page,
          provider: meta.provider,
          attempts: meta.attempts,
          status,
          success
        });

        if (!success && shouldRetry(status, hasTbody, meta.attempts, config.maxAttempts)) {
          nextRound.push(scheduleRetry(meta, config));
        }
      }
    }

    if (nextRound.length === 0) {
      break;
    }

    await sleep(backoff(nextRound[0].attempts));
    pending = nextRound;
  }

  return { rows, pageStats };
}

async function fetchBatch(reqs, config) {
  const tasks = reqs.map((req) => fetchOnce(req, config));
  return Promise.all(tasks);
}

async function fetchOnce(meta, config) {
  const start = Date.now();
  let status = 0;
  let body = '';

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), config.requestTimeoutMs);
    const response = await fetch(meta.url, { signal: controller.signal, headers: buildHeaders() });
    clearTimeout(timeoutId);
    status = response.status;
    body = await response.text();
  } catch (err) {
    status = 0;
    body = '';
    if (config.debug) {
      console.warn(`Fetch error ${meta.region} p${meta.page}:`, err && err.message ? err.message : err);
    }
  }

  return {
    meta,
    status,
    body,
    durationMs: Date.now() - start
  };
}

function parseHtmlRows(html, meta, config) {
  const tBodyMatch = html.match(/<tbody[^>]*>([\s\S]*?)<\/tbody>/i);
  const trs = tBodyMatch ? (tBodyMatch[1].match(/<tr[^>]*>[\s\S]*?<\/tr>/gi) || []) : [];
  const rows = [];

  for (let i = 0; i < trs.length; i += 1) {
    const tr = trs[i];
    const rank =
      pick(tr, /leaderboard-rank[^>]*[:]?placement=["']?(\d+)/) ||
      String((meta.page - 1) * config.pageSize + i + 1);
    const player = pick(tr, /leaderboard-user__nickname[^>]*>([\s\S]*?)<\/div>/);
    const team = pick(tr, /leaderboard-team__name[^>]*>([\s\S]*?)<\/a>/);
    const rawPoints =
      pick(tr, /column--highlight[\s\S]*?<div[^>]*>\s*([\d.,\u00A0\s]+)/) ||
      pick(tr, /column--highlight[^>]*>\s*([\d.,\u00A0\s]+)\s*<\/td>/) ||
      pick(tr, /column--right[\s\S]*?<div[^>]*>\s*([\d.,\u00A0\s]+)/) ||
      pick(tr, /column--right[^>]*>\s*([\d.,\u00A0\s]+)\s*<\/td>/);
    const points = rawPoints ? rawPoints.replace(/[^\d]/g, '') : '';

    if (!rank || !player || !points) {
      continue;
    }

    rows.push({
      region: meta.region,
      page: meta.page,
      rank: parseInt(rank, 10),
      player: sanitizeText(player),
      team: sanitizeText(team),
      points: parseInt(points, 10),
      platform: config.platform,
      timeframe: config.timeframe,
      provider: meta.provider
    });
  }

  return rows;
}

function prepareRows(rawRows, config) {
  const deduped = new Map();
  let dropped = 0;
  let duplicates = 0;

  for (const row of rawRows) {
    const normalized = normalizeRow(row, config);
    if (!normalized) {
      dropped += 1;
      continue;
    }

    const key = `${normalized.region}|${normalized.playerNormalized}`;
    const existing = deduped.get(key);
    if (!existing) {
      deduped.set(key, normalized);
      continue;
    }

    if (isBetter(normalized, existing)) {
      deduped.set(key, normalized);
      duplicates += 1;
    } else {
      duplicates += 1;
    }
  }

  if (config.debug) {
    console.log(`Prepare rows: input=${rawRows.length}, clean=${deduped.size}, dropped=${dropped}, dup=${duplicates}`);
  }

  return { rows: Array.from(deduped.values()), dropped, duplicates };
}

function buildUpsertPayloads(rows, config) {
  const playerCandidates = new Map();
  const rowsByNormalized = new Map();

  for (const row of rows) {
    if (!rowsByNormalized.has(row.playerNormalized)) {
      rowsByNormalized.set(row.playerNormalized, []);
    }
    rowsByNormalized.get(row.playerNormalized).push(row);

    const playerRegion = resolvePlayerRegion(row, config);
    if (!playerRegion) {
      continue;
    }

    const existing = playerCandidates.get(row.playerNormalized);
    if (!existing) {
      playerCandidates.set(row.playerNormalized, {
        nickname: row.player,
        region: playerRegion,
        rank: row.rank,
        points: row.points,
        sourceRegion: row.region
      });
      continue;
    }

    if (existing.sourceRegion === 'GLOBAL' && row.region !== 'GLOBAL') {
      playerCandidates.set(row.playerNormalized, {
        nickname: row.player,
        region: playerRegion,
        rank: row.rank,
        points: row.points,
        sourceRegion: row.region
      });
      continue;
    }

    if (existing.sourceRegion !== 'GLOBAL' && row.region === 'GLOBAL') {
      continue;
    }

    if (isBetter(row, existing)) {
      playerCandidates.set(row.playerNormalized, {
        nickname: row.player,
        region: playerRegion,
        rank: row.rank,
        points: row.points,
        sourceRegion: row.region
      });
    }
  }

  const playerUpserts = [];
  for (const [normalized, candidate] of playerCandidates.entries()) {
    playerUpserts.push({
      nickname: candidate.nickname,
      username: buildUsername(candidate.nickname),
      region: candidate.region,
      tranche: trancheFromRank(candidate.rank),
      current_season: config.currentSeason
    });
  }

  const snapshotRows = rows;
  const scoreRows = [];

  for (const [normalized, playerRows] of rowsByNormalized.entries()) {
    const candidate = playerCandidates.get(normalized);
    const playerRegion = candidate ? candidate.region : null;
    const chosen = selectScoreCandidate(playerRows, config.scoreStrategy, playerRegion);
    if (chosen) {
      scoreRows.push({
        playerNormalized: normalized,
        points: chosen.points,
        rank: chosen.rank
      });
    }
  }

  return { playerUpserts, snapshotRows, scoreRows };
}

function mapSnapshots(rows, playerIdByNormalized, config, runId) {
  const output = [];
  let skipped = 0;
  let ignoredGlobal = 0;

  for (const row of rows) {
    const playerId = playerIdByNormalized.get(row.playerNormalized);
    if (!playerId) {
      if (row.region === 'GLOBAL') {
        ignoredGlobal += 1;
      } else {
        skipped += 1;
      }
      continue;
    }

    output.push({
      player_id: playerId,
      region: row.region,
      snapshot_date: config.snapshotDate,
      points: row.points,
      rank: row.rank,
      collected_at: config.collectedAt,
      run_id: runId
    });
  }

  return { rows: output, skipped, ignoredGlobal };
}

function mapScores(rows, playerIdByNormalized, config) {
  const output = [];
  for (const row of rows) {
    const playerId = playerIdByNormalized.get(row.playerNormalized);
    if (!playerId) {
      continue;
    }
    output.push({
      player_id: playerId,
      season: config.currentSeason,
      points: row.points,
      rank: row.rank,
      date: config.snapshotDate,
      timestamp: config.collectedAt
    });
  }
  return { rows: output };
}

async function cleanupGlobalOnlyPlayers(config) {
  const { hasGlobal, hasNonGlobal } = await fetchSnapshotPlayerRegions(config);
  const globalOnly = Array.from(hasGlobal).filter((id) => !hasNonGlobal.has(id));

  if (globalOnly.length === 0) {
    return { players: 0, snapshots: 0, scores: 0 };
  }

  const scoresDeleted = await deleteRowsByPlayerIds(config, 'scores', globalOnly, 'player_id');
  const snapshotsDeleted = await deleteRowsByPlayerIds(
    config,
    'pr_snapshots',
    globalOnly,
    'player_id'
  );
  const playersDeleted = await deleteRowsByPlayerIds(config, 'players', globalOnly, 'id');

  return {
    players: playersDeleted,
    snapshots: snapshotsDeleted,
    scores: scoresDeleted
  };
}

async function fetchSnapshotPlayerRegions(config) {
  const hasGlobal = new Set();
  const hasNonGlobal = new Set();
  const pageSize = 1000;
  let offset = 0;
  let total = null;

  while (total === null || offset < total) {
    const url = new URL(`${config.supabaseUrl}/rest/v1/pr_snapshots`);
    url.searchParams.set('select', 'player_id,region');

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        ...buildSupabaseHeaders(config, 'count=exact'),
        Range: `${offset}-${offset + pageSize - 1}`
      }
    });

    const text = await response.text();
    if (!response.ok) {
      throw new Error(`Supabase lookup failed (pr_snapshots): ${formatSupabaseError(text)}`);
    }

    if (total === null) {
      const contentRange = response.headers.get('content-range');
      total = parseContentRangeTotal(contentRange);
    }

    const rows = text ? safeJson(text) || [] : [];
    for (const row of rows) {
      if (!row || !row.player_id || !row.region) continue;
      if (row.region === 'GLOBAL') {
        hasGlobal.add(row.player_id);
      } else {
        hasNonGlobal.add(row.player_id);
      }
    }

    if (rows.length < pageSize) {
      break;
    }
    offset += pageSize;
  }

  return { hasGlobal, hasNonGlobal };
}

async function addExistingPlayersForGlobalRows(config, rows, playerIdByNormalized) {
  const missingNicknames = new Set();

  for (const row of rows) {
    if (row.region !== 'GLOBAL') continue;
    if (playerIdByNormalized.has(row.playerNormalized)) continue;
    if (row.player) {
      missingNicknames.add(row.player);
    }
  }

  if (missingNicknames.size === 0) {
    return;
  }

  const existing = await fetchPlayersByNicknames(
    config,
    Array.from(missingNicknames)
  );

  for (const [normalized, id] of existing.entries()) {
    if (!playerIdByNormalized.has(normalized)) {
      playerIdByNormalized.set(normalized, id);
    }
  }
}

async function upsertPlayers(config, rows) {
  if (rows.length === 0) {
    return new Map();
  }

  const map = new Map();
  const batches = chunkArray(rows, config.dbBatchSize);
  for (const batch of batches) {
    const response = await supabaseInsert(config, 'players', batch, {
      onConflict: 'nickname',
      prefer: 'resolution=merge-duplicates,return=representation'
    });
    for (const row of response) {
      const normalized = normalizeName(row.nickname || '');
      if (normalized) {
        map.set(normalized, row.id);
      }
    }
  }
  return map;
}

async function deleteRowsByPlayerIds(config, table, ids, column) {
  let deleted = 0;
  const batches = chunkArray(ids, config.lookupBatchSize || 50);
  for (const batch of batches) {
    const values = batch.map((id) => `"${escapePostgrestLiteral(id)}"`).join(',');
    const url = new URL(`${config.supabaseUrl}/rest/v1/${table}`);
    url.searchParams.set(column, `in.(${values})`);

    const response = await fetch(url, {
      method: 'DELETE',
      headers: buildSupabaseHeaders(config, 'count=exact,return=minimal')
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(`Supabase delete failed (${table}): ${formatSupabaseError(text)}`);
    }

    const contentRange = response.headers.get('content-range');
    const batchDeleted = parseContentRangeTotal(contentRange);
    if (batchDeleted !== null) {
      deleted += batchDeleted;
    } else {
      deleted += batch.length;
    }
  }
  return deleted;
}

async function fetchPlayersByNicknames(config, nicknames) {
  const unique = Array.from(
    new Set(
      nicknames
        .map((name) => sanitizeText(name))
        .filter(Boolean)
    )
  );

  const map = new Map();
  const batches = chunkArray(unique, config.lookupBatchSize || 50);

  for (const batch of batches) {
    const filterValues = batch.map((name) => `"${escapePostgrestLiteral(name)}"`).join(',');
    const url = new URL(`${config.supabaseUrl}/rest/v1/players`);
    url.searchParams.set('select', 'id,nickname');
    url.searchParams.set('nickname', `in.(${filterValues})`);

    const response = await fetch(url, {
      method: 'GET',
      headers: buildSupabaseHeaders(config)
    });

    const text = await response.text();
    if (!response.ok) {
      throw new Error(`Supabase lookup failed (players): ${formatSupabaseError(text)}`);
    }

    const data = text ? safeJson(text) || [] : [];
    for (const row of data) {
      const normalized = normalizeName(row.nickname || '');
      if (normalized) {
        map.set(normalized, row.id);
      }
    }
  }

  return map;
}

async function upsertSnapshots(config, rows) {
  if (rows.length === 0) {
    return 0;
  }

  const batches = chunkArray(rows, config.dbBatchSize);
  for (const batch of batches) {
    await supabaseInsert(config, 'pr_snapshots', batch, {
      onConflict: 'player_id,region,snapshot_date',
      prefer: 'resolution=merge-duplicates,return=minimal'
    });
  }
  return rows.length;
}

async function upsertScores(config, rows) {
  if (rows.length === 0) {
    return 0;
  }

  const batches = chunkArray(rows, config.dbBatchSize);
  for (const batch of batches) {
    await supabaseInsert(config, 'scores', batch, {
      onConflict: 'player_id,season',
      prefer: 'resolution=merge-duplicates,return=minimal'
    });
  }
  return rows.length;
}

async function createIngestionRun(config) {
  const response = await supabaseInsert(config, 'ingestion_runs', [
    {
      source: config.ingestionSource,
      status: 'RUNNING',
      started_at: config.collectedAt
    }
  ], {
    prefer: 'return=representation'
  });
  if (!response || !response.length) {
    throw new Error('Failed to create ingestion run.');
  }
  return response[0];
}

async function finalizeRun(config, run, status, totalRows, skippedRows, errorMessage) {
  if (!run || !run.id) return;
  const payload = {
    status,
    finished_at: new Date().toISOString(),
    total_rows_written: totalRows
  };
  if (skippedRows && skippedRows > 0) {
    payload.error_message = `Skipped rows: ${skippedRows}`;
  }
  if (errorMessage) {
    payload.error_message = errorMessage;
  }
  await supabasePatch(config, 'ingestion_runs', `id=eq.${run.id}`, payload);
}

async function supabaseInsert(config, table, rows, options) {
  if (!rows || rows.length === 0) {
    return [];
  }

  const url = new URL(`${config.supabaseUrl}/rest/v1/${table}`);
  if (options && options.onConflict) {
    url.searchParams.set('on_conflict', options.onConflict);
  }

  const response = await fetch(url, {
    method: 'POST',
    headers: buildSupabaseHeaders(config, options && options.prefer),
    body: JSON.stringify(rows)
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`Supabase insert failed (${table}): ${formatSupabaseError(text)}`);
  }

  if (!text) {
    return [];
  }
  return safeJson(text) || [];
}

async function supabasePatch(config, table, filter, payload) {
  const url = `${config.supabaseUrl}/rest/v1/${table}?${filter}`;
  const response = await fetch(url, {
    method: 'PATCH',
    headers: buildSupabaseHeaders(config, 'return=minimal'),
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Supabase update failed (${table}): ${formatSupabaseError(text)}`);
  }
}

function buildSupabaseHeaders(config, prefer) {
  const headers = {
    apikey: config.supabaseKey,
    Authorization: `Bearer ${config.supabaseKey}`,
    'Content-Type': 'application/json'
  };
  if (prefer) {
    headers.Prefer = prefer;
  }
  if (config.supabaseSchema) {
    headers['Accept-Profile'] = config.supabaseSchema;
    headers['Content-Profile'] = config.supabaseSchema;
  }
  return headers;
}

function printDryRunSummary(fetchResult, prepared, buildResult) {
  const failedPages = fetchResult.pageStats.filter((page) => !page.success).length;
  console.log(`Dry run: pages=${fetchResult.pageStats.length}, failedPages=${failedPages}`);
  console.log(`Dry run: rawRows=${fetchResult.rows.length}, cleanRows=${prepared.rows.length}`);
  console.log(
    `Dry run: players=${buildResult.playerUpserts.length}, snapshots=${buildResult.snapshotRows.length}, scores=${buildResult.scoreRows.length}`
  );

  const statusCounts = {};
  const providerCounts = {};
  for (const page of fetchResult.pageStats) {
    const statusKey = String(page.status);
    statusCounts[statusKey] = (statusCounts[statusKey] || 0) + 1;
    providerCounts[page.provider] = (providerCounts[page.provider] || 0) + 1;
  }

  const statusSummary = Object.keys(statusCounts)
    .sort((a, b) => Number(a) - Number(b))
    .map((key) => `${key}:${statusCounts[key]}`)
    .join(', ');
  const providerSummary = Object.keys(providerCounts)
    .sort()
    .map((key) => `${key}:${providerCounts[key]}`)
    .join(', ');

  console.log(`Dry run: statusCounts=${statusSummary || 'none'}`);
  console.log(`Dry run: providerCounts=${providerSummary || 'none'}`);
}

function computeStatus(pageStats, skippedSnapshots) {
  const failedPages = pageStats.filter((page) => !page.success).length;
  if (failedPages > 0 || skippedSnapshots > 0) {
    return 'PARTIAL';
  }
  return 'SUCCESS';
}

function normalizeRow(row, config) {
  const region = String(row.region || '').toUpperCase();
  if (!PR_REGIONS.has(region)) {
    return null;
  }
  if (region === 'GLOBAL' && !config.includeGlobal) {
    return null;
  }

  const player = sanitizeText(row.player);
  const playerNormalized = normalizeName(player);
  if (!player || !playerNormalized) {
    return null;
  }

  const points = Number.isFinite(row.points) ? row.points : parseInt(row.points, 10);
  const rank = Number.isFinite(row.rank) ? row.rank : parseInt(row.rank, 10);
  if (!Number.isFinite(points) || points <= 0 || !Number.isFinite(rank) || rank <= 0) {
    return null;
  }

  return {
    region,
    page: row.page,
    rank,
    player,
    team: sanitizeText(row.team),
    points,
    platform: row.platform,
    timeframe: row.timeframe,
    provider: row.provider,
    playerNormalized
  };
}

function resolvePlayerRegion(row, config) {
  if (PLAYER_REGIONS.has(row.region)) {
    return row.region;
  }
  return null;
}

function selectScoreCandidate(rows, strategy, playerRegion) {
  if (!rows || rows.length === 0) {
    return null;
  }

  if (strategy === 'GLOBAL') {
    const globals = rows.filter((row) => row.region === 'GLOBAL');
    if (globals.length > 0) {
      return pickBest(globals);
    }
  }

  if (strategy === 'REGION' && playerRegion) {
    const regionRows = rows.filter((row) => row.region === playerRegion);
    if (regionRows.length > 0) {
      return pickBest(regionRows);
    }
  }

  return pickBest(rows);
}

function pickBest(rows) {
  return rows.reduce((best, row) => {
    if (!best) return row;
    if (row.points !== best.points) {
      return row.points > best.points ? row : best;
    }
    return row.rank < best.rank ? row : best;
  }, null);
}

function isBetter(candidate, current) {
  if (candidate.points !== current.points) {
    return candidate.points > current.points;
  }
  return candidate.rank < current.rank;
}

function trancheFromRank(rank) {
  if (rank <= 5) return '1-5';
  if (rank <= 10) return '6-10';
  if (rank <= 15) return '11-15';
  if (rank <= 20) return '16-20';
  if (rank <= 25) return '21-25';
  if (rank <= 30) return '26-30';
  return '31-infini';
}

function buildUsername(nickname) {
  const base = String(nickname || '').toLowerCase().replace(/[^a-z0-9]/g, '');
  if (base) {
    return base;
  }
  return `player${Math.abs(simpleHash(String(nickname)))}`;
}

function buildTarget(region, page, config) {
  return `https://fortnitetracker.com/events/powerrankings?platform=${config.platform}&region=${region}&time=${config.timeframe}&page=${page}`;
}

function buildProviderUrl(provider, targetUrl, key, config) {
  if (provider === 'scrapfly') {
    return (
      'https://api.scrapfly.io/scrape' +
      `?key=${encodeURIComponent(key)}` +
      '&asp=true&render_js=true&country=us' +
      `&url=${encodeURIComponent(targetUrl)}`
    );
  }

  if (provider === 'scraperapi') {
    return (
      'https://api.scraperapi.com/?' +
      `api_key=${encodeURIComponent(key)}` +
      `&render=${config.scraperapiRender ? 'true' : 'false'}` +
      '&wait_selector=tbody' +
      `&timeout=${config.requestTimeoutMs}` +
      `&url=${encodeURIComponent(targetUrl)}`
    );
  }

  const base = config.scrapedoBaseUrl || 'http://api.scrape.do/';
  const separator = base.includes('?') ? '&' : '?';
  return (
    `${base}${separator}` +
    `url=${encodeURIComponent(targetUrl)}` +
    `&token=${encodeURIComponent(key)}`
  );
}

function buildHeaders() {
  return {
    'User-Agent': 'fortnite-pronos-ingest/1.0'
  };
}

function pickPrimaryProvider(region, page, config) {
  const providers = getWeightedProviders(config);
  if (!providers.length) {
    return 'scraperapi';
  }
  const total = providers.reduce((sum, provider) => sum + provider.weight, 0);
  const slot = Math.abs(simpleHash(`${region}-${page}`)) % total;
  let cursor = 0;
  for (const provider of providers) {
    cursor += provider.weight;
    if (slot < cursor) {
      return provider.name;
    }
  }
  return providers[0].name;
}

function pickKeyFor(provider, region, page, attempts, keys) {
  const list =
    provider === 'scrapfly'
      ? keys.scrapfly
      : provider === 'scraperapi'
        ? keys.scraperapi
        : keys.scrapedo;
  if (!list.length) {
    throw new Error(`No keys configured for ${provider}`);
  }
  const idx = Math.abs(simpleHash(`${provider}:${region}:${page}:${attempts}`)) % list.length;
  return list[idx];
}

function scheduleRetry(meta, config) {
  const providers = getEnabledProviders(config);
  if (!providers.length) {
    return {
      region: meta.region,
      page: meta.page,
      provider: meta.provider,
      attempts: meta.attempts
    };
  }

  let provider = meta.provider;
  if (!providers.includes(provider)) {
    provider = providers[0];
  } else if (meta.attempts % 2 === 1 && providers.length > 1) {
    provider = nextProvider(provider, providers);
  }

  return {
    region: meta.region,
    page: meta.page,
    provider,
    attempts: meta.attempts
  };
}

function shouldRetry(status, hasTbody, attempts, maxAttempts) {
  if (attempts >= maxAttempts) {
    return false;
  }
  if (!hasTbody) return true;
  if (status === 0 || status === 429 || status === 403) return true;
  if (status >= 500 && status < 600) return true;
  return false;
}

function nextProvider(current, providers) {
  const index = providers.indexOf(current);
  if (index === -1) {
    return providers[0];
  }
  return providers[(index + 1) % providers.length];
}

function getWeightedProviders(config) {
  const providers = [];
  if (isProviderEnabled('scrapfly', config)) {
    providers.push({ name: 'scrapfly', weight: config.weights.scrapfly });
  }
  if (isProviderEnabled('scraperapi', config)) {
    providers.push({ name: 'scraperapi', weight: config.weights.scraperapi });
  }
  if (isProviderEnabled('scrapedo', config)) {
    providers.push({ name: 'scrapedo', weight: config.weights.scrapedo });
  }
  return providers;
}

function getEnabledProviders(config) {
  return ['scrapfly', 'scraperapi', 'scrapedo'].filter((provider) =>
    isProviderEnabled(provider, config)
  );
}

function isProviderEnabled(provider, config) {
  if (provider === 'scrapfly') {
    return config.weights.scrapfly > 0 && config.keys.scrapfly.length > 0;
  }
  if (provider === 'scraperapi') {
    return config.weights.scraperapi > 0 && config.keys.scraperapi.length > 0;
  }
  return config.weights.scrapedo > 0 && config.keys.scrapedo.length > 0;
}

function backoff(attempt) {
  return Math.floor(300 * Math.pow(1.9, attempt)) + Math.floor(Math.random() * 300);
}

function simpleHash(str) {
  let h = 0;
  for (let i = 0; i < str.length; i += 1) {
    h = ((h << 5) - h + str.charCodeAt(i)) | 0;
  }
  return h;
}

function pick(str, regex) {
  if (!str) return '';
  const match = str.match(regex);
  return match ? match[1].trim() : '';
}

function extractHtml(body) {
  if (!body) return '';
  const trimmed = body.trim();
  if (trimmed.startsWith('{')) {
    const json = safeJson(trimmed);
    return json && json.result && json.result.content ? json.result.content : '';
  }
  return trimmed;
}

function safeJson(value) {
  try {
    return JSON.parse(value);
  } catch (err) {
    return null;
  }
}

function normalizeName(value) {
  return String(value || '')
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[\u200B-\u200D\uFEFF]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

function sanitizeText(value) {
  if (!value) return '';
  return String(value).replace(/\s+/g, ' ').trim();
}

function parseArgs(args) {
  const options = {};
  for (let i = 0; i < args.length; i += 1) {
    const arg = args[i];
    if (arg === '--dry-run') {
      options.dryRun = true;
    } else if (arg === '--cleanup-global-only') {
      options.cleanupGlobalOnly = true;
    } else if (arg === '--cleanup-only') {
      options.cleanupOnly = true;
    } else if (arg === '--regions' && args[i + 1]) {
      options.regions = args[i + 1];
      i += 1;
    } else if (arg === '--include-global') {
      options.includeGlobal = true;
    } else if (arg === '--score-strategy' && args[i + 1]) {
      options.scoreStrategy = args[i + 1];
      i += 1;
    } else if (arg === '--no-scores') {
      options.writeScores = false;
    }
  }
  return options;
}

function parseEnvList(name) {
  const raw = process.env[name];
  if (!raw) return [];
  return raw
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean);
}

function mergeTokens(list, token) {
  const output = Array.isArray(list) ? list.slice() : [];
  if (token) {
    output.push(String(token).trim());
  }
  return Array.from(new Set(output.map((value) => String(value).trim()).filter(Boolean)));
}

function parseBool(value, defaultValue) {
  if (value === undefined) return defaultValue;
  return String(value).toLowerCase() === 'true';
}

function parseSnapshotDate(value) {
  if (!value) {
    return new Date().toISOString().slice(0, 10);
  }
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return value;
  }
  return new Date().toISOString().slice(0, 10);
}

function buildRegions(globalPages, regionPages, includeGlobal, filter) {
  const regions = [
    { code: 'GLOBAL', first: 1, last: globalPages },
    { code: 'ASIA', first: 1, last: regionPages },
    { code: 'BR', first: 1, last: regionPages },
    { code: 'EU', first: 1, last: regionPages },
    { code: 'ME', first: 1, last: regionPages },
    { code: 'NAW', first: 1, last: regionPages },
    { code: 'NAC', first: 1, last: regionPages },
    { code: 'OCE', first: 1, last: regionPages }
  ];

  const filtered = filter
    ? filter
        .split(',')
        .map((item) => item.trim().toUpperCase())
        .filter(Boolean)
    : null;

  return regions.filter((region) => {
    if (region.code === 'GLOBAL' && !includeGlobal) {
      return false;
    }
    if (filtered && !filtered.includes(region.code)) {
      return false;
    }
    return true;
  });
}

function chunkArray(items, size) {
  const output = [];
  for (let i = 0; i < items.length; i += size) {
    output.push(items.slice(i, i + size));
  }
  return output;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function formatSupabaseError(text) {
  const json = safeJson(text);
  if (json && json.message) return json.message;
  return text;
}

function escapePostgrestLiteral(value) {
  return String(value).replace(/\\/g, '\\\\').replace(/\"/g, '\\"');
}

function parseContentRangeTotal(contentRange) {
  if (!contentRange) return null;
  const parts = contentRange.split('/');
  if (parts.length !== 2) return null;
  const total = parseInt(parts[1], 10);
  return Number.isFinite(total) ? total : null;
}

function loadEnvFromRoot(rootDir) {
  const envPath = path.join(rootDir, '.env');
  if (!fs.existsSync(envPath)) return;
  const content = fs.readFileSync(envPath, 'utf8');
  const lines = content.split(/\r?\n/);
  for (const line of lines) {
    if (!line || line.trim().startsWith('#')) continue;
    const idx = line.indexOf('=');
    if (idx === -1) continue;
    const key = line.slice(0, idx).trim();
    if (!key || process.env[key] !== undefined) continue;
    let value = line.slice(idx + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    process.env[key] = value;
  }
}
