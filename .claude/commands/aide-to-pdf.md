Du skal hjelpe brukeren med å **generere PDF** fra JIRA eller TODO dokumentasjon.

## Input

Brukeren har kjørt:
```bash
/aide-to-pdf <ISSUE_ID>
```

**Eksempler:**
- `/aide-to-pdf MELOSYS-7637` - JIRA-sak
- `/aide-to-pdf 17-fikse-validering` - TODO-plan

## Din oppgave

1. **Valider input:**
   - Hvis input matcher `MELOSYS-<tall>`: JIRA mode
   - Hvis input matcher `TODO-<tall>` eller `todo-<tall>`: TODO mode (shorthand)
   - Alt annet: TODO mode (full ID)
   - Hvis ingen input: Spør brukeren om JIRA-nummer eller TODO-ID

2. **Bestem REPORTS_ROOT:**
   ```bash
   # Sjekk environment variable først
   if [ -n "$MELOSYS_AIDE_REPORTS_PATH" ]; then
     REPORTS_ROOT="$MELOSYS_AIDE_REPORTS_PATH"
   elif [ -n "$MELOSYS_AIDE_INSTALLATION_PATH" ]; then
     REPORTS_ROOT="$MELOSYS_AIDE_INSTALLATION_PATH/reports"
   else
     # Fallback - anta vi er i melosys-aide eller melosys-web
     REPORTS_ROOT="reports"
   fi
   ```

3. **Finn TODO hvis shorthand (TODO-XX):**
   ```bash
   # Hvis input er "TODO-27" eller "todo-27"
   SEARCH_PREFIX=$(echo "$INPUT" | sed 's/^todo-/TODO-/')  # Normaliser til uppercase
   TODO_ID=$(find "$REPORTS_ROOT/todo" -maxdepth 1 -type d -name "${SEARCH_PREFIX}-*" | head -1 | xargs basename)

   if [ -z "$TODO_ID" ]; then
     echo "❌ Fant ingen TODO med ID: $INPUT"
     exit 1
   fi
   # Eksempel: TODO-27 → finner TODO-27-steg-ts-konvertering-analyse
   ```

4. **Sjekk at dokumentasjon eksisterer:**
   - JIRA: `$REPORTS_ROOT/jira/<ISSUE_ID>/`
   - TODO: `$REPORTS_ROOT/todo/<TODO_ID>/`
   - Hvis ikke: Informer at brukeren må kjøre `/aide-opprett` først

5. **Generer PDF:**
   ```bash
   # Pass riktig ID til aide-generate-pdf
   # JIRA: aide-generate-pdf MELOSYS-7890
   # TODO: aide-generate-pdf TODO-27-steg-ts-konvertering-analyse (full ID, ikke shorthand!)
   aide-generate-pdf <ID>
   ```

   **VIKTIG:** `aide-generate-pdf` scriptet trenger også å respektere `MELOSYS_AIDE_REPORTS_PATH`!

   Scriptet vil:
   - Kombinere alle markdown-filer (1-beskrivelse, 2-analyse, 3-løsning, 4-status)
   - Legge til forside med metadata
   - Konvertere til PDF med sidehode/sidefot
   - Output: `$REPORTS_ROOT/jira/<ID>/<ID>.pdf` eller `$REPORTS_ROOT/todo/<ID>/<ID>.pdf`

6. **Gi brukeren resultatet:**
   - Vis path til PDF-filen
   - Forklar hvordan åpne den: `open <path>`

## Feilhåndtering

**Hvis `md-to-pdf` ikke er installert:**
```text
❌ md-to-pdf er ikke installert

**Installer med:**
npm install -g md-to-pdf

Eller kjør fra melosys-web:
npx md-to-pdf
```

**Hvis dokumentasjon ikke eksisterer:**
```text
❌ Kunne ikke finne dokumentasjon for <ISSUE_ID>

**Har du kjørt opprett-kommandoen først?**

/aide-opprett <ISSUE_ID>
/aide-analyser <ISSUE_ID>
```

## Notater

- **Automatisk JIRA/TODO deteksjon:** Samme logikk som `/aide-opprett`
- **Output-lokasjon:** Samme mappe som markdown-filene (holder alt samlet)
- **MELOSYS_AIDE_REPORTS_PATH:** Scriptet respekterer environment variable hvis satt
- **Styling:** PDF inkluderer sidehode med issue-nummer og sidefot med sidetall
