# Story 6.3: Affichage leaderboard ordonné

Status: done

## Story

As a participant,
I want voir un leaderboard trié par delta PR décroissant pour ma partie,
so that je connaisse immédiatement le classement courant et la progression de chaque équipe.

## Acceptance Criteria

1. **Given** une partie a des `TeamScoreDelta` calculés, **When** un utilisateur authentifié appelle `GET /api/games/{gameId}/leaderboard`, **Then** il reçoit la liste des entrées triées par `deltaPr` décroissant, chaque entrée ayant : `rank` (1-based), `participantId`, `username`, `deltaPr`, `periodStart`, `periodEnd`, `computedAt`.

2. **Given** deux participants ont le même `deltaPr`, **When** le leaderboard est calculé, **Then** ils reçoivent le même rang et le suivant est incrémenté (ex. 2 participants à rang 1 → rang 3 pour le suivant — dense rank non requis, standard rank acceptable).

3. **Given** aucun delta n'a encore été calculé pour une partie (batch pas encore passé), **When** le leaderboard est demandé, **Then** la liste retournée est vide (200 OK, `[]`).

4. **Given** un utilisateur non authentifié appelle l'endpoint, **When** la requête arrive, **Then** le serveur répond 401.

5. **Given** le leaderboard est chargé dans l'interface `/games/:id/leaderboard`, **When** la page s'affiche, **Then** les équipes sont classées par delta PR décroissant, avec le username du participant, le rang, et la valeur de delta PR (affichée en +/- PR).

6. **Given** aucune donnée n'est disponible (liste vide), **When** la page charge, **Then** un message "Aucun score disponible — le calcul quotidien n'a pas encore été exécuté" est affiché.

7. **Given** le chargement échoue (erreur réseau ou 500), **When** la page tente de charger, **Then** un message d'erreur est affiché et la page ne plante pas.

8. **Given** le leaderboard charge correctement, **When** les données sont affichées, **Then** la date de dernière mise à jour (`computedAt` de la première entrée) est visible (ex. "Mis à jour le 28/02/2026 à 08:00").

## Technical Context

### FR Coverage
- **FR-38**: Le leaderboard affiche les équipes classées par delta PR décroissant, visible par tous les participants de la partie
- **NFR-P04**: Le leaderboard d'une partie se charge < 2 secondes pour 20 participants

### Analyse de l'existant — ce qui est DÉJÀ là

#### Backend — infrastructure disponible
- **`TeamScoreDelta`** domain model (`domain/team/model/`) — fields: `id`, `gameId`, `participantId`, `periodStart`, `periodEnd`, `deltaPr`, `computedAt` (Story 6.2)
- **`TeamScoreDeltaRepositoryPort`** — `findByGameId(UUID)`, `findByGameIdAndParticipantId()`, `save()` (Story 6.2)
- **`TeamScoreDeltaRepositoryAdapter`** + JPA entity + `TeamScoreDeltaJpaRepository` (Story 6.2)
- **`GameParticipantRepositoryPort`** — `findByGameIdWithUserFetch(UUID gameId)` retourne `List<GameParticipant>` avec `user` chargé (nom via `gp.getUser().getUsername()`)
- **`LeaderboardController`** existant à `/api/leaderboard` — utilise `TeamLeaderboardService` (ancien modèle `ScoreRepository`). **NE PAS MODIFIER.** Créer un controller séparé.
- Index DB déjà présent sur `team_score_deltas (game_id, delta_pr DESC)` — requête triée rapide

#### Frontend — infrastructure disponible
- Route `/games/:id/leaderboard` dans `game-routing.module.ts` → charge `LeaderboardModule` → `SimpleLeaderboardComponent` (montre le leaderboard de **joueurs** Fortnite, pas les équipes par delta PR)
- `LeaderboardService` (`core/services/leaderboard.service.ts`) — n'a pas de méthode `getGameDeltaLeaderboard(gameId)`
- Styles SCSS : `@use '../../shared/styles/mixins' as mixins` — `glass-card`, `gaming-gradient-bg`, `neon-glow`, `gaming-button` disponibles
- TranslationService : `public readonly t = inject(TranslationService)` — `t.t('key')` en template
- Pattern standalone OnPush : `ChangeDetectionStrategy.OnPush` + `cdr.markForCheck()` après async

#### Frontend — gap route leaderboard
La route `':id/leaderboard'` dans `game-routing.module.ts` charge `LeaderboardModule` qui affiche les **joueurs Fortnite** (player leaderboard). Story 6.3 doit la remplacer pour afficher le leaderboard **équipes par delta PR** (Team Score Delta Leaderboard).

### Gap 1 — Backend : DTO `TeamDeltaLeaderboardEntryDto`

```java
// src/main/java/com/fortnite/pronos/dto/TeamDeltaLeaderboardEntryDto.java
// Java record (Spring 3.3 / Java 21 compatible)
public record TeamDeltaLeaderboardEntryDto(
    int rank,
    UUID participantId,
    String username,
    int deltaPr,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDateTime computedAt
) {}
```

### Gap 2 — Backend : Service `TeamDeltaLeaderboardService` (2 deps — sous max 7)

```java
// src/main/java/com/fortnite/pronos/service/leaderboard/TeamDeltaLeaderboardService.java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamDeltaLeaderboardService {

  private final TeamScoreDeltaRepositoryPort deltaRepository;       // 1
  private final GameParticipantRepositoryPort participantRepository; // 2

  public List<TeamDeltaLeaderboardEntryDto> getLeaderboard(UUID gameId) {
    // 1. Build participant username map (1 DB call with user fetch)
    Map<UUID, String> usernameByParticipantId = buildUsernameMap(gameId);

    // 2. Fetch deltas, sort DESC by deltaPr
    List<TeamScoreDelta> deltas = deltaRepository.findByGameId(gameId);
    deltas.sort(Comparator.comparingInt(TeamScoreDelta::getDeltaPr).reversed());

    // 3. Assign rank (standard rank — same deltaPr same rank)
    return assignRanks(deltas, usernameByParticipantId);
  }

  private Map<UUID, String> buildUsernameMap(UUID gameId) {
    return participantRepository.findByGameIdWithUserFetch(gameId).stream()
        .filter(gp -> gp.getUser() != null)
        .collect(Collectors.toMap(GameParticipant::getId, GameParticipant::getUsername));
  }

  private List<TeamDeltaLeaderboardEntryDto> assignRanks(
      List<TeamScoreDelta> sorted, Map<UUID, String> usernameByParticipantId) {
    List<TeamDeltaLeaderboardEntryDto> result = new ArrayList<>();
    int rank = 1;
    for (int i = 0; i < sorted.size(); i++) {
      TeamScoreDelta delta = sorted.get(i);
      if (i > 0 && sorted.get(i).getDeltaPr() < sorted.get(i - 1).getDeltaPr()) {
        rank = i + 1; // Standard rank — tied entries keep same rank, next increments
      }
      result.add(new TeamDeltaLeaderboardEntryDto(
          rank,
          delta.getParticipantId(),
          usernameByParticipantId.getOrDefault(delta.getParticipantId(), "—"),
          delta.getDeltaPr(),
          delta.getPeriodStart(),
          delta.getPeriodEnd(),
          delta.getComputedAt()
      ));
    }
    return result;
  }
}
```

**ATTENTION** : La méthode `findByGameIdWithUserFetch(UUID gameId)` existe déjà dans `GameParticipantRepositoryPort`. Vérifier son implémentation dans `GameParticipantRepository` — utiliser `@Query` si Hibernate 6 SQM pose problème (relation `@ManyToOne Game game`).

### Gap 3 — Backend : Contrôleur `GameLeaderboardController`

```java
// src/main/java/com/fortnite/pronos/controller/GameLeaderboardController.java
@Slf4j
@RestController
@RequestMapping("/api/games/{gameId}/leaderboard")
@RequiredArgsConstructor
public class GameLeaderboardController {

  private final TeamDeltaLeaderboardService leaderboardService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<TeamDeltaLeaderboardEntryDto>> getLeaderboard(
      @PathVariable UUID gameId) {
    log.info("GameLeaderboard: fetching leaderboard for game={}", gameId);
    List<TeamDeltaLeaderboardEntryDto> leaderboard = leaderboardService.getLeaderboard(gameId);
    log.info("GameLeaderboard: returned {} entries for game={}", leaderboard.size(), gameId);
    return ResponseEntity.ok(leaderboard);
  }
}
```

**Sécurité** : `@PreAuthorize("isAuthenticated()")` — tous les utilisateurs connectés peuvent consulter. Pas de restriction ADMIN.
**LayeredArchitectureTest** : Controller → Service → Port ✓ (pas de JPA repo direct)
**DependencyInversionTest** : Controller ne dépend que du service, pas de JPA ✓

### Gap 4 — Frontend : Interface TypeScript

```typescript
// frontend/src/app/features/leaderboard/models/team-delta-leaderboard.model.ts
export interface TeamDeltaLeaderboardEntry {
  rank: number;
  participantId: string;
  username: string;
  deltaPr: number;
  periodStart: string;  // LocalDate serialized as "YYYY-MM-DD"
  periodEnd: string;
  computedAt: string;   // LocalDateTime serialized as ISO string
}
```

### Gap 5 — Frontend : Méthode dans `LeaderboardService`

```typescript
// Ajouter dans frontend/src/app/core/services/leaderboard.service.ts
getGameDeltaLeaderboard(gameId: string): Observable<TeamDeltaLeaderboardEntry[]> {
  const url = `${environment.apiUrl}/api/games/${gameId}/leaderboard`;
  return this.http.get<TeamDeltaLeaderboardEntry[]>(url).pipe(
    catchError(error => {
      this.logger.error('LeaderboardService: failed to load game delta leaderboard', error);
      return throwError(() => error);
    })
  );
}
```

### Gap 6 — Frontend : Composant `GameLeaderboardPageComponent`

```
frontend/src/app/features/leaderboard/game-leaderboard-page/
  game-leaderboard-page.component.ts
  game-leaderboard-page.component.html
  game-leaderboard-page.component.scss
  game-leaderboard-page.component.spec.ts
```

**Comportement :**
- Récupère `gameId` depuis `ActivatedRoute.params['id']` (parent game route)
- Appelle `leaderboardService.getGameDeltaLeaderboard(gameId)` au `ngOnInit`
- Affiche : loading → data (table classement) OU empty state OU error state
- Colonnes : Rang | Participant | Delta PR | Mise à jour
- Delta PR : formaté `+500 PR` ou `-200 PR` selon signe
- Date mise à jour : `computedAt` de la première entrée (si liste non vide)
- OnPush ChangeDetection + `cdr.markForCheck()` après subscribe

**Pattern DI :**
```typescript
private readonly route = inject(ActivatedRoute);
private readonly leaderboardService = inject(LeaderboardService);
private readonly cdr = inject(ChangeDetectorRef);
public readonly t = inject(TranslationService);
```

### Gap 7 — Frontend : Mise à jour route

```typescript
// frontend/src/app/features/game/game-routing.module.ts
// Remplacer :
{ path: ':id/leaderboard', loadChildren: () => import('../leaderboard/leaderboard.module').then(m => m.LeaderboardModule) },
// Par :
{ path: ':id/leaderboard', loadComponent: () => import('../leaderboard/game-leaderboard-page/game-leaderboard-page.component').then(c => c.GameLeaderboardPageComponent) },
```

**NOTE** : La route globale `/leaderboard` (dans `app.routes.ts`) charge toujours `LeaderboardModule` → `SimpleLeaderboardComponent` → inchangée.

### Gap 8 — Frontend : Clés i18n (4 langues)

Ajouter dans `fr.json`, `en.json`, `es.json`, `pt.json` :

```json
"gameLeaderboard": {
  "title": "Classement de la partie",
  "rank": "Rang",
  "participant": "Participant",
  "deltaPr": "Delta PR",
  "lastUpdate": "Mise à jour",
  "lastUpdateAt": "Mis à jour le {date}",
  "empty": "Aucun score disponible — le calcul quotidien n'a pas encore été exécuté.",
  "error": "Impossible de charger le classement. Veuillez réessayer.",
  "loading": "Chargement du classement...",
  "plus": "+{value} PR",
  "minus": "{value} PR"
}
```

### Contraintes architecture

- **CouplingTest** : `TeamDeltaLeaderboardService` : 2 deps ✓
- **NamingConventionTest** : se termine par `Service` ✓, Controller ✓
- **DomainIsolationTest** : aucune nouvelle classe domaine dans cette story ✓
- **LayeredArchitectureTest** : `GameLeaderboardController` → `TeamDeltaLeaderboardService` → ports ✓
- **DependencyInversionTest** : controller → service uniquement (pas de JPA direct) ✓
- **Spotless** : `mvn spotless:apply` avant `mvn test`

### Pattern de test backend : `TeamDeltaLeaderboardServiceTest`

```java
@ExtendWith(MockitoExtension.class)
class TeamDeltaLeaderboardServiceTest {
  @Mock TeamScoreDeltaRepositoryPort deltaRepository;
  @Mock GameParticipantRepositoryPort participantRepository;
  // Tests :
  // 1. whenMultipleDeltas_sortedByDeltaPrDescWithRank
  // 2. whenNoDeltas_returnsEmptyList
  // 3. whenTiedDeltaPr_sameRankAssigned
  // 4. whenParticipantNotInMap_usernameIsDash
  // 5. whenSingleParticipant_rank1
}
```

### Pattern de test backend : `GameLeaderboardControllerTest`

```java
@WebMvcTest(GameLeaderboardController.class)
class GameLeaderboardControllerTest {
  // 1. whenAuthenticated_returns200WithEntries
  // 2. whenNoDeltas_returns200WithEmptyList
  // Tests @WithMockUser + mockMvc.perform(get("/api/games/{id}/leaderboard", gameId))
}
```

### Pattern de test frontend : `GameLeaderboardPageComponent` spec

Tests à couvrir (≥ 85% lignes) :
1. `should render loading state`
2. `should render entries when data loaded`
3. `should display rank, username, deltaPr for each entry`
4. `should display + prefix for positive deltaPr`
5. `should display - prefix for negative deltaPr`
6. `should display empty state when list is empty`
7. `should display error state on service failure`

## Tasks / Subtasks

- [x] Task 1: DTO `TeamDeltaLeaderboardEntryDto` (AC: #1)
  - [x] `src/main/java/com/fortnite/pronos/dto/TeamDeltaLeaderboardEntryDto.java` (Java record)

- [x] Task 2: Service `TeamDeltaLeaderboardService` + tests (AC: #1, #2, #3)
  - [x] `src/main/java/com/fortnite/pronos/service/leaderboard/TeamDeltaLeaderboardService.java`
  - [x] `src/test/java/com/fortnite/pronos/service/leaderboard/TeamDeltaLeaderboardServiceTest.java` (5 tests)

- [x] Task 3: Contrôleur `GameLeaderboardController` + tests (AC: #1, #3, #4)
  - [x] `src/main/java/com/fortnite/pronos/controller/GameLeaderboardController.java`
  - [x] `src/test/java/com/fortnite/pronos/controller/GameLeaderboardControllerTest.java` (2 tests)

- [x] Task 4: Interface TypeScript + `getGameDeltaLeaderboard()` dans `LeaderboardService` (AC: #5)
  - [x] `frontend/src/app/features/leaderboard/models/team-delta-leaderboard.model.ts`
  - [x] Ajouter `getGameDeltaLeaderboard(gameId)` dans `frontend/src/app/core/services/leaderboard.service.ts`
  - [x] Ajouter test `getGameDeltaLeaderboard` dans `leaderboard.service.spec.ts`

- [x] Task 5: Composant `GameLeaderboardPageComponent` (AC: #5, #6, #7, #8)
  - [x] `frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.ts`
  - [x] `frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.html`
  - [x] `frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.scss`
  - [x] `frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.spec.ts` (8 tests)

- [x] Task 6: Mise à jour route + i18n (AC: #5, #6)
  - [x] Modifier `frontend/src/app/features/game/game-routing.module.ts` : `:id/leaderboard` → `loadComponent` → `GameLeaderboardPageComponent`
  - [x] Ajouter clés `gameLeaderboard.*` dans `fr.json`, `en.json`, `es.json`, `pt.json`

## Dev Notes

### Architecture : 0 nouvelle violation

```
GameLeaderboardController
  └─→ TeamDeltaLeaderboardService
        ├─→ TeamScoreDeltaRepositoryPort (Story 6.2)
        └─→ GameParticipantRepositoryPort (existant)
```

Aucun service ne parle directement à un JPA repository.

### GameParticipantRepository — `findByGameIdWithUserFetch`

La méthode `findByGameIdWithUserFetch(UUID gameId)` existe dans `GameParticipantRepositoryPort`.
Vérifier son implémentation dans `GameParticipantRepository` :
```java
@Query("SELECT gp FROM GameParticipant gp JOIN FETCH gp.user WHERE gp.game.id = :gameId")
List<GameParticipant> findByGameIdWithUserFetch(@Param("gameId") UUID gameId);
```
Si la requête @Query n'existe pas, l'ajouter (Hibernate 6 SQM ne résout pas `gameId` sur une relation `@ManyToOne Game game`).

### `getUsername()` sur `GameParticipant`

`GameParticipant.getUsername()` retourne `user != null ? user.getUsername() : null`. Si `findByGameIdWithUserFetch` fait un JOIN FETCH user, `getUser()` est chargé → `getUsername()` fonctionne. Utiliser `gp.getUsername()` (méthode déléguée) plutôt que `gp.getUser().getUsername()` (Loi de Demeter).

### `@WebMvcTest` + service mock

`GameLeaderboardController` a 1 seule dépendance : `TeamDeltaLeaderboardService`. Dans `GameLeaderboardControllerTest`, utiliser `@MockBean TeamDeltaLeaderboardService`.
Si `VisitTrackingService` ou un filtre de sécurité nécessite un MockBean supplémentaire, l'ajouter (cf. mémoire : `@WebMvcTest` ne charge que le web layer).

### Frontend : `gameId` depuis la route

Le composant `GameLeaderboardPageComponent` vit à `/games/:id/leaderboard`. L'`ActivatedRoute` donne le param `id` du parent. Utiliser `route.params` ou `route.snapshot.parent?.params['id']` selon la configuration du routeur (tester les deux).

Alternative plus robuste : utiliser `ActivatedRoute.snapshot.parent?.parent?.params['id']` si le layout ajoute un niveau. Valider en test avec `convertToParamMap`.

### Format delta PR

Afficher `+500 PR` pour positif, `-200 PR` pour négatif, `0 PR` pour zéro. Utiliser une pipe ou une méthode helper dans le composant :
```typescript
formatDelta(value: number): string {
  if (value > 0) return `+${value} PR`;
  return `${value} PR`; // includes the minus sign naturally
}
```

### Tests frontend — pattern ActivatedRoute mock

```typescript
const mockRoute = { params: of({ id: 'game-123' }), snapshot: { parent: { params: { id: 'game-123' } } } };
providers: [{ provide: ActivatedRoute, useValue: mockRoute }]
```

### i18n — fichiers existants

Les 4 fichiers i18n sont dans `frontend/src/assets/i18n/`. La clé `gameLeaderboard` n'existe pas encore. L'ajouter dans chaque fichier avant ou après `leaderboard`.

### Spotless + tests

Toujours exécuter `mvn spotless:apply -q --no-transfer-progress && mvn test` avant de valider.

### Project Structure Notes

- DTOs backend : `src/main/java/com/fortnite/pronos/dto/` (pattern record Java)
- Services leaderboard : `src/main/java/com/fortnite/pronos/service/leaderboard/`
- Contrôleurs : `src/main/java/com/fortnite/pronos/controller/`
- Composant Angular : `frontend/src/app/features/leaderboard/game-leaderboard-page/`
- Le `LeaderboardModule` existant n'est pas modifié — la route game-spécifique migre vers `loadComponent`

### References

- `TeamScoreDelta` domain model : `src/main/java/com/fortnite/pronos/domain/team/model/TeamScoreDelta.java`
- `TeamScoreDeltaRepositoryPort` : `src/main/java/com/fortnite/pronos/domain/port/out/TeamScoreDeltaRepositoryPort.java`
- `GameParticipantRepositoryPort` : `src/main/java/com/fortnite/pronos/domain/port/out/GameParticipantRepositoryPort.java`
- Routing game : `frontend/src/app/features/game/game-routing.module.ts`
- LeaderboardService frontend : `frontend/src/app/core/services/leaderboard.service.ts`
- Story 6.2 (contexte batch) : `_bmad-output/implementation-artifacts/6-2-calcul-quotidien-du-delta-pr.md`
- epics.md FR-38 / NFR-P04 : `_bmad-output/planning-artifacts/epics.md#Story-6.3`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- Backend: 2200 run, 19 failures + 1 error (all pre-existing known failures)
- Frontend: 2082/2082 SUCCESS (+36 tests vs baseline 2046)
- New tests green: TeamDeltaLeaderboardServiceTest (5), GameLeaderboardControllerTest (2), GameLeaderboardPageComponent spec (8), LeaderboardService getGameDeltaLeaderboard (2)

### Completion Notes List

- Route `:id/leaderboard` migrated from `loadChildren: LeaderboardModule` to `loadComponent: GameLeaderboardPageComponent`
- GameParticipant.getUsername() used (Law of Demeter delegation) instead of gp.getUser().getUsername()
- Standard rank algorithm (tied entries share rank, next increment = position+1)
- i18n added in all 4 languages (fr, en, es, pt)

### File List

- src/main/java/com/fortnite/pronos/dto/TeamDeltaLeaderboardEntryDto.java (CREATED)
- src/main/java/com/fortnite/pronos/service/leaderboard/TeamDeltaLeaderboardService.java (CREATED)
- src/main/java/com/fortnite/pronos/controller/GameLeaderboardController.java (CREATED)
- src/test/java/com/fortnite/pronos/service/leaderboard/TeamDeltaLeaderboardServiceTest.java (CREATED)
- src/test/java/com/fortnite/pronos/controller/GameLeaderboardControllerTest.java (CREATED)
- frontend/src/app/features/leaderboard/models/team-delta-leaderboard.model.ts (CREATED)
- frontend/src/app/core/services/leaderboard.service.ts (MODIFIED - added getGameDeltaLeaderboard)
- frontend/src/app/core/services/leaderboard.service.spec.ts (MODIFIED - 2 new tests)
- frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.ts (CREATED)
- frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.html (CREATED)
- frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.scss (CREATED)
- frontend/src/app/features/leaderboard/game-leaderboard-page/game-leaderboard-page.component.spec.ts (CREATED)
- frontend/src/app/features/game/game-routing.module.ts (MODIFIED - route :id/leaderboard)
- frontend/src/assets/i18n/fr.json (MODIFIED - gameLeaderboard section)
- frontend/src/assets/i18n/en.json (MODIFIED - gameLeaderboard section)
- frontend/src/assets/i18n/es.json (MODIFIED - gameLeaderboard section)
- frontend/src/assets/i18n/pt.json (MODIFIED - gameLeaderboard section)
- src/test/java/com/fortnite/pronos/config/SecurityConfigGameLeaderboardAuthorizationTest.java (CREATED)

## Review Follow-ups (AI — post-code-review fixes)

### Fixes appliqués

**H1 — FIXED**: No security test for `GameLeaderboardController`. The `@PreAuthorize("isAuthenticated()")` annotation was not tested at the security filter level. Created `SecurityConfigGameLeaderboardAuthorizationTest` (1 test: `unauthenticatedCannotGetLeaderboard` → 401/403). Note: `GameLeaderboardControllerTest` uses plain Mockito (not `@WebMvcTest`) so Spring Security annotations are bypassed — the security config test provides the required coverage for AC #4.

### Action items

- [ ] **[AI-Review][Low][L1]**: `GameLeaderboardControllerTest` uses `@ExtendWith(MockitoExtension.class)` instead of `@WebMvcTest` — `@PreAuthorize` annotation not exercised in unit tests. The new `SecurityConfigGameLeaderboardAuthorizationTest` compensates. Consider migrating controller test to `@WebMvcTest` for deeper integration.
