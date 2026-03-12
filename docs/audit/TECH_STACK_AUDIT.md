# Audit Stack Technique — TECH-020

**Date** : 2026-03-02
**Périmètre** : Backend (Spring Boot / Java 21 / Maven) + Frontend (Angular 20 / Node / npm) + BDD (PostgreSQL + Flyway) + CI/CD (.github/workflows)
**Auditeur** : Claude Code (claude-sonnet-4-6)

---

## Résumé exécutif

| Indicateur | Valeur |
|---|---|
| Score stack global | **6 / 10** |
| Risques critiques (P0) | **3** |
| Risques majeurs (P1) | **5** |
| Risques mineurs (P2) | **8** |
| Dépendances EOL identifiées | **2** (Spring Boot 3.3.x, Flyway 10.x bientôt) |
| Migrations Flyway | V1 → V41 **(34 fichiers, 8 gaps de numérotation)** |
| Pipelines CI/CD | **1 seul** (SonarQube uniquement — pas de build/test/deploy) |
| Secrets hardcodés | **1** (JWT_SECRET avec valeur de fallback en clair dans `application.yml`) |

---

## 1. Stack actuelle

### 1.1 Backend

| Composant | Version actuelle | Version stable (mars 2026) | Statut | EOL |
|---|---|---|---|---|
| **Java** | 21 (LTS) | 21.0.6 (LTS) | OK | Sept. 2031 |
| **Spring Boot** | **3.3.0** | 3.4.4 / 4.0.2 | **CRITIQUE** | 3.3.x EOL juin 2025 |
| **Spring Framework** | 6.1.x (géré) | 6.2.x | MAJEUR | via Boot |
| **Spring Security** | 6.3.0 (géré) | 6.4.x | MAJEUR | via Boot |
| **Hibernate ORM** | 6.5.x (géré) | 6.6.x | OK | via Boot |
| **PostgreSQL Driver** | 42.7.3 | 42.7.5 | MINEUR | Actif |
| **Flyway Core** | 10.11.0 | 11.4.0 | MAJEUR | 10.x fin 2025 estimé |
| **JJWT** | 0.12.5 | 0.12.6 | MINEUR | Actif |
| **Lombok** | 1.18.32 | 1.18.36 | MINEUR | Actif |
| **OpenCSV** | 5.11.1 | 5.12.0 | OK | Actif |
| **H2 Database** | 2.2.224 (géré) | 2.4.240 | MINEUR | Test uniquement |
| **ArchUnit** | 1.2.1 | 1.4.1 | MINEUR | Actif |
| **SpringDoc OpenAPI** | 2.5.0 | 3.0.1 | MINEUR | 2.x fin support 2025 |
| **Logstash Logback Encoder** | 7.4 | 9.0 | MINEUR | 7.x déprécié |
| **Micrometer** | 1.13.x (géré) | 1.14.x | MINEUR | via Boot |
| **Tomcat** | 10.1.x (géré) | 10.1.x | OK | via Boot |

#### Maven Plugins

| Plugin | Version actuelle | Version stable | Statut |
|---|---|---|---|
| `spring-boot-maven-plugin` | 3.3.0 (géré) | 3.4.4 | MAJEUR |
| `maven-compiler-plugin` | 3.13.0 | 3.14.0 | OK |
| `spotless-maven-plugin` | 2.43.0 | 3.2.1 | MINEUR |
| `spotbugs-maven-plugin` | 4.8.3.1 | 4.9.3 | MINEUR |
| `jacoco-maven-plugin` | 0.8.11 | 0.8.14 | MINEUR |
| `dependency-check-maven` | 9.0.9 | 12.1.0 | MAJEUR |
| `maven-surefire-plugin` | 3.2.5 | 3.5.3 | MINEUR |
| `springdoc-openapi-maven-plugin` | 1.4 | 2.0.0 | MINEUR |

### 1.2 Frontend

| Composant | Version actuelle | Version stable (mars 2026) | Statut | Notes |
|---|---|---|---|---|
| **Node.js** | 20.x (CI) | 22.14 (LTS) | MINEUR | Node 20 EOL avr. 2026 |
| **TypeScript** | ~5.8.2 | 5.9.3 | OK | Compatible Angular 20 |
| **Angular Core** | ^20.3.17 | 20.3.17 / 21.2.x | OK | Patché depuis JIRA-SEC-003 |
| **Angular CLI** | ^20.3.18 | 20.3.18 | OK | |
| **Angular CDK** | ^20.2.14 | 20.2.14 | OK | |
| **Angular Material** | ^20.2.14 | 20.2.14 | OK | |
| **Angular SSR** | ^20.3.18 | 20.3.18 | OK | Patché |
| **RxJS** | ~7.8.0 | 7.8.2 | OK | |
| **zone.js** | ~0.15.0 | 0.15.0 | OK | |
| **@stomp/stompjs** | ^7.3.0 | 7.3.0 | OK | |
| **sockjs-client** | ^1.6.1 | 1.6.1 | OK | |
| **chart.js** | ^4.5.1 | 4.5.1 | OK | |
| **ng2-charts** | ^8.0.0 | 8.0.0 | OK | |
| **express** | ^5.2.1 | 5.2.1 | OK | SSR server |
| **@analogjs/vite-plugin-angular** | ^1.17.1 | 2.2.3 | MINEUR | |
| **karma** | ~6.4.0 | 6.4.4 | OK | |
| **jasmine-core** | ~5.7.0 | 5.7.1 / 6.x | OK | |

---

## 2. Risques identifiés

### CRITIQUE — P0

#### P0-1 : Spring Boot 3.3.0 en EOL depuis juin 2025

**Composants concernés** : `spring-boot-starter-parent 3.3.0` et toutes ses dépendances gérées (Spring Security 6.3.0, Tomcat 10.1.x, Jackson 2.17.1, Micrometer 1.13.x).

Spring Boot 3.3.x a atteint sa fin de vie en **juin 2025**. La dernière version corrective de la branche 3.3.x est la **3.3.13**. Aucun patch de sécurité n'est désormais publié pour 3.3.0. Le projet embarque Spring Security 6.3.0, qui a depuis reçu des correctifs de sécurité dans 6.3.x et 6.4.x.

**Impact** : Vulnérabilités de sécurité non corrigées dans le runtime, absence de support officiel.
**Remédiation** : Mettre à jour vers **Spring Boot 3.3.13** (correctif mineur, risque faible, 0 breaking change). Planifier ensuite la migration vers **3.4.x** (LTS en cours, support jusqu'à fin 2026).

---

#### P0-2 : Absence de pipeline CI/CD de build, test et déploiement

Le seul workflow GitHub Actions présent est `.github/workflows/sonar.yml`, qui exécute uniquement l'analyse statique SonarQube. Il n'existe **aucun pipeline** pour :
- Compiler et tester le backend Maven (`mvn test`)
- Exécuter les tests frontend Karma (`npm run test:ci`)
- Construire les artefacts de production (`mvn package`, `ng build`)
- Déployer en environnement de staging ou de production

**Impact critique** : Toute régression peut être mergée sur `main` sans être détectée par le CI. La qualité gate SonarQube passe avec `-DskipTests`, les tests ne sont jamais exécutés en CI.

**Remédiation** : Créer un workflow `ci.yml` avec les étapes : checkout → Java 21 setup → `mvn spotless:apply && mvn test` → Node 20 setup → `npm ci && npm run test:ci` → build (optionnel). Ajouter protection de branche (`main` : require PR + CI green).

---

#### P0-3 : Secret JWT avec valeur de fallback hardcodée

Dans `src/main/resources/application.yml` ligne 3 :

```yaml
secret: ${JWT_SECRET:development-jwt-secret-key-change-in-production-this-is-only-for-local-dev-work-32chars-minimum}
```

La syntaxe `${VAR:valeur_par_defaut}` de Spring signifie que si `JWT_SECRET` n'est pas défini en environnement, la clé littérale est utilisée pour signer tous les JWT. Si un environnement autre que dev démarre sans `JWT_SECRET`, l'application tourne avec une clé publiquement connue dans le dépôt.

**Impact** : Forgery de JWT possible avec la clé publique, élévation de privilèges potentielle.
**Note positive** : `application-prod.yml` utilise correctement `${JWT_SECRET}` sans fallback — le risque est limité aux environnements non-prod mal configurés, mais le secret ne devrait jamais être écrit dans un fichier commité.
**Remédiation** : Supprimer la valeur de fallback. Utiliser `${JWT_SECRET}` sans défaut dans `application.yml`. Documenter l'obligation de définir cette variable dans `README` ou `docs/`.

---

### MAJEUR — P1

#### P1-1 : Node.js 20 EOL en avril 2026

Le pipeline CI utilise Node 20 (`node-version: "20"`). Node.js 20 (LTS) atteint sa fin de vie en **avril 2026** — dans moins de 2 mois. Le frontend utilise des packages (`@types/node: ^20.19.35`) typés pour Node 20.

**Remédiation** : Migrer vers **Node.js 22 LTS** (support jusqu'à avril 2027). Mettre à jour la CI et vérifier la compatibilité des packages.

---

#### P1-2 : Flyway 10.11.0 — version majeure dépassée

Flyway 11.x est disponible depuis novembre 2024 et constitue désormais la branche stable courante. La version 10.x reste supportée mais la cadence de patches se réduit.

**Impact** : Pas de correctifs de sécurité garantis à terme, fonctionnalités manquantes (nouvelles bases, améliorations de performance des verrous).
**Remédiation** : Migrer vers **Flyway 11.4.0**. Attention : migration de majeure, vérifier le guide de migration Flyway 10 → 11 (changements de configuration YAML possible).

---

#### P1-3 : Gaps de numérotation dans les migrations Flyway

La séquence des migrations est incomplète et contient des versions décimales non-standard :

- **Versions manquantes** : V2, V3, V5, V7, V8, V10, V11, V15 (8 gaps)
- **Versions décimales** : V9_1, V10_1 (nommage non standard `_` au lieu de `.`)

Flyway tolère les gaps mais leur présence indique que des migrations ont pu être supprimées après avoir été appliquées en base, ce qui constitue une violation du principe d'immutabilité des migrations.

**Risque** : Incohérence entre les schémas de différents environnements (dev/staging/prod). Impossibilité de recréer un environnement propre depuis zéro sans erreurs.
**Remédiation** : Audit des migrations manquantes (existaient-elles ? ont-elles été mergées dans V1 ?). Documenter les gaps dans un fichier `MIGRATION_GAPS.md`. Standardiser la numérotation future en entiers séquentiels.

---

#### P1-4 : `management.endpoint.health.show-details: always` en production

Dans `application-prod.yml` ligne 100 :
```yaml
endpoint:
  health:
    show-details: always
```

Cela expose au endpoint public `/actuator/health` : le nombre de connexions du pool HikariCP, le statut de la base de données, l'espace disque, les détails Zipkin, etc.

**Impact** : Fuite d'informations sur l'infrastructure (nom du pool "FortnitePronosCP", statut Redis, etc.) utilisable pour cibler des attaques.
**Remédiation** : Passer `show-details: when_authorized` en production, protégé par un rôle Spring Security `ACTUATOR`.

---

#### P1-5 : PostgreSQL driver 42.7.3 — 2 versions de patch en retard

Le driver JDBC PostgreSQL 42.7.3 est dépassé par la version **42.7.5** publiée en 2025, qui corrige des bugs de connexion SSL et des comportements sur les types JSONB.

**Remédiation** : Mise à jour vers `42.7.5` dans `pom.xml`. Risque : faible (patch mineur).

---

### MINEUR — P2

#### P2-1 : CORS trop permissif — `allowed-headers: "*"`

Dans `application.yml` :
```yaml
cors:
  allowed-headers: "*"
```

Accepter tous les headers entrants peut faciliter certaines attaques CSRF/injection de headers. En production, la liste devrait être explicite.
**Remédiation** : Lister explicitement : `Authorization, Content-Type, X-Requested-With, Accept, Origin`.

---

#### P2-2 : H2 Console activée dans le profil `h2`

Dans `application-h2.yml` ligne 15 :
```yaml
h2:
  console:
    enabled: true
    path: /h2-console
```

Si ce profil est activé accidentellement hors environnement de test, la console H2 est accessible sans authentification Spring Security par défaut.
**Remédiation** : S'assurer que la configuration Spring Security bloque `/h2-console` sur tous les profils non-test. Ajouter une annotation `@Profile("h2")` sur le bean de configuration H2 si applicable.

---

#### P2-3 : `spring.security.user.name: admin / password: admin` dans `application-dev.yml`

Le profil `dev` définit un utilisateur Spring Security par défaut avec des credentials triviaux. Si `dev` est utilisé comme base pour un déploiement accessible en réseau, ce compte est fonctionnel.
**Remédiation** : Supprimer ce bloc ou ajouter un commentaire explicite et une vérification que le profil `dev` n'est jamais actif en environnement exposé.

---

#### P2-4 : `dependency-check-maven` 9.0.9 — version très ancienne

Le plugin OWASP Dependency Check est en version 9.0.9 alors que la version **12.1.0** est disponible. Les versions intermédiaires ont amélioré la base NVD locale, la gestion des faux positifs et les performances d'analyse. La configuration actuelle (`failBuildOnCVSS: 7`) est bonne mais le plugin sous-jacent peut manquer des CVE récentes.
**Remédiation** : Mettre à jour vers `12.1.0`. Vérifier que `suppressionFile: owasp-suppression.xml` est à jour.

---

#### P2-5 : SpringDoc OpenAPI 2.5.0 — proche de fin de support

SpringDoc 2.x cible Spring Boot 3.x. La version 3.x (en béta publique) ciblera Spring Boot 4.x. La version 2.5.0 est en retard de plusieurs mises à jour correctives (2.8.x est la branche courante en mars 2026).
**Remédiation** : Mettre à jour vers `2.8.x` (compatible Spring Boot 3.3.x).

---

#### P2-6 : `logstash-logback-encoder` 7.4 — 2 majeures de retard

La version 9.0 est disponible depuis mi-2025. La version 7.4 ne reçoit plus de patches.
**Remédiation** : Mise à jour vers `9.x`. Vérifier les changements de configuration JSON de l'encoder dans `logback.xml` si existant.

---

#### P2-7 : Environnement prod — `apiUrl: localhost:8081`

Dans `frontend/src/environments/environment.prod.ts` :
```typescript
apiUrl: 'http://localhost:8081',
apiBaseUrl: 'http://localhost:8081',
wsUrl: 'ws://localhost:8081/ws',
```

L'URL de production pointe vers `localhost`. Ceci indique que la configuration de production n'est pas complète — soit l'URL réelle est injectée via Angular build `--configuration` et un fichier de remplacement, soit ce fichier n'a jamais été mis à jour pour un déploiement réel.
**Remédiation** : Documenter la stratégie de substitution Angular build (fileReplacements dans `angular.json`) ou mettre les vraies URLs.

---

#### P2-8 : Tracing Zipkin activé en prod sans endpoint sécurisé

Dans `application-prod.yml` :
```yaml
management:
  tracing:
    enabled: true
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
```

Le tracing est activé avec un endpoint Zipkin par défaut en HTTP non chiffré (`http://`). Si `ZIPKIN_ENDPOINT` n'est pas défini, les traces partent vers localhost (inerte), mais si un Zipkin est accessible en réseau sans TLS, les traces de requêtes (incluant potentiellement des données sensibles) transitent en clair.
**Remédiation** : S'assurer que `ZIPKIN_ENDPOINT` est défini avec une URL HTTPS. Désactiver le tracing si Zipkin n'est pas déployé (`enabled: false` par défaut).

---

## 3. Configuration & Sécurité

### Profils Spring actifs

| Profil | Fichier | Usage |
|---|---|---|
| `dev` (défaut) | `application-dev.yml` | Développement local — PostgreSQL local |
| `h2` | `application-h2.yml` | Tests d'intégration — H2 in-memory |
| `prod` | `application-prod.yml` | Production |

**Observation** : `application.yml` définit `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`, ce qui signifie que si la variable d'environnement n'est pas définie, le profil `dev` est actif par défaut — comportement correct pour le développement local.

### Variables d'environnement requises en production

| Variable | Obligatoire | Défaut si absente | Risque |
|---|---|---|---|
| `JWT_SECRET` | Oui (`application-prod.yml`) | Aucun (fail-fast) | Correct en prod |
| `JWT_SECRET` | Oui | **Valeur hardcodée** (`application.yml`) | CRITIQUE non-prod |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Oui | `localhost:5432/fortnite_pronos` | Acceptable dev |
| `DB_USERNAME`, `DB_PASSWORD` | Oui (prod) | `fortnite_user / fortnite_pass` (dev) | Risque si mal configuré |
| `FORTNITE_API_KEY` | Non | Vide | Feature désactivée |
| `ZIPKIN_ENDPOINT` | Non | `http://localhost:9411` | Voir P2-8 |
| `SUPABASE_URL`, `SUPABASE_ANON_KEY` | Non | Vide | Feature optionnelle |
| `SPRING_PROFILES_ACTIVE` | Non | `dev` | Acceptable |

### Bilan sécurité configuration

| Point de contrôle | Statut |
|---|---|
| Secrets non commités en prod | OK (`application-prod.yml` : pas de fallback) |
| Secret JWT avec fallback dans base | NON OK (voir P0-3) |
| H2 console désactivée en prod | OK |
| CORS restreint aux origines | OK (localhost uniquement) |
| CORS headers non restrictifs | NON OK (voir P2-1) |
| `ddl-auto: validate` en prod/dev | OK (pas de `create-drop` hors H2) |
| Actuator endpoints limités en prod | Partiel (health details trop verbose) |
| RLS PostgreSQL activée | OK (V24 — toutes les tables) |
| Vue `users_public` sans password | OK (V25) |
| `show-sql: false` en prod | OK |

---

## 4. Base de données

### Moteur

| Paramètre | Valeur |
|---|---|
| SGBD | **PostgreSQL** (version non spécifiée dans le code — runtime) |
| ORM | Hibernate 6.5.x (via Spring Boot 3.3.0) |
| Migrations | **Flyway 10.11.0** |
| BDD de test | H2 2.2.224 (mode PostgreSQL) |
| UUID | `uuid-ossp` extension |

### Migrations Flyway

**Total : 34 fichiers de migration (V1 → V41)**

| Groupe | Migrations | Description |
|---|---|---|
| Schéma initial | V1 | Tables core (users, players, teams, scores, trades) + 10 indexes |
| Draft & Games | V4, V6, V9_1, V10_1 | Draft tables, games tables, alignement schéma |
| Performance | V12, V13, V14, V16 | +30 indexes de performance (composite, partial, covering) |
| Domaine métier | V17-V23 | Trading system, ingestion PR, régions, normalisation |
| Sécurité | V24-V28 | RLS toutes tables, vue users_public, suppression doublons, SECURITY DEFINER |
| Fonctionnalités Sprint 3-4 | V29-V41 | Player aliases, soft delete, draft async, snapshots, pipeline identité, metadata |

**Gaps identifiés dans la numérotation** :

| Versions manquantes | Hypothèse |
|---|---|
| V2, V3 | Fusionnées dans V1 lors d'une réécriture du schéma initial |
| V5 | Supprimée (migration intermédiaire abandonnée) |
| V7, V8 | Idem |
| V10 | Remplacée par V10_1 (nommage non standard) |
| V11 | Supprimée |
| V15 | Supprimée (entre performance V14 et V16) |

**Recommandation** : Créer un fichier `docs/MIGRATION_HISTORY.md` documentant les raisons des gaps pour éviter la confusion lors de l'onboarding.

### Index présents

La stratégie d'indexation est mature et bien travaillée :
- Indexes simples sur FK : OK (V1, V14, V25)
- Indexes composites critiques : OK (V16 — `idx_scores_player_season_points`, `idx_players_region_tranche`)
- Indexes partiels (`WHERE until IS NULL`, `WHERE season >= 2025`) : OK — bonne pratique
- Indexes fonctionnels (`LOWER(nickname)`, `LOWER(username)`) : OK — recherche case-insensitive
- Index couvrant (`idx_players_covering`) : OK — évite les heap fetches

**Amélioration possible** : Pas d'index sur `games.deleted_at` (soft delete ajouté en V30). Les requêtes filtrant `WHERE deleted_at IS NULL` bénéficieraient d'un index partiel.

### Row Level Security (RLS)

RLS activée sur toutes les tables en V24. Les politiques sont correctement configurées :
- Lecture publique sur les tables non-sensibles (players, scores, games, teams)
- Lecture filtrée par `auth.uid()` sur `users` et `notifications`
- Accès bloqué (`USING (false)`) sur `scrape_runs` et `ingestion_runs`

**Observation** : `auth.uid()` retourne `NULL::uuid` dans la fonction stub créée pour les environnements non-Supabase (V24). Cela signifie que les politiques `auth.uid() = user_id` ne correspondent jamais via PostgREST, mais le backend Java bypasse RLS via le rôle `service_role`. Comportement correct mais à documenter.

### Pool de connexions HikariCP

| Paramètre | Dev | Prod |
|---|---|---|
| `maximum-pool-size` | 10 | 30 |
| `minimum-idle` | 2 | 10 |
| `connection-timeout` | 3s | 8s |
| `leak-detection-threshold` | 300s (5min!) | 15s |
| `max-lifetime` | 30min | 30min |

**Anomalie** : Le `leak-detection-threshold` en dev est de **5 minutes** (300 000ms), ce qui signifie qu'une fuite de connexion ne sera détectée qu'après 5 minutes — inutile comme garde-fou. En prod (15s) c'est correct.

---

## 5. CI/CD

### Pipelines configurés

| Workflow | Fichier | Trigger | Étapes |
|---|---|---|---|
| **SonarQube** | `sonar.yml` | push/main, PR, manual | validate-secrets → sonar backend (skip tests) → sonar frontend (avec tests Karma) |

### Ce qui est absent (risques majeurs)

| Pipeline manquant | Impact |
|---|---|
| **Build & Test backend** (`mvn test`) | Aucune exécution automatique des 2000+ tests JUnit |
| **Build & Test frontend** (`npm run test:ci`) | Tests Karma exécutés uniquement dans Sonar |
| **Build de production** (`mvn package`, `ng build`) | Pas de vérification que le build compile |
| **Déploiement staging** | Pas de CD |
| **Déploiement production** | Pas de CD |
| **Vérification Spotless** | `mvn spotless:check` non exécuté en CI |
| **OWASP Dependency Check** | Plugin configuré mais jamais exécuté en CI |
| **SpotBugs** | Configuré dans pom.xml mais jamais exécuté en CI |

### Analyse du pipeline SonarQube existant

**Points positifs** :
- Protection des secrets via `${{ secrets.SONAR_HOST_URL }}` et `${{ secrets.SONAR_TOKEN }}` : correct
- `fetch-depth: 0` pour l'analyse des blame : correct
- `qualitygate.wait: true` : le pipeline attend le résultat de la quality gate
- `cancel-in-progress: true` : évite les analyses dupliquées
- Tests frontend avec coverage exécutés dans le job Sonar

**Points négatifs** :
- Backend Sonar avec `-DskipTests` : les tests ne sont jamais exécutés, Sonar n'a pas de rapport de couverture JaCoCo
- Node version 20 dans la CI — à migrer vers 22 (voir P1-1)
- Pas de cache Maven local dans le job backend (uniquement `cache: maven` dans setup-java — à vérifier si effectif)

### Quality Gates

Sonar est configuré mais sans vérifier les gates de couverture JaCoCo (les tests sont skippés). Les gates définies dans `pom.xml` (80% instructions, 75% branches) ne sont jamais vérifiées en CI.

---

## 6. Outillage

| Outil | Version | Rôle | Statut |
|---|---|---|---|
| **Spotless** | 2.43.0 (plugin) + Google Java Format 1.19.1 | Formatage Java | Actif (phase compile) — version plugin périmée |
| **SpotBugs** | 4.8.3.1 | Analyse statique bugs | Configuré mais non exécuté en CI |
| **JaCoCo** | 0.8.11 | Couverture de code | Actif (phase test) — seuils 80%/75% |
| **OWASP Dependency Check** | 9.0.9 | Vulnérabilités CVE | Configuré mais non exécuté en CI — version très ancienne |
| **ArchUnit** | 1.2.1 | Tests d'architecture | Actif (inclus dans Surefire) |
| **SonarQube** | Scanner 5.5.0 | Qualité de code | Actif en CI |
| **Karma** | 6.4.0 | Tests frontend | Actif (ChromeHeadless) |
| **Jasmine** | 5.7.0 | Framework test frontend | Actif |
| **Logstash Logback** | 7.4 | JSON logging | Actif — version périmée |

---

## 7. Cohérence de la stack

### Compatibilité vérifiée

| Paire | Compatibilité |
|---|---|
| Java 21 + Spring Boot 3.3.0 | OK |
| Spring Boot 3.3.0 + Hibernate 6.5.x | OK |
| Spring Boot 3.3.0 + Flyway 10.11.0 | OK |
| Spring Boot 3.3.0 + SpringDoc 2.5.0 | OK |
| Angular 20.3.x + TypeScript 5.8.x | OK |
| Angular 20.3.x + RxJS 7.8.x | OK |
| Angular 20.3.x + zone.js 0.15.x | OK |
| @stomp/stompjs 7.x + sockjs-client 1.6.x | OK |
| chart.js 4.x + ng2-charts 8.x | OK |

### Incohérence identifiée

| Incohérence | Détail |
|---|---|
| `@angular/cdk 20.2.14` vs `@angular/core 20.3.17` | Version mineure différente — acceptable (Angular garantit la compatibilité dans la même majeure) mais idéalement alignées sur 20.3.x |
| H2 `MODE=PostgreSQL` | H2 n'émule pas parfaitement PostgreSQL — risque de divergence de comportement Hibernate sur les types UUID, les enums, et les index partiels |
| Redis dépendance en pom.xml | `spring-boot-starter-data-redis` inclus mais `spring.cache.type: caffeine` par défaut — Redis non utilisé en dev/test. Risque de confusion et de dépendances inutiles si Redis n'est pas disponible en prod |

---

## 8. Recommandations priorisées

### P0 — Immédiat (semaine 1)

| # | Action | Effort | Impact |
|---|---|---|---|
| 1 | Supprimer le fallback du JWT_SECRET dans `application.yml` | 5 min | Elimine P0-3 |
| 2 | Créer le pipeline CI `ci.yml` (build + test backend + frontend) | 2h | Elimine P0-2, détecte régressions |
| 3 | Protéger la branche `main` (require CI green + PR review) | 15 min | Sécurise le processus |

### P1 — Court terme (semaine 2-3)

| # | Action | Effort | Impact |
|---|---|---|---|
| 4 | Upgrader Spring Boot `3.3.0 → 3.3.13` dans `pom.xml` | 1h | Elimine P0-1, patches sécurité |
| 5 | Upgrader PostgreSQL driver `42.7.3 → 42.7.5` | 15 min | Elimine P1-5 |
| 6 | Migrer CI vers Node.js 22 (`node-version: "22"`) | 15 min | Elimine P1-1 avant EOL avr. 2026 |
| 7 | Passer `show-details: when_authorized` en prod | 15 min | Elimine P1-4 |
| 8 | Ajouter index partiel Flyway sur `games.deleted_at` | 30 min | Performance soft-delete |

### P2 — Moyen terme (sprint suivant)

| # | Action | Effort | Impact |
|---|---|---|---|
| 9 | Upgrader Flyway `10.11.0 → 11.4.0` | 2h | Elimine P1-2 |
| 10 | Upgrader `dependency-check-maven` `9.0.9 → 12.1.0` | 30 min | Elimine P2-4 |
| 11 | Restreindre `allowed-headers` CORS | 15 min | Elimine P2-1 |
| 12 | Mettre à jour SpringDoc `2.5.0 → 2.8.x` | 30 min | Elimine P2-5 |
| 13 | Mettre à jour `logstash-logback-encoder` `7.4 → 9.x` | 1h | Elimine P2-6 |
| 14 | Intégrer SpotBugs et OWASP Dependency Check dans le CI | 1h | Active les quality gates existantes |
| 15 | Documenter les gaps de migration Flyway | 1h | Elimine P1-3 (documentation) |

### P3 — Long terme (planification sprint dédié)

| # | Action | Notes |
|---|---|---|
| 16 | Migration Spring Boot `3.3.x → 3.4.x` | Breaking changes potentiels, sprint dédié |
| 17 | Migration Angular `20.x → 21.x` | Sprint dédié après stabilisation 21.x |
| 18 | Évaluer remplacement H2 (tests) par `testcontainers-postgresql` | Éliminerait les divergences de comportement H2/PostgreSQL |
| 19 | Évaluer suppression de `spring-boot-starter-data-redis` si Redis non déployé | Réduit les dépendances inutiles |
| 20 | Configurer URL production réelle dans `environment.prod.ts` | Nécessite stratégie de déploiement définie |

---

## Annexe — Inventory complet des fichiers analysés

| Fichier | Description |
|---|---|
| `pom.xml` | 29 dépendances directes, 8 plugins Maven |
| `frontend/package.json` | 14 dépendances prod, 11 devDependencies |
| `src/main/resources/application.yml` | Configuration principale (tous profils) |
| `src/main/resources/application-dev.yml` | Profil développement |
| `src/main/resources/application-prod.yml` | Profil production |
| `src/main/resources/application-h2.yml` | Profil test H2 |
| `src/main/resources/application.properties` | Propriétés de base (legacy) |
| `frontend/src/environments/environment.ts` | Config dev Angular |
| `frontend/src/environments/environment.prod.ts` | Config prod Angular |
| `frontend/src/environments/environment.cdn.ts` | Config CDN Angular |
| `src/main/resources/db/migration/V1__clean_schema.sql` → `V41__...sql` | 34 fichiers de migration |
| `.github/workflows/sonar.yml` | Unique pipeline CI/CD |
