# Story 5.2 Runtime Validation

Story: `5-2-trade-mutuel-1v1-avec-acceptation-adverse`
Date: `2026-03-07`
Status: `closed`

## Observation

- Le flow local `POST /api/games/{gameId}/draft/trade` retournait `500` au lieu de `201`.
- Le stacktrace backend pointait vers `ObjectOptimisticLockingFailureException` sur `DraftParticipantTradeEntity`.

## Root Cause

- Le domaine `DraftParticipantTrade` pre-assignait l'UUID du trade.
- L'entite JPA `DraftParticipantTradeEntity` etait annotee `@GeneratedValue(strategy = GenerationType.UUID)`.
- Sous Hibernate 6.6, ce melange faisait traiter le trade comme une entite detachee inconnue, ce qui cassait le `merge()` sur la creation.

## Resolution

- `src/main/java/com/fortnite/pronos/model/DraftParticipantTradeEntity.java`
  - suppression de `@GeneratedValue`
  - alignement sur un UUID assigne par le domaine avant persistance
- `src/test/java/com/fortnite/pronos/repository/DraftParticipantTradeJpaRepositoryTest.java`
  - ajout d'un test rouge/vert pour la creation avec UUID pre-assigne
  - ajout d'un test rouge/vert pour la mise a jour du meme trade
- `src/test/java/com/fortnite/pronos/controller/DraftParticipantTradeControllerTest.java`
  - ajout du `@DisplayName` manquant pour fermer le low finding BMAD `L1`

## Validation

- `mvn -q -Dtest="DraftParticipantTradeJpaRepositoryTest,DraftParticipantTradeServiceTest,DraftParticipantTradeControllerTest,SecurityConfigDraftParticipantTradeAuthorizationTest" test` -> OK
- `POST /api/games/b551d1dd-5061-4f52-a71e-35b318219aca/draft/trade?user=thibaut` -> `201 CREATED`
- `POST /api/games/b551d1dd-5061-4f52-a71e-35b318219aca/draft/trade/87eef780-230e-4bc0-99be-b0004dd7fab9/accept?user=marcel` -> `200 OK`
- DB local:
  - trade `87eef780-230e-4bc0-99be-b0004dd7fab9` en `ACCEPTED`
  - `draft_picks` swappes: `thibaut -> Bugha_EU`, `marcel -> EULow1`

## BMAD Handling

- Le bug est traite comme follow-up runtime d'une story marquee `done`, car le contrat API etait livre mais non revalide en execution locale.
- La story 5.2 reste `done` apres correction et revalidation.
- Aucun travail d'hebergement n'est rouvert: la priorite locale reste trades, scraping et flux API.
