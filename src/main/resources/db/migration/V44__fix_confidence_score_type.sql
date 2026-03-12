-- Migration V44: Corrige le type de confidence_score dans player_identity_pipeline
-- V35 a déclaré SMALLINT mais l'entité JPA attend INTEGER (int4)
-- SMALLINT -> INTEGER est un cast sûr (widening)
ALTER TABLE player_identity_pipeline
    ALTER COLUMN confidence_score TYPE INTEGER;
