# TECH-017D — Audit Sécurité : CORS / CSRF / XSS / Validation des entrées

> Audité le 2026-03-04

---

## 1. CORS

**Statut : CONFORME**

| Paramètre | Valeur | Évaluation |
|---|---|---|
| Origines dev | `localhost:4200/4201`, `127.0.0.1:4200/4201` | OK — local uniquement |
| Origines prod | `https://fortnitepronos.com`, `https://*.fortnitepronos.com` | OK — HTTPS forcé |
| Méthodes | `GET, POST, PUT, DELETE, OPTIONS` | OK |
| Credentials | `allowCredentials: true` | Correct pour JWT+Cookie |
| Max-age | 3600s | OK |

Aucune action requise.

---

## 2. CSRF

**Statut : DÉSACTIVÉ INTENTIONNELLEMENT — CORRECT**

- `http.csrf(AbstractHttpConfigurer::disable)` dans `SecurityConfig`
- Justification : API stateless, `SessionCreationPolicy.STATELESS`, authentification Bearer JWT
- Conclusion : CSRF inutile sans session cookie. Décision d'architecture correcte.

---

## 3. Headers de sécurité

**Statut : EXCELLENT**

| Header | Valeur |
|---|---|
| HSTS | 31 536 000s, includeSubDomains, preload |
| X-Frame-Options | DENY |
| X-Content-Type-Options | nosniff |
| Referrer-Policy | STRICT_ORIGIN_WHEN_CROSS_ORIGIN |
| HPKP | Activé |

---

## 4. XSS

**Statut : FAIBLE RISQUE**

- API JSON uniquement, aucun rendu HTML côté serveur
- Logs paramétrés (pas de `log.info("user: " + input)`)
- Encodage output délégué au frontend Angular (séparation correcte)

---

## 5. Validation des entrées — Gaps corrigés

### Problèmes identifiés et corrigés (PR 2026-03-04)

| Contrôleur | Endpoint | Problème | Correction |
|---|---|---|---|
| `GameController` | `POST /api/games/join` | `@RequestBody JoinGameRequest` sans `@Valid` | `@Valid` ajouté |
| `ScoreController` | `POST /scores/player/{id}` | `@RequestBody ScoreUpdateRequest` sans `@Valid` | `@Valid` + `@NotNull timestamp` |
| `ScoreController` | `POST /scores/batch` | `@RequestBody BatchScoreUpdateRequest` sans `@Valid` | `@Valid` + `@NotEmpty playerScores` + `@NotNull timestamp` |
| `TeamController` | `POST /api/teams` | `@RequestBody CreateTeamRequest` sans `@Valid` | `@Valid` + `@NotBlank name` + `@Min(2020) season` |
| `TeamController` | `POST .../players/add` | `@RequestBody AddPlayerRequest` sans `@Valid` | `@Valid` + `@NotNull playerId` + `@Min(0) position` |

### Contraintes ajoutées aux DTOs

```java
// JoinGameRequest
@NotNull(message = "L'ID de la game est requis") UUID gameId;
@NotNull(message = "L'ID de l'utilisateur est requis") UUID userId;

// ScoreController inline records
record ScoreUpdateRequest(int points, @NotNull OffsetDateTime timestamp) {}
record BatchScoreUpdateRequest(@NotEmpty Map<UUID,Integer> playerScores, @NotNull OffsetDateTime timestamp) {}

// TeamController inline records
record CreateTeamRequest(UUID userId, @NotBlank String name, @Min(2020) int season) {}
record AddPlayerRequest(@NotNull UUID playerId, @Min(0) int position) {}
record RemovePlayerRequest(@NotNull UUID playerId) {}
```

### Points acceptables (non modifiés)

| Contrôleur | Raison |
|---|---|
| `DraftController` `Map<String,String>` | Endpoint legacy avec flexibilité intentionnelle |
| `TeamController.removePlayer` | Retourne toujours 403 (hors trade flow) |
| `TeamController.makeChanges` | Retourne toujours 403 (hors trade flow) |
| `ScoreController.createScore(@RequestBody Score)` | Endpoint ADMIN-only sur entity JPA, sans contraintes Jakarta ajoutables sans modifier l'entité |
| `PrIngestionController` CSV | Profil dev uniquement (`@Profile({"dev","h2","test"})`) |

---

## 6. Authentification / Autorisation

**Statut : CONFORME**

- JWT Bearer, BCryptPasswordEncoder, STATELESS
- Routes `/api/admin/**` et `/api/users/**` : `ROLE_ADMIN` requis
- Routes `/api/**` : `authenticated()` requis
- Tests d'autorisation : `SecurityConfigAdminDashboardAuthorizationTest`, `SecurityConfigAdminPipelineAuthorizationTest`, `SecurityConfigAdminDatabaseAuthorizationTest`, + 10 autres suites

---

## 7. Résumé

| Catégorie | Avant | Après | Action |
|---|---|---|---|
| CORS | ✅ | ✅ | RAS |
| CSRF | ✅ | ✅ | RAS (stateless correct) |
| Headers | ✅ | ✅ | RAS |
| XSS | ✅ | ✅ | RAS (API JSON) |
| Validation input | ⚠️ 6 gaps | ✅ | 5 gaps corrigés |
| Auth/AuthZ | ✅ | ✅ | RAS |
