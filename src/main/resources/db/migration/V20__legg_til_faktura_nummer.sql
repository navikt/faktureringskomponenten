ALTER TABLE faktura ADD COLUMN eksternt_fakturanummer varchar(40);

ALTER TABLE ekstern_faktura_status DROP COLUMN faktura_nummer;