# Supabase Wiring Notes

## Goal
Prepare the project for a Supabase-backed data source without changing runtime
behavior yet.

## Current status
- Supabase REST client is wired (schema-aware headers) with row mappings.
- Seed provider uses `player_assignments`; fallback uses core tables if
  `SUPABASE_SEED_GAME_ID` is set.
- MCP Supabase access is configured locally (Codex MCP, read-only).

## Connection baseline
Supabase uses Postgres. For Spring Boot, supply the usual DB variables:
- `DB_HOST`: `db.<project-ref>.supabase.co`
- `DB_PORT`: `5432`
- `DB_NAME`: `postgres` (or the DB name in Supabase)
- `DB_USERNAME`: `postgres` (or your Supabase DB user)
- `DB_PASSWORD`: your Supabase DB password

The app also accepts:
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SCHEMA` (default `public`)
- `SUPABASE_SEED_GAME_ID` (optional seed fallback)

## MCP (Codex)
- Uses `@supabase/mcp-server-supabase` via a local wrapper script.
- Requires `SUPABASE_ACCESS_TOKEN` (Supabase PAT). If unset, it falls back to `MCP_API_KEY`.
- Project ref is derived from `SUPABASE_URL` or set via `SUPABASE_PROJECT_REF`.

## Entity mapping (backend)
Primary tables mapped in code:
- users, games, game_participants
- teams, team_players
- players, scores

Seed view (preferred):
- `player_assignments` (pronostiqueur, nickname, region, score, rank)

See `docs/db-design.md` and `docs/db/supabase-mapping.md`.

## Dev / prod strategy
- Recommended: two Supabase projects (dev vs prod) for strict separation.
- Alternative: shared project with separate schemas (`SUPABASE_SCHEMA=dev|prod`).
- Tests: keep H2 in-memory (no Supabase calls).

## Next steps
- Validate the `player_assignments` view or core tables in Supabase.
- Switch `fortnite.data.provider` to `supabase` in dev once validated.
