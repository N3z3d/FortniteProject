# Audit Master Plan

## Contexte & Objectifs
- Objectif principal: piloter un programme d'audit securite et qualite par lots courts, tracables et actionnables.
- Perimetre backend: auth/access control, validation d'entree, secrets/configuration, exposition sensible.
- Perimetre frontend: securite UX, affichage erreurs, sanitisation, non-regression.
- Contraintes:
- pas de dependance externe ajoutee sans justification explicite
- aucun changement de contrat API public pendant la phase d'audit
- execution incrementalement ticket par ticket

## Methodologie
- Referentiel principal: OWASP Top 10.
- Mapping des tickets:
- `JIRA-AUDIT-016`: A01 + A05 (auth/access/CORS)
- `JIRA-AUDIT-017`: A03 (injection + validation)
- `JIRA-AUDIT-018`: A05 + A09 (security misconfiguration + logging/monitoring)
- `JIRA-AUDIT-019`: frontend security UX (XSS/messages sensibles)
- `JIRA-AUDIT-020`: non-regression tests securite
- `JIRA-AUDIT-021`: remediation sweep + cloture
- Severite findings:
- `Critical`: exploitable immediatement, impact majeur
- `High`: risque eleve, correction prioritaire
- `Medium`: risque modere, correction planifiee
- `Low`: hygiene, correction opportuniste
- Definition d'un finding:
- probleme reproductible + preuve (commande/output/fichier) + impact + action de remediation

## Matrice Tickets
| Ticket | Scope | Owner | Dependencies | Status | Evidence |
|---|---|---|---|---|---|
| JIRA-AUDIT-010 | Epic parent, pilotage global OWASP | @Codex | none | DONE | Cloture finale OWASP avec preuves consolidees (2026-02-14 12:38) |
| JIRA-AUDIT-016 | Auth, access control, CORS | @Codex | none | DONE | Matrice endpoint/role + tests `SecurityConfigCorsTest`, `SecurityConfigurationTest`, `UserResolverTest`, `GameControllerSimpleTest` |
| JIRA-AUDIT-017 | Validation entree + injection | @Codex | JIRA-AUDIT-016 | DONE | Scan controllers/DTO/repositories + findings derives `028/029/030` |
| JIRA-AUDIT-018 | Secrets/config/exposition sensible | @Codex | JIRA-AUDIT-016 | DONE | Fail-fast secrets prod + hardening Actuator valides |
| JIRA-AUDIT-019 | Frontend securite UX/XSS/messages | @Codex | JIRA-AUDIT-017 | DONE | Scan UI error flows + finding derive `031` |
| JIRA-AUDIT-020 | Tests securite non-regression | @Codex | JIRA-AUDIT-017,019 | DONE | Revalidation finale backend/frontend + i18n audit + builds verts (2026-02-14 12:38) |
| JIRA-AUDIT-021 | Remediation finale + cloture | @Codex | JIRA-AUDIT-020 | DONE | Tous findings High/Critical traites, risque residuel: none |
| JIRA-AUDIT-022 | Remediation CORS centralisee | @Codex | JIRA-AUDIT-016 | DONE | WebConfig cleanup + controllers + SecurityConfigCorsTest |
| JIRA-AUDIT-023 | Remediation XSS toast (`innerHTML`) | @Codex | JIRA-AUDIT-019 | DONE | NotificationCenter refactor Renderer2 + test XSS |
| JIRA-AUDIT-024 | Remediation secrets/config fail-fast | @Codex | JIRA-AUDIT-018 | DONE | `application-prod.yml` + `application.properties` nettoyes, tests/compile verts |
| JIRA-AUDIT-025 | Remediation usurpation userResolver (`user` query param, `X-Test-User`) | @Codex | JIRA-AUDIT-016 | DONE | UserResolver durci + `UserResolverTest` |
| JIRA-AUDIT-026 | Remediation trust IDs client (`creatorId`/`userId`) dans GameController | @Codex | JIRA-AUDIT-016 | DONE | `GameController` durci + `GameControllerSimpleTest` |
| JIRA-AUDIT-027 | Restreindre endpoints Actuator sensibles + exposition prod | @Codex | JIRA-AUDIT-018 | DONE | `SecurityConfig` + tests d'autorisation Actuator |
| JIRA-AUDIT-028 | IDOR TeamController (userId non proprietaire) | @Codex | JIRA-AUDIT-020 | DONE | TeamController durci (401/403) + blocage des operations directes remove/changes hors trade + tests verts |
| JIRA-AUDIT-029 | ScoreController write non restreint | @Codex | JIRA-AUDIT-020 | DONE | ScoreController durci (ADMIN only) + validation manuelle USER/ADMIN |
| JIRA-AUDIT-030 | Validation d'entree incomplete (Map payload + hours) | @Codex | JIRA-AUDIT-020 | DONE | DTOs valides + bornes `hours` + fix ObjectError/FieldError dans handler validation + tests backend verts |
| JIRA-AUDIT-031 | Fuite messages backend techniques frontend | @Codex | JIRA-AUDIT-020 | DONE | Sanitisation centralisee + validations UI + tests frontend passes |
| JIRA-AUDIT-032 | Anti brute-force join-with-code (rate limit) | @Codex | JIRA-AUDIT-020 | DONE | Guard backend + mapping UI 429 + validation manuelle OK |

## Playbooks d'execution
### Playbook 1 - Audit auth/access/CORS (`JIRA-AUDIT-016`)
1. Cartographier endpoints critiques backend (`/api/games`, `/api/drafts`, `/api/trades`, `/api/admin`).
2. Verifier authn/authz par role et ownership attendu.
3. Verifier CORS pour origines/methodes/headers.
4. Capturer findings dans le journal de preuves.

### Matrice endpoint x role (`JIRA-AUDIT-016`)
| Endpoint | Role attendu | Controle observe | Statut |
|---|---|---|---|
| `POST /api/games` | Utilisateur authentifie | `userResolver.resolve(...)` + `creatorId` force serveur dans `GameController` | OK |
| `POST /api/games/join` | Utilisateur authentifie | `userResolver.resolve(...)` + `userId` force serveur | OK |
| `POST /api/games/{id}/start-draft` | Proprietaire game uniquement | Check `gameDto.getCreatorId()` => `403` sinon | OK |
| `DELETE /api/games/{id}` | Proprietaire game uniquement | Check `gameDto.getCreatorId()` => `403` sinon | OK |
| `POST /api/games/{id}/regenerate-code` | Proprietaire game uniquement | Check `gameDto.getCreatorId()` => `403` sinon | OK |
| `POST /api/games/{id}/rename` | Proprietaire game uniquement | Check `gameDto.getCreatorId()` => `403` sinon | OK |
| `/api/admin/**` | `ROLE_ADMIN` | Regle explicite dans `SecurityConfig` | OK |
| `/actuator/health` | Public | `permitAll()` | OK |
| `/actuator/*` (autres) | `ROLE_ADMIN` uniquement | Regle explicite `.requestMatchers("/actuator/**").hasRole("ADMIN")` + test dedie | OK |

### Playbook 2 - Audit validation/injection (`JIRA-AUDIT-017`)
1. Inventorier DTO + validations (`@Valid`, contraintes).
2. Inspecter points d'entree controllers/services.
3. Verifier chemins persistence a risque (concatenation, mapping direct non valide).
4. Ticketer/corriger selon severite.

### Playbook 3 - Audit secrets/config (`JIRA-AUDIT-018`)
1. Scanner config/env/logs pour secrets ou donnees sensibles.
2. Verifier niveau de details des erreurs exposees client.
3. Definir regles de redaction/masquage.
4. Documenter recommandations.

### Playbook 4 - Frontend securite UX (`JIRA-AUDIT-019`)
1. Scanner affichage erreurs et contenus dynamiques.
2. Verifier absence de messages techniques bruts.
3. Verifier sanitisation sur zones sensibles.
4. Ajouter plan de correction.

### Playbook 5 - Tests et validations (`JIRA-AUDIT-020`, `021`)
- Backend:
- `mvn test` (cible selon modules touches)
- `mvn -DskipTests compile`
- Frontend:
- `npm run test:ci -- --watch=false`
- `npm run build`
- i18n (si textes modifies):
- `node frontend/scripts/i18n-audit.js`

## Journal des preuves
Format obligatoire:
- `Date | Ticket | Finding | Gravite | Fichier/Ligne | Decision`

Exemples:
- `2026-02-12 | JIRA-AUDIT-016 | Endpoint delete game accessible sans ownership strict | High | src/main/.../GameController.java:XX | Correction immediate`
- `2026-02-12 | JIRA-AUDIT-018 | Message erreur expose detail interne | Medium | src/main/.../GlobalExceptionHandler.java:XX | Ticket correctif`
- `2026-02-12 18:02 | JIRA-AUDIT-022 | CORS redondant MVC + @CrossOrigin locaux | High | src/main/java/com/fortnite/pronos/config/WebConfig.java / src/main/java/com/fortnite/pronos/controller/TradeController.java / src/main/java/com/fortnite/pronos/controller/LeaderboardController.java | Corrige, centralisation via SecurityConfig`
- `2026-02-12 18:02 | JIRA-AUDIT-022 | Validation regressions CORS | Medium | src/test/java/com/fortnite/pronos/config/SecurityConfigCorsTest.java | 4 tests verts (dev/prod, pas de wildcard)`
- `2026-02-12 18:24 | JIRA-AUDIT-023 | Injection XSS possible via toast.innerHTML | High | frontend/src/app/shared/components/notification-center/notification-center.component.ts | Corrige, rendu texte via Renderer2 (no innerHTML)`
- `2026-02-12 18:24 | JIRA-AUDIT-023 | Non-regression XSS toast | Medium | frontend/src/app/shared/components/notification-center/notification-center.component.spec.ts | Test ajoute, payload HTML/JS rendu en texte brut`
- `2026-02-12 19:32 | JIRA-AUDIT-016 | Usurpation possible via UserResolver (priorite username param + X-Test-User avant principal JWT) | High | src/main/java/com/fortnite/pronos/service/UserResolver.java | Ticket derive JIRA-AUDIT-025 cree`
- `2026-02-12 19:33 | JIRA-AUDIT-016 | Trust d'identite client (`creatorId`/`userId`) dans create/join game | High | src/main/java/com/fortnite/pronos/controller/GameController.java | Ticket derive JIRA-AUDIT-026 cree`
- `2026-02-12 19:47 | JIRA-AUDIT-025 | Query param user + header X-Test-User restreints aux profils dev/h2/test | High | src/main/java/com/fortnite/pronos/service/UserResolver.java | Corrige, tests `UserResolverTest` verts`
- `2026-02-12 19:47 | JIRA-AUDIT-026 | Ignorer `creatorId`/`userId` client en create/join game | High | src/main/java/com/fortnite/pronos/controller/GameController.java | Corrige, tests `GameControllerSimpleTest` verts`
- `2026-02-12 20:25 | JIRA-AUDIT-016 | Validation auth/access/CORS backend | Medium | mvn -Dtest=SecurityConfigCorsTest,SecurityConfigurationTest,UserResolverTest,GameControllerSimpleTest test | 29 tests verts`
- `2026-02-12 20:27 | JIRA-AUDIT-018 | Exposition Actuator sensible (`env/configprops/loggers/datasource`) | High | src/main/resources/application.yml + src/main/java/com/fortnite/pronos/config/SecurityConfig.java | Ticket derive JIRA-AUDIT-027 cree`
- `2026-02-12 20:27 | JIRA-AUDIT-018 | Fallback credentials en profil prod (`DB_USERNAME:postgres`, `DB_PASSWORD:postgres`) | High | src/main/resources/application-prod.yml | Ticket de remediation existant JIRA-AUDIT-024 confirme`
- `2026-02-12 20:32 | JIRA-AUDIT-024 | Fallback DB credentials supprimes en prod (`${DB_USERNAME}`, `${DB_PASSWORD}`) | High | src/main/resources/application-prod.yml | Corrige partiellement, compile vert`
- `2026-02-12 20:33 | JIRA-AUDIT-018 | Revalidation backend auth/access apres changements config | Medium | mvn -Dtest=SecurityConfigCorsTest,SecurityConfigurationTest,UserResolverTest,GameControllerSimpleTest test | 29 tests verts`
- `2026-02-12 21:03 | JIRA-AUDIT-024 | Alignement final fail-fast secrets + suppression redondance datasource properties | High | src/main/resources/application-prod.yml / src/main/resources/application.properties | Corrige, validation backend ciblee + compile verts`
- `2026-02-12 21:03 | JIRA-AUDIT-027 | Durcissement Actuator (`health` public, autres endpoints admin) + reduction exposition | High | src/main/java/com/fortnite/pronos/config/SecurityConfig.java / src/main/resources/application.yml / src/main/resources/application-prod.yml | Corrige, tests `SecurityConfigActuatorAuthorizationTest` verts`
- `2026-02-12 21:03 | JIRA-AUDIT-018 | Cloture lot config/exposition sensible | Medium | mvn -Dtest=SecurityConfigActuatorAuthorizationTest,SecurityConfigCorsTest,SecurityConfigurationTest,UserResolverTest,GameControllerSimpleTest,ProductionConfigurationSecurityTest test | 36 tests verts`
- `2026-02-12 21:17 | JIRA-AUDIT-017 | IDOR possible via `userId` sur endpoints write team | High | src/main/java/com/fortnite/pronos/controller/TeamController.java:134,165,177,190 | Ticket derive JIRA-AUDIT-028 cree`
- `2026-02-12 21:17 | JIRA-AUDIT-017 | Endpoints write score accessibles a tout utilisateur authentifie | High | src/main/java/com/fortnite/pronos/controller/ScoreController.java:40,47,54,60,65 | Ticket derive JIRA-AUDIT-029 cree`
- `2026-02-12 21:17 | JIRA-AUDIT-017 | Validation d'entree partielle (payload Map + param hours non borne) | Medium | src/main/java/com/fortnite/pronos/controller/GameController.java:240,427 / src/main/java/com/fortnite/pronos/controller/AdminDashboardController.java:44 | Ticket derive JIRA-AUDIT-030 cree`
- `2026-02-12 21:17 | JIRA-AUDIT-019 | Fuite potentielle de messages backend techniques dans UI | High | frontend/src/app/features/game/services/game-command.service.ts:252 / frontend/src/app/features/draft/services/draft.service.ts:259 | Ticket derive JIRA-AUDIT-031 cree`
- `2026-02-12 21:17 | JIRA-AUDIT-020 | Passage en execution tests non-regression derives | Medium | Jira-tache.txt / docs/audit/AUDIT_MASTER_PLAN.md | Ticket 020 passe IN_PROGRESS`
- `2026-02-12 21:20 | JIRA-AUDIT-020 | Revalidation baseline securite backend avant remediation derives | Medium | mvn -Dtest=SecurityConfigActuatorAuthorizationTest,SecurityConfigCorsTest,SecurityConfigurationTest,UserResolverTest,GameControllerSimpleTest test | 33 tests verts`
- `2026-02-12 21:20 | JIRA-AUDIT-020 | Verification compile backend avant lots correctifs | Medium | mvn -DskipTests compile | BUILD SUCCESS`
- `2026-02-13 06:56 | JIRA-AUDIT-028 | Durcissement TeamController contre spoofing `userId` (create/add/remove/changes) | High | src/main/java/com/fortnite/pronos/controller/TeamController.java | Correction appliquee, mismatch => 403`
- `2026-02-13 06:56 | JIRA-AUDIT-028 | Ajout tests de non-regression IDOR TeamController | High | src/test/java/com/fortnite/pronos/controller/TeamControllerSecurityTest.java | 6 tests verts`
- `2026-02-13 06:57 | JIRA-AUDIT-020 | Revalidation securite backend post-correctif IDOR TeamController | Medium | mvn -Dtest=TeamControllerSecurityTest,SecurityConfigurationTest,SecurityConfigCorsTest,UserResolverTest test | 23 tests verts`
- `2026-02-13 07:06 | JIRA-AUDIT-029 | Durcissement ScoreController write endpoints (update/batch/recalculate/create/delete) avec controle `ADMIN` via `UserResolver` | High | src/main/java/com/fortnite/pronos/controller/ScoreController.java | Correction appliquee, non-admin => 403`
- `2026-02-13 07:06 | JIRA-AUDIT-029 | Ajout tests de non-regression authz ScoreController | High | src/test/java/com/fortnite/pronos/controller/ScoreControllerSecurityTest.java | 8 tests verts`
- `2026-02-13 07:06 | JIRA-AUDIT-020 | Revalidation securite backend post-correctif ScoreController | Medium | mvn -Dtest=TeamControllerSecurityTest,ScoreControllerSecurityTest,SecurityConfigurationTest,SecurityConfigCorsTest,UserResolverTest test | 31 tests verts`
- `2026-02-13 07:20 | JIRA-AUDIT-031 | Sanitisation centralisee des messages backend frontend (`extractBackendErrorDetails`, `toSafeUserMessage`) | High | frontend/src/app/core/utils/user-facing-error-message.util.ts | Corrige, fallback i18n sur messages techniques/mojibake`
- `2026-02-13 07:20 | JIRA-AUDIT-031 | Application de la sanitisation sur GameCommandService + DraftService | High | frontend/src/app/features/game/services/game-command.service.ts / frontend/src/app/features/draft/services/draft.service.ts | Corrige, plus de passthrough 5xx brut`
- `2026-02-13 07:20 | JIRA-AUDIT-031 | Validation tests frontend securite messages | Medium | npx ng test --watch=false --browsers=ChromeHeadless --include game-command.service.spec.ts --include draft.service.spec.ts --include user-facing-error-message.util.spec.ts | 43 SUCCESS`
- `2026-02-13 07:20 | JIRA-AUDIT-031 | Revalidation full Karma | Medium | npm --prefix frontend run test:ci | sortie verte (exit code 0)`
- `2026-02-13 17:52 | JIRA-AUDIT-031 | Join-with-code n'expose plus les messages backend bruts (`Invalid input parameters`, `Not found`) | High | frontend/src/app/features/game/join-game/join-game.component.ts / frontend/src/app/shared/components/main-layout/main-layout.component.ts / frontend/src/app/features/game/services/join-error-message.resolver.ts | Corrige, fallback i18n + mapping code/message`
- `2026-02-13 17:52 | JIRA-AUDIT-031 | Remplacement prompt renommage par composant dialogue avec validation explicite | Medium | frontend/src/app/features/game/components/rename-game-dialog/rename-game-dialog.component.ts / frontend/src/app/features/game/services/game-detail-actions.service.ts | Corrige, erreur claire sur espaces/min/max/nom identique`
- `2026-02-13 17:52 | JIRA-AUDIT-031 | Validation frontend post-correctifs UX/i18n | Medium | npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --include join-game.component.spec.ts --include main-layout.component.spec.ts --include game-detail-actions.service.spec.ts --include rename-game-dialog.component.spec.ts / node frontend/scripts/i18n-audit.js / npm --prefix frontend run build | 59 tests verts + audit i18n OK + build OK`
- `2026-02-13 20:44 | JIRA-AUDIT-032 | Endpoint `join-with-code` vulnerable au brute-force (pas de throttling dedie) | High | src/main/java/com/fortnite/pronos/controller/GameController.java | Guard dedie ajoute (15 tentatives/minute/acteur), 429 sur depassement`
- `2026-02-13 20:44 | JIRA-AUDIT-032 | Mapping frontend du 429 join-code vers message i18n utilisateur | Medium | frontend/src/app/features/game/services/join-error-message.resolver.ts / join-game.component.spec.ts / main-layout.component.spec.ts | Corrige, cle `games.joinDialog.tooManyAttempts` + tests verts`
- `2026-02-13 20:44 | JIRA-AUDIT-032 | Validation technique post-correctif anti brute-force | Medium | mvn -Dtest=InvitationCodeAttemptGuardTest,GameControllerSimpleTest test / npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --include join-game.component.spec.ts --include main-layout.component.spec.ts --include translation.service.spec.ts / node frontend/scripts/i18n-audit.js / npm --prefix frontend run build | Backend 19 tests verts + Frontend 58 tests verts + audit/build OK`
- `2026-02-13 21:15 | JIRA-AUDIT-031/JIRA-AUDIT-032 | Validation manuelle UI confirmee (messages sanitises + blocage brute-force visible) | Medium | Application web (join-with-code + modale rename) | Tickets clotures et retires du backlog`
- `2026-02-13 21:21 | JIRA-AUDIT-029 | Validation manuelle authorization write score | Medium | POST /scores/recalculate/season/2026 (user=thibaut => 403, user=admin => 200) | Ticket cloture et retire du backlog`
- `2026-02-13 07:34 | JIRA-AUDIT-030 | Remplacement payloads permissifs par DTOs validates (`join-with-code`, `rename`) | Medium | src/main/java/com/fortnite/pronos/controller/GameController.java / src/main/java/com/fortnite/pronos/dto/JoinGameWithCodeRequest.java / src/main/java/com/fortnite/pronos/dto/RenameGameRequest.java | Corrige, `@Valid` + garde 400 explicite`
- `2026-02-13 07:34 | JIRA-AUDIT-030 | Bornage strict de `hours` (1..168) + mapping validation 400 | Medium | src/main/java/com/fortnite/pronos/controller/AdminDashboardController.java / src/main/java/com/fortnite/pronos/config/GlobalExceptionHandler.java | Corrige, ajout `@Min/@Max` + handler `ConstraintViolationException``
- `2026-02-13 07:38 | JIRA-AUDIT-030 | Revalidation backend post-correctif DTO/hours | Medium | mvn -Dtest=TeamControllerSecurityTest,ScoreControllerSecurityTest,GameControllerSimpleTest,AdminDashboardControllerTest,GlobalExceptionHandlerTest test | 44 tests verts`
- `2026-02-14 11:47 | JIRA-AUDIT-030 | Eviter 500 sur validation join-code (ObjectError cast) | Medium | src/main/java/com/fortnite/pronos/config/GlobalExceptionHandler.java / src/test/java/com/fortnite/pronos/config/GlobalExceptionHandlerTest.java | Corrige, mapping FieldError+ObjectError; tests cibles backend verts`
- `2026-02-14 11:50 | JIRA-AUDIT-028 | Revalidation securite post-fix IDOR TeamController | Medium | mvn -Dtest=TeamControllerSecurityTest,SecurityConfigurationTest,SecurityConfigCorsTest,UserResolverTest test | 23 tests verts, reste validation manuelle fonctionnelle`
- `2026-02-14 12:30 | JIRA-AUDIT-028 | Cloture IDOR TeamController et alignement metier trade-only | High | src/main/java/com/fortnite/pronos/controller/TeamController.java / src/test/java/com/fortnite/pronos/controller/TeamControllerSecurityTest.java | DONE, remove/changes => 403 + non-regression securite/trade verte`
- `2026-02-14 12:38 | JIRA-AUDIT-020 | Revalidation finale backend securite ciblee | Medium | mvn -Dtest=SecurityConfigActuatorAuthorizationTest,SecurityConfigCorsTest,SecurityConfigurationTest,UserResolverTest,GameControllerSimpleTest,TeamControllerSecurityTest,ScoreControllerSecurityTest,GlobalExceptionHandlerTest,InvitationCodeAttemptGuardTest,AdminDashboardControllerTest,TradingServiceTddTest,TradeQueryServiceTddTest test | 101 tests verts`
- `2026-02-14 12:38 | JIRA-AUDIT-020 | Revalidation finale frontend securite ciblee | Medium | npx --prefix frontend ng test --watch=false --browsers=ChromeHeadless --include game-command.service.spec.ts --include draft.service.spec.ts --include user-facing-error-message.util.spec.ts --include join-game.component.spec.ts --include main-layout.component.spec.ts --include rename-game-dialog.component.spec.ts --include game-detail-actions.service.spec.ts --include translation.service.spec.ts | 119 tests verts`
- `2026-02-14 12:38 | JIRA-AUDIT-020 | Validation qualite finale (i18n/build/compile) | Medium | node frontend/scripts/i18n-audit.js / npm --prefix frontend run build / mvn -DskipTests compile | i18n audit OK (968 keys) + build frontend OK + compile backend OK`
- `2026-02-14 12:38 | JIRA-AUDIT-021 | Consolidation finale remediations OWASP | Medium | Jira-tache.txt / docs/audit/AUDIT_MASTER_PLAN.md | Tous findings High/Critical traites, risque residuel none`
- `2026-02-14 12:38 | JIRA-AUDIT-010 | Cloture finale epic OWASP | Medium | Jira-tache.txt / docs/audit/AUDIT_MASTER_PLAN.md | Sous-chaine 016..021 complete, preuves consolidees`
- `2026-02-15 22:20 | JIRA-AUDIT-006 | Baseline couverture backend/frontend + hotspots <60% | Medium | target/site/jacoco/jacoco.xml / frontend/coverage/frontend / docs/audit/COVERAGE_GAP_REPORT.md | Backend 70.67%, Frontend 80.08%, gaps critiques identifies`
- `2026-02-15 22:20 | JIRA-AUDIT-006 | Suites instables detectees pendant collecte couverture | High | GameWorkflowIntegrationTest (4 fails 409) / Karma (21 fails) | Tickets derives crees: JIRA-TEST-001 a JIRA-TEST-004`

## Plan de remediation
- Criteres de cloture ticket:
- tous les criteres d'acceptation du ticket sont coches
- preuves de validation ajoutees dans ce document
- Criteres de cloture globale `JIRA-AUDIT-010`:
- `JIRA-AUDIT-016` a `JIRA-AUDIT-021` termines
- findings `Critical/High` corriges ou risques acceptes explicitement
- suite de validation technique verte (tests/build) apres remediation
- Risque residuel final (2026-02-14): none

## Historique
- 2026-02-12: creation du document, split du parent `JIRA-AUDIT-010`, ouverture des sous-tickets `JIRA-AUDIT-016` a `JIRA-AUDIT-021`.
- 2026-02-12: findings preliminaires enregistres depuis `JIRA-AUDIT-016` et tickets de remediation ajoutes: `JIRA-AUDIT-022` (CORS), `JIRA-AUDIT-023` (XSS toast), `JIRA-AUDIT-024` (secrets/config).
- 2026-02-12 18:02: `JIRA-AUDIT-022` termine (suppression CORS redondant + tests CORS passes) puis retire du backlog actif.
- 2026-02-12 18:24: `JIRA-AUDIT-023` termine (suppression `innerHTML` dans NotificationCenter + test XSS) puis retire du backlog actif.
- 2026-02-12 19:32: nouveau finding auth/access sur `UserResolver`; creation du ticket `JIRA-AUDIT-025` pour remediation dediee.
- 2026-02-12 19:33: nouveau finding auth/access sur `GameController` (IDs payload sensibles); creation du ticket `JIRA-AUDIT-026`.
- 2026-02-12 19:47: `JIRA-AUDIT-025` et `JIRA-AUDIT-026` termines (durcissement auth resolver + payload spoofing), puis retires du backlog actif.
- 2026-02-12 20:27: `JIRA-AUDIT-016` termine (matrice endpoint/role + tests backend cibles), puis retire du backlog actif.
- 2026-02-12 20:27: demarrage `JIRA-AUDIT-018`; findings tickets confirmes/crees: `JIRA-AUDIT-024` (fallback credentials) et `JIRA-AUDIT-027` (hardening Actuator).
- 2026-02-12 20:32: demarrage execution `JIRA-AUDIT-024`; suppression des fallbacks DB credentials en `application-prod.yml`, verification compile OK.
- 2026-02-12 20:33: revalidation backend (29 tests securite cibles verts), poursuite `JIRA-AUDIT-018` et `JIRA-AUDIT-024`.
- 2026-02-12 21:03: `JIRA-AUDIT-024` termine (fail-fast secrets/config + nettoyage redondances), `JIRA-AUDIT-027` termine (policy Actuator `health` public / autres admin), puis `JIRA-AUDIT-018` cloture.
- 2026-02-12 21:17: `JIRA-AUDIT-017` execute et cloture (findings derives `JIRA-AUDIT-028` a `JIRA-AUDIT-030`), puis ticket retire du backlog actif.
- 2026-02-12 21:17: `JIRA-AUDIT-019` execute et cloture (finding derive `JIRA-AUDIT-031`), puis ticket retire du backlog actif.
- 2026-02-12 21:17: `JIRA-AUDIT-020` demarre pour couvrir les derives critiques/hauts via tests de non-regression.
- 2026-02-12 21:20: baseline de validation backend relancee (33 tests securite cibles + compile vert) avant implementation des remediations derivees.
- 2026-02-13 06:56: demarrage remediation `JIRA-AUDIT-028` (IDOR TeamController), guard ownership ajoute sur endpoints write avec 401/403 explicites.
- 2026-02-13 06:57: validation post-correctif `JIRA-AUDIT-028` executee (23 tests securite cibles verts).
- 2026-02-13 07:06: demarrage remediation `JIRA-AUDIT-029` (write score tampering), guard `ADMIN` applique sur les endpoints write score + tests dedies verts.
- 2026-02-13 07:20: demarrage remediation `JIRA-AUDIT-031` (messages backend techniques frontend), sanitisation centralisee + revalidation tests cibles et full Karma.
- 2026-02-13 07:34: demarrage remediation `JIRA-AUDIT-030` (DTO + bornes `hours`), correctifs backend appliques + tests cibles verts.
- 2026-02-13 17:52: `JIRA-AUDIT-031` etendu suite retour utilisateur (join-code i18n + composant de renommage), tests cibles + audit i18n + build frontend valides.
- 2026-02-13 21:15: validation manuelle utilisateur OK pour `JIRA-AUDIT-031` et `JIRA-AUDIT-032`; tickets clotures puis supprimes du backlog.
- 2026-02-13 21:21: validation manuelle `JIRA-AUDIT-029` confirmee (`USER` bloque, `ADMIN` autorise); ticket cloture puis supprime du backlog.
- 2026-02-14 11:47: `JIRA-AUDIT-030` finalise (fix defensive sur `GlobalExceptionHandler` pour erreurs de validation globales), tests backend cibles repasses verts; ticket retire du backlog.
- 2026-02-14 11:50: revalidation securite `JIRA-AUDIT-028` executee (23 tests verts); ticket maintenu en BLOCKED en attente de validation manuelle fonctionnelle teams.
- 2026-02-14 12:30: `JIRA-AUDIT-028` cloture et retire du backlog (durcissement ownership + blocage remove/changes hors trade, tests securite/trade verts).
- 2026-02-14 12:38: `JIRA-AUDIT-020` cloture apres revalidation finale backend/frontend + i18n audit + build/compile (tous verts).
- 2026-02-14 12:38: `JIRA-AUDIT-021` cloture (sweep final OWASP termine, aucun risque residuel High/Critical).
- 2026-02-14 12:38: `JIRA-AUDIT-010` cloture et retire du backlog (epic OWASP complete).
- 2026-02-15 22:20: execution `JIRA-AUDIT-006` (rapport `docs/audit/COVERAGE_GAP_REPORT.md`) puis creation des tickets derives `JIRA-TEST-001..004` pour stabilisation suites et comblement des gaps de couverture.
