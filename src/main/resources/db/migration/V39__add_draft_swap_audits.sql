CREATE TABLE IF NOT EXISTS draft_swap_audits (
  id UUID PRIMARY KEY,
  draft_id UUID NOT NULL,
  participant_id UUID NOT NULL,
  player_out_id UUID NOT NULL,
  player_in_id UUID NOT NULL,
  occurred_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_draft_swap_audits_draft_id
  ON draft_swap_audits (draft_id);
