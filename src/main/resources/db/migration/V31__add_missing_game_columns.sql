-- V31: Add missing columns to games table
-- Adds invitation code expiration and trading-related columns

-- Invitation code expiration
ALTER TABLE games ADD COLUMN IF NOT EXISTS invitation_code_expires_at TIMESTAMP;

-- Trading columns (if not already present)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'games' AND column_name = 'trading_enabled') THEN
        ALTER TABLE games ADD COLUMN trading_enabled BOOLEAN DEFAULT false;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'games' AND column_name = 'max_trades_per_team') THEN
        ALTER TABLE games ADD COLUMN max_trades_per_team INTEGER DEFAULT 5;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'games' AND column_name = 'trade_deadline') THEN
        ALTER TABLE games ADD COLUMN trade_deadline TIMESTAMP;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'games' AND column_name = 'current_season') THEN
        ALTER TABLE games ADD COLUMN current_season INTEGER DEFAULT EXTRACT(YEAR FROM CURRENT_DATE);
    END IF;
END $$;

-- Index for invitation code expiration
CREATE INDEX IF NOT EXISTS idx_games_invitation_expires ON games(invitation_code_expires_at) WHERE invitation_code IS NOT NULL;

COMMENT ON COLUMN games.invitation_code_expires_at IS 'Expiration time for the invitation code, NULL means permanent';
