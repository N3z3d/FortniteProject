-- V34: PR Snapshots — append-only rank history table for sparkline charts
-- One row per (player_id, region, snapshot_date). No UPDATE, only INSERT + SELECT.

CREATE TABLE IF NOT EXISTS pr_snapshots (
    id             UUID         NOT NULL,
    player_id      UUID         NOT NULL,
    region         VARCHAR(10)  NOT NULL,
    rank           INT          NOT NULL,
    pr_value       INT          NOT NULL DEFAULT 0,
    snapshot_date  DATE         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_pr_snapshots_player_region_date
        UNIQUE (player_id, region, snapshot_date)
);

-- Index optimised for sparkline queries: player + region + date DESC
CREATE INDEX IF NOT EXISTS idx_pr_snapshots_player_region_date
    ON pr_snapshots (player_id, region, snapshot_date DESC);
