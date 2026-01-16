-- Add UNKNOWN to region enum and enforce unique nickname when missing.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_enum e ON e.enumtypid = t.oid
    WHERE t.typname = 'region_enum' AND e.enumlabel = 'UNKNOWN'
  ) THEN
    ALTER TYPE region_enum ADD VALUE 'UNKNOWN';
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
    WHERE t.relname = 'players'
      AND c.contype = 'u'
      AND a.attname = 'nickname'
  ) THEN
    ALTER TABLE players ADD CONSTRAINT players_nickname_unique UNIQUE (nickname);
  END IF;
END $$;
