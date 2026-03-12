CREATE TABLE IF NOT EXISTS team_score_deltas (
  id              UUID         NOT NULL,
  game_id         UUID         NOT NULL,
  participant_id  UUID         NOT NULL,
  period_start    DATE         NOT NULL,
  period_end      DATE         NOT NULL,
  delta_pr        INTEGER      NOT NULL DEFAULT 0,
  computed_at     TIMESTAMP    NOT NULL,
  CONSTRAINT pk_team_score_deltas PRIMARY KEY (id),
  CONSTRAINT uq_team_score_delta UNIQUE (game_id, participant_id, period_start, period_end)
);

CREATE INDEX IF NOT EXISTS idx_team_score_deltas_game
  ON team_score_deltas (game_id, delta_pr DESC);
