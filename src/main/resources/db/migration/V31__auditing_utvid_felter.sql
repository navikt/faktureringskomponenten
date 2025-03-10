ALTER TABLE fakturaserie
    ALTER COLUMN opprettet_av TYPE VARCHAR (50);
ALTER TABLE fakturaserie
    ALTER COLUMN endret_av TYPE VARCHAR (50);

ALTER TABLE faktura
    ALTER COLUMN opprettet_av TYPE VARCHAR (50);
ALTER TABLE faktura
    ALTER COLUMN endret_av TYPE VARCHAR (50);

ALTER TABLE faktura_linje
    ALTER COLUMN opprettet_av TYPE VARCHAR (50);

