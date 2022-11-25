CREATE TYPE fakturaserie_status AS ENUM (
    'OPPRETTET',
    'UNDER_BESTILLING',
    'KANSELLERT',
    'FERDIG'
);
CREATE CAST (character varying as fakturaserie_status) WITH INOUT AS IMPLICIT;

CREATE TYPE fakturaserie_intervall AS ENUM (
    'MANEDLIG',
    'KVARTAL'
);
CREATE CAST (character varying as fakturaserie_intervall) WITH INOUT AS IMPLICIT;


CREATE TYPE faktura_status AS ENUM (
    'OPPRETTET',
    'BESTILLT',
    'KANSELLERT'
);
CREATE CAST (character varying as faktura_status) WITH INOUT AS IMPLICIT;

CREATE TABLE fakturaserie
(
    id                             SERIAL PRIMARY KEY,
    vedtaks_id                     VARCHAR(20) UNIQUE NOT NULL,
    faktura_gjelder                VARCHAR(240)       NOT NULL,
    fodselsnummer                  NUMERIC(11)        NOT NULL,
    fullmektig_fodselsnummer       NUMERIC(11),
    fullmektig_organisasjonsnummer VARCHAR(20),
    fullmektig_kontaktperson       VARCHAR,
    referanse_bruker               VARCHAR            NOT NULL,
    referanse_nav                  VARCHAR            NOT NULL,
    startdato                      DATE,
    sluttdato                      DATE,
    status                         fakturaserie_status,
    intervall                      fakturaserie_intervall,
    opprettet_Tidspunkt            DATE
);

CREATE TABLE faktura
(
    id              SERIAL PRIMARY KEY,
    fakturaserie_id INT NOT NULL,
    dato_bestilt    DATE,
    status          faktura_status,
    CONSTRAINT fk_fakturaserie FOREIGN KEY (fakturaserie_id) REFERENCES fakturaserie (id)
);

CREATE TABLE faktura_linje
(
    id          SERIAL PRIMARY KEY,
    faktura_id  INT NOT NULL,
    periode_fra DATE,
    periode_til DATE,
    beskrivelse VARCHAR(240) NOT NULL,
    belop       NUMERIC(10, 2),
    CONSTRAINT fk_faktura FOREIGN KEY (faktura_id) REFERENCES faktura (id)
);

