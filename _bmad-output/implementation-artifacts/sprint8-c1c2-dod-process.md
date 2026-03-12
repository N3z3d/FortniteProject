# Story sprint8-C1C2: Critères DoD commits Git + validation CI/CD

Status: review

<!-- METADATA
  story_key: sprint8-c1c2-dod-process
  branch: story/sprint8-c1c2-dod-process
  sprint: Sprint 8
-->

## Story

As a developer agent working on FortniteProject,
I want explicit DoD criteria enforcing commit-per-story discipline and CI/CD validation for workflow changes,
so that git history stays readable, work is never lost, and CI changes are always proven green before a story is closed.

## Acceptance Criteria

1. `checklist.md` contains a new criterion **C1**: "chaque story passant en `done` doit avoir au moins 1 commit Git créé *pendant* cette story (avant de passer à la story suivante)."
2. `checklist.md` contains a new criterion **C2**: "toute story modifiant `ci.yml` ou tout fichier `.github/workflows/*.yml` doit être validée par un push sur la branche principale et un CI vert visible dans GitHub Actions UI avant de passer en `done`."
3. Les deux critères sont placés dans la section appropriée de `checklist.md` (§ "Final Status Verification" ou nouvelle section "Workflow & Process").
4. La formulation des critères est actionnable et non ambiguë (un agent peut vérifier oui/non).
5. Zéro régression : les 2243 tests Vitest frontend + baselines backend restent inchangés (aucun code modifié).

## Tasks / Subtasks

- [x] Task 1: Lire le `checklist.md` actuel et identifier l'emplacement optimal pour C1 et C2 (AC: #3)
  - [x] 1.1: Lire `_bmad/bmm/workflows/4-implementation/dev-story/checklist.md` in full
  - [x] 1.2: Identifier la section "🔚 Final Status Verification" comme emplacement cible
- [x] Task 2: Ajouter le critère C1 (AC: #1, #4)
  - [x] 2.1: Insérer dans la section "Final Status Verification" : critère "Git Commit Par Story" ajouté
- [x] Task 3: Ajouter le critère C2 (AC: #2, #4)
  - [x] 3.1: Insérer dans la section "Final Status Verification" (après C1) : critère "Validation CI/CD Obligatoire" ajouté
- [x] Task 4: Vérifier que les critères sont cohérents avec les critères existants et non dupliqués (AC: #3, #4)

## Dev Notes

### Contexte
- Issu de la retro Sprint 7 (Leçons L4 et L5) — les deux anti-patterns ont été observés en Sprint 7 :
  - **L5** : 4 sprints de travail non commités → commit `6a64bd4` de 434 fichiers
  - **L4** : `sprint7-f2-ci-vitest-et-docker-push` ne pouvait pas passer `done` sans validation GitHub Actions réelle
- Ce sont des corrections process, pas des corrections de code

### Fichier cible unique
```
_bmad/bmm/workflows/4-implementation/dev-story/checklist.md
```
- Section cible : `## 🔚 Final Status Verification`
- Ajouter les deux critères APRÈS `- [ ] **Sprint Status Updated:**` et AVANT `- [ ] **Quality Gates Passed:**`

### Formulation exacte recommandée

**C1 — Git Commit Par Story :**
```markdown
- [ ] **Git Commit Par Story :** Au moins 1 commit Git créé *pendant* cette story avant de passer à la suivante. Vérifier : `git log --oneline -5` doit montrer un commit récent lié à cette story. Absence de commit = story ne peut PAS passer en `done`.
```

**C2 — Validation CI/CD Obligatoire :**
```markdown
- [ ] **Validation CI/CD Obligatoire :** Si la story modifie `ci.yml` ou tout fichier `.github/workflows/*.yml` → validation requise par push sur branche principale + CI vert dans GitHub Actions UI. Test local insuffisant. Story ne peut PAS passer en `done` sans ce push confirmé.
```

### Règles critiques à respecter
- **Lire le fichier AVANT d'éditer** (le linter peut avoir modifié les fins de ligne)
- **Utiliser l'outil Edit** (pas sed/awk ni Write complet) — modifier uniquement les 2 lignes concernées
- **Ne pas casser la structure** Markdown existante (indentation, emojis de section, liste `- [ ]`)
- **Aucun test à ajouter** — c'est un fichier de processus BMAD, pas du code applicatif

### Pre-existing Gaps / Known Issues

- [KNOWN] Frontend: 0 failures pre-existing (baseline 2243/2243 — projet-context.md §Pre-existing Failures Baseline)
- [KNOWN] Backend: ~15 failures pre-existing (GameDataIntegrationTest 4, FortniteTrackerServiceTddTest 6, PlayerServiceTddTest 1, ScoreServiceTddTest 3, GameStatisticsServiceTddTest 1 error) — exclues du CI via `-Dexcludes`
- [NONE] Aucun impact de cette story sur les tests (aucun code modifié)

### Project Structure Notes

```
_bmad/
└── bmm/
    └── workflows/
        └── 4-implementation/
            └── dev-story/
                └── checklist.md   ← SEUL fichier modifié
```

- Aucun fichier backend, frontend, ou SQL à toucher
- Aucune migration Flyway
- Aucun test à créer (fichier process, pas code applicatif)
- Aucun i18n requis

### References

- [Source: sprint-7-retro-2026-03-12.md §Leçons L4 et L5]
- [Source: _bmad-output/implementation-artifacts/sprint-status.yaml#sprint8-c1c2-dod-process]
- [Source: _bmad/bmm/workflows/4-implementation/dev-story/checklist.md §Final Status Verification]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- C1 et C2 ajoutés dans `checklist.md` §Final Status Verification (après "Sprint Status Updated", avant "Quality Gates Passed")
- Formulation actionnable et vérifiable par un agent (git log + GitHub Actions UI)
- 0 test à ajouter (fichier process BMAD uniquement)

### File List

- `_bmad/bmm/workflows/4-implementation/dev-story/checklist.md` — modifié (ajout critères C1 et C2 dans section Final Status Verification)
