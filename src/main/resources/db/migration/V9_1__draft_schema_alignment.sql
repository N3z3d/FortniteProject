-- Align draft schema ahead of V10 seed (drafts, picks, orders, participant links)

ALTER TABLE drafts
  ADD COLUMN IF NOT EXISTS game_id UUID,
  ADD COLUMN IF NOT EXISTS current_round INTEGER,
  ADD COLUMN IF NOT EXISTS current_pick INTEGER,
  ADD COLUMN IF NOT EXISTS total_rounds INTEGER,
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS started_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS finished_at TIMESTAMP;

ALTER TABLE draft_picks
  ADD COLUMN IF NOT EXISTS participant_id UUID,
  ADD COLUMN IF NOT EXISTS round INTEGER,
  ADD COLUMN IF NOT EXISTS selection_time TIMESTAMP;

CREATE TABLE IF NOT EXISTS draft_orders (
  id UUID PRIMARY KEY,
  draft_id UUID NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
  participant_id UUID NOT NULL REFERENCES game_participants(id) ON DELETE CASCADE,
  draft_position INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'game_participant_players'
        AND column_name = 'participant_id'
    )
    AND NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'game_participant_players'
        AND column_name = 'game_participant_id'
    )
  THEN
    ALTER TABLE game_participant_players RENAME COLUMN participant_id TO game_participant_id;
  END IF;
END $$;

ALTER TABLE game_participant_players
  ADD COLUMN IF NOT EXISTS id UUID DEFAULT uuid_generate_v4(),
  ADD COLUMN IF NOT EXISTS selection_order INTEGER;
