# Story 8.2 Runtime Validation

Story: `8-2-realignement-contrat-draft-serpent-front-back`
Date: `2026-03-08`
Status: `closed`

## Observation

- La suite dediee `frontend/e2e/draft-flow.spec.ts` etait encore inutilisable au debut de la story: `3 fixme`.
- Le front snake draft dependait d un pseudo endpoint legacy `/api/drafts/{gameId}/board-state` et publiait les picks par un canal websocket qui ne persistait pas correctement le flow local.
- Le runtime a aussi revele un ecart de contrat non documente: `GET /api/games/{id}/draft/snake/turn` renvoie un `userId`, alors que l ecran draft avait besoin d un `participantId` pour retrouver le picker courant dans le board.
- Cote backend, un pick snake pouvait etre enregistre sans mettre a jour le roster du participant (`selectedPlayers`), ce qui cassait le reflet du pick apres reload.

## Validation

- Backend:
  - `mvn -q -Dtest=GameDraftServiceTddTest test` -> `OK`
  - Le test rouge puis vert confirme qu un pick snake ajoute le joueur au roster participant et sauvegarde l entite.
- Frontend unitaire:
  - `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/features/draft/services/draft.service.snake.spec.ts --include src/app/features/draft/components/snake-draft-page/snake-draft-page.component.spec.ts` -> `25 SUCCESS`
  - `npx ng test --watch=false --browsers=ChromeHeadless --include src/app/features/game/game-detail/game-detail.component.spec.ts --include src/app/features/game/game-home/game-home.component.spec.ts` -> `64 SUCCESS`
- E2E runtime:
  - `npx playwright test e2e/draft-flow.spec.ts` -> `3 passed`
  - `DRAFT-01`: le createur peut demarrer le snake draft depuis le detail partie
  - `DRAFT-02`: le picker courant peut selectionner un joueur sur le board
  - `DRAFT-03`: le pick confirme est reflete apres rechargement de la page
- Environnement local valide pendant la preuve:
  - `fortnite-app-local` : `healthy`
  - `fortnite-postgres-local` : `healthy`
  - `GET /actuator/health` : `UP`

## BMAD Handling

- La story `8.2` peut passer en `review`: les 5 acceptance criteria sont couverts par code, tests cibles et validation runtime.
- Les reliquats connus sont maintenant explicites et hors scope du lot:
  - synchro WebSocket fine du snake draft
  - couverture E2E `trade/swap`
- Railway/staging reste hors priorite et n est pas requis pour cette validation locale "prod-like".
