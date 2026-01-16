-- =====================================================
-- V27: Fix security definer view + flyway RLS
-- SECURITY: Final security fixes
-- =====================================================

-- Drop and recreate view with SECURITY INVOKER (default)
DROP VIEW IF EXISTS public.users_public;

CREATE VIEW public.users_public AS
SELECT
    id,
    username,
    email,
    role,
    current_season,
    created_at,
    updated_at
FROM public.users;

-- Enable RLS on flyway_schema_history (system table)
-- Note: This may not work on all PostgreSQL setups
-- ALTER TABLE public.flyway_schema_history ENABLE ROW LEVEL SECURITY;
