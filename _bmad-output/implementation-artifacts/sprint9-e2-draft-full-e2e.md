# Story sprint9-E2: DRAFT-FULL-01 — E2E flux draft serpent complet

Status: done

<!-- METADATA
  story_key: sprint9-e2-draft-full-e2e
  branch: main
  sprint: Sprint 9
-->

## Story

As a QA engineer,
I want a Playwright E2E test that exercises a complete snake draft from state creation to game ACTIVE,
so that the draft completion flow (all picks + finish-draft + ACTIVE status) is proven end-to-end and regressions are caught before they reach users.

## Acceptance Criteria

1. `DRAFT-FULL-01` crée un jeu en état DRAFTING via l'API (pattern `beforeAll`) — aucune dépendance sur la seed statique.
2. Chaque participant fait ses picks via `POST /api/games/{gameId}/draft/snake/pick` jusqu'à épuisement des slots (teamSize=1, 2 participants = 2 picks total).
3. Après tous les picks, `POST /api/draft/{gameId}/finish` est appelé → le jeu passe en statut `ACTIVE`.
4. Le statut `ACTIVE` est vérifié via `GET /api/games/{gameId}` (API) — pas d'assertion UI fragile sur WebSocket.
5. Le test utilise `test.setTimeout(35_000)`, `softDeleteLocalGamesByPrefix('E2E-DF-')`, et préfixe `E2E-DF-`.
6. Le test est en mode API-level (pas de navigation navigateur) — même pattern que `alt-flows.spec.ts` (ALT-01/02/03).
7. `test.skip()` gracieux si le backend n'est pas disponible (guard sur l'échec `beforeAll`).

## Tasks / Subtasks

- [x] Task 1: Recherche — comprendre les endpoints disponibles et les patterns existants (AC: #1, #6)
  - [x] 1.1: Lire `frontend/e2e/helpers/trade-swap-helpers.ts` — pattern `prepareTradeReadyGame`
  - [x] 1.2: Lire `frontend/e2e/alt-flows.spec.ts` — pattern API-level
  - [x] 1.3: Lire `frontend/e2e/draft-flow.spec.ts` — pattern `createStartedDraftGame`
  - [x] 1.4: Analyser `SnakeDraftController` + `GameDraftService` — confirmer que la transition DRAFTING→ACTIVE nécessite `POST /api/draft/{gameId}/finish`
- [x] Task 2: Créer `frontend/e2e/draft-full-flow.spec.ts` (AC: #1-#7)
  - [x] 2.1: Implémenter `DRAFT-FULL-01` — setup via API + picks complets + finish-draft + verify ACTIVE
- [x] Task 3: Mettre à jour `sprint-status.yaml` (AC: #5)

## Dev Notes

### Décision architecturale clé : DRAFTING → ACTIVE

La transition DRAFTING→ACTIVE n'est **PAS automatique** après tous les picks. Elle nécessite un appel explicite à :
```
POST /api/draft/{gameId}/finish
```
(base path `/api/drafts` ou `/api/draft`, mappage `/{gameId}/finish`)

Ce n'est pas `POST /api/games/{gameId}/finish-draft` (qui n'existe pas). Le contrôleur `DraftController` a `@RequestMapping({"/api/drafts", "/api/draft"})` → endpoint effectif : `POST /api/draft/{gameId}/finish`.

### Raison du choix API-level (pas UI)

Un vrai draft UI complet nécessiterait :
- Deux contextes navigateur en parallèle (thibaut + teddy)
- Coordination WebSocket en temps réel entre les deux
- Gestion des tours snake (le curseur peut assigner n'importe qui en premier selon shuffle aléatoire)

Ce niveau de complexité dépasse le budget de 35s par test et est extrêmement fragile. Le pattern établi (`alt-flows.spec.ts`) est API-level pour exactement cette raison. L'action item E2 de la retro sprint-8 précise : "Si non faisable [UI], documenter le compromis explicitement dans le test."

**Compromis documenté** : Le test DRAFT-FULL-01 vérifie la logique complète du draft (création → picks → finish → ACTIVE) via l'API directement. La couverture UI du draft snake est assurée par DRAFT-01/02/03 dans `draft-flow.spec.ts`.

### Endpoints utilisés

| Action | Endpoint |
|--------|----------|
| Créer jeu | `POST /api/games?user=admin` |
| Rejoindre | `POST /api/games/join?user={u}` |
| Démarrer draft | `POST /api/games/{id}/start-draft?user=admin` |
| Init curseurs snake | `POST /api/games/{id}/draft/snake/initialize?user=admin` |
| Tour actuel | `GET /api/games/{id}/draft/snake/turn?region=GLOBAL` |
| Participants | `GET /api/games/{id}/participants` |
| Soumettre pick | `POST /api/games/{id}/draft/snake/pick?user={u}` |
| Terminer draft | `POST /api/draft/{id}/finish` |
| Statut jeu | `GET /api/games/{id}?user=admin` |

### Player IDs (seed V1001)

```
BUGHA_EU_T1  = '10000000-0000-0000-0000-000000000001'  -- thibaut pick
AQUA_EU_T1   = '10000000-0000-0000-0000-000000000002'  -- teddy pick
```

Ces deux joueurs sont distincts, même région EU, non utilisés par `prepareTradeReadyGame` pour le pick de teddy (qui utilise `teddyOpening` = 000002 = Aqua). On réutilise les mêmes constantes.

### Paramètres du jeu de test (teamSize=1)

- `maxParticipants: 2` (admin + teddy → 2 participants)
- `teamSize: 1` — 1 joueur par équipe → 2 picks total pour finir le draft
- `tranchesEnabled: false` — pas de contrainte de tranche (simplifie le test)
- `draftMode: 'SNAKE'`

Avec `admin` + `teddy` (pas de `thibaut`) on évite les conflits avec les jeux `prepareTradeReadyGame` de `alt-flows.spec.ts` qui utilisent `admin + thibaut + teddy`.

**Correction** : Le jeu doit avoir exactement 2 participants pour que 2 picks (1 par participant) remplissent toutes les équipes et permettent `finish`.

### Algorithme picks dans `seedAllPicks`

La boucle itère max `maxPicks * 2` fois pour éviter les boucles infinies. À chaque itération :
1. Résoudre le picker actuel via `/turn` + `/participants`
2. Soumettre le pick du joueur assigné à ce picker
3. Vérifier via `/details` si tous les participants ont au moins 1 joueur sélectionné

### Pattern `test.skip()` / guard

Si `beforeAll` lève une exception (backend non disponible), `fixture` reste `null`. Chaque test vérifie `if (!fixture) { test.skip(); return; }` pour une dégradation gracieuse.

### Project Structure Notes

```
frontend/e2e/
├── draft-full-flow.spec.ts    ← NOUVEAU (1 test: DRAFT-FULL-01)
├── alt-flows.spec.ts           ← inchangé (3 tests: ALT-01/02/03)
├── draft-flow.spec.ts          ← inchangé (3 tests: DRAFT-01/02/03)
├── helpers/
│   ├── app-helpers.ts          ← inchangé
│   ├── trade-swap-helpers.ts   ← inchangé
│   └── local-db-helpers.ts     ← inchangé
```

### References

- [Source: frontend/e2e/alt-flows.spec.ts — pattern API-level référence]
- [Source: frontend/e2e/helpers/trade-swap-helpers.ts — prepareTradeReadyGame + seedOpeningRosters]
- [Source: frontend/e2e/draft-flow.spec.ts — createStartedDraftGame]
- [Source: SnakeDraftController.java — POST /api/games/{gameId}/draft/snake/pick]
- [Source: DraftController.java — POST /api/draft/{gameId}/finish]
- [Source: GameDraftService.java — finishDraft() → game.completeDraft() → ACTIVE]
- [Source: sprint8-retro-2026-03-15.md §E2 — "créer état DRAFTING via API admin en beforeAll"]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- Recherche complète : 8 fichiers backend + 4 fichiers E2E analysés
- Découverte clé : transition DRAFTING→ACTIVE via `POST /api/draft/{id}/finish` (DraftController, non automatique)
- Choix API-level documenté et justifié (WebSocket multi-contexte infaisable en 35s)

### Completion Notes List

- `DRAFT-FULL-01` implémenté en mode API-level (conforme à alt-flows.spec.ts)
- teamSize=1, 2 participants (admin+teddy), 2 picks total avant finish
- Guard `fixture` null → `test.skip()` si backend non disponible
- `softDeleteLocalGamesByPrefix('E2E-DF-')` dans `beforeAll` et `afterAll`
- Endpoint `POST /api/draft/{gameId}/finish` utilisé pour la transition DRAFTING→ACTIVE
- Statut ACTIVE vérifié via `GET /api/games/{id}` avec `expect.poll()`

### File List

- `frontend/e2e/draft-full-flow.spec.ts` — créé (1 test: DRAFT-FULL-01)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — mis à jour
