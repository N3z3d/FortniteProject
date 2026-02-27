# Story 2.2: Detail joueur avec pseudo courant, region et snapshots

Status: done

## Story

As a connected user,
I want ouvrir une fiche detaillee d'un joueur,
so that je puisse evaluer son profil et son historique avant de drafter/trader (FR-11).

## Acceptance Criteria

1. **Given** un joueur existe dans le catalogue, **When** l'utilisateur appelle `GET /players/{id}/profile`, **Then** la reponse contient: nickname, region principale, tranche, locked, un map prByRegion (region -> derniere valeur PR), et lastSnapshotDate.

2. **Given** un joueur a des snapshots dans plusieurs regions, **When** la fiche est consultee, **Then** `prByRegion` contient la valeur PR la plus recente pour chaque region.

3. **Given** un joueur sans aucun snapshot PR enregistre, **When** la fiche est consultee, **Then** `prByRegion` est une map vide et `lastSnapshotDate` est null.

4. **Given** un joueur inexistant, **When** `GET /players/{id}/profile` est appele, **Then** la reponse est 404 Not Found.

## Tasks / Subtasks

- [x] Task 1: Ajouter `findByPlayerRecent(UUID, int days)` a `RankSnapshotRepositoryPort` (AC: #1, #2)
  - [x] Methode retourne `List<RankSnapshot>` pour toutes regions d'un joueur sur les N derniers jours
- [x] Task 2: Implementer dans `RankSnapshotJpaRepository` + `RankSnapshotRepositoryAdapter` (AC: #1, #2)
- [x] Task 3: Creer `PlayerDetailDto` record (AC: #1, #2, #3)
  - [x] Champs: id, nickname, region, tranche, locked, prByRegion (Map<String,Integer>), lastSnapshotDate (LocalDate)
  - [x] Methode factory `from(Player, List<RankSnapshot>)`
- [x] Task 4: Creer `PlayerDetailService` dans `service/catalogue/` (AC: #1, #2, #3, #4)
  - [x] Methode `getPlayerDetail(UUID id)` — 404 si joueur inconnu
  - [x] Snapshot window: 90 jours (constants MAX_SNAPSHOT_DAYS)
  - [x] prByRegion: groupBy region, prendre le snapshot le plus recent, extraire prValue
- [x] Task 5: Ajouter `GET /players/{id}/profile` a `PlayerController` (AC: #1, #4)
- [x] Task 6: Tests `PlayerDetailServiceTest` (AC: #1, #2, #3, #4)

## Dev Notes

### Architecture constraints
- `PlayerDetailService` dans `service/catalogue/` — 2 deps (PlayerDomainRepositoryPort + RankSnapshotRepositoryPort)
- `PlayerDetailDto` dans `dto/player/`
- Port `RankSnapshotRepositoryPort` est dans `domain/port/out/` — ajout d'une methode OK (ISP: meme contexte lecture snapshots)
- CouplingTest: max 7 deps — PlayerDetailService = 2 deps ✓

### PlayerNotFoundException existante?
Verifier `com.fortnite.pronos.exception.*` pour une exception 404 a reutiliser.

### RankSnapshotEntity fields
Champs: `playerId` (UUID), `region` (String), `prValue` (int), `rank` (int), `snapshotDate` (LocalDate)

### PlayerDetailDto design
```java
public record PlayerDetailDto(
    UUID id, String nickname, String region, String tranche, boolean locked,
    Map<String, Integer> prByRegion, LocalDate lastSnapshotDate) {

  public static PlayerDetailDto from(Player player, List<RankSnapshot> snapshots) {
    Map<String, Integer> prByRegion = snapshots.stream()
        .collect(Collectors.toMap(
            RankSnapshot::getRegion,
            RankSnapshot::getPrValue,
            (a, b) -> b));  // keep latest (snapshots tri par date ASC, b plus recent)
    LocalDate lastDate = snapshots.stream()
        .map(RankSnapshot::getSnapshotDate)
        .max(Comparator.naturalOrder())
        .orElse(null);
    return new PlayerDetailDto(player.getId(), player.getNickname(), player.getRegionName(),
        player.getTranche(), player.isLocked(), prByRegion, lastDate);
  }
}
```

### New JPA query
```java
// Dans RankSnapshotJpaRepository
@Query("SELECT r FROM RankSnapshotEntity r WHERE r.playerId = :playerId AND r.snapshotDate >= :since ORDER BY r.snapshotDate ASC")
List<RankSnapshotEntity> findByPlayerSince(@Param("playerId") UUID playerId, @Param("since") LocalDate since);
```

### Previous story learnings
- Spotless: `mvn spotless:apply -q --no-transfer-progress && mvn test`
- `Player.restore(UUID, null, username, nickname, PlayerRegion, tranche, 2025, false)` pour les tests
- `RankSnapshot.restore(UUID, UUID, region, rank, prValue, LocalDate)` pour les tests
- `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@BeforeEach` pattern

### Project Structure Notes
- `src/main/java/com/fortnite/pronos/dto/player/PlayerDetailDto.java` (nouveau)
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerDetailService.java` (nouveau)
- `src/main/java/com/fortnite/pronos/domain/port/out/RankSnapshotRepositoryPort.java` (modifie — ajout findByPlayerRecent)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/RankSnapshotJpaRepository.java` (modifie)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/RankSnapshotRepositoryAdapter.java` (modifie)
- `src/main/java/com/fortnite/pronos/controller/PlayerController.java` (modifie — ajout endpoint)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerDetailServiceTest.java` (nouveau)

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### File List

- `_bmad-output/implementation-artifacts/2-2-detail-joueur-avec-pseudo-courant-region-et-snapshots.md`
- `src/main/java/com/fortnite/pronos/domain/port/out/RankSnapshotRepositoryPort.java` (modifie — ajout findByPlayerRecent)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/RankSnapshotJpaRepository.java` (modifie — ajout findByPlayerSince query)
- `src/main/java/com/fortnite/pronos/adapter/out/persistence/player/RankSnapshotRepositoryAdapter.java` (modifie — ajout findByPlayerRecent impl)
- `src/main/java/com/fortnite/pronos/dto/player/PlayerDetailDto.java` (nouveau)
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerDetailService.java` (nouveau)
- `src/main/java/com/fortnite/pronos/controller/PlayerController.java` (modifie — ajout GET /players/{id}/profile + imports)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerDetailServiceTest.java` (nouveau — 5 tests)
