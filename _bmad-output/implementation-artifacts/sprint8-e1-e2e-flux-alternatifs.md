# Story sprint8-E1: E2E flux alternatifs — trade refusé, swap invalide, draft complet

Status: ready-for-dev

<!-- METADATA
  story_key: sprint8-e1-e2e-flux-alternatifs
  branch: story/sprint8-e1-e2e-flux-alternatifs
  sprint: Sprint 8
-->

## Story

As a QA engineer,
I want Playwright E2E tests covering rejection and invalid-input flows (trade refused, invalid swap, complete draft),
so that the backend validation logic is proven end-to-end and regressions in error paths are caught before they reach users.

## Acceptance Criteria

1. Un test E2E `TRADE-REJECT-01` couvre : proposer un trade → le participant adverse le **refuse** → le trade reste à l'état `REJECTED` dans l'UI.
2. Un test E2E `SWAP-INVALID-01` couvre : tenter un swap solo avec un joueur de rang **meilleur** (violation de règle) → le backend retourne une erreur → un message d'erreur est visible dans l'UI.
3. Un test E2E `DRAFT-FULL-01` couvre : un draft serpent se termine avec toutes les équipes **complètes** (tous les slots remplis) → le statut de la partie passe à `ACTIVE` ou `FINISHED`.
4. Les 3 nouveaux tests utilisent les helpers existants (`waitForPageReady`, `forceLoginWithProfile`, `cleanupTradeFixtureUsers`) — 0 duplication de boilerplate.
5. Les tests sont dans `frontend/e2e/` et s'intègrent sans régression dans la suite Playwright existante (17 tests + 3 nouveaux = 20 tests minimum).
6. Chaque test inclut `test.setTimeout(35_000)` et un guard URL `waitForPageReady` en début.

## Tasks / Subtasks

- [ ] Task 1: Analyser les helpers existants et la seed data disponible (AC: #4)
  - [ ] 1.1: Lire `frontend/e2e/helpers/app-helpers.ts`, `trade-swap-helpers.ts`, `local-db-helpers.ts`
  - [ ] 1.2: Lire `frontend/e2e/trade-swap-flow.spec.ts` pour comprendre les patterns existants
  - [ ] 1.3: Identifier les données seed V1001/V1002 utilisables (joueurs Bugha/Aqua/Clix, partie CREATING)
- [ ] Task 2: Créer `frontend/e2e/alternative-flows.spec.ts` (AC: #1, #2, #3, #5, #6)
  - [ ] 2.1: Implémenter `TRADE-REJECT-01` — proposer trade + refus adverse + vérifier état REJECTED
  - [ ] 2.2: Implémenter `SWAP-INVALID-01` — swap avec violation de règle + vérifier message erreur UI
  - [ ] 2.3: Implémenter `DRAFT-FULL-01` — draft serpent complet + vérifier statut partie ACTIVE/FINISHED
- [ ] Task 3: Valider en local (AC: #5)
  - [ ] 3.1: Lancer `npm run test:e2e` — confirmer 0 régression sur les 17 tests existants
  - [ ] 3.2: Confirmer que les 3 nouveaux tests passent (ou documenter les skip avec raison)

## Dev Notes

### Stack E2E actuelle
- **Framework** : Playwright (`frontend/playwright.config.ts`)
- **Tests existants** : `frontend/e2e/` — smoke.spec.ts (8), game-lifecycle.spec.ts, draft-flow.spec.ts, trade-swap-flow.spec.ts, trade-multi-flow.spec.ts, trade-dashboard.spec.ts, full-game-flow.spec.ts
- **Helpers** : `frontend/e2e/helpers/app-helpers.ts`, `trade-swap-helpers.ts`, `local-db-helpers.ts`
- **Commande** : `npm run test:e2e` (requiert app sur `:4200` + backend sur `:8080`)

### Pattern standard spec E2E (à réutiliser)
```typescript
import { test, expect } from '@playwright/test';
import { waitForPageReady, forceLoginWithProfile } from './helpers/app-helpers';
import { cleanupTradeFixtureUsers, softDeleteLocalGamesByPrefix } from './helpers/local-db-helpers';

const BACKEND_URL = process.env['BACKEND_URL'] ?? 'http://localhost:8080';

test.describe('Flux alternatifs', () => {
  test.beforeAll(async () => {
    await cleanupTradeFixtureUsers(BACKEND_URL);
    await softDeleteLocalGamesByPrefix('E2E-ALT-', BACKEND_URL);
  });

  test('TRADE-REJECT-01: trade refusé par le participant adverse', async ({ page }) => {
    test.setTimeout(35_000);
    await forceLoginWithProfile(page, 'thibaut');
    if (!await waitForPageReady(page, '/games')) { test.skip(); return; }
    // ... logique de test
  });
});
```

### TRADE-REJECT-01 — Logique
1. Login `thibaut` (proposant) + login `marcel` (recevant, via 2e page/contexte)
2. `thibaut` propose un trade depuis `/games/{id}/trades`
3. `marcel` reçoit la notification → clique "Refuser"
4. `thibaut` recharge → vérifier que le trade est `REJECTED` dans l'historique
- **Note** : utiliser `expect.poll()` pour l'état async du trade (même pattern que `trade-multi-flow.spec.ts`)
- **Endpoint** : `DELETE /api/games/{gameId}/trades/{tradeId}` pour le refus

### SWAP-INVALID-01 — Logique
1. Login `thibaut` avec un joueur de tranche 1 dans son équipe
2. Tenter un swap vers un joueur de tranche 0 (rang meilleur = violation)
3. Vérifier qu'un snackbar/message d'erreur apparaît dans l'UI (`mat-snack-bar-container` ou `.error-message`)
4. Vérifier que le roster n'a PAS changé après l'erreur
- **Endpoint** : `POST /api/games/{gameId}/draft/swap-solo` → attendu 400/409
- **Message erreur backend** : `InvalidSwapException` → `DomainExceptionHandler` → 400

### DRAFT-FULL-01 — Logique
1. Créer/utiliser une partie DRAFTING avec 2 participants et 1 joueur par équipe (slots réduits)
2. `thibaut` et `marcel` font chacun leur pick via `POST /api/draft/snake/{draftId}/pick`
3. Vérifier que le statut de la partie passe à `ACTIVE` dans l'UI (badge de statut)
- **Note** : ce test peut être fragile si la seed data n'a pas de partie DRAFTING prête → prévoir un `test.skip()` si les données ne sont pas disponibles
- Alternative : utiliser l'API admin pour créer une partie de test en DRAFTING

### Patterns critiques à appliquer
- **Pas de `waitForTimeout(N)` fixe** → utiliser `expect.poll()` ou `toBeAttached({ timeout: N })`
- **Pas de variable zombie** → toute `const x = await locator.textContent()` doit être consommée dans un `expect(x)`
- **`test.skip()` si données manquantes** → jamais de `throw` ou fail dur si l'environnement n'est pas prêt
- **Préfixe E2E** : utiliser `E2E-ALT-` pour les fixtures créées par ces tests

### Helpers disponibles (ne pas recréer)
```typescript
// app-helpers.ts
waitForPageReady(page, route, waitMs?)  // navigation + guard URL
forceLoginWithProfile(page, username)   // login via profil-selection UI
loginAsAdmin(page)                      // login admin

// trade-swap-helpers.ts
// (inspecter le fichier pour les helpers disponibles)

// local-db-helpers.ts
cleanupTradeFixtureUsers(backendUrl)
softDeleteLocalGamesByPrefix(prefix, backendUrl)
```

### Pre-existing Gaps / Known Issues

- [KNOWN] Frontend: 0 failures pre-existing Vitest (baseline 2243/2243)
- [KNOWN] Les tests E2E requièrent app sur :4200 + backend sur :8080 — skip automatique si non disponible
- [KNOWN] `DRAFT-FULL-01` peut nécessiter un `test.skip()` si la seed data n'a pas de partie DRAFTING active — documenter dans le test
- [KNOWN] Backend: ~15 failures pre-existing exclues du CI

### Project Structure Notes

```
frontend/e2e/
├── alternative-flows.spec.ts    ← NOUVEAU (3 tests: TRADE-REJECT-01, SWAP-INVALID-01, DRAFT-FULL-01)
├── helpers/
│   ├── app-helpers.ts            ← inchangé (réutilisé)
│   ├── trade-swap-helpers.ts     ← inchangé (réutilisé)
│   └── local-db-helpers.ts       ← inchangé (réutilisé)
```

### References

- [Source: project-context.md §Pièges Connus — Variable zombie E2E, waitForPageReady pattern]
- [Source: sprint-6-retro-2026-03-10.md §Patterns §Code review E2E]
- [Source: sprint-7-retro-2026-03-12.md §Action Items E1]
- [Source: frontend/e2e/trade-swap-flow.spec.ts — pattern de référence]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List

- `frontend/e2e/alternative-flows.spec.ts` — créé (3 tests E2E flux alternatifs)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — modifié
