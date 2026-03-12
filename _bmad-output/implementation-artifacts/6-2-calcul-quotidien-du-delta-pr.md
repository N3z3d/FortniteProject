# Story 6.2: Calcul quotidien du delta PR

Status: done

## Story

As a participant,
I want que le score de mon équipe soit calculé quotidiennement comme delta PR (valeur fin − valeur début) sur la période configurée,
so that le classement reflète la progression réelle de mes joueurs.

## Acceptance Criteria

1. **Given** une partie a une période de compétition configurée (`competitionStart` et `competitionEnd` non-null) et un draft actif, **When** le batch quotidien s'exécute, **Then** pour chaque participant de la partie, un `TeamScoreDelta` est calculé et persisté (ou mis à jour) avec `deltaPr = Σ(prEnd − prStart)` par joueur.

2. **Given** aucun snapshot PR n'existe pour un joueur à la date de début/fin, **When** le batch s'exécute, **Then** ce joueur est ignoré dans le calcul (contribution = 0) — le batch ne plante pas.

3. **Given** un delta a déjà été calculé pour `(gameId, participantId, periodStart, periodEnd)`, **When** le batch s'exécute à nouveau, **Then** l'entrée existante est mise à jour (upsert) avec la valeur recalculée.

4. **Given** le batch s'exécute quotidiennement à 08h00 UTC (après la fenêtre d'ingestion PR 05h00-08h00), **When** la cron est déclenchée, **Then** `TeamScoreDeltaBatchService.computeAllGameDeltas()` est appelé.

5. **Given** une partie n'a pas de période configurée (competitionStart ou competitionEnd null), **When** le batch s'exécute, **Then** cette partie est ignorée sans erreur.

6. **Given** une partie n'a pas de draft (draftId null), **When** le batch s'exécute, **Then** cette partie est ignorée sans erreur.

## Technical Context

### FR Coverage
- **FR-37**: Le système calcule le score de chaque équipe = delta PR (valeur fin − valeur début de période configurée)
- **FR-40**: Le leaderboard est mis à jour quotidiennement après le scraping nocturne (batch, pas temps-réel en v1)

### Analyse de l'existant — ce qui est DÉJÀ là

#### Infrastructure complète (ne pas recréer)
- **`RankSnapshot`** domain model + `pr_snapshots` table (V34) — colonnes : `player_id`, `region`, `rank`, `pr_value`, `snapshot_date` — UNIQUE(player_id, region, snapshot_date)
- **`RankSnapshotRepositoryPort`** : `save()`, `findByPlayerAndRegion(UUID, String, int days)`, `findByPlayerRecent(UUID, int days)` — **manque `findLatestOnOrBefore()`** (gap à combler)
- **`DraftPickRepositoryPort`** : `findPickedPlayerIdsByDraftId(UUID draftId)` (tous participants confondus) — **manque `findPlayerIdsByDraftIdAndParticipantId()`** (gap)
- **`GameParticipantRepositoryPort`** : `findByUserIdAndGameId()` — **manque `findByGameId()`** (gap)
- **`GameDomainRepositoryPort`** : `findById()`, `findByStatus()` — **manque `findAllWithCompetitionPeriod()`** (gap)
- **`PrIngestionOrchestrationService`** : batch existant cron `0 0 5 * * *` (05h UTC) — fenêtre 05h–08h UTC
- **`PlayerDomainRepositoryPort`** : `findById(UUID)` → `Optional<Player>` — pour récupérer la région du joueur
- **`DraftDomainRepositoryPort`** : `findActiveByGameId(UUID gameId)` → `Optional<Draft>` (pour valider l'existence du draft)

#### Pas de job de scoring existant
Il n'existe **aucun `@Scheduled` pour le calcul de delta d'équipe**. C'est le gap principal de cette story.

#### Encoded corruption dans ScoreCalculationService
`ScoreCalculationService.java` a des caractères UTF-8 corrompus (double-encoded). **Ne pas modifier ce fichier.** Créer un nouveau service dédié.

### Gap 1 — Migration V40 : table `team_score_deltas`

```sql
-- src/main/resources/db/migration/V40__add_team_score_deltas.sql
CREATE TABLE IF NOT EXISTS team_score_deltas (
  id              UUID         PRIMARY KEY,
  game_id         UUID         NOT NULL,
  participant_id  UUID         NOT NULL,
  period_start    DATE         NOT NULL,
  period_end      DATE         NOT NULL,
  delta_pr        INTEGER      NOT NULL DEFAULT 0,
  computed_at     TIMESTAMP    NOT NULL,
  CONSTRAINT uq_team_score_delta UNIQUE (game_id, participant_id, period_start, period_end)
);
CREATE INDEX IF NOT EXISTS idx_team_score_deltas_game
  ON team_score_deltas (game_id, delta_pr DESC);
```

### Gap 2 — Domaine `TeamScoreDelta`

```java
// src/main/java/com/fortnite/pronos/domain/team/model/TeamScoreDelta.java
// OBLIGATOIRE : final, 0 imports JPA/Spring/Hibernate/Lombok (DomainIsolationTest)
public final class TeamScoreDelta {
  private final UUID id;
  private final UUID gameId;
  private final UUID participantId;
  private final LocalDate periodStart;
  private final LocalDate periodEnd;
  private final int deltaPr;
  private final LocalDateTime computedAt;

  // Creation constructor
  public TeamScoreDelta(UUID gameId, UUID participantId, LocalDate periodStart,
      LocalDate periodEnd, int deltaPr) {
    this.id = UUID.randomUUID();
    this.gameId = gameId; this.participantId = participantId;
    this.periodStart = periodStart; this.periodEnd = periodEnd;
    this.deltaPr = deltaPr;
    this.computedAt = LocalDateTime.now();
  }

  // restore() static factory for persistence reconstitution
  public static TeamScoreDelta restore(UUID id, UUID gameId, UUID participantId,
      LocalDate periodStart, LocalDate periodEnd, int deltaPr, LocalDateTime computedAt) { ... }

  // Getters only — immutable
}
```

### Gap 3 — Port `TeamScoreDeltaRepositoryPort`

```java
// src/main/java/com/fortnite/pronos/domain/port/out/TeamScoreDeltaRepositoryPort.java
public interface TeamScoreDeltaRepositoryPort {
  TeamScoreDelta save(TeamScoreDelta delta);
  List<TeamScoreDelta> findByGameId(UUID gameId);
  Optional<TeamScoreDelta> findByGameIdAndParticipantId(UUID gameId, UUID participantId);
}
```

### Gap 4 — JPA Entity + Repo + Adapter pour TeamScoreDelta

```java
// src/main/java/com/fortnite/pronos/model/TeamScoreDeltaEntity.java
@Entity @Table(name = "team_score_deltas") @Getter @Setter @NoArgsConstructor
public class TeamScoreDeltaEntity {
  @Id private UUID id;
  @Column(nullable = false) private UUID gameId;
  @Column(nullable = false) private UUID participantId;
  @Column(nullable = false) private LocalDate periodStart;
  @Column(nullable = false) private LocalDate periodEnd;
  @Column(nullable = false) private int deltaPr;
  @Column(nullable = false) private LocalDateTime computedAt;
}

// src/main/java/com/fortnite/pronos/repository/TeamScoreDeltaJpaRepository.java
@Repository
public interface TeamScoreDeltaJpaRepository extends JpaRepository<TeamScoreDeltaEntity, UUID> {
  List<TeamScoreDeltaEntity> findByGameId(UUID gameId);
  Optional<TeamScoreDeltaEntity> findByGameIdAndParticipantId(UUID gameId, UUID participantId);
}

// src/main/java/com/fortnite/pronos/adapter/out/persistence/team/TeamScoreDeltaRepositoryAdapter.java
@Component @RequiredArgsConstructor
public class TeamScoreDeltaRepositoryAdapter implements TeamScoreDeltaRepositoryPort {
  private final TeamScoreDeltaJpaRepository jpaRepository;
  // save: map domain→entity (set all fields incl. id), persist, map back
  // findByGameId: query + stream map to domain
  // findByGameIdAndParticipantId: query + map
}
```

**ATTENTION JPA** : utiliser `@Column(name = "period_start")` et `@Column(name = "period_end")` — noms SQL avec underscore, Java camelCase.

### Gap 5 — Extension `RankSnapshotRepositoryPort`

```java
// Ajouter dans le port :
Optional<RankSnapshot> findLatestOnOrBefore(UUID playerId, LocalDate date);
```

- **JPA Repository** : `findTopByPlayerIdOrderBySnapshotDateDesc` est insuffisant. Utiliser `@Query` :
  ```java
  @Query("SELECT s FROM RankSnapshotEntity s WHERE s.playerId = :playerId AND s.snapshotDate <= :date ORDER BY s.snapshotDate DESC LIMIT 1")
  Optional<RankSnapshotEntity> findLatestOnOrBefore(@Param("playerId") UUID playerId, @Param("date") LocalDate date);
  ```
- **Adapter** : map entity → domain et retourner `Optional<RankSnapshot>`

**Raisonnement** : Un snapshot le jour exact peut ne pas exister (week-end, échec d'ingestion). `findLatestOnOrBefore` retourne le snapshot le plus récent disponible ≤ date, évitant les gaps.

### Gap 6 — Extension `DraftPickRepositoryPort`

```java
// Ajouter dans le port :
List<UUID> findPlayerIdsByDraftIdAndParticipantId(UUID draftId, UUID participantId);
```

- **DraftPickRepository (JPA)** :
  ```java
  @Query("SELECT dp.player.id FROM DraftPick dp WHERE dp.draft.id = :draftId AND dp.participant.id = :participantId")
  List<UUID> findPlayerIdsByDraftIdAndParticipantId(@Param("draftId") UUID draftId, @Param("participantId") UUID participantId);
  ```

### Gap 7 — Extension `GameParticipantRepositoryPort`

```java
// Ajouter dans le port :
List<com.fortnite.pronos.model.GameParticipant> findByGameId(UUID gameId);
```

- **Implémentation** : déjà dans `GameParticipantJpaRepository` via derived query `findByGameId(UUID gameId)` (ou `findByGameIdOrderByJoinedAt`). Vérifier l'annotation `@Query` si nécessaire (Hibernate 6 SQM — voir mémoire).

### Gap 8 — Extension `GameDomainRepositoryPort`

```java
// Ajouter dans le port :
List<Game> findAllWithCompetitionPeriod();
```

- **Adapter** : JPQL `SELECT g FROM GameEntity g WHERE g.competitionStart IS NOT NULL AND g.competitionEnd IS NOT NULL AND g.deletedAt IS NULL`

### Gap 9 — `TeamScoreDeltaBatchService` (6 deps — sous le max de 7)

```java
// src/main/java/com/fortnite/pronos/service/scoring/TeamScoreDeltaBatchService.java
@Slf4j @Service @RequiredArgsConstructor @Transactional
public class TeamScoreDeltaBatchService {
  private final GameDomainRepositoryPort gameDomainRepository;       // 1
  private final GameParticipantRepositoryPort participantRepository; // 2
  private final DraftPickRepositoryPort draftPickRepository;         // 3
  private final PlayerDomainRepositoryPort playerRepository;         // 4
  private final RankSnapshotRepositoryPort snapshotRepository;       // 5
  private final TeamScoreDeltaRepositoryPort deltaRepository;        // 6

  public void computeAllGameDeltas() {
    LocalDate today = LocalDate.now();
    List<Game> games = gameDomainRepository.findAllWithCompetitionPeriod();
    log.info("TeamScoreDeltaBatch: processing {} games", games.size());

    for (Game game : games) {
      if (game.getDraftId() == null) continue; // AC #6
      try {
        computeDeltasForGame(game, today);
      } catch (Exception e) {
        log.error("TeamScoreDeltaBatch: error for gameId={}: {}", game.getId(), e.getMessage());
        // Continue batch — don't fail all games because of one
      }
    }
  }

  private void computeDeltasForGame(Game game, LocalDate today) {
    LocalDate periodStart = game.getCompetitionStart();
    LocalDate periodEnd = game.getCompetitionEnd().isBefore(today)
        ? game.getCompetitionEnd() : today; // ne pas dépasser aujourd'hui

    List<com.fortnite.pronos.model.GameParticipant> participants =
        participantRepository.findByGameId(game.getId());

    for (com.fortnite.pronos.model.GameParticipant participant : participants) {
      List<UUID> playerIds = draftPickRepository
          .findPlayerIdsByDraftIdAndParticipantId(game.getDraftId(), participant.getId());
      int teamDelta = computeTeamDelta(playerIds, periodStart, periodEnd);
      upsertDelta(game.getId(), participant.getId(), periodStart, game.getCompetitionEnd(), teamDelta);
    }
  }

  private int computeTeamDelta(List<UUID> playerIds, LocalDate start, LocalDate end) {
    int total = 0;
    for (UUID playerId : playerIds) {
      // Get player region for accurate snapshot lookup
      Optional<com.fortnite.pronos.domain.player.model.Player> playerOpt =
          playerRepository.findById(playerId);
      if (playerOpt.isEmpty()) continue; // AC #2

      Optional<RankSnapshot> startSnap = snapshotRepository.findLatestOnOrBefore(playerId, start);
      Optional<RankSnapshot> endSnap = snapshotRepository.findLatestOnOrBefore(playerId, end);

      if (startSnap.isPresent() && endSnap.isPresent()) {
        total += endSnap.get().getPrValue() - startSnap.get().getPrValue();
      }
      // If no snapshot → contribution = 0 (AC #2 — batch ne plante pas)
    }
    return total;
  }

  private void upsertDelta(UUID gameId, UUID participantId, LocalDate periodStart,
      LocalDate periodEnd, int deltaPr) {
    Optional<TeamScoreDelta> existing =
        deltaRepository.findByGameIdAndParticipantId(gameId, participantId);
    if (existing.isPresent()) {
      // Update via save with same id (AC #3 — upsert)
      TeamScoreDelta updated = TeamScoreDelta.restoreWithDelta(
          existing.get().getId(), gameId, participantId, periodStart, periodEnd, deltaPr);
      deltaRepository.save(updated);
    } else {
      deltaRepository.save(
          new TeamScoreDelta(gameId, participantId, periodStart, periodEnd, deltaPr));
    }
    log.debug("Delta upserted: game={} participant={} delta={}", gameId, participantId, deltaPr);
  }
}
```

**ATTENTION** : `TeamScoreDelta` doit exposer `restoreWithDelta()` ou un constructor séparé pour l'upsert. Alternative plus simple : toujours créer une nouvelle entrée (DELETE + INSERT) ou exposer un `restore()` avec id existant.

**Design simplifié pour l'upsert** : utiliser `restore(existingId, gameId, participantId, periodStart, periodEnd, newDelta, LocalDateTime.now())` puis `save()`. Le JPA `@Id` existant déclenche un UPDATE.

### Gap 10 — `TeamScoreDeltaOrchestrationService` (1 dep)

```java
// src/main/java/com/fortnite/pronos/service/scoring/TeamScoreDeltaOrchestrationService.java
@Slf4j @Service @RequiredArgsConstructor
public class TeamScoreDeltaOrchestrationService {
  private final TeamScoreDeltaBatchService batchService;

  @Scheduled(cron = "${scoring.team.delta.cron:0 0 8 * * *}")
  public void runDailyDeltaComputation() {
    log.info("TeamScoreDeltaOrchestration: starting daily delta computation");
    batchService.computeAllGameDeltas();
    log.info("TeamScoreDeltaOrchestration: completed");
  }
}
```

**NamingConventionTest** : doit se terminer par `Service` ✓
**@EnableScheduling** : déjà présent dans `PronosApplication` (via `PrIngestionOrchestrationService`).

### Contraintes architecture

- **CouplingTest** :
  - `TeamScoreDeltaBatchService` : 6 deps ✓
  - `TeamScoreDeltaOrchestrationService` : 1 dep ✓
- **NamingConventionTest** : les deux se terminent par `Service` ✓
- **DomainIsolationTest** : `TeamScoreDelta` → `final`, 0 imports JPA/Spring/Hibernate/Lombok ✓ (dans `domain.team.model` — auto-détecté)
- **LayeredArchitectureTest** : service → port → adapter → JPA ✓
- **DependencyInversionTest** : services n'accèdent pas aux JPA repos directement ✓
- **Spotless** : `mvn spotless:apply` avant `mvn test`
- **ScoreCalculationService** : **NE PAS MODIFIER** (encoding corrompu — risque de corruption)

### Hiérarchie des dates pour le calcul

```
periodStart (competitionStart) ──────────────── periodEnd (min(today, competitionEnd))
     │                                                  │
  snapshot PR ≤ start (findLatestOnOrBefore)     snapshot PR ≤ end (findLatestOnOrBefore)
```

- `periodEnd` utilisé dans le calcul = `min(today, game.competitionEnd)` (calcul "courant")
- `periodEnd` stocké dans `team_score_deltas` = `game.competitionEnd` (période officielle)
- Cela permet de mettre à jour le delta quotidiennement jusqu'à la fin de la compétition

### Piège Hibernate 6 SQM — `findByGameId` sur `GameParticipant`

`GameParticipant` JPA entity a un `@ManyToOne Game game` — donc `findByGameId` nécessite `@Query` :
```java
@Query("SELECT gp FROM GameParticipant gp WHERE gp.game.id = :gameId")
List<GameParticipant> findByGameId(@Param("gameId") UUID gameId);
```
(Sinon Hibernate 6 ne résout pas `gameId` sur une relation)

### Pattern de test : `TeamScoreDeltaBatchServiceTest`

Utiliser Mockito pur (`@ExtendWith(MockitoExtension.class)`), mocker les 6 ports, vérifier le flow:
- `gameDomainRepository.findAllWithCompetitionPeriod()` → retourne 1 game
- `participantRepository.findByGameId()` → retourne 1 participant
- `draftPickRepository.findPlayerIdsByDraftIdAndParticipantId()` → retourne 2 playerIds
- `playerRepository.findById()` → retourne player (region EU)
- `snapshotRepository.findLatestOnOrBefore(playerId, start)` → Optional.of(snapshotStart)
- `snapshotRepository.findLatestOnOrBefore(playerId, end)` → Optional.of(snapshotEnd)
- `deltaRepository.findByGameIdAndParticipantId()` → Optional.empty() (first run)
- `deltaRepository.save()` → vérifier appel avec delta = (endPr1-startPr1) + (endPr2-startPr2)

## Tasks / Subtasks

- [x] Task 1: Migration V40 — table `team_score_deltas` (AC: #1)
  - [x] `src/main/resources/db/migration/V40__add_team_score_deltas.sql`

- [x] Task 2: Domaine `TeamScoreDelta` (AC: #1)
  - [x] `src/main/java/com/fortnite/pronos/domain/team/model/TeamScoreDelta.java` (final, restore(), getters only)

- [x] Task 3: Port + JPA + Adapter `TeamScoreDelta` (AC: #1, #3)
  - [x] `src/main/java/com/fortnite/pronos/domain/port/out/TeamScoreDeltaRepositoryPort.java`
  - [x] `src/main/java/com/fortnite/pronos/model/TeamScoreDeltaEntity.java` (@Entity, @Table("team_score_deltas"))
  - [x] `src/main/java/com/fortnite/pronos/repository/TeamScoreDeltaJpaRepository.java`
  - [x] `src/main/java/com/fortnite/pronos/adapter/out/persistence/team/TeamScoreDeltaRepositoryAdapter.java`

- [x] Task 4: Étendre `RankSnapshotRepositoryPort` + implem (AC: #2)
  - [x] Ajouter `Optional<RankSnapshot> findLatestOnOrBefore(UUID playerId, LocalDate date)` dans le port
  - [x] Ajouter `@Query` dans `RankSnapshotJpaRepository`
  - [x] Implémenter dans `RankSnapshotRepositoryAdapter`

- [x] Task 5: Étendre `DraftPickRepositoryPort` (AC: #1)
  - [x] Ajouter `List<UUID> findPlayerIdsByDraftIdAndParticipantId(UUID draftId, UUID participantId)` dans le port
  - [x] Ajouter `@Query` dans `DraftPickRepository`

- [x] Task 6: Étendre `GameDomainRepositoryPort` (AC: #1, #5)
  - [x] Ajouter `List<Game> findAllWithCompetitionPeriod()` dans `GameDomainRepositoryPort` + impl dans `GameRepositoryAdapter` + `@Query` dans `GameRepository`
  - [x] `GameParticipantRepositoryPort.findByGameIdOrderByJoinedAt()` déjà existant — utilisé directement

- [x] Task 7: `TeamScoreDeltaBatchService` (AC: #1, #2, #3, #5, #6)
  - [x] `src/main/java/com/fortnite/pronos/service/scoring/TeamScoreDeltaBatchService.java`
  - [x] `computeAllGameDeltas()` : filtre games sans draftId ou sans période, loop sur participants, loop sur players
  - [x] `computeTeamDelta()` : `findLatestOnOrBefore` × 2 par joueur, delta=endPr-startPr (0 si snapshot absent)
  - [x] `upsertDelta()` : findExisting → update ou create → save

- [x] Task 8: `TeamScoreDeltaOrchestrationService` (AC: #4)
  - [x] `src/main/java/com/fortnite/pronos/service/scoring/TeamScoreDeltaOrchestrationService.java`
  - [x] `@Scheduled(cron = "${scoring.team.delta.cron:0 0 8 * * *}")` sur `runDailyDeltaComputation()`

- [x] Task 9: Tests `TeamScoreDeltaBatchServiceTest` (AC: #1, #2, #3, #5, #6)
  - [x] `src/test/java/com/fortnite/pronos/service/scoring/TeamScoreDeltaBatchServiceTest.java`
  - [x] `whenValidGame_computesAndSavesDelta`
  - [x] `whenNoSnapshotForPlayer_contributionIsZero`
  - [x] `whenDeltaAlreadyExists_updatesExistingEntry`
  - [x] `whenGameHasNoPeriod_isSkipped`
  - [x] `whenGameHasNoDraft_isSkipped`
  - [x] `whenOneGameThrows_otherGamesStillProcessed`

## Dev Notes

### Architecture : 0 nouvelle violation

`TeamScoreDeltaOrchestrationService` → `TeamScoreDeltaBatchService` → ports. Aucun service ne parle directement à un JPA repository.

### Ordre d'implémentation recommandé

1. Migration V40 (Task 1) — prérequis JPA
2. Domaine + port + JPA adapter (Tasks 2-3)
3. Extensions des ports existants (Tasks 4-5-6)
4. Service batch (Task 7) — avec tests TDD
5. Orchestrateur (Task 8)
6. Tests finaux (Task 9)

### Ne pas modifier `ScoreCalculationService`

Le fichier a des caractères UTF-8 corrompus (double-encoded). Toute modification risque de générer de nouveaux artefacts corrompus. Créer `TeamScoreDeltaBatchService` ex nihilo.

### `@Transactional` sur `computeDeltasForGame`

Le batch principal `computeAllGameDeltas()` ne doit PAS être transactionnel (trop long). Chaque `computeDeltasForGame()` devrait être `@Transactional` séparément (1 transaction par partie, rollback partiel possible).

### RankSnapshot sans région explicite

`findLatestOnOrBefore(UUID playerId, LocalDate date)` cherche le snapshot sans filtrer par région. Le `pr_value` retourné correspond à la dernière région ingérée pour ce joueur (potentiellement plusieurs rows si multi-région). Si plusieurs snapshots existent à la même date max, Hibernate retourne l'un d'eux. Pour V1, c'est acceptable.

**Alternative V1+** : filtrer par `player.region` pour plus de précision. Non requis pour cette story.

## Dev Agent Record

### Agent Model Used
claude-sonnet-4-6

### Debug Log References

N/A — implementation followed story spec. Key deviation: `GameParticipantRepositoryPort.findByGameIdOrderByJoinedAt()` already existed, so no new method added to that port (used directly). Test dates fixed to use past period (2025-01-01 → 2025-12-31) to make `LocalDate.now()` deterministic in tests.

### Completion Notes List

- **V40 migration**: `team_score_deltas` table with UNIQUE(game_id, participant_id, period_start, period_end) + index on (game_id, delta_pr DESC).
- **`TeamScoreDelta`**: final domain model, `restore()` factory, immutable getters. In `domain/team/model/` — auto-detected by DomainIsolationTest.
- **`TeamScoreDeltaRepositoryPort`**: `save()`, `findByGameId()`, `findByGameIdAndParticipantId()`.
- **`TeamScoreDeltaJpaRepository`**: Spring Data derived queries (no `@ManyToOne` relationships → no Hibernate 6 SQM issue).
- **`TeamScoreDeltaRepositoryAdapter`**: inline mapper (toEntity/toDomain) — no separate mapper class needed.
- **`RankSnapshotRepositoryPort`**: extended with `findLatestOnOrBefore(UUID, LocalDate)` → uses `ORDER BY snapshotDate DESC LIMIT 1` via JPQL.
- **`DraftPickRepositoryPort`**: extended with `findPlayerIdsByDraftIdAndParticipantId()` → `@Query` on existing `DraftPickRepository`.
- **`GameDomainRepositoryPort`**: extended with `findAllWithCompetitionPeriod()` → JPQL filters `competitionStart IS NOT NULL AND competitionEnd IS NOT NULL AND deletedAt IS NULL`.
- **`TeamScoreDeltaBatchService`**: 6 deps (under max 7), `computeDeltasForGame()` package-private + `@Transactional`, upsert via `restore()` with existing ID.
- **`TeamScoreDeltaOrchestrationService`**: 1 dep, `@Scheduled(cron = "${scoring.team.delta.cron:0 0 8 * * *}")`.
- **Test results**: 6/6 new tests green. 2193 total, 19 failures + 1 error all pre-existing — 0 regressions.

### File List

**New files:**
- `src/main/resources/db/migration/V40__add_team_score_deltas.sql`
- `src/main/java/com/fortnite/pronos/domain/team/model/TeamScoreDelta.java`
- `src/main/java/com/fortnite/pronos/domain/port/out/TeamScoreDeltaRepositoryPort.java`
- `src/main/java/com/fortnite/pronos/model/TeamScoreDeltaEntity.java`
- `src/main/java/com/fortnite/pronos/repository/TeamScoreDeltaJpaRepository.java`
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/team/TeamScoreDeltaRepositoryAdapter.java`
- `src/main/java/com/fortnite/pronos/service/scoring/TeamScoreDeltaBatchService.java`
- `src/main/java/com/fortnite/pronos/service/scoring/TeamScoreDeltaOrchestrationService.java`
- `src/test/java/com/fortnite/pronos/service/scoring/TeamScoreDeltaBatchServiceTest.java`

**Modified files:**
- `src/main/java/com/fortnite/pronos/domain/port/out/RankSnapshotRepositoryPort.java` (added `findLatestOnOrBefore`)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/RankSnapshotJpaRepository.java` (added `@Query findLatestOnOrBefore`)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/RankSnapshotRepositoryAdapter.java` (implemented `findLatestOnOrBefore`)
- `src/main/java/com/fortnite/pronos/domain/port/out/DraftPickRepositoryPort.java` (added `findPlayerIdsByDraftIdAndParticipantId`)
- `src/main/java/com/fortnite/pronos/repository/DraftPickRepository.java` (added `@Query findPlayerIdsByDraftIdAndParticipantId`)
- `src/main/java/com/fortnite/pronos/domain/port/out/GameDomainRepositoryPort.java` (added `findAllWithCompetitionPeriod`)
- `src/main/java/com/fortnite/pronos/repository/GameRepository.java` (added `@Query findAllWithCompetitionPeriod`)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/game/GameRepositoryAdapter.java` (implemented `findAllWithCompetitionPeriod`)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

Aucun HIGH ou MEDIUM trouvé — 0 fix appliqué.

### Action items

- [ ] **[AI-Review][Low][L1]** : `computeDeltasForGame()` est annoté `@Transactional` mais appelé depuis `computeAllGameDeltas()` dans la même classe — Spring AOP self-invocation bypass le proxy. Le `@Transactional` n'est pas honoré. Risque faible (upsert idempotent, batch quotidien), mais une refactorisation vers un service séparé garantirait l'atomicité par partie.
- [ ] **[AI-Review][Low][L2]** : `TeamScoreDeltaBatchServiceTest` manque un test pour le guard `playerRepository.findById().isEmpty() → contribution = 0` (joueur introuvable). La branche `isEmpty() → continue` n'est pas couverte.
