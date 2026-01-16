-- =====================================================
-- V28: Optimize RLS policies for performance
-- PERFORMANCE: Use (select auth.uid()) instead of auth.uid()
-- This prevents re-evaluation for each row
-- =====================================================

-- Note: These policies use Supabase auth.uid() function
-- They only apply in Supabase environment, not H2/PostgreSQL dev

-- Notifications: Users can only see their own
-- DROP POLICY IF EXISTS "Users can view own notifications" ON public.notifications;
-- CREATE POLICY "Users can view own notifications"
-- ON public.notifications FOR SELECT
-- USING (user_id = (SELECT auth.uid()));

-- Users: Users can only see their own profile
-- DROP POLICY IF EXISTS "Users can view own profile" ON public.users;
-- CREATE POLICY "Users can view own profile"
-- ON public.users FOR SELECT
-- USING (id = (SELECT auth.uid()));
