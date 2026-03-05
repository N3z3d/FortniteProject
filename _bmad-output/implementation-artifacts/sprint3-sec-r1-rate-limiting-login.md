# Story Sprint3-SEC-R1: Rate Limiting sur /api/auth (Bucket4j)

Status: ready-for-dev

## Story

As a security engineer,
I want protéger les endpoints d'authentification contre les attaques brute-force,
so that les tentatives de connexion massives soient bloquées avant exposition publique.

## Acceptance Criteria

1. **Given** un client effectue plus de 5 tentatives POST sur `/api/auth/login` en moins de 60 secondes depuis la même IP, **When** la 6ème requête arrive, **Then** la réponse est HTTP 429 Too Many Requests avec un message clair et un header `Retry-After`.

2. **Given** un client respecte la limite de débit, **When** il s'authentifie normalement, **Then** la requête passe sans modification (transparent pour l'utilisateur légitime).

3. **Given** un client a été limité (429), **When** 60 secondes s'écoulent, **Then** il peut à nouveau s'authentifier normalement (la fenêtre se réinitialise).

4. **Given** l'endpoint `/api/auth/register` (si existant) ou `/api/auth/**`, **When** du rate limiting est configuré, **Then** SEULS les endpoints d'auth sont limités (les autres APIs ne sont pas affectées).

5. **Given** l'application est configurée, **When** les tests unitaires sont exécutés, **Then** le comportement 429 vs 200 est couvert par au minimum 5 tests.

## Technical Context

### Dépendance à ajouter (pom.xml)

```xml
<dependency>
    <groupId>com.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

### Architecture cible

- **`RateLimitingFilter`** dans `src/main/java/com/fortnite/pronos/config/` (implements `OncePerRequestFilter`)
  - Intercepte uniquement les requêtes vers `/api/auth/**`
  - Utilise une `ConcurrentHashMap<String, Bucket>` en mémoire (pas de Redis — local only)
  - Clé = IP (`request.getRemoteAddr()`)
  - Bucket = 5 tokens, refill 5 tokens toutes les 60 secondes
  - Si bucket vide → 429 avec JSON `{"error": "Too Many Requests", "retryAfterSeconds": X}`

- **`SecurityConfig`** : enregistrer le filtre AVANT `UsernamePasswordAuthenticationFilter`
  - `http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)`

### Contraintes

- Pas de Redis, pas de Hazelcast — mémoire locale suffisante pour usage local
- ConcurrentHashMap : `computeIfAbsent(ip, k -> createNewBucket())`
- Header `Retry-After` obligatoire (RFC 6585)
- Pas de modification des DTOs d'auth existants
- Pas de changement du flux d'authentification JWT existant

### Fichiers à créer

1. `src/main/java/com/fortnite/pronos/config/RateLimitingFilter.java`
2. `src/test/java/com/fortnite/pronos/config/RateLimitingFilterTest.java`

### Fichiers à modifier

1. `pom.xml` — ajouter dépendance Bucket4j
2. `src/main/java/com/fortnite/pronos/config/SecurityConfig.java` — enregistrer le filtre

### Tests attendus (JUnit 5 + Mockito)

- `shouldAllowRequestsUnderLimit()` — 5 requêtes successives → toutes 200/passthrough
- `shouldReturn429OnExcessRequests()` — 6ème requête → 429
- `shouldResetAfterWindow()` — après expiration bucket → de nouveau autorisé
- `shouldNotLimitNonAuthEndpoints()` — `/api/games` non limité
- `shouldIncludeRetryAfterHeader()` — header présent lors du 429

### Règles qualité

- Respecter la loi de Demeter : pas de chaîne d'appels dans le filtre
- Max 80 lignes pour `RateLimitingFilter`
- TDD : tester avant d'implémenter
- `mvn spotless:apply` avant `mvn test`

## Definition of Done

- [ ] Dépendance Bucket4j dans pom.xml
- [ ] `RateLimitingFilter` créé et enregistré dans SecurityConfig
- [ ] ≥5 tests couvrant nominal + edge cases
- [ ] `mvn test` passe (hors pre-existing failures connues)
- [ ] Pas de régression sur les tests existants
- [ ] sprint-status.yaml mis à jour : `sprint3-sec-r1-rate-limiting-login: done`
