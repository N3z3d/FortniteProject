# Story: sprint19-fix-resolution-adapter-config - Validation explicite de l'adapter de resolution

Status: done

<!-- METADATA
  story_key: sprint19-fix-resolution-adapter-config
  sprint: Sprint 19
  priority: P3 (tech debt retro Sprint 18 - A18c)
  date_created: 2026-04-21
  source: sprint18-retrospective A18c + sprint-status.yaml Sprint 19 Phase 4
-->

## Story

En tant que developpeur exploitant le pipeline d'identite joueurs,
je veux que la configuration `RESOLUTION_ADAPTER` soit validee explicitement au demarrage,
afin qu'une valeur vide ou inconnue produise un message actionnable au lieu d'un `NoSuchBeanDefinitionException` opaque.

## Context / Root Cause

La story Sprint 15 a introduit deux implementations de `ResolutionPort` selectionnees par `resolution.adapter`:
- `StubResolutionAdapter` actif pour `resolution.adapter=stub` ou propriete absente.
- `FortniteApiResolutionAdapter` actif pour `resolution.adapter=fortnite-api`.

L'etat actuel de `application.yml` definit pourtant `resolution.adapter: ${RESOLUTION_ADAPTER:}`. Si la variable est absente ou vide, Spring voit une propriete presente mais blank; `matchIfMissing=true` ne s'applique pas, aucun bean `ResolutionPort` n'est cree, puis les services qui injectent ce port echouent avec une erreur generique de dependance manquante.

La retrospective Sprint 18 trace ce probleme en E3 / A18c: `RESOLUTION_ADAPTER=""` mene a `NoSuchBeanDefinitionException`; le demarrage doit echouer avec un message explicite.

Decision de story:
- `RESOLUTION_ADAPTER` absent doit activer `stub`, conformement au contrat historique "stub par defaut".
- `RESOLUTION_ADAPTER=""`, blank ou valeur inconnue doit echouer en fail-fast avec un message clair listant les valeurs supportees: `stub`, `fortnite-api`.
- Aucun fallback silencieux vers `stub` pour une valeur explicitement invalide, car cela masquerait une mauvaise configuration de production.

## Acceptance Criteria

1. Si `resolution.adapter` est absent, le contexte Spring cree un seul bean `ResolutionPort`, `StubResolutionAdapter`, et `adapterName()` retourne `stub`.
2. Si `resolution.adapter=stub`, le contexte Spring cree un seul bean `ResolutionPort`, `StubResolutionAdapter`, et `adapterName()` retourne `stub`.
3. Si `resolution.adapter=fortnite-api`, le contexte Spring cree un seul bean `ResolutionPort`, `FortniteApiResolutionAdapter`, et `adapterName()` retourne `fortnite-api`.
4. Si `resolution.adapter` est blank (`""`, espaces) ou une valeur inconnue, le demarrage echoue avant l'injection des consumers avec une erreur explicite contenant au minimum:
   - la cle `resolution.adapter`;
   - la valeur invalide recue;
   - les valeurs autorisees `stub` et `fortnite-api`;
   - une indication de configuration via `RESOLUTION_ADAPTER`.
5. Pour les valeurs invalides, l'erreur principale documentee par les tests ne doit pas etre `NoSuchBeanDefinitionException` / `UnsatisfiedDependencyException` sur `ResolutionPort`.
6. `application.yml` utilise un defaut coherent avec le contrat local: absence de `RESOLUTION_ADAPTER` => `resolution.adapter=stub`.
7. L'endpoint existant `GET /api/admin/players/pipeline/adapter-info` reste coherent pour les deux configs valides (`stub`, `fortnite-api`).
8. Aucun changement frontend, DB/Flyway, contrat REST public, `ResolutionPort.resolvePlayer(...)`, ou reintroduction de `resolveFortniteId(...)`.

## Tasks / Subtasks

- [x] Task 1 - Tests rouges de configuration Spring (AC: #1, #2, #3, #4, #5)
  - [x] 1.1 Creer `src/test/java/com/fortnite/pronos/config/ResolutionAdapterConfigurationTest.java`.
  - [x] 1.2 Utiliser `ApplicationContextRunner` pour charger seulement les adapters resolution et un mock `FortniteApiPort`.
  - [x] 1.3 Couvrir `resolution.adapter` absent -> bean unique `StubResolutionAdapter`.
  - [x] 1.4 Couvrir `resolution.adapter=stub` -> bean unique `StubResolutionAdapter`.
  - [x] 1.5 Couvrir `resolution.adapter=fortnite-api` -> bean unique `FortniteApiResolutionAdapter`.
  - [x] 1.6 Couvrir `resolution.adapter=` blank et `resolution.adapter=bogus` -> startup failure avec message explicite.
  - [x] 1.7 Verifier dans les assertions que le failure message ne depend pas d'une absence opaque de bean `ResolutionPort`.

- [x] Task 2 - Corriger le defaut de configuration (AC: #1, #6)
  - [x] 2.1 Modifier `src/main/resources/application.yml` pour que `RESOLUTION_ADAPTER` absent resolve vers `stub`.
  - [x] 2.2 Conserver `docker-compose.local.yml` en mode local explicite `RESOLUTION_ADAPTER=${RESOLUTION_ADAPTER:-fortnite-api}` sauf si les tests prouvent une incoherence.
  - [x] 2.3 Documenter dans les commentaires config que `stub` est le defaut local sans variable, et `fortnite-api` requiert une cle API valide.

- [x] Task 3 - Ajouter une validation fail-fast actionnable (AC: #4, #5)
  - [x] 3.1 Ajouter un validateur de configuration dans `src/main/java/com/fortnite/pronos/config/` ou un package config adjacent.
  - [x] 3.2 Valider la valeur apres resolution des placeholders et trim.
  - [x] 3.3 Rejeter blank explicite et toute valeur hors `stub|fortnite-api` avec une exception claire; ne pas rejeter une propriete absente dans un contexte de test ou `application.yml` n'est pas charge, car `matchIfMissing=true` doit encore activer le stub.
  - [x] 3.4 Garantir que cette validation s'execute avant que `PlayerIdentityPipelineService` ou `ResolutionQueueService` ne provoque un `NoSuchBeanDefinitionException`.
  - [x] 3.5 Ne pas ajouter de dependance externe pour cette validation.

- [x] Task 4 - Preserver le wiring existant des adapters (AC: #2, #3, #7, #8)
  - [x] 4.1 Ne pas changer la semantique de `StubResolutionAdapter.resolvePlayer(...)`.
  - [x] 4.2 Ne pas changer la semantique de `FortniteApiResolutionAdapter.resolvePlayer(...)`.
  - [x] 4.3 Verifier que `adapterName()` reste `stub` / `fortnite-api`.
  - [x] 4.4 Verifier que `PlayerIdentityPipelineService.getAdapterInfo()` ne necessite aucune nouvelle dependance.
  - [x] 4.5 Ne pas toucher a `ResolutionPort` sauf necessite directe de compilation; ne pas reintroduire `resolveFortniteId(...)`.

- [x] Task 5 - Validation backend (AC: #1 a #8)
  - [x] 5.1 Executer `mvn spotless:apply --no-transfer-progress`.
  - [x] 5.2 Executer `mvn -Dtest="ResolutionAdapterConfigurationTest,FortniteApiResolutionAdapterTest,ResolutionQueueServiceTest,PlayerIdentityPipelineServiceTest,AdminPlayerPipelineControllerTest" test --no-transfer-progress`.
  - [x] 5.3 Executer `rg -n "resolveFortniteId" src/main src/test` et documenter zero resultat.
  - [x] 5.4 Si une suite large est lancee et rencontre des dettes pre-existantes (`GameDataIntegrationTest`, `GameStatisticsServiceTddTest`, `CouplingTest` sur `GameDraftService`), les documenter sans les masquer ni les corriger dans cette story.

### Review Findings

- [x] [Review][Defer] `fortnite-api` peut demarrer sans `FORTNITE_API_KEY`, puis retourner silencieusement `not found` au premier lookup [src/main/java/com/fortnite/pronos/adapter/out/api/FortniteApiAdapter.java:35] - deferred, pre-existing; tracking cree : `sprint19-fix-fortnite-api-key-config`

## Dev Notes

### Developer Context

Cette story est un correctif de configuration backend. Elle ne doit pas elargir le pipeline, ajouter d'endpoint, modifier le frontend, ou changer la resolution metier.

Le but n'est pas de rendre les configs invalides "fonctionnelles"; le but est de rendre l'echec comprehensible et previsible. Une valeur explicitement invalide de `RESOLUTION_ADAPTER` doit etre traitee comme une erreur de deploiement.

### Current Code Anchors

- `src/main/resources/application.yml`
  - Contient `resolution.adapter: ${RESOLUTION_ADAPTER:}`; c'est la source du blank par defaut.
- `docker-compose.local.yml`
  - Definit `RESOLUTION_ADAPTER=${RESOLUTION_ADAPTER:-fortnite-api}` pour les validations locales avec API Fortnite live.
- `src/main/java/com/fortnite/pronos/domain/port/out/ResolutionPort.java`
  - Contrat actuel: `resolvePlayer(String pseudo, String region)` + `adapterName()`.
- `src/main/java/com/fortnite/pronos/adapter/out/resolution/StubResolutionAdapter.java`
  - `@ConditionalOnProperty(name = "resolution.adapter", havingValue = "stub", matchIfMissing = true)`.
- `src/main/java/com/fortnite/pronos/adapter/out/resolution/FortniteApiResolutionAdapter.java`
  - `@ConditionalOnProperty(name = "resolution.adapter", havingValue = "fortnite-api")`.
- `src/main/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineService.java`
  - Injecte `ResolutionPort`, expose `getAdapterInfo()` et `suggestEpicId(...)`.
- `src/main/java/com/fortnite/pronos/service/ingestion/ResolutionQueueService.java`
  - Injecte `ResolutionPort` pour le batch de resolution non bloquant.

### Architecture Compliance

- Respecter l'architecture hexagonale: les services continuent de dependre de `ResolutionPort`, jamais des adapters concrets.
- Les adapters restent dans `adapter/out/resolution`; la validation de config appartient a `config/`, pas au domaine.
- Ne pas ajouter de nouveau port, controller, DTO public ou migration Flyway.
- Ne pas augmenter le couplage de `PlayerIdentityPipelineService` ou `ResolutionQueueService`.
- Si un nouveau `@Service` est ajoute, verifier la limite CouplingTest de 7 dependances; preferer une classe `@Configuration` ou un validateur dedie sans dependances metier.

### Implementation Guidance

Approche recommandee:
- Corriger le defaut YAML vers `resolution.adapter: ${RESOLUTION_ADAPTER:stub}`.
- Ajouter un validateur de demarrage qui inspecte `Environment#getProperty("resolution.adapter")` apres trim.
- Le validateur doit s'executer assez tot pour produire son propre message d'erreur avant les consumers de `ResolutionPort`. Si un simple bean normal ne garantit pas l'ordre, utiliser un mecanisme Spring de demarrage precoce (`BeanFactoryPostProcessor`, `EnvironmentPostProcessor`, ou equivalent local teste par `ApplicationContextRunner`).

Message d'erreur attendu, en substance:

```text
Invalid resolution.adapter value '<value>'. Supported values: stub, fortnite-api.
Set RESOLUTION_ADAPTER=stub for local stub mode or RESOLUTION_ADAPTER=fortnite-api with FORTNITE_API_KEY configured.
```

Ne pas normaliser une valeur inconnue vers `stub`: cela ferait passer les tests mais recreerait le risque de production identifie en retrospective.

### Testing Requirements

Tests unitaires/config requis:
- Nominal 1: propriete absente -> `StubResolutionAdapter`.
- Nominal 2: `resolution.adapter=stub` -> `StubResolutionAdapter`.
- Nominal 3: `resolution.adapter=fortnite-api` -> `FortniteApiResolutionAdapter`.
- Edge 1: `resolution.adapter=` -> erreur explicite.
- Edge 2: `resolution.adapter=   ` -> erreur explicite.
- Edge 3: `resolution.adapter=bogus` -> erreur explicite.
- Regression: `adapterName()` reste aligne avec `adapter-info`.

Validation ciblee:

```powershell
mvn spotless:apply --no-transfer-progress
mvn -Dtest="ResolutionAdapterConfigurationTest,FortniteApiResolutionAdapterTest,ResolutionQueueServiceTest,PlayerIdentityPipelineServiceTest,AdminPlayerPipelineControllerTest" test --no-transfer-progress
rg -n "resolveFortniteId" src/main src/test
```

### Previous Story Intelligence

- `sprint19-fix-deprecated-resolution` a supprime le bridge `ResolutionPort.resolveFortniteId(...)`. Cette story ne doit pas le reintroduire.
- La validation ciblee de la story precedente etait verte sur `FortniteApiResolutionAdapterTest`, `ResolutionQueueServiceTest`, `PlayerIdentityPipelineServiceTest`, `PrIngestionRowProcessorUnresolvedTest`, `PrIngestionRowProcessorAliasTest`.
- Les regressions larges documentees comme hors-scope restent: `GameDataIntegrationTest`, `GameStatisticsServiceTddTest.shouldMapNullPlayerIdsToUnknown`, et `CouplingTest` sur `GameDraftService`.

### Git Intelligence

Derniers commits observes:
- `5819c24 fix(pipeline): remove deprecated resolution bridge`
- `c1d9320 fix(draft): guard all draft routes against non-DRAFTING game status`
- `cce8138 fix(store): prevent race condition when force-refreshing games on navigation`

Signal utile: les lots recents restent petits, testes cible par cible, puis documentent les dettes pre-existantes au lieu de les melanger au correctif.

### Pre-existing Gaps / Known Issues

- Le worktree contient de nombreux changements non lies a cette story. Le dev agent ne doit pas les revert.
- `ResolutionQueueService` n'a pas encore de trigger runtime de production; dette pre-existante deferree par la review de `sprint19-fix-deprecated-resolution`.
- `tryResolveEntry()` conserve un catch large autour de la mutation et de la persistence; dette pre-existante a traiter separement.
- `docker-compose.local.yml` force `fortnite-api` par defaut en local Docker; cette story ne doit changer ce choix que si la config invalide ne peut pas etre corrigee autrement.

### Latest Technical Information

Aucune recherche web n'est requise. Le sujet concerne le wiring Spring interne existant, sans changement de version, dependance externe ou API publique. Les versions a respecter restent celles du project context: Java 21 et Spring Boot 3.4.5.

### References

- `_bmad-output/implementation-artifacts/sprint18-retrospective.md` - E3 et action item A18c.
- `_bmad-output/implementation-artifacts/sprint18-adapter-info-endpoint.md` - origine review deferred E3 et contrat `adapterName()`.
- `_bmad-output/implementation-artifacts/deferred-work.md` - dette centralisee E3.
- `_bmad-output/implementation-artifacts/sprint19-fix-deprecated-resolution.md` - contrat `resolvePlayer(...)` nettoye, ne pas reintroduire `resolveFortniteId`.
- `_bmad-output/implementation-artifacts/sprint-status.yaml` - Sprint 19 Phase 4.
- `_bmad-output/planning-artifacts/architecture.md` - `ResolutionPort` swappable et adapters dans `adapter/out/resolution`.
- `_bmad-output/planning-artifacts/prd.md` - NFR-I02, NFR-R03, NFR-M01.
- `_bmad-output/project-context.md` - Spring Boot 3.4.5, architecture hexagonale, CouplingTest, DoD backend.

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- Red: `mvn -Dtest=ResolutionAdapterConfigurationTest test --no-transfer-progress` a d'abord echoue sur la classe de validation absente, apres correction d'une assertion AssertJ incompatible.
- Green cible: `mvn -Dtest=ResolutionAdapterConfigurationTest test --no-transfer-progress` -> 6 tests, 0 failure.
- Spotless: `mvn spotless:apply --no-transfer-progress` -> succes, fichiers Java formates.
- Validation ciblee: `mvn -Dtest="ResolutionAdapterConfigurationTest,FortniteApiResolutionAdapterTest,ResolutionQueueServiceTest,PlayerIdentityPipelineServiceTest,AdminPlayerPipelineControllerTest" test --no-transfer-progress` -> 61 tests, 0 failure.
- Garde-fou: `rg -n "resolveFortniteId" src/main src/test` -> zero resultat.
- Regression large: `mvn test --no-transfer-progress` -> echec uniquement sur dettes pre-existantes documentees: `CouplingTest` (`GameDraftService` 8 deps), `GameDataIntegrationTest` (4 fixtures), `GameStatisticsServiceTddTest.shouldMapNullPlayerIdsToUnknown` (NPE).
- Code review: BMAD layers executes; Acceptance Auditor valide les ACs, 1 dette hors scope deferree, 4 findings rejetes comme intentionnels ou non reproduits.
- Verification review: `mvn -Dtest=ResolutionAdapterConfigurationTest test --no-transfer-progress` -> 6 tests, 0 failure.

### Completion Notes List

- Story creee par workflow `bmad-create-story`.
- Ultimate context engine analysis completed - comprehensive developer guide created.
- Ajout de `ResolutionAdapterConfiguration` avec validation fail-fast via `BeanFactoryPostProcessor`, executee avant l'instanciation des consumers de `ResolutionPort`.
- `resolution.adapter` absent reste compatible avec `matchIfMissing=true` et active le stub; `resolution.adapter=` / espaces / valeur inconnue echouent avec un message actionnable mentionnant `RESOLUTION_ADAPTER`, `stub` et `fortnite-api`.
- `application.yml` utilise maintenant le defaut local `RESOLUTION_ADAPTER:stub` et documente le mode `fortnite-api` avec `FORTNITE_API_KEY`.
- Aucun changement de semantique sur `ResolutionPort`, `StubResolutionAdapter`, `FortniteApiResolutionAdapter`, frontend, DB/Flyway ou contrat REST public.
- Code review BMAD terminee sans patch fonctionnel requis; la dette `fortnite-api` sans `FORTNITE_API_KEY` est deferree dans `deferred-work.md` et trackee par `sprint19-fix-fortnite-api-key-config`.

### File List

- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/deferred-work.md`
- `_bmad-output/implementation-artifacts/sprint19-fix-resolution-adapter-config.md`
- `src/main/java/com/fortnite/pronos/adapter/out/resolution/FortniteApiResolutionAdapter.java`
- `src/main/java/com/fortnite/pronos/adapter/out/resolution/StubResolutionAdapter.java`
- `src/main/java/com/fortnite/pronos/config/ResolutionAdapterConfiguration.java`
- `src/main/java/com/fortnite/pronos/controller/AdminPlayerPipelineController.java`
- `src/main/java/com/fortnite/pronos/domain/player/model/FortnitePlayerData.java`
- `src/main/java/com/fortnite/pronos/dto/admin/AdapterInfoResponse.java`
- `src/main/java/com/fortnite/pronos/dto/admin/EpicIdSuggestionResponse.java`
- `src/main/resources/application.yml`
- `src/test/java/com/fortnite/pronos/controller/AdminPlayerPipelineControllerTest.java`
- `src/test/java/com/fortnite/pronos/config/ResolutionAdapterConfigurationTest.java`
- `tasks/todo.md`

### Change Log

- 2026-04-21: Story creee en `ready-for-dev`.
- 2026-04-21: Validation fail-fast de `resolution.adapter` implementee, tests de configuration ajoutes, story passee en `review`.
- 2026-04-22: Code review BMAD terminee; aucune correction fonctionnelle requise, 1 dette hors scope deferree, story passee en `done`.
