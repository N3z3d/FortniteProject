-- V29: Create player_aliases table for JPA mapping alignment
CREATE TABLE IF NOT EXISTS player_aliases (
  alias_id UUID PRIMARY KEY,
  player_id UUID REFERENCES players(id) ON DELETE CASCADE,
  nickname VARCHAR(255),
  source VARCHAR(255),
  status VARCHAR(255),
  current BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_player_aliases_player_id ON player_aliases(player_id);
CREATE INDEX IF NOT EXISTS idx_player_aliases_player_current ON player_aliases(player_id, current);
