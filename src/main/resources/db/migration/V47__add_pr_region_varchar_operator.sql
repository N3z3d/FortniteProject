-- Add equality operator between pr_region and character varying
-- Required for Hibernate 6 batch UPDATE WHERE clauses that bind enum values as character varying
-- Error fixed: "operator does not exist: pr_region = character varying"
CREATE FUNCTION pr_region_eq_varchar(pr_region, character varying)
RETURNS boolean AS $$
  SELECT $1 = $2::pr_region;
$$ LANGUAGE SQL IMMUTABLE;

CREATE OPERATOR = (
  LEFTARG = pr_region,
  RIGHTARG = character varying,
  PROCEDURE = pr_region_eq_varchar
);
