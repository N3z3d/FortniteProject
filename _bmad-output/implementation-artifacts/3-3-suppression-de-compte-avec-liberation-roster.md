# Story 3.3: Suppression de compte avec libération roster

Status: review

## Story

As a user,
I want supprimer mon compte proprement,
so that mes données personnelles soient retirées sans bloquer les parties en cours.

## Acceptance Criteria

1. **Given** un utilisateur authentifié appelle `DELETE /api/account`, **When** la suppression est traitée, **Then** le compte est soft-deleted (champ `deleted_at` non-null), le statut HTTP est 204 No Content — aucune donnée PII n'est retournée.

2. **Given** un utilisateur participe à des parties en statut CREATING, DRAFTING ou ACTIVE, **When** il supprime son compte, **Then** il est retiré de ces parties (ses `GameParticipant` sont supprimés) avant la suppression du compte.

3. **Given** un utilisateur ne participe qu'à des parties en statut FINISHED, **When** il supprime son compte, **Then** ses participations aux parties terminées sont conservées (pour l'historique) mais le compte est tout de même soft-deleted.

4. **Given** un utilisateur qui est créateur d'une partie en cours (CREATING / DRAFTING / ACTIVE), **When** il tente de supprimer son compte, **Then** le système répond 409 CONFLICT avec un message explicite — il doit d'abord archiver ou transférer la partie.

5. **Given** un utilisateur non authentifié appelle `DELETE /api/account`, **When** la requête arrive, **Then** le système répond 401 UNAUTHORIZED.

6. **Given** une précondition invalide (compte déjà supprimé), **When** la même action est soumise, **Then** le système répond 404 NOT FOUND.

## Tasks / Subtasks

- [x] Task 1: Migration Flyway `V37__add_user_soft_delete.sql` (AC: #1)
  - [x] `src/main/resources/db/migration/V37__add_user_soft_delete.sql`
  - [x] `ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;`
  - [x] Index partiel sur `deleted_at` IS NULL

- [x] Task 2: Ajouter `deletedAt` au `User` JPA model (AC: #1)
  - [x] `src/main/java/com/fortnite/pronos/model/User.java`
  - [x] Champ `LocalDateTime deletedAt;` avec `@Column(name = "deleted_at")`
  - [x] PAS de Lombok `@Data` modification — simplement ajouter le champ

- [x] Task 3: Ajouter `softDelete(UUID userId)` dans `UserRepositoryPort` + `UserRepository` (AC: #1)
  - [x] `src/main/java/com/fortnite/pronos/domain/port/out/UserRepositoryPort.java` — ajout `void softDelete(UUID userId, LocalDateTime deletedAt);`
  - [x] `src/main/java/com/fortnite/pronos/repository/UserRepository.java` — `@Modifying @Query` + `default` delegate

- [x] Task 4: Utiliser `findByCreatorId` et `findGamesByUserId` existants (AC: #4)
  - [x] Aucun nouveau port method nécessaire — `GameDomainRepositoryPort.findByCreatorId()` + `findGamesByUserId()` suffisent
  - [x] Filtrage par status dans `UserDeletionService`

- [x] Task 5: Créer `AccountDeletionBlockedException` (AC: #4)
  - [x] `src/main/java/com/fortnite/pronos/exception/AccountDeletionBlockedException.java`
  - [x] Extends `RuntimeException`, message explicite
  - [x] Handler dans `DomainExceptionHandler` → HTTP 409 CONFLICT, code `ACCOUNT_DELETION_BLOCKED`

- [x] Task 6: Créer `UserDeletionService` (AC: #1, #2, #3, #4, #6)
  - [x] `src/main/java/com/fortnite/pronos/service/UserDeletionService.java`
  - [x] 3 dépendances : `UserRepositoryPort`, `GameDomainRepositoryPort`, `GameParticipantService`
  - [x] Méthode `deleteAccount(UUID userId)` implémentée avec les 5 étapes

- [x] Task 7: Créer `AccountController` (AC: #1, #5)
  - [x] `src/main/java/com/fortnite/pronos/controller/AccountController.java`
  - [x] `DELETE /api/account` — résolution user via `UserResolver.resolve()`
  - [x] Retourne 204 si succès, 401 si non authentifié

- [x] Task 8: Tests TDD `UserDeletionServiceTest` (AC: #1, #2, #3, #4, #5, #6)
  - [x] `src/test/java/com/fortnite/pronos/service/UserDeletionServiceTest.java` (nouveau fichier — 9 tests)
  - [x] Nominal: delete → soft-delete appelé, user libéré de ses parties actives
  - [x] Nominal: delete → parties FINISHED non touchées
  - [x] Edge: user inexistant → `UserNotFoundException`
  - [x] Edge: user déjà soft-deleted → `UserNotFoundException`
  - [x] Edge: user créateur de partie active → `AccountDeletionBlockedException`
  - [x] Edge: user participant à plusieurs parties actives → retiré de toutes
  - [x] Edge: filtre créateur vs participant actif correct

## Dev Notes

### Architecture

Flux complet :
```
AccountController → UserDeletionService → UserRepositoryPort.softDelete()
                                       → GameDomainRepositoryPort.findActiveGamesByCreatorId()
                                       → GameParticipantService.leaveGame() (pour chaque partie active)
```

**CouplingTest** : `UserDeletionService` aura 3 dépendances injectées (`UserRepositoryPort`, `GameDomainRepositoryPort`, `GameParticipantService`) → bien sous la limite de 7.

**DomainIsolationTest** : `AccountDeletionBlockedException` est dans `exception/` (pas `domain/`) → conforme.

### Soft-delete User — Pattern

Même pattern que `Game` (V30 migration) :
- `deleted_at TIMESTAMP` nullable en base
- Champ `LocalDateTime deletedAt` dans le JPA model
- `softDelete(userId)` via `@Modifying @Query` (UPDATE, pas DELETE)
- Les requêtes existantes sur `UserRepository` ne filtrent PAS `deleted_at` — c'est intentionnel pour cette story (scope minimal). Un filtre global pourra être ajouté dans une story future si nécessaire.

### Libération roster — Parties actives

Statuts "actifs" à traiter : `CREATING`, `DRAFTING`, `ACTIVE`
Statuts "terminés" à ignorer : `FINISHED`

La libération utilise `GameParticipantService.leaveGame(userId, gameId)` qui appelle `removeUserFromGame()` (retire du domaine + sauvegarde).

**Attention** : `leaveGame()` lève `UnauthorizedAccessException` si l'utilisateur est le créateur. On gère ce cas AVANT (AC #4) en bloquant la suppression si créateur d'une partie active.

### `findActiveGamesByCreatorId` — Query

Requête JPA à ajouter dans `GameRepository`:
```java
@Query("SELECT g FROM GameEntity g WHERE g.creatorId = :creatorId AND g.status IN ('CREATING', 'DRAFTING', 'ACTIVE') AND g.deletedAt IS NULL")
List<GameEntity> findActiveGamesByCreatorId(@Param("creatorId") UUID creatorId);
```

### `findActiveParticipations` — Alternative via `GameParticipantRepositoryPort`

Pour trouver les parties actives où l'user est participant (non-créateur), deux approches possibles :
1. Utiliser `gameParticipantRepository.findByUserId(userId)` + filtrer — mais `findByUserId` n'existe pas encore
2. Utiliser `gameDomainRepository.findAll()` + filtrer — coûteux

**Recommandé** : Ajouter `List<GameParticipant> findActiveParticipationsByUserId(UUID userId)` dans `GameParticipantRepositoryPort` avec une query native SQL ou JPQL qui joint `game_participants` et `games` pour filtrer statut+non-créateur+non-deleté. Ça garde tout en domain port.

Alternative plus simple : `GameParticipantRepositoryPort.findByUserId(userId)` retourne toutes les participations → filtrer dans le service sur `game.getStatus()`. Ajouter la méthode `List<com.fortnite.pronos.model.GameParticipant> findByUserId(UUID userId)` au port.

**Choix implémentation** : ajouter `findByUserId(UUID userId)` au `GameParticipantRepositoryPort` (méthode dérivée Spring Data — triviale), puis dans le service charger le domain Game pour chaque participation et filtrer les statuts actifs.

### Règles d'architecture obligatoires

- **MaximumClassLinesCondition** : `UserDeletionService.java` ≤ 500 lignes (~100 lignes attendues)
- **DomainExceptionHandler** : vérifier taille avant ajout handler (actuellement ~150 lignes, safe)
- **Spotless** : `mvn spotless:apply -q --no-transfer-progress` avant `mvn test`
- **Linter** : relire les fichiers test après écriture — le linter peut corrompre les lambdas

### Pattern test TDD (reproduire exactement)

```java
@ExtendWith(MockitoExtension.class)
class UserDeletionServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private GameDomainRepositoryPort gameDomainRepository;
    @Mock private GameParticipantService gameParticipantService;
    @Mock private GameParticipantRepositoryPort gameParticipantRepository;

    @InjectMocks private UserDeletionService service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("player1");
        user.setEmail("player1@test.com");
        user.setPassword("hashed");
    }
}
```

**Stub softDelete** : `doNothing().when(userRepository).softDelete(userId);`

### Gestion GDPR (NFR-S04)

La soft-delete conserve les données en base pour l'intégrité référentielle mais masque le compte.
Pour ce MVP : soft-delete = "suppression logique". Les champs PII (username, email) restent en base mais le compte est inaccessible. Un process d'anonymisation physique peut être ajouté en story future si requis.

### Project Structure Notes

- Nouveau fichier exception: `src/main/java/com/fortnite/pronos/exception/AccountDeletionBlockedException.java`
- Nouveau service: `src/main/java/com/fortnite/pronos/service/UserDeletionService.java`
- Nouveau controller: `src/main/java/com/fortnite/pronos/controller/AccountController.java`
- Nouveau test: `src/test/java/com/fortnite/pronos/service/UserDeletionServiceTest.java`
- Migration: `src/main/resources/db/migration/V37__add_user_soft_delete.sql`
- Modifications: `User.java`, `UserRepositoryPort.java`, `UserRepository.java`, `GameDomainRepositoryPort.java`, `GameRepositoryAdapter.java`, `GameRepository.java` (JPA), `GameParticipantRepositoryPort.java`, `DomainExceptionHandler.java`

### References

- `User.java` JPA model: `src/main/java/com/fortnite/pronos/model/User.java` (pas de deletedAt actuellement)
- `UserRepositoryPort.java`: `src/main/java/com/fortnite/pronos/domain/port/out/UserRepositoryPort.java`
- `UserRepository.java`: `src/main/java/com/fortnite/pronos/repository/UserRepository.java`
- `GameParticipantService.leaveGame()`: `src/main/java/com/fortnite/pronos/service/game/GameParticipantService.java` L.59-69
- `UserResolver.resolve()`: `src/main/java/com/fortnite/pronos/service/UserResolver.java`
- `DomainExceptionHandler`: `src/main/java/com/fortnite/pronos/config/DomainExceptionHandler.java`
- Pattern soft-delete games: `src/main/resources/db/migration/V30__add_soft_delete_to_games.sql`
- Architecture hexagonale: `_bmad-output/planning-artifacts/architecture.md`
- GameStatus enum: `src/main/java/com/fortnite/pronos/domain/game/model/GameStatus.java`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- Flyway `V37__add_user_soft_delete.sql` — ajout colonne `deleted_at TIMESTAMP` + index sur `users`.
- `User.java` — champ `LocalDateTime deletedAt` ajouté avec `@Column(name = "deleted_at")`.
- `UserRepositoryPort.softDelete(UUID, LocalDateTime)` ajouté + implémenté via `@Modifying @Query` dans `UserRepository`.
- `AccountDeletionBlockedException` créée dans `exception/` + handler 409 dans `DomainExceptionHandler`.
- `UserDeletionService` — 3 deps (UserRepositoryPort, GameDomainRepositoryPort, GameParticipantService). Réutilise `findByCreatorId` et `findGamesByUserId` existants, filtre par status CREATING/DRAFTING/ACTIVE, bloque si créateur actif, libère participations non-créateur puis soft-delete.
- `AccountController` — `DELETE /api/account` → 204 No Content, 401 si non authentifié.
- 9 nouveaux tests dans `UserDeletionServiceTest` — 4 nominaux + 5 edge cases. Tous verts.
- Suite complète : 2061 run, 26 failures, 1 error (baseline pré-existante identique). Zéro régression.

### File List

- `src/main/resources/db/migration/V37__add_user_soft_delete.sql` (NEW)
- `src/main/java/com/fortnite/pronos/model/User.java` (MODIFIED — +deletedAt field)
- `src/main/java/com/fortnite/pronos/domain/port/out/UserRepositoryPort.java` (MODIFIED — +softDelete)
- `src/main/java/com/fortnite/pronos/repository/UserRepository.java` (MODIFIED — +softDeleteById query)
- `src/main/java/com/fortnite/pronos/exception/AccountDeletionBlockedException.java` (NEW)
- `src/main/java/com/fortnite/pronos/config/DomainExceptionHandler.java` (MODIFIED — +AccountDeletionBlocked handler)
- `src/main/java/com/fortnite/pronos/service/UserDeletionService.java` (NEW)
- `src/main/java/com/fortnite/pronos/controller/AccountController.java` (NEW)
- `src/test/java/com/fortnite/pronos/service/UserDeletionServiceTest.java` (NEW — 9 tests)
