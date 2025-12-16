-- Initialisation du schéma
-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enums
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('USER', 'ADMIN', 'PARTICIPANT', 'SPECTATEUR');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE region_enum AS ENUM ('EU', 'NAW', 'BR', 'ASIA', 'OCE', 'NAC', 'ME');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Tables principales
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL DEFAULT '$2a$10$defaultpassword',
    role user_role NOT NULL DEFAULT 'PARTICIPANT',
    current_season INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS players (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fortnite_id VARCHAR(255) UNIQUE,
    username VARCHAR(100),
    nickname VARCHAR(100) UNIQUE NOT NULL,
    region region_enum NOT NULL,
    tranche VARCHAR(10) NOT NULL DEFAULT '1',
    current_season INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    owner_id UUID REFERENCES users(id),
    season INTEGER NOT NULL DEFAULT 2025,
    total_score INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS scores (
    player_id UUID NOT NULL REFERENCES players(id),
    season INTEGER NOT NULL,
    points INTEGER NOT NULL DEFAULT 0,
    placement INTEGER,
    rank INTEGER,
    kills INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    matches_played INTEGER NOT NULL DEFAULT 0,
    date DATE,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(player_id, season)
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL DEFAULT 'Notification',
    type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scrape_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    duration_ms BIGINT,
    players_processed INTEGER NOT NULL DEFAULT 0,
    players_failed INTEGER NOT NULL DEFAULT 0,
    errors TEXT
);

CREATE TABLE IF NOT EXISTS team_players (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position > 0),
    until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(team_id, player_id, created_at)
);

CREATE TABLE IF NOT EXISTS trades (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_from_id UUID NOT NULL REFERENCES teams(id),
    team_to_id UUID NOT NULL REFERENCES teams(id),
    player_out_id UUID NOT NULL REFERENCES players(id),
    player_in_id UUID NOT NULL REFERENCES players(id),
    from_user_id UUID NOT NULL REFERENCES users(id),
    to_user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_scores_player_id ON scores(player_id);
CREATE INDEX IF NOT EXISTS idx_scores_season ON scores(season);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_teams_owner_id ON teams(owner_id);
CREATE INDEX IF NOT EXISTS idx_scrape_runs_status ON scrape_runs(status);
CREATE INDEX IF NOT EXISTS idx_team_players_team ON team_players(team_id);
CREATE INDEX IF NOT EXISTS idx_team_players_player ON team_players(player_id);
CREATE INDEX IF NOT EXISTS idx_trades_team_from ON trades(team_from_id);
CREATE INDEX IF NOT EXISTS idx_trades_team_to ON trades(team_to_id);
CREATE INDEX IF NOT EXISTS idx_trades_status ON trades(status); 
