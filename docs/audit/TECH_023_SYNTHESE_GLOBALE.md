# TECH-023 — Synthèse Globale des Audits & Roadmap Sprint 3

> Consolidé le 2026-03-04
> Dépend de : TECH-010, TECH-016, TECH-017A/B/C/D/E/F, TECH-018, TECH-020
> Sources : 12 rapports d'audit — TECH-010 à TECH-022 + TECH-017A–F

---

## 1. Vue d'ensemble — Scores par dimension

| Dimension | Rapport | Score | Tendance | Priorité sprint 3 |
|---|---|---|---|---|
| **Architecture hexagonale** | TECH-015 | 🟡 71% | ↑ (était 55%) | HIGH |
| **SOLID** | TECH-014 | 🟡 86% | ↑ (était 65%) | MEDIUM |
| **Sécurité** | TECH-017F | 🟢 57/60 | ↑ (était ~40/60) | LOW (résidus) |
| **Tests** | TECH-018 | 🟡 82% | → stable | MEDIUM |
| **Stack technique** | TECH-020 | 🔴 6/10 | → | HIGH |
| **SonarQube** | TECH-016 | 🔴 ERROR | → | MEDIUM |
| **DRY** | TECH-019 | 🔴 4.5/10 | → | HIGH |
| **Conventions/Lint** | TECH-021 | 🟡 ~75% | → | LOW |
| **Loi de Demeter** | TECH-022 | 🟡 ~80% | → | LOW |
| **Clean Code (taille)** | TECH-013 | 🟡 ~80% | ↑ | MEDIUM |
| **Score global** | — | **🟡 72%** | ↑ depuis ~58% | — |

---

## 2. Dettes techniques — Inventaire priorisé

### 2.1 CRITIQUES — Bloquants qualité/sécurité (P0)

| ID | Zone | Problème | Rapport | Effort |
|---|---|---|---|---|
| **P0-1** | Stack | Spring Boot 3.3.0 EOL (juin 2025) — pas de patches sécu | TECH-020 | 2h |
| **P0-2** | CI/CD | Aucun pipeline build+test — régressions invisibles en CI | TECH-020 | 3h |
| **P0-3** | Stack | JWT_SECRET avec fallback hardcodé dans `application.yml` | TECH-020 | 5 min |
| **P0-4** | DRY | `SecurityTestBeans` dupliqué dans 14 fichiers de test | TECH-019 | 2h |
| **P0-5** | DRY | Helpers `safeInt`/`safeBool` copiés dans 5 mappers | TECH-019 | 1h |
| **P0-6** | Architecture | 30+ services toujours sur JPA direct (DIP violé) | TECH-015 | Continu |

### 2.2 MAJEURS — Dette qualité significative (P1)

| ID | Zone | Problème | Rapport | Effort |
|---|---|---|---|---|
| **P1-1** | Tests | 46 tests Vitest Zone.js instables (fakeAsync incompatible) | TECH-018 | 4h |
| **P1-2** | SonarQube | 141 magic numbers (`java:S109`) | TECH-016 | 4h |
| **P1-3** | SonarQube | 88 violations assertions tests (S5838+S5853+S5778) | TECH-016 | 3h |
| **P1-4** | SOLID | `DraftController` 657 lignes + corruption UTF-8 (SRP) | TECH-014 | 3h |
| **P1-5** | SOLID | `DraftService.java` 509 lignes — Strategy manquant pour DraftMode | TECH-014 | 2h |
| **P1-6** | Stack | Node.js 20 EOL avril 2026 — CI + `@types/node` | TECH-020 | 30 min |
| **P1-7** | Stack | `management.endpoint.health.show-details: always` en prod | TECH-020 | 15 min |
| **P1-8** | Tests | Couverture `leaderboard.service.ts` < 50% (487 lignes critiques) | TECH-018 | 3h |
| **P1-9** | DRY | handleError/resolveErrorMessage copié dans 3 services Angular | TECH-019 | 2h |
| **P1-10** | Architecture | `Game.java` (551L) doublon domain/legacy — confusion critique | TECH-010 | Sprint dédié |

### 2.3 MINEURS — Amélioration progressive (P2)

| ID | Zone | Problème | Rapport | Effort |
|---|---|---|---|---|
| **P2-1** | SonarQube | 36 méthodes avec trop de `return` (S1142) | TECH-016 | 2h |
| **P2-2** | SonarQube | 22 `String.toLowerCase()` sans `Locale` (S1449) | TECH-016 | 30 min |
| **P2-3** | Sécurité | Rate limiting absent sur `/api/auth/**` | TECH-017F | 2h |
| **P2-4** | Sécurité | WebSocket `/ws/**` en `permitAll()` | TECH-017F | 1h |
| **P2-5** | Stack | Flyway 10.11.0 → 11.x | TECH-020 | 2h |
| **P2-6** | Stack | `dependency-check-maven` 9.0.9 → 12.1.0 | TECH-020 | 30 min |
| **P2-7** | Tests | `GameDataIntegrationTest` (4 tests) — données manquantes | TECH-018 | 1h |
| **P2-8** | SOLID | `GameDomainRepositoryPort` ~12 méthodes → split read/write | TECH-014 | 1h |
| **P2-9** | Conventions | Incohérences naming (quelques classes sans suffix Service) | TECH-021 | 1h |
| **P2-10** | DRY | Interfaces `Player` TypeScript x3 variantes non unifiées | TECH-019 | 1h |

---

## 3. Analyse des tendances

### 3.1 Ce qui va bien ✅

| Zone | Constat |
|---|---|
| **Sécurité applicative** | 57/60 — JWT, HSTS, X-Frame-Options, CSRF stateless correct, 0 vuln npm, .env sécurisé |
| **Couverture de tests** | Lines 86.89% / Branches 73.36% — objectifs CLAUDE.md atteints |
| **Architecture domain** | 5/8 domaines migrés, domain models 100% purs (pas de JPA/Spring) |
| **Controllers** | 100% conformes — aucun accès direct aux repos |
| **Tests déterminisme** | 94% — mocks/adapters sur tout I/O, pas de Thread.sleep() |
| **SOLID global** | 86% — en progression depuis 65% en début Sprint 1 |
| **Tests d'architecture** | HexagonalArchitectureTest + LayeredArchitectureTest + DependencyInversionTest en CI |
| **0 bug, 0 vulnérabilité** | SonarQube confirme : dette uniquement cosmétique/maintenabilité |

### 3.2 Points de risque ⚠️

| Zone | Risque |
|---|---|
| **Spring Boot 3.3.0 EOL** | Pas de patches sécurité depuis juin 2025 — P0 |
| **Absence CI/CD** | 2288 tests jamais exécutés en automatique — régressions invisibles |
| **137 imports `model.*`** | Dette de migration hexagonale — couplage fort au legacy |
| **DraftController 657L** | Fichier le plus à risque (UTF-8 + SRP) — modifications à risque élevé |
| **Score DRY 4.5/10** | ~1400 lignes dupliquées — maintenance coûteuse |

---

## 4. Métriques quantitatives globales

### 4.1 Volume de code

| Catégorie | Valeur |
|---|---|
| Fichiers Java (main) | 431 |
| Fichiers Java (test) | 251 |
| Fichiers TypeScript (src) | 316 |
| Migrations SQL (Flyway) | V1–V41 (34 fichiers, 8 gaps de numérotation) |
| Total tests backend | ~2 288 |
| Total tests frontend | 2 195 (2 149 passing, 46 pre-existing failures) |

### 4.2 Couverture

| Métrique | Backend | Objectif | Statut |
|---|---|---|---|
| Lignes | 86.89% | ≥ 85% | ✅ |
| Branches | 73.36% | ≥ 70% | ✅ |
| Fonctions | 84.64% | ≥ 80% | ✅ |
| Instructions | 86.31% | ≥ 85% | ✅ |

### 4.3 SonarQube Quality Gate

| Catégorie | Count | Priorité |
|---|---|---|
| CODE_SMELL MAJOR | 354 | Medium |
| CODE_SMELL MINOR | 146 | Low |
| Bugs | 0 | — |
| Vulnérabilités | 0 | — |
| **Quality Gate** | **ERROR** | Target: WARNING+ |

### 4.4 Architecture hexagonale

| Domaine | Migration | DIP |
|---|---|---|
| Game | ✅ Complet | ✅ |
| Draft | ✅ Complet | ✅ |
| Player | ✅ Complet | ✅ |
| Team | ✅ Complet | ✅ |
| Trade | ✅ Complet | ✅ |
| User | ❌ Pas de port | ❌ |
| Score | ❌ Pas de port | ❌ |
| Ingestion | Partiel | Partiel |

---

## 5. Roadmap Sprint 3 — Plan d'action priorisé

### Phase 1 — Semaine 1 : Bloquants critiques (P0, effort < 1 jour)

```
Objectif : Éliminer les risques immédiats et rétablir la confiance CI/CD
```

| Tâche | Effort | Impact | Ticket |
|---|---|---|---|
| **Supprimer fallback JWT_SECRET** dans `application.yml` | 5 min | Élimination P0-3 | TECH-S3-001 |
| **Créer pipeline CI** `ci.yml` (build + mvn test + npm run test:vitest) | 3h | Élimination P0-2 | TECH-S3-002 |
| **Upgrader Spring Boot** `3.3.0 → 3.3.13` | 1h | Élimination P0-1 | TECH-S3-003 |
| **Migrer Node.js CI** vers 22 | 15 min | Élimination P1-6 | TECH-S3-004 |
| **Fix `show-details: when_authorized`** prod | 15 min | Élimination P1-7 | TECH-S3-005 |

### Phase 2 — Semaine 2-3 : DRY + Tests + SonarQube (P0/P1)

```
Objectif : Réduire la duplication et monter le Quality Gate de ERROR → WARNING
```

| Tâche | Effort | Impact | Ticket |
|---|---|---|---|
| **Extraire `MappingUtils`** (safeInt/safeBool) | 1h | DRY P0-5 | TECH-S3-006 |
| **Extraire `SecurityTestBeans` helper** partagé | 2h | DRY P0-4 | TECH-S3-007 |
| **Corriger 46 tests Zone.js** (fakeAsync → async) | 4h | Tests P1-1 | TECH-S3-008 |
| **Corriger 141 magic numbers** (`java:S109`) | 4h | Sonar P1-2 | TECH-S3-009 |
| **Corriger 88 violations assertions** (S5838+S5853+S5778) | 3h | Sonar P1-3 | TECH-S3-010 |
| **Coverage leaderboard.service.ts** (+30 tests, 50%→80%) | 3h | Tests P1-8 | TECH-S3-011 |
| **Extraire `ErrorHandlerUtils`** Angular (handleError partagé) | 2h | DRY P1-9 | TECH-S3-012 |

### Phase 3 — Semaine 3-4 : Architecture + SOLID (P1/P2)

```
Objectif : Progresser la migration hexagonale et réduire les violations SRP
```

| Tâche | Effort | Impact | Ticket |
|---|---|---|---|
| **Splitter `DraftController`** (657L → DraftPickController + DraftStateController) + réparer UTF-8 | 3h | SRP P1-4 | TECH-S3-013 |
| **Migrer `UserService`** → `UserDomainPort` + `UserRepositoryAdapter` | 3h | DIP P0-6 | TECH-S3-014 |
| **Migrer `ScoreService`** → `ScoreDomainPort` + adapter | 2h | DIP P0-6 | TECH-S3-015 |
| **Introduire Strategy** pour `DraftMode` dans `DraftService` | 2h | OCP P1-5 | TECH-S3-016 |
| **Corriger 22 `toLowerCase()` sans Locale** | 30 min | Sonar P2-2 | TECH-S3-017 |
| **Upgrader PostgreSQL driver** `42.7.3 → 42.7.5` | 15 min | Stack P1-5 | TECH-S3-018 |

### Phase 4 — Sprint 3 fin : Sécurité résiduelle + Stack (P2/P3)

```
Objectif : Finaliser les résidus sécurité et planifier les upgrades majeurs
```

| Tâche | Effort | Impact | Ticket |
|---|---|---|---|
| **Rate limiting** `/api/auth/**` (bucket4j) | 2h | Sécu P2-3 | TECH-S3-019 |
| **WebSocket JWT handshake** interceptor | 1h | Sécu P2-4 | TECH-S3-020 |
| **Unifier interfaces `Player` TypeScript** | 1h | DRY P2-10 | TECH-S3-021 |
| **Upgrader `dependency-check-maven`** `9.0.9 → 12.1.0` | 30 min | Stack P2-6 | TECH-S3-022 |
| **Évaluer upgrade Flyway** `10.x → 11.x` (sprint dédié) | Analyse | Stack P2-5 | TECH-S3-P |

---

## 6. Objectifs qualité Sprint 3

| Métrique | Sprint 2 | Cible Sprint 3 |
|---|---|---|
| SonarQube Quality Gate | ❌ ERROR | ✅ WARNING ou SUCCESS |
| Magic numbers | 141 | < 20 |
| Violations assertions | 88 | 0 |
| Tests Vitest instables | 46 | 0 |
| Score DRY | 4.5/10 | 6/10 |
| Couverture leaderboard.service.ts | < 50% | ≥ 80% |
| Spring Boot EOL | 3.3.0 ❌ | 3.3.13 ✅ |
| Pipeline CI build+test | ❌ Absent | ✅ Créé |
| Domaines hexagonaux migrés | 5/8 | 7/8 (+User+Score) |

---

## 7. Dépendances entre actions

```
P0-2 (CI pipeline) ←── TECH-S3-002 ──→ bloque: toutes les regressions détectées
P0-1 (Spring Boot upgrade) ←── TECH-S3-003 ──→ dépend de: TECH-S3-002 green
P0-4 + P0-5 (DRY) ──→ réduit complexité de: TECH-S3-013 (DraftController split)
P1-1 (Zone.js fix) ──→ prérequis de: CI green sur frontend
P1-4 (DraftController split) ──→ prérequis de: TECH-S3-016 (Strategy DraftMode)
TECH-S3-014 (UserDomainPort) ──→ dépend de: TECH-010 patterns connus ✅
```

---

## 8. Risques résiduels non adressés en sprint 3

| Risque | Raison de report | Mitigation |
|---|---|---|
| Migration Spring Boot 3.3.x → 3.4.x | Breaking changes, sprint dédié | 3.3.13 = acceptable |
| Suppression doublon `model/Game.java` vs `domain/game/model/Game.java` | 137 imports à migrer | Tickets incrémentaux |
| Testcontainers PostgreSQL remplacement H2 | Sprint dédié nécessaire | Risques H2/PG notés |
| `login.component.ts` 602 lignes | P3 refactoring | Tests existants protègent |
| Alerting Flyway gaps documentation | Effort doc non prioritaire | `MIGRATION_HISTORY.md` planifié |

---

## 9. Score de maturité global

| Dimension | Sprint 1 | Sprint 2 | Cible Sprint 3 |
|---|---|---|---|
| Sécurité | 40/60 | **57/60** | 59/60 |
| Architecture | 55% | **71%** | 80% |
| SOLID | 65% | **86%** | 90% |
| Qualité code (Sonar) | N/A | **ERROR** | WARNING |
| DRY | N/A | **4.5/10** | 6/10 |
| Tests | 75% | **82%** | 88% |
| Stack technique | N/A | **6/10** | 8/10 |
| **MATURITÉ GLOBALE** | **~58%** | **~72%** | **~83%** |

> **Conclusion** : Le projet est en bonne trajectoire. La base de code est fonctionnelle et sécurisée. Les dettes identifiées sont maîtrisées et non critiques pour la production, à l'exception de Spring Boot EOL et l'absence de pipeline CI/CD — ces deux points doivent être adressés en semaine 1 de Sprint 3. Le score DRY 4.5/10 est la priorité technique la plus impactante après le CI/CD.
