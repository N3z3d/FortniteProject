# Story Sprint7 F2: CI Vitest et Docker push GHCR

Status: blocked

<!-- METADATA
  story_key: sprint7-f2-ci-vitest-et-docker-push
  branch: story/sprint7-f2-ci-vitest-et-docker-push
  sprint: Sprint 7
  priority: P0
  Note: Story CI/process. Objectif: rendre le pipeline GitHub Actions exploitable avec un step Vitest visible et un job GHCR robuste, sans prétendre que les failures Vitest pre-existantes sont déjà corrigées.
-->

## Story

As a maintainer preparing the app to be demo-able and CI-driven,
I want the GitHub Actions pipeline to execute Vitest explicitly and build/push the production image to GHCR with a robust image naming strategy,
so that CI reports the real frontend unit-test baseline and can publish a deployable container once the branch is merged on `main`.

## Acceptance Criteria

1. Le workflow `.github/workflows/ci.yml` contient un step Vitest explicite dans le job frontend.
2. Le step Vitest ne masque plus silencieusement son exit code dans le shell; son état et son log restent visibles même tant que `sprint7-z1` n a pas fermé les rouges connus.
3. Le workflow publie un artefact de log Vitest exploitable pour analyser la baseline restante.
4. Le job Docker pousse une image GHCR avec un nom d image normalisé en minuscules pour éviter les erreurs de casse GitHub/GHCR.
5. Le build Angular production est validé localement.
6. Le build Docker `production` est validé localement.
7. Les rouges Vitest restants sont documentés comme le scope de la story suivante `sprint7-z1-fix-zonejs-debounce-failures`, pas cachés par `F2`.

## Tasks / Subtasks

- [x] Task 1 - Qualifier l état réel du pipeline frontend actuel (AC: #1, #2, #3, #7)
  - [x] 1.1 Vérifier le workflow CI existant
  - [x] 1.2 Mesurer le baseline réel de `npm run test:vitest`

- [x] Task 2 - Durcir le step Vitest CI sans le masquer (AC: #1, #2, #3, #7)
  - [x] 2.1 Remplacer le `|| true` shell par une step CI explicite avec état visible
  - [x] 2.2 Publier le log Vitest en artefact
  - [x] 2.3 Ajouter un résumé CI pour rendre le statut lisible

- [x] Task 3 - Fiabiliser le job Docker GHCR (AC: #4, #6)
  - [x] 3.1 Normaliser le nom d image GHCR en minuscules
  - [x] 3.2 Valider localement le build `production`

- [x] Task 4 - Validation locale et synchronisation BMAD (AC: #5, #6, #7)
  - [x] 4.1 Valider `npm run build`
  - [x] 4.2 Consigner le baseline Vitest encore rouge
  - [x] 4.3 Mettre la story en `review` avec dépendance explicite au premier run GitHub réel

## Dev Notes

### Current Measured Baseline

- Le workflow CI comportait déjà un step Vitest et un job Docker GHCR, mais le step frontend utilisait `npm run test:vitest || true`, ce qui masquait le vrai statut shell.
- Le nom d image GHCR utilisait directement `${{ github.repository }}`, ce qui peut casser sur GHCR quand le repo contient des majuscules.
- A l ouverture de `F2`, la suite Vitest etait encore rouge; `sprint7-z1-fix-zonejs-debounce-failures` l a depuis fermee et permet maintenant de rendre la step Vitest bloquante a nouveau.

### Scope Guardrails

- Cette story ne corrige pas les failures Vitest elles-mêmes.
- Cette story ne déploie rien publiquement et ne rouvre pas le staging.
- Cette story prépare le pipeline et rend la dette restante observable au lieu de la cacher.

### Technical Requirements

- Fichiers principaux:
  - `.github/workflows/ci.yml`
  - `frontend/package.json`
  - `frontend/vitest.config.mts`
  - `frontend/src/vitest-setup.ts`

### Testing Requirements

- `npm run test:vitest` doit être exécuté pour capturer le baseline réel.
- `npm run build` doit être validé localement.
- `docker build --target production -t fortnite-ci-local .` doit être validé localement.

### Pre-existing Gaps / Known Issues

- [KNOWN] Le push GHCR réel ne peut pas être prouvé localement; il dépend du premier run GitHub Actions sur `main`.

### References

- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `.github/workflows/ci.yml`
- `frontend/package.json`
- `frontend/vitest.config.mts`
- `frontend/src/vitest-setup.ts`
- `Dockerfile`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story créée le 2026-03-11 depuis le backlog Sprint 7 prioritaire
- Validation locale détaillée dans `_bmad-output/implementation-artifacts/sprint7-f2-ci-vitest-et-docker-push-runtime-validation-2026-03-11.md`

### Implementation Plan

- Rendre le step Vitest CI explicite et observable.
- Garder le job frontend vert tant que Z1 n a pas corrigé la baseline rouge.
- Fiabiliser le job GHCR pour éviter les erreurs de nommage.
- Valider localement les builds réellement exécutables hors GitHub.

### Completion Notes List

- Le step Vitest du workflow utilise maintenant `continue-on-error: true` avec `set -o pipefail`, ce qui garde l exit code réel visible au lieu de le masquer par `|| true`.
- Le workflow upload maintenant `frontend-vitest-log` pour conserver le baseline exact des failures restantes.
- Un résumé GitHub Actions est ajouté pour distinguer clairement un passage vert d un rouge non bloquant en attente de `Z1`.
- Le job Docker normalise maintenant le nom d image GHCR en minuscules avant build/push.
- Depuis la fermeture de `Z1`, la step Vitest CI est redevenue bloquante; `continue-on-error` a été retiré pour que toute régression frontend échoue réellement la job `frontend`.
- Validation locale réussie:
  - `npm run build` -> OK
  - `docker build --target production -t fortnite-ci-local .` -> OK
- La story est bloquée uniquement par la preuve externe du push GHCR réel, qui devra être confirmée par le premier run GitHub Actions sur `main`.

### File List

- `_bmad-output/implementation-artifacts/sprint7-f2-ci-vitest-et-docker-push.md`
- `_bmad-output/implementation-artifacts/sprint7-f2-ci-vitest-et-docker-push-runtime-validation-2026-03-11.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `.github/workflows/ci.yml`

### Change Log

- 2026-03-11 - Story créée et workflow CI durci: step Vitest visible, artefact log, summary CI, image GHCR normalisée en minuscules.
- 2026-03-11 - Validation locale du build Angular et du build Docker `production` effectuée; baseline Vitest rouge qualifiée pour `Z1`.
- 2026-03-12 - Code review appliquée: la step Vitest CI redevient bloquante maintenant que `Z1` a fermé le baseline rouge; la story passe en `blocked` en attente du premier run GitHub réel.

## Senior Developer Review (AI)

### Reviewer

GPT-5 Codex

### Date

2026-03-12

### Outcome

Approved on code, but blocked on external validation. Story status should move to `blocked` until the first GitHub Actions run on `main` proves the GHCR push.

### Findings Fixed

1. HIGH - `.github/workflows/ci.yml`
   - La step Vitest etait encore non bloquante via `continue-on-error: true` alors que `Z1` a maintenant ferme le baseline rouge. Dans cet etat, une regression frontend pouvait repasser en faux vert en CI. La step Vitest est redevenue bloquante et le summary GitHub a ete realigne.

### Validation

- `npx vitest run src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.spec.ts`
- `npm run test:vitest`
- `npm run build`

### Result Summary

- Le workflow CI est coherent avec un baseline Vitest vert
- Le reliquat `F2` n est plus du code CI local mais uniquement la preuve GHCR distante
