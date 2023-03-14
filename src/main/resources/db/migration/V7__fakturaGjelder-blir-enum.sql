CREATE TYPE faktura_gjelder AS ENUM (
    'TRY'
);
CREATE CAST (character varying as faktura_gjelder) WITH INOUT AS IMPLICIT;

ALTER TABLE fakturaserie ALTER COLUMN faktura_gjelder TYPE faktura_gjelder USING 'TRYGDEAVGIFT';