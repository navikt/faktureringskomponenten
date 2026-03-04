# Claude Code Wrapper: aide-analyser

Analyser kodebasen for en JIRA-sak eller TODO-plan.

**Input:** $ARGUMENTS (alle argumenter etter kommandoen)

## Smart deteksjon

Parse `$ARGUMENTS`:

**JIRA mode:** Hvis første ord starter med `MELOSYS-`
- Eksempel: `/aide-analyser MELOSYS-7890`
- Analyser for JIRA-sak: `MELOSYS-7890`

**TODO mode:** Hvis første ord starter med `TODO-` eller `todo-`
- Eksempel: `/aide-analyser TODO-01` eller `/aide-analyser todo-01-redux-migration`
- Analyser for TODO-plan

**Feilhåndtering:** Hvis argument mangler eller ugyldig format, vis:
```text
❌ Mangler argument

Bruk:
/aide-analyser MELOSYS-XXXX     # For JIRA-sak
/aide-analyser TODO-XX          # For TODO-plan (shorthand)
/aide-analyser todo-XX-navn     # For TODO-plan (full)

Eksempler:
/aide-analyser MELOSYS-7890
/aide-analyser TODO-01
/aide-analyser todo-01-redux-migration
```

---

# Prompt: Analyser kodebasen (JIRA eller TODO)

**Formål:** Analysere kodebase og opprette implementeringsplan (tilsvarer `/aide-analyser` i Claude Code)

**📚 VIKTIG:** Denne prompten følger felles analyse-instruksjoner:
- `../../core/analysis-instructions/complexity-detection.md` - Hvordan detektere LAV/MIDDELS/HØY
- `../../core/analysis-instructions/analysis-process.md` - Steg-for-steg analyseprosess
- `../../core/analysis-instructions/documentation-generation.md` - Hvilke templates å bruke

**Se også:** `../../core/docs/WORKFLOWS.md#kompleksitetsdeteksjon` - Kompleksitetsdeteksjon og workflows

---



---

## Prompt: JIRA-sak

```text
Analyser kodebasen for JIRA-sak MELOSYS-XXXX og opprett implementeringsplan:

STEG 1: LES BESKRIVELSE (JIRA)
- Les fil: reports/jira/MELOSYS-XXXX/1-beskrivelse.md
- Identifiser:
  - Hva skal endres/legges til?
  - Hvilke akseptansekriterier må oppfylles?
  - Hvilket omfang? (Frontend/Backend/Begge)

STEG 2: DETEKTER KOMPLEKSITET
📚 Følg: ../../core/analysis-instructions/complexity-detection.md

Klassifiser som LAV/MIDDELS/HØY basert på:
- Antall filer (1 = LAV, 3-10 = MIDDELS, 10+ = HØY)
- Operasjonstype (fjern/erstatt = LAV, refaktorer = MIDDELS, migrer = HØY)
- Patterns ("alle" = HØY)

STEG 3: ANALYSER BASERT PÅ KOMPLEKSITET
📚 Følg: ../../core/analysis-instructions/analysis-process.md

Prosessen varierer basert på kompleksitet:
- LAV: Quick Fix (< 15 min analyse)
- MIDDELS: Komponentanalyse (20-45 min analyse)
- HØY: Bred søk + kategorisering (1-3 timer analyse)

STEG 4: OPPDATER 2-ANALYSE.MD
📚 Følg: ../../core/analysis-instructions/documentation-generation.md

Velg template basert på kompleksitet:
- LAV: < 80 linjer
- MIDDELS: 100-200 linjer
- HØY: Bruk core/templates/jira/2-analyse.md (200-400 linjer)

Skriv til: reports/jira/MELOSYS-XXXX/2-analyse.md

STEG 5: OPPRETT IMPLEMENTERINGSPLAN (3-LØSNING.MD)
📚 Følg: ../../core/analysis-instructions/documentation-generation.md § 3-løsning.md

Strukturer med TDD:
- Steg 0: Skriv tester (RED phase)
- Steg 1-N: Implementering (GREEN phase)
- Testing-strategi (REFACTOR phase)

Velg template basert på kompleksitet:
- LAV: < 60 linjer
- MIDDELS: 100-150 linjer
- HØY: Bruk core/templates/jira/3-løsning.md (150-250 linjer)

Skriv til: reports/jira/MELOSYS-XXXX/3-løsning.md

STEG 6: OPPDATER 4-STATUS.MD
📚 Følg: ../../core/analysis-instructions/documentation-generation.md § 4-status.md

Velg template basert på kompleksitet:
- LAV: Enkel sjekkliste (< 30 linjer)
- MIDDELS/HØY: Fasebasert tracking (50-100 linjer)

Skriv til: reports/jira/MELOSYS-XXXX/4-status.md

STEG 7: BEKREFT
- Vis oppsummering:
  - 📊 Kompleksitet: [LAV/MIDDELS/HØY]
  - 📁 Påvirkede filer: [antall]
  - 🔗 API-påvirkning: [Frontend/Backend/Both]
  - 📝 Implementeringsplan: [antall steg]
  - 📖 Neste steg: Kjør "Implementer løsningen for MELOSYS-XXXX med TDD"

VIKTIG:
- Følg core/docs/WORKFLOWS.md - Fase 2
- Følg core/docs/DOCUMENTATION_STANDARD.md for struktur
- Følg systems/melosys-web/docs/KODESTANDARD.md for kodestandarder
- Bruk ALLTID fil:linje format for referanser
- Bruk systems/melosys-web/api-mapping/ for API-analyse

Referanse: core/docs/WORKFLOWS.md → "JIRA-sak workflow → Fase 2"
```

---

## Prompt: TODO-plan

```text
Analyser kodebasen for TODO-plan TODO-XX-navn og opprett implementeringsplan:

STEG 1: LES BESKRIVELSE (TODO)
- Les fil: reports/todo/TODO-XX-navn/1-beskrivelse.md
- Identifiser:
  - Hva skal endres/legges til?
  - Hvilket omfang? (Antall filer, komponenter)
  - Teknisk gjeld eller migrering?

STEG 2: DETEKTER KOMPLEKSITET
📚 Følg: ../../core/analysis-instructions/complexity-detection.md

Klassifiser som LAV/MIDDELS/HØY basert på:
- Antall filer (1 = LAV, 3-10 = MIDDELS, 10+ = HØY)
- Operasjonstype (fjern/erstatt = LAV, refaktorer = MIDDELS, migrer = HØY)
- Patterns ("alle" = HØY)

STEG 3: ANALYSER BASERT PÅ KOMPLEKSITET
📚 Følg: ../../core/analysis-instructions/analysis-process.md

Prosessen varierer basert på kompleksitet:
- LAV: Quick Fix (< 15 min analyse)
- MIDDELS: Komponentanalyse (20-45 min analyse)
- HØY: Bred søk + kategorisering + migreringsplan (1-3 timer analyse)

STEG 4: OPPDATER 2-ANALYSE.MD
📚 Følg: ../../core/analysis-instructions/documentation-generation.md

Skriv til: reports/todo/TODO-XX-navn/2-analyse.md

STEG 5: OPPRETT MIGRERINGSPLAN (3-LØSNING.MD)
📚 Følg: ../../core/analysis-instructions/documentation-generation.md § 3-løsning.md

Strukturer med TDD og faser:
- Steg 0: Skriv tester (RED phase)
- Steg 1-N: Implementering (GREEN phase)
- Testing-strategi (REFACTOR phase)

Skriv til: reports/todo/TODO-XX-navn/3-løsning.md

STEG 6: OPPDATER 4-STATUS.MD
📚 Følg: ../../core/analysis-instructions/documentation-generation.md § 4-status.md

Skriv til: reports/todo/TODO-XX-navn/4-status.md

STEG 7: BEKREFT
- Vis oppsummering med kompleksitet og antall filer
  - 📖 Neste steg: Kjør "Implementer løsningen for TODO-XX-navn med TDD"

VIKTIG:
- Følg core/docs/WORKFLOWS.md - TODO workflow
- Følg core/docs/DOCUMENTATION_STANDARD.md for struktur
- Bruk ALLTID fil:linje format for referanser

Referanse: core/docs/WORKFLOWS.md → "TODO-plan workflow → Fase 2"
```

---

## Forventet output: JIRA

```text
✅ Kodebase-analyse fullført for MELOSYS-XXXX

📊 Funn:
- Kompleksitet: Middels (4 filer påvirkes)
- API-påvirkning: Frontend + Backend
- Nye komponenter: 1
- Endrede komponenter: 3
- Risikonivå: Lav

📁 Påvirkede filer:
- src/sider/UserProfile.tsx:45 (endre validering)
- src/komponenter/UserForm.tsx:120 (legg til felt)
- src/api/userService.ts:30 (nytt API-kall)
- src/__tests__/UserProfile.test.tsx (nye tester)

🔗 API-påvirkning:
- Backend: FagsakController.kt:58 (legg til felt i DTO)
- Endpoint: PUT /fagsaker/{saksnr}

📝 Implementeringsplan opprettet:
- Steg 0: Skriv 3 tester (RED phase)
- Steg 1-4: Implementer endringer (GREEN phase)
- Testing-strategi: Full kvalitetssjekk (REFACTOR phase)

Filer oppdatert:
- reports/jira/MELOSYS-XXXX/2-analyse.md ✅
- reports/jira/MELOSYS-XXXX/3-løsning.md ✅
- reports/jira/MELOSYS-XXXX/4-status.md ✅

📖 Neste steg:
Bruk prompt fra prompts/aide-løs.md for å implementere løsningen med TDD.
```

---

## Forventet output: TODO

```text
✅ Kodebase-analyse fullført for TODO-XX-navn

📊 Funn:
- Kompleksitet: HØY (155 filer påvirkes)
- Type: Migrering (Redux Form → react-hook-form)
- Risikonivå: Middels

📁 Påvirkede filer (kategorisert):
LAV kompleksitet (82 filer):
- src/forms/SimpleForm.tsx:12 (< 10 felt, basic validation)
- src/forms/ContactForm.tsx:45 (enkelt skjema)
- ...

MIDDELS kompleksitet (58 filer):
- src/forms/UserProfileForm.tsx:120 (15 felt, sync validation)
- src/forms/AddressForm.tsx:89 (custom components)
- ...

HØY kompleksitet (15 filer):
- src/forms/WizardForm.tsx:234 (multi-step, FieldArray)
- src/forms/DynamicForm.tsx:456 (async validation, Redux integration)
- ...

📝 Migreringsplan opprettet:
- Fase 1: Pilot (3-5 enkle former) - 1-2 dager
- Fase 2: Batch 1 (LAV kompleksitet) - 5-7 dager
- Fase 3: Batch 2 (MIDDELS kompleksitet) - 8-12 dager
- Fase 4: Komplekse former - 4-6 dager
- Testing og kvalitetssikring: 2-3 dager

Estimat:
- Manuell: ~20 uker
- AI-assistert: 3-4 uker (85% tidsbesparelse)

Filer oppdatert:
- reports/todo/TODO-XX-navn/2-analyse.md ✅
- reports/todo/TODO-XX-navn/3-løsning.md ✅
- reports/todo/TODO-XX-navn/4-status.md ✅

📖 Neste steg:
Bruk prompt fra prompts/aide-løs.md for å implementere løsningen med TDD.
```

---

## Tips

- **JIRA:** Bruk API-mapping aktivt - det sparer mye tid
- **TODO:** Start med pilot (enkle filer) for å etablere patterns
- Vær spesifikk med fil:linje referanser - det hjelper ved implementering
- Vurder alltid API-påvirkning - backend-endringer tar lengre tid
- **Kompleksitet:** Følg TASK_ANALYSIS_STRATEGY.md for riktig scope


---

## Neste steg

**JIRA:**
```text
/aide-løs MELOSYS-XXXX   # Implementer med TDD
```

**TODO:**
```text
/aide-løs TODO-XX        # Implementer (shorthand)
```
