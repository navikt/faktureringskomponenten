ALTER TABLE fakturaserie RENAME COLUMN vedtaks_id TO referanse;

ALTER TABLE fakturaserie
    ALTER COLUMN referanse TYPE VARCHAR(40),
    ALTER COLUMN referanse SET NOT NULL;
