-- Migration V42: Aligne draft_async_selections avec le schéma de l'entité JPA
-- V20 a créé la table avec window_no (INTEGER) ; V33 attendait window_id (UUID FK)
-- Cette migration transforme la table vers le schéma canonique de l'entité

-- 1. Désactiver RLS temporairement pour altérer la table
ALTER TABLE public.draft_async_selections DISABLE ROW LEVEL SECURITY;

-- 2. Supprimer l'ancienne contrainte unique et les FKs inutiles
DROP INDEX IF EXISTS uq_draft_async_selections_unique;
ALTER TABLE public.draft_async_selections
    DROP CONSTRAINT IF EXISTS draft_async_selections_draft_id_fkey;

-- 3. Supprimer les colonnes de l'ancienne structure (table vide en environnement frais)
ALTER TABLE public.draft_async_selections
    DROP COLUMN IF EXISTS draft_id,
    DROP COLUMN IF EXISTS window_no,
    DROP COLUMN IF EXISTS region_slot;

-- 4. Ajouter la colonne window_id (UUID FK vers draft_async_windows)
ALTER TABLE public.draft_async_selections
    ADD COLUMN IF NOT EXISTS window_id UUID NOT NULL DEFAULT gen_random_uuid();

-- Retirer le DEFAULT une fois la colonne créée (elle sera renseignée par l'application)
ALTER TABLE public.draft_async_selections
    ALTER COLUMN window_id DROP DEFAULT;

-- 5. Ajouter la FK vers draft_async_windows
ALTER TABLE public.draft_async_selections
    ADD CONSTRAINT fk_das_window
    FOREIGN KEY (window_id) REFERENCES draft_async_windows(id) ON DELETE CASCADE;

-- 6. Ajouter la contrainte unique canonique
ALTER TABLE public.draft_async_selections
    ADD CONSTRAINT uq_das_window_participant
    UNIQUE (window_id, participant_id);

-- 7. Réactiver RLS
ALTER TABLE public.draft_async_selections ENABLE ROW LEVEL SECURITY;
