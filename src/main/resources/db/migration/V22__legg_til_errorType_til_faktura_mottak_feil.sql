CREATE TYPE error_type AS ENUM (
    'MANGLENDE_OPPLYSNINGER',
    'FAKTURA_FINNES_IKKE'
);

CREATE CAST (character varying as error_type) WITH INOUT AS IMPLICIT;

ALTER TABLE faktura_mottak_feil ADD COLUMN error_type error_type;