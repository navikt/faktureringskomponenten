ALTER TYPE faktura_status
    ADD VALUE 'AVBRUTT';

update faktura
set status = 'AVBRUTT'
where status = 'KANSELLERT'