# Story Sprint3-A4: Corriger les 10 violations DRY critiques

Status: ready-for-dev

## Story

As a developer,
I want éliminer les duplications de code critiques identifiées dans l'audit DRY-019,
so that la maintenance est simplifiée et les risques de divergence éliminés.

## Acceptance Criteria

1. **Given** les helpers `safeInt`/`safeBool` dupliqués dans 5 mappers (DRY-001), **When** le refactor est fait, **Then** ils sont centralisés dans `MappingUtils` et tous les mappers l'importent.

2. **Given** les méthodes `createUserReference`/`createPlayerReference` dupliquées dans 3 mappers (DRY-002), **When** le refactor est fait, **Then** elles sont centralisées dans `EntityReferenceFactory` dans `adapter/out/persistence/support/`.

3. **Given** la méthode `buildAndRecordError` dupliquée dans `DomainExceptionHandler` et `GameExceptionHandler` (DRY-003), **When** le refactor est fait, **Then** elle est extraite dans `ExceptionResponseBuilder` réutilisé par les deux handlers.

4. **Given** la `SecurityTestBeans` inner class dupliquée dans 14 fichiers de test (DRY-006), **When** le refactor est fait, **Then** elle est extraite dans `SecurityTestBeansConfig` et les 14 fichiers l'importent via `@Import`.

5. **Given** tous les refactors, **When** `mvn verify` est exécuté, **Then** aucune régression n'est introduite (même baseline que pre-existing).

## Technical Context

### DRY-001 : MappingUtils (CRITIQUE P0)

**Créer** : `src/main/java/com/fortnite/pronos/adapter/out/persistence/support/MappingUtils.java`

```java
package com.fortnite.pronos.adapter.out.persistence.support;

public final class MappingUtils {
    private MappingUtils() {}

    public static int safeInt(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static boolean safeBool(Boolean value) {
        return value != null && value;
    }
}
```

**Modifier** (supprimer les copies locales et importer) :
- `adapter/out/persistence/game/GameEntityMapper.java` (lignes ~371-379)
- `adapter/out/persistence/player/PlayerEntityMapper.java` (lignes ~100-104)
- `adapter/out/persistence/team/TeamEntityMapper.java` (lignes ~156-160)
- `adapter/out/persistence/draft/DraftEntityMapper.java` (~ligne 62)
- `service/draft/DraftService.java` (~ligne 481) — si présent

---

### DRY-002 : EntityReferenceFactory (CRITIQUE P0)

**Créer** : `src/main/java/com/fortnite/pronos/adapter/out/persistence/support/EntityReferenceFactory.java`

```java
package com.fortnite.pronos.adapter.out.persistence.support;

import com.fortnite.pronos.model.Draft;
import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import java.util.UUID;

public final class EntityReferenceFactory {
    private EntityReferenceFactory() {}

    public static User userRef(UUID userId) {
        if (userId == null) return null;
        User user = new User();
        user.setId(userId);
        return user;
    }

    public static User userRef(UUID userId, String username) {
        if (userId == null) return null;
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        return user;
    }

    public static Player playerRef(UUID playerId) {
        if (playerId == null) return null;
        return Player.builder().id(playerId).build();
    }

    public static Game gameRef(UUID gameId) {
        if (gameId == null) return null;
        Game game = new Game();
        game.setId(gameId);
        return game;
    }

    public static Draft draftRef(UUID draftId) {
        if (draftId == null) return null;
        Draft draft = new Draft();
        draft.setId(draftId);
        return draft;
    }
}
```

**Modifier** :
- `adapter/out/persistence/game/GameEntityMapper.java` (lignes ~147-185)
- `adapter/out/persistence/game/GameRepositoryAdapter.java` (~ligne 211)
- `adapter/out/persistence/team/TeamEntityMapper.java` (lignes ~134-155)

---

### DRY-003 : ExceptionResponseBuilder (CRITIQUE P0)

**Créer** : `src/main/java/com/fortnite/pronos/config/ExceptionResponseBuilder.java`

```java
package com.fortnite.pronos.config;

import com.fortnite.pronos.service.admin.ErrorEntry;
import com.fortnite.pronos.service.admin.ErrorJournalService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ExceptionResponseBuilder {
    private ExceptionResponseBuilder() {}

    public static ResponseEntity<GlobalExceptionHandler.ErrorResponse> buildAndRecord(
            Exception ex,
            HttpServletRequest request,
            HttpStatus status,
            String error,
            String code,
            ErrorJournalService errorJournalService) {

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
}
```

**Modifier** :
- `config/DomainExceptionHandler.java` — supprimer méthode privée `buildAndRecordError`, déléguer à `ExceptionResponseBuilder.buildAndRecord(...)`
- `config/GameExceptionHandler.java` — idem

---

### DRY-006 : SecurityTestBeansConfig (MAJEURE P1)

**Créer** : `src/test/java/com/fortnite/pronos/config/SecurityTestBeansConfig.java`

```java
package com.fortnite.pronos.config;

import com.fortnite.pronos.security.JwtAuthenticationFilter;
import com.fortnite.pronos.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;

@TestConfiguration
public class SecurityTestBeansConfig {
    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService) {
        JwtService jwtService = org.mockito.Mockito.mock(JwtService.class);
        return new JwtAuthenticationFilter(jwtService, userDetailsService) {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, IOException {
                filterChain.doFilter(request, response);
            }
        };
    }
}
```

**Modifier les 14 fichiers de test** — supprimer la `SecurityTestBeans` inner class et ajouter `@Import(SecurityTestBeansConfig.class)` :
- `SecurityConfigSnakeDraftAuthorizationTest.java`
- `SecurityConfigSimultaneousDraftAuthorizationTest.java`
- `SecurityConfigSwapSoloAuthorizationTest.java`
- `SecurityConfigDraftAuditAuthorizationTest.java`
- `SecurityConfigDraftParticipantTradeAuthorizationTest.java`
- `SecurityConfigAdminDraftRosterAuthorizationTest.java`
- `SecurityConfigIncidentAuthorizationTest.java`
- `SecurityConfigAccountAuthorizationTest.java`
- `SecurityConfigAdminSupervisionAuthorizationTest.java`
- `SecurityConfigAdminScrapeAuthorizationTest.java`
- `SecurityConfigGameConfigurePeriodAuthorizationTest.java`
- `SecurityConfigGameLeaderboardAuthorizationTest.java`
- (autres si présents)

---

### Tests à créer

1. `src/test/java/com/fortnite/pronos/adapter/out/persistence/support/MappingUtilsTest.java` — 6 tests (safeInt null, non-null, default; safeBool null, true, false)
2. `src/test/java/com/fortnite/pronos/adapter/out/persistence/support/EntityReferenceFactoryTest.java` — 10 tests (null params, chaque ref type)
3. `src/test/java/com/fortnite/pronos/config/ExceptionResponseBuilderTest.java` — 3 tests (status, code, path)

### Ordre d'exécution recommandé

1. DRY-001 (MappingUtils) → le plus simple, peu de risque
2. DRY-006 (SecurityTestBeansConfig) → modifie les tests, pas le code prod
3. DRY-002 (EntityReferenceFactory) → vérifie les imports existants d'abord
4. DRY-003 (ExceptionResponseBuilder) → le plus délicat (package config/)

### Règles qualité

- `mvn spotless:apply` avant `mvn test` sur CHAQUE lot
- Max 500 lignes/classe, max 50 lignes/méthode
- Loi de Demeter : pas de chaîne d'appels dans les nouvelles classes
- Tests créés AVANT refactor (TDD)
- Chaque DRY traité = 1 commit

## Definition of Done

- [ ] DRY-001 : `MappingUtils` créé, 5 mappers migrés, 6 tests verts
- [ ] DRY-002 : `EntityReferenceFactory` créé, 3 mappers migrés, 10 tests verts
- [ ] DRY-003 : `ExceptionResponseBuilder` créé, 2 handlers migrés, 3 tests verts
- [ ] DRY-006 : `SecurityTestBeansConfig` créé, 14 (ou +) tests migrés
- [ ] `mvn verify` : aucune nouvelle régression
- [ ] sprint-status.yaml mis à jour : `sprint3-a4-dry-violations-top10: done`
