ALTER TABLE fakturaserie RENAME COLUMN vedtaks_id TO referanse_id;

ALTER TABLE fakturaserie
    ALTER COLUMN referanse_id TYPE VARCHAR(40),
    ALTER COLUMN referanse_id SET NOT NULL;
