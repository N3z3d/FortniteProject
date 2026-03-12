-- Story 5.2: Draft participant trade proposals (FR-34, FR-35)
CREATE TABLE IF NOT EXISTS draft_participant_trades (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL,
    proposer_participant_id UUID NOT NULL,
    target_participant_id UUID NOT NULL,
    player_from_proposer_id UUID NOT NULL,
    player_from_target_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    proposed_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_draft_participant_trades_draft_status
    ON draft_participant_trades (draft_id, status);
