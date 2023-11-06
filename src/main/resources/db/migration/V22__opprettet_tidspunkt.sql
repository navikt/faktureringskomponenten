ALTER TABLE fakturaserie
    ALTER COLUMN opprettet_tidspunkt TYPE TIMESTAMP,
    ALTER COLUMN opprettet_tidspunkt SET NOT NULL;
ALTER TABLE faktura
    ADD COLUMN opprettet_tidspunkt TIMESTAMP NOT NULL;
