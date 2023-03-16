CREATE TYPE innbetalingstype AS ENUM (
    'TRYGDEAVGIFT'
);
CREATE CAST (character varying as innbetalingstype) WITH INOUT AS IMPLICIT;

ALTER TABLE fakturaserie ALTER COLUMN faktura_gjelder TYPE innbetalingstype USING 'TRYGDEAVGIFT';
ALTER TABLE fakturaserie RENAME COLUMN faktura_gjelder TO faktura_gjelder_innbetalingstype;