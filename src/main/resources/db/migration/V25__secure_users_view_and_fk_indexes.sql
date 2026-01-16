-- =====================================================
-- V25: Secure users view + Missing FK indexes
-- SECURITY: Hide password column from API
-- PERFORMANCE: Add indexes on foreign keys
-- =====================================================

-- Create secure view excluding password
CREATE OR REPLACE VIEW public.users_public AS
SELECT
    id,
    username,
    email,
    role,
    current_season,
    created_at,
    updated_at
FROM public.users;

-- Grant access to the view (PostgreSQL standard, Supabase specific)
-- Note: In H2/standard PG this may need adjustment
-- GRANT SELECT ON public.users_public TO anon, authenticated;

-- =====================================================
-- Add missing indexes on foreign keys
-- =====================================================

-- draft_async_selections
CREATE INDEX IF NOT EXISTS idx_draft_async_selections_participant_id
ON public.draft_async_selections(participant_id);

CREATE INDEX IF NOT EXISTS idx_draft_async_selections_player_id
ON public.draft_async_selections(player_id);

-- draft_orders
CREATE INDEX IF NOT EXISTS idx_draft_orders_draft_id
ON public.draft_orders(draft_id);

CREATE INDEX IF NOT EXISTS idx_draft_orders_participant_id
ON public.draft_orders(participant_id);

-- pr_snapshots
CREATE INDEX IF NOT EXISTS idx_pr_snapshots_run_id
ON public.pr_snapshots(run_id);

-- trade_offered_players
CREATE INDEX IF NOT EXISTS idx_trade_offered_players_player_id
ON public.trade_offered_players(player_id);

-- trade_requested_players
CREATE INDEX IF NOT EXISTS idx_trade_requested_players_player_id
ON public.trade_requested_players(player_id);

-- trades - missing FK indexes
CREATE INDEX IF NOT EXISTS idx_trades_original_trade_id
ON public.trades(original_trade_id);

CREATE INDEX IF NOT EXISTS idx_trades_from_user_id
ON public.trades(from_user_id);

CREATE INDEX IF NOT EXISTS idx_trades_to_user_id
ON public.trades(to_user_id);

CREATE INDEX IF NOT EXISTS idx_trades_player_in_id
ON public.trades(player_in_id);

CREATE INDEX IF NOT EXISTS idx_trades_player_out_id
ON public.trades(player_out_id);
