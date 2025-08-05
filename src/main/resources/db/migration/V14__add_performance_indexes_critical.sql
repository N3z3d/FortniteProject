-- Performance indexes for critical queries identified in analysis
-- These indexes will significantly improve dashboard loading times

-- Index for team season lookups (most common query)
CREATE INDEX IF NOT EXISTS idx_team_season ON teams(season);

-- Index for team player active status queries
CREATE INDEX IF NOT EXISTS idx_team_player_active ON team_players(team_id, until) WHERE until IS NULL;

-- Index for player region and tranche queries (used in dashboard statistics)
CREATE INDEX IF NOT EXISTS idx_player_region ON players(region);
CREATE INDEX IF NOT EXISTS idx_player_tranche ON players(tranche);

-- Index for score season queries (used in leaderboard calculations)
CREATE INDEX IF NOT EXISTS idx_score_season ON scores(season);
CREATE INDEX IF NOT EXISTS idx_score_player_season ON scores(player_id, season);

-- Composite index for team ownership queries
CREATE INDEX IF NOT EXISTS idx_team_owner_season ON teams(owner_id, season);

-- Index for user authentication queries
CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);

-- Index for game participant queries
CREATE INDEX IF NOT EXISTS idx_game_participant_game ON game_participants(game_id);
CREATE INDEX IF NOT EXISTS idx_game_participant_user ON game_participants(user_id);

-- Add statistics for query planner
ANALYZE teams;
ANALYZE team_players;
ANALYZE players;
ANALYZE scores;
ANALYZE users;
ANALYZE game_participants;