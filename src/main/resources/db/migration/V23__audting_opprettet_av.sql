ALTER TABLE fakturaserie
    ADD COLUMN opprettet_av VARCHAR(20) DEFAULT 'N/A';
ALTER TABLE fakturaserie
    ALTER COLUMN opprettet_av DROP DEFAULT;
ALTER TABLE fakturaserie
    ALTER COLUMN opprettet_av SET NOT NULL;

ALTER TABLE faktura
    ADD COLUMN opprettet_av VARCHAR(20) DEFAULT 'N/A';
ALTER TABLE faktura
    ALTER COLUMN opprettet_av DROP DEFAULT;
ALTER TABLE faktura
    ALTER COLUMN opprettet_av SET NOT NULL;

ALTER TABLE faktura_linje
    ADD COLUMN opprettet_av VARCHAR(20) DEFAULT 'N/A';
ALTER TABLE faktura_linje
    ALTER COLUMN opprettet_av DROP DEFAULT;
ALTER TABLE faktura_linje
    ALTER COLUMN opprettet_av SET NOT NULL;
