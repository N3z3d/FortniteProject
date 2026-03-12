# JIRA-TECH-023 — Synthèse Technique des Audits TECH-019 à TECH-022

**Date :** 2026-03-02
**Auditeur :** Claude Sonnet 4.6
**Sources :** TECH-019 (DRY), TECH-020 (Stack), TECH-021 (Conventions), TECH-022 (Demeter)

---

## 1. Résumé exécutif

- **Score santé global estimé : 5.6 / 10** (moyenne pondérée des 4 scores : DRY 4.5, Stack 6.0, Conventions 6.5, Demeter 5.5)
- **Le runtime Spring Boot 3.3.0 est en EOL depuis juin 2025** — aucun patch de sécurité n'est publié pour cette branche ; Spring Security 6.3.0 embarqué n'est plus corrigé.
- **Aucun pipeline CI build+test n'existe** : les 2 100+ tests JUnit/Karma ne s'exécutent jamais automatiquement sur `main` — toute régression peut être mergée sans détection.
- **Un secret JWT possède une valeur de fallback hardcodée** dans `application.yml`, ouvrant une possibilité de forgery de token sur tout environnement non-prod mal configuré.
- **~1 400 lignes dupliquées** identifiées (17 patterns DRY), dont 210 lignes dans des boilerplates de test (`SecurityTestBeans` ×14) et 90 lignes de gestion d'erreur HTTP copiées dans 3 services Angular.
- **4 classes backend et 2 composants frontend dépassent la limite de 500 lignes**, dont `DraftController` à 657 lignes (+157) et `login.component.ts` à 602 lignes (+102).
- **Un stub non implémenté `isUserTurn()` retourne toujours `true`** dans `DraftService` : toute validation de tour de jeu est contournée en silence dans les drafts actifs.

---

## 2. Matrice de priorité — Findings P0/Critiques unifiés

| # | ID | Finding | Audit | Blast radius | Effort fix |
|---|---|---|---|---|---|
| 1 | P0-3 | Secret JWT avec fallback hardcodé dans `application.yml` | TECH-020 | Sécurité — tous env non-prod | 5 min |
| 2 | P0-2 | Absence totale de pipeline CI build+test (backend + frontend) | TECH-020 | Toute la base de code | 2h |
| 3 | C1 | `DraftService.isUserTurn()` retourne toujours `true` — stub non implémenté | TECH-021 | Logique métier draft | 2h |
| 4 | P0-1 | Spring Boot 3.3.0 en EOL (juin 2025) — Spring Security 6.3.0 non patché | TECH-020 | Runtime complet | 1h |
| 5 | DRY-006 | `SecurityTestBeans` inner class dupliquée dans 14 fichiers de test | TECH-019 | Maintenabilité CI/tests | 2h |
| 6 | DRY-003 | `buildAndRecordError` copié mot-pour-mot dans 2 `@ControllerAdvice` | TECH-019 | Gestion d'erreurs | 1h |
| 7 | DEM-001 | `DraftPickDto` : chaînes de 3 niveaux (`getParticipant().getUser().getId()`) | TECH-022 | Couche DTO draft | 1h |
| 8 | DEM-004 | `TeamDto` : logique métier (calcul score) dans une DTO, navigation 3 niveaux | TECH-022 | Service leaderboard | 2h |
| 9 | DRY-005 | `handleError`/`resolveErrorMessage` dupliqués dans 3 services Angular (~90 lignes) | TECH-019 | UX erreurs frontend | 4h |
| 10 | C2 | `DraftController` 657 lignes (seuil 500) | TECH-021 | Maintenabilité backend | 4h |
| 11 | DRY-004 | Interface `Player` TypeScript définie 7+ fois, champs incompatibles | TECH-019 | Frontend entier | 2j |
| 12 | P1-4 | `health.show-details: always` expose l'infrastructure en production | TECH-020 | Sécurité prod | 15 min |
| 13 | DEM-007/008 | `team.getOwner().getId()` répété dans 3 services leaderboard sans délégation | TECH-022 | Services leaderboard | 1h |
| 14 | DRY-007 | Pattern `Enum.valueOf(other.name())` : 12+ occurrences sans validation compile-time | TECH-019 | Mappers JPA | 3h |
| 15 | C4 | Corruption UTF-8 massive dans JavaDoc de `DraftController` et `DraftService` | TECH-021 | Lisibilité/débogage | 30 min |

---

## 3. Quick Wins — Corrections < 30 min, risque zéro

| Action | Fichier | Audit | Temps |
|---|---|---|---|
| Supprimer le fallback JWT hardcodé : `${JWT_SECRET:...}` → `${JWT_SECRET}` | `application.yml:3` | TECH-020 | 5 min |
| Passer `show-details: when_authorized` en prod | `application-prod.yml:100` | TECH-020 | 5 min |
| Supprimer les 3 blocs `if (hours < MIN || hours > MAX)` redondants (doublons de `@Min`/`@Max`) | `AdminDashboardController.java:73,109,125` | TECH-019 | 10 min |
| Réduire les 6 constantes `MIN_*_HOURS`/`MAX_*_HOURS` à 2 (`MIN_HOURS=1`, `MAX_HOURS=168`) | `AdminDashboardController.java:38-43` | TECH-019 | 10 min |
| Migrer CI vers Node.js 22 (`node-version: "22"`) avant EOL avril 2026 | `.github/workflows/sonar.yml` | TECH-020 | 5 min |
| Mettre à jour PostgreSQL driver `42.7.3 → 42.7.5` dans `pom.xml` | `pom.xml` | TECH-020 | 5 min |
| Corriger l'encodage UTF-8 de `DraftController.java` et `DraftService.java` (script Python) | `DraftController.java:34`, `DraftService.java:33` | TECH-021 | 15 min |
| Remplacer `response.getStatusCodeValue()` (déprécié Spring 6) par `.getStatusCode().value()` | `LeaderboardControllerTest.java:49` | TECH-021 | 5 min |
| Extraire `private getCurrentGames()` dans `user-games.store.ts` (éliminer 3 occurrences de `stateSubject.value.games`) | `user-games.store.ts:55,62,154` | TECH-022 | 15 min |
| Constante partagée `EMPTY_REGION_DISTRIBUTION` dans `core/constants/regions.constants.ts` | `dashboard.repository.ts:175`, `leaderboard.repository.ts:116` | TECH-019 | 10 min |
| Restreindre `allowed-headers: "*"` CORS à la liste explicite | `application.yml` | TECH-020 | 10 min |

---

## 4. Patterns systémiques — Violations transverses (multi-audits)

### 4.1 Le domaine `Draft` concentre 4 audits à la fois

`DraftService` (520 lignes), `DraftController` (657 lignes), `DraftPickDto` (violation Demeter DEM-001), `DraftDto` (DEM-002), `DraftDomainFacade` (DEM-003), et la corruption UTF-8 affectent le même périmètre fonctionnel. Ce cluster représente la zone de dette technique la plus dense du projet. S'y ajoute le stub `isUserTurn()` non implémenté (TECH-021 C1) qui constitue une faille de logique métier réelle.

### 4.2 Le modèle `Player` est fracturé sur les deux couches

- **Backend (DRY-012) :** `PlayerDto`, `CataloguePlayerDto`, `PlayerRecommendResponse`, `DraftAvailablePlayerResponse` mappent les mêmes champs `id/nickname/region/tranche` depuis des sources différentes (legacy `model.Player` vs domain `domain.player.model.Player`), symptôme d'une migration hexagonale incomplète.
- **Frontend (DRY-004) :** 7+ interfaces `Player` TypeScript aux champs incompatibles (`username` vs `name` vs `nickname`, présence/absence de `marketValue`, `isWorldChampion`, `tranche`). Les composants ne partagent pas de contrat commun.
- **Impact combiné :** tout refactoring de `Player` requiert une intervention dans 15+ fichiers sans filet de sécurité type-safe.

### 4.3 La gestion d'erreur viole DRY et Demeter simultanément

- `buildAndRecordError` est copié dans 2 handlers (DRY-003).
- `handleError`/`resolveErrorMessage` est copié dans 3 services Angular (DRY-005).
- `AdminAlertService` navigue à 2 niveaux dans les DTOs métriques pour extraire des scalaires (DEM-013).
- Le résultat est une gestion d'erreur fragmentée, incohérente (messages différents selon le service), et difficile à modifier globalement.

### 4.4 Les leaderboard services violent DRY et Demeter ensemble

`TeamLeaderboardService`, `PlayerLeaderboardService`, et `PronostiqueurLeaderboardService` partagent le pattern `team.getOwner().getId()` / `team.getOwner().getUsername()` sans délégation (DEM-007, DEM-008, DEM-009), et accèdent à des propriétés de Player via `teamPlayer.getPlayer().getId()` (DEM-008). Ces services font aussi partie des classes "à surveiller" en taille (potentiel DRY sur les méthodes de scoring). Une extraction de `TeamOwnerInfo` ou des accesseurs délégataires sur `Team` réglerait 6 violations Demeter en un seul changement.

### 4.5 L'absence de CI crée un risque multiplicateur

Sans pipeline build+test automatique (TECH-020 P0-2), aucune des régressions introduites par les violations ci-dessus (stub `isUserTurn`, divergences d'enum, mappings `Player` incorrects) ne serait détectée avant la mise en production. La quality gate SonarQube backend tourne avec `-DskipTests` — les seuils de couverture (80%/75%) ne sont jamais vérifiés en CI.

---

## 5. Propositions de tickets JIRA

| Ticket | Titre | Description |
|---|---|---|
| `JIRA-TECH-024` | Sécuriser le secret JWT et durcir la config prod | Supprimer le fallback hardcodé de `JWT_SECRET`, passer `health.show-details: when_authorized`, restreindre CORS headers — 3 correctifs de sécurité groupés < 30 min. |
| `JIRA-TECH-025` | Créer le pipeline CI build+test GitHub Actions | Ajouter `ci.yml` : checkout → Java 21 → `mvn spotless:apply && mvn test` → Node 22 → `npm ci && npm run test:ci` → protection de branche `main` (require CI green + PR review). |
| `JIRA-TECH-026` | Upgrader Spring Boot 3.3.0 → 3.3.13 (EOL juin 2025) | Mettre à jour `spring-boot-starter-parent` vers 3.3.13 pour recevoir les patches de sécurité Spring Security 6.3.x ; planifier ensuite la migration vers 3.4.x. |
| `JIRA-TECH-027` | Implémenter `isUserTurn()` et refactorer `DraftController` + `DraftService` | Corriger le stub `isUserTurn()` qui retourne toujours `true` (faille logique métier), décomposer `DraftController` (657 lignes) et `DraftService` (520 lignes) sous le seuil de 500 lignes. |
| `JIRA-TECH-028` | Extraire helpers partagés DRY : `MappingUtils`, `EntityReferenceFactory`, `SecurityTestBeansConfig` | Créer `MappingUtils` (safeInt/safeBool), `EntityReferenceFactory` (userRef/playerRef/gameRef), et `SecurityTestBeansConfig` partagée — élimine ~280 lignes dupliquées backend. |
| `JIRA-TECH-029` | Centraliser la gestion d'erreur HTTP Angular (`HttpErrorHandlerService`) | Extraire `resolveErrorMessage`/`handleError` depuis 3 services Angular vers un `HttpErrorHandlerService` dans `core/services/` ; remplacer les ~90 lignes dupliquées. |
| `JIRA-TECH-030` | Unifier les interfaces `Player` TypeScript en `CorePlayer` + extensions | Définir `CorePlayer` (id, nickname, region, tranche) dans `shared/models/player.model.ts`, créer `LeaderboardPlayer`, `DraftPlayer`, `TradePlayer` comme extensions, migrer les 7+ définitions actuelles. |

---

## 6. Registre des risques production

| # | Risque | Source | Probabilité | Impact | Mitigation urgente |
|---|---|---|---|---|---|
| R1 | **Forgery de JWT** : la clé `development-jwt-secret-key-...` est publique dans le dépôt git — tout environnement sans `JWT_SECRET` défini génère des tokens signés avec cette clé connue | TECH-020 P0-3 | Moyenne (dépend de la config env) | Critique — élévation de privilèges | Supprimer le fallback immédiatement (`${JWT_SECRET}` sans défaut) |
| R2 | **Contournement du tour de draft** : `DraftService.isUserTurn()` retourne `true` pour tout draft actif — n'importe quel participant peut jouer à la place d'un autre sans contrôle | TECH-021 C1 | Haute (code actif) | Haute — intégrité fonctionnelle du produit | Implémenter ou désactiver l'endpoint appelant jusqu'à correction |
| R3 | **Régression non détectée sur `main`** : absence de CI build+test ; les 2 100+ tests ne s'exécutent jamais automatiquement — une feature cassée peut être déployée | TECH-020 P0-2 | Haute (ongoing) | Haute — qualité et stabilité prod | Créer `ci.yml` + protection de branche en priorité absolue |
| R4 | **Fuite d'informations infrastructure** : `/actuator/health` expose en clair pool HikariCP, statut DB, espace disque à tout visiteur non authentifié en production | TECH-020 P1-4 | Haute (config active) | Moyenne — reconnaissance d'infrastructure | `show-details: when_authorized` (15 min de correction) |
| R5 | **Divergence silencieuse de mapping d'enum** : le pattern `TargetEnum.valueOf(source.name())` utilisé dans 12+ mappers sans garde compile-time — un renommage d'enum dans le domaine lève `IllegalArgumentException` au runtime sans avertissement à la compilation | TECH-019 DRY-007 | Faible (stabilité actuelle des enums) | Haute — crash runtime en production lors du prochain refactoring d'enum | Créer `EnumMapper` générique avec validation, ajouter test `EnumMappingConsistencyTest` |

---

*Rapport généré le 2026-03-02 — Consolidation de TECH-019, TECH-020, TECH-021, TECH-022*
*Périmètre couvert : 425 fichiers Java + 309 fichiers TypeScript + stack infra + CI/CD*
