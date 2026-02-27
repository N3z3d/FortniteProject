# Story 3.1: Création de partie paramétrée

Status: done

## Story

As a game creator,
I want configurer région, taille équipe, tranche, période de compétition et mode de draft lors de la création d'une partie,
so that la partie correspond exactement à mon format de compétition.

## Acceptance Criteria

1. **Given** un utilisateur authentifié, **When** il soumet `POST /api/games` avec tous les paramètres requis (name, maxParticipants, teamSize, trancheSize, tranchesEnabled, competitionStart, competitionEnd, draftMode, regionRules), **Then** la partie est créée avec statut CREATING et tous les champs persistés.

2. **Given** un utilisateur authentifié, **When** il crée une partie avec `tranchesEnabled=false`, **Then** `trancheSize` est ignoré et le mode choix libre est actif (aucun plancher de rang ne sera appliqué en draft).

3. **Given** une requête de création invalide (teamSize ≤ 0, trancheSize ≤ 0, competitionEnd avant competitionStart, draftMode absent), **When** la requête est soumise, **Then** le backend répond 400 avec un message d'erreur explicite — aucun état invalide n'est persisté.

4. **Given** une partie créée, **When** on relit la partie via `GET /api/games/{id}`, **Then** tous les champs de configuration (teamSize, trancheSize, tranchesEnabled, competitionStart, competitionEnd, draftMode) sont présents dans la réponse.

5. **Given** un utilisateur non authentifié, **When** il appelle `POST /api/games`, **Then** il reçoit 401.

## Tasks / Subtasks

- [ ] Task 1: Créer enum `DraftMode` dans le domaine (AC: #1, #2)
  - [ ] `src/main/java/com/fortnite/pronos/domain/game/model/DraftMode.java`
  - [ ] Valeurs: `SNAKE`, `SIMULTANEOUS`

- [ ] Task 2: Enrichir `domain/game/model/Game.java` avec les nouveaux champs (AC: #1, #2, #3)
  - [ ] Ajouter: `DraftMode draftMode`, `int teamSize`, `int trancheSize`, `boolean tranchesEnabled`, `LocalDate competitionStart`, `LocalDate competitionEnd`
  - [ ] Ajouter validation dans le constructeur business: teamSize ≥ 1, trancheSize ≥ 1 si tranchesEnabled, draftMode non null
  - [ ] Ajouter les setters/getters correspondants
  - [ ] Ajouter `restore()` : mettre à jour la signature avec les 6 nouveaux paramètres

- [ ] Task 3: Mettre à jour `CreateGameRequest` (AC: #1, #2, #3)
  - [ ] Ajouter `@NotNull DraftMode draftMode`
  - [ ] Ajouter `@Min(1) @Max(50) int teamSize` (default 5)
  - [ ] Ajouter `@Min(1) @Max(200) int trancheSize` (default 10)
  - [ ] Ajouter `boolean tranchesEnabled` (default true)
  - [ ] Ajouter `LocalDate competitionStart` (nullable)
  - [ ] Ajouter `LocalDate competitionEnd` (nullable)
  - [ ] Validation cross-field: si les deux présentes, competitionEnd >= competitionStart

- [ ] Task 4: Mettre à jour `GameCreationService.buildDomainGame()` (AC: #1, #2)
  - [ ] Mapper les 6 nouveaux champs de `CreateGameRequest` vers `domain.game.model.Game`
  - [ ] Si `tranchesEnabled=false` → `trancheSize` peut rester à sa valeur par défaut (ignoré en draft)

- [ ] Task 5: Mettre à jour l'entité JPA `model/Game.java` (AC: #4)
  - [ ] Ajouter les 6 colonnes JPA avec `@Column`
  - [ ] `draftMode` : `@Enumerated(EnumType.STRING)`, nullable=false, default "SNAKE"
  - [ ] `teamSize` : nullable=false, default 5
  - [ ] `trancheSize` : nullable=false, default 10
  - [ ] `tranchesEnabled` : nullable=false, default true
  - [ ] `competitionStart`, `competitionEnd` : `@Column(name="competition_start/end")`, nullable=true

- [ ] Task 6: Mettre à jour `GameEntityMapper` (AC: #4)
  - [ ] `toEntity()` : mapper les 6 nouveaux champs domaine → JPA
  - [ ] `toDomain()` : mapper les 6 nouveaux champs JPA → domaine
  - [ ] Gérer null safety pour `competitionStart`/`competitionEnd`

- [ ] Task 7: Mettre à jour `GameDtoMapper` (AC: #4)
  - [ ] `fromDomainGame()` : inclure les 6 nouveaux champs dans `GameDto`
  - [ ] Ajouter les champs correspondants dans `GameDto`

- [ ] Task 8: Migration Flyway V36 (AC: #1, #4)
  - [ ] `src/main/resources/db/migration/V36__add_game_draft_configuration.sql`
  - [ ] ALTER TABLE games ADD COLUMN: `draft_mode VARCHAR(20) NOT NULL DEFAULT 'SNAKE'`, `team_size INTEGER NOT NULL DEFAULT 5`, `tranche_size INTEGER NOT NULL DEFAULT 10`, `tranches_enabled BOOLEAN NOT NULL DEFAULT true`, `competition_start DATE`, `competition_end DATE`

- [ ] Task 9: Tests `GameCreationServiceTest` (AC: #1, #2, #3)
  - [ ] Nominal SNAKE: partie créée avec tous les champs
  - [ ] Nominal SIMULTANEOUS: draftMode=SIMULTANEOUS persisté
  - [ ] Mode sans tranches: tranchesEnabled=false
  - [ ] Edge case: teamSize=0 → exception
  - [ ] Edge case: trancheSize=0 avec tranchesEnabled=true → exception
  - [ ] Edge case: competitionEnd < competitionStart → 400

## Dev Notes

### Contexte brownfield — DÉCISION ARCHITECTURE CRITIQUE

`GameCreationService` utilise **déjà** `domain.game.model.Game` via `GameDomainRepositoryPort`. Le brownfield est résolu pour la création.

**Règle absolue :** Toute nouvelle story Epic 3+ utilise UNIQUEMENT `domain.game.model.*` et JAMAIS `model.Game` (JPA legacy). Le mapper `GameEntityMapper` fait le pont entre les deux.

Ne pas confondre les deux classes :
- ✅ `com.fortnite.pronos.domain.game.model.Game` — domaine pur, SOLID, à utiliser dans les services
- ❌ `com.fortnite.pronos.model.Game` — entité JPA legacy, uniquement dans `adapter/out/persistence/` et `model/`

### Fichiers existants à modifier

- `src/main/java/com/fortnite/pronos/domain/game/model/Game.java` — ajouter 6 champs + validation
- `src/main/java/com/fortnite/pronos/dto/CreateGameRequest.java` — ajouter 6 champs (Lombok `@Data`)
- `src/main/java/com/fortnite/pronos/service/game/GameCreationService.java` — méthode `buildDomainGame()` (lignes 157-163)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/game/GameEntityMapper.java` — `toEntity()`/`toDomain()`
- `src/main/java/com/fortnite/pronos/dto/mapper/GameDtoMapper.java` — `fromDomainGame()`
- `src/main/java/com/fortnite/pronos/dto/GameDto.java` — ajouter les champs

### Nouveau fichier à créer

```
src/main/java/com/fortnite/pronos/domain/game/model/DraftMode.java
```
```java
package com.fortnite.pronos.domain.game.model;

public enum DraftMode {
  SNAKE,
  SIMULTANEOUS
}
```
Conforme `DomainIsolationTest` : enum pur, 0 dépendance JPA/Spring.

### Enrichissement de `domain/game/model/Game.java`

Le constructeur business devient (les 3 paramètres existants + 4 obligatoires nouveaux) :
```java
public Game(String name, UUID creatorId, int maxParticipants,
            DraftMode draftMode, int teamSize, int trancheSize, boolean tranchesEnabled) {
    validateCreation(name, creatorId, maxParticipants, draftMode, teamSize, trancheSize, tranchesEnabled);
    // ... existing setup ...
    this.draftMode = draftMode;
    this.teamSize = teamSize;
    this.trancheSize = trancheSize;
    this.tranchesEnabled = tranchesEnabled;
    // competitionStart/End sont nullables (configurables en Story 6.1)
}
```

Validation règles :
```java
if (draftMode == null) throw new IllegalArgumentException("Draft mode cannot be null");
if (teamSize < 1) throw new IllegalArgumentException("Team size must be >= 1");
if (tranchesEnabled && trancheSize < 1) throw new IllegalArgumentException("Tranche size must be >= 1 when tranches are enabled");
```

⚠️ **Self-invocation AOP** : Pas de `@Cacheable` sur ces méthodes — pas de risque.

⚠️ **Lombok rewrite** : `domain/game/model/Game.java` est un modèle pur SANS Lombok. Ne pas ajouter `@Data`/`@Builder`. Ajouter manuellement getters + setters.

### Enrichissement `buildDomainGame()` dans `GameCreationService`

```java
private Game buildDomainGame(com.fortnite.pronos.model.User creator, CreateGameRequest request) {
    Game game = new Game(
        request.getName(),
        creator.getId(),
        request.getMaxParticipants(),
        request.getDraftMode(),
        request.getTeamSize(),
        request.getTrancheSize(),
        request.isTraanchesEnabled()
    );
    if (request.getDescription() != null) {
        game.setDescription(request.getDescription());
    }
    if (request.getCompetitionStart() != null) {
        game.setCompetitionStart(request.getCompetitionStart());
    }
    if (request.getCompetitionEnd() != null) {
        game.setCompetitionEnd(request.getCompetitionEnd());
    }
    return game;
}
```

### Migration Flyway V36

Fichier : `src/main/resources/db/migration/V36__add_game_draft_configuration.sql`
```sql
ALTER TABLE games
  ADD COLUMN IF NOT EXISTS draft_mode       VARCHAR(20)  NOT NULL DEFAULT 'SNAKE',
  ADD COLUMN IF NOT EXISTS team_size        INTEGER      NOT NULL DEFAULT 5,
  ADD COLUMN IF NOT EXISTS tranche_size     INTEGER      NOT NULL DEFAULT 10,
  ADD COLUMN IF NOT EXISTS tranches_enabled BOOLEAN      NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS competition_start DATE,
  ADD COLUMN IF NOT EXISTS competition_end   DATE;
```

### Pattern `restore()` dans `Game`

Ajouter les 6 paramètres à la signature statique `restore()` (à la fin, pour conserver la compatibilité des appels existants dans `GameEntityMapper`). Mettre à jour `GameEntityMapper.toDomain()` en conséquence.

### Tests — stratégie

Pattern établi en Epics 1-2 :
- `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@BeforeEach` — test unitaire Mockito
- `mvn spotless:apply -q --no-transfer-progress && mvn test` — toujours avant de committer
- Linter peut réécrire les fichiers — relire avant édition
- Pas de `@SpringBootTest` — test unitaire pur pour `GameCreationServiceTest`

Mock `GameDomainRepositoryPort.save(Game)` avec `when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0))`.

### CouplingTest

`GameCreationService` a actuellement 4 dépendances : `GameDomainRepositoryPort`, `UserRepositoryPort`, `ValidationService`, `InvitationCodeService` — bien sous la limite de 7.

### NamingConventionTest

`DraftMode.java` est un enum dans `domain/game/model/` — pas de contrainte "finit par Service".

### Validation cross-field competitionStart/competitionEnd

À ajouter dans `ValidationService.validateCreateGameRequest()` (ou dans un validateur Bean Validation personnalisé). Recommandé : `ValidationService` pour cohérence avec le pattern existant.

### `GameDto` — champs à ajouter

```java
private DraftMode draftMode;
private int teamSize;
private int trancheSize;
private boolean tranchesEnabled;
private LocalDate competitionStart;
private LocalDate competitionEnd;
```

### Références

- FR-15: Création de partie configurée — [Source: epics.md#Story 3.1]
- FR-16: Mode sans tranches — [Source: epics.md#Story 3.1]
- NFR-M01: Architecture hexagonale obligatoire — [Source: epics.md#NonFunctional Requirements]
- NFR-M02: Max 500 lignes/classe, 50 lignes/méthode — [Source: epics.md#NonFunctional Requirements]
- `domain/game/model/Game.java` (domaine pur — à enrichir)
- `service/game/GameCreationService.java` (service hexagonal — utilise déjà le domaine)
- `adapter/out/persistence/game/GameEntityMapper.java` (mapper JPA ↔ domaine)
- `dto/CreateGameRequest.java` (DTO entrée — Lombok @Data)
- `dto/mapper/GameDtoMapper.java` (mapper domaine → DTO sortie)
- `dto/GameDto.java` (DTO sortie)
- `model/Game.java` (JPA entity legacy — à mettre à jour pour les colonnes)
- V35 (dernier Flyway) → V36 (nouveau)

### Project Structure Notes

```
src/main/java/com/fortnite/pronos/
  domain/game/model/
    DraftMode.java              ← NEW
    Game.java                   ← MODIFY (+6 champs, +validation)
  dto/
    CreateGameRequest.java      ← MODIFY (+6 champs)
    GameDto.java                ← MODIFY (+6 champs)
    mapper/
      GameDtoMapper.java        ← MODIFY
  service/game/
    GameCreationService.java    ← MODIFY (buildDomainGame)
  adapter/out/persistence/game/
    GameEntityMapper.java       ← MODIFY (toEntity/toDomain)
  model/
    Game.java                   ← MODIFY (+6 colonnes JPA)
src/main/resources/db/migration/
    V36__add_game_draft_configuration.sql  ← NEW

src/test/java/com/fortnite/pronos/service/game/
    GameCreationServiceTest.java  ← NEW (ou MODIFY si existant)
```

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List
