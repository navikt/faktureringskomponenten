# Felles beregning av totalbeløp

## Status
Akseptert

## Kontekst
Vi trenger å kunne gjøre en totalberegning av faktureringsbeløp for å finne differansen ved en årsavregning.
Beregningen slik den er nå skjer basert på fakturalinjer.
Dersom vi kun trenger å komme fram til en totalsum per år trenger vi ikke å ta hensyn til fakturalinjer og oppdeling av faktura.
Dersom vi skal flytte beregning for reduserte måneder til API vil dette kreve vesentlig refaktorering. Siden vi har mye logikk for dette i oppdeling av fakturalinjer.

Vi opprettet først et nytt endepunkt for å eksponere logikk for beregning av beløp for perioder, for å unngå duplisering av kode.
Vi var ikke fornøyd med alle de ekstra kallene.
Vi vurderte å lage et bibliotek, men forventer ingen endring i denne koden, og det ville da ikke være verdt innsatsen.

## Beslutning
Vi har kopiert koden for beregning av totalbeløp for en gitt tidsperiode i melosys-api.

## Konsekvenser
Vi må vedlikeholde beregning fra BeløpBeregner 2 steder: her og i melosys-api.

## Referanser
https://jira.adeo.no/browse/MELOSYS-6570
