# Claude Code Wrapper: aide-løs

Implementer løsningen for en JIRA-sak eller TODO-plan med Test-Driven Development.

**Input:** $ARGUMENTS (alle argumenter etter kommandoen)

## Smart deteksjon

Parse `$ARGUMENTS`:

**JIRA mode:** Hvis første ord starter med `MELOSYS-`
- Eksempel: `/aide-løs MELOSYS-7890`
- Implementer for JIRA-sak: `MELOSYS-7890`

**TODO mode:** Hvis første ord starter med `TODO-` eller `todo-`
- Eksempel: `/aide-løs TODO-01` eller `/aide-løs todo-01-redux-migration`
- Implementer for TODO-plan

**Feilhåndtering:** Hvis argument mangler eller ugyldig format, vis:
```text
❌ Mangler argument

Bruk:
/aide-løs MELOSYS-XXXX     # For JIRA-sak
/aide-løs TODO-XX          # For TODO-plan (shorthand)
/aide-løs todo-XX-navn     # For TODO-plan (full)

Eksempler:
/aide-løs MELOSYS-7890
/aide-løs TODO-01
/aide-løs todo-01-redux-migration
```

---

# Prompt: Implementer løsning med TDD (JIRA eller TODO)

**Formål:** Implementere løsning med Test-Driven Development (tilsvarer `/aide-løs` i Claude Code)

---



---

## Prompt: JIRA-sak

```text
Implementer løsningen for JIRA-sak MELOSYS-XXXX med Test-Driven Development:

LES FØRST:
- Les: reports/jira/MELOSYS-XXXX/2-analyse.md (påvirkede filer)
- Les: reports/jira/MELOSYS-XXXX/3-løsning.md (implementeringsplan)
- Les: systems/melosys-web/docs/KODESTANDARD.md (kodestandarder)

═══════════════════════════════════════════════════════
FASE 1: RED PHASE - SKRIV TESTER SOM FEILER
═══════════════════════════════════════════════════════

STEG 1: LES "STEG 0" FRA 3-LØSNING.MD
- Identifiser alle tester som skal skrives
- Identifiser testfiler og testcases

STEG 2: OPPRETT TESTFILER
- Følg structure fra 3-løsning.md
- Følg core/docs/TESTING_RULES.md
- Følg systems/melosys-web/docs/KODESTANDARD.md - Testing-seksjon
- Bruk Vitest syntax

STEG 3: KJØR TESTER
- Kjør: pnpm test -- --run <testfil>
- Verifiser at tester FEILER (dette er forventet!)

STEG 4: STOPP OG BEKREFT
⛔ STOPP HER - Be bruker om bekreftelse før du fortsetter til GREEN phase

Vis oppsummering:
- ❌ [antall] tester skrevet
- ❌ Alle feiler som forventet (RED phase)
- 📖 Klar for GREEN phase?

═══════════════════════════════════════════════════════
FASE 2: GREEN PHASE - IMPLEMENTER TIL TESTER PASSERER
═══════════════════════════════════════════════════════

STEG 5: IMPLEMENTER STEG 1 FRA 3-LØSNING.MD
- Les Steg 1 fra 3-løsning.md
- Implementer koden som beskrevet
- Følg systems/melosys-web/docs/KODESTANDARD.md strengt:
  - TypeScript (ikke JavaScript)
  - Funksjonelle komponenter (ikke class)
  - react-hook-form (ikke redux-form)
  - Maks 300 linjer per fil

STEG 6: KJØR TESTER FOR STEG 1
- Kjør: pnpm test -- --run <testfil>
- Verifiser at relevante tester nå PASSERER

STEG 7-N: GJENTA FOR ALLE STEG
- For hvert steg i 3-løsning.md:
  1. Implementer
  2. Kjør tester
  3. Verifiser at tester passerer

STEG N+1: STOPP OG BEKREFT
⛔ STOPP HER - Be bruker om bekreftelse før du fortsetter til REFACTOR phase

Vis oppsummering:
- ✅ [antall] steg implementert
- ✅ Alle tester passerer (GREEN phase)
- 📖 Klar for REFACTOR phase?

═══════════════════════════════════════════════════════
FASE 3: REFACTOR PHASE - KVALITETSSJEKK OG CLEANUP
═══════════════════════════════════════════════════════

STEG N+2: FULL TEST-SUITE
- Kjør: pnpm test -- --run
- Verifiser: ALLE tester passerer (ingen regresjoner)

STEG N+3: TYPESCRIPT CHECK
- Kjør: npx tsc --noEmit
- Verifiser: Ingen type-feil

STEG N+4: ESLINT
- Kjør: pnpm run eslint
- Fikse eventuelle linting-feil

STEG N+5: BYGG
- Kjør: pnpm run build
- Verifiser: Bygget lykkes

STEG N+6: OPPDATER STATUS
- Oppdater: reports/jira/MELOSYS-XXXX/4-status.md
- Sett status til:
  - Dokumentasjon: ✅ Ferdig
  - Analyse: ✅ Ferdig
  - Implementering: ✅ Ferdig
  - Testing: ✅ Alle tester passerer
  - Kvalitetssjekk: ✅ tsc ✅ eslint ✅ build

STEG N+7: MANUELL TESTPLAN
- Vis manuell testplan fra 3-løsning.md
- Be bruker om å teste manuelt

STEG N+8: BEKREFT FERDIG
- Vis oppsummering:
  - ✅ Implementering fullført
  - ✅ [antall] tester passerer
  - ✅ TypeScript OK
  - ✅ ESLint OK
  - ✅ Bygg OK
  - 📝 Status oppdatert
  - 📖 Klar for manuell test og commit

VIKTIG:
- Følg core/docs/WORKFLOWS.md - Fase 3
- Følg core/docs/TESTING_RULES.md - TDD-tilnærming
- Følg systems/melosys-web/docs/KODESTANDARD.md - ALLTID
- STOPP ved hver fase og be om bekreftelse
- ALDRI hopp over tester
- ALDRI bruk JavaScript, class components, eller redux-form

Referanse: core/docs/WORKFLOWS.md → "JIRA-sak workflow → Fase 3"
```text

---

## Prompt: TODO-plan

```text
Implementer løsningen for TODO-plan TODO-XX-navn med Test-Driven Development:

LES FØRST:
- Les: reports/todo/TODO-XX-navn/2-analyse.md (påvirkede filer)
- Les: reports/todo/TODO-XX-navn/3-løsning.md (implementeringsplan)
- Les: systems/melosys-web/docs/KODESTANDARD.md (kodestandarder)

═══════════════════════════════════════════════════════
FASE 1: RED PHASE - SKRIV TESTER SOM FEILER
═══════════════════════════════════════════════════════

STEG 1: LES "STEG 0" FRA 3-LØSNING.MD
- Identifiser alle tester som skal skrives
- Identifiser testfiler og testcases

STEG 2: OPPRETT TESTFILER
- Følg structure fra 3-løsning.md
- Følg core/docs/TESTING_RULES.md
- Følg systems/melosys-web/docs/KODESTANDARD.md - Testing-seksjon
- Bruk Vitest syntax

STEG 3: KJØR TESTER
- Kjør: pnpm test -- --run <testfil>
- Verifiser at tester FEILER (dette er forventet!)

STEG 4: STOPP OG BEKREFT
⛔ STOPP HER - Be bruker om bekreftelse før du fortsetter til GREEN phase

Vis oppsummering:
- ❌ [antall] tester skrevet
- ❌ Alle feiler som forventet (RED phase)
- 📖 Klar for GREEN phase?

═══════════════════════════════════════════════════════
FASE 2: GREEN PHASE - IMPLEMENTER TIL TESTER PASSERER
═══════════════════════════════════════════════════════

STEG 5: IMPLEMENTER STEG 1 FRA 3-LØSNING.MD
- Les Steg 1 fra 3-løsning.md
- Implementer koden som beskrevet
- Følg systems/melosys-web/docs/KODESTANDARD.md strengt

STEG 6: KJØR TESTER FOR STEG 1
- Kjør: pnpm test -- --run <testfil>
- Verifiser at relevante tester nå PASSERER

STEG 7-N: GJENTA FOR ALLE STEG
- For hvert steg i 3-løsning.md:
  1. Implementer
  2. Kjør tester
  3. Verifiser at tester passerer

STEG N+1: STOPP OG BEKREFT
⛔ STOPP HER - Be bruker om bekreftelse før du fortsetter til REFACTOR phase

Vis oppsummering:
- ✅ [antall] steg implementert
- ✅ Alle tester passerer (GREEN phase)
- 📖 Klar for REFACTOR phase?

═══════════════════════════════════════════════════════
FASE 3: REFACTOR PHASE - KVALITETSSJEKK OG CLEANUP
═══════════════════════════════════════════════════════

STEG N+2: FULL TEST-SUITE
- Kjør: pnpm test -- --run
- Verifiser: ALLE tester passerer (ingen regresjoner)

STEG N+3: TYPESCRIPT CHECK
- Kjør: npx tsc --noEmit
- Verifiser: Ingen type-feil

STEG N+4: ESLINT
- Kjør: pnpm run eslint
- Fikse eventuelle linting-feil

STEG N+5: BYGG
- Kjør: pnpm run build
- Verifiser: Bygget lykkes

STEG N+6: OPPDATER STATUS
- Oppdater: reports/todo/TODO-XX-navn/4-status.md
- Sett status til:
  - Dokumentasjon: ✅ Ferdig
  - Analyse: ✅ Ferdig
  - Implementering: ✅ Ferdig
  - Testing: ✅ Alle tester passerer
  - Kvalitetssjekk: ✅ tsc ✅ eslint ✅ build

STEG N+7: BEKREFT FERDIG
- Vis oppsummering:
  - ✅ Implementering fullført
  - ✅ [antall] tester passerer
  - ✅ TypeScript OK
  - ✅ ESLint OK
  - ✅ Bygg OK
  - 📝 Status oppdatert
  - 📖 Klar for commit

VIKTIG:
- Følg core/docs/WORKFLOWS.md - TODO workflow
- Følg core/docs/TESTING_RULES.md - TDD-tilnærming
- Følg systems/melosys-web/docs/KODESTANDARD.md - ALLTID
- STOPP ved hver fase og be om bekreftelse
- ALDRI hopp over tester

Referanse: core/docs/WORKFLOWS.md → "TODO-plan workflow → Fase 3"
```text

---

## Forventet output

### JIRA - Etter RED phase:

```text
═══════════════════════════════════════════════════════
FASE 1: RED PHASE - FERDIG
═══════════════════════════════════════════════════════

✅ Testfiler opprettet:
- src/__tests__/UserProfile.test.tsx (3 testcases)
- src/__tests__/UserForm.test.tsx (2 testcases)

❌ Testresultat:
FAIL  src/__tests__/UserProfile.test.tsx
  ❌ should validate new field (implementering mangler)
  ❌ should show error for invalid input (implementering mangler)
  ❌ should update backend on submit (implementering mangler)

FAIL  src/__tests__/UserForm.test.tsx
  ❌ should render new field (implementering mangler)
  ❌ should validate field (implementering mangler)

📊 Status: 5/5 tester feiler (forventet i RED phase)

⛔ STOPP: Klar for GREEN phase? Skal jeg implementere løsningen?
```text

### Etter GREEN phase:

```text
═══════════════════════════════════════════════════════
FASE 2: GREEN PHASE - FERDIG
═══════════════════════════════════════════════════════

✅ Implementering fullført:
- Steg 1: Lagt til validering i UserProfile.tsx:45 ✅
- Steg 2: Lagt til felt i UserForm.tsx:120 ✅
- Steg 3: Lagt til API-kall i userService.ts:30 ✅
- Steg 4: Oppdatert types i user.types.ts:15 ✅

✅ Testresultat:
PASS  src/__tests__/UserProfile.test.tsx (5 tests)
PASS  src/__tests__/UserForm.test.tsx (2 tests)

📊 Status: 7/7 tester passerer

⛔ STOPP: Klar for REFACTOR phase? Skal jeg kjøre kvalitetssjekk?
```text

### Etter REFACTOR phase:

```text
═══════════════════════════════════════════════════════
FASE 3: REFACTOR PHASE - FERDIG
═══════════════════════════════════════════════════════

✅ Full test-suite:
  PASS  src/__tests__/UserProfile.test.tsx (5 tests)
  PASS  src/__tests__/UserForm.test.tsx (2 tests)
  PASS  (47 andre testsuiter)
  Tests: 134 passed, 134 total

✅ TypeScript check:
  No errors found

✅ ESLint:
  No linting errors

✅ Bygg:
  Build completed successfully
  dist/ generated

📝 Status oppdatert:
  reports/jira/MELOSYS-XXXX/4-status.md

📋 Manuell testplan:
1. Start dev-server: pnpm start
2. Naviger til /bruker/profil
3. Fyll ut det nye feltet
4. Verifiser validering
5. Lagre og verifiser backend-oppdatering

✅ IMPLEMENTERING FULLFØRT!

Neste steg:
1. Test manuelt (se plan over)
2. Commit endringene
3. Push til remote
4. Opprett pull request
```text

---

## Tips

- **Vær tålmodig:** TDD tar tid, men gir høyere kvalitet
- **Ikke hopp over faser:** RED → GREEN → REFACTOR er viktig
- **Be om bekreftelse:** Det er tryggere å stoppe mellom faser
- **Følg kodestandarder:** Spesielt TypeScript, funksjonelle komponenter, react-hook-form
- **Verifiser testresultater:** Se at tester faktisk feiler i RED og passerer i GREEN


---

## Etter implementering

**Kvalitetssjekk (kjøres automatisk i REFACTOR phase):**
```bash
pnpm test -- --run    # Alle tester
npx tsc --noEmit      # TypeScript check
pnpm run eslint       # ESLint
pnpm run build        # Bygg
```

**Neste steg etter godkjent kvalitetssjekk:**
1. Test manuelt (følg manuell testplan fra 3-løsning.md)
2. Be bruker om commit-melding
