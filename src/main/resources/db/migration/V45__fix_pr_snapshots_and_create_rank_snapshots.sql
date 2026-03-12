-- Migration V45: Deux corrections de schéma
--
-- 1. pr_snapshots (V22 legacy): manque les colonnes id et pr_value attendues par PrSnapshot entity
-- 2. rank_snapshots (V34 nouveau): RankSnapshotEntity renommée pour éviter le double-mapping

-- ============================================================
-- 1. Compléter pr_snapshots avec les colonnes manquantes
-- ============================================================
ALTER TABLE pr_snapshots
    ADD COLUMN IF NOT EXISTS id         UUID    DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS pr_value   INTEGER NOT NULL DEFAULT 0;

-- Retirer les DEFAULT dynamiques
ALTER TABLE pr_snapshots ALTER COLUMN id DROP DEFAULT;

-- ============================================================
-- 2. Créer rank_snapshots pour RankSnapshotEntity
-- ============================================================
CREATE TABLE IF NOT EXISTS rank_snapshots (
    id            UUID         NOT NULL,
    player_id     UUID         NOT NULL,
    region        VARCHAR(10)  NOT NULL,
    rank          INTEGER      NOT NULL,
    pr_value      INTEGER      NOT NULL DEFAULT 0,
    snapshot_date DATE         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_rank_snapshots_player_region_date
        UNIQUE (player_id, region, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_rank_snapshots_player_region_date
    ON rank_snapshots (player_id, region, snapshot_date DESC);
