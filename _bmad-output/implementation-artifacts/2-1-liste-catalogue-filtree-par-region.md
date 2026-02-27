# Story 2.1: Liste catalogue filtree par region

Status: done

## Story

As a connected user,
I want consulter la liste joueurs avec filtre region,
so that je trouve rapidement des candidats pertinents pour ma partie (FR-10).

## Acceptance Criteria

1. **Given** le catalogue contient des joueurs multi-regions, **When** l'utilisateur applique un filtre region via `GET /players/catalogue?region=EU`, **Then** seuls les joueurs de la region EU sont retournes (max 1000) avec leurs champs catalogue (id, nickname, region, tranche, locked).

2. **Given** aucun filtre region n'est fourni, **When** l'utilisateur appelle `GET /players/catalogue`, **Then** tous les joueurs sont retournes (toutes regions confondues, max 1000).

3. **Given** une region invalide est fournie, **When** la requete est soumise, **Then** le systeme repond 400 Bad Request.

4. **Given** un utilisateur non authentifie tente d'acceder au catalogue, **When** la requete est soumise sans JWT valide, **Then** le systeme repond 401 Unauthorized.

## Tasks / Subtasks

- [x] Task 1: Creer `CataloguePlayerDto` record (AC: #1, #2)
  - [x] Champs: id (UUID), nickname (String), region (String), tranche (String), locked (boolean)
  - [x] Methode factory statique `from(Player player)` utilisant le modele domaine
- [x] Task 2: Creer `PlayerCatalogueService` dans `service/catalogue/` (AC: #1, #2)
  - [x] Methode `findByRegion(PlayerRegion region)` — utilise `PlayerDomainRepositoryPort.findByRegion()`
  - [x] Methode `findAll()` — utilise `PlayerDomainRepositoryPort.findAll()`
  - [x] Resultat plafonne a 1000 joueurs
- [x] Task 3: Ajouter endpoint `GET /players/catalogue` a `PlayerController` (AC: #1, #2, #3)
  - [x] Param query optionnel `region` (String, converti en `PlayerRegion`)
  - [x] Retourne `List<CataloguePlayerDto>`
- [x] Task 4: Tests `PlayerCatalogueServiceTest` (AC: #1, #2)
  - [x] filtre EU retourne joueurs EU uniquement
  - [x] pas de filtre retourne tous les joueurs
  - [x] resultat plafonne a 1000 quand depassement
  - [x] liste vide si aucun joueur dans la region

## Dev Notes

### Architecture constraints
- Hexagonal: `PlayerCatalogueService` doit utiliser `PlayerDomainRepositoryPort` (pas le JPA repo directement)
- `CataloguePlayerDto` va dans `dto/player/` (package existant)
- Le service va dans `service/catalogue/` (nouveau sous-package OK)
- CouplingTest: max 7 deps par @Service — PlayerCatalogueService n'a qu'1 dep (PlayerDomainRepositoryPort)
- NamingConventionTest: classe dans `..service..` doit finir par `Service` ✓
- DomainIsolationTest: PlayerCatalogueService est dans service/, pas dans domain/ — OK

### Endpoints existants (ne pas casser)
- `GET /players/region/{region}` — EXISTE DEJA, utilise ancien `Player.Region` (JPA model) — NE PAS SUPPRIMER (backward compat)
- `GET /players/search?region=EU` — EXISTE DEJA, utilise PlayerService — NE PAS TOUCHER
- Nouveau endpoint: `GET /players/catalogue?region=EU` — distinct, utilise domaine

### PlayerRegion valeurs valides
`UNKNOWN, EU, NAW, BR, ASIA, OCE, NAC, ME, NA`

### PlayerDomainRepositoryPort — methodes disponibles
```java
List<Player> findByRegion(PlayerRegion region);
List<Player> findAll();
```

### Previous story learnings (Epic 1)
- Spotless DOIT etre lance avant mvn test: `mvn spotless:apply -q --no-transfer-progress && mvn test`
- Linter modifie les fichiers apres save — toujours relire avant edit si du temps s'est ecoule
- `Player.restore(UUID, String, String, String, PlayerRegion, String, int, boolean)` — factory statique pour tests
- `@Service @RequiredArgsConstructor @Slf4j` pattern standard pour les services
- Tests: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@BeforeEach` pour setup

### CataloguePlayerDto design
```java
public record CataloguePlayerDto(UUID id, String nickname, String region, String tranche, boolean locked) {
  public static CataloguePlayerDto from(com.fortnite.pronos.domain.player.model.Player player) {
    return new CataloguePlayerDto(
        player.getId(),
        player.getNickname(),
        player.getRegionName(),
        player.getTranche(),
        player.isLocked());
  }
}
```

### Controller pattern
```java
@GetMapping("/catalogue")
public ResponseEntity<List<CataloguePlayerDto>> getCatalogue(
    @RequestParam(required = false) String region) {
  if (region != null) {
    try {
      PlayerRegion playerRegion = PlayerRegion.valueOf(region.toUpperCase());
      return ResponseEntity.ok(catalogueService.findByRegion(playerRegion));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }
  return ResponseEntity.ok(catalogueService.findAll());
}
```

### Project Structure Notes
- `src/main/java/com/fortnite/pronos/dto/player/CataloguePlayerDto.java` (nouveau)
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueService.java` (nouveau)
- `src/main/java/com/fortnite/pronos/controller/PlayerController.java` (modifie — ajout endpoint)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueServiceTest.java` (nouveau)

### References
- FR-10: Catalogue filtre par region — [Source: epics.md#Story 2.1]
- NFR-M01: Architecture hexagonale obligatoire — [Source: epics.md#NonFunctional Requirements]
- NFR-M03: Coverage >= 85% — [Source: epics.md#NonFunctional Requirements]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List

- `_bmad-output/implementation-artifacts/2-1-liste-catalogue-filtree-par-region.md`
- `src/main/java/com/fortnite/pronos/dto/player/CataloguePlayerDto.java` (nouveau)
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueService.java` (nouveau)
- `src/main/java/com/fortnite/pronos/controller/PlayerController.java` (modifie — ajout endpoint GET /players/catalogue + imports)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueServiceTest.java` (nouveau — 7 tests)
