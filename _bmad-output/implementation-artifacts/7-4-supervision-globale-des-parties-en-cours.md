# Story 7.4: Supervision globale des parties en cours

Status: done

## Story

As an admin,
I want une vue globale de toutes les parties actives (CREATING, DRAFTING, ACTIVE),
so that je puisse piloter l'exploitation sans ouvrir chaque game individuellement.

## Acceptance Criteria

1. `GET /api/admin/supervision/games` retourne 200 avec la liste de toutes les parties dont le statut est `CREATING`, `DRAFTING` ou `ACTIVE` (soft-deleted exclues).
2. Chaque entrÃ©e retournÃ©e contient : `gameId`, `gameName`, `status`, `draftMode`, `participantCount`, `maxParticipants`, `creatorUsername`, `createdAt`.
3. `GET /api/admin/supervision/games?status=DRAFTING` filtre par statut unique ; une valeur invalide retourne 400.
4. Un utilisateur anonyme reÃ§oit 401 ou 403 sur `GET /api/admin/supervision/games`.
5. Un utilisateur authentifiÃ© sans rÃ´le ADMIN reÃ§oit 403 sur `GET /api/admin/supervision/games`.
6. Un utilisateur ADMIN reÃ§oit 200 avec la liste correcte.
7. La route frontend `/admin/games` affiche le composant de supervision et est accessible uniquement aux admins (AdminGuard).
8. Le tableau frontend affiche : nom, badge statut colorÃ©, compte participants (X/Y), crÃ©ateur, date de crÃ©ation.
9. Des onglets "Toutes / En crÃ©ation / En draft / Active" filtrent la liste localement (sans nouveau call HTTP).
10. En cas d'erreur HTTP, la liste reste vide et une banniÃ¨re non-bloquante est affichÃ©e.

## Tasks / Subtasks

- [x] Task 1 â€” Backend : ajouter `findByStatusInWithFetch` sur `GameRepository` (AC: #1, #3)
  - [x] 1.1 Ajouter la mÃ©thode `findByStatusInWithFetch(Collection<GameStatus> statuses)` avec `@Query` JPQL FETCH JOIN (participants + user + creator) et filtre `deletedAt IS NULL`
  - [x] 1.2 Ordonner par `createdAt DESC`

- [x] Task 2 â€” Backend : crÃ©er `GameSupervisionDto` (AC: #2)
  - [x] 2.1 Record Java : `UUID gameId`, `String gameName`, `String status`, `String draftMode`, `int participantCount`, `int maxParticipants`, `String creatorUsername`, `LocalDateTime createdAt`
  - [x] 2.2 Placer dans `com.fortnite.pronos.dto.admin`

- [x] Task 3 â€” Backend : crÃ©er `AdminGameSupervisionService` (AC: #1, #2, #3)
  - [x] 3.1 1 seule dÃ©pendance : `GameRepository`
  - [x] 3.2 MÃ©thode `getAllActiveGames()` : appelle `findByStatusInWithFetch(List.of(CREATING, DRAFTING, ACTIVE))`, mappe vers `GameSupervisionDto`
  - [x] 3.3 MÃ©thode `getActiveGamesByStatus(GameStatus status)` : appelle `findByStatusInWithFetch(List.of(status))`, mappe vers `GameSupervisionDto`

- [x] Task 4 â€” Backend : crÃ©er `AdminGameSupervisionController` (AC: #1, #3, #4, #5, #6)
  - [x] 4.1 `@RestController @RequestMapping("/api/admin/supervision") @PreAuthorize("hasRole('ADMIN')")`
  - [x] 4.2 `GET /games` â€” appelle `getAllActiveGames()`, retourne `ResponseEntity<List<GameSupervisionDto>>`
  - [x] 4.3 `GET /games?status={status}` avec `@RequestParam(required = false) GameStatus status` â€” si null â†’ `getAllActiveGames()`, sinon â†’ `getActiveGamesByStatus(status)` ; statut invalide â†’ Spring lÃ¨ve `MethodArgumentTypeMismatchException` (400, dÃ©jÃ  gÃ©rÃ© par `GlobalExceptionHandler`)

- [x] Task 5 â€” Backend tests (AC: #1, #2, #3, #4, #5, #6)
  - [x] 5.1 `AdminGameSupervisionServiceTest` â€” 6 tests (4 getAllActiveGames + 2 getActiveGamesByStatus), tous verts
  - [x] 5.2 `AdminGameSupervisionControllerTest` â€” 3 tests (getAllGames x2 + getByStatus x1), tous verts
  - [x] 5.3 `SecurityConfigAdminSupervisionAuthorizationTest` â€” 4 tests @WebMvcTest (anonyme, non-admin, admin, admin+filter), tous verts

- [x] Task 6 â€” Frontend : Ã©tendre `admin.models.ts` (AC: #8)
  - [x] 6.1 Ajouter `export type GameSupervisionStatus = 'CREATING' | 'DRAFTING' | 'ACTIVE'`
  - [x] 6.2 Ajouter `export interface GameSupervisionEntry { ... }`

- [x] Task 7 â€” Frontend : Ã©tendre `AdminService` (AC: #8, #10)
  - [x] 7.1 Ajouter `getGamesSupervision(status?: GameSupervisionStatus): Observable<GameSupervisionEntry[]>`
  - [x] 7.2 URL : `GET ${environment.apiUrl}/api/admin/supervision/games` + `?status=${status}` si dÃ©fini
  - [x] 7.3 `catchError(() => of([]))` â€” erreur HTTP â†’ liste vide

- [x] Task 8 â€” Frontend : crÃ©er `AdminGamesSupervisionComponent` (AC: #7, #8, #9, #10)
  - [x] 8.1 Standalone component dans `frontend/src/app/features/admin/games-supervision/admin-games-supervision/`
  - [x] 8.2 Injecter `AdminService` via `inject()`
  - [x] 8.3 Signal `allGames = signal<GameSupervisionEntry[]>([])` + `selectedStatus = signal<GameSupervisionStatus | 'ALL'>('ALL')`
  - [x] 8.4 Signal computed `filteredGames = computed(...)` filtre `allGames` selon `selectedStatus`
  - [x] 8.5 `loading = signal(false)` + `loadError = false`
  - [x] 8.6 `ngOnInit` appelle `loadGames()` â†’ `adminService.getGamesSupervision()` (tous statuts), alimente `allGames`; erreur â†’ `loadError = true`
  - [x] 8.7 Template : 4 onglets filtrant localement, mat-table avec colonnes name/status/participants/creator/createdAt, loading spinner, banniÃ¨re erreur non-bloquante
  - [x] 8.8 Badge statut colorÃ© : `CREATING`â†’gris, `DRAFTING`â†’orange, `ACTIVE`â†’vert (via class CSS)

- [x] Task 9 â€” Frontend : ajouter la route dans `admin.routes.ts` (AC: #7)
  - [x] 9.1 Route `{ path: 'games', loadComponent: ... }` ajoutÃ©e

- [x] Task 10 â€” Frontend tests (AC: #7, #8, #9, #10)
  - [x] 10.1 `AdminGamesSupervisionComponent` spec â€” 10 tests, tous verts (crÃ©ation, liste, filtres par statut, Ã©tat vide, banniÃ¨re erreur, loadError=true, statusClass x3, spinner)
  - [x] 10.2 `AdminService` â€” 3 tests supplÃ©mentaires (GET sans param, GET avec ?status=DRAFTING, erreur HTTP â†’ [])

## Dev Notes

### Backend

**`GameRepository` â€” nouvelle mÃ©thode Ã  ajouter** (aprÃ¨s `findByStatusWithFetch`) :
```java
/** OPTIMISÃ‰: Trouve les parties par liste de statuts avec fetch (supervision admin) */
@Query(
    "SELECT DISTINCT g FROM Game g "
        + "LEFT JOIN FETCH g.participants p "
        + "LEFT JOIN FETCH p.user "
        + "LEFT JOIN FETCH g.creator "
        + "WHERE g.status IN :statuses "
        + "AND g.deletedAt IS NULL "
        + "ORDER BY g.createdAt DESC")
List<Game> findByStatusInWithFetch(@Param("statuses") Collection<GameStatus> statuses);
```
Ne pas inclure `LEFT JOIN FETCH g.regionRules` (inutile pour la supervision, rÃ©duit la charge).

**`GameSupervisionDto`** â€” record dans `com.fortnite.pronos.dto.admin` :
```java
public record GameSupervisionDto(
    UUID gameId,
    String gameName,
    String status,
    String draftMode,
    int participantCount,
    int maxParticipants,
    String creatorUsername,
    LocalDateTime createdAt) {}
```
`createdAt` reste `LocalDateTime` (comme le champ JPA `Game.createdAt`).

**`AdminGameSupervisionService`** â€” dans `com.fortnite.pronos.service.admin`, 1 dÃ©pendance :
```java
@Service
public class AdminGameSupervisionService {
  static final List<GameStatus> ACTIVE_STATUSES = List.of(GameStatus.CREATING, GameStatus.DRAFTING, GameStatus.ACTIVE);

  private final GameRepository gameRepository;

  public AdminGameSupervisionService(GameRepository gameRepository) {
    this.gameRepository = gameRepository;
  }

  public List<GameSupervisionDto> getAllActiveGames() {
    return gameRepository.findByStatusInWithFetch(ACTIVE_STATUSES).stream()
        .map(this::toDto)
        .toList();
  }

  public List<GameSupervisionDto> getActiveGamesByStatus(GameStatus status) {
    return gameRepository.findByStatusInWithFetch(List.of(status)).stream()
        .map(this::toDto)
        .toList();
  }

  private GameSupervisionDto toDto(Game game) {
    return new GameSupervisionDto(
        game.getId(),
        game.getName(),
        game.getStatus().name(),
        game.getDraftMode().name(),
        game.getParticipants().size(),
        game.getMaxParticipants(),
        game.getCreator().getUsername(),
        game.getCreatedAt());
  }
}
```

**`AdminGameSupervisionController`** â€” dans `com.fortnite.pronos.controller`, path `/api/admin/supervision` :
- `@PreAuthorize("hasRole('ADMIN')")` sur la classe
- `@Validated` sur la classe si `@RequestParam` valide une enum
- Spring convertit automatiquement `?status=DRAFTING` en `GameStatus.DRAFTING` ; valeur invalide â†’ `MethodArgumentTypeMismatchException` â†’ 400 (dÃ©jÃ  gÃ©rÃ© par `GlobalExceptionHandler`)

**Security test** â€” mÃªme patron que `SecurityConfigAdminScrapeAuthorizationTest` :
```java
@WebMvcTest(controllers = AdminGameSupervisionController.class)
@Import({SecurityConfig.class, SecurityConfigAdminSupervisionAuthorizationTest.SecurityTestBeans.class})
@ActiveProfiles("security-it")
```
`@MockBean` requis : `AdminGameSupervisionService`, `ErrorJournalService`, `VisitTrackingService`, `UserDetailsService`.

**Pas de migration DB** â€” V41 est la derniÃ¨re migration ; `games` table existante suffit.

**CouplingTest** â€” `AdminGameSupervisionService` a 1 dÃ©pendance âœ“ (max 7).

**NamingConventionTest** â€” classes `@Service` dans `..service..` doivent se terminer par `Service` âœ“.

**DependencyInversionTest** â€” contrÃ´leurs ne doivent PAS dÃ©pendre directement de repositories. `AdminGameSupervisionController` dÃ©pend de `AdminGameSupervisionService`, pas de `GameRepository` directement âœ“.

### Frontend

**Chemin du service** : `frontend/src/app/features/admin/services/admin.service.ts`

**ModÃ¨les** : `frontend/src/app/features/admin/models/admin.models.ts` â€” ajouter `GameSupervisionStatus` et `GameSupervisionEntry` en fin de fichier.

**Pattern lazy loading** (copier depuis `admin.routes.ts`) :
```typescript
{
  path: 'games',
  canActivate: [AdminGuard],
  loadComponent: () =>
    import('./games-supervision/admin-games-supervision/admin-games-supervision.component')
      .then(c => c.AdminGamesSupervisionComponent)
}
```

**Filtrage local** â€” `allGames` contient tous les statuts (CREATING+DRAFTING+ACTIVE) chargÃ©s en une seule requÃªte. Les onglets mettent Ã  jour `selectedStatus` signal â†’ `filteredGames` computed recalcule. Pas de nouveau appel HTTP par onglet.

**Badge statut** â€” CSS classes :
```scss
.status-badge {
  &--creating { background: #9e9e9e; color: #fff; }
  &--drafting  { background: #ff9800; color: #000; }
  &--active    { background: #4caf50; color: #fff; }
}
```

**Colonnes mat-table** â€” `displayedColumns = ['name', 'status', 'participants', 'creator', 'createdAt']`
- `participants` affiche `{{ entry.participantCount }}/{{ entry.maxParticipants }}`
- `createdAt` formatÃ© avec `DatePipe` (`'dd/MM/yyyy HH:mm'`)

**Imports standalone requis** :
`MatTableModule, MatTabsGroup, MatProgressSpinnerModule, MatChipsModule, CommonModule, DatePipe` (ou `AsyncPipe`).

### Testing Standards

- Backend : Mockito uniquement (pas de `@SpringBootTest`) pour service et controller unit tests
- Backend security : `@WebMvcTest` avec `@WithMockUser` â€” mÃªme pattern que `SecurityConfigAdminScrapeAuthorizationTest`
- Frontend : `TestBed.configureTestingModule` avec `HttpClientTestingModule` pour les tests de service ; `provideHttpClientTesting()` si standalone
- Frontend composant : flusher les requÃªtes HTTP avec `HttpTestingController`, utiliser `fixture.detectChanges()` aprÃ¨s flush

## Project Structure Notes

**Nouveaux fichiers backend** :
```
src/main/java/com/fortnite/pronos/
  dto/admin/GameSupervisionDto.java
  service/admin/AdminGameSupervisionService.java
  controller/AdminGameSupervisionController.java

src/test/java/com/fortnite/pronos/
  service/admin/AdminGameSupervisionServiceTest.java
  controller/AdminGameSupervisionControllerTest.java
  config/SecurityConfigAdminSupervisionAuthorizationTest.java
```

**Fichiers backend modifiÃ©s** :
```
src/main/java/com/fortnite/pronos/repository/GameRepository.java
  â†’ +findByStatusInWithFetch(Collection<GameStatus>)
```

**Nouveaux fichiers frontend** :
```
frontend/src/app/features/admin/games-supervision/
  admin-games-supervision/
    admin-games-supervision.component.ts
    admin-games-supervision.component.html
    admin-games-supervision.component.scss
    admin-games-supervision.component.spec.ts
```

**Fichiers frontend modifiÃ©s** :
```
frontend/src/app/features/admin/
  models/admin.models.ts        â†’ +GameSupervisionStatus, +GameSupervisionEntry
  services/admin.service.ts     â†’ +getGamesSupervision()
  admin.routes.ts               â†’ +{ path: 'games', loadComponent: ... }
```

**Alignement architecture** :
- Controller â†’ Service â†’ Repository (pas de court-circuit)
- Service dans `service/admin/` (package respectÃ© par `NamingConventionTest`)
- DTO dans `dto/admin/` (cohÃ©rent avec `PipelineAlertDto`, `ScrapeLogDto`)
- Component dans `features/admin/games-supervision/` (cohÃ©rent avec `pipeline/`, `incidents/`)

### References

- [Source: `_bmad-output/planning-artifacts/epics.md` â€” Epic 7, Story 7.4] FR-47
- [Source: `src/main/java/com/fortnite/pronos/model/Game.java`] Champs disponibles : `id`, `name`, `status`, `draftMode`, `createdAt (LocalDateTime)`, `creator (User)`, `participants (List<GameParticipant>)`, `maxParticipants`
- [Source: `src/main/java/com/fortnite/pronos/repository/GameRepository.java`] Pattern `findByStatusWithFetch` + `findByStatusInWithFetch` Ã  ajouter, pattern JPQL FETCH JOIN confirmÃ©
- [Source: `src/test/java/com/fortnite/pronos/config/SecurityConfigAdminScrapeAuthorizationTest.java`] Pattern @WebMvcTest security test avec SecurityTestBeans inner class
- [Source: `frontend/src/app/features/admin/admin.routes.ts`] Pattern lazy loadComponent + canActivate AdminGuard
- [Source: `frontend/src/app/features/admin/services/admin.service.ts`] Pattern catchError + HttpClient injectable
- [Source: memory â€” CouplingTest] Max 7 dÃ©pendances injectÃ©es par @Service
- [Source: memory â€” NamingConventionTest] Classes @Service dans `..service..` doivent se terminer par `Service`
- [Source: memory â€” DependencyInversionTest] Controllers ne doivent pas dÃ©pendre directement de Repositories
- [Source: memory â€” spotless] Toujours exÃ©cuter `mvn spotless:apply` avant `mvn test` sur nouveaux fichiers

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

- TypeScript error on `getGamesSupervision` params: `{ status: GameSupervisionStatus | undefined }` not assignable to `Record<string, string | ...>`. Fixed by using explicit `const params: Record<string, string> = {}` with conditional key assignment.

### Completion Notes List

- Runtime follow-up 2026-03-07: local frontend profiles realigned with seeded backend accounts (`admin`, `thibaut`, `marcel`, `teddy`), `/admin/games` revalidated end-to-end in browser, and AdminGuard behavior confirmed for admin, non-admin, and anonymous users. See `7-4-supervision-globale-des-parties-en-cours-runtime-validation-2026-03-07.md`.

- âœ… Task 1: `findByStatusInWithFetch(Collection<GameStatus>)` added to `GameRepository` with JPQL FETCH JOIN (participants+user+creator), `deletedAt IS NULL`, `ORDER BY createdAt DESC`.
- âœ… Task 2: `GameSupervisionDto` record created in `dto/admin/` â€” 8 fields (gameId, gameName, status, draftMode, participantCount, maxParticipants, creatorUsername, createdAt).
- âœ… Task 3: `AdminGameSupervisionService` â€” 1 dep (GameRepository), `ACTIVE_STATUSES = [CREATING, DRAFTING, ACTIVE]`, two public methods, private `toDto()` mapper.
- âœ… Task 4: `AdminGameSupervisionController` at `/api/admin/supervision`, `@PreAuthorize("hasRole('ADMIN')")`, single endpoint `GET /games` with optional `GameStatus status` param.
- âœ… Task 5: 13 backend tests all green â€” service (6), controller unit (3), security @WebMvcTest (4). Backend total: 2247 (19F+1E pre-existing, 0 new regressions).
- âœ… Task 6: `GameSupervisionStatus` type + `GameSupervisionEntry` interface added to `admin.models.ts`.
- âœ… Task 7: `getGamesSupervision(status?)` added to `AdminService` with `catchError(() => of([]))`.
- âœ… Task 8: `AdminGamesSupervisionComponent` standalone â€” signals (allGames, selectedStatus, filteredGames computed, loading), 4-tab local filter, mat-table 5 columns, CSS badge classes, error banner.
- âœ… Task 9: Route `{ path: 'games', loadComponent: ... }` added to `admin.routes.ts`.
- âœ… Task 10: 13 frontend tests all green â€” component (10) + service (3 new). Frontend total: 2133/2133 (0 failures, +15 vs baseline of 2118).

### File List

**Backend â€” New files:**
- `src/main/java/com/fortnite/pronos/dto/admin/GameSupervisionDto.java`
- `src/main/java/com/fortnite/pronos/service/admin/AdminGameSupervisionService.java`
- `src/main/java/com/fortnite/pronos/controller/AdminGameSupervisionController.java`
- `src/test/java/com/fortnite/pronos/service/admin/AdminGameSupervisionServiceTest.java`
- `src/test/java/com/fortnite/pronos/controller/AdminGameSupervisionControllerTest.java`
- `src/test/java/com/fortnite/pronos/config/SecurityConfigAdminSupervisionAuthorizationTest.java`

**Backend â€” Modified files:**
- `src/main/java/com/fortnite/pronos/repository/GameRepository.java` (+`Collection` import, +`findByStatusInWithFetch`)

**Frontend â€” New files:**
- `frontend/src/app/features/admin/games-supervision/admin-games-supervision/admin-games-supervision.component.ts`
- `frontend/src/app/features/admin/games-supervision/admin-games-supervision/admin-games-supervision.component.html`
- `frontend/src/app/features/admin/games-supervision/admin-games-supervision/admin-games-supervision.component.scss`
- `frontend/src/app/features/admin/games-supervision/admin-games-supervision/admin-games-supervision.component.spec.ts`

**Frontend â€” Modified files:**
- `frontend/src/app/features/admin/models/admin.models.ts` (+`GameSupervisionStatus`, +`GameSupervisionEntry`)
- `frontend/src/app/features/admin/services/admin.service.ts` (+`of` import, +`catchError` import, +`GameSupervisionEntry`/`GameSupervisionStatus` imports, +`getGamesSupervision()`)
- `frontend/src/app/features/admin/services/admin.service.spec.ts` (+`GameSupervisionEntry` import, +3 `getGamesSupervision` tests)
- `frontend/src/app/features/admin/admin.routes.ts` (+`games` route)
- `frontend/src/app/core/services/user-context.service.ts` (local profiles aligned with seeded backend identities)
- `frontend/src/app/core/services/auth-switch.service.ts` (dev switch users aligned with seeded backend identities)
- `frontend/src/app/core/services/team.service.ts` (default local fallback username aligned with seeded backend identity)
- `frontend/src/app/core/services/user-context.service.spec.ts` (profile/session expectations aligned with seeded backend identities)
- `frontend/src/app/core/services/auth-switch.service.spec.ts` (dev switch expectations aligned with seeded backend identities)
- `frontend/src/app/core/services/team.service.spec.ts` (+fallback username test for seeded local identity)


## Review Follow-ups (AI â€” post-code-review fixes)

### Fixes appliquÃ©s (H1, H2, M1, M2, L1, L2, L3)

**H1 â€” FIXED** : Ajout test `?status=NOT_A_STATUS` â†’ 400 dans `SecurityConfigAdminSupervisionAuthorizationTest` (test `invalidStatusValueReturns400`).

**H2 â€” FIXED** : Validation dans `AdminGameSupervisionService.getActiveGamesByStatus()` â€” lÃ¨ve `IllegalArgumentException` si le statut n'est pas dans `ACTIVE_STATUSES` (CREATING/DRAFTING/ACTIVE). `IllegalArgumentException` â†’ 400 dÃ©jÃ  gÃ©rÃ© par `GlobalExceptionHandler`. +2 tests service (rejectsFinishedStatus, rejectsCancelledStatus).

**M1 â€” FIXED** : `GameSupervisionDto.createdAt` : `LocalDateTime` â†’ `OffsetDateTime`. `toDto()` : `game.getCreatedAt().atOffset(ZoneOffset.UTC)`. Tests service et controller mis Ã  jour.

**M2 â€” FIXED** : Inline array `['ALL','CREATING','DRAFTING','ACTIVE'][$event]` extrait vers `readonly statusFilters: StatusFilter[] = [...]` dans le composant. Template utilise `statusFilters[$event]`.

**L1 â€” FIXED** : `ACTIVE_STATUSES` est maintenant `private static final`.

**L2 â€” FIXED** : `loadGames()` est maintenant `protected`. Spec mise Ã  jour pour utiliser `component['loadGames']()`.

**Tests post-review** : Backend 2250 (+3 vs 2247), Frontend 2133/2133. 0 nouvelles rÃ©gressions.

**L3 - FIXED** : `AdminGamesSupervisionComponentSpec` verifie maintenant explicitement que le filtre `CREATING` retourne une entree de statut `CREATING` (`expect(component.filteredGames()[0].status).toBe('CREATING')`).

**Runtime validation 2026-03-07** : `/admin/games` revalide en local avec vraies identites seedes (`admin`, `thibaut`, `marcel`, `teddy`) ; admin -> 2 parties visibles, non-admin -> redirect `/games`, anonyme -> redirect `/login?returnUrl=%2Fadmin%2Fgames`.

### Action items restants

- Aucun.
