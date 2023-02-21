DROP TABLE IF EXISTS faktura_mottak_feil;
CREATE TABLE faktura_mottak_feil
(
    id                      SERIAL PRIMARY KEY,
    error                   VARCHAR,
    kafka_melding           VARCHAR,
    vedtaks_id              VARCHAR(20),
    faktura_referanse_nr    VARCHAR(20),
    kafka_offset            BIGINT,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
