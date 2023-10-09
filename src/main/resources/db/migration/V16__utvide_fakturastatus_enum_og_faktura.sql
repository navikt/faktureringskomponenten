ALTER TYPE faktura_status
    ADD VALUE 'MANGLENDE_INNBETALING';
ALTER TYPE faktura_status
    ADD VALUE 'INNE_I_OEBS';
ALTER TYPE faktura_status
    ADD VALUE 'FEIL';

ALTER TABLE faktura
    ADD COLUMN sist_oppdatert DATE;

ALTER TABLE faktura_mottatt
    RENAME TO ekstern_faktura_status
