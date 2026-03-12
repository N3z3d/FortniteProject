-- Migration V43: Aligne draft_async_windows avec le schéma de l'entité JPA
-- V20 a créé la table avec window_no/region_slot/opens_at/closes_at
-- L'entité attend slot/deadline/total_expected (schéma V33)

-- 1. Désactiver RLS
ALTER TABLE public.draft_async_windows DISABLE ROW LEVEL SECURITY;

-- 2. Supprimer l'index unique sur les anciens colonnes
DROP INDEX IF EXISTS uq_draft_async_windows_draft_window;

-- 3. Supprimer les colonnes de l'ancienne structure (table vide en environnement frais)
ALTER TABLE public.draft_async_windows
    DROP COLUMN IF EXISTS window_no,
    DROP COLUMN IF EXISTS region_slot,
    DROP COLUMN IF EXISTS opens_at,
    DROP COLUMN IF EXISTS closes_at;

-- 4. Ajouter les colonnes du nouveau schéma
ALTER TABLE public.draft_async_windows
    ADD COLUMN IF NOT EXISTS slot          VARCHAR(50) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS deadline      TIMESTAMP   NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS total_expected INT        NOT NULL DEFAULT 0;

-- Retirer les DEFAULT (renseignés par l'application)
ALTER TABLE public.draft_async_windows
    ALTER COLUMN slot          DROP DEFAULT,
    ALTER COLUMN deadline      DROP DEFAULT,
    ALTER COLUMN total_expected DROP DEFAULT;

-- 5. Réactiver RLS
ALTER TABLE public.draft_async_windows ENABLE ROW LEVEL SECURITY;
