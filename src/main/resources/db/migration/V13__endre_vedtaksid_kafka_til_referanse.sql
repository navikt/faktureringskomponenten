ALTER TABLE faktura_mottak_feil RENAME COLUMN vedtaks_id TO referanse;

ALTER TABLE faktura_mottak_feil
    ALTER COLUMN referanse TYPE VARCHAR(40),
    ALTER COLUMN referanse SET NOT NULL;
