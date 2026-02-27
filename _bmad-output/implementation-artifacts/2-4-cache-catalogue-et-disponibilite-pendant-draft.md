# Story 2.4: Cache catalogue et disponibilite pendant draft

Status: done

## Story

As a connected user,
I want un catalogue rapide et disponible pendant les drafts,
so that je ne suis pas bloque par les traitements batch (FR-13, FR-14).

## Acceptance Criteria

1. **Given** le serveur vient de demarrer ou le scraping nocturne vient de terminer, **When** les premieres requetes catalogue arrivent, **Then** le cache est deja chaud (pas de cold start visible pour l'utilisateur) — conformite NFR-P05.

2. **Given** le cache est chaud, **When** `GET /players/catalogue` ou `GET /players/catalogue?region=EU` est appele, **Then** la reponse est servie depuis le cache (< 2s, 0 requete DB) — conformite NFR-P01, NFR-SC02.

3. **Given** un draft est en cours, **When** un participant consulte le catalogue (`GET /players/catalogue` ou `GET /players/catalogue?region=*`), **Then** la reponse reste disponible (servie depuis le cache meme sous charge draft).

4. **Given** une requete de recherche `GET /players/catalogue/search?q=ninja` est soumise, **When** le catalogue contient des joueurs correspondants, **Then** la reponse est retournee en temps reel (recherche non mise en cache, acceptable jusqu'a ~8000 joueurs — FR-14 "temps reel").

5. **Given** le cache catalogue est vide (demarrage a froid), **When** le warmup se termine, **Then** `findAll()` et `findByRegion(region)` pour toutes les regions connues sont pre-charges en cache.

## Tasks / Subtasks

- [x] Task 1: Ajouter noms de cache `catalogue-all` et `catalogue-region` a `CacheConfig` (AC: #2, #3)
  - [x] TTL 24h (aligne sur cycle scraping nocturne)
  - [x] Ajouter aux listes `CACHE_NAMES` (Redis prod) et `concurrentMapCacheManager` (local/test)
- [x] Task 2: Ajouter `@Cacheable` a `PlayerCatalogueService` (AC: #2, #3, #4)
  - [x] `@Cacheable(value = "catalogue-all", key = "'all'")` sur `findAll()`
  - [x] `@Cacheable(value = "catalogue-region", key = "#region.name()")` sur `findByRegion()`
  - [x] `searchByNickname()` reste NON cache (temps reel, requete variable)
- [x] Task 3: Creer `PlayerCatalogueWarmupService` (AC: #1, #5)
  - [x] Implements `ApplicationListener<ApplicationReadyEvent>` (pattern existant — cf. `DatabaseAutoConfiguration`)
  - [x] Appelle `playerCatalogueService.findAll()` au demarrage
  - [x] Appelle `playerCatalogueService.findByRegion(region)` pour chaque valeur de `PlayerRegion`
  - [x] Methode publique `warmup()` callable par le scraping scheduler (Epic 1) apres batch nocturne
  - [x] Logguer debut/fin warmup (log.info)
- [x] Task 4: Tests `PlayerCatalogueWarmupServiceTest` (AC: #1, #5)
  - [x] Warmup au demarrage appelle findAll()
  - [x] Warmup au demarrage appelle findByRegion() pour chaque region
  - [x] warmup() publique peut etre appelee independamment de l'event
  - [x] Aucun appel si playerCatalogueService est vide
- [x] Task 5: Tests `PlayerCatalogueServiceCacheTest` (AC: #2, #3)
  - [x] Verifier que findAll() est annote @Cacheable (via Spring context ou verif annotation reflective)
  - [x] Verifier que findByRegion() est annote @Cacheable avec key = region.name()
  - [x] Verifier que searchByNickname() n'est pas cache

## Dev Notes

### Infrastructure cache existante (NE PAS recreer)
`src/main/java/com/fortnite/pronos/config/CacheConfig.java` gere deja:
- Redis (prod, `@ConditionalOnProperty("spring.data.redis.host")`) + ConcurrentMap (local/test)
- Liste `CACHE_NAMES` centralisee — ajouter `"catalogue-all"` et `"catalogue-region"` ici
- TTLs specifiques par cache via Map `cacheConfigurations` (Redis) et `cacheManager.setCacheNames` (local)
- Profile `test` → `testCacheManager()` cree un `ConcurrentMapCacheManager` avec `CACHE_NAMES` — les 2 nouveaux noms doivent y etre inclus aussi

**TTL recommande: 24h** (cache est invalide naturellement au prochain cycle scraping)

### Pattern @Cacheable existant (reproduire exactement)
Utilise dans `LeaderboardStatsService`, `TeamLeaderboardService`, `PlayerService` :
```java
@Cacheable(value = "leaderboard", key = "#season")
public List<LeaderboardEntryDTO> getLeaderboard(int season) { ... }

@Cacheable(value = "playerPages",
    key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
public Page<PlayerDto> getAllPlayers(Pageable pageable) { ... }
```
Application pour Story 2.4 :
```java
@Cacheable(value = "catalogue-all", key = "'all'")
public List<CataloguePlayerDto> findAll() { ... }

@Cacheable(value = "catalogue-region", key = "#region.name()")
public List<CataloguePlayerDto> findByRegion(PlayerRegion region) { ... }
```

### Attention: self-invocation Spring AOP
`@Cacheable` ne fonctionne QUE si la methode est appelee depuis l'exterieur du bean (Spring AOP proxy).
- `findAll()` et `findByRegion()` sont appelees depuis `PlayerController` et `PlayerCatalogueWarmupService` → OK, cache fonctionne.
- `searchByNickname()` appelle `playerRepository.findAll()` directement (pas `this.findAll()`) → pas de probleme de self-invocation.
- Ne pas tenter de faire appeler `findAll()` par `searchByNickname()` via `this.findAll()` — le cache ne se declencherait pas.

### Pattern ApplicationListener<ApplicationReadyEvent> existant
`DatabaseAutoConfiguration` (meme package config) suit ce pattern. Pour le warmup:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerCatalogueWarmupService implements ApplicationListener<ApplicationReadyEvent> {

  private final PlayerCatalogueService playerCatalogueService;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    warmup();
  }

  public void warmup() {
    log.info("[CATALOGUE-WARMUP] Starting cache warmup...");
    playerCatalogueService.findAll();
    for (PlayerRegion region : PlayerRegion.values()) {
      playerCatalogueService.findByRegion(region);
    }
    log.info("[CATALOGUE-WARMUP] Cache warmed for {} regions", PlayerRegion.values().length);
  }
}
```
- Placer dans `service/catalogue/` (pas dans `config/`) → NamingConventionTest: finit par `Service` ✓
- 1 seule dependance: `PlayerCatalogueService` → CouplingTest max 7 OK ✓

### PlayerRegion enum — valeurs
`UNKNOWN, EU, NAW, BR, ASIA, OCE, NAC, ME, NA` (9 valeurs)
Le warmup appelle `findByRegion(region)` pour TOUTES (y compris UNKNOWN) — simplicit, nul etat invalide.

### searchByNickname — comportement inchange
La methode n'est pas annotee `@Cacheable`:
- Requete variable (key serait le terme de recherche — trop de cles possibles)
- FR-14 dit "temps reel" pour la recherche
- Reste O(N) in-memory (~8000 players max), < 100ms → acceptable sous charge draft

### Pas de @CacheEvict dans cette story
- TTL 24h = expiration naturelle alignee sur le cycle scraping
- Warmup post-scraping: methode publique `warmup()` sur `PlayerCatalogueWarmupService` — sera appelee par le scheduler scraping (Epic 1, hors scope Story 2.4)
- Pas besoin d'`@CacheEvict` dans cette story

### CacheConfig — comment ajouter les 2 nouveaux noms
```java
// Dans la liste CACHE_NAMES:
private static final String CACHE_CATALOGUE_ALL = "catalogue-all";
private static final String CACHE_CATALOGUE_REGION = "catalogue-region";

private static final List<String> CACHE_NAMES = List.of(
    ..., // existants
    CACHE_CATALOGUE_ALL,
    CACHE_CATALOGUE_REGION
);

// Dans cacheConfigurations (Redis):
cacheConfigurations.put(CACHE_CATALOGUE_ALL,
    defaultConfig.entryTtl(Duration.ofHours(CATALOGUE_TTL_HOURS)));
cacheConfigurations.put(CACHE_CATALOGUE_REGION,
    defaultConfig.entryTtl(Duration.ofHours(CATALOGUE_TTL_HOURS)));
```
Ajouter la constante: `private static final long CATALOGUE_TTL_HOURS = 24L;`

### Tests — strategie
Les tests `@Cacheable` sont difficiles a tester unitairement sans Spring context complet.
Approche recommandee (pas de `@SpringBootTest` pour eviter les tests lents):
- `PlayerCatalogueWarmupServiceTest`: test unitaire Mockito pur — verifier que `findAll()` et `findByRegion(region)` sont appeles pour chaque region lors du warmup
- `PlayerCatalogueServiceCacheTest`: test reflechissant les annotations — verifier que les methodes ont `@Cacheable` avec les bons attributs:
```java
@Test void findAllHasCacheableAnnotation() throws Exception {
    Method m = PlayerCatalogueService.class.getMethod("findAll");
    Cacheable c = m.getAnnotation(Cacheable.class);
    assertThat(c).isNotNull();
    assertThat(c.value()).contains("catalogue-all");
}
```

### Previous story learnings (Stories 2.1-2.3)
- Spotless: `mvn spotless:apply -q --no-transfer-progress && mvn test`
- `Player.restore(UUID, null, username, nickname, PlayerRegion, tranche, 2025, false)` pour les tests
- `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@BeforeEach` pattern
- CouplingTest: max 7 deps par `@Service` — respecte (1 dep pour warmup, 1 dep pour catalogue)
- NamingConventionTest: classes dans `..service..` doivent finir par `Service` ✓
- DomainIsolationTest: classes dans `service/` peuvent avoir Spring annotations — OK
- Linter peut réécrire les fichiers — toujours relire avant édition

### Project Structure Notes
- `src/main/java/com/fortnite/pronos/config/CacheConfig.java` (modifie — ajout catalogue-all + catalogue-region)
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueService.java` (modifie — ajout @Cacheable)
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueWarmupService.java` (nouveau)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueWarmupServiceTest.java` (nouveau)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueServiceCacheTest.java` (nouveau)

### References
- FR-13: Cache Spring obligatoire, 1000 joueurs max par region — [Source: epics.md#Story 2.4]
- FR-14: Catalogue accessible pendant draft, recherche temps reel — [Source: epics.md#Story 2.4]
- NFR-P01: Pages catalogue < 2s cache chaud — [Source: epics.md#NonFunctional Requirements]
- NFR-P05: Warmup avant ouverture du trafic — [Source: epics.md#NonFunctional Requirements]
- NFR-SC02: 100% appels catalogue depuis cache en prod — [Source: epics.md#NonFunctional Requirements]
- NFR-M01: Architecture hexagonale obligatoire — [Source: epics.md#NonFunctional Requirements]
- CacheConfig existante: `src/main/java/com/fortnite/pronos/config/CacheConfig.java`
- Pattern ApplicationListener: `src/main/java/com/fortnite/pronos/config/DatabaseAutoConfiguration.java`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- CacheConfig.java: ajout CATALOGUE_TTL_HOURS=24L, CACHE_CATALOGUE_ALL, CACHE_CATALOGUE_REGION aux constantes, CACHE_NAMES, et cacheConfigurations Redis
- PlayerCatalogueService.java: @Cacheable("catalogue-all", key="'all'") sur findAll() + @Cacheable("catalogue-region", key="#region.name()") sur findByRegion() — searchByNickname() reste non cache (real-time FR-14)
- PlayerCatalogueWarmupService.java: ApplicationListener<ApplicationReadyEvent>, warmup() appelle findAll() + findByRegion(region) pour les 9 valeurs PlayerRegion
- Tests: 12 nouveaux (5 warmup + 7 annotation reflection) — tous verts. Suite totale: 2026 run, 26+1 pre-existing, 0 regression
- [CODE REVIEW FIXES] F-001 (HIGH): PlayerCatalogueServiceCacheIntegrationTest ajoutee (4 tests Spring AOP end-to-end: findAll cache hit, findByRegion cache hit, regions independantes, searchByNickname non cache). F-002 (MEDIUM): warmup() protege par try-catch, exception loggee en WARN (la DB indisponible ne tue pas le demarrage). F-003 (MEDIUM): PlayerRegion.UNKNOWN exclu de la boucle warmup (8 regions connues au lieu de 9). Test swallowsExceptionFromService() et callsFindByRegionForEveryKnownRegion() ajoutes. Suite finale: 2031 run, 26+1 pre-existing, 0 regression.

### File List

- `_bmad-output/implementation-artifacts/2-4-cache-catalogue-et-disponibilite-pendant-draft.md`
- `src/main/java/com/fortnite/pronos/config/CacheConfig.java` (modifie — ajout catalogue-all + catalogue-region, TTL 24h)
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueService.java` (modifie — ajout @Cacheable sur findAll() et findByRegion())
- `src/main/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueWarmupService.java` (nouveau)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueWarmupServiceTest.java` (nouveau — 6 tests, dont swallowsExceptionFromService)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueServiceCacheTest.java` (nouveau — 7 tests)
- `src/test/java/com/fortnite/pronos/service/catalogue/PlayerCatalogueServiceCacheIntegrationTest.java` (nouveau — 4 tests Spring AOP end-to-end)

