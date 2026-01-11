# Supabase - Mapping & Strategy

Status: wiring done (REST client + row mappings). MCP Supabase is configured locally (read-only).

## Connection config (app)

Properties (environment):
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SCHEMA` (default: `public`)
- `SUPABASE_SEED_GAME_ID` (optional fallback for seed)
- `FORTNITE_DATA_PROVIDER=supabase`

## MCP config (local)

- Uses `@supabase/mcp-server-supabase` via the local Codex MCP config.
- Requires `SUPABASE_ACCESS_TOKEN` (Supabase PAT). If unset, it falls back to `MCP_API_KEY`.
- Project ref is derived from `SUPABASE_URL` or set via `SUPABASE_PROJECT_REF`.

## Entity mapping (from docs/db-design.md)

Primary tables mapped:
- users
- games
- game_participants
- teams
- team_players
- players
- scores

Seed view (preferred):
- `player_assignments` (pronostiqueur, nickname, region, score, rank)

## Strategy

- Dev: keep `csv` provider by default (stable CSV seed).
- Prod: switch to `supabase` provider when credentials and RPCs are ready.
- Tests: keep H2 in-memory (no Supabase calls).

## Notes

- If `player_assignments` is absent or empty, the seed fallback uses core tables.
- Configure `SUPABASE_SEED_GAME_ID` to scope the fallback to one game.
- Validate schema against `docs/db-design.md` before switching providers.
