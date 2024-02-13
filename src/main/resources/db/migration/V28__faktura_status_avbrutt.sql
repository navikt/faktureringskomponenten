ALTER TYPE faktura_status
    ADD VALUE 'AVBRUTT';

commit;

update faktura
set status = 'AVBRUTT'
where status = 'KANSELLERT';