# Power Rankings Ingestion (Supabase)

Goal: scrape FortniteTracker power rankings and ingest the player catalog directly into Supabase.
No local CSV/HTML outputs.

Targets (Supabase tables)
- `players` (upsert by nickname)
- `pr_snapshots` (daily points + rank, per region)
- `scores` (optional, current-season points)
- `ingestion_runs` (traceability)

Prerequisites
- Node 18+ (global fetch)
- Supabase REST access with a service role key
- Flyway migration `V22__add_pr_ingestion_tables.sql` applied
- Scrape.do and/or Scrapfly/ScraperAPI keys in `.env`

Environment (set in `.env` or shell)
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY` (or `SUPABASE_API_KEY`)
- `SUPABASE_SCHEMA` (default `public`)
- `SCRAPFLY_KEYS` (comma-separated)
- `SCRAPERAPI_KEYS` (comma-separated)
- `SCRAPEDO_TOKEN` (single token) or `SCRAPEDO_TOKENS` (comma-separated)
- `SCRAPERAPI_RENDER` (true/false, default false)
- `SCRAPE_PLATFORM` (default `pc`)
- `SCRAPE_TIMEFRAME` (default `year`)
- `SCRAPE_PAGE_SIZE` (default `100`)
- `SCRAPE_GLOBAL_PAGES` (default `21`)
- `SCRAPE_REGION_PAGES` (default `3`)
- `SCRAPE_INCLUDE_GLOBAL` (default `false`)
- `SCRAPE_CHUNK_SIZE` (default `40`)
- `SCRAPE_MAX_ATTEMPTS` (default `8`)
- `SCRAPE_TIMEOUT_MS` (default `20000`)
- `SCRAPFLY_WEIGHT` (default `1`)
- `SCRAPERAPI_WEIGHT` (default `1`)
- `SCRAPEDO_WEIGHT` (default `1`)
- `SCRAPEDO_BASE_URL` (default `http://api.scrape.do/`)
- `SCRAPE_DEBUG` (default `false`)
- `INGESTION_SOURCE` (default `FORTNITE_TRACKER`)
- `INGESTION_SEASON` (default `2025`)
- `INGESTION_BATCH_SIZE` (default `500`)
- `INGESTION_WRITE_SCORES` (default `true`)
- `SCORE_STRATEGY` (MAX | GLOBAL | REGION)
- `INGESTION_LOOKUP_BATCH_SIZE` (default `50`)
- `SNAPSHOT_DATE` (optional, `YYYY-MM-DD`)

Commands
```bash
node scripts/ingest/power-rankings.js
node scripts/ingest/power-rankings.js --dry-run
node scripts/ingest/power-rankings.js --regions EU,NAC
node scripts/ingest/power-rankings.js --include-global
node scripts/ingest/power-rankings.js --cleanup-global-only
```

Notes
- If `SCRAPE_INCLUDE_GLOBAL=false`, GLOBAL pages are skipped entirely.
- If `SCRAPE_INCLUDE_GLOBAL=true`, GLOBAL rows never create players; they only add snapshots/scores for players already in the catalog.
- `SCORE_STRATEGY=MAX` uses the highest points row per player to update `scores`.
