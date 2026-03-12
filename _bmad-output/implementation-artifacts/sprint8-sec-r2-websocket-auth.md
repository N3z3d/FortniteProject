# Story sprint8-SEC-R2: Authentification JWT sur les WebSockets STOMP

Status: ready-for-dev

<!-- METADATA
  story_key: sprint8-sec-r2-websocket-auth
  branch: story/sprint8-sec-r2-websocket-auth
  sprint: Sprint 8
-->

## Story

As a security-conscious developer,
I want WebSocket connections (STOMP via SockJS) to require a valid JWT token,
so that unauthenticated users cannot connect to the draft/trade real-time channels before the app is publicly exposed.

## Acceptance Criteria

1. Une connexion WebSocket sans JWT valide est rejetée avec un code d'erreur (401 ou fermeture de connexion).
2. Une connexion WebSocket avec un JWT valide dans le query param `?token=<jwt>` est acceptée et le STOMP session est établi.
3. Le token JWT est extrait et validé via `JwtUtil` (classe existante) au moment du handshake ou du CONNECT frame STOMP.
4. Les canaux `/topic/draft/**` et `/topic/trade/**` restent inaccessibles sans JWT valide.
5. Les tests existants de draft et trade (Vitest + JUnit) continuent de passer — 0 régression.
6. Un test d'autorisation `SecurityConfigWebSocketAuthorizationTest` est créé couvrant : anonyme → rejeté, JWT valide → accepté.
7. Le frontend (`WebSocketService`) transmet le token JWT au handshake SockJS via query param.

## Tasks / Subtasks

- [ ] Task 1: Backend — HandshakeInterceptor JWT (AC: #1, #2, #3)
  - [ ] 1.1: Créer `JwtHandshakeInterceptor` dans `config/` implémentant `HandshakeInterceptor`
  - [ ] 1.2: Dans `beforeHandshake()` : extraire `?token=` des query params, valider via `JwtUtil.validateToken()`, rejeter si invalide (return false)
  - [ ] 1.3: Enregistrer dans `WebSocketConfig.registerStompEndpoints()` : `.addInterceptors(jwtHandshakeInterceptor)`
- [ ] Task 2: Backend — ChannelInterceptor STOMP CONNECT (AC: #3, #4) [optionnel si HandshakeInterceptor suffit]
  - [ ] 2.1: Si le HandshakeInterceptor ne couvre pas les sous-abonnements `/topic/**`, créer `JwtChannelInterceptor` implémentant `ChannelInterceptor`
  - [ ] 2.2: Dans `preSend()` : intercepter `StompCommand.CONNECT`, extraire `Authorization` header du STOMP frame, valider JWT
  - [ ] 2.3: Enregistrer dans `WebSocketConfig.configureClientInboundChannel()`
- [ ] Task 3: Frontend — Transmettre le token au handshake (AC: #7)
  - [ ] 3.1: Dans `WebSocketService.connect()`, récupérer le token via `AuthService.getToken()`
  - [ ] 3.2: Passer le token en query param SockJS : `new SockJS(\`${WS_URL}?token=${token}\`)`
  - [ ] 3.3: Si token absent, ne pas tenter la connexion (log warning, retourner sans throw)
- [ ] Task 4: Tests backend (AC: #6)
  - [ ] 4.1: Créer `SecurityConfigWebSocketAuthorizationTest` dans `src/test/.../config/`
  - [ ] 4.2: Test anonyme → connexion rejetée (handshake retourne false ou STOMP ERROR frame)
  - [ ] 4.3: Test JWT valide → connexion acceptée
- [ ] Task 5: Tests frontend (AC: #5)
  - [ ] 5.1: Mettre à jour `WebSocketService` spec pour couvrir le nouveau paramètre token
  - [ ] 5.2: Vérifier que les tests draft/trade existants ne cassent pas (mock `AuthService.getToken()`)
- [ ] Task 6: Security — Authorization test (AC: #6) — MANDATORY
  - [ ] 6.1: `SecurityConfigWebSocketAuthorizationTest` avec `@SpringBootTest` (pas `@WebMvcTest` — WS nécessite contexte complet ou mock WS)

## Dev Notes

### Architecture WebSocket actuelle
- Backend : `WebSocketConfig.java` (dans `config/`) — déjà en place
  - Endpoint STOMP : `/ws` via SockJS
  - Topics : `/topic/draft/{draftId}`, `/topic/trade/{gameId}`, `/topic/draft/{draftId}/simultaneous`
  - App destinations : `/app/**`
- Frontend : `WebSocketService` dans `core/services/`
  - `connect()` crée un `SockJS` + `Client` STOMP
  - `subscribeToDraft()`, `subscribeToSimultaneous()` pour s'abonner aux topics

### Contrainte SockJS — PAS de header Authorization
SockJS ne supporte pas les headers HTTP personnalisés au handshake. L'approche **query param** est la seule viable :
```typescript
// Frontend — WebSocketService.connect()
const token = this.authService.getToken();
const sockjs = new SockJS(`${environment.wsUrl}?token=${encodeURIComponent(token ?? '')}`);
```

### Pattern HandshakeInterceptor backend
```java
// src/main/java/com/fortnite/pronos/config/JwtHandshakeInterceptor.java
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtUtil jwtUtil;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                attributes.put("username", username);
                return true;
            }
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {}
}
```

### WebSocketConfig — enregistrement de l'interceptor
```java
// Dans WebSocketConfig.registerStompEndpoints() :
registry.addEndpoint("/ws")
    .addInterceptors(jwtHandshakeInterceptor)
    .setAllowedOriginPatterns("*")
    .withSockJS();
```

### JwtUtil existant
- Classe : `com.fortnite.pronos.config.JwtUtil` (déjà en place depuis Sprint 6)
- Méthodes utilisables : `validateToken(String token)`, `extractUsername(String token)`
- NE PAS modifier JwtUtil — utiliser tel quel

### Test WebSocket backend
Les tests WebSocket avec `@WebMvcTest` ne chargent pas le contexte WS complet. Options :
- **Option A** (recommandée) : `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` pour HTTP handshake
- **Option B** : Mock `HandshakeInterceptor` dans un `@WebMvcTest` dédié en testant le bean directement (unit test du `beforeHandshake`)

Pattern recommandé pour le test unitaire (Option B — plus rapide, 0 dépendance externe) :
```java
@ExtendWith(MockitoExtension.class)
class JwtHandshakeInterceptorTest {
    @Mock JwtUtil jwtUtil;
    @InjectMocks JwtHandshakeInterceptor interceptor;

    @Test void rejects_when_no_token() {
        // MockHttpServletRequest sans paramètre "token"
        // → beforeHandshake() retourne false
    }

    @Test void rejects_when_invalid_token() {
        when(jwtUtil.validateToken("bad")).thenReturn(false);
        // → beforeHandshake() retourne false
    }

    @Test void accepts_valid_token() {
        when(jwtUtil.validateToken("good")).thenReturn(true);
        when(jwtUtil.extractUsername("good")).thenReturn("thibaut");
        // → beforeHandshake() retourne true, attributes["username"] = "thibaut"
    }
}
```

### Contraintes critiques backend
- **Max 7 dépendances par @Service** : `JwtHandshakeInterceptor` n'est pas un `@Service` — c'est un `@Component` — pas de limite stricte
- **Domaine pur** : pas de modification dans `domain/`
- **Spotless** : lancer `mvn spotless:apply` avant `mvn test` sur tout nouveau fichier Java
- **ArchUnit** : `JwtHandshakeInterceptor` va dans `config/` (controllers layer) — conforme

### Contraintes critiques frontend
- **inject()** pour DI : `private readonly authService = inject(AuthService)` dans `WebSocketService`
- **Pas de `fakeAsync+tick()`** dans les tests Vitest — utiliser Pattern A ou B (project-context.md §Conversion fakeAsync→Vitest)
- **Token absent** : si `getToken()` retourne `null`, passer `""` ou ne pas connecter (décision : log warning + skip)

### Impact sur les tests existants
- `WebSocketService.spec.ts` : les spy/mocks existants ne passent pas de token → ajouter `mockAuthService.getToken.and.returnValue('mock-jwt')`
- Tests draft/trade Vitest : pas d'impact direct (ils mockent le `WebSocketService` entier)
- Tests backend draft/trade JUnit : pas d'impact (ils ne testent pas la couche WS)

### Pre-existing Gaps / Known Issues

- [KNOWN] Frontend: 0 failures pre-existing (baseline 2243/2243 — project-context.md §Pre-existing Failures Baseline)
- [KNOWN] Backend: ~15 failures pre-existing exclues du CI (GameDataIntegrationTest 4, FortniteTrackerServiceTddTest 6, PlayerServiceTddTest 1, ScoreServiceTddTest 3, GameStatisticsServiceTddTest 1 error)
- [KNOWN] En mode dev (`DevSecurityConfig`), le rate limiter peut générer des 429 dans les tests d'intégration parallèles — acceptable

### Project Structure Notes

```
src/main/java/com/fortnite/pronos/
├── config/
│   ├── JwtHandshakeInterceptor.java    ← NOUVEAU
│   ├── WebSocketConfig.java             ← modifié (addInterceptors)
│   └── JwtUtil.java                     ← inchangé (utilisé tel quel)

src/test/java/com/fortnite/pronos/
├── config/
│   └── JwtHandshakeInterceptorTest.java ← NOUVEAU (unit test)

frontend/src/app/core/services/
└── web-socket.service.ts               ← modifié (query param token)
frontend/src/app/core/services/
└── web-socket.service.spec.ts          ← modifié (mock authService.getToken)
```

### References

- [Source: project-context.md §3 Règles Critiques Backend — Services Couplage, Architecture Tests]
- [Source: project-context.md §4 Règles Critiques Frontend — inject(), fakeAsync→Vitest]
- [Source: sprint-7-retro-2026-03-12.md §Action Items S1]
- [Source: sprint-status.yaml#sprint3-sec-r2-websocket-auth (backlog depuis Sprint 3)]

## Dev Agent Record

### Agent Model Used

claude-sonnet-4-6

### Debug Log References

### Completion Notes List

### File List

- `src/main/java/com/fortnite/pronos/config/JwtHandshakeInterceptor.java` — créé
- `src/main/java/com/fortnite/pronos/config/WebSocketConfig.java` — modifié (addInterceptors)
- `src/test/java/com/fortnite/pronos/config/JwtHandshakeInterceptorTest.java` — créé
- `frontend/src/app/core/services/web-socket.service.ts` — modifié (query param token)
- `frontend/src/app/core/services/web-socket.service.spec.ts` — modifié (mock getToken)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — modifié
