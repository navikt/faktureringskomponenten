
CREATE TYPE faktura_mottatt_status AS ENUM (
    'MANGLENDE_BETALING',
    'INNE_I_OEBS',
    'FEIL'
);
CREATE CAST (character varying as faktura_mottatt_status) WITH INOUT AS IMPLICIT;

CREATE TABLE faktura_mottatt
(
    id                      SERIAL PRIMARY KEY,
    faktura_referanse_nr    VARCHAR(20) NOT NULL,
    faktura_nummer          VARCHAR(20),
    dato                    DATE,
    status                  faktura_mottatt_status,
    faktura_belop           NUMERIC(10, 2),
    ubetalt_belop           NUMERIC(10, 2),
    feilmelding             VARCHAR,
    sendt                   BOOLEAN
);