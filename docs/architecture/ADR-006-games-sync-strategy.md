# ADR-006 - Strategie de synchronisation Games multi-clients (sans polling global)

- Status: Accepted
- Date: 2026-02-17
- Decision owners: @Codex
- Related tickets: JIRA-FIX-021, JIRA-FIX-020, JIRA-FIX-016

## Contexte

Le polling global (15s) provoque un effet de refresh visuel et degrade l'UX.
Le besoin est de propager create/join/leave/delete game entre clients sans F5.

## Options evaluees

| Option | Latence percue | Cout infra | Complexite | Fiabilite cross-client | Decision |
|---|---|---|---|---|---|
| Polling global (15s) | Moyenne a mauvaise | Faible | Faible | Moyenne | Rejete |
| Listeners-only (focus/visibility/actions locales) | Bonne locale, moyenne cross-client | Faible | Faible | Moyenne | Partiel |
| SSE (events serveur -> clients) | Bonne | Moyenne | Moyenne | Bonne | Retenu |
| WebSocket full duplex | Excellente | Plus eleve | Elevee | Excellente | Non retenu maintenant |

## Decision

Adopter une strategie **hybride en 2 phases**:

1. Phase immediate (JIRA-FIX-020):
- supprimer le polling global;
- garder des listeners frontend (`focus`, `visibilitychange`) et invalidations ciblees apres actions locales;
- ajouter un canal SSE pour events critiques (`GAME_DELETED`, `GAME_JOINED`, `GAME_LEFT`, `GAME_UPDATED`).

2. Phase ulterieure (si charge/latence insuffisante):
- evoluer vers WebSocket uniquement si SSE montre des limites fortes.

## Rational

- SSE couvre le besoin dominant (push serveur -> client) avec une complexite inferieure a WebSocket.
- Le fallback listeners garde un comportement robuste en cas de perte temporaire du flux SSE.
- Suppression du polling global = suppression des animations parasites vues par l'utilisateur.

## Plan de migration

1. Introduire endpoint SSE backend securise pour events games par utilisateur.
2. Ajouter `GamesRealtimeService` frontend (abonnement SSE + retry/backoff simple).
3. Brancher `UserGamesStore` sur events SSE + invalidation ciblee.
4. Conserver `focus/visibilitychange` comme rattrapage non-invasif.
5. Retirer definitivment le polling layout.
6. Ajouter tests store/layout + test integration API event.

## Criteres de succes

- Aucun polling global actif en continu.
- Update cross-client visible sans F5 pour delete/join/leave.
- Aucun clignotement periodique du layout.
- Regression tests verts sur store/layout/detail.
