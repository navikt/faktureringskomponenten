# Eksponere endepunkt for beregning av totalsum.md

## Status
Akseptert

## Kontekst
Vi trenger å kunne gjøre en totalberegning for å finne differansen fra tidligere år ved Årsavregning.
Beregningen slik den er nå skjer basert på fakturalinjer.
Dersom vi kun trenger å komme fram til en totalsum per år trenger vi ikke å ta hensyn til fakturalinjer og oppdeling av faktura.
Dersom vi skal flytte beregning for reduserte måneder til API vil dette kreve vesentlig refaktorering. Siden vi har mye logikk for dette i oppdeling av fakturalinjer.
Vi har også vurdert duplisering, men alle er enige i att det er en dårlig løsning, fordi vi må vedlikeholde beregning 2 steder.
Foreslår derfor å bare sende inn perioder vi skal beregne for, så regner vi ut totalen for en satt tidsperiode. I kontekst av årsberegning blir dette alltid for inneværende år.

## Beslutning
Vi endte opp med å lage ett nytt endepunkt for å eksponere logikk for beregning av beløp for perioder.
ref TotalBeløpController

## Konsekvenser
Konsekvensen slik den er og kommer til å bli er att beregning egentlig ikke nødvendigvis hører til faktureringskomponenten.
Det kan også være litt forvirrende mtp att vi allerede har en beregningsmodul. ref: https://github.com/navikt/melosys-trygdeavgift-beregning

## Referanser
https://jira.adeo.no/browse/MELOSYS-6570