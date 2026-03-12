# Audit Conventions & Style — TECH-021
Date: 2026-03-02

## Résumé exécutif

- **Score conventions : 6.5/10**
- **Violations critiques : 4**
- **Violations majeures : 9**
- **Violations mineures : 7**

Le projet présente une bonne base de conventions, mais souffre de plusieurs incohérences accumulées au fil des sprints : mélange de suffixes DTO/Dto, nommage de logger non uniforme, méthodes stub avec `return true` sans implémentation, corruption UTF-8 dans les commentaires JavaDoc, et mélange des patterns `@import` (déprécié) vs `@use` dans les SCSS.

---

## Backend Java

### 1. Nommage — Classes et DTOs (MAJEUR)

**Violation : Mélange de suffixes `DTO` (ALLCAPS) et `Dto` (mixedCase) dans le même package.**

Les classes de transfert de données n'ont pas de convention uniforme :

| Convention `DTO` (ALLCAPS) | Convention `Dto` (mixedCase) |
|---|---|
| `LeaderboardEntryDTO` | `CounterTradeRequestDto` |
| `LeaderboardStatsDTO` | `DraftDto` / `DraftPickDto` |
| `PlayerLeaderboardEntryDTO` | `GameDetailDto` / `GameDto` |
| `PronostiqueurLeaderboardEntryDTO` | `TradeRequestDto` / `TradeResponseDto` |
| `TeamDTO` / `TeamPlayerDTO` / `PlayerDTO` (inner, ApiController) | `TeamDto` / `TeamPlayerDto` (dto/team/) |

Fichiers concernés :
- `src/main/java/com/fortnite/pronos/dto/LeaderboardEntryDTO.java:17`
- `src/main/java/com/fortnite/pronos/dto/LeaderboardStatsDTO.java:11`
- `src/main/java/com/fortnite/pronos/dto/PlayerLeaderboardEntryDTO.java:11`
- `src/main/java/com/fortnite/pronos/dto/PronostiqueurLeaderboardEntryDTO.java:14`
- `src/main/java/com/fortnite/pronos/controller/ApiController.java:221` (inner classes `TeamDTO`, `TeamPlayerDTO`, `PlayerDTO`)
- `src/main/java/com/fortnite/pronos/dto/CounterTradeRequestDto.java:19`

**Règle à appliquer :** Unifier sur `Dto` (convention Java moderne, évite la confusion avec les acronymes).

---

### 2. Nommage — Logger non uniforme (MINEUR)

**Violation : Le champ Logger n'a pas de convention unifiée entre `log`, `LOG` et `LOGGER`.**

Extrait :
- `src/main/java/com/fortnite/pronos/service/util/TeamRosterSanitizer.java:14` → `private static final Logger log = ...` (camelCase, non-UPPER_SNAKE_CASE)
- `src/main/java/com/fortnite/pronos/controller/ApiController.java:39` → `private static final Logger LOG = ...`
- `src/main/java/com/fortnite/pronos/util/LoggingUtils.java:193` → `private static final Logger LOGGER = ...`
- `src/main/java/com/fortnite/pronos/service/ingestion/PrCsvParser.java:21` → `private static final Logger LOG = ...`

**Règle à appliquer :** Les loggers créés manuellement via `LoggerFactory.getLogger()` doivent être `UPPER_SNAKE_CASE` (`LOG` ou `LOGGER`). Les classes annotées `@Slf4j` (Lombok) utilisent `log` implicitement — cela est accepté. La coexistence des deux méthodes dans le même projet crée une incohérence.

---

### 3. Nommage — Méthodes de test : mélange de conventions (MAJEUR)

Deux conventions coexistent dans les tests :

**Convention A (préférable) : `shouldXxx()` avec `@DisplayName`**
```
// GameStatisticsServiceTest.java:53
@Test
@DisplayName("devrait calculer correctement la distribution des joueurs par region")
void shouldCalculateRegionDistributionCorrectly() {
```

**Convention B : `methodName_whenCondition_thenResult()` sans `@DisplayName` systématique**
```
// AdminDraftRosterServiceTest.java:125
@Test
void assignPlayer_whenValid_createsDraftPick() {

// DraftSimultaneousControllerTest.java:50
@Test
void whenPickValid_submitsAndReturns200() {
```

**Convention C : `methodName_usesServiceResult_whenCondition()` sans `@DisplayName`**
```
// LeaderboardControllerTest.java:40
@Test
void getLeaderboard_usesServiceResult_whenCacheIsEmpty() {
```

Fichiers avec la Convention C (sans `@DisplayName` sur la classe) :
- `src/test/java/com/fortnite/pronos/controller/LeaderboardControllerTest.java:30`

**Règle à appliquer :** Choisir une convention et l'appliquer uniformément. La recommandation est `should...When...()` + `@DisplayName` en français.

---

### 4. Taille des classes et méthodes (CRITIQUE)

#### Classes dépassant la limite de 500 lignes

| Fichier | Lignes | Statut |
|---|---|---|
| `src/main/java/.../controller/DraftController.java` | 657 | CRITIQUE (+157) |
| `src/main/java/.../domain/game/model/Game.java` | 551 | CRITIQUE (+51) |
| `src/main/java/.../controller/GameController.java` | 537 | CRITIQUE (+37) |
| `src/main/java/.../service/draft/DraftService.java` | 520 | CRITIQUE (+20) |
| `src/main/java/.../model/Game.java` (legacy) | 511 | CRITIQUE (+11) |
| `src/main/java/.../service/TradingService.java` | 459 | A surveiller |
| `src/main/java/.../service/CsvDataLoaderService.java` | 418 | A surveiller |
| `src/main/java/.../service/TeamService.java` | 385 | OK |
| `src/main/java/.../adapter/out/persistence/game/GameEntityMapper.java` | 382 | OK |

**4 classes dépassent la limite des 500 lignes de CLAUDE.md.**

---

### 5. Méthode stub avec retour incorrect (CRITIQUE)

**Violation : `DraftService.isUserTurn()` contient une implémentation placeholder qui retourne toujours `true`.**

```java
// src/main/java/com/fortnite/pronos/service/draft/DraftService.java:181-190
public boolean isUserTurn(com.fortnite.pronos.model.Draft draft, UUID userId) {
  if (!com.fortnite.pronos.model.Draft.Status.ACTIVE.equals(draft.getStatus())) {
    return false;
  }
  // Cette méthode nécessiterait l'accès aux participants
  // Pour l'instant, on retourne true pour éviter l'erreur de compilation
  return true;  // BUG : retourne toujours true pour tout draft actif
}
```

Ce stub non implémenté peut permettre à n'importe quel utilisateur d'effectuer un pick à la place d'un autre dans un draft actif. La méthode `isUserTurnForGame()` (ligne 304) est la version correcte et implémentée.

---

### 6. Utilisation de `com.fortnite.pronos.model.*` inline (MAJEUR)

**Violation : Les types legacy sont référencés en FQN (Fully Qualified Name) inline au lieu d'être importés.**

Exemples dans `DraftService.java` :
- Ligne 52 : `public com.fortnite.pronos.model.Draft createDraft(com.fortnite.pronos.model.Game game, ...)`
- Ligne 57 : `com.fortnite.pronos.model.Draft draft = new com.fortnite.pronos.model.Draft(game);`
- Ligne 83 : `public com.fortnite.pronos.model.Draft nextPick(com.fortnite.pronos.model.Draft draft, ...)`

Idem dans `TradingService.java` :
- Ligne 45 : `public com.fortnite.pronos.model.Trade proposeTradeWithPlayerIds(...)`
- Ligne 113 : `public com.fortnite.pronos.model.Trade acceptTrade(UUID tradeId, UUID userId)`

Ce pattern indique que ces services sont en cours de migration hexagonale mais n'ont pas finalisé la suppression des imports du modèle legacy.

---

### 7. Corruption UTF-8 dans les commentaires JavaDoc (CRITIQUE)

**Violation : Le fichier `DraftController.java` contient une corruption UTF-8 massive dans son JavaDoc (lignes 31-80).**

Extrait de `src/main/java/com/fortnite/pronos/controller/DraftController.java:34` :
```
* ContrÃƒÆ'Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ'Ã¢â‚¬ ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ'...
```

La même corruption est visible dans `DraftService.java` :
- Ligne 33 : `Service pour la gestion des drafts Clean Code : sÃƒÆ'Ã†'Ãƒâ€šÃ‚Â©paration des responsabilitÃƒÆ'Ã†'Ãƒâ€šÃ‚Â©s`
- Ligne 55 : `log.debug("CrÃƒÆ'Ã†â€™Ãƒâ€šÃ‚Â©ation d'un draft pour la game {}"...`

C'est une double-encodage UTF-8 connu (décrit dans MEMORY.md). Rend le code illisible en débogage.

---

### 8. Méthode `mapToInt` avec lambda redondant (MINEUR)

**Violation : Utilisation d'une lambda là où une référence de méthode suffit (violation Clean Code / Sonar S1612).**

```java
// src/main/java/com/fortnite/pronos/service/draft/DraftService.java:79
return game.getRegionRules().stream().mapToInt(rule -> rule.getMaxPlayers()).sum();
// Devrait être :
return game.getRegionRules().stream().mapToInt(GameRegionRule::getMaxPlayers).sum();
```

---

### 9. Annotation `@DisplayName` absente sur certaines classes de test (MINEUR)

**Violation : `LeaderboardControllerTest` n'a pas d'annotation `@DisplayName` au niveau de la classe.**

```java
// src/test/java/com/fortnite/pronos/controller/LeaderboardControllerTest.java:29-30
@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {  // Pas de @DisplayName
```

Contraste avec les fichiers conformes :
- `GameNotificationServiceTest.java:24` → `@DisplayName("GameNotificationService Tests")`
- `UserServiceTest.java:27` → `@DisplayName("Tests - UserService")`
- `GameStatisticsServiceTest.java:30` → pas de @DisplayName de classe mais @DisplayName par test

---

### 10. Duplication de classes inner-classes DTO dans ApiController (MAJEUR)

**Violation : `ApiController.java:221` définit `TeamDTO`, `TeamPlayerDTO`, `PlayerDTO` comme inner classes, dupliquant les types existants dans `dto/team/TeamDto.java` et `dto/player/PlayerDto.java`.**

Ces classes inner ne respectent pas SRP (Single Responsibility Principle) : un contrôleur ne devrait pas définir des modèles de données.

---

## Frontend TypeScript

### 11. Fichier de composant sans suffixe `.component.ts` (MINEUR)

**Violation : `data-source-indicator.ts` ne respecte pas la convention Angular de nommage `*.component.ts`.**

```
frontend/src/app/shared/components/data-source-indicator/data-source-indicator.ts
```

Tous les autres composants Angular du projet utilisent le pattern `*.component.ts`. Ce fichier devrait être renommé `data-source-indicator.component.ts`.

De plus, la classe exportée s'appelle `DataSourceIndicator` au lieu de `DataSourceIndicatorComponent` :
```typescript
// frontend/src/app/shared/components/data-source-indicator/data-source-indicator.ts:23
export class DataSourceIndicator implements OnInit, OnDestroy {
```

---

### 12. Usage de `any` non typé (MAJEUR)

**Violation : `dashboard.component.ts:120` déclare un tableau sans type.**

```typescript
// frontend/src/app/features/dashboard/dashboard.component.ts:120
dashboardLeaderboard: any[] = [];
```

Violation du principe de typage fort TypeScript. Un type `LeaderboardEntryDTO[]` ou une interface dédiée devrait être utilisé.

---

### 13. Constante UPPER_SNAKE_CASE dans une propriété de classe TypeScript (MINEUR)

**Violation : `REFRESH_INTERVAL` est déclaré comme propriété private dans `dashboard.component.ts`, ce qui mélange la convention des constantes module-level avec les membres de classe.**

```typescript
// frontend/src/app/features/dashboard/dashboard.component.ts:112
private readonly REFRESH_INTERVAL = 300000; // 5 minutes
```

Les membres de classe Angular doivent être en `camelCase`. Les constantes en `UPPER_SNAKE_CASE` appartiennent au niveau module (`const REFRESH_INTERVAL = 300000`). Cette ambiguïté est une violation de Clean Code.

---

### 14. Mélange de `@import` (déprécié) et `@use` dans les SCSS (MAJEUR)

**Violation : Les fichiers SCSS utilisent à la fois `@import` (déprécié en Sass moderne) et `@use` (standard actuel), sans cohérence.**

Fichiers utilisant encore `@import` :
- `frontend/src/app/shared/components/snake-order-bar/snake-order-bar.component.scss:1` → `@import '../../../shared/styles/mixins';`
- `frontend/src/app/features/leaderboard/simple-leaderboard.component.scss:2` → `@import '../../shared/styles/mixins';`
- `frontend/src/app/shared/components/coin-flip-animation/coin-flip-animation.component.scss:1` → `@import '../../../shared/styles/mixins';`
- `frontend/src/app/features/trades/trade-list/trade-list.component.scss:2` → `@import '../../../shared/styles/_mixins.scss';`
- `frontend/src/app/features/teams/team-detail/team-detail.scss:2` → `@import '../../../shared/styles/_mixins.scss';`
- `frontend/src/app/features/game/game-home/game-home.component.scss:2` → `@import '../../../shared/styles/mixins';`

Fichiers conformes utilisant `@use` :
- `frontend/src/app/features/admin/admin-dashboard/admin-dashboard.component.scss:1` → `@use '../../../shared/styles/mixins' as mixins;`
- `frontend/src/app/features/admin/error-journal/error-journal.component.scss:1` → `@use '../../../shared/styles/mixins' as mixins;`

Au total, **au moins 20 fichiers SCSS utilisent encore `@import`** au lieu de `@use`.

---

### 15. Injection via constructeur vs `inject()` non uniformisée (MINEUR)

**Violation : Coexistence des patterns d'injection `constructor()` et `inject()` dans les mêmes couches.**

Pattern `inject()` (recommandé dans MEMORY.md pour les composants standalone) :
```typescript
// admin-user-list.component.ts:35
private readonly adminService = inject(AdminService);
```

Pattern constructeur (ancien, mais encore utilisé dans des services récents) :
```typescript
// data-source-indicator.ts:29
constructor(private dataSourceStrategy: DataSourceStrategy) {}

// core/services/team.service.ts:31
constructor(private http: HttpClient, private userContextService: UserContextService) {}

// dashboard.component.ts:122
constructor(
  private http: HttpClient,
  private router: Router,
  ...
)
```

Les composants standalone (`DashboardComponent` est `standalone: true`) devraient préférer `inject()` selon les guidelines du projet. La coexistence n'est pas une erreur bloquante, mais crée une incohérence stylistique.

---

### 16. Nom de fichier SCSS sans préfixe underscore pour les partials (MINEUR)

**Violation : `team-detail.scss` (sans `.component.`) existe aux côtés de fichiers `.component.scss`, et le répertoire `team-list/team-list.scss` n'a pas non plus le suffixe `.component`.**

```
frontend/src/app/features/teams/team-detail/team-detail.scss         ← manque .component
frontend/src/app/features/teams/team-list/team-list.scss             ← manque .component
```

Tous les autres composants du projet utilisent `*.component.scss`. Ces deux fichiers brisent la convention.

---

### 17. Utilisation de `assertEquals()` JUnit4 au lieu d'AssertJ (MINEUR)

**Violation : `LeaderboardControllerTest.java` utilise `assertEquals` de JUnit 5 (`org.junit.jupiter.api.Assertions`) alors que le reste du projet utilise AssertJ (`assertThat`).**

```java
// src/test/java/com/fortnite/pronos/controller/LeaderboardControllerTest.java:3
import static org.junit.jupiter.api.Assertions.*;

// Ligne 49
assertEquals(200, response.getStatusCodeValue());  // Déprécié depuis Spring 6
```

Le reste des tests du projet utilise systématiquement AssertJ :
```java
// PlayerControllerTest.java:36 (conforme)
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
```

De plus, `response.getStatusCodeValue()` est déprécié depuis Spring 6 — utiliser `response.getStatusCode().value()`.

---

## Taille des classes Frontend

| Fichier | Lignes | Statut |
|---|---|---|
| `features/auth/login/login.component.ts` | 602 | CRITIQUE (+102) |
| `shared/components/main-layout/main-layout.component.ts` | 523 | CRITIQUE (+23) |
| `features/trades/components/trading-dashboard/trading-dashboard.component.ts` | 492 | A surveiller |
| `features/trades/components/trade-proposal/trade-proposal.component.ts` | 491 | A surveiller |
| `features/trades/trade-form/trade-form.component.ts` | 489 | A surveiller |
| `features/dashboard/dashboard.component.ts` | 489 | A surveiller |
| `core/services/leaderboard.service.ts` | 487 | A surveiller |

**2 composants frontend dépassent la limite de 500 lignes.**

---

## TODOs/FIXMEs

Aucun `TODO`, `FIXME`, `HACK` ou `XXX` trouvé dans le code Java, TypeScript ou SCSS. **Bonne pratique respectée.**

---

## Top violations à corriger (par priorité)

### CRITIQUE

| # | Finding | Fichier:Ligne | Impact |
|---|---|---|---|
| C1 | Méthode stub `isUserTurn()` retourne toujours `true` — faille logique | `service/draft/DraftService.java:181` | Sécurité / logique métier |
| C2 | `DraftController.java` dépasse 500 lignes (657) | `controller/DraftController.java:1` | Maintenabilité |
| C3 | `GameController.java` dépasse 500 lignes (537) | `controller/GameController.java:1` | Maintenabilité |
| C4 | Corruption UTF-8 massive dans les JavaDoc de `DraftController` et `DraftService` | `DraftController.java:34`, `DraftService.java:33` | Lisibilité |

### MAJEUR

| # | Finding | Fichier:Ligne | Impact |
|---|---|---|---|
| M1 | Mélange `DTO`/`Dto` dans les DTOs — 4 classes avec `DTO` vs 20+ avec `Dto` | `dto/LeaderboardEntryDTO.java:17` | Cohérence |
| M2 | Inner classes `TeamDTO`/`PlayerDTO` dans `ApiController` dupliquent `dto/team/TeamDto` | `controller/ApiController.java:221` | DRY / SRP |
| M3 | Types legacy référencés en FQN inline plutôt qu'importés | `service/draft/DraftService.java:52` | Lisibilité |
| M4 | `login.component.ts` dépasse 500 lignes (602) | `login/login.component.ts:1` | Maintenabilité |
| M5 | `main-layout.component.ts` dépasse 500 lignes (523) | `main-layout/main-layout.component.ts:1` | Maintenabilité |
| M6 | 20+ fichiers SCSS utilisent `@import` déprécié au lieu de `@use` | ex: `snake-order-bar.component.scss:1` | Compatibilité Sass |
| M7 | `dashboardLeaderboard: any[]` non typé | `dashboard.component.ts:120` | Typage TypeScript |
| M8 | `LeaderboardControllerTest` sans `@DisplayName` et avec `assertEquals` JUnit4 | `LeaderboardControllerTest.java:30` | Convention tests |
| M9 | Mélange des conventions de nommage des tests (`shouldX`, `methodName_whenX`, `whenX`) | Multiples fichiers | Cohérence |

### MINEUR

| # | Finding | Fichier:Ligne | Impact |
|---|---|---|---|
| m1 | Logger `log` (camelCase) dans une constante `static final` | `service/util/TeamRosterSanitizer.java:14` | Convention UPPER_SNAKE_CASE |
| m2 | `REFRESH_INTERVAL` en UPPER_SNAKE_CASE dans propriété de classe | `dashboard.component.ts:112` | Convention |
| m3 | Fichier `data-source-indicator.ts` (pas `.component.ts`), classe non suffixée `Component` | `data-source-indicator/data-source-indicator.ts:23` | Convention Angular |
| m4 | `team-detail.scss` et `team-list.scss` sans suffixe `.component` | `team-detail/team-detail.scss:1` | Convention |
| m5 | `mapToInt(rule -> rule.getMaxPlayers())` → utiliser référence de méthode | `service/draft/DraftService.java:79` | Clean Code |
| m6 | Mélange `inject()` / constructeur pour DI dans les composants standalone | `dashboard.component.ts:122` | Cohérence guideline |
| m7 | `response.getStatusCodeValue()` déprécié Spring 6 | `LeaderboardControllerTest.java:49` | Compatibilité |

---

## Synthèse

| Catégorie | Résultat |
|---|---|
| Annotations Spring (@Service/@Component/@Repository) | Conformes — 185 classes annotées correctement |
| PascalCase classes Java | Conforme |
| camelCase méthodes Java | Conforme |
| UPPER_SNAKE_CASE constantes Java | 1 violation (Logger `log` dans TeamRosterSanitizer) |
| Packages en minuscules | Conforme |
| @DisplayName dans les tests | Présent dans 146/170 fichiers de test — bonne couverture |
| Sélecteurs Angular kebab-case `app-*` | Conforme (60 composants vérifiés) |
| Fichiers Angular `*.component.ts` | 1 violation (`data-source-indicator.ts`) |
| SCSS — variables custom CSS props | Bien utilisées (`_tokens.scss`, `--color-*`, `--space-*`) |
| SCSS — `@use` vs `@import` | Mélange — ~20 fichiers encore sur `@import` |
| TODOs/FIXMEs non résolus | **0** — Excellente hygiène |
