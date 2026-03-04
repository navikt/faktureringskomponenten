# Pre-PR Review (Claude Code)

## Before: Smart deteksjon

**Før base-innholdet kjøres:**

1. **Sjekk working directory:**
   - Er vi i `melosys-web/` eller `melosys-api/`?
   - Eller i workspace-root med endringer i systems/?

2. **Parse argumenter (valgfritt):**
   - `fokus=sikkerhet,ytelse` → Kun disse kategoriene
   - `filer=File1.tsx,File2.ts` → Kun disse filene
   - Ingen args → Full review

3. **Auto-detect scope:**
   - Endringer i `*.tsx`, `*.ts` (frontend) → Les KODESTANDARD.md
   - Endringer i `*.java` (backend) → Les melosys-api docs
   - Endringer med `api.get()`, `api.post()` → Les API_MAPPING_GUIDE.md

---

# Pre-PR Review

**Formål:** Reviewe lokale kodeendringer før commit/PR for å sikre kvalitet og konsistens.



---

## Prosess

STEG 1: HENT ENDRINGER
- Kjør: `git status` for å se hvilke filer som er endret
- Kjør: `git diff` for unstaged changes
- Kjør: `git diff --staged` for staged changes
- Identifiser hvilke filer som er påvirket

STEG 2: BESTEM SCOPE
- Frontend (TypeScript/React): Les systems/melosys-web/docs/KODESTANDARD.md
- Backend (Java): Les systems/melosys-api/README.md og patterns.md
- API-kall: Les systems/melosys-web/api-mapping/API_MAPPING_GUIDE.md

STEG 3: REVIEW-KATEGORIER

**Sjekk mot KODESTANDARD.md (hvis frontend):**
- ✅ TypeScript: strict mode, types, no any
- ✅ React: hooks rules, component patterns, prop types
- ✅ Styling: Tailwind usage, responsive design
- ✅ Testing: Test coverage, TDD-prinsipper
- ✅ Tilgjengelighet: ARIA, semantisk HTML, keyboard navigation

**Sikkerhet:**
- ⚠️ Hardkodet secrets, API keys, credentials
- ⚠️ SQL injection, XSS vulnerabilities
- ⚠️ Input validation, sanitization
- ⚠️ Auth/authorization checks

**Ytelse:**
- 🚀 Unødvendige re-renders (React)
- 🚀 Ineffektive loops/queries
- 🚀 Memory leaks
- 🚀 Bundle size impact

**Git Best Practices:**
- 📝 Nye filer staged med `git add <filnavn>`
- 📝 Flyttede filer bruker `git mv`
- 📝 Commits følger conventional commits format
- 📝 Følger core/docs/GIT_RULES.md

**API-konsistens (hvis API-kall):**
- 🔗 Følg API_MAPPING_GUIDE.md
- 🔗 Error handling på alle API-kall
- 🔗 Loading states
- 🔗 Type safety på responses

STEG 4: GI STRUKTURERT FEEDBACK

**Format:**
```markdown
## 📊 Review Summary

**Filer reviewet:** N filer
**Alvorlighetsgrad:** 🟢 Godkjent / 🟡 Mindre issues / 🔴 Kritiske issues

---

## ✅ Positivt
- Liste med ting som er bra

---

## ⚠️ Issues funnet

### 🔴 Kritisk (må fikses før commit)
1. [Fil:linje] Problem beskrivelse
   **Hvorfor:** Forklaring
   **Løsning:**
   ```typescript
   // Korrekt kode her
   ```

### 🟡 Forbedringsforslag (vurder å fikse)
1. [Fil:linje] Forslag beskrivelse
   **Hvorfor:** Forklaring
   **Løsning:**
   ```typescript
   // Forbedret kode her
   ```

---

## 📝 Neste steg

- [ ] Fiks kritiske issues
- [ ] Vurder forbedringsforslag
- [ ] Kjør tests: `npm test`
- [ ] Commit med format: `type(scope): beskrivelse`
```

STEG 5: FORSLAG TIL COMMIT-MELDING

Hvis ingen kritiske issues, generer commit-melding:
```text
type(scope): kort beskrivelse

- Detaljert endring 1
- Detaljert endring 2

Refs: MELOSYS-XXXX (hvis relevant)
```

---

## VIKTIG

- ⚠️ Du kan IKKE kjøre `git commit` (blokkert av settings.json)
- ✅ Du kan gi commit-melding som tekst
- 📖 Følg core/docs/GIT_RULES.md for commit-format
- 🎯 Vær konstruktiv og spesifikk i feedback
- 💡 Gi kodeeksempler på løsninger


---

## After: Claude Code-spesifikke tips

**Etter review er fullført:**

### Quick fixes

Hvis små issues funnet, tilby:
```text
💡 Vil du at jeg fikser disse umiddelbart?
   1. Alle kritiske issues
   2. Kun [spesifikk issue]
   3. Nei, jeg fikser selv
```

### Neste kommandoer

Foreslå basert på resultat:

**Hvis ingen kritiske issues:**
```text
✅ Klar for commit!

Foreslått commit-melding:
[vis melding her]

Neste steg:
- Kjør tests: npm test
- Commit: [kopier melding over]
- Push: git push
```

**Hvis kritiske issues:**
```text
⚠️ Kritiske issues må fikses først.

Etter fikser, kjør:
- /aide-gh-review (review på nytt)
- /aide-lag-tester (hvis mangler tests)
```

### Integrasjon med workflow

- Hvis endring er del av JIRA-sak → Oppdater `4-status.md`
- Hvis endring påvirker API → Sjekk API_MAPPING_GUIDE.md
- Hvis nye komponenter → Sjekk at tests eksisterer

### VS Code integration hint

```text
💡 TIP: Legg til som VS Code task i .vscode/tasks.json:
{
  "label": "Pre-PR Review",
  "type": "shell",
  "command": "echo '? /aide-gh-review' | gh copilot"
}

Keybinding: Cmd+Shift+R for rask review
```
