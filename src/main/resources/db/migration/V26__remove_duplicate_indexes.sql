-- =====================================================
-- V26: Remove duplicate indexes
-- PERFORMANCE: Clean up redundant indexes
-- =====================================================

-- game_participants: keep idx_game_participants_game, drop duplicate
DROP INDEX IF EXISTS public.idx_game_participant_game;

-- game_participants: keep idx_game_participants_user, drop duplicate
DROP INDEX IF EXISTS public.idx_game_participant_user;

-- scores: keep idx_scores_season, drop duplicate
DROP INDEX IF EXISTS public.idx_score_season;

-- team_players: keep idx_team_players_active, drop duplicate
DROP INDEX IF EXISTS public.idx_team_player_active;
