CREATE TABLE faktura_mottak_feil
(
    id                      SERIAL PRIMARY KEY,
    feil_melding            VARCHAR,
    kafka_melding           VARCHAR,
    vedtaks_id              VARCHAR(20),
    faktura_referanse_nr    VARCHAR(20),
    kafka_offset            BIGINT
);
