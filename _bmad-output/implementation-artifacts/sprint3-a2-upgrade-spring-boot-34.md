# Story Sprint3-A2: Upgrade Spring Boot 3.3.0 → 3.4.x

Status: ready-for-dev

## Story

As a developer,
I want mettre à jour Spring Boot de 3.3.0 (EOL juin 2025) vers 3.4.x,
so that le projet bénéficie des correctifs de sécurité et reste maintenu.

## Acceptance Criteria

1. **Given** la version actuelle est 3.3.0, **When** l'upgrade est appliqué, **Then** `pom.xml` déclare Spring Boot 3.4.x (dernière version stable de la ligne 3.4).

2. **Given** l'upgrade Spring Boot, **When** `mvn verify` est exécuté, **Then** tous les tests passent (hors pre-existing failures connues, inchangées).

3. **Given** les breaking changes 3.3→3.4, **When** ils existent, **Then** le code est adapté en conséquence (migration properties, API changes, etc.).

4. **Given** le build Maven, **When** `mvn dependency:tree` est exécuté, **Then** aucune dépendance en conflit de version n'est signalée.

5. **Given** l'application démarre, **When** elle est lancée en local, **Then** le healthcheck `/actuator/health` répond `{"status":"UP"}`.

## Technical Context

### Version cible

Spring Boot **3.4.5** (dernière stable de la ligne 3.4 au 2026-03-04).

### Modification principale

```xml
<!-- pom.xml ligne 10 — AVANT -->
<version>3.3.0</version>

<!-- pom.xml ligne 10 — APRÈS -->
<version>3.4.5</version>
```

### Breaking changes connus 3.3→3.4

1. **`spring.jpa.open-in-view`** : désactivé par défaut dans 3.4 (était `true` en 3.3). Si l'application charge des relations lazy hors transaction → `LazyInitializationException`. Fix : ajouter `spring.jpa.open-in-view=false` dans application.properties (ou `=true` pour restaurer, mais non recommandé).

2. **Actuator** : `/actuator/health` peut changer de format — vérifier.

3. **Hibernate 6.5→6.6** : migration mineure, compatible.

4. **`spring.security.filter.order`** : changement possible dans l'ordre des filtres. Vérifier que `RateLimitingFilter` (si implémenté en parallèle) fonctionne.

5. **Deprecations** : vérifier les warnings à la compilation.

### Procédure

1. Modifier `pom.xml` : `3.3.0` → `3.4.5`
2. Exécuter `mvn spotless:apply -q -B && mvn dependency:tree -B --no-transfer-progress` → vérifier conflits
3. Exécuter `mvn verify -B --no-transfer-progress` → noter les nouvelles failures (vs pre-existing)
4. Corriger les nouvelles failures
5. Vérifier `mvn spring-boot:run` (ou `java -jar target/*.jar`) → healthcheck OK

### Pre-existing failures connues (NE PAS corriger)

Ces tests étaient déjà en échec avant l'upgrade et ne doivent PAS être imputés à l'upgrade :
- `GameDataIntegrationTest` (4 failures — données)
- `FortniteTrackerServiceTddTest` (6 failures — messages FR attendus)
- `PlayerServiceTddTest` (1)
- `ScoreCalculationServiceTddTest` (2)
- `ScoreCalculationServiceTest` (2)
- `ScoreServiceTddTest` (3)
- `GameStatisticsServiceTddTest` (1 NPE)
Total connu : ~15-20 failures pre-existantes

### Fichiers à modifier

1. `pom.xml` — version Spring Boot uniquement (ligne `<version>3.3.0</version>` dans `<parent>`)
2. `src/main/resources/application.properties` — si adaptation nécessaire (open-in-view, etc.)
3. Code source si breaking changes détectés

### Règles qualité

- Pas de régression par rapport au baseline pre-existant
- Si une nouvelle failure apparaît → la corriger AVANT de marquer done
- `mvn spotless:apply` obligatoire avant `mvn test`
- Documenter les adaptations effectuées dans sprint-status.yaml

## Definition of Done

- [ ] `pom.xml` : Spring Boot 3.4.5
- [ ] `mvn verify` : pas de nouvelles failures (vs baseline pre-existant)
- [ ] Healthcheck `/actuator/health` : `{"status":"UP"}`
- [ ] Adaptations breaking changes documentées
- [ ] sprint-status.yaml mis à jour : `sprint3-a2-upgrade-spring-boot-34: done`
