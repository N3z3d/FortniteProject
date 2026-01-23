-- V30: Add missing columns to games table
-- This migration adds soft delete and invitation code expiration support

-- Soft delete support
ALTER TABLE games ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Invitation code expiration
ALTER TABLE games ADD COLUMN IF NOT EXISTS invitation_code_expires_at TIMESTAMP;

-- Trading columns (if not already present)
ALTER TABLE games ADD COLUMN IF NOT EXISTS trading_enabled BOOLEAN DEFAULT false;
ALTER TABLE games ADD COLUMN IF NOT EXISTS max_trades_per_team INTEGER DEFAULT 5;
ALTER TABLE games ADD COLUMN IF NOT EXISTS trade_deadline TIMESTAMP;
ALTER TABLE games ADD COLUMN IF NOT EXISTS current_season INTEGER DEFAULT EXTRACT(YEAR FROM CURRENT_DATE);

-- Create index for efficient querying of non-deleted games
CREATE INDEX IF NOT EXISTS idx_games_deleted_at ON games(deleted_at);

-- Create partial index for active (non-deleted) games
CREATE INDEX IF NOT EXISTS idx_games_active ON games(id) WHERE deleted_at IS NULL;

-- Index for invitation code expiration
CREATE INDEX IF NOT EXISTS idx_games_invitation_expires ON games(invitation_code_expires_at) WHERE invitation_code IS NOT NULL;

COMMENT ON COLUMN games.deleted_at IS 'Timestamp when the game was soft-deleted, NULL if active';
COMMENT ON COLUMN games.invitation_code_expires_at IS 'Expiration time for the invitation code, NULL means permanent';
