-- Backfill canonical creator participants for non-deleted games.
-- Audit equivalent (read-only):
-- SELECT g.id
-- FROM games g
-- WHERE g.deleted_at IS NULL
--   AND g.creator_id IS NOT NULL
--   AND NOT EXISTS (
--     SELECT 1 FROM game_participants gp
--     WHERE gp.game_id = g.id AND gp.user_id = g.creator_id
--   );

UPDATE game_participants gp
SET is_creator = TRUE,
    draft_order = COALESCE(
      gp.draft_order,
      (
        SELECT COALESCE(MAX(existing_gp.draft_order), 0) + 1
        FROM game_participants existing_gp
        WHERE existing_gp.game_id = g.id
      )
    ),
    joined_at = COALESCE(gp.joined_at, g.created_at, CURRENT_TIMESTAMP)
FROM games g
WHERE gp.game_id = g.id
  AND gp.user_id = g.creator_id
  AND g.deleted_at IS NULL
  AND g.creator_id IS NOT NULL
  AND COALESCE(gp.is_creator, FALSE) = FALSE;

INSERT INTO game_participants (id, game_id, user_id, draft_order, joined_at, is_creator)
SELECT
  uuid_generate_v4(),
  g.id,
  g.creator_id,
  (
    SELECT COALESCE(MAX(existing_gp.draft_order), 0) + 1
    FROM game_participants existing_gp
    WHERE existing_gp.game_id = g.id
  ),
  COALESCE(g.created_at, CURRENT_TIMESTAMP),
  TRUE
FROM games g
WHERE g.deleted_at IS NULL
  AND g.creator_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM game_participants gp
    WHERE gp.game_id = g.id AND gp.user_id = g.creator_id
  );
