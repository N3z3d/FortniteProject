# Story sprint8-F2: Validation CI GitHub Actions + GHCR push (fermeture A3b)

Status: ready-for-dev

<!-- METADATA
  story_key: sprint8-f2-validation
  branch: story/sprint8-f2-validation
  sprint: Sprint 8
-->

## Story

As a developer,
I want to confirm that the CI pipeline (`ci.yml`) runs correctly on GitHub Actions with the Vitest step visible and the Docker image pushed to GHCR,
so that `sprint7-f2-ci-vitest-et-docker-push` can be officially closed as `done` and A3b (open since Sprint 3) is definitively resolved.

## Acceptance Criteria

1. Le push `da5d88f` (ou tout commit poussé sur `main`) déclenche le workflow GitHub Actions `.github/workflows/ci.yml`.
2. Le job **frontend** inclut un step "Tests Vitest (unitaires)" visible dans l'UI GitHub Actions.
3. Le step Vitest se termine (même avec `continue-on-error: true`) — le résultat est loggué dans l'artefact `vitest-ci-log`.
4. Le job **docker-build-and-push** s'exécute et pousse deux tags vers `ghcr.io/n3z3d/fortniteproject` : `latest` et le SHA du commit.
5. L'image est visible dans le Package Registry GitHub (`github.com/N3z3d/FortniteProject/pkgs/container/fortniteproject`).
6. Si l'un des jobs échoue → diagnostiquer la cause, corriger `ci.yml`, re-pousser, et valider.
7. `sprint7-f2-ci-vitest-et-docker-push` passe en `done` dans `sprint-status.yaml`.

## Tasks / Subtasks

- [ ] Task 1: Vérifier l'état du CI sur GitHub (AC: #1, #2, #3, #4)
  - [ ] 1.1: Ouvrir `https://github.com/N3z3d/FortniteProject/actions` et vérifier le dernier run
  - [ ] 1.2: Confirmer que le step "Tests Vitest" est visible dans le job frontend
  - [ ] 1.3: Confirmer que le job docker-build-and-push a réussi
- [ ] Task 2: Vérifier l'image dans GHCR (AC: #4, #5)
  - [ ] 2.1: Ouvrir `https://github.com/N3z3d/FortniteProject/pkgs/container/fortniteproject`
  - [ ] 2.2: Confirmer la présence du tag `latest` et du tag SHA
- [ ] Task 3: Diagnostiquer et corriger si échec (AC: #6)
  - [ ] 3.1: Si job frontend échoue → lire les logs, corriger, re-pousser
  - [ ] 3.2: Si job docker échoue → vérifier `permissions: packages: write`, nom image lowercase, re-pousser
- [ ] Task 4: Fermer sprint7-f2 et sprint8-f2 (AC: #7)
  - [ ] 4.1: Passer `sprint7-f2-ci-vitest-et-docker-push: done` dans sprint-status.yaml
  - [ ] 4.2: Passer `sprint8-f2-validation: review` dans sprint-status.yaml

## Dev Notes

### Contexte
- `ci.yml` a été mis à jour en Sprint 7 (commit `6a64bd4`) et poussé sur main (commit `da5d88f`, 2026-03-12)
- Le push a déjà été effectué — cette story consiste à **vérifier** que le CI a bien tourné

### Fichier CI actuel (`.github/workflows/ci.yml`)
Structure attendue :
```yaml
jobs:
  backend:   # mvn verify
  frontend:  # npm ci + lint + test:vitest (continue-on-error: true) + build
  docker-build-and-push:  # docker build + push ghcr.io/n3z3d/fortniteproject
    permissions:
      contents: read
      packages: write
```

### Cause fréquente d'échec GHCR push
- **Image name non-lowercase** : Le workflow normalise déjà via `tr '[:upper:]' '[:lower:]'` — vérifier que `IMAGE_NAME` est bien `ghcr.io/n3z3d/fortniteproject` (minuscules)
- **`permissions: packages: write` absent** : Déjà dans le job — mais vérifier que le job a bien ce block (pas seulement au niveau workflow)
- **Token insuffisant** : `GITHUB_TOKEN` fonctionne automatiquement dans GitHub Actions pour GHCR si `packages: write` est déclaré

### Scénarios de correction (si échec)

**Scénario A — Vitest step absent ou non visible :**
```yaml
# Vérifier dans le job frontend que ce step existe:
- name: Tests Vitest (unitaires)
  id: vitest
  continue-on-error: true
  shell: bash
  run: |
    set -o pipefail
    npm run test:vitest 2>&1 | tee vitest-ci.log
```

**Scénario B — Docker push 403 :**
```yaml
# Vérifier que le job a:
permissions:
  contents: read
  packages: write
# Et que le login step est présent:
- name: Log in to GHCR
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

**Scénario C — Image name invalide :**
```bash
# Le step de normalisation doit produire:
IMAGE_NAME=ghcr.io/n3z3d/fortniteproject  # tout en minuscules
```

### Pre-existing Gaps / Known Issues

- [KNOWN] Frontend: 0 failures pre-existing (baseline 2243/2243)
- [KNOWN] Backend: ~15 failures pre-existing exclues du CI via `-Dexcludes`
- [KNOWN] Le step Vitest utilise `continue-on-error: true` — des failures Vitest ne bloquent pas le CI mais sont loggées

### Project Structure Notes

```
.github/
└── workflows/
    └── ci.yml   ← potentiellement modifié si correction nécessaire
_bmad-output/implementation-artifacts/
└── sprint-status.yaml   ← sprint7-f2 → done + sprint8-f2 → review
```

### References

- [Source: _bmad-output/implementation-artifacts/sprint-7-retro-2026-03-12.md §Leçon L4]
- [Source: .github/workflows/ci.yml]
- [Source: sprint-status.yaml#sprint7-f2-ci-vitest-et-docker-push]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List

- `.github/workflows/ci.yml` — potentiellement modifié (si correction nécessaire)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — modifié (sprint7-f2 → done, sprint8-f2 → review)
