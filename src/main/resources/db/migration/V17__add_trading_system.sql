-- Migration V17: Add Trading System
-- Add trading-related fields to existing tables and update trade table

-- Update games table with trading fields
ALTER TABLE games ADD COLUMN trading_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE games ADD COLUMN max_trades_per_team INTEGER DEFAULT 5;
ALTER TABLE games ADD COLUMN trade_deadline TIMESTAMP;

-- Update teams table with completed trades count
ALTER TABLE teams ADD COLUMN completed_trades_count INTEGER DEFAULT 0;
ALTER TABLE teams ADD COLUMN game_id UUID;

-- Update players table with locked field
ALTER TABLE players ADD COLUMN locked BOOLEAN DEFAULT FALSE;

-- Update trades table with new structure for comprehensive trading
ALTER TABLE trades ADD COLUMN from_team_id UUID;
ALTER TABLE trades ADD COLUMN to_team_id UUID;
ALTER TABLE trades ADD COLUMN proposed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE trades ADD COLUMN accepted_at TIMESTAMP;
ALTER TABLE trades ADD COLUMN rejected_at TIMESTAMP;
ALTER TABLE trades ADD COLUMN cancelled_at TIMESTAMP;
ALTER TABLE trades ADD COLUMN original_trade_id UUID;

-- Update status column to support new statuses
-- First, update existing data
UPDATE trades SET status = 'PENDING' WHERE status IS NULL;

-- Add constraint for status enum
ALTER TABLE trades DROP CONSTRAINT IF EXISTS trades_status_check;
ALTER TABLE trades ADD CONSTRAINT trades_status_check 
  CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'COUNTERED', 'COMPLETED'));

-- Create junction tables for multi-player trades
CREATE TABLE trade_offered_players (
  trade_id UUID NOT NULL,
  player_id UUID NOT NULL,
  PRIMARY KEY (trade_id, player_id),
  FOREIGN KEY (trade_id) REFERENCES trades(id) ON DELETE CASCADE,
  FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

CREATE TABLE trade_requested_players (
  trade_id UUID NOT NULL,
  player_id UUID NOT NULL,
  PRIMARY KEY (trade_id, player_id),
  FOREIGN KEY (trade_id) REFERENCES trades(id) ON DELETE CASCADE,
  FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- Add foreign key constraints
ALTER TABLE trades ADD CONSTRAINT fk_trades_from_team 
  FOREIGN KEY (from_team_id) REFERENCES teams(id);
  
ALTER TABLE trades ADD CONSTRAINT fk_trades_to_team 
  FOREIGN KEY (to_team_id) REFERENCES teams(id);

ALTER TABLE trades ADD CONSTRAINT fk_trades_original_trade 
  FOREIGN KEY (original_trade_id) REFERENCES trades(id);

ALTER TABLE teams ADD CONSTRAINT fk_teams_game 
  FOREIGN KEY (game_id) REFERENCES games(id);

-- Create indexes for performance
CREATE INDEX idx_trades_from_team ON trades(from_team_id);
CREATE INDEX idx_trades_to_team ON trades(to_team_id);
CREATE INDEX idx_trades_status ON trades(status);
CREATE INDEX idx_trades_proposed_at ON trades(proposed_at);
CREATE INDEX idx_trades_game_id ON trades(from_team_id, to_team_id);
CREATE INDEX idx_teams_game_id ON teams(game_id);
CREATE INDEX idx_players_locked ON players(locked);

-- Migrate existing trade data if any exists
UPDATE trades SET from_team_id = team_from_id WHERE team_from_id IS NOT NULL;
UPDATE trades SET to_team_id = team_to_id WHERE team_to_id IS NOT NULL;
UPDATE trades SET proposed_at = created_at WHERE created_at IS NOT NULL AND proposed_at IS NULL;

-- Enable trading for existing games (optional - can be controlled per game)
UPDATE games SET trading_enabled = FALSE WHERE trading_enabled IS NULL;

COMMENT ON TABLE trade_offered_players IS 'Junction table for players offered in a trade';
COMMENT ON TABLE trade_requested_players IS 'Junction table for players requested in a trade';
COMMENT ON COLUMN games.trading_enabled IS 'Whether trading is enabled for this game';
COMMENT ON COLUMN games.max_trades_per_team IS 'Maximum number of completed trades per team';
COMMENT ON COLUMN games.trade_deadline IS 'Deadline after which no new trades can be proposed';
COMMENT ON COLUMN teams.completed_trades_count IS 'Number of completed trades for this team';
COMMENT ON COLUMN players.locked IS 'Whether this player is locked from trading (e.g., during matches)';