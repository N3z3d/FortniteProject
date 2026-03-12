# Story 3.2: Invitations par lien/code et join flow

Status: done

## Story

As a game creator,
I want inviter des participants via lien ou code,
so that je puisse remplir la partie sans friction.

## Acceptance Criteria

1. **Given** une partie existe avec un code d'invitation valide (non expiré), **When** un utilisateur authentifié appelle `POST /api/games/join-with-code` avec ce code, **Then** il rejoint la partie cible (statut 200 + GameDto) et est ajouté comme participant.

2. **Given** une partie existe avec un code d'invitation **expiré**, **When** un utilisateur appelle `POST /api/games/join-with-code` avec ce code, **Then** le système répond 400 avec un message explicite "Invitation code is expired or invalid" — aucun participant n'est ajouté.

3. **Given** un code d'invitation inexistant ou appartenant à une partie soft-deletée, **When** un utilisateur appelle `POST /api/games/join-with-code`, **Then** le système répond 404.

4. **Given** un utilisateur fait plus de 15 tentatives en 1 minute sur `POST /api/games/join-with-code`, **When** la 16ème tentative arrive, **Then** le système répond avec une erreur de rate limiting (le guard `InvitationCodeAttemptGuard` est déjà en place).

5. **Given** un créateur de partie veut regénérer le code, **When** il appelle `POST /api/games/{id}/regenerate-code?duration=48h`, **Then** un nouveau code est persisté avec expiration à 48h.

6. **Given** une précondition invalide (partie pleine, mauvais statut, utilisateur déjà dans la partie), **When** la même action est soumise avec un code valide, **Then** le système rejette la requête avec un message d'erreur explicite — aucun état invalide n'est persisté.

## Tasks / Subtasks

- [x] Task 1: Créer `InvalidInvitationCodeException` (AC: #2)
  - [x] `src/main/java/com/fortnite/pronos/exception/InvalidInvitationCodeException.java`
  - [x] Extends `RuntimeException`, message explicite "Invitation code is expired or invalid"

- [x] Task 2: Ajouter la validation d'expiration dans `GameParticipantService` (AC: #1, #2) — **GAP CRITIQUE**
  - [x] Dans `findGameFromRequest()` : après `gameRepository.findByInvitationCode(code)`, appeler `game.isInvitationCodeValid()`
  - [x] Si `!game.isInvitationCodeValid()` → throw `InvalidInvitationCodeException`
  - [x] Ne pas modifier la logique `findGameOrThrow(request.getGameId())` (chemin sans code)

- [x] Task 3: Ajouter handler dans `DomainExceptionHandler` (AC: #2)
  - [x] `src/main/java/com/fortnite/pronos/config/DomainExceptionHandler.java` (chemin réel, pas exception/handler/)
  - [x] `@ExceptionHandler(InvalidInvitationCodeException.class)` → HTTP 400 BAD_REQUEST
  - [x] DomainExceptionHandler reste à 138 lignes (≤ 500)

- [x] Task 4: Écrire les tests TDD (AC: #1, #2, #3, #6)
  - [x] `src/test/java/com/fortnite/pronos/service/game/GameParticipantServiceTddTest.java` (nouveau fichier)
  - [x] Nominal: join avec code valide → retourne true
  - [x] Nominal: join avec code valide et expiry future → retourne true
  - [x] Edge: join avec code expiré → `InvalidInvitationCodeException`
  - [x] Edge: join sans code → fallback gameId
  - [x] Edge: join avec code inexistant → `GameNotFoundException`
  - [x] Edge: join avec code valide mais partie pleine → `GameFullException`
  - [x] Edge: join avec code valide mais utilisateur déjà dans la partie → `UserAlreadyInGameException`
  - [x] Edge: join avec code valide mais partie en mauvais statut → `InvalidGameStateException`

- [x] Task 5: Vérifier la couverture de `GameCreationService.regenerateInvitationCode()` (AC: #5)
  - [x] Absent dans `GameCreationServiceTest.java` → nested class `RegenerateInvitationCode` ajoutée
  - [x] 3 tests ajoutés : sans duration (permanent), avec duration "48h", gameId inconnu → GameNotFoundException

## Dev Notes

### GAP CRITIQUE IDENTIFIÉ

`GameParticipantService.findGameFromRequest()` (lignes 102-111) trouve un jeu via son code mais **ne vérifie jamais l'expiration** :

```java
// ÉTAT ACTUEL — BUG : code expiré accepté
private Game findGameFromRequest(JoinGameRequest request) {
    if (request.getInvitationCode() != null && !request.getInvitationCode().isBlank()) {
        return gameRepository
            .findByInvitationCode(request.getInvitationCode())
            .orElseThrow(() -> new GameNotFoundException(...));
        // ← ICI : pas de game.isInvitationCodeValid()
    }
    return findGameOrThrow(request.getGameId());
}

// CIBLE — fix minimal
private Game findGameFromRequest(JoinGameRequest request) {
    if (request.getInvitationCode() != null && !request.getInvitationCode().isBlank()) {
        Game game = gameRepository
            .findByInvitationCode(request.getInvitationCode())
            .orElseThrow(() -> new GameNotFoundException(...));
        if (!game.isInvitationCodeValid()) {
            throw new InvalidInvitationCodeException("Invitation code is expired or invalid");
        }
        return game;
    }
    return findGameOrThrow(request.getGameId());
}
```

### Infrastructure déjà en place (NE PAS RECRÉER)

| Composant | Fichier | État |
|-----------|---------|------|
| Domain: champs code/expiry | `domain/game/model/Game.java` | ✅ L.XX: `invitationCode`, `invitationCodeExpiresAt`, `isInvitationCodeValid()`, `isInvitationCodeExpired()` |
| Port query | `domain/port/out/GameDomainRepositoryPort.java` | ✅ `findByInvitationCode(String)` |
| Adapter query | `adapter/out/persistence/game/GameRepositoryAdapter.java` L.60-61 | ✅ délègue vers JPA repo avec soft-delete filter |
| JPA query | `repository/GameRepository.java` L.58-59 | ✅ `@Query "... AND g.deletedAt IS NULL"` |
| Génération code | `service/InvitationCodeService.java` | ✅ `generateUniqueCode()`, enum `CodeDuration` (24h/48h/7d/permanent) |
| Rate limiting | `service/InvitationCodeAttemptGuard.java` | ✅ 15 tentatives/min par user+IP |
| Régénération | `service/game/GameCreationService.java` L.86-111 | ✅ `regenerateInvitationCode(gameId, duration)` |
| Endpoint join-with-code | `controller/GameController.java` L.174-203 | ✅ intègre guard + normalization (trim+uppercase) |
| Endpoint regenerate-code | `controller/GameController.java` L.346-380 | ✅ |
| Mapping DTO | `dto/mapper/GameDtoMapper.java` L.30-32 | ✅ `invitationCode`, `invitationCodeExpiresAt`, `isInvitationCodeExpired` |
| Mapping entity | `adapter/out/persistence/game/GameEntityMapper.java` L.53-54, 213-214 | ✅ bidirectionnel |

### Règles d'architecture obligatoires

- **DomainIsolationTest**: `InvalidInvitationCodeException` est dans `exception/` (pas dans `domain/`) → pas de contrainte domaine
- **CouplingTest**: `GameParticipantService` a actuellement 4 deps : `gameRepository`, `legacyGameRepository`, `gameParticipantRepository`, `userRepository`. L'ajout de `InvalidInvitationCodeException` n'ajoute pas de dépendance injectée (c'est juste un `new`), rester sous 7 deps.
- **MaximumClassLinesCondition**: `GameParticipantService.java` ≤ 500 lignes. Vérifier après modification.
- **DomainExceptionHandler**: Vérifier taille avant ajout handler (≤ 500 lignes).

### Pattern de test TDD (reproduire exactement)

```java
@ExtendWith(MockitoExtension.class)
class GameParticipantServiceTddTest {

    @Mock private GameDomainRepositoryPort gameRepository;
    @Mock private GameRepositoryPort legacyGameRepository;
    @Mock private GameParticipantRepositoryPort gameParticipantRepository;
    @Mock private UserRepositoryPort userRepository;

    @InjectMocks private GameParticipantService service;

    private UUID userId;
    private UUID gameId;
    private com.fortnite.pronos.model.User user;
    private com.fortnite.pronos.domain.game.model.Game game;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        gameId = UUID.randomUUID();
        user = new com.fortnite.pronos.model.User();
        user.setId(userId);
        user.setUsername("player1");
        // game = new Game("Test", creatorId, 4, DraftMode.SNAKE, 5, 10, true);
        // puis game.addParticipant(new GameParticipant(userId, "creator", true));
    }
}
```

**Attention**: `when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0))` pour stubber le save.

**Attention linter**: Toujours relire les fichiers après écriture — le linter peut corrompre les lambdas en test (`i.getArgument(0)` au lieu de `inv -> inv.getArgument(0)`).

### Gestion du `invitationCode` dans `Game.restore()` — paramètre n°10

Le `Game.restore()` accepte 24 paramètres. `invitationCode` est passé en position 10 (index 9), `invitationCodeExpiresAt` en position 11 (index 10). Pour les tests de domaine, utiliser `game.setInvitationCode("TESTCODE")` et `game.setInvitationCodeExpiresAt(...)` après construction du domaine.

### Cas `invitationCodeExpiresAt = null` (permanent)

`game.isInvitationCodeValid()` retourne `true` si `invitationCode != null` ET (`invitationCodeExpiresAt == null` OU `invitationCodeExpiresAt.isAfter(now)`). Donc un code sans expiration est toujours valide. Tester ce cas.

### Project Structure Notes

- Nouveau fichier exception: `src/main/java/com/fortnite/pronos/exception/InvalidInvitationCodeException.java`
- Modification: `src/main/java/com/fortnite/pronos/service/game/GameParticipantService.java` (méthode `findGameFromRequest` — ~10 lignes)
- Modification: `src/main/java/com/fortnite/pronos/exception/handler/DomainExceptionHandler.java` (+1 handler)
- Nouveau test: `src/test/java/com/fortnite/pronos/service/game/GameParticipantServiceTddTest.java`

### References

- `GameParticipantService.findGameFromRequest()`: `src/main/java/com/fortnite/pronos/service/game/GameParticipantService.java` L.102-112
- `Game.isInvitationCodeValid()`: `src/main/java/com/fortnite/pronos/domain/game/model/Game.java`
- `DomainExceptionHandler`: `src/main/java/com/fortnite/pronos/exception/handler/DomainExceptionHandler.java`
- `InvitationCodeAttemptGuard`: `src/main/java/com/fortnite/pronos/service/InvitationCodeAttemptGuard.java`
- Architecture hexagonale: `_bmad-output/planning-artifacts/architecture.md`

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

- Fixed critical security gap: `GameParticipantService.findGameFromRequest()` now calls `game.isInvitationCodeValid()` after finding a game by invitation code — expired codes now throw `InvalidInvitationCodeException` (400) instead of being accepted.
- Created `InvalidInvitationCodeException` in `exception/` package (extends RuntimeException).
- Added `@ExceptionHandler(InvalidInvitationCodeException.class)` in `config/DomainExceptionHandler.java` → HTTP 400 BAD_REQUEST, error code `INVALID_INVITATION_CODE`.
- Fixed existing `GameParticipantServiceTest.findsGameByInvitationCode` to call `game.setInvitationCode("TESTCODE")` so the new validation passes.
- 8 new tests in `GameParticipantServiceTddTest` covering all invitation code join scenarios (valid permanent, valid with expiry, expired, not found, full game, already participant, bad status, fallback to gameId).
- 3 new tests in `GameCreationServiceTest.RegenerateInvitationCode` covering regenerate with null duration, with "48h" duration, and unknown gameId.
- Full test suite: 2052 run, 26 pre-existing failures, 1 pre-existing error. Zero regressions.

### File List

- `src/main/java/com/fortnite/pronos/exception/InvalidInvitationCodeException.java` (NEW)
- `src/main/java/com/fortnite/pronos/service/game/GameParticipantService.java` (MODIFIED — findGameFromRequest +4 lines)
- `src/main/java/com/fortnite/pronos/config/DomainExceptionHandler.java` (MODIFIED — +1 handler)
- `src/test/java/com/fortnite/pronos/service/game/GameParticipantServiceTddTest.java` (NEW — 8 tests)
- `src/test/java/com/fortnite/pronos/service/game/GameParticipantServiceTest.java` (MODIFIED — setInvitationCode fix)
- `src/test/java/com/fortnite/pronos/service/game/GameCreationServiceTest.java` (MODIFIED — +RegenerateInvitationCode nested class, 3 tests)

## Review Follow-ups (AI — post-code-review fixes)

### Fix appliqué (H1)

**H1 — FIXED**: `GameParticipantService.validateUserCanLeaveGame()` et `removeUserFromGame()` utilisaient `IllegalStateException` (capturée par le handler 4.3 → 409 `DRAFT_WINDOW_VIOLATION`). Remplacé par `InvalidGameStateException` (→ 409 `INVALID_GAME_STATE`, sémantiquement correct).

### Action items

- [ ] **[AI-Review][Medium][M1]** : AC#4 (rate limiting `InvitationCodeAttemptGuard`) non testé dans ce sprint. Ajouter un test d'intégration vérifiant que la 16ème tentative sur `POST /api/games/join-with-code` retourne 429.
- [ ] **[AI-Review][Low][L1]** : `InvalidInvitationCodeException` manque de `serialVersionUID` — ajouter `private static final long serialVersionUID = 1L`.
- [ ] **[AI-Review][Low][L2]** : `GameParticipantServiceTddTest` manque de `@DisplayName` sur les méthodes de test — inconsistant avec les autres fichiers tests du projet.
- [ ] **[AI-Review][Low][L3]** : Story Dev Notes indiquent le mauvais chemin `exception/handler/DomainExceptionHandler.java` — chemin réel : `config/DomainExceptionHandler.java`.
