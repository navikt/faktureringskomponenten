ALTER TABLE faktura ADD COLUMN ekstern_faktura_nummer varchar(40);

ALTER TABLE ekstern_faktura_status DROP COLUMN faktura_nummer;