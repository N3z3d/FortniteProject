# Story sprint9-pipeline-rewrite: FortniteTracker Scraping Adapter — Java rewrite de power-rankings.js

Status: done

<!-- METADATA
  story_key: sprint9-pipeline-rewrite
  branch: story/sprint9-pipeline-rewrite
  sprint: Sprint 9
-->

## Story

As a platform engineer,
I want a Java `FortniteTrackerScrapingAdapter` that implements `PrRegionCsvSourcePort` and scrapes FortniteTracker.com via proxy services (Scrapfly / ScraperAPI / Scrape.do),
so that the Spring Boot scheduled ingestion pipeline (`PrIngestionOrchestrationService`) has a real bean to inject and can run autonomously without the Node.js script.

## Architecture Decisions (session 2026-03-15)

| # | Décision | Choix retenu | Raison |
|---|---|---|---|
| D1 | API officielle vs Scraping | **Scraping** | API tracker.gg = lookup individuel par epic ID, pas leaderboard paginé. Rate limit 1 req/2s incompatible. |
| D2 | HTTP Client | **RestTemplate bean dédié 20s** | `RestTemplateConfig` existe déjà, cohérence avec `FortniteApiAdapter`. Nouvelle `@Bean restTemplateForScraping`. |
| D3 | Retry | **Boucle Java custom** | Rotation provider sur tentatives impaires + backoff custom non exprimable avec `@Retryable` ou RetryTemplate. |
| D4 | Structure | **3 classes** : adapter + parser + urlbuilder | Clean Code <500 lignes, Single Responsibility, testabilité unitaire par classe. |
| D5 | Config | **`@ConfigurationProperties(prefix="scraping.fortnitetracker")`** | 12 propriétés — `@Value` trop verbeux. |
| D6 | JSoup | **1.22.1** | Version stable actuelle (recherche 2026-03-15). |
| D7 | Pages | **Séquentiel** dans `fetchCsv` | Job nocturne 5h-8h UTC, séparation des responsabilités. L'orchestrateur parallélisera si besoin. |

## Acceptance Criteria

1. `FortniteTrackerScrapingAdapter` est un `@Component` dans `adapter/out/scraping/` implémentant `PrRegionCsvSourcePort`.
2. `fetchCsv(PrRegion region)` scrape les pages 1 à N de FortniteTracker.com pour la région donnée, retourne un CSV au format `nickname,region,points,rank,snapshot_date` (format attendu par `PrCsvParser`), ou `Optional.empty()` si toutes les pages échouent.
3. Les 3 fournisseurs proxy sont supportés : **Scrapfly**, **ScraperAPI**, **Scrape.do**. Les clés sont lues via `@ConfigurationProperties(prefix="scraping.fortnitetracker")` — injectées depuis env vars (`SCRAPFLY_KEYS`, `SCRAPERAPI_KEYS`, `SCRAPEDO_TOKEN`).
4. Le fournisseur primaire est choisi de façon déterministe par `hash(region + page) % totalWeight`. En cas d'échec, le fournisseur est rotatif à la tentative suivante.
5. Retry : **8 tentatives max** par page, backoff exponentiel : `300 * 1.9^attempt + random(300)` ms — boucle Java custom (pas `@Retryable`).
6. Parsing HTML avec **JSoup 1.22.1** (à ajouter dans `pom.xml`) : extraire `tbody tr` → rank, nickname, points via sélecteurs CSS (Dev Notes §Parsing JSoup).
7. Déduplication par `(region, normalizedNickname)` : conserver la ligne avec le plus grand `points` (tie-break : rank plus bas).
8. Normalisation des noms : NFKD + suppression des diacritiques + zero-width chars + collapse espaces + lowercase (identique à `normalizeName()` du script Node.js).
9. Configuration via `@ConfigurationProperties(prefix="scraping.fortnitetracker")`. `fetchCsv` retourne `Optional.empty()` proprement si aucun provider configuré — pas d'exception.
10. Au minimum **10 tests unitaires** répartis dans 3 classes de test :
    - `FortniteTrackerHtmlParserTest` : nominal, ligne malformée, tbody vide, normalisation, déduplication
    - `ProxyUrlBuilderTest` : URL Scrapfly, ScraperAPI, Scrape.do — vérification exacte des paramètres
    - `FortniteTrackerScrapingAdapterTest` : retry 3-fails-then-success, fallback empty quand exhausted, provider rotation, CSV assemblé correctement
11. Le script Node.js `scripts/ingest/power-rankings.js` est archivé dans `scripts/ingest/archive/power-rankings.legacy.js` avec commentaire en tête.
12. 0 régression sur les tests existants (backend 2299 run baseline, frontend 2243/2243).

## Tasks / Subtasks

- [ ] Task 1: Ajouter JSoup dans pom.xml + bean RestTemplate scraping (AC: #6, D2)
  - [ ] 1.1: Ajouter `org.jsoup:jsoup:1.22.1` dans `<dependencies>` du `pom.xml`
  - [ ] 1.2: Ajouter `@Bean restTemplateForScraping()` dans `RestTemplateConfig` — timeout 20s connect + 20s read
  - [ ] 1.3: Lancer `mvn spotless:apply` pour vérifier aucune erreur de format

- [ ] Task 2: Créer `@ConfigurationProperties` de scraping (AC: #3, #9, D5)
  - [ ] 2.1: Créer `FortniteTrackerScrapingProperties` (record `@ConfigurationProperties(prefix="scraping.fortnitetracker")`) :
    - `List<String> scrapflyKeys` — parse `${scraping.fortnitetracker.scrapfly-keys:}` (comma-separated)
    - `List<String> scraperapiKeys` — parse `${scraping.fortnitetracker.scraperapi-keys:}`
    - `List<String> scrapedoTokens` — parse `${scraping.fortnitetracker.scrapedo-token:}`
    - `int maxAttempts` — défaut 8
    - `int requestTimeoutMs` — défaut 20000
    - `int pagesPerRegion` — défaut 4
    - `String platform` — défaut "pc"
    - `String timeframe` — défaut "year"
    - `Map<String,Integer> weights` — défauts scrapfly=1, scraperapi=1, scrapedo=1
  - [ ] 2.2: Annoter `@EnableConfigurationProperties(FortniteTrackerScrapingProperties.class)` dans une `@Configuration` ou dans `PronosApplication`
  - [ ] 2.3: Ajouter dans `application.properties` les propriétés commentées avec valeurs par défaut

- [ ] Task 3: Créer `ProxyUrlBuilder` dans `adapter/out/scraping/` (AC: #3, D4)
  - [ ] 3.1: Classe `ProxyUrlBuilder` (pas de `@Component` — instanciée par l'adapter)
  - [ ] 3.2: `String build(String provider, String targetUrl, String key, int requestTimeoutMs)` → URL proxy complète
  - [ ] 3.3: Méthode `String buildTarget(String region, int page, String platform, String timeframe)` → URL FortniteTracker
  - [ ] 3.4: Tester dans `ProxyUrlBuilderTest` (AC: #10)

- [ ] Task 4: Créer `FortniteTrackerHtmlParser` dans `adapter/out/scraping/` (AC: #6, #7, #8, D4)
  - [ ] 4.1: Classe `FortniteTrackerHtmlParser` — méthode `List<ScrapedRow> parse(String html, String region, int pageOrdinalOffset)`
  - [ ] 4.2: Parsing JSoup `doc.select("tbody tr")` — rank via attr `placement` ou position ordinale, nickname via `[class*=leaderboard-user__nickname]`, points via `td[class*=column--highlight] div` puis fallback `td[class*=column--right] div`
  - [ ] 4.3: Méthode statique `String normalizeName(String value)` — NFKD + diacritiques + zero-width + lowercase
  - [ ] 4.4: Méthode `Map<String, ScrapedRow> deduplicate(List<ScrapedRow> rows)` — clé `region|normalized`, isBetter = points > ou tie rank <
  - [ ] 4.5: Tester dans `FortniteTrackerHtmlParserTest` avec fixture HTML dans `src/test/resources/scraping/` (AC: #10)

- [ ] Task 5: Créer `FortniteTrackerScrapingAdapter` dans `adapter/out/scraping/` (AC: #1, #2, #4, #5, D3)
  - [ ] 5.1: `@Component FortniteTrackerScrapingAdapter implements PrRegionCsvSourcePort`
    - Injecte : `FortniteTrackerScrapingProperties`, `@Qualifier("restTemplateForScraping") RestTemplate`, `FortniteTrackerHtmlParser`, `ProxyUrlBuilder`
  - [ ] 5.2: `Optional<String> fetchCsv(PrRegion region)` — boucle pages 1..pagesPerRegion, agrège, déduplique, assemble CSV
  - [ ] 5.3: `Optional<List<ScrapedRow>> fetchPageWithRetry(String region, int page)` — boucle 0..maxAttempts-1, backoff custom, rotation provider
  - [ ] 5.4: `String pickProvider(String region, int page, int attempt)` — hash déterministe + rotation sur impair
  - [ ] 5.5: `String pickKey(String provider, int attempt)` — round-robin sur la liste de clés
  - [ ] 5.6: `String assembleCsv(List<ScrapedRow> rows)` — header + lignes avec `LocalDate.now()`
  - [ ] 5.7: Tester dans `FortniteTrackerScrapingAdapterTest` avec RestTemplate mocké (AC: #10)

- [ ] Task 6: Archiver le script Node.js (AC: #11)
  - [ ] 6.1: `mkdir scripts/ingest/archive`
  - [ ] 6.2: `mv scripts/ingest/power-rankings.js scripts/ingest/archive/power-rankings.legacy.js`
  - [ ] 6.3: Ajouter en tête : `// ARCHIVED 2026-03-15 — replaced by FortniteTrackerScrapingAdapter.java`

- [ ] Task 7: Validation finale (AC: #12)
  - [ ] 7.1: `mvn spotless:apply && mvn test` — vérifier baseline maintenu, 0 nouvelle régression
  - [ ] 7.2: `npm run test:vitest` — vérifier 2243/2243

## Dev Notes

### Décision D1 — Pourquoi pas l'API officielle tracker.gg

L'API `GET https://api.fortnitetracker.com/v1/powerrankings/{platform}/{region}/{epic}` permet de récupérer le rang d'**un joueur spécifique** (par son epic ID). Elle ne fournit pas un leaderboard paginé complet. Pour construire la liste top-100 par région, il faudrait connaître tous les epic IDs à l'avance — bootstrap problem insoluble. Rate limit 1 req/2s. **Le scraping du site reste l'unique approche viable pour le leaderboard complet.**

### Architecture — Structure 3 classes

```
adapter/out/scraping/
├── FortniteTrackerScrapingAdapter.java   ← orchestration, retry, CSV assembly (implémente port)
├── FortniteTrackerHtmlParser.java        ← parsing JSoup + normalizeName + deduplicate
└── ProxyUrlBuilder.java                  ← construction URLs proxies et target FT
```

`FortniteTrackerHtmlParser` et `ProxyUrlBuilder` ne sont PAS des `@Component` — instanciés par l'adapter. Plus faciles à tester (pas de Spring context needed).

### Record interne ScrapedRow

```java
// Visible package-private dans adapter/out/scraping/
record ScrapedRow(String nickname, String region, int points, int rank) {}
```

### Format CSV attendu par PrCsvParser

```
nickname,region,points,rank,snapshot_date
Bugha,EU,12500,1,2026-03-15
Aqua,EU,11200,2,2026-03-15
```

Headers obligatoires (dans `PrCsvParser.REQUIRED_HEADERS`) : `nickname`, `region`, `points`, `rank`, `snapshot_date`.

`snapshotDate` = `LocalDate.now()` en UTC.

### Régions supportées

```java
// PrIngestionOrchestrationService.SUPPORTED_REGIONS
EU, NAC, NAW, BR, ASIA, OCE, ME, GLOBAL
```

URL FortniteTracker par région/page :
```
https://fortnitetracker.com/events/powerrankings?platform=pc&region={REGION}&time=year&page={PAGE}
```

### Bean RestTemplate dédié (D2)

```java
// Dans RestTemplateConfig.java — AJOUTER ce bean
@Bean
public RestTemplate restTemplateForScraping() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(20_000);
    factory.setReadTimeout(20_000);
    return new RestTemplate(factory);
}
```

Injecter dans l'adapter avec `@Qualifier("restTemplateForScraping")`.

### URLs Proxy (port exact du Node.js)

```java
// ProxyUrlBuilder.build(provider, targetUrl, key, timeoutMs)

// Scrapfly
"https://api.scrapfly.io/scrape"
  + "?key=" + encode(key)
  + "&asp=true&render_js=true&country=us"
  + "&url=" + encode(targetUrl)

// ScraperAPI
"https://api.scraperapi.com/?"
  + "api_key=" + encode(key)
  + "&render=false"
  + "&wait_selector=tbody"
  + "&timeout=" + timeoutMs
  + "&url=" + encode(targetUrl)

// Scrape.do
"http://api.scrape.do/"
  + "?url=" + encode(targetUrl)
  + "&token=" + encode(token)
```

### Parsing JSoup (D6 — JSoup 1.22.1)

```java
Document doc = Jsoup.parse(html);
Elements rows = doc.select("tbody tr");
for (int i = 0; i < rows.size(); i++) {
    Element tr = rows.get(i);

    // Rank : attribut placement ou position ordinale
    String rankAttr = tr.select("[class*=leaderboard-rank]").attr("placement");
    int rank = rankAttr.isEmpty() ? (pageOrdinalOffset + i + 1) : Integer.parseInt(rankAttr);

    // Player nickname
    String player = tr.select("[class*=leaderboard-user__nickname]").text().trim();

    // Points : colonne highlight (fallback column--right)
    Element highlightDiv = tr.selectFirst("td[class*=column--highlight] div");
    Element rightDiv = tr.selectFirst("td[class*=column--right] div");
    String pointsText = highlightDiv != null ? highlightDiv.text()
                      : rightDiv != null ? rightDiv.text() : "";
    String pointsClean = pointsText.replaceAll("[^0-9]", "");

    if (player.isEmpty() || pointsClean.isEmpty()) continue;
    rows.add(new ScrapedRow(player, region, Integer.parseInt(pointsClean), rank));
}
```

**Note critique** : le HTML peut changer. Si `tbody` vide ou 0 ligne parsée → retourner liste vide (pas exception).

### Backoff et retry custom (D3)

```java
private long backoffMs(int attempt) {
    return (long)(300 * Math.pow(1.9, attempt)) + (long)(Math.random() * 300);
}

// Dans fetchPageWithRetry :
for (int attempt = 0; attempt < props.maxAttempts(); attempt++) {
    String provider = pickProvider(region, page, attempt);
    String key = pickKey(provider, attempt);
    String proxyUrl = urlBuilder.build(provider, targetUrl, key, props.requestTimeoutMs());
    try {
        ResponseEntity<String> resp = restTemplate.getForEntity(proxyUrl, String.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            List<ScrapedRow> parsed = htmlParser.parse(resp.getBody(), region, (page-1)*100);
            if (!parsed.isEmpty()) return Optional.of(parsed);
        }
    } catch (Exception e) {
        log.warn("Scraping attempt {}/{} failed for region={} page={}: {}", attempt+1, props.maxAttempts(), region, page, e.getMessage());
    }
    if (attempt < props.maxAttempts() - 1) Thread.sleep(backoffMs(attempt));
}
return Optional.empty();
```

### Sélection déterministe du provider (D3)

```java
private String pickProvider(String region, int page, int attempt) {
    List<String> available = getAvailableProviders(); // filtre par clés configurées et poids > 0
    if (available.isEmpty()) return "scraperapi";
    // Rotation sur les tentatives impaires
    int slot = Math.abs((region + "-" + page).hashCode()) % available.size();
    int index = (slot + (attempt % available.size())) % available.size();
    return available.get(index);
}
```

### @ConfigurationProperties (D5)

```java
@ConfigurationProperties(prefix = "scraping.fortnitetracker")
public record FortniteTrackerScrapingProperties(
    @DefaultValue("") String scrapflyKeys,   // comma-separated → parser en List dans constructor
    @DefaultValue("") String scraperapiKeys,
    @DefaultValue("") String scrapedoToken,
    @DefaultValue("8") int maxAttempts,
    @DefaultValue("20000") int requestTimeoutMs,
    @DefaultValue("4") int pagesPerRegion,
    @DefaultValue("pc") String platform,
    @DefaultValue("year") String timeframe
) {}
```

Env vars mappées via Spring Boot : `SCRAPING_FORTNITETRACKER_SCRAPFLY_KEYS=key1,key2` ou dans `.env`.

### Fixture HTML pour les tests

Créer `src/test/resources/scraping/ft-leaderboard-eu-page1.html` — HTML minimal avec 3 `<tr>` simulant la structure FortniteTracker (voir Dev Notes §Parsing JSoup pour les classes CSS à reproduire).

### Contraintes architecture (CRITICAL)

- **`@Component`** — `FortniteTrackerScrapingAdapter` uniquement. Les helpers (`Parser`, `UrlBuilder`) ne sont PAS des beans Spring.
- **CouplingTest max 7 deps** : l'adapter injecte 4 choses (Props + RestTemplate + Parser + UrlBuilder). En sécurité.
- **Domaine pur** : ne pas toucher `domain/` ni `service/ingestion/PrRegionCsvSourcePort.java`
- **Spotless** : `mvn spotless:apply` avant tout `mvn test`
- **ArchUnit** : `adapter/out/scraping/` est dans la couche adapter — conforme. Vérifier `LayeredArchitectureTest`.
- **DomainIsolationTest** : scan sur `com.fortnite.pronos.domain..` uniquement — `adapter/` hors scope, aucun changement ArchUnit nécessaire.

### Pre-existing Gaps / Known Issues

- [KNOWN] Backend: ~15 failures pre-existing (GameDataIntegrationTest 4, FortniteTrackerServiceTddTest 6, PlayerServiceTddTest 1, ScoreServiceTddTest 3, GameStatisticsServiceTddTest 1 error) — exclues du CI
- [KNOWN] Frontend: 2243/2243 passing — 0 failures
- [KNOWN] `PrIngestionOrchestrationService` est `@ConditionalOnProperty` → non instancié par défaut → 0 DI failure possible durant les tests
- [KNOWN] FortniteTracker peut bloquer les proxies sur GLOBAL — `fetchCsv(GLOBAL)` peut retourner `Optional.empty()` — normal
- [KNOWN] Le HTML FortniteTracker peut changer — les sélecteurs JSoup sont basés sur la structure 2025-2026 observée dans le Node.js. Si 0 lignes parsées, loguer un WARNING pour alerter.

### Project Structure Notes

```
src/main/java/com/fortnite/pronos/
├── config/
│   └── RestTemplateConfig.java          ← modifié (+ bean restTemplateForScraping 20s)
├── service/ingestion/
│   ├── PrRegionCsvSourcePort.java       ← interface existante (NE PAS MODIFIER)
│   ├── PrIngestionOrchestrationService  ← existant, orchestre par région
│   └── PrCsvParser.java                 ← existant, parse le CSV retourné
└── adapter/out/scraping/
    ├── FortniteTrackerScrapingAdapter.java   ← NOUVEAU (@Component, implémente port)
    ├── FortniteTrackerHtmlParser.java        ← NOUVEAU (JSoup parser + normalizeName + deduplicate)
    ├── ProxyUrlBuilder.java                  ← NOUVEAU (URL construction)
    └── ScrapedRow.java (ou record inline)    ← NOUVEAU (record interne)

src/test/java/com/fortnite/pronos/adapter/out/scraping/
├── FortniteTrackerScrapingAdapterTest.java   ← NOUVEAU (≥4 tests)
├── FortniteTrackerHtmlParserTest.java        ← NOUVEAU (≥4 tests)
└── ProxyUrlBuilderTest.java                  ← NOUVEAU (≥3 tests, total ≥10 AC#10)

src/test/resources/scraping/
└── ft-leaderboard-eu-page1.html             ← NOUVEAU (fixture HTML)

scripts/ingest/archive/
└── power-rankings.legacy.js                 ← ARCHIVÉ (déplacé depuis scripts/ingest/)
```

### References

- [Source: scripts/ingest/power-rankings.js — logique scraping, proxies, retry, normalizeName, déduplication]
- [Source: service/ingestion/PrRegionCsvSourcePort.java — interface à implémenter]
- [Source: service/ingestion/PrIngestionOrchestrationService.java — orchestrateur appelant fetchCsv()]
- [Source: service/ingestion/PrCsvParser.java — format CSV attendu (required headers)]
- [Source: config/RestTemplateConfig.java — pattern bean RestTemplate existant]
- [Source: adapter/out/api/FortniteApiAdapter.java — pattern adapter HTTP existant dans le projet]
- [Source: tracker.gg/developers/docs/titles/fortnitepr — API officielle (lookup individuel, non retenu)]
- [Source: sprint-8-retro-2026-03-15.md §7 — action items Sprint 9]
- [Source: project-context.md §3 Règles Critiques Backend — CouplingTest max 7 deps, Spotless, ArchUnit]
- [Architecture session 2026-03-15 — 7 décisions D1-D7 documentées dans §Architecture Decisions]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- Implemented `FortniteTrackerScrapingAdapter` as `@Component` implementing `PrRegionCsvSourcePort` in `adapter/out/scraping/`.
- `FortniteTrackerHtmlParser` and `ProxyUrlBuilder` are plain classes (not Spring beans), instantiated by the adapter — max testability, no Spring context needed.
- `FortniteTrackerScrapingProperties` uses `@Component` + `@ConfigurationProperties` pattern (consistent with existing `SeedProperties`).
- JSoup 1.22.1 added to `pom.xml`. `org.jsoup..` added to HexagonalArchitectureTest allowed packages for `adapter.out`.
- `LayeredArchitectureTest`: added `"Adapters"` to Services layer access list (adapter implements `PrRegionCsvSourcePort` from `service.ingestion`).
- 26 tests total across 3 test classes (ProxyUrlBuilderTest: 6, FortniteTrackerHtmlParserTest: 12, FortniteTrackerScrapingAdapterTest: 8).
- Fixture `ft-leaderboard-eu-page1.html` created in `src/test/resources/scraping/` with 3 rows simulating FortniteTracker structure.
- `power-rankings.js` archived to `scripts/ingest/archive/power-rankings.legacy.js` with archive comment.
- Full test baseline: 2330 tests run, 0 failures, 0 errors (with standard pre-existing excludes). 0 regressions.

### File List

- `pom.xml` — modifié (jsoup 1.22.1)
- `src/main/java/com/fortnite/pronos/config/RestTemplateConfig.java` — modifié (bean restTemplateForScraping 20s)
- `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingProperties.java` — créé
- `src/main/java/com/fortnite/pronos/adapter/out/scraping/ScrapedRow.java` — créé
- `src/main/java/com/fortnite/pronos/adapter/out/scraping/ProxyUrlBuilder.java` — créé
- `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerHtmlParser.java` — créé
- `src/main/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingAdapter.java` — créé
- `src/test/java/com/fortnite/pronos/adapter/out/scraping/ProxyUrlBuilderTest.java` — créé
- `src/test/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerHtmlParserTest.java` — créé
- `src/test/java/com/fortnite/pronos/adapter/out/scraping/FortniteTrackerScrapingAdapterTest.java` — créé
- `src/test/resources/scraping/ft-leaderboard-eu-page1.html` — créé
- `scripts/ingest/archive/power-rankings.legacy.js` — archivé
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — modifié
