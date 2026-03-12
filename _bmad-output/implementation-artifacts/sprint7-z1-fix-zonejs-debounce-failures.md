# Story Sprint7 Z1: Fix Zone.js / Vitest failures

Status: done

<!-- METADATA
  story_key: sprint7-z1-fix-zonejs-debounce-failures
  branch: story/sprint7-z1-fix-zonejs-debounce-failures
  sprint: Sprint 7
  priority: P1
  Note: Story de stabilisation tests. Objectif: fermer les faux rouges Vitest lies aux patterns legacy fakeAsync/tick, done() et mocks incomplets, sans elargir le scope produit.
-->

## Story

As a maintainer trying to keep CI trustworthy,
I want the Vitest suite to stop failing on Zone.js-era patterns and incomplete observable mocks,
so that the frontend test baseline becomes actionable and the CI step from `sprint7-f2` can report the real state of the app.

## Acceptance Criteria

1. Les specs frontend qui echouaient sur les patterns `fakeAsync/tick()` incompatibles avec Vitest ou jsdom sont stabilisees sans changer le code produit pour "faire passer" artificiellement les tests.
2. Les callbacks `done()` legacy qui cassaient sous Vitest sont supprimes des specs synchrones et remplaces par des assertions deterministes.
3. Les tests qui appelaient des spies renvoyant `undefined` a la place d un `Observable` sont corriges pour ne plus generer d erreurs non interceptees.
4. `npm run test:vitest` passe localement avec zero failure bloquante.
5. Le resultat final et les reliquats eventuels sont documentes dans un artefact de validation runtime.

## Tasks / Subtasks

- [x] Task 1 - Qualifier le baseline Vitest reel et isoler les premiers blockers (AC: #1, #2, #3, #4)
  - [x] 1.1 Rejouer `npm run test:vitest` et identifier les suites reellement bloquantes
  - [x] 1.2 Distinguer les vraies failures des warnings jsdom/Angular non bloquants

- [x] Task 2 - Stabiliser les specs debounce / fakeAsync incompatibles (AC: #1, #4)
  - [x] 2.1 Realigner les tests catalogue dependants du debounce sur une attente explicite
  - [x] 2.2 Garder les assertions produit inchangées en ne corrigeant que le harness de test

- [x] Task 3 - Supprimer les patterns `done()` legacy et corriger les mocks incomplets (AC: #2, #3, #4)
  - [x] 3.1 Nettoyer les specs synchrones encore basees sur `done()`
  - [x] 3.2 Ajouter des retours `Observable` explicites la ou Vitest remontait `subscribe` sur `undefined`

- [x] Task 4 - Validation finale et synchronisation BMAD (AC: #4, #5)
  - [x] 4.1 Rejouer la suite Vitest complete
  - [x] 4.2 Exporter un rapport JSON de preuve
  - [x] 4.3 Mettre la story en `review`

## Dev Notes

### Current Measured Baseline

- Le baseline de depart ne se limitait pas a `fakeAsync/tick()`: plusieurs suites legacy etaient aussi cassees par des callbacks `done()` et des spies `Observable` incomplets.
- Le scope a ete garde strictement cote tests: aucune logique produit n a ete modifiee pour forcer un vert artificiel.

### Scope Guardrails

- Cette story ne change pas le comportement fonctionnel de l application.
- Cette story ne traite pas l infrastructure/staging.
- Les warnings jsdom/canvas/scrollTo et certains warnings Angular restent qualifies comme bruit non bloquant tant que la suite est verte.

### Technical Requirements

- Fichiers principaux:
  - `frontend/src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.spec.ts`
  - `frontend/src/app/features/diagnostic/diagnostic.component.spec.ts`
  - `frontend/src/app/features/game/services/game-detail-actions.service.spec.ts`
  - `frontend/src/app/core/repositories/leaderboard.repository.spec.ts`
  - `frontend/src/app/core/repositories/dashboard.repository.spec.ts`
  - `frontend/src/app/core/facades/dashboard.facade.spec.ts`

### Testing Requirements

- `npm run test:vitest` doit passer localement.
- `npx vitest run --reporter=json --outputFile vitest-z1-report.json` doit produire une preuve exploitable.

### Pre-existing Gaps / Known Issues

- [KNOWN] Le run Vitest reste verbeux a cause de warnings jsdom (`canvas`, `scrollTo`) et warnings Angular (`NG0912`, `NG0914`), mais ils ne rendent plus le build rouge.
- [KNOWN] Certaines specs avaient deja du contexte local non lie directement a `Z1`; le travail a ete borne aux patterns de test bloquants.

### References

- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/sprint7-z1-fix-zonejs-debounce-failures-runtime-validation-2026-03-11.md`
- `frontend/vitest-z1-report.json`
- `_bmad-output/implementation-artifacts/sprint7-f2-ci-vitest-et-docker-push.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story creee le 2026-03-11 depuis le backlog Sprint 7 prioritaire
- Validation runtime detaillee dans `_bmad-output/implementation-artifacts/sprint7-z1-fix-zonejs-debounce-failures-runtime-validation-2026-03-11.md`

### Implementation Plan

- Traiter d abord les suites de debounce reel.
- Eliminer ensuite les callbacks `done()` qui rendaient Vitest instable.
- Corriger les mocks `Observable` incomplets qui remontaient des `subscribe` sur `undefined`.
- Figé le resultat final dans un rapport JSON et basculer la story en `review`.

### Completion Notes List

- La suite catalogue dependante du debounce ne repose plus sur `fakeAsync/tick()` mais sur des helpers `async/await` deterministes.
- Les callbacks `done()` legacy ont ete supprimes des specs synchrones encore rouges dans les repositories, facades et services frontend concernes.
- Les faux rouges les plus bruyants restants ont ete fermes par des mocks plus stricts dans `diagnostic.component.spec.ts` et `game-detail-actions.service.spec.ts`.
- Le baseline final n est plus "<= 5 failures qualifiees" mais `0 failure`:
  - `721` suites / `721` vertes
  - `2243` tests / `2243` verts
- Les warnings jsdom/Angular restent visibles dans les logs, mais ils ne bloquent plus la CI Vitest.
- La story est prete pour review.

### File List

- `_bmad-output/implementation-artifacts/sprint7-z1-fix-zonejs-debounce-failures.md`
- `_bmad-output/implementation-artifacts/sprint7-z1-fix-zonejs-debounce-failures-runtime-validation-2026-03-11.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `frontend/src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.spec.ts`
- `frontend/src/app/features/diagnostic/diagnostic.component.spec.ts`
- `frontend/src/app/features/game/services/game-detail-actions.service.spec.ts`
- `frontend/src/app/core/repositories/leaderboard.repository.spec.ts`
- `frontend/src/app/core/repositories/dashboard.repository.spec.ts`
- `frontend/src/app/core/facades/dashboard.facade.spec.ts`
- `frontend/src/app/features/dashboard/services/dashboard-data.service.spec.ts`
- `frontend/src/app/features/catalogue/services/player-catalogue.service.spec.ts`
- `frontend/src/app/features/game/services/game-command.service.spec.ts`
- `frontend/src/app/core/services/websocket.service.spec.ts`
- `frontend/src/app/features/teams/services/team-detail-data.service.spec.ts`
- `frontend/src/app/features/admin/services/admin.service.spec.ts`
- `frontend/src/app/core/services/leaderboard.service.spec.ts`
- `frontend/src/app/core/services/user-games.store.spec.ts`
- `frontend/src/app/core/services/user-context.service.spec.ts`
- `frontend/src/app/features/legal/legal.component.spec.ts`
- `frontend/src/app/features/game/services/game-query.service.spec.ts`
- `frontend/src/app/features/game/services/game-data.service.spec.ts`
- `frontend/src/app/features/trades/components/trade-proposal/trade-proposal.component.spec.ts`
- `frontend/vitest-z1-report.json`

### Change Log

- 2026-03-11 - Stabilisation des suites Vitest rouges sur debounce, callbacks `done()` et mocks `Observable` incomplets.
- 2026-03-11 - Validation complete `2243/2243` verte et export JSON `frontend/vitest-z1-report.json`.
- 2026-03-12 - Code review appliquee: le test catalogue debounce passe sur fake timers Vitest au lieu d une attente reelle, puis story approuvee.

## Senior Developer Review (AI)

### Reviewer

GPT-5 Codex

### Date

2026-03-12

### Outcome

Approved after fixes. Story status can move to `done`.

### Findings Fixed

1. MEDIUM - `frontend/src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.spec.ts`
   - Le fix initial `Z1` attendait `250ms` en temps reel pour laisser passer le `debounceTime`, ce qui gardait la suite dependante de l horloge murale et la ralentissait inutilement. Le spec utilise maintenant `vi.useFakeTimers()` et `vi.advanceTimersByTimeAsync(...)`, conformement au pattern Vitest cible pour les debounces reels.

### Validation

- `npx vitest run src/app/features/catalogue/pages/player-catalogue-page/player-catalogue-page.component.spec.ts`
- `npm run test:vitest`

### Result Summary

- La suite catalogue debounce reste verte sans attente reelle
- La suite Vitest complete reste verte apres le fix de review
