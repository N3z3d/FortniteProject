ALTER TABLE player_identity_pipeline
  ADD COLUMN corrected_username VARCHAR(255),
  ADD COLUMN corrected_region VARCHAR(20),
  ADD COLUMN corrected_by VARCHAR(255),
  ADD COLUMN corrected_at TIMESTAMP;
