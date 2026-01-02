-- Ensure games metadata exists before V11/V15 seeds

ALTER TABLE games
  ADD COLUMN IF NOT EXISTS description VARCHAR(500),
  ADD COLUMN IF NOT EXISTS invitation_code VARCHAR(10);

CREATE UNIQUE INDEX IF NOT EXISTS uq_games_invitation_code
  ON games(invitation_code)
  WHERE invitation_code IS NOT NULL;
