
ALTER TYPE faktura_status
    ADD VALUE 'MANGLENDE_INNBETALING';
ALTER TYPE faktura_status
    ADD VALUE 'INNE_I_OEBS';
ALTER TYPE faktura_status
    ADD VALUE 'FEIL';

ALTER TABLE faktura
    DROP COLUMN innbetalt_belop;

ALTER TABLE faktura
    ADD COLUMN sist_oppdatert DATE;

ALTER TABLE faktura_mottatt
    RENAME TO ekstern_faktura_status;

ALTER TABLE ekstern_faktura_status RENAME COLUMN faktura_referanse_nr TO faktura_id;

ALTER TABLE ekstern_faktura_status
    DROP CONSTRAINT fk_faktura_referanse_nr;

ALTER TABLE ekstern_faktura_status
    ADD CONSTRAINT fk_faktura_id
        FOREIGN KEY (faktura_id) REFERENCES faktura(id)
            ON DELETE CASCADE;

ALTER TABLE faktura_mottak_feil
    DROP COLUMN referanse;


