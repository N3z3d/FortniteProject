-- Drafts table
CREATE TABLE IF NOT EXISTS drafts (
    id UUID PRIMARY KEY,
    season INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP
);

-- Draft picks table
CREATE TABLE IF NOT EXISTS draft_picks (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL REFERENCES drafts(id) ON DELETE CASCADE,
    pick_number INTEGER NOT NULL,
    player_id UUID NOT NULL,
    participant VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_draft_picks_draft ON draft_picks(draft_id); 