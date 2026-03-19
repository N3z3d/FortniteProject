-- Revert V47: drop the custom operator and function
DROP OPERATOR IF EXISTS =(pr_region, character varying);
DROP FUNCTION IF EXISTS pr_region_eq_varchar(pr_region, character varying);

-- Revert V46: drop the implicit cast
DROP CAST IF EXISTS (varchar AS pr_region);

-- Convert pr_snapshots.region from pr_region enum to varchar
-- This fixes Hibernate 6 batch UPDATE WHERE clause type mismatch
-- "operator does not exist: pr_region = character varying"
ALTER TABLE pr_snapshots ALTER COLUMN region TYPE varchar USING region::varchar;
