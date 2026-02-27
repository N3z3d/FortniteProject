-- V33: Simultaneous-draft windows and selections
-- draft_async_windows: one per pick slot, tracks submission deadline and status.
-- draft_async_selections: one per participant per window (anonymous until resolution).

CREATE TABLE IF NOT EXISTS draft_async_windows (
    id             UUID         NOT NULL,
    draft_id       UUID         NOT NULL,
    slot           VARCHAR(50)  NOT NULL,
    deadline       TIMESTAMP    NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    total_expected INT          NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS draft_async_selections (
    id             UUID      NOT NULL,
    window_id      UUID      NOT NULL,
    participant_id UUID      NOT NULL,
    player_id      UUID      NOT NULL,
    submitted_at   TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (window_id, participant_id)
);
