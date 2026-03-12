# Story 8.6: Stabilisation auth E2E et admin guard

Status: done

<!-- METADATA
  story_key: 8-6-stabilisation-auth-e2e-et-admin-guard
  branch: story/8-6-stabilisation-auth-e2e-et-admin-guard
  sprint: Sprint 4
  Note: Story locale de hardening E2E. Objectif: fermer les faux negatifs auth/admin reveles par le rerun complet post-8.5 et remettre le pack Playwright entier en vert, rerunnable, sans reset manuel.
-->

## Story

As a local user relying on the full E2E pack as a prod-like safety net,
I want the auth helpers, seeded admin access, and logout assertions to behave consistently on the real local runtime contract,
so that the complete Playwright suite becomes green and rerunnable without hiding admin or auth regressions behind flaky test scaffolding.

## Acceptance Criteria

1. Les helpers E2E seedes n envoient plus un faux bearer token sur les appels API locaux quand le runtime attend `X-Test-User`.
2. Les parcours admin E2E par acces direct (`/admin`, `/admin/pipeline`, `/admin/users`, `/admin/games`, `/admin/database`, `/admin/logs`, `/admin/errors`) passent sur une session seedee sans redirection parasite vers `/games`.
3. Les assertions de logout et les tests smoke/auth n heritent plus de headers ou session forces apres nettoyage.
4. Les correctifs auth/admin sont couverts par des tests unitaires frontend adaptes.
5. Le pack Playwright complet repasse au vert au moins une fois sans echec.
6. La rerunabilite du pack complet est prouvee par un second run vert sans reset manuel.

## Tasks / Subtasks

- [x] Task 1 - Qualifier le vrai blocage restant apres 8.5 (AC: #1, #2, #3)
  - [x] 1.1 Rerun le pack complet et isoler les echecs encore reels
  - [x] 1.2 Distinguer les bugs de contrat runtime des faux negatifs de helper

- [x] Task 2 - Realigner les helpers auth seedes sur le contrat backend local (AC: #1, #3)
  - [x] 2.1 Utiliser `X-Test-User` pour les tokens synthetiques E2E au lieu d un faux bearer
  - [x] 2.2 Nettoyer explicitement la session forcee et les headers extra dans les specs auth/smoke

- [x] Task 3 - Fermer le gap admin route/guard revele par le rerun (AC: #2, #4)
  - [x] 3.1 Corriger la logique du `AdminGuard` pour utiliser la vraie autorisation admin
  - [x] 3.2 Mettre a jour les tests unitaires associes

- [x] Task 4 - Verification complete et documentation BMAD (AC: #4, #5, #6)
  - [x] 4.1 Lancer les tests unitaires frontend cibles
  - [x] 4.2 Lancer `admin.spec.ts`
  - [x] 4.3 Lancer le pack Playwright complet deux fois
  - [x] 4.4 Consigner la preuve runtime et remettre le board a jour

## Dev Notes

### Current Measured Baseline

- A la cloture de `8.5`, le scope trade/realtime etait vert mais le pack Playwright complet restait en echec.
- Le rerun complet montrait `46 passed`, `7 failed`, `1 did not run`.
- Les echecs restants etaient concentres sur l auth E2E legacy et les routes admin directes.

### Scope Guardrails

- Cette story ne rouvre ni Railway, ni staging, ni infrastructure.
- Cette story ne remplace pas l architecture auth de production; elle ferme le contrat local E2E.
- Cette story reste limitee aux faux negatifs auth/admin qui empechent le pack complet de jouer son role de filet de securite.

### Technical Requirements

- Surfaces prioritaires:
  - `frontend/e2e/helpers/app-helpers.ts`
  - `frontend/e2e/auth.spec.ts`
  - `frontend/e2e/smoke.spec.ts`
  - `frontend/e2e/admin.spec.ts`
  - `frontend/src/app/core/interceptors/auth.interceptor.ts`
  - `frontend/src/app/core/guards/admin.guard.ts`

### Testing Requirements

- Toute correction auth/admin doit avoir au moins un test unitaire frontend adapte.
- La story n est pas terminee tant que `admin.spec.ts` et le pack complet ne passent pas.
- Le second rerun vert sans reset manuel est obligatoire pour considerer la story fermee.

### Pre-existing Gaps / Known Issues

- [KNOWN] Les helpers E2E auth contiennent encore un reliquat de nettoyage legacy dans `app-helpers.ts` qui merite une rationalisation separee si le module evolue encore.
- [KNOWN] La vraie auth publique/staging reste hors scope tant que l app reste en phase locale prod-like.

### References

- `_bmad-output/planning-artifacts/epics.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/8-5-couverture-e2e-multi-trade-realtime.md`
- `_bmad-output/implementation-artifacts/8-5-couverture-e2e-multi-trade-realtime-runtime-validation-2026-03-10.md`
- `docs/testing/E2E_LOCAL_RUNBOOK.md`
- `frontend/e2e/auth.spec.ts`
- `frontend/e2e/admin.spec.ts`
- `frontend/e2e/smoke.spec.ts`
- `frontend/e2e/helpers/app-helpers.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.ts`
- `frontend/src/app/core/guards/admin.guard.ts`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Story created on 2026-03-11 from the remaining full-pack auth/admin failures observed after `8.5`
- Runtime validation captured in `_bmad-output/implementation-artifacts/8-6-stabilisation-auth-e2e-et-admin-guard-runtime-validation-2026-03-11.md`

### Implementation Plan

- Qualifier les vraies causes des echecs restants du pack complet.
- Corriger d abord le contrat auth E2E seed local.
- Fermer ensuite le redirect admin parasite sur navigation directe.
- Rejouer le pack complet jusqu a preuve de rerunabilite.

### Completion Notes List

- Le vrai blocage auth etait contractuel: les tokens synthetiques `e2e.*` etaient envoyes en `Authorization: Bearer ...`, ce qui court-circuitait le fallback backend `X-Test-User` utilise en local. `AuthInterceptor` envoie maintenant uniquement `X-Test-User` sur ces sessions seedes.
- Les specs `auth` et `smoke` nettoient maintenant explicitement les headers/session forces, ce qui remet `AUTH-03` et `SMOKE-05` en coherence avec le runtime reel.
- Le redirect admin parasite n etait pas un bug Playwright: `AdminGuard` verifiait `currentUser.role === 'Administrateur'`, alors que la session seedee persistait `jwt_user.role = ADMIN`. Le guard s appuie maintenant sur `UserContextService.isAdmin()`, donc l acces direct `/admin*` ne tombe plus a tort sur `/games`.
- `admin.spec.ts` passe maintenant `9/9` sur session seedee locale.
- Le pack Playwright complet est revenu vert et rerunnable:
  - premier rerun: `54 passed`, `0 failed`
  - second rerun sans reset manuel: `54 passed`, `0 failed`
- La story ferme donc le reliquat auth/admin laisse par `8.5` et remet le pack local dans un etat vraiment exploitable comme filet de securite.

### File List

- `_bmad-output/implementation-artifacts/8-6-stabilisation-auth-e2e-et-admin-guard.md`
- `_bmad-output/implementation-artifacts/8-6-stabilisation-auth-e2e-et-admin-guard-runtime-validation-2026-03-11.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/planning-artifacts/epics.md`
- `docs/testing/E2E_LOCAL_RUNBOOK.md`
- `frontend/e2e/admin.spec.ts`
- `frontend/e2e/auth.spec.ts`
- `frontend/e2e/helpers/app-helpers.ts`
- `frontend/e2e/smoke.spec.ts`
- `frontend/src/app/core/guards/admin.guard.spec.ts`
- `frontend/src/app/core/guards/admin.guard.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.spec.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.ts`

### Change Log

- 2026-03-11 - Story created to close the remaining auth/admin E2E false negatives revealed after 8.5.
- 2026-03-11 - Synthetic E2E auth now uses the local `X-Test-User` contract, logout-related specs clean forced session state, `AdminGuard` was realigned on `isAdmin()`, and the full Playwright pack became green twice consecutively.

## Senior Developer Review (AI)

### Reviewer

GPT-5 Codex

### Date

2026-03-11

### Outcome

Approved after fixes. Story status can move to `done`.

### Findings Fixed

1. HIGH - `frontend/src/app/core/interceptors/auth.interceptor.ts`
   - Les tokens synthetiques E2E etaient convertis en faux bearer tokens, ce qui empechait le fallback backend local de reconnaitre `X-Test-User`. Le contrat seed local est maintenant coherent avec le runtime Docker.
2. HIGH - `frontend/src/app/core/guards/admin.guard.ts`
   - Le guard dependait d un libelle UI (`Administrateur`) au lieu de la vraie autorisation admin, ce qui faisait echouer les acces directs `/admin*` apres refresh ou navigation seedee. Le guard utilise maintenant `isAdmin()`.
3. MEDIUM - `frontend/e2e/auth.spec.ts` et `frontend/e2e/smoke.spec.ts`
   - Les tests de logout et smoke reutilisaient encore un contexte force, ce qui contaminait certaines assertions protegees. Le nettoyage explicite des headers/session a ete ajoute.

### Validation

- `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/core/guards/admin.guard.spec.ts --include src/app/core/interceptors/auth.interceptor.spec.ts`
- `npx playwright test e2e/admin.spec.ts`
- `npx playwright test`
- second rerun `npx playwright test`

### Result Summary

- Story `8.6` satisfait ses 6 acceptance criteria.
- Le pack Playwright complet est maintenant `54 passed`, `0 failed`, et rerunnable sur deux runs consecutifs.
- Le prochain travail BMAD utile n est plus une story E2E locale Epic 8, mais le backlog prioritaire Sprint 7 hors infra publique.
