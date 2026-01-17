-- flyway:executeInTransaction=false
-- Migration V12: Ajout d'index pour optimiser les performances
-- Corrige les problèmes identifiés de requêtes lentes

-- Index pour optimiser les requêtes de leaderboard par saison
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_teams_season 
ON teams(season) 
WHERE season IS NOT NULL;

-- Index pour optimiser les requêtes de score par joueur et saison
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_scores_player_season 
ON scores(player_id, season) 
WHERE player_id IS NOT NULL AND season IS NOT NULL;

-- Index pour optimiser les requêtes de games par statut
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_games_status 
ON games(status) 
WHERE status IS NOT NULL;

-- Index pour optimiser les requêtes de participants par game
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_game_participants_game_id 
ON game_participants(game_id) 
WHERE game_id IS NOT NULL;

-- Index pour optimiser les requêtes de team_players actifs
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_team_players_active 
ON team_players(team_id, until) 
WHERE until IS NULL;

-- Index composite pour optimiser les requêtes team + owner + saison
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_teams_owner_season 
ON teams(owner_id, season) 
WHERE owner_id IS NOT NULL AND season IS NOT NULL;

-- Index pour optimiser les requêtes de draft par game
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_drafts_game_id 
ON drafts(game_id) 
WHERE game_id IS NOT NULL;

-- Index pour optimiser les requêtes de draft_picks par draft
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_draft_picks_draft_id 
ON draft_picks(draft_id) 
WHERE draft_id IS NOT NULL;

-- Index pour optimiser les requêtes de joueurs par région
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_players_region 
ON players(region) 
WHERE region IS NOT NULL;

-- Index pour optimiser les requêtes de transfers
DO $$
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_name = 'transfers'
    )
  THEN
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_transfers_from_team ON transfers(from_team_id) WHERE from_team_id IS NOT NULL';
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_transfers_to_team ON transfers(to_team_id) WHERE to_team_id IS NOT NULL';
    EXECUTE 'ANALYZE transfers';
  END IF;
END $$;

-- Commentaires pour documentation
COMMENT ON INDEX idx_teams_season IS 'Optimise les requêtes de leaderboard par saison';
COMMENT ON INDEX idx_scores_player_season IS 'Optimise le calcul des scores de joueurs';
COMMENT ON INDEX idx_games_status IS 'Optimise les filtres par statut de game';
COMMENT ON INDEX idx_game_participants_game_id IS 'Optimise les requêtes de participants';
COMMENT ON INDEX idx_team_players_active IS 'Optimise les requêtes de joueurs actifs dans les équipes';
COMMENT ON INDEX idx_teams_owner_season IS 'Index composite pour team/owner/saison';
COMMENT ON INDEX idx_players_region IS 'Optimise les filtres par région de joueurs';

-- Statistiques pour le query planner
ANALYZE teams;
ANALYZE scores;
ANALYZE games;
ANALYZE game_participants;
ANALYZE team_players;
ANALYZE players;
ANALYZE drafts;
ANALYZE draft_picks;
