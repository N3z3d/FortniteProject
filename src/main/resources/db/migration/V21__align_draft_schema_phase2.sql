-- Draft schema alignment (phase 2, constraints and safe backfills)

UPDATE draft_picks dp
SET region_slot = p.region::text::draft_region_slot
FROM players p
WHERE dp.player_id = p.id
  AND dp.region_slot IS NULL
  AND p.region::text IN ('EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME');

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM draft_picks WHERE region_slot IS NULL) THEN
    ALTER TABLE draft_picks ALTER COLUMN region_slot SET NOT NULL;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM pg_indexes
      WHERE schemaname = 'public'
        AND indexname = 'uq_draft_picks_draft_player'
    )
  THEN
    IF NOT EXISTS (
        SELECT 1
        FROM draft_picks
        GROUP BY draft_id, player_id
        HAVING COUNT(*) > 1
      )
    THEN
      CREATE UNIQUE INDEX uq_draft_picks_draft_player
      ON draft_picks(draft_id, player_id);
    END IF;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM pg_indexes
      WHERE schemaname = 'public'
        AND indexname = 'uq_game_participants_game_draft_order'
    )
  THEN
    IF NOT EXISTS (
        SELECT 1
        FROM game_participants
        WHERE draft_order IS NOT NULL
        GROUP BY game_id, draft_order
        HAVING COUNT(*) > 1
      )
    THEN
      CREATE UNIQUE INDEX uq_game_participants_game_draft_order
      ON game_participants(game_id, draft_order)
      WHERE draft_order IS NOT NULL;
    END IF;
  END IF;
END $$;
