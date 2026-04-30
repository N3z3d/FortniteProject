# Story: sprint19-contract-create-game-region-rules - Contrat explicite createGame pour regionRules

Status: done

<!-- METADATA
  story_key: sprint19-contract-create-game-region-rules
  sprint: Sprint 19
  priority: P3 (correct-course structural follow-up, priorite d'execution recommandee dans le sprint)
  date_created: 2026-04-24
  source: correct-course sprint19 invitation-code 2026-04-24 + Epic 3 Story 3.5
-->

## Story

En tant que createur de partie,
je veux que l'API de creation de partie applique un contrat explicite pour `regionRules`,
afin que le backend n'invente pas silencieusement une configuration de gameplay differente de celle demandee par le client.

## Context / Root Cause

Le correct-course du 2026-04-24 a isole un probleme de contrat a la frontiere `POST /api/games`:

- `CreateGameUseCase.addRegionRules(...)` ajoute aujourd'hui toutes les `PlayerRegion.ACTIVE_REGIONS` avec `7` slots quand `request.getRegionRules()` est `null` ou vide.
- `GameCreationService.addRegionRules(...)` reproduit exactement le meme fallback, ce qui laisse coexister deux chemins backend portant la meme ambiguite.
- `ValidationService.validateRegionRules(...)` considere encore `null` et `isEmpty()` comme des cas valides, donc la requete n'est jamais rejetee explicitement.
- `CreateGameRequest.isValid()` accepte actuellement une creation minimale sans `regionRules`.
- La route runtime `/games/create` pointe sur `CreateGameComponent`, et son `onSubmit()` envoie aujourd'hui `regionRules: {}` tout en affichant une configuration "pre-configured".
- `GameCreationWizardComponent`, bien que non route, appelle lui aussi `gameService.createGame(...)` avec `regionRules: {}`.
- Des tests encodent deja ce comportement implicite, par exemple `CreateGameUseCaseTest.shouldPopulateDefaultRegionRulesWhenRequestOmitsThem()` et `CreateGameIntegrationTest.shouldCreateGameSuccessfully()` avec `Collections.emptyMap()`.

Resultat: le serveur transforme silencieusement "absent" ou "{}" en "toutes les regions actives", ce qui masque la vraie decision produit et casse la lisibilite du contrat API.

Decision de story:

- le backend ne doit plus synthetiser `ACTIVE_REGIONS` par defaut quand `regionRules` manque ou est vide;
- l'appel create-game doit echouer explicitement si `regionRules` n'est pas materialise dans le payload;
- si l'UI veut proposer un template par defaut, elle doit l'envoyer explicitement;
- ce sujet reste strictement borne au contrat `createGame()` / `regionRules` et ne doit pas absorber la migration "createur toujours participant canonique".

## Acceptance Criteria

1. Quand `POST /api/games` recoit un payload avec `regionRules` explicite et non vide, la game creee persiste exactement ces regles et uniquement celles-ci; aucune region supplementaire issue de `ACTIVE_REGIONS` n'est ajoutee cote serveur.
2. Quand `POST /api/games` recoit un payload sans `regionRules`, le backend rejette la requete avec un `400` explicite mentionnant `regionRules`, et aucune game n'est persistee.
3. Quand `POST /api/games` recoit un payload avec `regionRules: {}` vide, le backend rejette la requete avec le meme contrat explicite qu'en cas d'absence, et aucune synthese silencieuse de `ACTIVE_REGIONS` n'a lieu.
4. Le comportement est coherent sur les deux chemins backend existants: `CreateGameUseCase` (route active `GameController.createGame`) et `GameCreationService.createGame()` ne divergent plus sur `regionRules`.
5. La surface frontend routee `/games/create` envoie un `CreateGameRequest` avec `regionRules` explicite et un `maxParticipants` coherent avec la configuration effectivement affichee/selectionnee par l'utilisateur; elle ne poste plus `{}`.
6. Aucune autre surface frontend encore compilee qui appelle `gameService.createGame(...)` ne peut compter sur un fallback serveur implicite. Si `GameCreationWizardComponent` reste dans le depot, son payload doit lui aussi materialiser des `regionRules` explicites et coherents avec son template.
7. La story ne change ni la politique "pas de code d'invitation a la creation", ni le flux post-create existant, ni l'invariant createur-participant traite dans une story dediee.
8. Les tests prouvent a minima:
   - persistance exacte des `regionRules` explicites;
   - rejet `400` sur `regionRules` absent;
   - rejet `400` sur `regionRules` vide;
   - payload frontend explicite sur la route active `/games/create`;
   - disparition ou reecriture des tests qui validaient auparavant la population implicite de `ACTIVE_REGIONS`.
9. Si une preuve runtime E2E est ajustee, elle s'appuie sur les suites create-game existantes (`game-lifecycle` / `full-game-flow`) ou sur leurs helpers, au lieu de creer une spec parallele redondante.

## Tasks / Subtasks

- [x] Task 1 - Ecrire les tests rouges backend du contrat explicite `regionRules` (AC: #1, #2, #3, #4, #8)
  - [x] 1.1 Modifier `src/test/java/com/fortnite/pronos/core/usecase/CreateGameUseCaseTest.java` pour remplacer l'ancienne attente "populate default region rules" par:
    - succes avec map explicite persistee telle quelle;
    - echec explicite quand `regionRules` est absent;
    - echec explicite quand `regionRules` est vide.
  - [x] 1.2 Modifier `src/test/java/com/fortnite/pronos/service/game/GameCreationServiceDomainMigrationTest.java` pour imposer le meme contrat sur le chemin legacy `GameCreationService.createGame(...)`.
  - [x] 1.3 Modifier `src/test/java/com/fortnite/pronos/controller/GameControllerSimpleTest.java` pour prouver le `400` sur payload incomplet/vide a la frontiere HTTP.
  - [x] 1.4 Modifier `src/test/java/com/fortnite/pronos/integration/CreateGameIntegrationTest.java` pour remplacer le succes avec `Collections.emptyMap()` par un vrai payload explicite et ajouter un cas d'echec `400` pour le contrat incomplet.
  - [x] 1.5 Si la validation est porteuse dans le DTO, etendre `src/test/java/com/fortnite/pronos/dto/CreateGameRequestValidationTest.java` et/ou `src/test/java/com/fortnite/pronos/dto/CreateGameRequestTest.java` pour rendre le contrat visible au plus tot.

- [x] Task 2 - Supprimer le fallback implicite cote backend sans dupliquer la logique (AC: #1, #2, #3, #4, #7)
  - [x] 2.1 Introduire un point unique de validation/materialisation du contrat `regionRules` reutilisable par `CreateGameUseCase` et `GameCreationService`.
  - [x] 2.2 Retirer la synthese silencieuse `PlayerRegion.ACTIVE_REGIONS -> 7` de `src/main/java/com/fortnite/pronos/core/usecase/CreateGameUseCase.java`.
  - [x] 2.3 Retirer la meme synthese de `src/main/java/com/fortnite/pronos/service/game/GameCreationService.java`.
  - [x] 2.4 Faire echouer `regionRules == null` et `regionRules.isEmpty()` avec une erreur metier explicite (`InvalidGameRequestException` ou equivalent projet) plutot qu'un `IllegalArgumentException` opaque ou un fallback silencieux.
  - [x] 2.5 Aligner `src/main/java/com/fortnite/pronos/service/ValidationService.java` et, si pertinent, `src/main/java/com/fortnite/pronos/dto/CreateGameRequest.java` pour que "missing/empty" ne soit plus traite comme valide dans le contexte create-game.
  - [x] 2.6 Ne pas modifier la politique invitation-code ni la logique createur-participant dans cette story.

- [x] Task 3 - Realigner la route frontend active `/games/create` sur un payload explicite (AC: #5, #7, #8)
  - [x] 3.1 Modifier `frontend/src/app/features/game/create-game/create-game.component.ts`.
  - [x] 3.2 Faire en sorte que le payload `CreateGameRequest` derive explicitement de la configuration visible ou selectionnee, au lieu d'envoyer `regionRules: {}` en dur.
  - [x] 3.3 Garantir la coherence `maxParticipants <-> somme(regionRules)` dans le payload envoye.
  - [x] 3.4 Si la surface quick-create affiche une configuration "pre-configured" ou un template implicite, materialiser cette configuration dans une constante ou un helper explicite cote client; ne pas la laisser cachee dans le backend.
  - [x] 3.5 Mettre a jour `frontend/src/app/features/game/create-game/create-game.component.spec.ts` pour verifier le payload exact et supprimer les attentes qui validaient `{}`.
  - [x] 3.6 Si la copie UI ou l'etat du formulaire est actuellement contradictoire avec le template reel, corriger le minimum necessaire pour que le texte visible, l'etat forme et le payload disent la meme chose.

- [x] Task 4 - Nettoyer les autres callers frontend create-game pour eviter un futur retour du bug (AC: #6, #8)
  - [x] 4.1 Auditer tous les appels `gameService.createGame(...)` / `command.createGame(...)` dans `frontend/src/app/features/game`.
  - [x] 4.2 Si `GameCreationWizardComponent` reste compile et testable, modifier `frontend/src/app/features/game/create-game/game-creation-wizard.component.ts` pour soumettre des `regionRules` explicites et coherents avec le template choisi.
  - [x] 4.3 Mettre a jour `frontend/src/app/features/game/create-game/game-creation-wizard.component.spec.ts` pour verrouiller ce contrat.
  - [x] 4.4 Si une autre surface create-game legacy est jugee hors runtime mais toujours compilee, soit l'aligner, soit la documenter et la neutraliser explicitement dans le scope de cette story; ne laisser aucun sender latent poster `{}`.

- [x] Task 5 - Prouver la non-regression create-game au niveau runtime et suites ciblees (AC: #8, #9)
  - [x] 5.1 Relire `frontend/e2e/helpers/app-helpers.ts#createQuickGame` et les suites `frontend/e2e/game-lifecycle.spec.ts` / `frontend/e2e/full-game-flow.spec.ts` pour reutiliser la preuve create-game existante.
  - [x] 5.2 Si une preuve E2E est modifiee, verifier qu'une creation via `/games/create` reste verte et, idealement, que la game creee expose des `regionRules` explicites via l'API/detail plutot qu'un fallback implicite.
  - [x] 5.3 Executer `mvn spotless:apply --no-transfer-progress`.
  - [x] 5.4 Executer `mvn -Dtest="CreateGameUseCaseTest,GameCreationServiceDomainMigrationTest,GameControllerSimpleTest,CreateGameIntegrationTest,CreateGameRequestValidationTest,CreateGameRequestTest,ValidationServiceTddTest" test --no-transfer-progress`.
  - [x] 5.5 Depuis `frontend/`, executer `npm run test:vitest -- src/app/features/game/create-game/create-game.component.spec.ts src/app/features/game/create-game/game-creation-wizard.component.spec.ts src/app/features/game/create-game/create-game-region-rules.util.spec.ts src/app/features/game/services/game-command.service.spec.ts src/app/features/game/services/game.service.spec.ts`.
  - [x] 5.6 Sur les specs E2E/fixtures impactees, executer `npx playwright test e2e/draft-full-flow.spec.ts e2e/trade-swap-flow.spec.ts` et documenter le blocage runtime restant quand `http://localhost:4200` n'est pas servi.
  - [x] 5.7 Documenter toute dette hors scope rencontree sans la corriger opportunistement.

### Review Findings

- [x] [Review][Patch] Mode avance `/games/create` casse avant POST car `mat-option value` fournit une string - Corrige: options numeriques et coercition `Number(...)` avant construction du payload `regionRules`.
- [x] [Review][Patch] `CreateGameRequest.isValid()` peut produire un 500 sur une valeur `regionRules` null - Corrige: le total ignore les valeurs null apres validation, avec test DTO dedie.
- [x] [Review][Patch] Changements invitation/archive hors scope de cette story - Justifie hors de cette story: `deleteInvitationCode(...)` est couvert par `sprint19-feat-invitation-code-advanced` et `archiveGame(...)` par `sprint14-fix-archive-endpoint`. La story `regionRules` ne les revendique pas comme changement de scope.
- [x] [Review][Patch] Des tests legacy du chemin `GameCreationService.createGame()` restent rouges sous le nouveau contrat - Corrige: helper de requete valide partage et tests integration legacy alignes sur `regionRules` explicite.
- [x] [Review][Patch] Le test controller happy-path conserve une requete create-game invalide - Corrige: le happy-path porte maintenant un payload `regionRules` explicite.
- [x] [Review][Patch] Preuve E2E elargie et peu fiable pour le scope `regionRules` - Justifie hors de cette story: les specs draft/tranche sont rattachees aux stories Sprint 14 dediees (`sprint14-e2e-ui-draft-complet`, `sprint14-refonte-tranche-system` / `sprint14-fix-tranche-ui`). Les fragilites relevees ont ete corrigees au minimum sans les presenter comme preuve principale de `regionRules`.
- [x] [Review][Patch] Nouvelles strings visibles non i18n dans la creation rapide - Corrige: template branche sur `games.create.tranchesEnabled` / `games.create.tranchesHint` dans les 4 locales.

## Dev Notes

### Developer Context

Cette story est le premier des deux follow-ups structurels crees par le correct-course du 2026-04-24 apres la review de `sprint19-feat-invitation-code-advanced`. Le but n'est pas de "retoucher invitation-code", mais de rendre explicite le contrat API `createGame()` qui etait implicitement derive du backend.

Le risque principal est de faire un demi-fix:

- corriger `CreateGameUseCase` mais laisser `GameCreationService` avec le vieux fallback;
- corriger le backend mais laisser le front route poster `{}`;
- corriger la route active mais laisser un autre caller frontend dormant reintroduire le probleme au prochain refactor;
- absorber en douce le sujet createur-participant, alors qu'il a sa propre story.

### Current Code Anchors

- `src/main/java/com/fortnite/pronos/controller/GameController.java`
  - La route active `POST /api/games` utilise `CreateGameUseCase.execute(...)`.
- `src/main/java/com/fortnite/pronos/core/usecase/CreateGameUseCase.java`
  - `addRegionRules(...)` synthetise aujourd'hui `PlayerRegion.ACTIVE_REGIONS` quand `regionRules` manque ou est vide.
- `src/main/java/com/fortnite/pronos/service/game/GameCreationService.java`
  - `createGame(...)` et `addRegionRules(...)` portent le meme fallback implicite.
- `src/main/java/com/fortnite/pronos/service/ValidationService.java`
  - `validateRegionRules(...)` traite encore `null` et `isEmpty()` comme succes.
- `src/main/java/com/fortnite/pronos/dto/CreateGameRequest.java`
  - `isValid()` autorise aujourd'hui un create-game minimal sans `regionRules`.
- `src/test/java/com/fortnite/pronos/core/usecase/CreateGameUseCaseTest.java`
  - Contient l'attente legacy `shouldPopulateDefaultRegionRulesWhenRequestOmitsThem()`.
- `src/test/java/com/fortnite/pronos/service/game/GameCreationServiceDomainMigrationTest.java`
  - Valide encore que `regionRules == null` skippe la validation.
- `src/test/java/com/fortnite/pronos/integration/CreateGameIntegrationTest.java`
  - Cree aujourd'hui une game avec `Collections.emptyMap()` et attend `201`.
- `frontend/src/app/features/game/create-game/create-game.component.ts`
  - Route active `/games/create`.
  - Envoie aujourd'hui `regionRules: {}` en dur.
  - Possede deja `DEFAULT_REGION_CONFIG`, mais le payload ne le materialise pas.
- `frontend/src/app/features/game/create-game/game-creation-wizard.component.ts`
  - Non route, mais appelle lui aussi `gameService.createGame(...)` avec `regionRules: {}`.
- `frontend/src/app/features/game/services/game-command.service.ts`
  - Point de sortie HTTP unique `POST /api/games`.
- `frontend/e2e/helpers/app-helpers.ts`
  - `createQuickGame(...)` cree une vraie partie via l'UI active.

### Architecture Compliance

- Respecter la clarification architecture du 2026-04-24:
  - `POST /api/games` ne synthetise pas silencieusement de regions par defaut cote serveur;
  - si le produit veut un template par defaut, le client l'envoie explicitement.
- Ne pas ouvrir la story "createur toujours participant canonique"; elle vit dans `sprint19-migrate-canonical-game-participants`.
- Eviter une divergence durable entre `CreateGameUseCase` et `GameCreationService`; la logique de contrat `regionRules` doit etre partagee ou centralisee.
- Aucun changement DB/Flyway, endpoint public supplementaire, ou refactor large hors scope.
- Garder le flux "pas de code a la creation" intact.

### Implementation Guidance

Approche recommandee:

1. Rendre le contrat explicite au plus tot:
   - soit via la validation du DTO;
   - soit via un helper/metier partage;
   - soit via les deux si necessaire.
   Dans tous les cas, `null` et `{}` doivent converger vers une erreur claire.

2. Supprimer le fallback serveur des deux chemins create-game:
   - `CreateGameUseCase`;
   - `GameCreationService`.

3. Realigner le front sur une source explicite:
   - la route active `/games/create` doit soumettre une vraie map;
   - si la surface affiche un template par defaut, ce template doit exister en tant que valeur cliente explicite, pas seulement comme inference backend.

4. Nettoyer les tests legacy:
   - supprimer ou reecrire les tests qui "prouvaient" l'ajout implicite de `ACTIVE_REGIONS`;
   - ajouter des assertions de payload / erreur explicite.

Ne pas resoudre ce probleme en deplacant simplement le fallback dans un autre helper backend. La source de verite voulue est: "le client dit explicitement ce qu'il veut, le serveur applique ou rejette explicitement".

### UX Requirements

- La route active `/games/create` ne doit pas afficher une configuration predefinie puis envoyer un payload different.
- Si un template par defaut reste present, le texte visible, l'etat formulaire et le payload doivent etre coherents.
- Aucun nouveau parcours n'est demande: conserver la navigation vers `/games/{id}` apres succes.
- En cas de rejet du contrat cote backend, le message remonte doit etre comprehensible et actionnable; eviter un message generique qui n'indique pas `regionRules`.

### Testing Requirements

Tests backend requis:

- nominal: payload avec `regionRules` explicites -> map persistee a l'identique;
- edge 1: `regionRules` absent -> `400` / erreur explicite;
- edge 2: `regionRules` vide -> `400` / erreur explicite;
- edge 3: les deux chemins backend (`CreateGameUseCase`, `GameCreationService`) restent alignes.

Tests frontend requis:

- route active `CreateGameComponent` -> payload explicite, plus `{}`;
- coherence `maxParticipants <-> regionRules`;
- tout caller frontend create-game restant compile ne peut plus compter sur un fallback serveur implicite.

Preuve runtime recommandee:

- reutiliser `game-lifecycle.spec.ts` et/ou `full-game-flow.spec.ts` si une preuve E2E est necessaire;
- preferer enrichir `createQuickGame(...)` ou verifier la game creee via l'API/detail plutot que creer une nouvelle spec parallele.

### Previous Story Intelligence

- `sprint19-feat-invitation-code-advanced` a explicitement reroute ce sujet hors scope lors du correct-course du 2026-04-24.
- `sprint19-fix-join-redirect` ne doit pas etre regresse: la creation reussie doit toujours aboutir a la page detail de la game.
- `sprint19-fix-invitation-code-security` et la politique "pas de code a la creation" restent hors sujet ici et ne doivent pas etre modifies.
- `sprint14-fix-participants-count` porte un follow-up distinct sur l'invariant createur-participant; ne pas melanger les deux chantiers.
- `docs/testing/E2E_LOCAL_RUNBOOK.md` indique deja que `full-game-flow.spec.ts` couvre `create -> generate code -> join -> detail visibility`; reutiliser ce pack si une preuve runtime est ajoutee.

### Git Intelligence

Derniers commits observes:

- `7b3537a fix: durcir suggest-epic-id apres review`
- `9568718 fix(pipeline): validate resolution adapter config`
- `5819c24 fix(pipeline): remove deprecated resolution bridge`
- `c1d9320 fix(draft): guard all draft routes against non-DRAFTING game status (BUG-S19-C)`
- `cce8138 fix(store): prevent race condition when force-refreshing games on navigation`

Signal utile: les lots recents corrigent un contrat precis, gardent le scope borne et documentent les dettes hors scope plutot que d'empiler des comportements opportunistes.

### Pre-existing Gaps / Known Issues

- Le worktree est deja tres sale; ne rien revert hors scope.
- La route active `CreateGameComponent` est actuellement contradictoire:
  - elle affiche une configuration "pre-configured";
  - elle possede `DEFAULT_REGION_CONFIG`;
  - mais son submit poste `regionRules: {}` et `maxParticipants: 5` en dur.
- `GameCreationWizardComponent` est non route mais toujours compile et testable; le laisser poster `{}` laisserait un contrat latent faux dans le depot.
- `CreateGameIntegrationTest` et certains tests frontend encodent encore le fallback implicite; ils devront etre reecrits, pas seulement ajuster des snapshots.
- La story soeur `sprint19-migrate-canonical-game-participants` reste hors scope; ne pas profiter de cette story pour modifier `ensureCreatorParticipantPersisted(...)` ou `getTotalParticipantCount()`.

### Latest Technical Information

Aucune recherche web n'est requise. Le sujet repose sur le contrat interne de l'application et les frameworks deja presents dans le depot: Java 21, Spring Boot 3.4.5, Angular 20, Vitest et Playwright. Aucun changement de version ni dependance externe n'est attendu.

### References

- `_bmad-output/planning-artifacts/epics.md` - Story 3.5 "Contrat explicite de creation de partie pour regionRules".
- `_bmad-output/planning-artifacts/architecture.md` - clarifications 2026-04-24 sur `regionRules` et l'absence de fallback serveur.
- `_bmad-output/planning-artifacts/sprint-change-proposal-2026-04-24.md` - recommandation de correct-course et handoff de sequencing.
- `_bmad-output/implementation-artifacts/sprint19-feat-invitation-code-advanced.md` - story source ou le sujet est explicitement exclu du scope.
- `_bmad-output/implementation-artifacts/sprint14-fix-participants-count.md` - follow-up distinct sur l'invariant createur-participant.
- `_bmad-output/implementation-artifacts/sprint-status.yaml` - Sprint 19, nouvelles entrees backlog.
- `_bmad-output/project-context.md` - stack, architecture, DoD et tests.
- `docs/testing/E2E_LOCAL_RUNBOOK.md` - suites E2E create-game existantes.
- `src/main/java/com/fortnite/pronos/controller/GameController.java`
- `src/main/java/com/fortnite/pronos/core/usecase/CreateGameUseCase.java`
- `src/main/java/com/fortnite/pronos/service/game/GameCreationService.java`
- `src/main/java/com/fortnite/pronos/service/ValidationService.java`
- `src/main/java/com/fortnite/pronos/dto/CreateGameRequest.java`
- `src/test/java/com/fortnite/pronos/core/usecase/CreateGameUseCaseTest.java`
- `src/test/java/com/fortnite/pronos/service/game/GameCreationServiceDomainMigrationTest.java`
- `src/test/java/com/fortnite/pronos/integration/CreateGameIntegrationTest.java`
- `frontend/src/app/features/game/create-game/create-game.component.ts`
- `frontend/src/app/features/game/create-game/game-creation-wizard.component.ts`
- `frontend/src/app/features/game/services/game-command.service.ts`
- `frontend/e2e/helpers/app-helpers.ts`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-04-24: Workflow legacy demande par la skill introuvable sous `_bmad/bmm/workflows/4-implementation/create-story/workflow.md`; workflow effectif charge depuis `_bmad/bmm/4-implementation/bmad-create-story/workflow.md`.
- 2026-04-24: Config BMAD, discover-inputs, checklist, sprint-status complet, lecons projet et tasks/todo relus avant creation.
- 2026-04-24: Cible deduite de `sprint19-` comme prochaine story backlog Sprint 19, soit `sprint19-contract-create-game-region-rules`, selon l'ordre du `sprint-status.yaml` et le handoff du correct-course.
- 2026-04-24: Artefacts planning relus: `epics.md`, `architecture.md`, `prd.md`, `sprint-change-proposal-2026-04-24.md`, `project-context.md`.
- 2026-04-24: Code cible relu: `GameController`, `CreateGameUseCase`, `GameCreationService`, `ValidationService`, `CreateGameRequest`, `CreateGameComponent`, `GameCreationWizardComponent`, `GameCommandService`, `app-helpers.ts`.
- 2026-04-24: Tests cibles relus: `CreateGameUseCaseTest`, `GameCreationServiceDomainMigrationTest`, `GameControllerSimpleTest`, `CreateGameIntegrationTest`, `GameControllerAuthenticationTest`, `CreateGameRequestValidationTest`, `CreateGameRequestTest`, specs frontend create-game.
- 2026-04-25: Workflow legacy `dev-story` introuvable sous `_bmad/bmm/workflows/4-implementation/dev-story/workflow.md`; workflow effectif charge depuis `_bmad/bmm/4-implementation/bmad-dev-story/workflow.md`.
- 2026-04-25: Contrat backend centralise dans `CreateGameRegionRulesContract.validateAndApply(...)`; fallback implicite retire de `CreateGameUseCase` et `GameCreationService`.
- 2026-04-25: `ValidationService.validateRegionRules(...)` et `CreateGameRequest` durcis pour rejeter `regionRules` absent ou vide; message metier explicite: `regionRules est requis et ne peut pas etre vide`.
- 2026-04-25: Route `/games/create`, wizard et modele frontend realignes sur des `regionRules` explicites via `buildBalancedRegionRules(...)` et `buildSingleRegionRules(...)`.
- 2026-04-25: Fixtures E2E/flows mono-region realignes sur `EU`; `trade-swap-helpers.ts` utilise maintenant une fixture draft mono-region coherente avec les picks attendus.
- 2026-04-25: Validation executee: `mvn spotless:apply` succes, backend cible `147 tests` verts, frontend cible `70 passed`, Playwright cible bloque ensuite par `http://localhost:4200` indisponible.

### Implementation Plan

- Backend: faire converger `CreateGameUseCase` et `GameCreationService` vers un contrat `regionRules` explicite, sans fallback implicite sur `ACTIVE_REGIONS`.
- Frontend: faire poster a chaque surface create-game un payload explicite et coherent avec la configuration visible, puis supprimer les attentes de tests qui validaient `{}`.
- Validation: couvrir use-case/service/controller/integration cote backend, specs create-game cote frontend, puis reutiliser les suites E2E create-game existantes si une preuve runtime est modifiee.

### Completion Notes List

- Contrat backend `regionRules` centralise via `CreateGameRegionRulesContract.validateAndApply(...)`; `null` et `{}` convergent vers `InvalidGameRequestException("regionRules est requis et ne peut pas etre vide")`.
- `CreateGameUseCase` et `GameCreationService` n'ajoutent plus silencieusement `ACTIVE_REGIONS -> 7`; les `regionRules` explicites sont persistes exactement telles quelles.
- `ValidationService.validateRegionRules(...)` et `CreateGameRequest` rejettent maintenant `regionRules` absent ou vide au plus tot dans le flux create-game.
- `CreateGameComponent` et `GameCreationWizardComponent` envoient des `regionRules` explicites coherentes avec `maxParticipants`; la copie visible de la route active a ete realignee avec le payload.
- Helpers frontend introduits: `buildBalancedRegionRules(...)` pour materialiser le template explicite de la route create-game, `buildSingleRegionRules(...)` pour les fixtures/tests mono-region.
- Les preuves runtime impactees ont ete alignees sur ce contrat explicite: drafts mono-region orientes sur `EU`, plus de dependance a un `GLOBAL` implicite, et `trade-swap-helpers.ts` utilise une fixture explicite compatible avec les picks attendus.
- Dette hors scope documentee: `npm run test:vitest` complet echoue toujours sur `player-catalogue-page.component.spec.ts` (erreur `network`), et la suite Playwright UI reste bloquee si `http://localhost:4200` n'est pas disponible.

### File List

- `src/main/java/com/fortnite/pronos/service/game/CreateGameRegionRulesContract.java`
- `src/main/java/com/fortnite/pronos/core/usecase/CreateGameUseCase.java`
- `src/main/java/com/fortnite/pronos/service/game/GameCreationService.java`
- `src/main/java/com/fortnite/pronos/service/ValidationService.java`
- `src/main/java/com/fortnite/pronos/dto/CreateGameRequest.java`
- `src/test/java/com/fortnite/pronos/core/usecase/CreateGameUseCaseTest.java`
- `src/test/java/com/fortnite/pronos/service/game/GameCreationServiceDomainMigrationTest.java`
- `src/test/java/com/fortnite/pronos/controller/GameControllerSimpleTest.java`
- `src/test/java/com/fortnite/pronos/integration/CreateGameIntegrationTest.java`
- `src/test/java/com/fortnite/pronos/dto/CreateGameRequestValidationTest.java`
- `src/test/java/com/fortnite/pronos/dto/CreateGameRequestTest.java`
- `src/test/java/com/fortnite/pronos/service/ValidationServiceTddTest.java`
- `frontend/src/app/features/game/create-game/create-game.component.ts`
- `frontend/src/app/features/game/create-game/game-creation-wizard.component.ts`
- `frontend/src/app/features/game/models/game.interface.ts`
- `frontend/src/app/features/game/create-game/create-game-region-rules.util.ts`
- `frontend/src/app/features/game/create-game/create-game-region-rules.util.spec.ts`
- `frontend/src/app/features/game/create-game/create-game.component.spec.ts`
- `frontend/src/app/features/game/create-game/game-creation-wizard.component.spec.ts`
- `frontend/src/app/features/game/services/game-command.service.spec.ts`
- `frontend/src/app/features/game/services/game.service.spec.ts`
- `frontend/e2e/draft-full-flow.spec.ts`
- `frontend/e2e/delete-archive.spec.ts`
- `frontend/e2e/draft-snake-tranches.spec.ts`
- `frontend/e2e/draft-two-players.spec.ts`
- `frontend/e2e/sprint14-ui-draft-complete.spec.ts`
- `frontend/e2e/draft-navigateur.spec.ts`
- `frontend/e2e/helpers/trade-swap-helpers.ts`
- `_bmad-output/implementation-artifacts/sprint19-contract-create-game-region-rules.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `tasks/todo.md`

### Change Log

- 2026-04-24: Story creee en `ready-for-dev` suite au correct-course Sprint 19 du 2026-04-24.
- 2026-04-25: Contrat `createGame()/regionRules` implemente cote backend/frontend; fallback implicite supprime, helper backend partage introduit, payloads create-game explicites et tests cibles verts.
- 2026-04-25: Fixtures et suites E2E impactees realignees sur des `regionRules` explicites; verification Playwright partielle validee puis bloquee par l'absence du serveur frontend local `http://localhost:4200`.
