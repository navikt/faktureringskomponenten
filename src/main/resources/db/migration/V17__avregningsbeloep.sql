ALTER TABLE faktura_linje
    ADD COLUMN avregning_forrige_beloep NUMERIC(10, 2),
    ADD COLUMN avregning_nytt_beloep NUMERIC(10, 2);
