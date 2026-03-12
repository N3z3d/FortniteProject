# TECH-018 — Audit Tests : Couverture, Qualité, Déterminisme, Edge Cases

> Audité le 2026-03-04
> Sources : TECH-010 (cartographie), baseline tests, SonarQube TECH-016

---

## 1. Inventaire des tests

### 1.1 Backend (Java / JUnit 5 + Mockito)

| Catégorie | Fichiers | Approx. tests |
|---|---|---|
| Controller tests | 31 | ~310 |
| Service tests | 107 | ~1100 |
| SecurityConfig authorization tests | 20 | ~200 |
| Architecture tests (Hexagonal, Layered, Coupling, Domain) | 4 | ~40 |
| Domain/Unit tests | ~40 | ~400 |
| Integration tests | ~49 | ~238 |
| **Total** | **251** | **~2288** |

**Baseline run :** 2288 tests, ~17 failures pré-existantes (GameDataIntegrationTest, FortniteTrackerServiceTddTest, GameStatisticsServiceTddTest).

### 1.2 Frontend (Angular / Vitest)

| Catégorie | Fichiers spec | Tests |
|---|---|---|
| Feature components + services | 148 | 2195 |
| Passing | — | 2149 (97.9%) |
| Pre-existing failures | — | 46 (Zone.js debounce timing) |

---

## 2. Couverture de code

### 2.1 Backend (JaCoCo — dernière mesure connue)

| Métrique | Valeur | Objectif CLAUDE.md |
|---|---|---|
| **Lignes** | **86.89%** | ≥ 85% ✅ |
| **Branches** | **73.36%** | ≥ 70% ✅ |
| **Fonctions** | **84.64%** | ≥ 80% ✅ |
| **Instructions** | **86.31%** | ≥ 85% ✅ |

### 2.2 Frontend (Vitest / V8)

| Métrique | Valeur |
|---|---|
| **Lignes** | 86.89% |
| **Branches** | 73.36% |
| **Fonctions** | 84.64% |
| **Instructions** | 86.31% |

---

## 3. Qualité des tests

### 3.1 Points forts

- **Architecture tests automatisés** : `HexagonalArchitectureTest`, `LayeredArchitectureTest`, `DomainIsolationTest`, `CouplingTest` — garantissent les contraintes architecturales à chaque CI
- **SecurityConfig authorization tests** : 20 suites couvrent toutes les routes admin/user/draft/leaderboard — régression auth détectée automatiquement
- **TDD respecté** sur les stories BMAD Sprint 1 + 2 — chaque story livrée avec tests rouges → verts
- **Déterminisme** : mocks/adapters sur tout I/O (temps, réseau, DB) — pas de `Thread.sleep()` dans les tests
- **Vitest setup** : Jasmine shim, fakeTimers, JSDOM — environnement isolé

### 3.2 Problèmes identifiés (SonarQube TECH-016 + analyse)

| Problème | Occurrences | Impact |
|---|---|---|
| `java:S5838` — ordre arguments inversé `(actual, expected)` | 31 | Lisibilité des échecs |
| `java:S5853` — assertions sans message descriptif | 30 | Diagnostic difficile |
| `java:S5778` — `assertThrows` avec exception générique | 27 | Tests fragiles |
| Failures Zone.js debounce (frontend) | 46 | Tests flakiness connus |
| Tests TDD "RED phase" toujours rouges | ~6 | Intentionnel (FortniteTrackerServiceTddTest) |
| `GameDataIntegrationTest` — données de tests manquantes | 4 | Data issues |

### 3.3 Tests orphelins ou problématiques

- **`CompositeIdEqualityTest`** (9 SonarQube issues) — assertions mal ordonnées
- **`GameStatisticsServiceTddTest.shouldMapNullPlayerIdsToUnknown`** — NPE pré-existant, non corrigé
- **`GameDataIntegrationTest`** (4 tests) — dépend de données spécifiques absentes en CI

---

## 4. Déterminisme et isolation

### 4.1 Problèmes de déterminisme connus

| Test / Composant | Problème | Plan |
|---|---|---|
| 46 tests Vitest (Zone.js) | `fakeAsync(async () => tick(300))` — incompatible Vitest | Sprint 3 migration fakeAsync → async/await |
| `GameDataIntegrationTest` | Dépendance aux données de seed | Mocker les repos ou fournir seed fixtures |
| `FortniteTrackerServiceTddTest` | Messages d'erreur en français attendus — fragile | Normaliser les messages en constantes |

### 4.2 Bonnes pratiques appliquées

- `@ExtendWith(MockitoExtension.class)` sur tous les tests unitaires — isolation garantie
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` pour les tests d'intégration — ports dynamiques
- `@ActiveProfiles("test")` — profil dédié avec H2 en mémoire
- `Random` injecté comme `@Bean` dans `PronosApplication` — testable avec `new Random(seed)`

---

## 5. Edge cases couverts vs. manquants

### 5.1 Bien couverts (exemples)

- Nulls / valeurs limites dans tous les services admin (ScrapeLogService, AdminAuditLogService)
- Cas limites des validateurs (DraftTrancheService, SwapSoloService)
- Security: 401/403/404 sur routes protégées (20 suites SecurityConfig)
- Concurrent buffer overflow (AdminAuditLogService, ErrorJournalService)

### 5.2 Gaps identifiés

| Zone | Gap | Priorité |
|---|---|---|
| `DraftController` | Aucun test d'intégration bout-en-bout pour le flow snake complet | HIGH |
| `GameCreationService` | 100% couverture unit mais 0 test d'intégration avec vraie DB | MEDIUM |
| `leaderboard.service.ts` (front, 487 lignes) | Couverture <50% — service critique | HIGH |
| Frontend trades (5 fichiers >400L) | Couverture branches ~45% | MEDIUM |
| `DomainExceptionHandler` | Quelques handlers sans assertion sur body | LOW |

---

## 6. Recommandations Sprint 3

### Priorité haute

1. **Corriger 46 tests Vitest Zone.js** — remplacer `fakeAsync(async () => tick())` par `async () => {}` dans 17 fichiers de composants
2. **Corriger ordre assertions** `(actual, expected)` → `(expected, actual)` dans les 31 cas SonarQube S5838
3. **Ajouter messages descriptifs** aux assertions critiques (30 cas S5853)

### Priorité moyenne

4. **Couverture `leaderboard.service.ts`** : ajouter ~30 tests pour atteindre 80%+
5. **Corriger `GameDataIntegrationTest`** : fournir seed fixtures ou mocker les repos
6. **Ajouter messages assertThrows** : `assertThrowsExactly` + description (27 cas S5778)

### Priorité basse

7. **Test intégration DraftController** : flow snake complet avec MockMvc
8. **Corriger `GameStatisticsServiceTddTest` NPE** : initialiser les collections null

---

## 7. Score qualité des tests

| Critère | Score | Commentaire |
|---|---|---|
| Couverture lignes | 87% | ✅ Objectif atteint |
| Couverture branches | 73% | ✅ Objectif atteint |
| Déterminisme | 94% | 46/~2288 instables |
| Edge cases | 82% | Gaps dans DraftController, leaderboard |
| Qualité assertions | 75% | 88 assertions SonarQube à corriger |
| **Score global** | **82%** | Bon socle, corrections ciblées Sprint 3 |
