---
stepsCompleted: ['step-01-init', 'step-02-context', 'step-03-starter', 'step-04-decisions', 'step-05-patterns', 'step-06-structure', 'step-07-validation', 'step-08-complete']
lastStep: 8
status: 'complete'
completedAt: '2026-02-22'
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/project-context.md'
  - 'docs/FORTNITE_API_RESEARCH.md'
  - 'docs/architecture/ADR-001-layered-architecture.md'
  - 'docs/architecture/ADR-002-database-technology.md'
  - 'docs/architecture/ADR-003-api-design-standards.md'
  - 'docs/architecture/ADR-004-cqrs-pattern-adoption.md'
  - 'docs/architecture/ADR-005-logging-strategy.md'
  - 'docs/architecture/ADR-006-games-sync-strategy.md'
  - 'docs/architecture/MIGRATION-ROADMAP.md'
  - 'docs/architecture/environments.md'
  - 'docs/architecture/supabase.md'
  - 'docs/db-design.md'
  - 'docs/db/supabase-mapping.md'
  - 'docs/ingestion/score_model.md'
  - 'docs/ingestion/pr_local.md'
  - 'docs/audit/ARCHITECTURE_AUDIT.md'
  - 'docs/audit/PERFORMANCE_AUDIT.md'
  - 'Jira-tache.txt'
  - 'CLAUDE.md'
workflowType: 'architecture'
project_name: 'FortniteProject'
user_name: 'Thibaut'
date: '2026-02-22'
---

# Architecture Decision Document — FortniteProject

_Ce document se construit collaborativement étape par étape. Les sections sont ajoutées au fil des décisions architecturales._

---

## Project Context Analysis

### Vue d'ensemble des exigences

**47 Functional Requirements — 7 domaines :**

| Domaine | FRs | Composant architectural principal |
|---|---|---|
| Pipeline de données | FR-01 à FR-09 | Scraping Engine + Resolution Service (nouveaux) |
| Catalogue joueurs | FR-10 à FR-14 | Player Catalogue API + Cache (nouveau) |
| Gestion de parties | FR-15 à FR-20 | Game Management (existant, enrichi) |
| Draft | FR-21 à FR-31 | Draft Engine — 2 modes + tranches + admin override (refactorisé) |
| Échanges & Swaps | FR-32 à FR-36 | Trade/Swap Service (existant + trade mutuel cross-région nouveau) |
| Leaderboard & Scoring | FR-37 à FR-40 | Leaderboard Service (existant, delta PR période) |
| Administration pipeline | FR-41 à FR-47 | Admin Pipeline Dashboard (extension FEAT-005) |

**NFRs architecturalement structurants :**

| NFR | Implication architecture |
|---|---|
| Cache catalogue < 2s | CacheConfig existant — warmup post-scraping obligatoire |
| Draft endpoints < 500ms | Validation plancher en mémoire, pas de requête DB à chaud |
| Pipeline non-bloquant | Queue asynchrone `resolution_queue` + worker schedulé séparé |
| Adapters swappables | Interfaces port hexagonaux pour FT + Fortnite-API.com |
| Timeout 10s API externe | Circuit breaker léger sur les adapters externes |
| ArchUnit 0 violation | Tout nouveau code = hexagonal natif dès le départ |
| ≥ 85% coverage enforced | TDD obligatoire, Spotless avant tout `mvn test` |

### Échelle & Complexité

| Indicateur | Valeur |
|---|---|
| **Complexité** | Élevée — brownfield + 2 nouveaux sous-systèmes majeurs |
| **Domaine technique** | Full-stack (Spring Boot 3.3 + Angular 20) + Data Pipeline |
| **Volume données** | ~8 000 joueurs (8 régions × 1 000) + snapshots PR append-only |
| **Intégrations externes** | 3 : FortniteTracker (scraping), Fortnite-API.com (résolution), Supabase (prod DB) |
| **Concurrence** | 100 users + 20 soumissions simultanées draft paramétré |
| **Composants nouveaux** | 4 majeurs : Scraping Engine, Resolution Service, Player Catalogue, Draft Engine v2 |
| **Composants enrichis** | 3 : Trade/Swap, Leaderboard, Admin Dashboard |

### Contraintes & Dépendances techniques

**Contraintes brownfield (non-négociables) :**
- `domain/` = classes `final`, sans JPA/Spring/Lombok — enforced par `DomainIsolationTest`
- Max 7 dépendances injectées par `@Service` — enforced par `CouplingTest`
- Max 500 lignes/classe, 50 lignes/méthode — enforced par `MaximumClassLinesCondition`
- `mvn spotless:apply` obligatoire avant tout `mvn test`
- `GameRepository` ambiguïté `save()`/`findById()` — pattern de désambiguïsation existant

**Dépendances critiques :**
- **POC bloquant** : clé Fortnite-API.com manquante → pipeline résolution non buildable avant validation
- **FEAT-005** (admin panel en cours) : FR-41 à FR-47 doivent s'intégrer dans l'existant, pas dupliquer
- **FEAT-004** (backlog) : correspond exactement à nos FR-01 à FR-14 — l'architecture aligne les deux
- **Soft delete Game** : toutes les requêtes `GameRepository` filtrent `deletedAt IS NULL`

**Stack fixée (pas de changement) :**
- Backend : Java 21, Spring Boot 3.3.0, Flyway 10.11, jjwt 0.12.5, H2 tests
- Frontend : Angular 20, Angular Material, RxJS 7.8, STOMP.js (WebSocket futur)
- Infra : Docker local → Supabase prod, JDBC direct, PostgreSQL 42.7.3

### Préoccupations transversales

| Concern | Portée | Mécanisme existant |
|---|---|---|
| **Cache** | Catalogue + Leaderboard | `CacheConfig` Spring — à étendre |
| **Sécurité** | Tous les endpoints | JWT existant + ADMIN role |
| **Logging** | Pipeline + erreurs | ADR-005 (logging strategy) |
| **Alertes** | UNRESOLVED + scraping failure | Email escalant (nouveau) |
| **Migration hexagonale** | 30 fichiers encore JPA-couplés | MIGRATION-ROADMAP.md — nouveaux composants = hexagonaux natifs |
| **Tests isolation** | All | `TestSecurityConfig`, `TestPasswordEncoderConfig`, `TestPrometheusConfig` |
| **I18n** | Tous les messages UI | 4 fichiers JSON (fr/en/es/pt) — toute nouvelle string doit y figurer |

---

## Starter Template Evaluation

Projet **brownfield** — stack entièrement fixée et en production. Aucun starter template à évaluer.

| Couche | Technologie | Statut |
|---|---|---|
| Backend | Spring Boot 3.3.0 + Java 21 | ✅ En production |
| Frontend | Angular 20 + Angular Material | ✅ En production |
| Base de données | PostgreSQL (Docker local / Supabase prod — JDBC direct uniquement) | ✅ En production |
| Auth | JWT custom (jjwt 0.12.5) + Spring Security | ✅ Fonctionnel, testé |
| Coverage backend | JaCoCo (Maven plugin) | ✅ Actif — 86.89% lignes |
| Tests backend | JUnit + H2 in-memory | ✅ En production |
| Tests frontend | Karma + Jasmine | ✅ En production |
| Formatter | Spotless (Maven) | ✅ Enforced avant `mvn test` |

> **Note auth :** Supabase Auth (GoTrue) existe mais n'est PAS utilisé. Le projet utilise Supabase uniquement comme hébergeur PostgreSQL via JDBC direct. Migration vers Supabase Auth = décision Growth, pas MVP — le JWT custom est opérationnel et testé.

---

## Core Architectural Decisions

### Decision Priority Analysis

**Décisions critiques (bloquent l'implémentation) :**
- POC obligatoire : valider FT scraping HTML structure + Fortnite-API.com pseudo→EpicID avant tout build pipeline
- `resolution_queue` (seule table manquante) : Flyway migration à écrire avant les adapters pipeline
- Bibliothèque scraping (JSoup vs Playwright) et circuit breaker : **différés — décision d'équipe** après POC

**Décisions importantes (shapent l'architecture) :**
- Draft state : DB-backed (table `DRAFTS` existante — `current_round`, `current_pick`, `updated_at`)
- Pipeline async : Spring @Scheduled + @Async (Quartz = Growth si multi-instance)
- Cache catalogue : Caffeine via Spring Cache (extension `CacheConfig` existante)
- WebSocket : STOMP.js/SockJS confirmé (stack déjà présente — `GameRealtimeController`)
- Admin pipeline : intégré dans FEAT-005, nouvelle route `/admin/pipeline`
- `player.status` : concept supprimé — champ ignoré, aucune gestion admin

**Décisions différées (post-MVP ou équipe) :**
- Bibliothèque scraping : JSoup+OkHttp vs Playwright → POC requis + avis équipe
- Circuit breaker : Resilience4j vs manuel → décision équipe après POC
- Quartz Scheduler : si redémarrage-safe ou multi-instance requis
- Redis : si multi-instance ou TTL persistant requis

---

### Data Architecture

**Schéma existant validé (`docs/db-design.md`) :**

Le schéma DB est déjà complet et bien conçu. Les tables suivantes existent et sont utilisables sans modification :

| Table | Usage |
|---|---|
| `PLAYERS` (fortnite_id, tracker_id) | Identité stable — Epic Account ID comme clé universelle |
| `PLAYER_ALIASES` (nickname_raw, source ENUM(OFFICIAL\|TRACKER), since/until) | Historique pseudos 2 sources |
| `PR_SNAPSHOTS` (player_id, region, snapshot_date, points, rank, run_id) | Snapshots PR append-only |
| `INGESTION_RUNS` (source, status, started_at, finished_at, error_message) | Traçabilité pipeline |
| `PR_INGESTION_TARGETS` (region, top_n, enabled) | Config top-N par région |
| `DRAFT_ASYNC_WINDOWS` + `DRAFT_ASYNC_SELECTIONS` | Mode simultané multi-jours |
| `DRAFTS` (current_round, current_pick, status, updated_at) | État draft persistent |

**Seule table à créer (Flyway migration) :**

```sql
-- File de résolution asynchrone Epic ID
resolution_queue (
  id UUID PK,
  raw_pseudo VARCHAR(255) NOT NULL,     -- pseudo brut scrapé
  region pr_region NOT NULL,             -- région d'origine (contexte retry)
  tracker_id VARCHAR(255),               -- tracker_id connu si disponible
  status ENUM(PENDING, RESOLVED, UNRESOLVED, RETRY) NOT NULL DEFAULT 'PENDING',
  attempt_count INT NOT NULL DEFAULT 0,
  fortnite_id VARCHAR(255) NULL,         -- Epic Account ID une fois résolu
  player_id UUID FK -> PLAYERS NULL,     -- lien joueur une fois résolu
  created_at TIMESTAMP NOT NULL,
  resolved_at TIMESTAMP NULL,
  last_attempt_at TIMESTAMP NULL
)
```

**Décision `player.status` :** concept supprimé du scope. Un joueur qui ne joue plus voit simplement ses PR stagner → son pronostiqueur perd des points. C'est la mécanique voulue. Le champ `status DEFAULT 'ACTIVE'` reste dans le DDL Flyway mais n'est jamais modifié ni administré.

**Politique snapshot manquant (décision) :** carry-forward — utiliser le dernier snapshot avant la date D. Zéro points = pénalité non souhaitée pour les joueurs actifs en dehors des tournois.

---

### Draft Engine v2

| Décision | Choix | Rationale |
|---|---|---|
| State management | **DB-backed** — table `DRAFTS` (current_round, current_pick, updated_at) | Draft multi-jours confirmé → in-memory = perte état au redémarrage |
| Cache lecture | `@Cacheable getDraftState(draftId)` — invalidation sur chaque pick | < 500ms NFR respecté sans compromettre la persistance |
| Mode serpent | `LIVE_SNAKE` (sans timer) + `LIVE_LINEAR_TIMER` (avec timer par pick) | Même engine — timer conditionnel sur `draft_timer_seconds != null` |
| Mode simultané | `ASYNC_RANDOM_RESOLVE` — DRAFT_ASYNC_WINDOWS + DRAFT_ASYNC_SELECTIONS | Multi-jours, résolution conflit aléatoire avant insertion DRAFT_PICKS |
| Invariant critique | Résolution conflits **avant** insertion `DRAFT_PICKS` | `UNIQUE(draft_id, player_id)` sur DRAFT_PICKS — jamais de doublon |
| Validation tranches | In-memory : `(slot-1) × taille_tranche + 1` | Zéro requête DB à chaud, calcul déterministe |
| `LIVE_LINEAR_TIMER` dans ENUM | Garder dans DDL, ignorer si non configuré | Pas de migration coûteuse, évolution future possible |

---

### Pipeline de données

| Décision | Choix | Rationale |
|---|---|---|
| Scraping Engine | **Différé équipe** (JSoup+OkHttp ou Playwright selon POC) | Inconnue I3 : FT = SSR ou SPA ? |
| Circuit Breaker | **Différé équipe** (Resilience4j recommandé sous réserve) | Décision après POC validé |
| Async | Spring `@Scheduled` (cron nuit + post-tournoi) + `@Async` (résolution) | Zéro dépendance externe, volume ~8000 joueurs MVP |
| Port hexagonal | `ScrapingPort` + `ResolutionPort` — adapters swappables | NFR adapters swappables respecté |
| État UNRESOLVED | Table `resolution_queue` — retry automatique, alerte admin > 24h | Pipeline non-bloquant |
| Top-N + watchlist | Ingestion = top-N configuré + tous les joueurs dans des games actives | Évite trous de données sur joueurs hors top-N |

---

### Cache & Performance

| Décision | Choix | Rationale |
|---|---|---|
| Cache catalogue | Spring Cache + **Caffeine** — extension `CacheConfig` existante | < 2s NFR, zéro infra, warmup post-scraping |
| Eviction | Manuelle admin (FR-14) + TTL configurable | Contrôle admin explicite |
| Cache leaderboard | Caffeine — déjà existant (`LeaderboardCacheService`) | Réutiliser l'existant |
| Cache draft state | `@Cacheable` lecture uniquement — `@CacheEvict` sur pick confirmé | Autorité = DB, cache = optimisation lecture |

---

### Authentication & Security

| Décision | Choix | Rationale |
|---|---|---|
| Auth | JWT custom jjwt 0.12.5 — inchangé | Fonctionnel et testé. Supabase Auth = Growth |
| Rôles pipeline | `ADMIN` role existant — pas de nouveau rôle | FR-41 à FR-47 = ADMIN uniquement |
| Clé API externe | `FORTNITE_API_KEY` variable d'environnement | POC requis avant implémentation |

---

### API & Communication

| Décision | Choix | Rationale |
|---|---|---|
| Player Catalogue | REST paginé offset/limit — classe `Pagination` existante | Cohérence avec API existante |
| Draft temps-réel | STOMP.js/SockJS — topics `/topic/draft/{gameId}` | Stack déjà présente (`GameRealtimeController`) |
| Admin pipeline status | Polling `/admin/pipeline/status` (30s MVP) | SSE = Growth |
| Trades | REST existant — extension cross-région uniquement | Pas de nouveau pattern |

---

### Frontend Architecture

| Décision | Choix | Rationale |
|---|---|---|
| Player Catalogue | Nouveau feature module `catalogue/` | Pattern feature isolation existant |
| Admin pipeline | Route `/admin/pipeline` dans feature `admin/` existante | Réutilise `AdminGuard` + `AdminService` |
| State management | Services + BehaviorSubject — pattern existant | Cohérence architecture features |

---

### Infrastructure & Deployment

| Décision | Choix | Rationale |
|---|---|---|
| Hosting | Docker local → Supabase prod — inchangé | Stack fixée |
| CI/CD | Maven Spotless + JaCoCo + Karma — inchangé | Stack fixée |
| Monitoring | `INGESTION_RUNS` table + Prometheus existant | Logs pipeline en DB |

---

### Decision Impact Analysis

**Séquence d'implémentation recommandée :**
1. Flyway migration `resolution_queue` → débloquer adapters pipeline
2. POC scraping (10 joueurs EU) → valider FT HTML + Fortnite-API.com avant build
3. `ScrapingPort` + adapter → `ResolutionPort` + adapter → `PlayerCatalogueService` (FR-01..14)
4. `DraftEngineService` v2 DB-backed + cache lecture (FR-21..31)
5. Extension Trade cross-région (FR-32..36)
6. `AdminPipelineController` + `AdminPipelineService` dans FEAT-005 (FR-41..47)

**Dépendances cross-composants :**
- Pipeline (FR-01..09) → Player Catalogue (FR-10..14) → Draft Engine v2 (FR-21..31)
- POC bloquant : toute implémentation pipeline suspendue jusqu'à validation
- FEAT-005 admin panel existant = stable pendant intégration FR-41..47

---

## Implementation Patterns & Consistency Rules

### Zones de conflit identifiées — 8 domaines

### Naming Patterns — Backend (nouveaux packages)

Conventions obligatoires pour les 4 composants nouveaux :

```
Scraping Engine  → adapter/out/scraping/    (adapter sortant vers FortniteTracker)
Resolution       → adapter/out/resolution/   (adapter sortant vers Fortnite-API.com)
Player Catalogue → service/catalogue/        (service métier)
Admin Pipeline   → service/admin/            (étend l'existant admin/)
Draft Engine v2  → service/draft/            (étend l'existant draft/)
Ports            → domain/port/out/          (ScrapingPort, ResolutionPort, PlayerCataloguePort)
```

### API Naming Patterns — Nouveaux endpoints

Convention : pluriel, kebab-case, sous `/api/`.

```
# Player Catalogue
GET  /api/players                         ← liste paginée
GET  /api/players/{id}                    ← détail joueur
GET  /api/players/{id}/aliases            ← historique pseudos
GET  /api/players/{id}/pr                 ← snapshots PR

# Admin Pipeline
POST /api/admin/pipeline/run              ← déclencher scraping manuel
GET  /api/admin/pipeline/status           ← état dernier run
GET  /api/admin/pipeline/queue            ← file résolution (paginée)
POST /api/admin/pipeline/resolve/{id}     ← résolution manuelle admin

# Draft Engine v2
GET  /api/games/{id}/draft/state          ← état draft actuel
POST /api/games/{id}/draft/pick           ← soumettre un pick (mode serpent)
POST /api/games/{id}/draft/selection      ← soumettre sélection (mode simultané)
```

### Format Patterns — Réponses API

**Réponse directe — pas d'enveloppe** (convention brownfield existante) :

```java
// ✅ Correct
@GetMapping("/api/players/{id}")
public PlayerDto getPlayer(@PathVariable UUID id) { ... }

// ❌ Interdit dans ce projet
public ApiResponse<PlayerDto> getPlayer(...) { ... }
```

- Erreurs : `GlobalExceptionHandler` existant → `{message: "...", code: "..."}`
- Pagination : classe `Pagination` existante → `{content: [...], page: N, size: N, totalElements: N}`
- Dates : ISO-8601 strings (`2026-02-22T02:00:00Z`)
- JSON fields : camelCase côté DTO Java/TypeScript

### Communication Patterns — WebSocket STOMP

Topics et format payload pour le draft temps-réel :

```
/topic/draft/{gameId}          ← état global (round, pick actuel, joueur actif)
/topic/draft/{gameId}/picks    ← picks confirmés
/topic/draft/{gameId}/timer    ← countdown (LIVE_LINEAR_TIMER uniquement)
```

Payload standard unifié :

```json
{
  "type": "PICK_CONFIRMED | TIMER_UPDATE | ROUND_CHANGE | DRAFT_FINISHED",
  "draftId": "uuid",
  "round": 3,
  "pick": 12,
  "payload": { }
}
```

### Domain Patterns — Nouveaux modèles hexagonaux

Tout nouveau modèle dans `domain/` = `final` + static factory + zéro annotation :

```java
// ✅ Pattern obligatoire
public final class PlayerCatalogue {
    public static PlayerCatalogue restore(UUID id, String fortniteId, String trackerId) { ... }
}

// Ports dans domain/port/out/
public interface ScrapingPort {
    List<RawPlayerEntry> scrapeLeaderboard(String region, int topN);
}
public interface ResolutionPort {
    Optional<String> resolveFortniteId(String pseudo, String region);
}
```

### Pipeline Patterns — Async & Gestion des erreurs

```java
// ✅ Scheduler non-bloquant
@Scheduled(cron = "0 2 * * *")
public void runDailyIngestion() {
    pipelineService.triggerIngestionAsync();  // @Async — retourne immédiatement
}

// ✅ UNRESOLVED = état, pas une exception
if (resolution.isEmpty()) {
    resolutionQueue.markUnresolved(entry.id());
    log.warn("UNRESOLVED: pseudo={} region={}", pseudo, region);
    return;  // pipeline continue sur l'entrée suivante
}
```

### Frontend Structure Pattern — Nouveau module Catalogue

```
frontend/src/app/features/catalogue/
├── catalogue.component.ts
├── catalogue.component.html
├── catalogue.component.scss
├── catalogue.routes.ts          ← lazy loading obligatoire
├── models/
│   └── player-catalogue.model.ts
├── services/
│   └── catalogue-data.service.ts
└── mappers/
    └── catalogue-api.mapper.ts
```

DI : `inject()` pour tous les composants standalone. `public readonly t = inject(TranslationService)`.

### Tests Naming Pattern

| Composant nouveau | Fichier test |
|---|---|
| `PlayerCatalogueService` | `PlayerCatalogueServiceTddTest.java` |
| Adapter Scraping | `ScrapingRepositoryAdapterTest.java` |
| Adapter Resolution | `ResolutionRepositoryAdapterTest.java` |
| `AdminPipelineCommandService` | `AdminPipelineCommandServiceTest.java` |
| `AdminPipelineQueryService` | `AdminPipelineQueryServiceTest.java` |
| `ScrapingPipelineService` | `ScrapingPipelineServiceTddTest.java` |
| `DraftPickOrchestrator` | `DraftPickOrchestratorTddTest.java` |
| `catalogue.component.ts` | `catalogue.component.spec.ts` |
| `catalogue-data.service.ts` | `catalogue-data.service.spec.ts` |

### Enforcement — Checklist agent (obligatoire avant tout commit)

```
[ ] Package dans la bonne couche (domain/ adapter/ service/ controller/)
[ ] Classe domain/ : final + sans JPA/Spring/Lombok + static factory
[ ] Service : ≤ 7 dépendances injectées (CouplingTest)
[ ] Classe : ≤ 500 lignes / Méthode : ≤ 50 lignes
[ ] mvn spotless:apply lancé avant mvn test
[ ] Port hexagonal créé si accès à ressource externe
[ ] Nouvelle string UI → ajoutée dans les 4 fichiers i18n (fr/en/es/pt)
[ ] Soft delete filtré si requête GameRepository
[ ] Tests : nominal + ≥3 edge cases + erreurs attendues
[ ] Coverage ≥ 85% sur le code ajouté
```

---

## Project Structure & Boundaries

> Projet brownfield : seuls les **nouveaux fichiers** sont listés. La structure existante est documentée dans `_bmad-output/project-context.md`.

### Nouveaux fichiers Backend — par domaine FR

#### Pipeline de données (FR-01 à FR-09)

```
src/main/java/com/fortnite/pronos/
│
├── domain/port/out/
│   ├── ScrapingPort.java                              ← FR-01..03 : interface scraping FT
│   └── ResolutionPort.java                            ← FR-04..06 : interface résolution Epic ID
│
├── adapter/out/scraping/
│   └── FortniteTrackerScrapingAdapter.java            ← impl ScrapingPort (JSoup ou Playwright — POC requis)
│
├── adapter/out/resolution/
│   └── FortniteApiResolutionAdapter.java              ← impl ResolutionPort (Fortnite-API.com)
│
└── service/ingestion/
    ├── IngestionOrchestrationService.java             ← @Scheduled entry point uniquement (< 50 lignes)
    ├── ScrapingPipelineService.java                   ← logique scraping paginé par région
    ├── ResolutionQueueService.java                    ← gestion PENDING/RETRY/UNRESOLVED + backoff
    └── IngestionAlertService.java                     ← alertes admin si UNRESOLVED > 24h
```

**Règles d'intégrité :**
- `IngestionOrchestrationService` → uniquement vers ports (`ScrapingPort`, `ResolutionPort`) — **jamais** vers `adapter/out/` directement
- `ScrapingPipelineService` → `ScrapingPort` uniquement
- `ResolutionQueueService` → `ResolutionPort` + `JpaRepository<ResolutionQueue, UUID>`

#### Player Catalogue API (FR-10 à FR-14)

```
├── service/catalogue/
│   ├── PlayerCatalogueService.java                    ← FR-10..13 : lecture paginée + cache Caffeine
│   │                                                     @EventListener(ApplicationReadyEvent) warmup
│   └── PlayerCatalogueAdminService.java               ← FR-14 : invalidation cache manuelle admin
│
├── controller/
│   └── PlayerCatalogueController.java                 ← GET /api/players, /{id}, /aliases, /pr
│
└── dto/player/
    ├── PlayerCatalogueDto.java
    ├── PlayerAliasDto.java
    └── PrSnapshotDto.java
```

**Règle critique :** `PlayerCatalogueController` → `PlayerCatalogueService` uniquement. Jamais vers repository directement (`LayeredArchitectureTest`).

**Modèle domaine :** utiliser `Player` existant dans `domain/player/model/Player.java`. **Pas de nouvelle classe `PlayerCatalogue` dans `domain/`.**

#### Flyway — Seule migration à créer

```
src/main/resources/db/migration/
└── V12__add_resolution_queue.sql
```

```sql
-- status = VARCHAR(20) CHECK (pas ENUM natif PG — H2 incompatible)
CREATE TABLE resolution_queue (
  id UUID PRIMARY KEY,
  raw_pseudo VARCHAR(255) NOT NULL,
  region VARCHAR(10) NOT NULL,
  tracker_id VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING','RESOLVED','UNRESOLVED','RETRY')),
  attempt_count INT NOT NULL DEFAULT 0,
  fortnite_id VARCHAR(255),
  player_id UUID REFERENCES players(id),
  created_at TIMESTAMP NOT NULL,
  resolved_at TIMESTAMP,
  last_attempt_at TIMESTAMP
);
```

#### Draft Engine v2 (FR-21 à FR-31)

```
└── service/draft/
    ├── DraftPickOrchestrator.java                     ← FR-21..27 : orchestration picks + mode routing
    │                                                     (remplace le nom générique "DraftEngineService")
    ├── DraftStateService.java                         ← FR-21 : lecture/écriture via DraftRepositoryPort existant
    ├── DraftTrancheValidator.java                     ← FR-22..24 : plancher in-memory (slot-1)×taille+1
    ├── DraftConflictResolver.java                     ← FR-25..27 : résolution doublons mode simultané
    └── DraftAdminOverrideService.java                 ← FR-28..31 : admin assign/remove/reset
```

**Règles d'intégrité :**
- `DraftStateService` → `DraftRepositoryPort` **existant** (pas de nouveau port — DB locale)
- `UNIQUE(draft_id, player_id)` sur `DRAFT_PICKS` = filet de sécurité race condition
- `DraftPickOrchestrator` : max 7 dépendances → déléguer à `DraftTrancheValidator`, `DraftConflictResolver`, `DraftStateService`

#### Trade & Swap extension (FR-32 à FR-36)

Pas de nouveau fichier. Extensions sur fichiers existants :
- `service/TradingService.java` → supprimer contrainte "même région" sur trades mutuels (FR-34)
- `service/ValidationService.java` → règle swap : free player + même région + rang inférieur (FR-32..33)

#### Admin Pipeline (FR-41 à FR-47)

```
├── service/admin/
│   ├── AdminPipelineCommandService.java               ← FR-41..43 : run manuel, resolve, reset
│   │                                                     isRunning atomique → 409 si pipeline actif
│   └── AdminPipelineQueryService.java                 ← FR-44..47 : status, queue paginée, stats
│
└── controller/
    └── AdminPipelineController.java                   ← POST /admin/pipeline/run
                                                          POST /admin/pipeline/resolve/{id}
                                                          GET  /admin/pipeline/status
                                                          GET  /admin/pipeline/queue
```

**Règle CouplingTest :** `AdminPipelineCommandService` ≤ 5 dépendances directes — déléguer à `ScrapingPipelineService`, `ResolutionQueueService`, `IngestionAlertService`.

---

### Nouveaux fichiers Frontend

#### Feature Catalogue (FR-10 à FR-14)

```
frontend/src/app/features/catalogue/
├── catalogue.component.ts
├── catalogue.component.html
├── catalogue.component.scss
├── catalogue.component.spec.ts
├── catalogue.routes.ts                                ← lazy loading
├── models/
│   └── player-catalogue.model.ts
├── services/
│   ├── catalogue-data.service.ts
│   └── catalogue-data.service.spec.ts
└── mappers/
    └── catalogue-api.mapper.ts
```

#### Draft WebSocket — nouveau service core (FR-21 à FR-27)

```
frontend/src/app/core/services/
├── draft-websocket.service.ts                         ← STOMP /topic/draft/{gameId}
└── draft-websocket.service.spec.ts
```

#### Admin Pipeline — extension feature admin existante (FR-41 à FR-47)

```
frontend/src/app/features/admin/
└── pipeline/
    ├── admin-pipeline.component.ts                    ← route /admin/pipeline
    ├── admin-pipeline.component.html
    ├── admin-pipeline.component.scss
    └── admin-pipeline.component.spec.ts
```

#### Routing — ajout dans `app.routes.ts`

```typescript
{ path: 'catalogue', loadChildren: () => import('./features/catalogue/catalogue.routes') }
{ path: 'admin/pipeline', /* déjà dans admin.routes */ }
```

---

### Configuration — Propriétés obligatoires à ajouter

```properties
# application.properties
scraping.timeout.seconds=30
scraping.scheduler.cron=0 2 * * *
resolution.retry.max-attempts=3
resolution.alert.unresolved-threshold-hours=24
catalogue.cache.name=playerCatalogue
```

---

### Mapping FR → Fichier (référence agent)

| FRs | Backend | Frontend |
|---|---|---|
| FR-01..03 | `ScrapingPort` + `FortniteTrackerScrapingAdapter` + `ScrapingPipelineService` | — |
| FR-04..06 | `ResolutionPort` + `FortniteApiResolutionAdapter` + `ResolutionQueueService` | — |
| FR-07..09 | `IngestionAlertService` + `V12__add_resolution_queue.sql` | `admin/pipeline/` |
| FR-10..13 | `PlayerCatalogueService` + `PlayerCatalogueController` | `features/catalogue/` |
| FR-14 | `PlayerCatalogueAdminService` | `admin/pipeline/` (invalidation cache) |
| FR-15..20 | Existant — GameService, GameController | Existant |
| FR-21..24 | `DraftPickOrchestrator` + `DraftStateService` + `DraftTrancheValidator` | `draft-websocket.service` |
| FR-25..27 | `DraftConflictResolver` | `draft-websocket.service` |
| FR-28..31 | `DraftAdminOverrideService` | Admin panel existant |
| FR-32..33 | `SwapValidationService` (nouveau) + `ValidationService` (existant) | Existant |
| FR-34..36 | `TradingService` (extension) | Existant |
| FR-37..40 | Existant — LeaderboardService | Existant |
| FR-41..43 | `AdminPipelineCommandService` + `AdminPipelineController` | `admin/pipeline/` |
| FR-44..47 | `AdminPipelineQueryService` + `AdminPipelineController` | `admin/pipeline/` |

---

### Failure Modes — Préventions intégrées

| Composant | Risque | Prévention |
|---|---|---|
| `ScrapingPipelineService` | Silent failure (0 lignes parsées) | `IngestionRun.status = PARTIAL` + alerte immédiate |
| `FortniteApiResolutionAdapter` | Rate limit 429 | Backoff exponentiel dans `ResolutionQueueService` |
| `DraftPickOrchestrator` | Race condition double pick | `UNIQUE(draft_id, player_id)` DB constraint |
| `DraftStateService` | Cache stale après restart | `@CacheEvict` sur `@PostConstruct` |
| `PlayerCatalogueService` | Cache froid au premier appel | Warmup `@EventListener(ApplicationReadyEvent.class)` |
| `AdminPipelineCommandService` | Double run simultané | Flag `AtomicBoolean isRunning` + 409 si actif |
| `DraftPickOrchestrator` (timer) | Timer expire sans déclenchement auto | `ScheduledExecutorService` par draft actif → pick auto à expiration |
| `SwapValidationService` | Dépendance `TeamRosterHistoryRepository` | Service extrait de `ValidationService` — SRP + CouplingTest < 7 |
| `V12__add_resolution_queue.sql` | ENUM natif incompatible H2 | `VARCHAR(20) CHECK IN (...)` |

---

## Architecture Validation Results

### Cohérence ✅

**Compatibilité des décisions :** toutes les technologies sont compatibles entre elles. Caffeine/Spring Cache, Flyway, STOMP.js/SockJS, Spring @Scheduled/@Async — zéro conflit de version ou de paradigme. La coexistence hexagonale + JPA legacy est le pattern documenté et enforced par ArchUnit.

**Consistance des patterns :** naming snake_case DB / PascalCase Java / kebab-case frontend cohérent sur tous les nouveaux composants. Tous les nouveaux endpoints suivent `/api/` + pluriel + kebab-case.

**Alignement structurel :** chaque FR est mappé à un fichier précis. Les frontières de couche sont respectées (controllers → services → ports → adapters). Aucune violation de `LayeredArchitectureTest` anticipée.

### Couverture des exigences ✅

**47 FRs couverts :** 7/7 domaines architecturalement supportés. FR-15..20 et FR-37..40 sur composants existants (zéro travail nouveau). FR-34..36 Trade = extension minimale `TradingService`.

**26 NFRs couverts :** tous les NFRs structurants ont un mécanisme documenté. Le timeout 10s API externe = `scraping.timeout.seconds=30` (timeout global scraping) + timeout OkHttp/Resilience4j sur appels Fortnite-API.com (décision équipe).

### Gaps résolus

| Gap | Décision retenue |
|---|---|
| Timer draft expiration (FR-22) | `ScheduledExecutorService` dédié par draft actif dans `DraftPickOrchestrator` |
| Swap "free player" validation (FR-32) | `SwapValidationService` extrait — `TeamRosterHistoryRepository` + règles métier isolées |
| WebSocket reconnexion | `GET /api/games/{id}/draft/state` couvre le re-sync à la reconnexion |
| Snapshot manquant | Carry-forward — dernier snapshot avant date D (documenté `docs/db-design.md`) |

### Architecture Completeness Checklist

**Analyse du contexte**
- [x] 47 FRs analysés, 7 domaines identifiés
- [x] 26 NFRs avec implications architecturales
- [x] Échelle et complexité évaluées (~8000 joueurs, 3 intégrations externes)
- [x] Contraintes brownfield documentées (ArchUnit, CouplingTest, Spotless)

**Décisions architecturales**
- [x] Stack fixée confirmée (Java 21, Spring Boot 3.3, Angular 20)
- [x] Auth JWT custom confirmée — Supabase Auth = Growth
- [x] Draft state DB-backed — multi-jours supporté
- [x] Cache Caffeine — extension CacheConfig existante
- [x] Pipeline async Spring @Scheduled + @Async
- [x] Scraping engine + circuit breaker différés équipe (POC requis)
- [x] `player.status` supprimé — concept non nécessaire
- [x] Seule nouvelle table : `resolution_queue` (Flyway V12)

**Patterns d'implémentation**
- [x] Naming conventions backend/frontend/DB
- [x] Format API réponses (direct, pas d'enveloppe)
- [x] Topics WebSocket STOMP définis
- [x] Patterns domaine hexagonal (final + static factory)
- [x] Patterns pipeline async + gestion UNRESOLVED
- [x] Checklist agent 10 points

**Structure du projet**
- [x] 23 nouveaux fichiers backend documentés
- [x] 8 nouveaux fichiers frontend documentés
- [x] 1 migration Flyway (V12)
- [x] 5 propriétés application.properties
- [x] FR → fichier mapping complet (14 lignes)
- [x] 9 failure modes avec préventions

### Architecture Readiness Assessment

**Statut global : PRÊT POUR IMPLÉMENTATION** *(sous réserve POC pipeline)*

**Niveau de confiance : Élevé**

**Points forts :**
- Schéma DB (`docs/db-design.md`) remarquablement complet — travail DB minimal restant
- Architecture hexagonale enforce par ArchUnit — agents ne peuvent pas dériver structurellement
- Patterns brownfield très établis — risque de conflit agent faible
- Failure modes anticipés avec préventions concrètes dès la conception

**Dépendance bloquante unique :**
- POC scraping FortniteTracker + résolution Fortnite-API.com — toute implémentation pipeline suspendue jusqu'à validation sur 10 joueurs EU

**Séquence d'implémentation définitive :**
1. `V12__add_resolution_queue.sql` (Flyway migration)
2. POC scraping : `ScrapingPort` + `FortniteTrackerScrapingAdapter` (10 joueurs EU)
3. POC résolution : `ResolutionPort` + `FortniteApiResolutionAdapter`
4. Pipeline complet : `ScrapingPipelineService` + `ResolutionQueueService` + `IngestionAlertService`
5. `PlayerCatalogueService` + `PlayerCatalogueController` + cache warmup
6. `DraftPickOrchestrator` + `DraftStateService` + `DraftTrancheValidator` + `DraftConflictResolver` + timer `ScheduledExecutorService`
7. `SwapValidationService` + extension `TradingService` cross-région
8. `AdminPipelineCommandService` + `AdminPipelineQueryService` + `AdminPipelineController`
9. Frontend : feature `catalogue/` + `draft-websocket.service` + `admin/pipeline/`
