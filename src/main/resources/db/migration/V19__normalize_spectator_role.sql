-- Normalize legacy role value after adding SPECTATOR to user_role enum
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role')
     AND EXISTS (
       SELECT 1
       FROM pg_type t
       JOIN pg_enum e ON t.oid = e.enumtypid
       WHERE t.typname = 'user_role' AND e.enumlabel = 'SPECTATOR'
     )
  THEN
    UPDATE users SET role = 'SPECTATOR' WHERE role = 'SPECTATEUR';
  END IF;
END $$;
