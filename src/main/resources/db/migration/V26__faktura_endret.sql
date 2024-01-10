ALTER TABLE faktura
    ADD COLUMN endret_av VARCHAR(20),
    ADD COLUMN endret_tidspunkt TIMESTAMP,
    DROP COLUMN sist_oppdatert;

UPDATE faktura
    set endret_tidspunkt = opprettet_tidspunkt
    where endret_tidspunkt is null;
UPDATE faktura
    set endret_av = opprettet_av
    where endret_av is null;