---
project_name: 'FortniteProject'
user_name: 'Thibaut'
date: '2026-02-21'
sections_completed: ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'quality_rules', 'workflow_rules', 'anti_patterns', 'definition_of_done']
status: 'complete'
rule_count: 53
optimized_for_llm: true
---

# Project Context for AI Agents — FortniteProject

_Règles critiques et patterns que les agents IA doivent respecter lors de toute implémentation de code. Ce fichier contient les détails non-évidents que les LLMs oublieraient sans ce contexte._

---

## 1. Stack Technologique & Versions

### Backend
| Technologie | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| Spring Security | (inclus Spring Boot) |
| Spring Data JPA | (inclus Spring Boot) |
| PostgreSQL driver | 42.7.3 |
| Flyway | 10.11.0 |
| JWT (jjwt) | 0.12.5 |
| Lombok | 1.18.32 |
| OpenCSV | 5.11.1 |
| ArchUnit | 1.2.1 |
| H2 (test uniquement) | (inclus Spring Boot) |

### Frontend
| Technologie | Version |
|---|---|
| Angular | ^20.0.0 |
| Angular Material | ^20.0.3 |
| Angular CDK | ^20.0.0 |
| RxJS | ~7.8.0 |
| TypeScript | ~5.8.2 |
| Vitest | 3.x (Karma/Jasmine supprimés Sprint 3) |
| Chart.js + ng2-charts | 4.5 / 8.0 |
| STOMP.js + SockJS | 7.2.1 / 1.6.1 |
| zone.js | ~0.15.0 |

---

## 2. Architecture

### Backend — Architecture Hexagonale (en migration)
```
src/main/java/com/fortnite/pronos/
├── domain/           ← Modèles purs (PAS de JPA, PAS de Spring, PAS de Lombok)
│   ├── game/model/   ← Game, GameParticipant, GameLifecycle…
│   ├── player/model/ ← Player
│   ├── team/model/   ← Team, TeamMember
│   ├── trade/model/  ← Trade, TradeStatus
│   ├── draft/model/  ← Draft, DraftStatus
│   └── port/out/     ← Interfaces de port (ex: GameRepositoryPort)
├── adapter/
│   ├── in/           ← (controllers en cours de migration)
│   └── out/persistence/ ← Mappers JPA + adapters repository
├── controller/       ← Controllers REST (@RestController)
├── service/          ← Services métier (en cours de migration vers ports)
├── model/            ← Entités JPA legacy (coexistent pendant migration)
├── repository/       ← Spring Data JPA repositories legacy
├── dto/              ← DTOs de requête/réponse
├── config/           ← Configuration Spring (Security, Cache, etc.)
└── exception/        ← Exceptions métier
```

### Frontend — Architecture par features
```
frontend/src/app/
├── core/
│   ├── repositories/   ← Accès HTTP bas niveau
│   ├── services/       ← Services transversaux (auth, i18n, navigation…)
│   └── utils/          ← Utilitaires partagés
├── features/           ← Modules feature (admin, dashboard, draft, game…)
│   └── [feature]/
│       ├── components/ ← Sous-composants
│       ├── services/   ← Services feature-specific
│       ├── models/     ← Interfaces/types feature
│       └── mappers/    ← Mappers API → modèle
└── shared/
    ├── components/     ← Composants UI réutilisables
    ├── models/         ← Modèles partagés
    └── services/       ← Services partagés (notification, focus…)
```

---

## 3. Règles Critiques Backend

### Domaine pur — OBLIGATOIRE
- Les classes dans `domain/` sont **`final`** et **sans annotations JPA/Spring/Lombok**
- Utiliser le pattern **static factory** : `Game.restore(...)` pour reconstituer depuis persistence
- `DomainIsolationTest` scanne automatiquement `com.fortnite.pronos.domain..` — tout nouveau package est auto-détecté
- **Loi de Demeter** : jamais de chaînes `a.getB().getC().doSomething()`

### Repositories & Soft Delete
- Seule l'entité `Game` a le soft delete via le champ `deletedAt`
- **Ne pas renommer les méthodes** de `GameRepository` — utiliser `@Query` pour filtrer `AND g.deletedAt IS NULL`
- `GameRepository` étend à la fois `JpaRepository<Game, UUID>` et `GameRepositoryPort` → ambiguïté sur `save()`/`findById()` : utiliser un champ typé `JpaRepository<Game, UUID>` pour désambiguïser

### Services — Couplage
- **Maximum 7 dépendances injectées** par `@Service` (enforced par `CouplingTest`)
- Si dépassement : déléguer vers des services existants

### Exceptions & Handlers
- `DomainExceptionHandler` (`@Order(1)`) : exceptions domaine-spécifiques
- `GlobalExceptionHandler` (`@Order(2)`) : exceptions communes/infra
- `AlreadyInGameFallbackHelper` : cas spécifique UserAlreadyInGameException (409)
- Chaque classe doit rester **< 500 lignes**

### Architecture Tests (ArchUnit)
- `HexagonalArchitectureTest` : domain → pas de JPA/Spring/Hibernate/Lombok
- `LayeredArchitectureTest` : controllers peuvent accéder au domain ; controllers **ne peuvent PAS** dépendre de repositories directement
- `CouplingTest` : max 7 deps par @Service
- `DependencyInversionTest` : mappers nécessitant un accès repo → package `service`, pas `controller`
- **Toujours lancer** `mvn spotless:apply` avant `mvn test` sur tout nouveau fichier

### @ConditionalOnProperty — Beans optionnels

Utiliser pour tout `@Service` devant être **désactivé par défaut** (jobs planifiés, adapters externes, feature flags).

```java
@Service
@ConditionalOnProperty(
    name = "feature.enabled",
    havingValue = "true",
    matchIfMissing = false)   // false = bean ABSENT si propriété non définie
public class MyOptionalService { ... }
```

**Injection dans les consumers :**
```java
// Spring injecte Optional.empty() si le bean est absent
private final Optional<MyOptionalService> myService;

public MyController(Optional<MyOptionalService> myService) {
    this.myService = myService;
}

// Usage
if (myService.isEmpty()) {
    return ResponseEntity.status(503).body("Feature disabled");
}
```

**Règles critiques :**
- Dans `@WebMvcTest` : le bean conditionnel est absent → `Optional.empty()` auto-injecté, **pas besoin de `@MockBean`** pour le bean absent
- Si `@Service` a 2 constructeurs (prod DI + package-private pour tests) → annoter le constructeur prod avec **`@Autowired`** (sinon Spring lève "No default constructor found")
- **Référence canonique** : `PrIngestionOrchestrationService` (`@ConditionalOnProperty ingestion.pr.scheduled.enabled`) + `AdminScrapeController` (`Optional<PrIngestionOrchestrationService>`)

### Tests Backend
- Baseline : **~2355 tests** (15 failures + 1 error pré-existants — voir §Pre-existing Failures Baseline)
- Tests H2 en mémoire pour les tests unitaires/intégration
- `TestSecurityConfig`, `TestPasswordEncoderConfig`, `TestPrometheusConfig` pour l'isolation de tests

---

## 4. Règles Critiques Frontend

### Injection de dépendances — OBLIGATOIRE
- Utiliser **`inject()`** pour les champs dans les composants/services standalone
- **Exception** : pour les services instanciés avec `new Service()` dans les tests → utiliser `@Optional()` constructor params au lieu de `inject()`
- `TranslationService` : toujours injecté comme **`public readonly t`**, utilisé dans les templates via `t.t('key')`

### Composants Standalone
- Tous les composants sont **standalone** (Angular 19+)
- Pas de NgModule — tout se configure dans `app.config.ts`
- Patterns DI : `inject(Service)` dans les champs de classe

### TypeScript — Strict Mode
- `strict: true`, `noImplicitReturns: true`, `noFallthroughCasesInSwitch: true`
- `strictTemplates: true` côté Angular compiler
- `isolatedModules: true`

### SCSS
- Import des mixins gaming : `@import '../../shared/styles/mixins'` (chemin relatif depuis le composant)
- Thème gaming avec variables CSS custom

### i18n
- 4 langues : `fr.json`, `en.json`, `es.json`, `pt.json` dans `src/assets/i18n/`
- Isolation par utilisateur : clés localStorage préfixées par userId (`TranslationService.setCurrentUserId()`)
- Toute nouvelle string visible → ajouter dans les **4 fichiers** i18n

### RxJS & BehaviorSubject
- Après `.complete()` sur un Subject : vérifier avec `.isStopped` (PAS `.closed`)
- Préférer `async` pipe dans les templates pour éviter les memory leaks

### Tests Frontend
- Framework : **Vitest** (Karma/Jasmine supprimés Sprint 3)
- Commande CI unit : `npm run test:vitest` (depuis `frontend/`)
- Commande CI e2e : `npm run test:e2e` (Playwright — requiert app sur :4200 + backend sur :8080)
- Baseline : **2206 tests, 2185 passing** (21 Zone.js pre-existing — Sprint 10; Sprint 7-Z1 avait atteint 2243/0)
- Coverage actuel : Lines 86.89%, Branches 73.36%, Functions 84.64%
- Certains composants ont des fichiers `.template.spec.ts` ET `.spec.ts` — vérifier les deux
- Le **linter** modifie les fichiers automatiquement au save → relire avant d'éditer si du temps est passé

### Conversion fakeAsync → Vitest (Pattern A et B)

`fakeAsync()`+`tick()` est incompatible avec Vitest/jsdom. Deux patterns selon le cas :

**Pattern A** — Observable synchrone (spy retourne `of(value)`) :
```typescript
// AVANT (incompatible Vitest)
it('should load', fakeAsync(() => {
  component.ngOnInit();
  tick(300);
  fixture.detectChanges();
  expect(component.data).toBeTruthy();
}));

// APRÈS
it('should load', async () => {
  component.ngOnInit();
  fixture.detectChanges();
  expect(component.data).toBeTruthy();
});
```

**Pattern B** — `debounceTime()` réel (le service utilise vraiment debounceTime) :
```typescript
// AVANT (incompatible Vitest)
it('should debounce search', fakeAsync(() => {
  component.searchControl.setValue('test');
  tick(300);
  fixture.detectChanges();
  expect(service.search).toHaveBeenCalledWith('test');
}));

// APRÈS
it('should debounce search', async () => {
  vi.useFakeTimers();
  component.searchControl.setValue('test');
  vi.advanceTimersByTime(300);
  fixture.detectChanges();
  expect(service.search).toHaveBeenCalledWith('test');
  vi.useRealTimers();
});
```

> `vi` est global dans ce projet (`globals: true` dans `vitest.config.mts`) — pas besoin d'import.
> Retirer les imports `fakeAsync`, `tick` de `@angular/core/testing` si plus aucun usage dans le fichier.

### Pre-existing Failures Baseline

Toute nouvelle story DOIT documenter les failures pré-existantes dans "Pre-existing Gaps / Known Issues" :

**Backend** (exclus du CI via `-Dexcludes`) :
- `GameDataIntegrationTest` — 4 failures, fixtures DB absentes en CI
- `FortniteTrackerServiceTddTest` — 6 failures, API externe non mockée (RED TDD intentionnel)
- `PlayerServiceTddTest` — 1 failure, RED TDD intentionnel
- `ScoreCalculationServiceTddTest` — 2 failures, RED TDD intentionnel
- `ScoreServiceTddTest` — 3 failures, RED TDD intentionnel
- `GameStatisticsServiceTddTest` — 1 NPE pré-existante non liée aux stories

**Frontend** : 0 failure pré-existante (baseline Sprint 7 — 2243/2243 verts)

**Règle** : si une story introduit un nouveau test rouge involontaire → c'est une régression à corriger.

---

## 5. Conventions de Nommage

### Backend
| Type | Convention | Exemple |
|---|---|---|
| Classes domaine | PascalCase | `GameParticipant`, `TradeStatus` |
| Services | PascalCase + Suffix | `GameCreationService`, `DraftService` |
| Controllers | PascalCase + Controller | `GameController`, `AdminDashboardController` |
| Ports | PascalCase + Port | `GameRepositoryPort`, `PlayerRepositoryPort` |
| Adapters | PascalCase + Adapter | `GameRepositoryAdapter` |
| Mappers | PascalCase + Mapper | `GameEntityMapper`, `TradeResponseMapper` |
| Tests | NomClasse + Test/TddTest | `GameServiceTddTest`, `GameControllerTest` |

### Frontend
| Type | Convention | Exemple |
|---|---|---|
| Composants | kebab-case (fichier) | `game-detail.component.ts` |
| Services | kebab-case (fichier) | `draft-state-helper.service.ts` |
| Interfaces | PascalCase | `DraftInterface`, `GameDto` |
| Specs | même nom + .spec.ts | `game-detail.component.spec.ts` |

---

## 6. Definition of Done (DoD)

Une story est **done** uniquement si **tous** les critères ci-dessous sont satisfaits :

### Critères obligatoires

| Critère | Règle |
|---|---|
| **Tests unitaires** | Tous les cas nominaux + ≥ 3 edge cases couverts ; 0 test rouge lié à la story |
| **Pas de régression** | Suite complète verte avant merge (backend `mvn verify -Dexcludes=<pre-existing>` + frontend `npm run test:vitest`) |
| **Coverage** | ≥ 85% lignes sur le code modifié par la story |
| **Sécurité** | Si nouveau `@RestController` → `SecurityConfig<ControllerName>AuthorizationTest` créé |
| **i18n** | Toute string visible ajoutée dans les 4 fichiers (`fr.json`, `en.json`, `es.json`, `pt.json`) |
| **Spotless** | `mvn spotless:apply` lancé avant tout commit backend |
| **Taille** | Aucune classe > 500 lignes, aucune méthode > 50 lignes |
| **File List** | Section "File List" de la story remplie avec tous les fichiers modifiés/créés/supprimés |
| **Code review** | Workflow `bmad-bmm-code-review` exécuté — tous les findings HIGH/MEDIUM résolus avant passage en `done` |
| **Sprint status** | Ticket passé à `done` dans `sprint-status.yaml` après code review validé |
| **Loi de Demeter** | Aucune chaîne `a.getB().getC().doSomething()` introduite |
| **Dockerfile** | Si `Dockerfile`, `tsconfig.app.json` ou `angular.json` modifié : `docker build . --target production` doit passer sans erreur |

### Adapter externe — Checklist supplémentaire

Obligatoire pour toute story ajoutant un adapter vers un service externe (scraping, API tierce, etc.) :

| Étape | Règle |
|---|---|
| **Port interface** | Définir dans `service/` ou package port approprié — pas de dépendance directe dans le service métier |
| **Mock adapter** | Fournir un mock/stub pour tests et dev local — zéro appel réseau réel en CI |
| **Dry-run d'abord** | Endpoint `/dry-run` validé avant toute activation scheduler en production |
| **Smoke check** | Validation du seuil minimum de données (ex: `rows >= 10`) dans l'orchestrateur |
| **Env vars commentées** | `application.properties` documente les clés via commentaires — jamais de vraie clé en valeur par défaut |
| **@ConditionalOnProperty** | Tout bean devant être OFF par défaut utilise `matchIfMissing = false` |
| **CGU vérifiées** | ToS du service tiers vérifiée et documentée dans `project-context.md` §Config Production |

### Critères bloquants (HALT)

- Code review non exécuté → NE PAS passer en `done` dans sprint-status.yaml
- Story avec `[AI-Review][HIGH]` ou `[AI-Review][MEDIUM]` ouverts → NE PAS passer en `done`
- Dev Agent Record `File List` vide → NE PAS passer en `review` (bloquer le workflow)
- Test rouge dans la suite pre-existing → signaler, ne pas masquer
- Nouveau `@Service` avec > 7 dépendances → refactoriser d'abord

---

## 7. Workflow de Développement

### Processus TDD obligatoire
1. **Red** : écrire le test qui échoue
2. **Green** : implémenter le minimum pour passer
3. **Refactor** : nettoyer sans casser

### Limites de taille
- **Max 500 lignes par classe** (backend ET frontend)
- **Max 50 lignes par méthode**
- Si dépassement → extraire des classes/méthodes

### Backlogs & Tickets
- Source de vérité unique : **`Jira-tache.txt`** à la racine du projet
- Format des commits : `type(scope): description` (ex: `feat(game): ...`, `fix(draft): ...`)

### Spotless (Backend)
- **Toujours** lancer `mvn spotless:apply` avant `mvn test` sur tout nouveau fichier Java
- Spotless s'exécute dans la phase `validate` et fait échouer les tests si non appliqué

---

## 8. Pièges Connus à Éviter

| Piège | Règle |
|---|---|
| Linter modifie les fichiers au save | Relire le fichier avant d'éditer si du temps est passé |
| `inject()` hors contexte DI | Utiliser `@Optional()` constructor params dans les services testés avec `new` |
| Ambiguïté `GameRepository.save()` | Utiliser un champ typé `JpaRepository<Game, UUID>` |
| Ajout classe dans `domain/` avec Lombok/JPA | Interdit — domaine pur uniquement |
| Nouvelle string UI sans i18n | Ajouter dans les 4 fichiers JSON |
| `@Service` avec > 7 deps | Déléguer vers services existants |
| Soft delete non filtré sur Game | Toujours filtrer `deletedAt IS NULL` via `@Query` |
| Commit sans spotless | Lancer `mvn spotless:apply` d'abord |
| Variable zombie dans spec E2E | `const x = await locator.textContent()` sans `expect(x)` → signalé en code review |
| Boilerplate goto+wait+URL-check dans E2E | Utiliser `waitForPageReady(page, route)` depuis `e2e/helpers/app-helpers.ts` |

---

## §WebSocket Security Pattern (Sprint 8 — SEC-R2)

### Contexte

SockJS ne supporte pas les **headers HTTP personnalisés** au handshake. L'approche standard `Authorization: Bearer <token>` est impossible. La seule solution viable est le **query param** au moment de la création de la connexion SockJS.

### Pattern de référence (implémenté Sprint 8)

**Backend — `JwtHandshakeInterceptor`** (`config/JwtHandshakeInterceptor.java`) :
```java
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && jwtUtil.validateToken(token)) {
                attributes.put("username", jwtUtil.extractUsername(token));
                return true;
            }
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }
}
```

**Frontend — `WebSocketService`** :
```typescript
const token = this.authService.getToken();
const sockjs = new SockJS(`${environment.wsUrl}?token=${encodeURIComponent(token ?? '')}`);
```

### Comportement actuel du chemin de rejet

⚠️ **Décision documentée** : Le rejet d'une connexion sans JWT valide passe par `IllegalStateException → STOMP ERROR frame` côté client, et **non** un HTTP 401 propre au handshake.

- **Pourquoi** : `JwtHandshakeInterceptor.beforeHandshake()` retourne `false` + `response.setStatusCode(UNAUTHORIZED)`, mais SockJS intercepte et encapsule la réponse, ce qui entraîne un STOMP ERROR au lieu d'un HTTP 401 visible.
- **Impact** : Le message d'erreur côté frontend n'est pas explicite — l'erreur doit être traitée dans le callback `onStompError` du client STOMP.
- **Décision** : Comportement conservé tel quel (fonctionnel). Correction HTTP 401 propre non prioritaire.

### Pour les futures stories WebSocket

Lors de la création d'une story de sécurité WebSocket, définir explicitement dans les ACs :
1. Le code de rejet (HTTP au handshake OU STOMP ERROR frame)
2. Le message côté client et son handling dans `WebSocketService`
3. Si `onStompError` doit déclencher une déconnexion / notification utilisateur

---

## §E2E Tests — Limitations et Patterns (Sprint 10)

### Prérequis

Les tests E2E Playwright **ne peuvent pas tourner sans le stack Docker local** :
```bash
docker-compose -f docker-compose.local.yml up -d   # app :8080, Angular :4200
npm run test:e2e                                    # depuis frontend/
```
Sans le stack, les fichiers `.spec.ts` compilent proprement mais les tests échouent immédiatement.

### Pattern multi-contexte (`browser.newContext()`)

Pour simuler **deux utilisateurs simultanés** sur la même page, utiliser `browser.newContext()` et non `browser.newPage()` :

| | `browser.newPage()` | `browser.newContext()` |
|---|---|---|
| Session partagée | ✅ même sessionStorage/cookies | ❌ sessions isolées |
| Deux utilisateurs | ❌ impossible | ✅ chacun son propre login |
| Usage | Onglets supplémentaires pour un même user | Simulations multi-user (WS, STOMP) |

```typescript
// ✅ CORRECT — deux sessions indépendantes
const contextA = await browser.newContext();
const pageA = await contextA.newPage();
await forceLoginWithProfile(pageA, 'thibaut');

const contextB = await browser.newContext();
const pageB = await contextB.newPage();
await forceLoginWithProfile(pageB, 'teddy');
```

### Timing STOMP — caveat critique

`#player-list` visible **ne garantit pas** que la subscription STOMP est active. Le composant Angular souscrit en `ngOnInit` via `WebSocketService.subscribeToDraft()` — le handshake SockJS est asynchrone.

**Règle** : avant de déclencher un event WS depuis Context A, attendre que Context B ait rendu au moins une **player-card** (data loadée = subscription active) :

```typescript
// ❌ insuffisant — DOM visible mais WS pas encore souscrit
await expect(pageB.locator('#player-list')).toBeVisible({ timeout: 15_000 });

// ✅ meilleure heuristique — les cartes sont rendues via HTTP après ngOnInit,
// ce qui réduit la fenêtre de timing avant que la subscription STOMP soit active.
// Pas une garantie absolue, mais suffisant en pratique sur stack local.
await expect(pageB.locator('.player-card').first()).toBeVisible({ timeout: 10_000 });
```

### Autres patterns E2E établis

- **`X-Test-User` header** : doit être défini par contexte via `page.context().setExtraHTTPHeaders({ 'X-Test-User': username })` — `forceLoginWithProfile()` le fait automatiquement
- **`SUITE_PREFIX`** : chaque suite utilise son propre préfixe pour le cleanup (`'E2E-WS-'`, `'E2E-DRAFT-82-'`, etc.) — évite les collisions entre suites
- **`resolveCurrentPickerUsername()`** : déterminer dynamiquement qui est le premier picker via API plutôt qu'assumer un ordre fixe
- **`expect.poll` intervals** : définir un tableau complet pour éviter le comportement indéfini après épuisement : `[500, 1_000, 2_000, 2_000, 2_000, 2_000, 2_000]`
- **Confirmation du pick** : après `btn-confirm.click()`, vérifier que `.confirm-zone` disparaît avant de poller l'observer — sinon un échec silencieux de click masque le vrai problème

---

## §Config Production — Pipeline FortniteTracker (Sprint 10)

### Variables d'environnement

| Variable | Défaut | Description |
|---|---|---|
| `INGESTION_PR_SCHEDULED_ENABLED` | `false` | Active le bean `PrIngestionOrchestrationService` (`@ConditionalOnProperty`) |
| `INGESTION_PR_SCHEDULED_CRON` | `0 0 5 * * *` | Cron de déclenchement (05:00 UTC quotidien) |
| `SCRAPING_FORTNITETRACKER_PAGES_PER_REGION` | `1` | Pages par région (1 = ~100 rows, dev/dry-run) ; mettre `4` pour scrape production complet |
| `SCRAPING_FORTNITETRACKER_SCRAPFLY_KEYS` | _(vide)_ | Clés Scrapfly séparées par virgule — ne jamais commiter |
| `SCRAPING_FORTNITETRACKER_SCRAPERAPI_KEYS` | _(vide)_ | Clés ScraperAPI séparées par virgule — ne jamais commiter |
| `SCRAPING_FORTNITETRACKER_SCRAPEDO_TOKEN` | _(vide)_ | Tokens Scrape.do séparés par virgule — ne jamais commiter |

### Activation en production — séquence obligatoire

```
1. Configurer au moins un set de clés proxy (Scrapfly recommandé)
2. Lancer POST /api/admin/scraping/dry-run?region=EU  → vérifier rows >= 10 et score dans [0, 10M]
3. Si dry-run OK → définir l'env var `INGESTION_PR_SCHEDULED_ENABLED=true` (Spring Boot relaxed binding l'applique automatiquement — pas besoin de modifier application.properties)
4. Vérifier les logs 05h00 UTC J+1 via GET /api/admin/scraping/logs
```

**Référence** : `src/main/resources/application.properties` (lignes commentées sections scraping + scheduler)

### §FortniteTracker ToS — Statut vérification

**Plateforme** : FortniteTracker.com est opéré par Tracker Network / tracker.gg

**Résultat de la vérification (2026-03-18)** :
- La page `tracker.gg/legal/terms-of-service` retourne HTTP 403 aux fetches automatisés — la ToS n'a **pas pu être lue directement**
- tracker.gg expose une **API officielle** (`tracker.gg/developers`) avec API key gratuite — voie légitime préférable au scraping HTML
- ⚠️ **Inférence non-vérifiée** : la ToS tracker.gg n'a pas été lue (403). Les ToS des plateformes de ce type *incluent généralement* une clause restreignant la collecte automatisée sans permission écrite — **ne pas supposer que c'est autorisé** ; vérifier manuellement avant toute montée en charge
- L'architecture proxy (Scrapfly/ScraperAPI/Scrape.do) scrape des **pages publiques** (pas derrière auth) — zone grise légale non résolue

**Recommandations** :
1. **Migration API officielle** : priorité haute — passer de HTML scraping à `tracker.gg/developers` API dès que le volume le permet (API key gratuite disponible)
2. **Revue trimestrielle** : vérifier manuellement `tracker.gg/legal` chaque trimestre — les ToS évoluent
3. **Volume limité** : `SCRAPING_FORTNITETRACKER_PAGES_PER_REGION=1` par défaut limite l'impact (≤ 8 req HTTP/jour pour 8 régions × 1 page ; ~800 lignes de données)
4. **Pas de données personnelles** : seuls les classements publics (nickname, rang, points) sont collectés — pas d'email, profil privé, etc.

---

## Usage Guidelines

**Pour les agents IA :**
- Lire ce fichier AVANT d'implémenter tout code dans ce projet
- Respecter TOUTES les règles exactement comme documentées
- En cas de doute, préférer l'option la plus restrictive
- Mettre à jour ce fichier si de nouveaux patterns émergent

**Pour Thibaut :**
- Garder ce fichier lean — centré sur les besoins des agents
- Mettre à jour lors de changements de stack technologique
- Réviser trimestriellement pour supprimer les règles obsolètes

_Last Updated: 2026-03-18 — Sprint 10 P0 docs: §3 @ConditionalOnProperty pattern + Optional injection; §4 baseline 2206/2185; §6 Adapter externe checklist (7 items); §E2E Limitations (multi-context, STOMP timing, SUITE_PREFIX); §Config Production FortniteTracker (env vars, activation séquence, ToS statut)._
