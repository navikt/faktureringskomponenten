# Opprettelse av faktureringskomponenten

## Status
Akseptert

## Kontekst
Vi har fått i oppdrag å fase ut avgiftssystemet (legacy løsning), for å hjelpe til med avvikling av stormaskin. 
Vi har i samråd med team Løst, som eier en fakturaløsning implementert med Oracle E-Business Suite (OEBS), kommet frem til 
at vi skal koble oss på OEBS-løsningen for fakturering av trygdeavgift i de tilfellene der bruker skal betale avgiften til NAV.

Gjennom dette beslutningsgrunnlaget ønsker vi å komme frem til hvordan samhandling mellom Melosys og OEBS skal foregå, 
og hvem som har ansvar for de ulike funksjonene og som eier de data som er tilknyttet. 

Grunnlaget gjelder i hovedsak:
1) hvilken løsning som skal ha ansvar for at fakturereringen foregår regelmessig i hele vedtaksperioden, og
2) hvem som har ansvar for å beregne endringer for neste faktura ved endringer i midt i en faktureringsperiode eller tilbake i tid.


## Beslutning
Ansvarsfordeling blir følgende:

### Melosys-api
Har ansvar for og eier data for vedtak og perioder det skal betales trygdeavgift for. 
Melosys eier også endringer i vedtaket, herunder endringer i perioder, sats og beløpet som gjelder trygdeavgift.
Melosys sender informasjon om vedtak og trygdeavgift som skal faktureres til faktureringskomponent som faktureringsoppdrag.
Melosys har ansvar for å støtte saksbehandling av melding om manglende innbetaling fra OEBS.

### Faktureringskomponent
- Har ansvar for å holde i gang regelmessig bestilling av faktura for hele vedtaksperioden.
- Sender bestilling av faktura til OEBS.
- Håndterer endringer og motregninger i framtidige faktura opp mot tidligere faktureringsoppdrag.
- Blir satt opp slik at den kan brukes til flere formål og ikke bare til fakturering av trygdeavgift. 

### OEBS
- Sending av faktura til den enkelte bruker, håndtering av innbetalinger og purring av manglende innbetalinger.
OEBS eier all informasjon om det som er fakturert og det som er innbetalt. Dette blir ikke sendt tilbake og blir ikke duplisert slik som i dagens løsning.
Dette medfører at OEBS må sende beskjed om manglende innbetaling etter purringer og frist. 
Dette i henhold til felles gjeldende skatteregler, eller det som er ønskelig fra fagområde.

## Begrunnelse
Vi har også vurdert en løsning hvor team Løst eier hele faktureringsprosessen, noe som ville trolig gi en enklere forvaltning.
Dette ville imidlertid kreve å bygge funksjonalitet for repeterende fakturering i en standard ERP-løsning som i utgangspunktet ikke er laget for den typen oppdrag.
Og funksjonaliteten må lages på nytt når OEBS erstattes som planlagt.

Vi har ansvar for å erstatte funksjonalitet som ligger i Avgiftssystemet og er de som eier behovet.
Det er en fordel at teamet eier og lager ny løsning selv i henhold til teamets plan. Teamet har også fått penger og har kapasitet for å gjøre det.
Team Løst har kommunisert at de har mindre kapasitet i år for å støtte under det behovet pga av andre aktiviteter.

## Konsekvenser
- Generiske faktureringsbegreper må brukes og spesifikke trygdeavgift eller andre lovvalg/medlemskapsbegreper unngås.
- Forvaltningen av faktureringsprosess og løsning er delt opp i 2 team.


## Referanser
https://confluence.adeo.no/display/TEESSI/Beslutningsunderlag+samhandling+Melosys+og+OEBS
