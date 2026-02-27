# AUDIT SECURITY - JIRA-TECH-017

Date: 2026-02-27
Perimetre: backend Spring Boot, frontend Angular, configuration runtime, Docker/Compose

## 1) Executive Summary

Niveau de risque actuel: **CRITIQUE**.

- P0 (critique): 3
- P1 (eleve): 4
- P2 (moyen): 2

Points bloquants immediats:
1. Authentification contournable sans verification de mot de passe sur le flux actif `/api/auth/login`.
2. Exposition potentielle du champ `password` via endpoints `/api/users` accessibles a tout utilisateur authentifie.
3. Vulnerabilites critiques frontend (`npm audit`: 1 critical, 18 high).

## 2) Methodologie / Preuves techniques

Commandes executees:
- `npm --prefix frontend audit --json`
- `mvn -DskipTests dependency-check:check -Dformat=JSON -DfailOnError=false`
- `rg` ciblant SecurityConfig, WebSocket, Auth, User API, yml, compose, scripts

Resultat scan npm:
- `TOTAL=30 CRITICAL=1 HIGH=18 MODERATE=8 LOW=3`
- Vulnerabilite critique observee: `@angular/ssr` avec advisory `GHSA-x288-3778-4hhx` (et autres advisories associes).

Limite scan backend CVE:
- Le scan OWASP Dependency Check backend ne peut pas etre considere fiable en l'etat:
  - `Error updating the NVD Data; the NVD returned a 403 or 404 error`
  - `Unable to continue dependency-check analysis`
- Cause: cle API NVD absente.

## 3) Findings detailles

### SEC-F-001 - Auth bypass sur login (P0)

Categorie OWASP: A07 Identification and Authentication Failures

Preuves:
- [src/main/java/com/fortnite/pronos/service/UnifiedAuthService.java](src/main/java/com/fortnite/pronos/service/UnifiedAuthService.java):40
- [src/main/java/com/fortnite/pronos/service/UnifiedAuthService.java](src/main/java/com/fortnite/pronos/service/UnifiedAuthService.java):43
- [src/main/java/com/fortnite/pronos/service/UnifiedAuthService.java](src/main/java/com/fortnite/pronos/service/UnifiedAuthService.java):52
- [src/main/java/com/fortnite/pronos/dto/auth/LoginRequest.java](src/main/java/com/fortnite/pronos/dto/auth/LoginRequest.java):16
- [src/main/java/com/fortnite/pronos/controller/AuthController.java](src/main/java/com/fortnite/pronos/controller/AuthController.java):171
- [src/test/java/com/fortnite/pronos/service/UnifiedAuthServiceTddTest.java](src/test/java/com/fortnite/pronos/service/UnifiedAuthServiceTddTest.java):222

Constat:
- La methode `UnifiedAuthService.login(...)` genere un JWT apres verification de l'existence utilisateur, sans verifier `request.password`.
- `LoginRequest.password` est obligatoire mais non utilise dans le flux actif.

Impact:
- Prise de compte probable des qu'un username valide est connu.

Remediation:
- Basculer le flux de login sur une verification stricte mot de passe (`AuthenticationManager` ou strategy prod fiable).
- Interdire toute emission de token sans verif de credential.

---

### SEC-F-002 - Exposition du mot de passe via API users (P0)

Categorie OWASP: A01 Broken Access Control / A02 Cryptographic Failures

Preuves:
- [src/main/java/com/fortnite/pronos/controller/UserController.java](src/main/java/com/fortnite/pronos/controller/UserController.java):18
- [src/main/java/com/fortnite/pronos/controller/UserController.java](src/main/java/com/fortnite/pronos/controller/UserController.java):25
- [src/main/java/com/fortnite/pronos/model/User.java](src/main/java/com/fortnite/pronos/model/User.java):31
- [src/main/java/com/fortnite/pronos/model/User.java](src/main/java/com/fortnite/pronos/model/User.java):64
- [src/main/java/com/fortnite/pronos/config/SecurityConfig.java](src/main/java/com/fortnite/pronos/config/SecurityConfig.java):114
- [src/main/java/com/fortnite/pronos/config/SecurityConfig.java](src/main/java/com/fortnite/pronos/config/SecurityConfig.java):115

Constat:
- `/api/users` retourne des entites `User` completes.
- `User` contient `password` et `getPasswordHash()` retourne ce meme champ.
- Les routes `/api/**` sont seulement `authenticated()`, pas `ADMIN`.

Impact:
- Fuite de hash/password vers des utilisateurs authentifies non-admin.

Remediation:
- Ne jamais serialiser `password` (`@JsonIgnore` + DTO de sortie dedie).
- Restreindre `/api/users/**` a `ROLE_ADMIN`.

---

### SEC-F-003 - Dependances frontend critiques (P0)

Categorie OWASP: A06 Vulnerable and Outdated Components

Preuves:
- [frontend/package.json](frontend/package.json):27
- `npm audit`: `TOTAL=30 CRITICAL=1 HIGH=18 MODERATE=8 LOW=3`
- Advisory critique inclut `GHSA-x288-3778-4hhx` sur `@angular/ssr`.

Constat:
- Le frontend embarque une pile avec vulnerabilites critiques/hautes non corrigees.

Impact:
- Surface d'attaque accrue (SSRF/XSS/fuites inter-requetes selon advisory).

Remediation:
- Upgrade Angular stack vers version corrigee et rerun `npm audit` jusqu'a disparition des severites critiques.

---

### SEC-F-004 - Secrets/config sensibles dans code et scripts (P1)

Categorie OWASP: A05 Security Misconfiguration

Preuves:
- [src/main/java/com/fortnite/pronos/service/auth/ProductionAuthenticationStrategy.java](src/main/java/com/fortnite/pronos/service/auth/ProductionAuthenticationStrategy.java):60
- [launch.bat](launch.bat):8
- [src/main/resources/application.yml](src/main/resources/application.yml):3
- [src/main/resources/application.yml](src/main/resources/application.yml):46
- [src/main/resources/application-dev.yml](src/main/resources/application-dev.yml):5
- [src/main/resources/application-dev.yml](src/main/resources/application-dev.yml):48
- [docker-compose.dev.yml](docker-compose.dev.yml):18
- `.env` non tracke (gitignore), `.env.example` tracke

Constat:
- Presence de secret JWT hardcode dans code/script.
- Presence de mots de passe par defaut (`fortnite_pass`, `admin`) en config dev.

Impact:
- Mauvaises pratiques reutilisables en prod par erreur, compromission simplifiee en cas de fuite repo/script.

Remediation:
- Supprimer secrets hardcodes, forcer variables d'environnement obligatoires.
- Rotation des secrets deja exposes.

---

### SEC-F-005 - AuthZ WebSocket admin + exposition health detaillee (P1)

Categorie OWASP: A01 Broken Access Control / A05 Security Misconfiguration

Preuves:
- [src/main/java/com/fortnite/pronos/config/SecurityConfig.java](src/main/java/com/fortnite/pronos/config/SecurityConfig.java):110
- [src/main/java/com/fortnite/pronos/config/WebSocketConfig.java](src/main/java/com/fortnite/pronos/config/WebSocketConfig.java):28
- [src/main/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineService.java](src/main/java/com/fortnite/pronos/service/admin/PlayerIdentityPipelineService.java):26
- [src/main/java/com/fortnite/pronos/config/SecurityConfig.java](src/main/java/com/fortnite/pronos/config/SecurityConfig.java):102
- [src/main/resources/application-prod.yml](src/main/resources/application-prod.yml):101

Constat:
- `/ws/**` est `permitAll`.
- Endpoint STOMP `/ws` accepte `setAllowedOriginPatterns("*")`.
- Topic admin `/topic/admin/pipeline` est publie sans preuve de regles de subscription role-based.
- `/actuator/health` est public et `show-details: always` en prod.

Impact:
- Risque de fuite d'informations d'admin via WS et details techniques via health endpoint.

Remediation:
- Ajouter une security message-level STOMP (`simpSubscribeDestMatchers`) et verrouiller `/topic/admin/**` a `ROLE_ADMIN`.
- En prod: `show-details: never` pour health public, ou health detaille reserve auth admin.

---

### SEC-F-006 - CSRF/CORS et validation d'entrees: ecarts (P1/P2)

Categorie OWASP: A05 Security Misconfiguration / A03 Injection

Preuves:
- [src/main/java/com/fortnite/pronos/config/SecurityConfig.java](src/main/java/com/fortnite/pronos/config/SecurityConfig.java):55
- [src/main/java/com/fortnite/pronos/config/DevSecurityConfig.java](src/main/java/com/fortnite/pronos/config/DevSecurityConfig.java):40
- [src/main/java/com/fortnite/pronos/controller/ScoreController.java](src/main/java/com/fortnite/pronos/controller/ScoreController.java):51
- [src/main/java/com/fortnite/pronos/controller/ScoreController.java](src/main/java/com/fortnite/pronos/controller/ScoreController.java):90

Constat:
- CSRF desactive globalement (acceptable uniquement si auth strictement Bearer et pas de cookie auth).
- En profil dev/h2/test: `anyRequest().permitAll()`.
- Plusieurs endpoints `@RequestBody` sans `@Valid` (validation inegale).

Impact:
- Risque de derive de configuration et de validation insuffisante sur certains flux metier.

Remediation:
- Encadrer explicitement la politique CSRF selon mode d'auth.
- Standardiser `@Valid` + contraintes DTO sur endpoints mutables.

---

### SEC-F-007 - Durcissement Docker/Compose incomplet (P1)

Categorie OWASP: A05 Security Misconfiguration

Preuves:
- [docker-compose.prod.yml](docker-compose.prod.yml):55
- [docker-compose.prod.yml](docker-compose.prod.yml):275
- [docker-compose.prod.yml](docker-compose.prod.yml):289
- [docker-compose.prod.yml](docker-compose.prod.yml):240
- [docker-compose.sonar.yml](docker-compose.sonar.yml):10
- [docker-compose.sonar.yml](docker-compose.sonar.yml):32

Constat:
- Exposition de ports management/dashboard (9090/3000/9000).
- Images `latest` (Prometheus, Grafana) non pinnees.
- Credentials faibles/default dans stack Sonar (`sonar/sonar`) et fallback Grafana `admin`.
- Mesures de confinement absentes (ex: `read_only`, `cap_drop`, `no-new-privileges`) dans compose.

Impact:
- Surface d'attaque infra augmentee, drift de version et compromission d'outils annexes.

Remediation:
- Pinner images par version/digest.
- Supprimer credentials par defaut, secrets obligatoires.
- Ajouter options de confinement conteneur.

---

### SEC-F-008 - Scan CVE backend non fiable (P2, gouvernance)

Categorie: Process / Security governance

Preuves:
- `mvn ... dependency-check:check` -> `NVD returned a 403 or 404` / `Unable to continue dependency-check analysis`

Constat:
- Absence de controle CVE backend fiable en continu.

Impact:
- Vulns backend recentes potentiellement non detectees.

Remediation:
- Configurer cle API NVD en CI/CD et produire rapport JSON/XML exploitable.

## 4) Plan de correction priorise

1. Corriger SEC-F-001 et SEC-F-002 avant tout deploiement.
2. Corriger SEC-F-003 (deps critiques frontend) dans le meme sprint.
3. Verrouiller WS/admin + health details (SEC-F-005).
4. Nettoyer secrets hardcodes + durcir compose (SEC-F-004/SEC-F-007).
5. Industrialiser scan CVE backend (SEC-F-008).

## 5) TICKETS JIRA GÉNÉRÉS

### Ticket 1 (P0)
ID: JIRA-SEC-001
TITRE: Bloquer le bypass mot de passe sur /api/auth/login
TYPE: Task
PRIORITE: P0
STATUT: TODO
ESTIMATE: 4-6h
ASSIGNE: ->
ASSIGNE_LE: ->
DERNIERE_MAJ: 2026-02-27
DEPENDENCIES: JIRA-TECH-017

OBJECTIF:
Garantir qu'aucun JWT ne soit emis sans verification du mot de passe.

DESCRIPTION:
- Remplacer/retirer le flux `UnifiedAuthService.login(...)` non securise.
- Verifier credentials via composant Spring Security standard.
- Interdire toute logique "MVP sans mot de passe" en runtime non-test.
- Implementation en TDD, lots <= 200 lignes.

CRITERES_D_ACCEPTATION:
- Login mauvais mot de passe => 401.
- Login bon mot de passe => 200 + JWT valide.
- Test de non-regression obligatoire: utilisateur existant + mauvais mot de passe ne recoit jamais de token.

### Ticket 2 (P0)
ID: JIRA-SEC-002
TITRE: Supprimer l'exposition du champ password sur /api/users
TYPE: Task
PRIORITE: P0
STATUT: TODO
ESTIMATE: 3-5h
ASSIGNE: ->
ASSIGNE_LE: ->
DERNIERE_MAJ: 2026-02-27
DEPENDENCIES: JIRA-TECH-017

OBJECTIF:
Empêcher toute fuite de mot de passe/hash via API.

DESCRIPTION:
- Introduire DTO de sortie user sans champ sensible.
- Ajouter protection role ADMIN sur endpoints user sensibles.
- Ajouter serialisation defensive (`@JsonIgnore` sur secret si besoin).
- Implementation en TDD, lots <= 200 lignes.

CRITERES_D_ACCEPTATION:
- `/api/users` inaccessible aux non-admin.
- Reponse API user ne contient jamais `password` ni hash.
- Test de non-regression obligatoire: assertion JSON absence de champ sensible.

### Ticket 3 (P0)
ID: JIRA-SEC-003
TITRE: Corriger les vulnerabilites critiques Angular SSR/dependances
TYPE: Task
PRIORITE: P0
STATUT: TODO
ESTIMATE: 4-8h
ASSIGNE: ->
ASSIGNE_LE: ->
DERNIERE_MAJ: 2026-02-27
DEPENDENCIES: JIRA-TECH-017

OBJECTIF:
Eliminer les CVE critiques/hautes de la stack frontend.

DESCRIPTION:
- Upgrader Angular/SSR vers versions corrigees.
- Executer `npm audit fix` puis corriger manuellement les residuels.
- Verifier build/test frontend apres upgrade.
- Implementation en TDD, lots <= 200 lignes.

CRITERES_D_ACCEPTATION:
- `npm audit` ne retourne plus de severite critical.
- Regression tests frontend passes.
- Test de non-regression obligatoire: scenario SSR/route sensible valide apres upgrade.

### Ticket 4 (P1 - categorie Secrets/Config)
ID: JIRA-SEC-004
TITRE: Eliminer secrets hardcodes et credentials par defaut
TYPE: Task
PRIORITE: P1
STATUT: TODO
ESTIMATE: 4-6h
ASSIGNE: ->
ASSIGNE_LE: ->
DERNIERE_MAJ: 2026-02-27
DEPENDENCIES: JIRA-TECH-017

OBJECTIF:
Supprimer les secrets du code et imposer une config securisee.

DESCRIPTION:
- Retirer secret JWT hardcode des classes/scripts.
- Remplacer fallbacks sensibles par variables obligatoires en environnements non-dev.
- Ajouter checks de demarrage si secret absent.
- TDD + lots <= 200 lignes.

CRITERES_D_ACCEPTATION:
- Aucun secret hardcode en source trackee.
- Demarrage prod echoue si secret critique manquant.
- Documentation variables obligatoires mise a jour.

### Ticket 5 (P1 - categorie AuthN/AuthZ + WS + Actuator)
ID: JIRA-SEC-005
TITRE: Durcir AuthZ WebSocket admin et endpoint health
TYPE: Task
PRIORITE: P1
STATUT: TODO
ESTIMATE: 4-6h
ASSIGNE: ->
ASSIGNE_LE: ->
DERNIERE_MAJ: 2026-02-27
DEPENDENCIES: JIRA-TECH-017

OBJECTIF:
Limiter l'acces aux flux admin et reduire l'exposition d'informations runtime.

DESCRIPTION:
- Ajouter regles STOMP role-based pour `/topic/admin/**`.
- Fermer `permitAll` WS quand non necessaire.
- Masquer details health publics en prod (`show-details: never` ou endpoint protege).
- TDD + lots <= 200 lignes.

CRITERES_D_ACCEPTATION:
- Un user non-admin ne peut pas s'abonner aux topics admin.
- Health public ne divulgue pas composants/details internes.
- Tests d'autorisation passes.

### Ticket 6 (P1 - categorie CORS/CSRF/XSS/Validation)
ID: JIRA-SEC-006
TITRE: Standardiser validation d'entrees et politique CSRF/CORS
TYPE: Task
PRIORITE: P1
STATUT: TODO
ESTIMATE: 3-5h
ASSIGNE: ->
ASSIGNE_LE: ->
DERNIERE_MAJ: 2026-02-27
DEPENDENCIES: JIRA-TECH-017

OBJECTIF:
Uniformiser la defense en profondeur sur les endpoints mutables.

DESCRIPTION:
- Ajouter `@Valid` et contraintes DTO sur endpoints identifies.
- Documenter clairement le modele CSRF (JWT bearer only) et garde-fous.
- Verifier CORS minimaliste par environnement.
- TDD + lots <= 200 lignes.

CRITERES_D_ACCEPTATION:
- Endpoints mutables critiques valident les payloads.
- Politique CSRF/CORS documentee et testee.
- Tests edge cases d'entree invalide passes.

### Ticket 7 (P1 - categorie Docker/Compose hardening)
ID: JIRA-SEC-007
TITRE: Durcir Docker Compose production et outils annexes
TYPE: Task
PRIORITE: P1
STATUT: TODO
ESTIMATE: 4-6h
ASSIGNE: ->
ASSIGNE_LE: ->
DERNIERE_MAJ: 2026-02-27
DEPENDENCIES: JIRA-TECH-017

OBJECTIF:
Reduire la surface d'attaque infrastructure conteneurisee.

DESCRIPTION:
- Pinner images (pas de `latest`).
- Supprimer credentials defaults Sonar/Grafana.
- Ajouter confinement (`read_only`, `cap_drop`, `no-new-privileges`, user non-root si possible).
- TDD infra smoke-tests + lots <= 200 lignes.

CRITERES_D_ACCEPTATION:
- Aucune image `latest` en prod.
- Aucun mot de passe par defaut dans compose.
- Stack demarre avec options de hardening actives.

### Ticket 8 (P2 - gouvernance scan CVE backend)
ID: JIRA-SEC-008
TITRE: Fiabiliser le scan OWASP Dependency Check backend en CI
TYPE: Task
PRIORITE: P2
STATUT: TODO
ESTIMATE: 2-4h
ASSIGNE: ->
ASSIGNE_LE: ->
DERNIERE_MAJ: 2026-02-27
DEPENDENCIES: JIRA-TECH-017

OBJECTIF:
Garantir un reporting CVE backend complet et a jour.

DESCRIPTION:
- Configurer cle API NVD en CI.
- Produire artefacts JSON/XML a chaque pipeline.
- Echouer pipeline sur seuil critique configurable.

CRITERES_D_ACCEPTATION:
- Le scan backend termine sans erreur NVD.
- Rapport CVE archive en artefact CI.
- Seuil de blocage documente et applique.
