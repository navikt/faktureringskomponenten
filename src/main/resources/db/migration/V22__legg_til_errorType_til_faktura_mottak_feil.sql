
ALTER TABLE faktura_mottak_feil ADD COLUMN error_type VARCHAR(40);
ALTER TABLE faktura_mottak_feil ALTER COLUMN faktura_referanse_nr TYPE VARCHAR(40);