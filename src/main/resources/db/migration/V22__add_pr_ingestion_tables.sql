-- flyway:executeInTransaction=false
-- Add ingestion tracking and PR snapshot tables

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pr_region') THEN
    CREATE TYPE pr_region AS ENUM ('EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME', 'GLOBAL');
  ELSE
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type t
        JOIN pg_enum e ON t.oid = e.enumtypid
        WHERE t.typname = 'pr_region' AND e.enumlabel = 'GLOBAL'
      )
    THEN
      ALTER TYPE pr_region ADD VALUE 'GLOBAL';
    END IF;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS ingestion_runs (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  source VARCHAR(50) NOT NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at TIMESTAMPTZ,
  status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
  total_rows_written INTEGER,
  error_message TEXT
);

CREATE TABLE IF NOT EXISTS pr_snapshots (
  player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  region pr_region NOT NULL,
  snapshot_date DATE NOT NULL,
  points INTEGER NOT NULL,
  rank INTEGER NOT NULL,
  collected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  run_id UUID REFERENCES ingestion_runs(id),
  PRIMARY KEY(player_id, region, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_pr_snapshots_region_date_rank
  ON pr_snapshots(region, snapshot_date, rank);

CREATE INDEX IF NOT EXISTS idx_pr_snapshots_player_region_date
  ON pr_snapshots(player_id, region, snapshot_date);
