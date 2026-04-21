# Story: sprint19-fix-deprecated-resolution - Nettoyage de la resolution depreciee

Status: done

<!-- METADATA
  story_key: sprint19-fix-deprecated-resolution
  sprint: Sprint 19
  priority: P3 (tech debt retro Sprint 18 - A18b)
  date_created: 2026-04-21
  source: sprint18-retrospective A18b + sprint-status.yaml Sprint 19 Phase 4
-->

## Story

En tant que developpeur maintenant le pipeline d'identite joueurs,
je veux supprimer le bridge deprecie `ResolutionPort.resolveFortniteId(...)` et durcir les appels `resolvePlayer(...)`,
afin que le pipeline n'entretienne plus deux contrats de resolution et ne puisse pas marquer un joueur comme resolu avec un Epic Account ID absent.

## Context / Root Cause

La story Sprint 16 a introduit le nouveau contrat `ResolutionPort.resolvePlayer(String pseudo, String region)` pour retourner `FortnitePlayerData` complet et eviter les doubles appels API. Pour compatibilite transitoire, `ResolutionPort` garde encore un bridge `@Deprecated default Optional<String> resolveFortniteId(...)`.

La retrospective Sprint 18 a cree l'action A18b: nettoyer cette migration et ajouter un garde `epicAccountId == null`. L'etat actuel du code montre que les appels runtime principaux utilisent deja `resolvePlayer(...)`, mais le bridge reste expose dans le port domaine et un test le couvre encore.

Points verifies le 2026-04-21:
- `ResolutionPort.java` expose encore `@Deprecated resolveFortniteId(...)`.
- `ResolutionQueueService.java` appelle deja `resolvePlayer(...)`, mais utilise `data.epicAccountId()` sans garde null/blank avant `confidenceScoreService.compute(...)` et `entry.resolve(...)`.
- `PlayerIdentityPipelineService.toSuggestion(...)` a deja un garde `data.epicAccountId() == null`.
- `PrIngestionRowProcessor` contient une methode privee `resolvePlayer(...)` sans lien avec `ResolutionPort`; ne pas l'injecter ou le modifier uniquement a cause du nom.

## Acceptance Criteria

1. `ResolutionPort` n'expose plus `resolveFortniteId(...)` ni annotation `@Deprecated` liee a la resolution; le contrat public de resolution est `resolvePlayer(...)`.
2. `rg -n "resolveFortniteId" src/main src/test` ne retourne aucun usage apres implementation.
3. `ResolutionQueueService` ignore proprement un `FortnitePlayerData` dont `epicAccountId()` est `null` ou blank: pas de `entry.resolve(...)`, pas de `identityRepository.save(...)`, pas de calcul de score, et le batch continue.
4. Le comportement non bloquant reste intact: une entree invalide, non trouvee, ou une exception d'adapter ne bloque pas les autres entrees UNRESOLVED.
5. `PlayerIdentityPipelineService.suggestEpicId(...)` reste aligne sur `resolvePlayer(...)` et conserve son garde `epicAccountId == null`.
6. `PrIngestionRowProcessor` est seulement verifie: aucune nouvelle dependance `ResolutionPort`, aucun changement fonctionnel si aucun appel deprecie n'y existe.
7. Tests backend ajoutes/mis a jour:
   - retrait du test de delegation depreciee dans `FortniteApiResolutionAdapterTest`;
   - test `ResolutionQueueServiceTest` pour `epicAccountId == null`;
   - test `ResolutionQueueServiceTest` pour `epicAccountId` blank;
   - rerun des tests de pipeline/admin impactes.
8. Aucun changement DB, frontend, endpoint public, ou configuration Spring n'est introduit par cette story.

## Tasks / Subtasks

- [x] Task 1 - Tests rouges de nettoyage du contrat (AC: #1, #2, #7)
  - [x] 1.1 Supprimer ou remplacer le test `deprecated_method_delegates_correctly` dans `FortniteApiResolutionAdapterTest`.
  - [x] 1.2 Ajouter une verification de compilation ou de recherche: plus aucun appel `resolveFortniteId` dans `src/main` et `src/test`.
  - [x] 1.3 Confirmer que les adapters `FortniteApiResolutionAdapter` et `StubResolutionAdapter` compilent avec le contrat `resolvePlayer` seul.

- [x] Task 2 - Retirer le bridge deprecie du port domaine (AC: #1, #2)
  - [x] 2.1 Modifier `src/main/java/com/fortnite/pronos/domain/port/out/ResolutionPort.java`.
  - [x] 2.2 Retirer la methode `default Optional<String> resolveFortniteId(...)`.
  - [x] 2.3 Retirer l'import devenu inutile si applicable.
  - [x] 2.4 Ne pas ajouter d'autre methode de compatibilite.

- [x] Task 3 - Durcir `ResolutionQueueService` contre les IDs absents (AC: #3, #4, #7)
  - [x] 3.1 Ajouter un helper prive clair, par exemple `hasValidEpicAccountId(FortnitePlayerData data)`.
  - [x] 3.2 Si l'ID est `null` ou blank, logguer en debug/warn court et retourner `0`.
  - [x] 3.3 Ne pas appeler `confidenceScoreService.compute(...)` avec un ID absent.
  - [x] 3.4 Ne pas appeler `entry.resolve(...)` ni `identityRepository.save(...)`.
  - [x] 3.5 Couvrir le cas `null` et le cas blank par tests unitaires.

- [x] Task 4 - Verifier les zones mentionnees par la retro sans inventer de travail (AC: #5, #6)
  - [x] 4.1 Verifier que `PlayerIdentityPipelineService.toSuggestion(...)` garde le comportement `notFound()` si `epicAccountId == null`.
  - [x] 4.2 Verifier que `PrIngestionRowProcessor` ne depend pas de `ResolutionPort` et ne contient aucun appel au bridge supprime.
  - [x] 4.3 Ne pas modifier `PrIngestionRowProcessor` sauf si la recherche revele un usage reel de `resolveFortniteId`.

- [x] Task 5 - Validation (AC: #2, #4, #7, #8)
  - [x] 5.1 Executer `rg -n "resolveFortniteId" src/main src/test` et documenter zero resultat.
  - [x] 5.2 Executer `mvn spotless:apply --no-transfer-progress`.
  - [x] 5.3 Executer `mvn -Dtest="FortniteApiResolutionAdapterTest,ResolutionQueueServiceTest,PlayerIdentityPipelineServiceTest,PrIngestionRowProcessorUnresolvedTest,PrIngestionRowProcessorAliasTest" test --no-transfer-progress`.
  - [x] 5.4 Si un compile break apparait ailleurs, corriger uniquement les appelants restants du bridge.
  - [x] 5.5 Documenter tout echec pre-existant sans le masquer.

### Review Findings

- [x] [Review][Defer] `ResolutionQueueService` n'a pas de trigger runtime de production [src/main/java/com/fortnite/pronos/service/ingestion/ResolutionQueueService.java:44] - deferred, pre-existing. La story interdit explicitement d'ajouter endpoint, configuration Spring ou changement de flux; garder en dette d'architecture dediee si le batch doit devenir operationnel.
- [x] [Review][Defer] `tryResolveEntry()` conserve un catch large autour de la mutation et de la persistence [src/main/java/com/fortnite/pronos/service/ingestion/ResolutionQueueService.java:69] - deferred, pre-existing. Le pattern existait deja avant le retrait du bridge; a traiter dans un durcissement transactionnel separe.

## Dev Notes

### Developer Context

Cette story est un nettoyage de contrat backend, pas une nouvelle feature pipeline. Le but est de finir la migration commencee en Sprint 16 et tracee en Sprint 18, avec le plus petit diff possible.

Ne pas reintroduire `FortniteApiPort` dans les services de pipeline. Depuis Sprint 16, les services doivent passer par `ResolutionPort.resolvePlayer(...)` pour conserver l'inversion de dependance et eviter les doubles appels API.

### Current Code Anchors

- `src/main/java/com/fortnite/pronos/domain/port/out/ResolutionPort.java`
  - Contient le contrat `resolvePlayer(...)`.
  - Contient encore le bridge `@Deprecated resolveFortniteId(...)` a supprimer.
- `src/main/java/com/fortnite/pronos/service/ingestion/ResolutionQueueService.java`
  - Appelle deja `resolutionPort.resolvePlayer(...)`.
  - Le risque actuel est l'absence de garde avant `data.epicAccountId()`.
- `src/main/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineService.java`
  - `suggestEpicId(...)` utilise deja `resolvePlayer(...)`.
  - `toSuggestion(...)` retourne deja `notFound()` si `data.epicAccountId() == null`.
- `src/main/java/com/fortnite/pronos/service/ingestion/PrIngestionRowProcessor.java`
  - La methode privee `resolvePlayer(...)` est un helper local d'ingestion, sans rapport avec `ResolutionPort`.
- `src/test/java/com/fortnite/pronos/adapter/out/resolution/FortniteApiResolutionAdapterTest.java`
  - Contient encore le test du bridge deprecie.
- `src/test/java/com/fortnite/pronos/service/ingestion/ResolutionQueueServiceTest.java`
  - Base de tests pour batch nominal, empty, exception, invalid pseudo/region, et score reel.

### Architecture Compliance

- Respecter l'architecture hexagonale: `ResolutionPort` reste dans `domain/port/out`, les adapters restent dans `adapter/out/resolution`, les services ne dependent pas des adapters concrets.
- Aucun nouveau bean Spring n'est necessaire.
- Aucun nouveau endpoint controller n'est necessaire.
- Aucun changement frontend/i18n n'est necessaire.
- Aucun changement Flyway n'est necessaire.
- Limite CouplingTest: `ResolutionQueueService` a 3 dependances; ne pas en ajouter.

### Testing Requirements

Tests unitaires requis:
- Nominal: une entree UNRESOLVED avec `FortnitePlayerData` complet est resolue et sauvegardee.
- Edge 1: `resolvePlayer(...)` retourne `Optional.empty()` -> entree reste UNRESOLVED.
- Edge 2: `resolvePlayer(...)` retourne un `FortnitePlayerData` avec `epicAccountId == null` -> entree reste UNRESOLVED, pas de save.
- Edge 3: `resolvePlayer(...)` retourne un `FortnitePlayerData` avec `epicAccountId` blank -> entree reste UNRESOLVED, pas de save.
- Error: `ResolutionPort` leve une exception -> batch continue.

Verification ciblee:
```powershell
rg -n "resolveFortniteId" src/main src/test
mvn spotless:apply --no-transfer-progress
mvn -Dtest="FortniteApiResolutionAdapterTest,ResolutionQueueServiceTest,PlayerIdentityPipelineServiceTest,PrIngestionRowProcessorUnresolvedTest,PrIngestionRowProcessorAliasTest" test --no-transfer-progress
```

### Pre-existing Gaps / Known Issues

- Le worktree contient de nombreux changements non lies a cette story. Le dev agent ne doit pas les revert.
- `CouplingTest` a deja signale dans des stories precedentes une dette hors-scope sur `GameDraftService`; si elle reapparait en suite large, la documenter comme pre-existante sauf preuve contraire.
- La story `sprint19-fix-resolution-adapter-config` couvre separement le cas `RESOLUTION_ADAPTER` invalide; ne pas traiter ce sujet ici.

### Latest Technical Information

Aucune recherche externe n'est requise pour cette story: le sujet est un contrat interne Java du projet, sans changement de version, dependance, API publique, ou framework. Les versions a respecter restent celles du project context: Java 21 et Spring Boot 3.4.5.

### References

- `_bmad-output/implementation-artifacts/sprint-status.yaml` - Sprint 19 Phase 4, A18b.
- `_bmad-output/implementation-artifacts/sprint18-retrospective.md` - Action item A18b et deferred E1/E2.
- `_bmad-output/implementation-artifacts/sprint16-pipeline-autoload.md` - origine du contrat `resolvePlayer(...)`.
- `_bmad-output/implementation-artifacts/sprint18-adapter-info-endpoint.md` - review deferred E1/E2.
- `_bmad-output/project-context.md` - architecture hexagonale, CouplingTest, DoD backend.
- `_bmad-output/planning-artifacts/architecture.md` - `ResolutionPort` swappable, pipeline non bloquant.
- `_bmad-output/planning-artifacts/prd.md` - NFR-R03, NFR-I02, NFR-M01.

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-04-21: Le chemin legacy de workflow demande par la skill (`_bmad/bmm/workflows/4-implementation/dev-story/workflow.md`) etait absent; workflow effectif charge depuis `_bmad/bmm/4-implementation/bmad-dev-story/workflow.md`.
- Red contract: `rg -n "resolveFortniteId" src/main src/test` retournait encore `ResolutionPort.java`.
- Red queue: `mvn -Dtest=ResolutionQueueServiceTest test --no-transfer-progress` a echoue sur les cas `epicAccountId == null` et blank (`expected: 0 but was: 1`).
- Red pipeline: `mvn -Dtest=PlayerIdentityPipelineServiceTest test --no-transfer-progress` a echoue sur `epicAccountId` blank (`found=true`).
- Green cible: `mvn -Dtest="ResolutionQueueServiceTest,PlayerIdentityPipelineServiceTest" test --no-transfer-progress` -> 37 tests, 0 failure.
- Validation ciblee BMAD: `mvn -Dtest="FortniteApiResolutionAdapterTest,ResolutionQueueServiceTest,PlayerIdentityPipelineServiceTest,PrIngestionRowProcessorUnresolvedTest,PrIngestionRowProcessorAliasTest" test --no-transfer-progress` -> 50 tests, 0 failure.
- Recherche finale: `rg -n "resolveFortniteId" src/main src/test` -> aucun resultat.
- Regressions larges: `mvn test --no-transfer-progress` echoue uniquement sur dettes pre-existantes/hors-scope (`CouplingTest` sur `GameDraftService` a 8 dependances, `GameStatisticsServiceTddTest.shouldMapNullPlayerIdsToUnknown`, `GameDataIntegrationTest` fixtures DB absentes). Ces fichiers ne sont pas modifies par cette story.

### Completion Notes List

- `ResolutionPort` n'expose plus le bridge `resolveFortniteId(...)`; le contrat public reste `resolvePlayer(...)`.
- `ResolutionQueueService` refuse les resolutions dont `FortnitePlayerData.epicAccountId()` est `null` ou blank avant calcul de score, `entry.resolve(...)` et sauvegarde.
- `PlayerIdentityPipelineService.suggestEpicId(...)` conserve le comportement `notFound()` pour `null` et traite aussi les IDs blank comme absents.
- `PrIngestionRowProcessor` a ete verifie et n'a pas ete modifie: aucune dependance `ResolutionPort`, aucun appel au bridge supprime.
- Aucun changement DB, frontend, endpoint public ou configuration Spring.

### File List

- `src/main/java/com/fortnite/pronos/domain/port/out/ResolutionPort.java`
- `src/main/java/com/fortnite/pronos/service/ingestion/ResolutionQueueService.java`
- `src/main/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineService.java`
- `src/test/java/com/fortnite/pronos/adapter/out/resolution/FortniteApiResolutionAdapterTest.java`
- `src/test/java/com/fortnite/pronos/service/ingestion/ResolutionQueueServiceTest.java`
- `src/test/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineServiceTest.java`
- `_bmad-output/implementation-artifacts/sprint19-fix-deprecated-resolution.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `tasks/todo.md`

### Change Log

- 2026-04-21: Story creee par workflow `bmad-create-story`; statut `ready-for-dev`.
- 2026-04-21: Implementation dev-story terminee; bridge deprecie supprime, guards `epicAccountId` null/blank ajoutes, tests cibles verts; statut `review`.
- 2026-04-21: Code review BMAD terminee; 2 dettes pre-existantes deferrees, aucun patch bloquant; statut `done`.
