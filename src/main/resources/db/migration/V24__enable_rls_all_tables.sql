-- =====================================================
-- V24: Enable RLS on all public tables
-- SECURITY: Protect data from unauthorized access
-- =====================================================

-- Ensure auth schema and uid() exist for non-Supabase environments.
CREATE SCHEMA IF NOT EXISTS auth;

DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM pg_proc p
      JOIN pg_namespace n ON n.oid = p.pronamespace
      WHERE n.nspname = 'auth' AND p.proname = 'uid'
  ) THEN
    EXECUTE 'CREATE FUNCTION auth.uid() RETURNS uuid LANGUAGE sql STABLE AS ''SELECT NULL::uuid''';
  END IF;
END $$;

-- Enable RLS on all tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.players ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scores ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scrape_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.team_players ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trades ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.drafts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.draft_picks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.games ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.game_region_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.game_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.game_participant_players ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.draft_orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trade_offered_players ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trade_requested_players ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.draft_async_windows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.draft_async_selections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ingestion_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pr_snapshots ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- RLS Policies for SERVICE ROLE (backend API)
-- Backend uses service_role key which bypasses RLS
-- These policies are for anon/authenticated users via PostgREST
-- =====================================================

-- PLAYERS: Public read, no direct write
CREATE POLICY "Players are viewable by everyone"
ON public.players FOR SELECT
USING (true);

-- SCORES: Public read, no direct write
CREATE POLICY "Scores are viewable by everyone"
ON public.scores FOR SELECT
USING (true);

-- PR_SNAPSHOTS: Public read, no direct write
CREATE POLICY "PR snapshots are viewable by everyone"
ON public.pr_snapshots FOR SELECT
USING (true);

-- GAMES: Public read (for listing), authenticated for participation
CREATE POLICY "Games are viewable by everyone"
ON public.games FOR SELECT
USING (true);

-- TEAMS: Public read
CREATE POLICY "Teams are viewable by everyone"
ON public.teams FOR SELECT
USING (true);

-- DRAFTS: Public read
CREATE POLICY "Drafts are viewable by everyone"
ON public.drafts FOR SELECT
USING (true);

-- DRAFT_PICKS: Public read
CREATE POLICY "Draft picks are viewable by everyone"
ON public.draft_picks FOR SELECT
USING (true);

-- DRAFT_ORDERS: Public read
CREATE POLICY "Draft orders are viewable by everyone"
ON public.draft_orders FOR SELECT
USING (true);

-- GAME_PARTICIPANTS: Public read
CREATE POLICY "Game participants are viewable by everyone"
ON public.game_participants FOR SELECT
USING (true);

-- GAME_PARTICIPANT_PLAYERS: Public read
CREATE POLICY "Game participant players are viewable by everyone"
ON public.game_participant_players FOR SELECT
USING (true);

-- GAME_REGION_RULES: Public read
CREATE POLICY "Game region rules are viewable by everyone"
ON public.game_region_rules FOR SELECT
USING (true);

-- TEAM_PLAYERS: Public read
CREATE POLICY "Team players are viewable by everyone"
ON public.team_players FOR SELECT
USING (true);

-- TRADES: Public read (for transparency)
CREATE POLICY "Trades are viewable by everyone"
ON public.trades FOR SELECT
USING (true);

-- TRADE_OFFERED_PLAYERS: Public read
CREATE POLICY "Trade offered players are viewable by everyone"
ON public.trade_offered_players FOR SELECT
USING (true);

-- TRADE_REQUESTED_PLAYERS: Public read
CREATE POLICY "Trade requested players are viewable by everyone"
ON public.trade_requested_players FOR SELECT
USING (true);

-- DRAFT_ASYNC_WINDOWS: Public read
CREATE POLICY "Draft async windows are viewable by everyone"
ON public.draft_async_windows FOR SELECT
USING (true);

-- DRAFT_ASYNC_SELECTIONS: Public read
CREATE POLICY "Draft async selections are viewable by everyone"
ON public.draft_async_selections FOR SELECT
USING (true);

-- NOTIFICATIONS: Users can only see their own
CREATE POLICY "Users can view own notifications"
ON public.notifications FOR SELECT
USING (auth.uid() = user_id);

-- USERS: Users can only see their own profile (password excluded via view)
CREATE POLICY "Users can view own profile"
ON public.users FOR SELECT
USING (auth.uid() = id);

-- SCRAPE_RUNS: Admin only (no public access)
CREATE POLICY "Scrape runs are admin only"
ON public.scrape_runs FOR SELECT
USING (false);

-- INGESTION_RUNS: Admin only (no public access)
CREATE POLICY "Ingestion runs are admin only"
ON public.ingestion_runs FOR SELECT
USING (false);
