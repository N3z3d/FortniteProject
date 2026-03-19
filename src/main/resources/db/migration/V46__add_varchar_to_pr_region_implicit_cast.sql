-- Allow implicit casting of varchar to pr_region enum
-- Required for Hibernate 6 batch UPDATE WHERE clauses that pass enum values as character varying
-- Error fixed: "operator does not exist: pr_region = character varying"
CREATE CAST (varchar AS pr_region) WITH INOUT AS IMPLICIT;
