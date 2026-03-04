# Claude Code Wrapper: aide-opprett

Opprett dokumentstruktur for en JIRA-sak eller TODO-plan.

**Input:** $ARGUMENTS (alle argumenter etter kommandoen)

## Smart deteksjon

Parse `$ARGUMENTS`:

**JIRA mode:** Hvis første ord starter med `MELOSYS-`
- Eksempel: `/aide-opprett MELOSYS-7890`
- Kjør: `aide-jira-opprett MELOSYS-7890`

**TODO mode (med navn):** Hvis første ord starter med `TODO-` (men ikke kun `TODO`)
- Eksempel: `/aide-opprett TODO-redux-form-migration Flytt alle forms`
- Kjør: `aide-todo-opprett "TODO-redux-form-migration" "Flytt alle forms"`

**TODO mode (autogenerert):** Hvis første ord er kun `TODO`
- Eksempel: `/aide-opprett TODO Flytt forms til React Hook Form`
- Kjør: `aide-todo-opprett "" "Flytt forms til React Hook Form"`

**Feilhåndtering:** Hvis argument mangler eller ugyldig format, vis:
```text
❌ Mangler argument

Bruk:
/aide-opprett MELOSYS-XXXX                    # For JIRA-sak
/aide-opprett TODO-<navn> <beskrivelse>       # TODO med navn
/aide-opprett TODO <beskrivelse>              # TODO autogenerert

Eksempler:
/aide-opprett MELOSYS-7890
/aide-opprett TODO-redux-form-migration Flytt forms fra Redux Form
/aide-opprett TODO Implementere dark mode
```

---

# Opprett dokumentasjon (JIRA eller TODO)

**Formål:** Opprette strukturert dokumentasjon for enten JIRA-sak eller TODO-plan.



---

## JIRA-sak

For å opprette JIRA-dokumentasjon:

STEG 0: VERIFISER ENVIRONMENT-VARIABLER
- Sjekk at MELOSYS_AIDE_INSTALLATION_PATH er satt
- Hvis ikke satt: Gi feilmelding og stopp

STEG 1: KJØR AUTOMATISK SCRIPT
- Kjør kommando: aide-jira-opprett MELOSYS-XXXX
  - Dette script gjør alt automatisk:
    - Henter JIRA-data via API
    - Parser JSON og fyller ut templates fra $MELOSYS_AIDE_INSTALLATION_PATH/core/templates/jira/
    - Oppretter alle 4 filer (1-beskrivelse.md, 2-analyse.md, 3-løsning.md, 4-status.md)
    - Stager filer i git

STEG 2: BESTEM REPORTS-ROOT
- Hvis MELOSYS_AIDE_REPORTS_PATH er satt:
  - Filene ligger i: $MELOSYS_AIDE_REPORTS_PATH/jira/MELOSYS-XXXX/
  - Scriptet hopper over git add (egen repo)
- Hvis IKKE satt:
  - Filene ligger i: $MELOSYS_AIDE_INSTALLATION_PATH/reports/jira/MELOSYS-XXXX/
  - Scriptet har allerede kjørt git add

STEG 3: VERIFISER OUTPUT
- Sjekk at filene ble opprettet:
  - <reports-root>/jira/MELOSYS-XXXX/1-beskrivelse.md
  - <reports-root>/jira/MELOSYS-XXXX/2-analyse.md
  - <reports-root>/jira/MELOSYS-XXXX/3-løsning.md
  - <reports-root>/jira/MELOSYS-XXXX/4-status.md

STEG 4: BEKREFT
- Vis oppsummering:
  - ✅ JIRA-data hentet
  - ✅ 4 filer opprettet
  - ✅ Filer staged i git (hvis i workspace)
  - 📖 Neste steg: Kjør "Analyser kodebasen for MELOSYS-XXXX"

VIKTIG:
- Følg core/docs/WORKFLOWS.md - Fase 1: JIRA-sak workflow
- Følg core/docs/DOCUMENTATION_STANDARD.md for struktur

Referanse: core/docs/WORKFLOWS.md → "JIRA-sak workflow → Fase 1"

---

## TODO-plan

For å opprette TODO-dokumentasjon:

STEG 0: VERIFISER ENVIRONMENT-VARIABLER
- Sjekk at MELOSYS_AIDE_INSTALLATION_PATH er satt
- Hvis ikke satt: Gi feilmelding og stopp

STEG 1: KJØR AUTOMATISK SCRIPT
- Kjør kommando: aide-todo-opprett "<tittel>" "<beskrivelse>"
  - Dette script gjør alt automatisk:
    - Tildeler nummer (neste ledige)
    - Genererer slug fra tittel
    - Oppretter katalog: todo/XX-slug/
    - Fyller ut templates fra $MELOSYS_AIDE_INSTALLATION_PATH/core/templates/todo/
    - Oppretter alle 4 filer (1-beskrivelse.md, 2-analyse.md, 3-løsning.md, 4-status.md)
    - Stager filer i git

STEG 2: BESTEM REPORTS-ROOT
- Hvis MELOSYS_AIDE_REPORTS_PATH er satt:
  - Filene ligger i: $MELOSYS_AIDE_REPORTS_PATH/todo/XX-slug/
  - Scriptet hopper over git add (egen repo)
- Hvis IKKE satt:
  - Filene ligger i: $MELOSYS_AIDE_INSTALLATION_PATH/todo/XX-slug/
  - Scriptet har allerede kjørt git add

STEG 3: VERIFISER OUTPUT
- Sjekk at filene ble opprettet:
  - <reports-root>/todo/XX-slug/1-beskrivelse.md
  - <reports-root>/todo/XX-slug/2-analyse.md
  - <reports-root>/todo/XX-slug/3-løsning.md
  - <reports-root>/todo/XX-slug/4-status.md

STEG 4: BEKREFT
- Vis oppsummering:
  - ✅ TODO-nummer tildelt: XX
  - ✅ Slug generert: XX-slug
  - ✅ 4 filer opprettet
  - ✅ Filer staged i git (hvis i workspace)
  - 📖 Neste steg: Kjør "Analyser TODO XX"

VIKTIG:
- Følg core/docs/WORKFLOWS.md - Fase 1: TODO-plan workflow
- Følg core/docs/DOCUMENTATION_STANDARD.md for struktur

Referanse: core/docs/WORKFLOWS.md → "TODO-plan workflow → Fase 1"

---

## Forventet output

### JIRA

```text
✅ JIRA-dokumentasjon opprettet for MELOSYS-XXXX

Filer opprettet:
- reports/jira/MELOSYS-XXXX/1-beskrivelse.md (ferdig utfylt)
- reports/jira/MELOSYS-XXXX/2-analyse.md (klar for analyse)
- reports/jira/MELOSYS-XXXX/3-løsning.md (klar for løsning)
- reports/jira/MELOSYS-XXXX/4-status.md (klar for status)

Git status:
  new file:   reports/jira/MELOSYS-XXXX/1-beskrivelse.md
  new file:   reports/jira/MELOSYS-XXXX/2-analyse.md
  new file:   reports/jira/MELOSYS-XXXX/3-løsning.md
  new file:   reports/jira/MELOSYS-XXXX/4-status.md

📖 Neste steg: Analyser kodebasen
```

### TODO

```text
✅ TODO-plan opprettet: 17-rydd-opp-i-console-log

Filer opprettet:
- todo/17-rydd-opp-i-console-log/1-beskrivelse.md (ferdig utfylt)
- todo/17-rydd-opp-i-console-log/2-analyse.md (klar for analyse)
- todo/17-rydd-opp-i-console-log/3-løsning.md (klar for løsning)
- todo/17-rydd-opp-i-console-log/4-status.md (klar for status)

Git status:
  new file:   todo/17-rydd-opp-i-console-log/1-beskrivelse.md
  new file:   todo/17-rydd-opp-i-console-log/2-analyse.md
  new file:   todo/17-rydd-opp-i-console-log/3-løsning.md
  new file:   todo/17-rydd-opp-i-console-log/4-status.md

📖 Neste steg: Analyser TODO
```

---

## Tips

**JIRA:**

- Hvis `aide-jira-opprett` feiler, sjekk at JIRA session cookie er oppdatert: `aide-jira-update-cookies`
- Hvis template-variabler ikke erstattes, sjekk at du bruker riktig syntax: `{{VARIABLE}}`

**TODO:**

- Slug genereres automatisk fra tittel (lowercase, bindestrek-separert)
- TODO-nummer tildeles automatisk (neste ledige nummer)

**Felles:**

- Hvis git staging feiler, sjekk at katalogen er opprettet først
- Hvis MELOSYS_AIDE_INSTALLATION_PATH ikke er satt, vil scriptet feile
- Hvis MELOSYS_AIDE_REPORTS_PATH er satt, må du manuelt håndtere git i det repoet


---

## Neste steg

**JIRA:**
```text
/aide-analyser MELOSYS-XXXX   # Analyser kodebase
```

**TODO:**
```text
/aide-analyser TODO-XX        # Analyser (shorthand)
```
