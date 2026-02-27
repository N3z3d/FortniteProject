CREATE TABLE IF NOT EXISTS player_identity_pipeline (
  id             UUID         NOT NULL,
  player_id      UUID         NOT NULL,
  player_username VARCHAR(255) NOT NULL,
  player_region  VARCHAR(20)  NOT NULL,
  epic_id        VARCHAR(128),
  status         VARCHAR(20)  NOT NULL DEFAULT 'UNRESOLVED',
  confidence_score SMALLINT   NOT NULL DEFAULT 0,
  resolved_by    VARCHAR(255),
  resolved_at    TIMESTAMP,
  rejected_at    TIMESTAMP,
  rejection_reason VARCHAR(512),
  created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
  PRIMARY KEY (id),
  CONSTRAINT uq_player_identity_player_id UNIQUE (player_id)
);

CREATE INDEX IF NOT EXISTS idx_player_identity_status
  ON player_identity_pipeline (status);

CREATE INDEX IF NOT EXISTS idx_player_identity_player_id
  ON player_identity_pipeline (player_id);
