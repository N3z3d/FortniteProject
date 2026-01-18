# ADR-001: Architecture cible - Hexagonal Architecture avec Migration Progressive

**Date**: 2026-01-18
**Statut**: ✅ ACCEPTÉ
**Décideurs**: Équipe technique FortniteProject
**Contexte technique**: Spring Boot 3.x, PostgreSQL, Angular frontend

---

## Contexte

Le projet FortniteProject présente actuellement une **architecture mixte** :
- Base en **Layered Architecture** (Controller → Service → Repository → Entity)
- Tentative partielle d'**Hexagonal Architecture** (package `core` avec use cases et domain)
- Boundaries flous entre couches
- Logique métier dispersée dans controllers et entities
- 6 classes > 500 lignes (violations SOLID)

**Problèmes identifiés** :
1. Controllers contiennent de la logique métier
2. Entities JPA couplées à la logique domain
3. Services trop larges avec trop de responsabilités (LeaderboardService 708 lignes)
4. Aucune règle d'architecture compiletime-enforced
5. Risque de régression et difficulté à tester

---

## Décision

Nous adoptons une **Hexagonal Architecture (Ports & Adapters)** comme architecture cible, avec une **migration progressive** pour minimiser les risques.

### Architecture cible

```
com.fortnite.pronos/
│
├── domain/                      # ❤️ CORE - Aucune dépendance externe
│   ├── model/                   # Entités métier pures (pas de @Entity)
│   │   ├── Game.java
│   │   ├── Team.java
│   │   ├── Player.java
│   │   └── ...
│   ├── service/                 # Services domain (règles métier)
│   │   ├── GameDomainService.java
│   │   ├── DraftDomainService.java
│   │   └── ...
│   ├── port/                    # Interfaces (contrats)
│   │   ├── in/                  # Use cases (API métier)
│   │   │   ├── CreateGameUseCase.java
│   │   │   ├── JoinGameUseCase.java
│   │   │   └── ...
│   │   └── out/                 # Repositories, external services
│   │       ├── GameRepository.java
│   │       ├── NotificationPort.java
│   │       └── ...
│   └── exception/               # Exceptions métier
│       ├── GameNotFoundException.java
│       └── ...
│
├── application/                 # 🎯 USE CASES - Orchestration
│   ├── usecase/                 # Implémentations use cases
│   │   ├── CreateGameUseCaseImpl.java
│   │   ├── JoinGameUseCaseImpl.java
│   │   └── ...
│   └── dto/                     # DTOs publics (API contracts)
│       ├── GameDto.java
│       ├── CreateGameRequest.java
│       └── ...
│
├── adapter/                     # 🔌 ADAPTERS - Infrastructure
│   ├── in/                      # Adapters entrants
│   │   └── web/                 # REST Controllers
│   │       ├── GameController.java
│   │       ├── LeaderboardController.java
│   │       └── ...
│   ├── out/                     # Adapters sortants
│   │   ├── persistence/         # JPA Repositories
│   │   │   ├── entity/          # Entities JPA
│   │   │   │   ├── GameEntity.java
│   │   │   │   └── ...
│   │   │   ├── mapper/          # Entity <-> Domain mappers
│   │   │   │   ├── GameMapper.java
│   │   │   │   └── ...
│   │   │   └── repository/      # Implémentations repositories
│   │   │       ├── GameRepositoryAdapter.java
│   │   │       └── ...
│   │   └── external/            # APIs externes
│   │       ├── FortniteTrackerAdapter.java
│   │       └── ...
│   └── config/                  # Configuration Spring
│       ├── SecurityConfig.java
│       ├── DatabaseConfig.java
│       └── ...
│
└── shared/                      # ⚙️ CROSS-CUTTING
    ├── exception/               # Exception handling global
    │   └── GlobalExceptionHandler.java
    └── util/                    # Utilitaires
        ├── AuditLogger.java
        └── ...
```

### Règles de dépendances

```
┌──────────────────────────────────────┐
│  adapter.in.web (Controllers)        │ ─┐
└──────────────────────────────────────┘  │
                                          │ depends on
┌──────────────────────────────────────┐  │
│  application (Use Cases, DTOs)       │ ◄┘
└──────────────────────────────────────┘  │
                 │                        │ depends on
                 │ depends on             │
                 ▼                        │
┌──────────────────────────────────────┐  │
│  domain (Model, Ports, Services)     │ ◄┘
└──────────────────────────────────────┘
                 ▲
                 │ implements
                 │
┌──────────────────────────────────────┐
│  adapter.out.persistence (JPA)       │
└──────────────────────────────────────┘
```

**Règles strictes** :
1. ✅ `domain` **ne dépend de RIEN** (pas de Spring, JPA, Jackson, etc.)
2. ✅ `application` dépend uniquement de `domain`
3. ✅ `adapter.in` dépend de `application` et `domain.port.in`
4. ✅ `adapter.out` dépend de `domain.port.out` et implémente les ports
5. ❌ `domain` ne doit **JAMAIS** dépendre de `adapter`
6. ❌ `application` ne doit **JAMAIS** dépendre de `adapter`

---

## Stratégie de migration

### Phase 1 : Fondations (Sprint 1-2) ✅ PRIORITAIRE

**Objectif** : Établir la nouvelle structure sans casser l'existant

1. ✅ Créer la nouvelle structure de packages
2. ✅ Écrire les tests d'architecture (ArchUnit)
   - Vérifier que `domain` n'a pas de dépendances externes
   - Vérifier que les dépendances respectent les règles
3. ✅ Documenter la nouvelle architecture (ce ADR)
4. ⏳ Créer des exemples de migration (1 use case complet en Hexagonal)

**Fichiers à créer** :
- `test/.../ArchitectureTest.java` (ArchUnit rules)
- `domain/port/in/CreateGameUseCase.java` (interface)
- `domain/port/out/GameRepositoryPort.java` (interface)
- `application/usecase/CreateGameUseCaseImpl.java` (implémentation)

### Phase 2 : Migration des God Services (Sprint 3-6) 🔥 CRITIQUE

**Objectif** : Refactoriser les 3 services > 500 lignes

**2.1. LeaderboardService (708 lignes → 4 services)**
```
Avant:
- LeaderboardService (1 service, 708 lignes)

Après:
- domain/service/LeaderboardQueryService (~150 lignes)
- domain/service/LeaderboardStatsService (~150 lignes)
- domain/service/PlayerRankingService (~150 lignes)
- adapter/out/persistence/LeaderboardRepositoryAdapter (~150 lignes)
```

**2.2. DataInitializationService (658 lignes → 5 services)**
```
Après:
- application/SeedOrchestrator (~100 lignes)
- domain/service/UserSeedService (~120 lignes)
- domain/service/PlayerSeedService (~120 lignes)
- domain/service/TeamSeedService (~120 lignes)
- domain/service/GameSeedService (~120 lignes)
```

**2.3. TeamService (562 lignes → 3 services)**
```
Après:
- domain/service/TeamQueryService (~180 lignes)
- domain/service/TeamCommandService (~180 lignes)
- domain/service/RosterManagementService (~180 lignes)
```

### Phase 3 : Migration du reste (Sprint 7-12)

1. Migrer GameController (530 lignes)
2. Isoler le domain des entities JPA (créer mappers)
3. Migrer TradingService (519 lignes)
4. Migrer les autres services progressivement

### Phase 4 : Cleanup (Sprint 13+)

1. Supprimer les anciens packages (une fois tous migrés)
2. Renforcer les tests d'architecture
3. Documenter les patterns pour l'équipe

---

## Tests d'architecture (ArchUnit)

```java
@ArchTest
public static final ArchRule domainShouldNotDependOnOutside =
    classes()
        .that().resideInAPackage("..domain..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage("..domain..", "java..", "lombok..");

@ArchTest
public static final ArchRule applicationShouldOnlyDependOnDomain =
    classes()
        .that().resideInAPackage("..application..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage("..application..", "..domain..", "java..", "lombok..", "org.springframework..");

@ArchTest
public static final ArchRule adaptersShouldNotDependOnEachOther =
    noClasses()
        .that().resideInAPackage("..adapter.in..")
        .should().dependOnClassesThat()
        .resideInAPackage("..adapter.out..");

@ArchTest
public static final ArchRule useCasesShouldBeNamedCorrectly =
    classes()
        .that().resideInAPackage("..domain.port.in..")
        .should().haveSimpleNameEndingWith("UseCase");
```

---

## Avantages

### ✅ Testabilité
- Domain testable sans Spring, JPA, ou infrastructure
- Mocks faciles via interfaces (ports)
- Tests unitaires rapides (pas de contexte Spring)

### ✅ Maintenabilité
- Séparation claire des responsabilités
- Règles d'architecture enforced au build
- Changements infrastructure n'impactent pas le domain

### ✅ Évolutivité
- Nouveau use case = nouvelle classe (OCP)
- Nouvelles features isolées (moins de merge conflicts)
- Remplacer JPA par autre ORM = changer uniquement adapter.out

### ✅ Onboarding
- Structure claire : "où mettre mon code ?"
- Exemples de migration documentés
- Tests d'architecture guident les devs

---

## Inconvénients et Mitigations

| Inconvénient | Mitigation |
|--------------|-----------|
| **Plus de fichiers** (mappers, interfaces) | Générer avec IDE templates |
| **Courbe d'apprentissage** | Documentation + pair programming |
| **Migration longue** (6-12 mois) | Migration progressive, pas de big bang |
| **Duplication temporaire** | Acceptable, supprimer ancien code au fur et à mesure |

---

## Alternatives considérées

### Alternative 1 : Garder Layered Architecture

❌ **Rejeté** : Ne résout pas les problèmes de couplage et de testabilité

### Alternative 2 : Microservices Architecture

❌ **Rejeté** : Trop complexe pour la taille actuelle de l'équipe et du projet. Complexité opérationnelle (orchestration, déploiement, monitoring) non justifiée pour un monolithe de 23k lignes.

### Alternative 3 : Clean Architecture (Uncle Bob)

⚠️ **Similaire à Hexagonal** : Clean Architecture et Hexagonal sont très proches. Hexagonal est plus pragmatique et moins dogmatique. Choisi pour simplicité.

---

## Conséquences

### Immédiates
1. Création de nouveaux packages (`domain`, `application`, `adapter`)
2. Écriture des tests d'architecture (ArchUnit)
3. Formation de l'équipe sur Hexagonal Architecture

### Court terme (Sprint 1-6)
1. Coexistence de deux architectures (ancien + nouveau)
2. Duplication temporaire de code pendant migration
3. Refactoring des 3 god services (LeaderboardService, DataInitializationService, TeamService)

### Long terme (6-12 mois)
1. Suppression complète de l'ancienne structure
2. 100% du code suit Hexagonal Architecture
3. Testabilité et maintenabilité améliorées
4. Onboarding plus rapide pour nouveaux devs

---

## Références

- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
- [Get Your Hands Dirty on Clean Architecture](https://github.com/thombergs/buckpal)
- [ArchUnit - Architecture Testing](https://www.archunit.org/)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)

---

## Suivi

| Date | Événement | Statut |
|------|-----------|--------|
| 2026-01-18 | ADR créé et approuvé | ✅ |
| 2026-01-18 | Structure packages créée | ⏳ En cours |
| TBD | Tests ArchUnit implémentés | ⏳ À faire |
| TBD | Exemple migration 1 use case | ⏳ À faire |
| TBD | LeaderboardService refactoré | ⏳ À faire |

---

**Prochaines étapes** :
1. ✅ Approuver cet ADR
2. ⏳ Créer les tests ArchUnit (JIRA-ARCH-010)
3. ⏳ Migrer 1 use case complet comme exemple
4. ⏳ Former l'équipe sur Hexagonal Architecture
5. ⏳ Commencer le refactoring de LeaderboardService
