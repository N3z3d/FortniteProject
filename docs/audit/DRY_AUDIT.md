# Audit DRY / Duplication de Code — TECH-019
Date: 2026-03-02
Auditeur: Claude Sonnet 4.6

---

## Résumé exécutif

| Métrique | Valeur |
|---|---|
| Score DRY global | **4.5 / 10** |
| Fichiers scannés (backend) | 425 fichiers Java |
| Fichiers scannés (frontend) | 309 fichiers TypeScript |
| Duplications CRITIQUES (P0) | **5** |
| Duplications MAJEURES (P1) | **7** |
| Duplications MINEURES (P2) | **5** |
| Lignes dupliquées estimées | ~1 400 lignes |

**Verdict** : Le projet présente une dette DRY significative. Les problèmes les plus graves concernent (1) les helpers de mapping répliqués dans chaque mapper JPA, (2) la prolifération d'interfaces `Player` TypeScript, (3) la logique `handleError`/`resolveErrorMessage` recopiée dans 3 services Angular, et (4) le `SecurityTestBeans` inner class dupliqué dans 14 fichiers de test.

---

## Duplications CRITIQUES (P0 — refactor immédiat)

### DRY-001 — Helpers `safeInt` / `safeBool` dupliqués dans tous les EntityMappers

**Fichiers concernés :**
- `adapter/out/persistence/game/GameEntityMapper.java` lignes 371–379
- `adapter/out/persistence/player/PlayerEntityMapper.java` lignes 100–104
- `adapter/out/persistence/team/TeamEntityMapper.java` lignes 156–160
- `adapter/out/persistence/draft/DraftEntityMapper.java` ligne 62
- `service/draft/DraftService.java` ligne 481

**Code dupliqué (extrait représentatif) :**
```java
// Présent à l'identique dans GameEntityMapper, PlayerEntityMapper, TeamEntityMapper
private static int safeInt(Integer value, int defaultValue) {
    return value != null ? value : defaultValue;
}
private static boolean safeBool(Boolean value) {
    return value != null && value;
}
```

**Description :** Cinq implémentations quasi-identiques de méthodes utilitaires de défensive null-safe. `DraftEntityMapper` a même une variante sans defaultValue (`return value != null && value > 0 ? value : 1`), créant une incohérence silencieuse.

**Impact :** 5 × ~6 lignes = 30 lignes dupliquées. Risque de divergence si un comportement par défaut doit changer (ex : `safeInt` renvoie 1 vs 0 selon le mapper).

**Recommandation :** Créer une classe `MappingUtils` dans `adapter/out/persistence/` (ou `adapter/out/persistence/support/`), package-private ou `final class`, avec les méthodes statiques `safeInt(Integer, int)`, `safeBool(Boolean)`. Tous les mappers l'importent.

---

### DRY-002 — `createUserReference` / `createPlayerReference` / `createGameReference` répliquées dans 3 mappers

**Fichiers concernés :**
- `adapter/out/persistence/game/GameEntityMapper.java` lignes 147–185 (méthodes `createPlayerReference`, `createDraftReference`, `createUserReference`)
- `adapter/out/persistence/game/GameRepositoryAdapter.java` ligne 211 (`createUserReference`)
- `adapter/out/persistence/team/TeamEntityMapper.java` lignes 134–155 (méthodes `createUserReference`, `createPlayerReference`, `createGameReference`)

**Code dupliqué (extrait) :**
```java
// GameEntityMapper.java:176  ET  TeamEntityMapper.java:134
private User createUserReference(UUID userId, String username) {
    if (userId == null) return null;
    User user = new User();
    user.setId(userId);
    user.setUsername(username);
    return user;
}

// GameEntityMapper.java:147  ET  TeamEntityMapper.java:143
private Player createPlayerReference(UUID playerId) {
    if (playerId == null) return null;
    return Player.builder().id(playerId).build();
}
```

**Description :** La création de "shallow reference" (objet JPA avec seulement l'ID pour éviter une requête inutile) est répliquée identiquement dans `GameEntityMapper`, `GameRepositoryAdapter` et `TeamEntityMapper`. La logique de `createGameReference` (dans `TeamEntityMapper:150`) n'existe pas dans `GameEntityMapper`, rendant les patrons asymétriques.

**Impact :** ~40 lignes dupliquées. Une modification du modèle `User` ou `Player` oblige à toucher plusieurs mappers.

**Recommandation :** Extraire dans `EntityReferenceFactory` (package `adapter/out/persistence/support/`) : méthodes statiques `userRef(UUID)`, `userRef(UUID, String)`, `playerRef(UUID)`, `gameRef(UUID)`, `draftRef(UUID)`.

---

### DRY-003 — `buildAndRecordError` identique dans `GameExceptionHandler` et `DomainExceptionHandler`

**Fichiers concernés :**
- `config/DomainExceptionHandler.java` lignes 149–161
- `config/GameExceptionHandler.java` lignes 67–79

**Code dupliqué :**
```java
// IDENTIQUE dans les deux fichiers
private ResponseEntity<GlobalExceptionHandler.ErrorResponse> buildAndRecordError(
    Exception ex, HttpServletRequest request, HttpStatus status, String error, String code) {

    GlobalExceptionHandler.ErrorResponse errorResponse =
        GlobalExceptionHandler.ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(error)
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .code(code)
            .build();

    errorJournalService.recordError(ErrorEntry.from(ex, request, errorResponse));
    return ResponseEntity.status(status).body(errorResponse);
}
```

**Description :** La méthode privée `buildAndRecordError` (14 lignes) est copiée mot pour mot dans deux `@ControllerAdvice`. Si le format de `ErrorResponse` change, il faut modifier les deux handlers.

**Impact :** 14 lignes dupliquées × 2. Risque de divergence lors de l'ajout d'un nouveau champ (`requestId`, etc.).

**Recommandation :** Extraire dans une classe abstraite `AbstractExceptionHandler` ou un utilitaire `ExceptionResponseBuilder` dans le package `config/` (non-`@ControllerAdvice`), réutilisé par les deux handlers. Vérifier que `AlreadyInGameFallbackHelper` peut aussi en profiter.

---

### DRY-004 — Interface `Player` TypeScript définie 7 fois dans des endroits différents

**Fichiers concernés :**
- `shared/models/player.model.ts` ligne 25 — `interface Player extends BasePlayer`
- `core/models/leaderboard.model.ts` ligne 1 — `interface Player` (id, name, nickname, region, tranche, points)
- `core/services/leaderboard.service.ts` ligne 11 — `interface Player` (id, nickname, region, tranche, points, isActive, rank, isWorldChampion, lastUpdate)
- `features/game/models/game.interface.ts` ligne 128 — `interface Player` (id, username, nickname, region, tranche, currentSeason, selected, available)
- `features/draft/models/draft.interface.ts` ligne 321 — `type Player = AvailablePlayer`
- `features/trades/services/trading.service.ts` ligne 8 — `interface Player` (id, name, region, team, averageScore, totalScore, gamesPlayed, marketValue, stats)
- `features/teams/services/team-detail-data.service.ts` ligne 9 — `interface Player` (id, username, nickname, region, tranche)
- `features/teams/team-edit/team-edit.component.ts` ligne 23 — `interface Player` (locale, privée)

**Description :** L'entité `Player` est représentée par 7+ interfaces distinctes, parfois incompatibles. `trades/Player` ajoute `marketValue` et `gamesPlayed` absents partout ailleurs. `leaderboard.service.Player` ajoute `isWorldChampion` et `lastUpdate`. `game.interface.Player` utilise `username` et non `name`. Les composants ne savent pas quelle interface importer.

**Impact :** Lisibilité très faible, erreurs de mapping silencieuses, impossible de refactorer un champ `Player` sans auditer 8 fichiers.

**Recommandation :** Centraliser dans `shared/models/player.model.ts` : une interface `CorePlayer` (id, nickname, region, tranche) avec des extensions spécifiques — `LeaderboardPlayer`, `DraftPlayer`, `TradePlayer`. Migrer toutes les importations. Le champ `name` vs `nickname` vs `username` doit être unifié.

---

### DRY-005 — `handleError` / `resolveErrorMessage` dupliqués dans 3 services Angular

**Fichiers concernés :**
- `features/game/services/game-command.service.ts` lignes 246–272
- `features/game/services/game-query.service.ts` lignes 164–213 (`resolveHttpErrorMessage` + `extractBackendMessage`)
- `features/draft/services/draft.service.ts` lignes 257–285

**Code dupliqué (extrait) :**
```typescript
// game-command.service.ts:254 ET draft.service.ts:269 (quasi-identique)
private resolveErrorMessage(error: HttpErrorResponse, backendMessage: string | null): string {
    if (error.error instanceof ErrorEvent) {
        return `${this.t.t('common.error')}: ${error.error.message}`;
    }
    switch (error.status) {
        case 400: return backendMessage || this.t.t('errors.validation');
        case 401: return this.t.t('errors.unauthorized');
        case 403: return backendMessage || this.t.t('errors.handler.forbiddenMessage');
        case 404: return backendMessage || this.t.t('errors.notFound');
        // ...
    }
}
```

**Description :** `GameCommandService` et `DraftService` ont des méthodes `handleError` et `resolveErrorMessage` quasi-identiques. `GameQueryService` réimplémente la même logique dans `resolveHttpErrorMessage` + `extractBackendMessage` (qui existe déjà dans le util `user-facing-error-message.util.ts` mais `GameQueryService` a sa propre copie privée de 30 lignes). L'utilitaire `extractBackendErrorDetails()` existe dans `core/utils/user-facing-error-message.util.ts` mais n'est pas utilisé dans `GameQueryService`.

**Impact :** ~90 lignes dupliquées. Incohérence : `draft.service.ts` a un message spécifique pour `case 0` (`draft.errors.connectionError`), `game-command.service.ts` non. Les corrections de traduction doivent être appliquées 3 fois.

**Recommandation :** Créer un `HttpErrorHandlerService` dans `core/services/` (ou utiliser pleinement `user-facing-error-message.util.ts`) qui expose `resolveMessage(error, context?)`. Les 3 services le délèguent. La table de codes HTTP reste en un seul endroit.

---

## Duplications MAJEURES (P1)

### DRY-006 — `SecurityTestBeans` inner class dupliqué dans 14 fichiers de test

**Fichiers concernés :** Tous les `SecurityConfig*AuthorizationTest.java` dans `src/test/java/com/fortnite/pronos/config/` :
- `SecurityConfigSnakeDraftAuthorizationTest.java` lignes 162–177
- `SecurityConfigSimultaneousDraftAuthorizationTest.java` lignes 133–148
- `SecurityConfigSwapSoloAuthorizationTest.java`
- `SecurityConfigDraftAuditAuthorizationTest.java`
- `SecurityConfigDraftParticipantTradeAuthorizationTest.java`
- `SecurityConfigAdminDraftRosterAuthorizationTest.java`
- `SecurityConfigIncidentAuthorizationTest.java`
- `SecurityConfigAccountAuthorizationTest.java`
- `SecurityConfigUserAuthorizationTest.java`
- `SecurityConfigAdminSupervisionAuthorizationTest.java`
- `SecurityConfigAdminScrapeAuthorizationTest.java`
- `SecurityConfigActuatorAuthorizationTest.java`
- `SecurityConfigGameConfigurePeriodAuthorizationTest.java`
- `SecurityConfigGameLeaderboardAuthorizationTest.java`

**Code dupliqué (identique dans les 14 fichiers) :**
```java
@TestConfiguration
public static class SecurityTestBeans {
    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
        JwtService jwtService = org.mockito.Mockito.mock(JwtService.class);
        return new JwtAuthenticationFilter(jwtService, userDetailsService) {
            @Override
            protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
                filterChain.doFilter(request, response);
            }
        };
    }
}
```

**Impact :** 14 × ~15 lignes = 210 lignes. Toute modification du filtre JWT (ex : ajout d'un paramètre au constructeur) oblige à modifier 14 fichiers.

**Recommandation :** Extraire dans une `@TestConfiguration` partagée `SecurityTestBeansConfig` dans le package `config/` (test scope). Référencer via `@Import(SecurityTestBeansConfig.class)` dans chaque test.

---

### DRY-007 — Enum mapping `XxxEnum.valueOf(other.name())` dupliqué dans 5 mappers + 2 services

**Fichiers et lignes :**
- `GameEntityMapper.java:312` — `GameStatus.valueOf(domainStatus.name())`
- `GameEntityMapper.java:320` — `GameStatus.valueOf(entityStatus.name())`
- `GameEntityMapper.java:328` — `DraftMode.valueOf(domainDraftMode.name())`
- `GameEntityMapper.java:336` — `DraftMode.valueOf(entityDraftMode.name())`
- `GameEntityMapper.java:344` — `Player.Region.valueOf(domainRegion.name())`
- `GameEntityMapper.java:352` — `PlayerRegion.valueOf(entityRegion.name())`
- `PlayerEntityMapper.java:68` — `Player.Region.valueOf(domainRegion.name())`
- `PlayerEntityMapper.java:80` — `PlayerRegion.valueOf(entityRegion.name())`
- `TradeEntityMapper.java:78` — `TradeStatus.valueOf(status.name())`
- `DraftEntityMapper.java:59` — `DraftStatus.valueOf(status.name())`
- `DraftService.java:478` — `DraftStatus.valueOf(status.name())`
- `GameCreationService.java:210` — `PlayerRegion.valueOf(region.name())`

**Description :** Le pattern `TargetEnum.valueOf(source.name())` est la stratégie de mapping d'enum utilisée partout. Il est sûr uniquement si les deux enums ont exactement les mêmes noms. S'ils divergent, `IllegalArgumentException` au runtime sans avertissement à la compilation.

**Impact :** 12+ occurrences. La logique de "même nom = même valeur" n'est pas garantie et n'est pas testée explicitement (sauf `EnumMappingTest`). `GameEntityMapper` seul a 6 méthodes de mapping d'enum distinctes.

**Recommandation :** Créer un utilitaire générique `EnumMapper.map(SourceEnum, Class<TargetEnum>)` avec validation au chargement. Ou utiliser une `EnumMappingUtils` avec des méthodes statiques typées pour chaque paire. La cohérence est garantie à la compilation.

---

### DRY-008 — `DraftParticipant` et `GameParticipant` dupliqués frontend

**Fichiers concernés :**
- `features/game/models/game.interface.ts` ligne 68 — `interface GameParticipant`
- `features/game/models/game.interface.ts` ligne 99 — `interface DraftParticipant`
- `features/draft/models/draft.interface.ts` ligne 44 — `interface DraftParticipant` (champs différents)
- `features/draft/models/draft.interface.ts` ligne 326 — `interface GameParticipant` (redéfini !)

**Description :** `DraftParticipant` est défini deux fois (dans `game.interface.ts` et `draft.interface.ts`) avec des champs légèrement différents. `GameParticipant` est aussi redéfini dans `draft.interface.ts` ligne 326, créant une duplication intra-fichier cross-module. Les composants doivent deviner quelle version importer.

**Impact :** 4 définitions pour 2 concepts. Risque de comportement silencieusement différent selon l'import utilisé.

**Recommandation :** Consolider dans `shared/models/` ou `features/draft/models/draft.interface.ts` (source canonique). `game.interface.ts` réexporte depuis `draft.interface.ts`.

---

### DRY-009 — `DraftRules` défini deux fois avec champs inconsistants

**Fichiers concernés :**
- `features/game/models/game.interface.ts` ligne 118 — `interface DraftRules`
- `features/draft/models/draft.interface.ts` ligne 30 — `interface DraftRules`

**Code dupliqué :**
```typescript
// game.interface.ts:118
interface DraftRules {
    maxPlayersPerTeam: number;
    timeLimitPerPick: number;
    autoPickEnabled: boolean;
    regionQuotas: { [region: string]: number };
}

// draft.interface.ts:30
interface DraftRules {
    maxPlayersPerTeam?: number;
    regionLimits?: { [region: string]: number };
    regionQuotas?: { [region: string]: number };
    timePerPick?: number;
    timeLimitPerPick?: number;   // doublon du champ ci-dessus!
    allowTrades?: boolean;
    snakeDraft?: boolean;
    autopickEnabled?: boolean;
    autoPickEnabled?: boolean;   // doublon de autopickEnabled!
    autopickTimeLimit?: number;
    autoPickDelay?: number;
}
```

**Description :** `DraftRules` dans `draft.interface.ts` a lui-même des doublons internes : `autopickEnabled` / `autoPickEnabled` et `timePerPick` / `timeLimitPerPick`. En plus d'être défini deux fois dans des fichiers différents.

**Impact :** Les composants qui utilisent `DraftRules` ne savent pas quel champ lire. Cela génère des `undefined` silencieux au runtime.

**Recommandation :** Une seule `DraftRules` dans `features/draft/models/draft.interface.ts`. Normaliser les noms : garder `timeLimitPerPick` et `autoPickEnabled` (camelCase cohérent). Supprimer tous les alias.

---

### DRY-010 — `ApiResponse<T>` interface TypeScript dupliquée

**Fichiers concernés :**
- `features/admin/models/admin.models.ts` ligne 125 — `interface ApiResponse<T>`
- `features/game/models/game.interface.ts` ligne 57 — `interface GameResponse` (success, message, data)

**Description :** `admin.models.ts` définit une interface `ApiResponse<T>` locale (success, data, message, timestamp) alors que `GameResponse` dans `game.interface.ts` couvre le même besoin. La structure backend `ApiResponse<T>` (avec `success`, `data`, `message`, `timestamp`) est donc typée différemment selon le domaine consommateur.

**Impact :** Si le backend change le format de réponse, deux interfaces doivent être mises à jour indépendamment.

**Recommandation :** Une seule `ApiResponse<T>` dans `shared/models/api-response.model.ts`, réutilisée par `admin.models.ts` et `game.interface.ts`.

---

### DRY-011 — Pattern `timeout + catchError(rethrow)` répété dans tous les `Repository` Angular

**Fichiers concernés :**
- `core/repositories/leaderboard.repository.ts` lignes 53, 65, 78, 88, 100, 110 (6 méthodes)
- `core/repositories/dashboard.repository.ts` lignes 85, 101, 117, 133 (4 méthodes)

**Code dupliqué :**
```typescript
// Répété 10+ fois avec uniquement le message de log qui change
return this.http.get<SomeType>(url).pipe(
    timeout(this.REQUEST_TIMEOUT),
    catchError(error => {
        this.logger.error('HttpXxxRepository: getYyy failed', error);
        throw error;
    })
);
```

**Impact :** 10 occurrences × ~5 lignes = ~50 lignes dupliquées. `REQUEST_TIMEOUT = 5000` est aussi dupliqué dans les deux classes.

**Recommandation :** Méthode privée helper `withTimeoutAndRethrow<T>(source$, methodName): Observable<T>` dans chaque repository, ou un opérateur RxJS personnalisé partagé dans `core/utils/rx-operators.util.ts`.

---

### DRY-012 — `PlayerDto.fromEntity()` et `PlayerDto.from()` vs `CataloguePlayerDto.from()` : mapping player répété

**Fichiers concernés :**
- `dto/player/PlayerDto.java` lignes 20–43 (2 factory methods, mapping depuis `model.Player`)
- `dto/player/CataloguePlayerDto.java` lignes 9–14 (mapping depuis `domain.player.model.Player`)
- `dto/PlayerRecommendResponse.java` lignes 10–17 (mapping depuis `domain.player.model.Player`)
- `dto/DraftAvailablePlayerResponse.java` ligne 5 (record avec id, nickname, region — sous-ensemble)

**Description :** Les champs `id`, `nickname`, `region`, `tranche` d'un joueur sont mappés 4 fois séparément depuis des sources différentes (model vs domain model). `PlayerDto` utilise encore le legacy `model.Player`, les 3 autres utilisent `domain.player.model.Player`. La divergence est aussi architecturale.

**Impact :** Migration vers domain objects incomplète. Chaque nouveau endpoint player ajoute un 5e DTO.

**Recommandation :** Centraliser dans un `PlayerDtoMapper` dans `service/` ou `dto/player/`, avec une seule méthode `toDto(domain.Player)` retournant un `PlayerSummaryDto` réutilisable. `CataloguePlayerDto`, `PlayerRecommendResponse`, et `DraftAvailablePlayerResponse` deviennent des projections ou sont fusionnés.

---

## Duplications MINEURES (P2)

### DRY-013 — `getEmptyRegionDistribution()` dupliqué dans `MockDashboardRepository` et `MockLeaderboardRepository`

**Fichiers :**
- `core/repositories/dashboard.repository.ts` lignes 175–185
- `core/repositories/leaderboard.repository.ts` lignes 116–127

**Code :**
```typescript
// Identique dans les deux fichiers
private getEmptyRegionDistribution(): { [key: string]: number } {
    return { 'EU': 0, 'NAC': 0, 'NAW': 0, 'BR': 0, 'ASIA': 0, 'OCE': 0, 'ME': 0 };
}
```

**Recommandation :** Constante partagée `EMPTY_REGION_DISTRIBUTION` dans `core/constants/regions.constants.ts`.

---

### DRY-014 — Validation `hours < 1 || hours > 168` répétée 3 fois dans `AdminDashboardController`

**Fichier :** `controller/AdminDashboardController.java` lignes 73, 109, 125

**Description :** La même validation manuelle (en doublon de `@Min`/`@Max` déjà présents) est répétée 3 fois avec la même constante et le même message d'erreur. Le `@Min`/`@Max` Spring Validation rend ces blocs if/else totalement redondants.

**Recommandation :** Supprimer les 3 blocs `if (hours < MIN_... || hours > MAX_...)`. La validation `@Min`/`@Max` avec `@Validated` sur la classe est suffisante, et le `GlobalExceptionHandler.handleConstraintViolationException` déjà en place retourne le 400 attendu.

---

### DRY-015 — `calculateSeasonProgressPercentage()` défini en dehors de toute classe dans `dashboard.repository.ts`

**Fichier :** `core/repositories/dashboard.repository.ts` lignes 8–14

**Description :** La fonction est une fonction module-level, utilisée seulement dans `MockDashboardRepository` et `HttpDashboardRepository`. Elle n'est pas exportée ni testée. Elle duplique probablement une logique similaire dans `DashboardStatsCalculatorService`.

**Recommandation :** Déléguer au `DashboardStatsCalculatorService` existant ou exposer depuis un utilitaire `DateUtils`.

---

### DRY-016 — `TestDataBuilderTest` n'est pas une classe de test

**Fichier :** `src/test/java/com/fortnite/pronos/util/TestDataBuilderTest.java`

**Description :** Cette classe mélange des factory methods (`createValidUser`, `createValidPlayer`, etc.) et des tests JUnit. Les factory methods sont `static` et `package-private`, donc potentiellement utilisables depuis d'autres tests dans le même package, mais leur visibilité est trop restrictive pour être un vrai helper partagé. D'autres tests dans le projet recréent localement leurs propres builders de `User` et `Player` avec `new User()`.

**Impact :** Duplication d'initialisation de test dans ~162 fichiers de test (tous les fichiers qui créent des `Player.builder()` ou `new Player()` manuellement).

**Recommandation :** Renommer en `TestDataBuilder` (sans suffixe `Test`), déplacer en `src/test/java/.../support/TestDataBuilder.java`, rendre `public`, et y centraliser toutes les factories de test.

---

### DRY-017 — `MIN_*_HOURS` et `MAX_*_HOURS` triplicatas dans `AdminDashboardController`

**Fichier :** `controller/AdminDashboardController.java` lignes 38–43

**Description :** Trois paires de constantes `MIN_RECENT_ACTIVITY_HOURS/MAX_RECENT_ACTIVITY_HOURS`, `MIN_ALERT_HOURS/MAX_ALERT_HOURS`, `MIN_VISIT_ANALYTICS_HOURS/MAX_VISIT_ANALYTICS_HOURS` ont toutes la valeur `1`/`168`. Ce sont les mêmes constantes répétées sous 3 noms différents.

**Recommandation :** Une seule paire `MIN_HOURS = 1` et `MAX_HOURS = 168` dans la classe.

---

## Plan d'action priorisé

### Phase 1 — Sprint immédiat (P0, ~3-4 jours)

| ID | Action | Effort | Impact |
|---|---|---|---|
| DRY-001 | Créer `MappingUtils` avec `safeInt`/`safeBool`, migrer les 5 mappers | S (2h) | Faible risque, gain maintenabilité |
| DRY-002 | Créer `EntityReferenceFactory`, migrer 3 classes | S (2h) | Faible risque, gain lisibilité |
| DRY-003 | Extraire `AbstractExceptionHandler` ou helper statique | S (1h) | Risque Spring Boot faible |
| DRY-005 | Créer `HttpErrorHandlerService` dans `core/services/` | M (4h) | 3 services Angular impactés, tests à MAJ |
| DRY-006 | Créer `SecurityTestBeansConfig` partagée, retirer les 14 copies | S (2h) | Tests only, zéro risque prod |

### Phase 2 — Sprint suivant (P1, ~5-6 jours)

| ID | Action | Effort | Impact |
|---|---|---|---|
| DRY-004 | Unifier les interfaces `Player` TypeScript → `CorePlayer` + extensions | L (2j) | Refactor large, nécessite MAJ composants |
| DRY-007 | Créer `EnumMapper` générique | M (3h) | Améliore safety, tests unitaires nécessaires |
| DRY-008/009 | Consolider `DraftParticipant`, `GameParticipant`, `DraftRules` | M (4h) | Frontend draft uniquement |
| DRY-011 | Opérateur RxJS `withTimeoutAndRethrow` | S (1h) | Repositories Angular |
| DRY-012 | Centraliser mapping `Player` en `PlayerDtoMapper` | M (3h) | Aligner avec migration hexagonale |

### Phase 3 — Backlog (P2, opportuniste)

| ID | Action | Effort |
|---|---|---|
| DRY-010 | Unifier `ApiResponse<T>` frontend | S |
| DRY-013 | Constante `EMPTY_REGION_DISTRIBUTION` | XS |
| DRY-014 | Supprimer les 3 blocs `if (hours ...)` redondants | XS |
| DRY-015 | Déléguer `calculateSeasonProgressPercentage` | XS |
| DRY-016 | Refactorer `TestDataBuilderTest` en vrai helper | S |
| DRY-017 | Réduire les 6 constantes à 2 | XS |

---

## Checklist qualité post-refactor

- [ ] SOLID respecté (SRP/OCP/LSP/ISP/DIP)
- [ ] Max 500 lignes par classe / Max 50 lignes par méthode
- [ ] 0 duplication détectée par re-scan Grep
- [ ] Nommage explicite, conventions respectées
- [ ] Tests unitaires ajoutés/MAJ sur tout helper extrait
- [ ] Pas de nouvelle dépendance non justifiée
- [ ] `DomainIsolationTest` toujours vert après extraction des helpers d'adapter

---

*Rapport généré le 2026-03-02 — Périmètre : 425 fichiers Java + 309 fichiers TypeScript*
