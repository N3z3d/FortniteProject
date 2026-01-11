-- Migration V13: Index avancés pour optimisations critiques identifiées
-- Optimisations spécifiques pour les requêtes les plus coûteuses

-- Index composite pour games avec créateur et date de création (getGamesByUser optimisé)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_games_creator_created_at 
ON games(creator_id, created_at DESC) 
WHERE creator_id IS NOT NULL;

-- Index pour optimiser les requêtes de code d'invitation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_games_invitation_code 
ON games(invitation_code) 
WHERE invitation_code IS NOT NULL;

-- Index composite pour participants avec utilisateur et date de sélection
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_participants_user_selection_time 
ON game_participants(user_id, last_selection_time DESC) 
WHERE user_id IS NOT NULL;

-- Index pour optimiser les requêtes de draft picks par participant
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_draft_picks_participant_id 
ON draft_picks(participant_id) 
WHERE participant_id IS NOT NULL;

-- Index composite pour draft picks par draft et joueur (évite doublons)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_draft_picks_draft_player 
ON draft_picks(draft_id, player_id) 
WHERE draft_id IS NOT NULL AND player_id IS NOT NULL;

-- Index pour optimiser les requêtes de scores par timestamp (données récentes)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_scores_timestamp_desc 
ON scores(timestamp DESC) 
WHERE timestamp IS NOT NULL;

-- Index composite pour team_players par équipe et position
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_team_players_team_position 
ON team_players(team_id, position) 
WHERE team_id IS NOT NULL AND position IS NOT NULL;

-- Index pour optimiser les requêtes de joueurs par username (recherche)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_players_username_gin 
ON players USING gin(to_tsvector('simple', username)) 
WHERE username IS NOT NULL;

-- Index pour optimiser les requêtes de joueurs par nickname (recherche)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_players_nickname_gin 
ON players USING gin(to_tsvector('simple', nickname)) 
WHERE nickname IS NOT NULL;

-- Index composite pour optimiser les requêtes de region rules
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_game_region_rules_game_region 
ON game_region_rules(game_id, region) 
WHERE game_id IS NOT NULL AND region IS NOT NULL;

-- Index pour optimiser les requêtes de notifications par utilisateur et date
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_user_created 
ON notifications(user_id, created_at DESC) 
WHERE user_id IS NOT NULL;

-- Index partiel pour games actives (statut CREATING, DRAFTING, ACTIVE)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_games_active_status 
ON games(status, created_at DESC) 
WHERE status IN ('CREATING', 'DRAFTING', 'ACTIVE');

-- Index pour optimiser les requêtes de trades
DO $$
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'trades' AND column_name = 'from_team_id'
    )
  THEN
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_trades_from_team ON trades(from_team_id) WHERE from_team_id IS NOT NULL';
  END IF;

  IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_name = 'trades' AND column_name = 'to_team_id'
    )
  THEN
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_trades_to_team ON trades(to_team_id) WHERE to_team_id IS NOT NULL';
  END IF;
END $$;

-- Index composite pour optimiser getTeamByPlayerAndSeason
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_team_players_player_season 
ON team_players(player_id, team_id) 
WHERE player_id IS NOT NULL AND team_id IS NOT NULL AND until IS NULL;

-- Commentaires pour documentation des optimisations
COMMENT ON INDEX idx_games_creator_created_at IS 'Optimise getGamesByUser - requêtes par créateur triées par date';
COMMENT ON INDEX idx_games_invitation_code IS 'Optimise joinGame - recherche par code invitation';
COMMENT ON INDEX idx_participants_user_selection_time IS 'Optimise collectUserGames - participations par utilisateur';
COMMENT ON INDEX idx_draft_picks_draft_player IS 'Optimise validatePlayerAvailable - évite sélections doubles';
COMMENT ON INDEX idx_scores_timestamp_desc IS 'Optimise leaderboard - scores récents en premier';
COMMENT ON INDEX idx_players_username_gin IS 'Optimise recherche de joueurs par nom utilisateur';
COMMENT ON INDEX idx_players_nickname_gin IS 'Optimise recherche de joueurs par surnom';
COMMENT ON INDEX idx_games_active_status IS 'Optimise findAvailableGames - index partiel pour games actives';
COMMENT ON INDEX idx_team_players_player_season IS 'Optimise getTeamByPlayerAndSeason - lookup direct';

-- Mise à jour des statistiques pour le query planner
ANALYZE games;
ANALYZE game_participants;
ANALYZE draft_picks;
ANALYZE scores;
ANALYZE team_players;
ANALYZE players;
ANALYZE game_region_rules;
ANALYZE notifications;
ANALYZE trades;

-- Validation des index créés
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE indexname LIKE 'idx_%'
ORDER BY tablename, indexname;
