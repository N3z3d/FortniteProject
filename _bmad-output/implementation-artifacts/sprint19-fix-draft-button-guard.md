# Story: sprint19-fix-draft-button-guard

## Story
**En tant que** participant ou créateur d'une partie,  
**Je veux que** les routes de draft soient inaccessibles quand la partie n'est pas en cours de draft,  
**Afin d'** éviter une erreur serveur lors d'un accès direct à `/games/:id/draft/*` hors contexte DRAFTING.

## Acceptance Criteria
- [x] AC1 : Accéder à `/games/:id/draft/snake` quand `game.status !== 'DRAFTING'` redirige vers `/games/:id`
- [x] AC2 : Accéder à `/games/:id/draft/snake` quand `game.status === 'DRAFTING'` autorise l'accès
- [x] AC3 : La même protection s'applique aux routes `/draft`, `/draft/simultaneous` et `/draft/audit`
- [x] AC4 : Un `gameId` manquant redirige vers `/games`
- [x] AC5 : Une erreur API lors de la vérification redirige vers `/games`

## Tasks/Subtasks
- [x] Task 1 : Créer le guard `draftStatusGuard`
  - [x] 1.1 Écrire les tests RED dans `draft-status.guard.spec.ts` (5 scénarios : no gameId, status non-DRAFTING via store, status DRAFTING via store, fallback API DRAFTING, erreur API)
  - [x] 1.2 Implémenter `frontend/src/app/features/draft/guards/draft-status.guard.ts` (même pattern que `simultaneousModeGuard`)
  - [x] 1.3 Confirmer que tous les tests passent (GREEN)
- [x] Task 2 : Appliquer le guard aux routes draft dans `game-routing.module.ts`
  - [x] 2.1 Ajouter `canActivate: [draftStatusGuard]` aux routes `/:id/draft`, `/:id/draft/snake`, `/:id/draft/audit`
  - [x] 2.2 Ajouter `draftStatusGuard` au tableau `canActivate` de `/:id/draft/simultaneous` (en premier, avant `simultaneousModeGuard`)
  - [x] 2.3 Lancer la suite complète de tests frontend — 0 régressions

## Dev Notes
- **Root cause** : Routes `/:id/draft/*` sans `canActivate` → accès libre → le backend répond 4xx/5xx si pas de draft en cours
- **Pattern référence** : `simultaneous-mode.guard.ts` — même squelette (store → API fallback → catchError)
- **Statut à vérifier** : `game.status === 'DRAFTING'` (type `GameStatus` dans `game.interface.ts` ligne 66)
- **Redirection** : status non-DRAFTING → `/games/:id` ; gameId absent ou erreur API → `/games`
- **Store** : `UserGamesStore.findGameById(gameId)` (returns `Game | undefined`)
- **API fallback** : `GameService.getGameById(gameId)` (returns `Observable<Game>`)
- **Fichier guard cible** : `frontend/src/app/features/draft/guards/draft-status.guard.ts`
- **Fichier routing** : `frontend/src/app/features/game/game-routing.module.ts`
- **Test framework** : Vitest + Jasmine shim (voir MEMORY — `jasmine.createSpyObj`, `TestBed.runInInjectionContext`, `firstValueFrom`)

## Dev Agent Record

### Debug Log
- 2026-04-19 : Analyse — `/:id/draft/snake` n'a que `canDeactivate`, aucun `canActivate` status check. `/:id/draft` et `/:id/draft/audit` : aucun guard du tout. Fix : nouveau guard `draftStatusGuard` appliqué à toutes les routes draft.

### Completion Notes
- Guard `draftStatusGuard` créé (même squelette que `simultaneousModeGuard`)
- 5 groupes de tests couvrant : gameId absent, store DRAFTING, store non-DRAFTING, API fallback DRAFTING, erreur API
- `game-routing.module.ts` : 4 routes mises à jour avec `canActivate: [draftStatusGuard]`
- Suite frontend : 2376/2376 tests verts, 0 régression

## File List
- frontend/src/app/features/draft/guards/draft-status.guard.ts
- frontend/src/app/features/draft/guards/draft-status.guard.spec.ts
- frontend/src/app/features/game/game-routing.module.ts

## Change Log
- 2026-04-19 : Création guard `draftStatusGuard` + application aux 4 routes draft (BUG-S19-C)

## Status
review
