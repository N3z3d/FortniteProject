-- V32: Snake-draft region cursors table
-- Tracks the per-region cursor (round + pick) for snake-draft mode.
-- snake_order stores participant UUIDs as comma-separated text.

CREATE TABLE IF NOT EXISTS draft_region_cursors (
    draft_id     UUID        NOT NULL,
    region       VARCHAR(10) NOT NULL,
    current_round INT        NOT NULL DEFAULT 1,
    current_pick  INT        NOT NULL DEFAULT 1,
    snake_order   TEXT        NOT NULL,
    PRIMARY KEY (draft_id, region)
);
