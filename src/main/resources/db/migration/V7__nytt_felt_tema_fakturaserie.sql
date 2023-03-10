CREATE TYPE fakturaserie_tema AS ENUM (
    'TRY'
);
CREATE CAST (character varying as fakturaserie_tema) WITH INOUT AS IMPLICIT;

ALTER TABLE fakturaserie ADD COLUMN tema fakturaserie_tema;