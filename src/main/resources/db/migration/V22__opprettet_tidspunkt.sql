ALTER TABLE fakturaserie
    ALTER COLUMN opprettet_tidspunkt TYPE TIMESTAMP,
    ALTER COLUMN opprettet_tidspunkt SET NOT NULL;

ALTER TABLE faktura
    ADD COLUMN opprettet_tidspunkt TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE faktura
    ALTER COLUMN opprettet_tidspunkt DROP DEFAULT;
ALTER TABLE faktura
    ALTER COLUMN opprettet_tidspunkt SET NOT NULL;

ALTER TABLE faktura_linje
    ADD COLUMN opprettet_tidspunkt TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE faktura_linje
    ALTER COLUMN opprettet_tidspunkt DROP DEFAULT;
ALTER TABLE faktura_linje
    ALTER COLUMN opprettet_tidspunkt SET NOT NULL;

ALTER TABLE ekstern_faktura_status
    ADD COLUMN opprettet_tidspunkt TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE ekstern_faktura_status
    ALTER COLUMN opprettet_tidspunkt DROP DEFAULT;
ALTER TABLE ekstern_faktura_status
    ALTER COLUMN opprettet_tidspunkt SET NOT NULL;

ALTER TABLE faktura_mottak_feil
    ADD COLUMN opprettet_tidspunkt TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE faktura_mottak_feil
    ALTER COLUMN opprettet_tidspunkt DROP DEFAULT;
ALTER TABLE faktura_mottak_feil
    ALTER COLUMN opprettet_tidspunkt SET NOT NULL;
