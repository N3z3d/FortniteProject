# Story: sprint19-migrate-canonical-game-participants - Invariant canonique createur-participant

Status: done

<!-- METADATA
  story_key: sprint19-migrate-canonical-game-participants
  sprint: Sprint 19
  priority: P3 (correct-course structural follow-up, hors scope invitation-code)
  date_created: 2026-04-29
  source: correct-course sprint19 invitation-code 2026-04-24 + BMAD code-review sprint19-feat-invitation-code-advanced 2026-04-29
-->

## Story

En tant que mainteneur du domaine Game,
je veux que le createur d'une game soit toujours persiste comme participant canonique,
afin que les counts, les checks de capacite, la readiness draft et les lectures legacy reposent sur une seule source de verite.

## Context / Root Cause

Le correct-course du 2026-04-24 et la review de `sprint19-feat-invitation-code-advanced` ont isole un sujet hors scope: le systeme n'a pas encore ferme globalement l'invariant "createur = participant canonique".

Etat actuel observe:

- `CreateGameUseCase.execute(...)` et `GameCreationService.createGame(...)` ajoutent deja le createur dans le modele domaine avant persistence.
- `GameEntityMapper.toDomain(...)` ajoute encore un participant createur synthetique quand la ligne manque dans l'entite JPA.
- `GameParticipantService.ensureCreatorParticipantPersisted(...)` persiste encore une ligne createur apres certains joins, avec un commentaire de compatibilite legacy.
- `Game.getTotalParticipantCount()` retourne `participants.size()`, donc les counts deviennent faux si une game legacy est restauree sans ligne createur et sans couche de compatibilite.
- `Game.isParticipant(creatorId)` retourne toujours `true`, ce qui masque une partie du probleme pour les guards, mais ne remplace pas une ligne persistante canonique.

Decision de story:

- le createur doit exister comme ligne `game_participants` persistante pour chaque game non supprimee concernnee;
- les games legacy sans ligne createur doivent etre auditees puis backfillees de facon explicite;
- les chemins de creation doivent continuer a persister le createur exactement une fois;
- les helpers de compatibilite ne doivent plus etre la source durable du count ou de la readiness;
- cette story ne doit pas modifier le contrat invitation-code ni le contrat `createGame()/regionRules`.

## Acceptance Criteria

1. Un audit identifie les games non supprimees dont le createur n'a pas de ligne correspondante dans `game_participants`, sans modifier de donnees pendant l'audit.
2. Un backfill/mecanisme de migration dedie cree les lignes createur manquantes de facon idempotente: relancer l'operation ne cree aucun doublon.
3. Apres backfill, toute game non supprimee avec `creator_id` valide possede exactement une ligne participant pour ce createur, marquee `creator = true`.
4. Les games creees apres cette story persistent le createur comme participant canonique des le flux de creation, sur `CreateGameUseCase` et `GameCreationService`, sans dependance a un helper post-join.
5. Les counts de participants, `isFull()`, `canAddParticipants()`, `startDraft()` et les DTO `currentParticipantCount` restent corrects pour:
   - une game legacy backfillee;
   - une game deja canonique;
   - une game creee apres migration;
   - une tentative de doublon createur.
6. Le systeme empeche ou ignore explicitement les doublons `(game_id, user_id)` pour le createur; aucun chemin applicatif ne peut produire deux lignes createur pour la meme game.
7. `GameEntityMapper.ensureCreatorParticipant(...)` et `GameParticipantService.ensureCreatorParticipantPersisted(...)` sont retires ou transformes en garde documente sans effet normal apres preuve que le backfill couvre les donnees ciblees.
8. Aucun comportement invitation-code n'est change: generation, suppression, consommation unique et messages de join restent hors scope.
9. Aucun changement fonctionnel `regionRules` n'est introduit; le contrat explicite create-game reste porte par `sprint19-contract-create-game-region-rules`.
10. Les tests couvrent au minimum:
    - legacy sans ligne createur -> backfill cree une seule ligne;
    - legacy deja canonique -> aucun doublon;
    - create-game nouveau -> createur persiste exactement une fois;
    - count/capacite/draft readiness apres backfill;
    - non-regression DTO pour `currentParticipantCount`;
    - idempotence du backfill.

## Tasks / Subtasks

- [x] Task 1 - Ecrire les tests rouges de l'invariant canonique (AC: #1, #2, #3, #5, #10)
  - [x] 1.1 Ajouter un test repository/service qui construit une game legacy sans ligne createur et prouve qu'elle est detectee.
  - [x] 1.2 Ajouter un test d'idempotence: backfill execute deux fois -> une seule ligne createur.
  - [x] 1.3 Ajouter un test count/capacite: une game backfillee compte le createur une fois et garde `isFull()` / `canAddParticipants()` coherents.
  - [x] 1.4 Ajouter un test draft readiness: createur + un participant permet `startDraft()`, createur seul ne suffit pas.
  - [x] 1.5 Ajouter un test DTO mapper ou integration qui verrouille `currentParticipantCount` sans fallback synthetique.

- [x] Task 2 - Concevoir le backfill sans changement de schema public non valide (AC: #1, #2, #3, #6)
  - [x] 2.1 Choisir explicitement l'approche: migration SQL de donnees, service admin/maintenance, ou startup repair borne.
  - [x] 2.2 Justifier le choix dans la story avant implementation si cela touche une zone sensible DB.
  - [x] 2.3 Filtrer les games soft-deleted selon la decision produit; par defaut, cibler les games non supprimees.
  - [x] 2.4 Inserer `joined_at` a partir de `game.created_at` quand disponible, sinon utiliser un timestamp controle.
  - [x] 2.5 Marquer la ligne createur avec `is_creator = true`.
  - [x] 2.6 Garantir l'idempotence via une condition `NOT EXISTS` ou une contrainte/verification applicative.

- [x] Task 3 - Verrouiller les chemins de creation et de persistence (AC: #4, #6, #10)
  - [x] 3.1 Relire `CreateGameUseCase.execute(...)` et `GameCreationService.createGame(...)`; conserver un seul pattern d'ajout du createur.
  - [x] 3.2 Verifier `GameRepositoryAdapter.save(...)` et `GameEntityMapper.toEntity(...)` pour ne pas perdre la ligne createur lors d'un save/update.
  - [x] 3.3 Ajouter ou renforcer les tests sur `GameRepositoryAdapter` et `GameEntityMapper` pour la persistence exacte des participants.
  - [x] 3.4 Si une protection contre doublons est ajoutee, preferer une solution locale et testee; ne pas ajouter une dependance externe.

- [x] Task 4 - Retirer la compatibilite devenue inutile ou la borner explicitement (AC: #5, #7)
  - [x] 4.1 Supprimer ou neutraliser `GameParticipantService.ensureCreatorParticipantPersisted(...)` apres preuve que le backfill couvre les donnees ciblees.
  - [x] 4.2 Remplacer `GameEntityMapper.ensureCreatorParticipant(...)` par un mapping strict, ou conserver un garde documente uniquement si une decision explicite l'impose.
  - [x] 4.3 Verifier que `Game.getTotalParticipantCount()` reste base sur la collection persistante et ne reintegre pas un fallback `+1`.
  - [x] 4.4 Verifier que `Game.isParticipant(...)` ne masque pas un count faux dans les tests de cette story.

- [x] Task 5 - Validation ciblee et documentation de cloture (AC: #8, #9, #10)
  - [x] 5.1 Executer `mvn spotless:apply --no-transfer-progress`.
  - [x] 5.2 Executer les tests backend cibles ajoutes/changes, a minima les tests domain, mapper, repository adapter et services create/join touches.
  - [x] 5.3 Si une migration SQL est ajoutee, executer une preuve integration H2/PostgreSQL locale adaptee au pattern du repo.
  - [x] 5.4 Confirmer qu'aucun fichier invitation-code, i18n ou frontend n'a ete modifie sauf necessite demontree.
  - [x] 5.5 Mettre a jour la File List et les Completion Notes avant passage en `review`.

### Review Findings

- [x] [Review][Patch] Backfill createur incompatible avec un `draft_order = 1` deja occupe - corrige en attribuant le prochain `draft_order` disponible dans la migration SQL et le service de maintenance, avec test de regression.
- [x] [Review][Defer] Hygiene Git/worktree trop large pour cette story - deferred, une dette dediee est ajoutee dans `deferred-work.md`.

## Dev Notes

### Developer Context

Cette story est le deuxieme follow-up structurel sorti du correct-course Sprint 19. Elle existe pour eviter que la story invitation-code absorbe un chantier de migration de donnees.

Le resultat attendu n'est pas "faire passer un count par fallback". Le resultat attendu est que la persistence contienne la verite: le createur apparait comme participant canonique, exactement une fois, pour les games ciblees.

### Current Code Anchors

- `src/main/java/com/fortnite/pronos/domain/game/model/Game.java`
  - `getTotalParticipantCount()` retourne `participants.size()`.
  - `isFull()`, `canAcceptParticipants()`, `canAddParticipants()` et `startDraft()` dependent de ce count.
  - `isParticipant(UUID)` retourne `true` pour le createur meme si la liste ne contient pas sa ligne.
- `src/main/java/com/fortnite/pronos/core/usecase/CreateGameUseCase.java`
  - Ajoute un `GameParticipant` createur avec `draftOrder = 1` avant `gameDomainRepositoryPort.save(game)`.
- `src/main/java/com/fortnite/pronos/service/game/GameCreationService.java`
  - Ajoute le createur via `addCreatorAsParticipant(...)` avant `saveDomainGame(game)`.
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/game/GameEntityMapper.java`
  - `ensureCreatorParticipant(...)` ajoute un participant createur synthetique au mapping entity -> domain quand la ligne manque.
- `src/main/java/com/fortnite/pronos/service/game/GameParticipantService.java`
  - `ensureCreatorParticipantPersisted(...)` cree encore une ligne createur apres join si elle manque.
- `src/main/java/com/fortnite/pronos/model/GameParticipant.java`
  - Entite JPA `game_participants` avec `game`, `user`, `joinedAt`, `creator`; aucune contrainte visible dans l'entite ne garantit seule l'unicite `(game_id, user_id)`.
- `src/main/java/com/fortnite/pronos/repository/GameParticipantRepository.java`
  - Expose deja `existsByUserIdAndGameId(...)`, `existsByGameIdAndUserId(...)`, `findByUserIdAndGameId(...)` et les fetchs par game.

### Architecture Compliance

- Respecter l'architecture hexagonale en migration: le domaine reste sans Spring/JPA/Lombok.
- Garder les changements chirurgicaux: pas de refactor large des models Game/Draft/Participant hors invariant createur.
- Ne pas introduire de nouveau schema public ou migration structurelle sans plan explicite; une migration de donnees idempotente est acceptable si elle est justifiee et testee.
- Ne pas depasser 500 lignes par classe ni 50 lignes par methode; extraire un service de backfill si le code de migration grossit.
- Ne pas ajouter de dependance externe.

### Implementation Guidance

Approche recommandee:

1. Commencer par des tests rouges sur l'etat legacy: entite `Game` avec `creator` mais sans `GameParticipant` createur.
2. Ajouter le plus petit mecanisme de backfill idempotent compatible avec les patterns du repo.
3. Prouver que les chemins de creation creent deja ou continuent a creer exactement une ligne createur.
4. Retirer seulement ensuite les couches de compatibilite qui inventent ou persistent le createur tardivement.
5. Garder les corrections de count basees sur la collection persistante; ne pas reintegrer un fallback `creator + participants`.

Si une contrainte unique DB est envisagee, la traiter comme decision de conception explicite: verifier l'existant, backfiller/nettoyer les doublons d'abord, puis seulement appliquer la contrainte. Sans preuve, preferer une verification applicative idempotente bornee a cette story.

### Testing Requirements

Tests backend requis:

- nominal: create-game nouveau -> ligne createur presente et `currentParticipantCount = 1`;
- edge 1: legacy sans ligne createur -> backfill cree une ligne;
- edge 2: legacy deja canonique -> backfill ne cree pas de doublon;
- edge 3: duplicate potentiel -> le systeme refuse ou ignore proprement;
- edge 4: count/capacite/draft readiness apres backfill;
- edge 5: mapper entity -> domain ne masque plus un manque de persistence sans test qui le signale.

Commandes recommandees:

```powershell
mvn spotless:apply --no-transfer-progress
mvn -Dtest="GameDomainModelTest,GameEntityMapperTest,GameRepositoryAdapterTest,GameCreationServiceDomainMigrationTest,CreateGameUseCaseTest,GameParticipantServiceTddTest,GameParticipantServiceTest" test --no-transfer-progress
```

### Previous Story Intelligence

- `sprint14-fix-participants-count` a supprime les fallbacks `+1` dans `GameDtoMapper` et a identifie que l'invariant createur-participant restait incomplet pour les donnees legacy.
- `sprint19-contract-create-game-region-rules` a deja traite le contrat explicite `regionRules`; ne pas rouvrir ce sujet ici.
- `sprint19-feat-invitation-code-advanced` a confirme que ce chantier est hors scope invitation-code et doit rester separe.

### Pre-existing Gaps / Known Issues

- Le worktree est tres sale; ne rien revert hors scope.
- La story source `sprint19-feat-invitation-code-advanced` est encore `in-progress` tant que les changements hors scope ne sont pas separes ou rattaches proprement.
- Le projet documente des failures backend pre-existantes dans `_bmad-output/project-context.md`; ne pas masquer une nouvelle failure liee a cette story.
- Les suites Playwright locales exigent `http://localhost:4200` et le backend local; cette story devrait pouvoir etre validee principalement en backend cible.

### Latest Technical Information

Aucune recherche web n'est requise. Le sujet repose sur les patterns internes du depot: Java 21, Spring Boot 3.4.5, Spring Data JPA, H2/PostgreSQL local, JUnit/Mockito/AssertJ. Aucun changement de version ni nouvelle dependance n'est attendu.

### References

- `_bmad-output/planning-artifacts/epics.md` - Story 3.6 "Invariant canonique createur-participant".
- `_bmad-output/planning-artifacts/architecture.md` - Clarifications approuvees 2026-04-24, "Invariant de participation des games".
- `_bmad-output/planning-artifacts/sprint-change-proposal-2026-04-24.md` - Decision recommandee et sequence propre.
- `_bmad-output/implementation-artifacts/sprint14-fix-participants-count.md` - Historique des counts et follow-up correct-course.
- `_bmad-output/implementation-artifacts/sprint19-feat-invitation-code-advanced.md` - Story source qui exclut ce sujet du scope invitation-code.
- `_bmad-output/implementation-artifacts/sprint19-contract-create-game-region-rules.md` - Story soeur deja creee pour l'autre sujet hors scope.
- `_bmad-output/project-context.md` - Stack, architecture, DoD et baseline tests.
- `src/main/java/com/fortnite/pronos/domain/game/model/Game.java`
- `src/main/java/com/fortnite/pronos/core/usecase/CreateGameUseCase.java`
- `src/main/java/com/fortnite/pronos/service/game/GameCreationService.java`
- `src/main/java/com/fortnite/pronos/service/game/GameParticipantService.java`
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/game/GameEntityMapper.java`
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/game/GameRepositoryAdapter.java`
- `src/main/java/com/fortnite/pronos/repository/GameParticipantRepository.java`
- `src/main/java/com/fortnite/pronos/model/GameParticipant.java`
- `src/test/java/com/fortnite/pronos/domain/game/model/GameDomainModelTest.java`
- `src/test/java/com/fortnite/pronos/adapter/out/persistence/game/GameEntityMapperTest.java`
- `src/test/java/com/fortnite/pronos/adapter/out/persistence/game/GameRepositoryAdapterTest.java`
- `src/test/java/com/fortnite/pronos/service/game/GameCreationServiceDomainMigrationTest.java`
- `src/test/java/com/fortnite/pronos/core/usecase/CreateGameUseCaseTest.java`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-04-29: Story creee depuis la review BMAD de `sprint19-feat-invitation-code-advanced` apres verification que `sprint19-contract-create-game-region-rules` existait deja et que cette story n'avait qu'une entree `backlog` dans `sprint-status.yaml`.
- 2026-04-29: Contexte relu: epics Story 3.6, architecture clarifications 2026-04-24, sprint-change-proposal, story `sprint14-fix-participants-count`, code `Game`, `CreateGameUseCase`, `GameCreationService`, `GameParticipantService`, `GameEntityMapper`, `GameRepositoryAdapter`, repositories/tests associes.
- 2026-04-30: TDD red confirme sur `GameCreatorParticipantBackfillServiceTest,GameEntityMapperTest` avant creation du service de backfill.
- 2026-04-30: Regression large `mvn verify --no-transfer-progress` passee apres alignement de la fixture `GameWorkflowIntegrationTest` sur l'invariant canonique.

### Implementation Plan

- Backend: prouver l'etat legacy par tests, ajouter un backfill idempotent, puis retirer ou borner les helpers de compatibilite.
- Persistence: garantir exactement une ligne createur par game ciblee, sans doublon, et conserver les counts bases sur la collection persistante.
- Validation: couvrir domain/mapper/repository/service/usecase avec tests cibles avant passage en review.
- Decision de backfill: migration SQL de donnees `V50` + service maintenance teste pour l'audit/idempotence applicative; pas de schema public ajoute.
- Decision de compatibilite: mapping `GameEntityMapper` strict et suppression du helper post-join; les donnees legacy sont traitees par backfill explicite.

### Completion Notes List

- Audit et backfill implementes via `GameCreatorParticipantBackfillService`: detection read-only, update du flag createur existant, insertion idempotente des createurs manquants, soft-deleted ignorees.
- Migration SQL `V50__backfill_canonical_game_creator_participants.sql` ajoutee pour backfiller les donnees existantes sans changement de schema public.
- `GameEntityMapper` ne cree plus de participant synthetique; les chemins de creation `CreateGameUseCase` et `GameCreationService` restent verifies avec un createur persiste exactement une fois.
- `GameParticipantService` ne depend plus du helper legacy post-join; les tests join/create/repository valident counts, capacite, readiness draft et DTO `currentParticipantCount`.
- `GameWorkflowIntegrationTest` aligne sa fixture de game directe JPA sur l'invariant canonique pour ne plus tester l'ancien fallback implicite.
- Review follow-up 2026-04-30: le backfill legacy attribue maintenant le prochain `draft_order` disponible au createur lorsque `1` est deja occupe, au lieu de risquer l'index unique `uq_game_participants_game_draft_order`.
- Preuve migration adaptee au repo: Flyway est desactive sous profil `test`; la validation locale couvre donc le mecanisme de backfill en integration JPA/H2, tandis que `V50` reste une migration PostgreSQL data-only basee sur `uuid_generate_v4()` deja utilise dans le schema.
- Validations: `mvn spotless:apply --no-transfer-progress`; tests cibles `GameCreatorParticipantBackfillServiceTest,GameEntityMapperTest,GameRepositoryAdapterTest,GameCreationServiceDomainMigrationTest,CreateGameUseCaseTest,GameParticipantServiceTddTest,GameParticipantServiceTest,GameDomainModelTest` verts; `GameWorkflowIntegrationTest` vert; review follow-up cible `151 tests, 0 failure`; `mvn verify --no-transfer-progress` vert avant follow-up.

### File List

- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/sprint19-migrate-canonical-game-participants.md`
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/game/GameEntityMapper.java`
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/game/GameRepositoryAdapter.java`
- `src/main/java/com/fortnite/pronos/domain/port/out/GameDomainRepositoryPort.java`
- `src/main/java/com/fortnite/pronos/model/GameParticipant.java`
- `src/main/java/com/fortnite/pronos/repository/GameParticipantRepository.java`
- `src/main/java/com/fortnite/pronos/repository/GameRepository.java`
- `src/main/java/com/fortnite/pronos/service/game/GameCreatorParticipantBackfillService.java`
- `src/main/java/com/fortnite/pronos/service/game/GameParticipantService.java`
- `src/main/resources/db/migration/V50__backfill_canonical_game_creator_participants.sql`
- `src/test/java/com/fortnite/pronos/adapter/out/persistence/game/GameEntityMapperTest.java`
- `src/test/java/com/fortnite/pronos/adapter/out/persistence/game/GameRepositoryAdapterTest.java`
- `src/test/java/com/fortnite/pronos/core/usecase/CreateGameUseCaseTest.java`
- `src/test/java/com/fortnite/pronos/integration/GameWorkflowIntegrationTest.java`
- `src/test/java/com/fortnite/pronos/service/game/GameCreationServiceDomainMigrationTest.java`
- `src/test/java/com/fortnite/pronos/service/game/GameCreatorParticipantBackfillServiceTest.java`
- `src/test/java/com/fortnite/pronos/service/game/GameParticipantServiceTddTest.java`
- `src/test/java/com/fortnite/pronos/service/game/GameParticipantServiceTest.java`

### Change Log

- 2026-04-29: Story creee en `ready-for-dev` pour materialiser le hors scope createur-participant identifie par correct-course et code-review.
- 2026-04-30: Implementation completee; invariant createur-participant backfille, compatibilite legacy retiree, validations backend vertes; story prete pour review.
- 2026-04-30: Review follow-up applique; conflit potentiel `draft_order = 1` corrige, tests cibles verts, story passee en `done`.
