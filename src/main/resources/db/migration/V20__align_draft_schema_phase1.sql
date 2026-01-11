-- Draft schema alignment (phase 1, additive and backward-compatible)

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'draft_region_slot') THEN
    CREATE TYPE draft_region_slot AS ENUM ('EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME');
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pick_source') THEN
    CREATE TYPE pick_source AS ENUM ('USER', 'AUTO', 'ASYNC_RESOLVE');
  END IF;
END $$;

ALTER TABLE draft_picks
  ADD COLUMN IF NOT EXISTS region_slot draft_region_slot,
  ADD COLUMN IF NOT EXISTS pick_source pick_source,
  ADD COLUMN IF NOT EXISTS picked_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS tranche_at_pick INTEGER;

UPDATE draft_picks
SET picked_at = COALESCE(picked_at, selection_time, created_at, CURRENT_TIMESTAMP);

UPDATE draft_picks
SET pick_source = COALESCE(
  pick_source,
  CASE
    WHEN auto_pick THEN 'AUTO'::pick_source
    ELSE 'USER'::pick_source
  END
);

ALTER TABLE draft_picks
  ALTER COLUMN pick_source SET DEFAULT 'USER'::pick_source,
  ALTER COLUMN picked_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE draft_picks
  ALTER COLUMN pick_source SET NOT NULL,
  ALTER COLUMN picked_at SET NOT NULL;

CREATE TABLE IF NOT EXISTS draft_async_windows (
  id UUID PRIMARY KEY,
  draft_id UUID NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
  window_no INTEGER NOT NULL,
  region_slot draft_region_slot NOT NULL,
  opens_at TIMESTAMP NOT NULL,
  closes_at TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_draft_async_windows_draft_window
ON draft_async_windows(draft_id, window_no, region_slot);

CREATE TABLE IF NOT EXISTS draft_async_selections (
  id UUID PRIMARY KEY,
  draft_id UUID NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
  participant_id UUID NOT NULL REFERENCES game_participants(id) ON DELETE CASCADE,
  window_no INTEGER NOT NULL,
  region_slot draft_region_slot NOT NULL,
  player_id UUID NOT NULL REFERENCES players(id),
  submitted_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_draft_async_selections_unique
ON draft_async_selections(draft_id, participant_id, window_no, region_slot, player_id);
