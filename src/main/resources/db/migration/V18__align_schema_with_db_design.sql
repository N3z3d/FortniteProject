-- flyway:executeInTransaction=false
-- Final schema alignment with db-design and JPA expectations

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') AND NOT EXISTS (
      SELECT 1
      FROM pg_type t
      JOIN pg_enum e ON t.oid = e.enumtypid
      WHERE t.typname = 'user_role' AND e.enumlabel = 'SPECTATOR'
    )
  THEN
    ALTER TYPE user_role ADD VALUE 'SPECTATOR';
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'region_enum') AND NOT EXISTS (
      SELECT 1
      FROM pg_type t
      JOIN pg_enum e ON t.oid = e.enumtypid
      WHERE t.typname = 'region_enum' AND e.enumlabel = 'NA'
    )
  THEN
    ALTER TYPE region_enum ADD VALUE 'NA';
  END IF;
END $$;

UPDATE users SET role = 'USER' WHERE role = 'PARTICIPANT';

ALTER TABLE games
  ADD COLUMN IF NOT EXISTS finished_at TIMESTAMP;

ALTER TABLE game_participants
  ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS is_creator BOOLEAN;

UPDATE game_participants SET joined_at = COALESCE(joined_at, CURRENT_TIMESTAMP);
UPDATE game_participants SET is_creator = COALESCE(is_creator, FALSE);

ALTER TABLE game_participants
  ALTER COLUMN is_creator SET DEFAULT FALSE;

ALTER TABLE team_players
  ADD COLUMN IF NOT EXISTS since TIMESTAMPTZ;

UPDATE team_players SET since = COALESCE(since, created_at);

ALTER TABLE drafts
  ALTER COLUMN status SET DEFAULT 'PENDING';

UPDATE drafts SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP);
UPDATE drafts SET updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP);
UPDATE drafts SET current_round = COALESCE(current_round, 1);
UPDATE drafts SET current_pick = COALESCE(current_pick, 1);
UPDATE drafts SET total_rounds = COALESCE(total_rounds, 1);

ALTER TABLE drafts
  ALTER COLUMN created_at SET NOT NULL,
  ALTER COLUMN updated_at SET NOT NULL,
  ALTER COLUMN current_round SET NOT NULL,
  ALTER COLUMN current_pick SET NOT NULL,
  ALTER COLUMN total_rounds SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.table_constraints
      WHERE table_name = 'drafts' AND constraint_name = 'fk_drafts_game'
    )
  THEN
    ALTER TABLE drafts
      ADD CONSTRAINT fk_drafts_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE;
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_drafts_game_id ON drafts(game_id) WHERE game_id IS NOT NULL;

DO $$
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'draft_picks' AND column_name = 'round'
    )
    AND NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'draft_picks' AND column_name = 'round_number'
    )
  THEN
    ALTER TABLE draft_picks RENAME COLUMN round TO round_number;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'draft_picks' AND column_name = 'round'
    )
    AND EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'draft_picks' AND column_name = 'round_number'
    )
  THEN
    UPDATE draft_picks SET round_number = COALESCE(round_number, round);
    ALTER TABLE draft_picks DROP COLUMN round;
  END IF;
END $$;

ALTER TABLE draft_picks
  ADD COLUMN IF NOT EXISTS round_number INTEGER,
  ADD COLUMN IF NOT EXISTS time_taken_seconds INTEGER,
  ADD COLUMN IF NOT EXISTS auto_pick BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE draft_picks SET selection_time = COALESCE(selection_time, CURRENT_TIMESTAMP);
UPDATE draft_picks SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP);
UPDATE draft_picks SET auto_pick = COALESCE(auto_pick, FALSE);
UPDATE draft_picks SET round_number = COALESCE(round_number, 1);

ALTER TABLE draft_picks
  ALTER COLUMN selection_time SET NOT NULL,
  ALTER COLUMN created_at SET NOT NULL,
  ALTER COLUMN auto_pick SET NOT NULL,
  ALTER COLUMN round_number SET NOT NULL;

DO $$
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'draft_picks' AND column_name = 'participant_id'
    ) AND NOT EXISTS (
      SELECT 1 FROM draft_picks WHERE participant_id IS NULL
    )
  THEN
    ALTER TABLE draft_picks ALTER COLUMN participant_id SET NOT NULL;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM information_schema.table_constraints
      WHERE table_name = 'draft_picks' AND constraint_name = 'fk_draft_picks_participant'
    )
  THEN
    ALTER TABLE draft_picks
      ADD CONSTRAINT fk_draft_picks_participant
      FOREIGN KEY (participant_id) REFERENCES game_participants(id) ON DELETE CASCADE;
  END IF;
END $$;

UPDATE trades SET from_team_id = COALESCE(from_team_id, team_from_id);
UPDATE trades SET to_team_id = COALESCE(to_team_id, team_to_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM trades WHERE from_team_id IS NULL) THEN
    ALTER TABLE trades ALTER COLUMN from_team_id SET NOT NULL;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM trades WHERE to_team_id IS NULL) THEN
    ALTER TABLE trades ALTER COLUMN to_team_id SET NOT NULL;
  END IF;
END $$;
