# Audit Loi de Demeter — TECH-022
Date: 2026-03-02
Périmètre: Backend `src/main/java/com/fortnite/pronos/` + Frontend `frontend/src/app/`

---

## Résumé exécutif

- **Score Demeter : 5.5/10**
- **Violations HAUTE sévérité : 6** (chaînes > 2 niveaux dans logique métier et DTOs)
- **Violations MOYENNE sévérité : 11** (chaînes 2 niveaux répétées dans services/leaderboard/templates)
- **Violations BASSE sévérité : 6** (accès légitimes dans mappers/config ou sans impact métier fort)

La codebase présente une contamination systémique au niveau des couches DTO et leaderboard-service. Les violations HAUTES sont concentrées sur deux fichiers : `DraftPickDto.java` (3 niveaux) et `DraftDomainFacade.java`. Les leaderboard services violent massivement la LoD via `team.getOwner().getId()` / `team.getOwner().getUsername()` répété sans délégation. Le frontend présente des violations modérées dans les templates HTML (accès 3 niveaux sur `trade.playerOut.stats.kills`) et dans un composant Angular (`user-games.store.ts`).

---

## Violations HAUTES (refactor requis)

### DEM-001 — DraftPickDto : chaîne de 3 niveaux sur participant et sur game

- **Fichier :** `src/main/java/com/fortnite/pronos/dto/DraftPickDto.java:43-44,51`
- **Code actuel :**
  ```java
  .pickerId(draftPick.getParticipant().getUser().getId())
  .pickerName(draftPick.getParticipant().getUser().getUsername())
  // ...
  int participantCount = draftPick.getDraft().getGame().getParticipants().size();
  ```
- **Problème :** Deux chaînes de 3 niveaux dans la même méthode statique de factory. `DraftPickDto` connaît la structure interne de `GameParticipant` (qui contient un `User`), et également la structure de `Draft` (qui contient un `Game`, qui contient des `Participant`). La DTO dépend de 4 objets distincts au lieu d'un seul.
- **Correction suggérée :** Ajouter des méthodes délégataires sur `DraftPick` :
  `draftPick.getPickerUserId()`, `draftPick.getPickerUsername()`, `draftPick.getParticipantCount()`. Ces méthodes encapsulent la navigation interne. L'alternative plus propre est de passer des paramètres primitifs au builder plutôt qu'un objet entité complet.

---

### DEM-002 — DraftDto : chaîne 2 niveaux sur un objet imbriqué étranger

- **Fichier :** `src/main/java/com/fortnite/pronos/dto/DraftDto.java:39-40`
- **Code actuel :**
  ```java
  .gameId(draft.getGame().getId())
  .gameName(draft.getGame().getName())
  ```
- **Problème :** `DraftDto` est une DTO de la couche application qui connaît la structure interne de `Draft`, mais appelle directement `.getGame()` pour en extraire des propriétés. Un draft n'est pas supposé exposer son objet `Game` entier à une DTO — il devrait exposer `draft.getGameId()` et `draft.getGameName()` comme propriétés directes, ou la DTO doit recevoir ces valeurs via des paramètres séparés.
- **Correction suggérée :** Soit ajouter `getGameId()` / `getGameName()` sur `Draft` comme propriétés dénormalisées ou accesseurs délégataires, soit faire passer les valeurs en paramètres à `fromDraft(Draft draft, UUID gameId, String gameName)`.

---

### DEM-003 — DraftDomainFacade : navigation Game depuis Draft dans logique domaine

- **Fichier :** `src/main/java/com/fortnite/pronos/application/facade/DraftDomainFacade.java:168`
- **Code actuel :**
  ```java
  private int getParticipantCount(Draft draft) {
    if (draft.getGame() != null) {
      return draft.getGame().getTotalParticipantCount();
    }
    return 0;
  }
  ```
- **Problème :** Le facade de domaine accède à `Game` via `Draft.getGame()`, puis appelle `getTotalParticipantCount()` sur le `Game`. C'est une violation dans la couche domaine elle-même : le `Draft` devrait soit exposer directement le nombre de participants, soit la méthode devrait recevoir le `Game` en paramètre direct.
- **Correction suggérée :** Ajouter `draft.getParticipantCount()` qui délègue à `game.getTotalParticipantCount()` en interne, ou passer `participantCount` comme paramètre primitif à la méthode appelante.

---

### DEM-004 — TeamDto : navigation profonde tp.getPlayer().getScores().stream()

- **Fichier :** `src/main/java/com/fortnite/pronos/dto/team/TeamDto.java:47-52`
- **Code actuel :**
  ```java
  if (tp.getPlayer() == null || tp.getPlayer().getScores() == null) {
    return 0;
  }
  return tp.getPlayer().getScores().stream()
      .filter(s -> java.util.Objects.equals(s.getSeason(), team.getSeason()))
      .mapToInt(s -> s.getPoints())
      .sum();
  ```
- **Problème :** La DTO navigue depuis `TeamPlayer` → `Player` → `Scores` (3 niveaux) pour calculer un score agrégé. La DTO est consciente de la structure interne du graphe `TeamPlayer → Player → Score`. Ce calcul est une logique métier qui n'a pas sa place dans une DTO statique : il appartient au service ou à un mapper dédié.
- **Correction suggérée :** Extraire `calculatePlayerSeasonScore(TeamPlayer tp, int season)` dans un service/mapper dédié. La DTO `TeamDto` ne doit recevoir que des valeurs déjà calculées.

---

### DEM-005 — Template trade-detail : chaîne 3 niveaux `trade.playerOut.stats.kills`

- **Fichier :** `frontend/src/app/features/trades/trade-detail/trade-detail.component.html:69,73,77,101,105,109`
- **Code actuel :**
  ```html
  <span class="stat-value">{{ trade.playerOut.stats.kills }}</span>
  <span class="stat-value">{{ trade.playerOut.stats.wins }}</span>
  <span class="stat-value">{{ trade.playerOut.stats.kd }}</span>
  <span class="stat-value">{{ trade.playerIn.stats.kills }}</span>
  <span class="stat-value">{{ trade.playerIn.stats.wins }}</span>
  <span class="stat-value">{{ trade.playerIn.stats.kd }}</span>
  ```
- **Problème :** Le template accède à 3 niveaux de profondeur : `trade` → `playerOut`/`playerIn` → `stats` → propriété. Les templates Angular doivent rester superficiels. Si le modèle `Trade` évolue (ex: renommage de `stats`), tous ces accès cassent silencieusement. De plus, la logique de navigation est répétée 6 fois.
- **Correction suggérée :** Exposer des méthodes helpers dans le composant TypeScript : `getPlayerOutStats()` retournant un objet plat `{ kills, wins, kd }`, ou utiliser une interface `PlayerStats` directement flat dans le type `Trade`. Alternativement, un pipe `playerStats` ou une sous-composante `<app-player-stats [player]="trade.playerOut">` résout l'encapsulation.

---

### DEM-006 — PronostiqueurLeaderboardService : `userTeams.get(0).getOwner()`

- **Fichier :** `src/main/java/com/fortnite/pronos/service/leaderboard/PronostiqueurLeaderboardService.java:63`
- **Code actuel :**
  ```java
  com.fortnite.pronos.model.User user = userTeams.get(0).getOwner();
  ```
- **Problème :** Accès à un élément de liste externe suivi d'un appel de méthode sur le résultat. La liste est un paramètre reçu — le service suppose que la liste est non vide et navigue directement dans les internals de l'objet retourné. Fragile (NPE si liste vide) et violation LoD (le service connaît la structure de `Team` pour en extraire son `Owner`). Le `userId` est déjà disponible comme clé de la map `teamsByUser` — l'owner devrait être résolu par injection d'un `UserRepository` ou passé en paramètre.
- **Correction suggérée :** Injecter `UserRepository` et résoudre l'utilisateur par `userId` (déjà connu), ou structurer la map comme `Map<User, List<Team>>` pour éviter le `get(0).getOwner()`.

---

## Violations MOYENNES

### DEM-007 — TeamLeaderboardService : `team.getOwner().getId()` et `.getUsername()` répétés

- **Fichier :** `src/main/java/com/fortnite/pronos/service/leaderboard/TeamLeaderboardService.java:78-79,141-142`
- **Code actuel :**
  ```java
  .ownerId(team.getOwner().getId())
  .ownerUsername(team.getOwner().getUsername())
  ```
  Présent deux fois dans la même classe (méthodes `getLeaderboard` et `getLeaderboardByGame`).
- **Problème :** Le service accède à `owner` via `team` et en extrait deux propriétés. Le `Team` pourrait exposer `getOwnerId()` / `getOwnerUsername()` comme raccourcis, ou le service pourrait extraire une méthode privée `buildOwnerInfo(Team team)`.
- **Correction suggérée :** Méthode privée `extractOwnerInfo(Team team)` qui retourne un DTO `OwnerInfo(UUID id, String username)`, ou exposer `team.getOwnerId()` / `team.getOwnerUsername()` comme propriétés directes (pattern DTO flattening).

---

### DEM-008 — PlayerLeaderboardService : `team.getOwner().getUsername()` et `teamPlayer.getPlayer().getId()`

- **Fichier :** `src/main/java/com/fortnite/pronos/service/leaderboard/PlayerLeaderboardService.java:118,125,129`
- **Code actuel :**
  ```java
  UUID playerId = teamPlayer.getPlayer().getId();
  // ...
  new TeamInfoDto(team.getId().toString(), team.getName(), team.getOwner().getUsername())
  // ...
  .add(team.getOwner().getUsername());
  ```
- **Problème :** Navigation identique à DEM-007, mais dans un service différent, confirmant le pattern systémique. `team.getOwner().getUsername()` est dupliqué dans la boucle sans extraction.
- **Correction suggérée :** Même approche que DEM-007. En plus, `teamPlayer.getPlayer().getId()` devrait être `teamPlayer.getPlayerId()` si `TeamPlayer` peut exposer cette propriété directement.

---

### DEM-009 — AdminGameSupervisionService : `game.getCreator().getUsername()`

- **Fichier :** `src/main/java/com/fortnite/pronos/service/admin/AdminGameSupervisionService.java:49`
- **Code actuel :**
  ```java
  game.getCreator().getUsername(),
  ```
- **Problème :** Le service admin navigue dans `Game` pour accéder à `User.username`. Si le type du champ `creator` change (ex: migration vers `CreatorRef`), ce code casse silencieusement.
- **Correction suggérée :** Ajouter `game.getCreatorUsername()` comme méthode délégataire sur `Game`, ou exposer le champ `creatorUsername` dans une projection JPA pour éviter le chargement de l'entité `User` complète.

---

### DEM-010 — GameDomainService : `game.getCreator().getId()` en logique métier domaine

- **Fichier :** `src/main/java/com/fortnite/pronos/core/domain/GameDomainService.java:70-71`
- **Code actuel :**
  ```java
  && game.getCreator().getId() != null
  && game.getCreator().getId().equals(user.getId())
  ```
- **Problème :** Logique métier dans le domaine qui navigue à travers un objet imbriqué. `game.getCreator()` est appelé deux fois pour en tirer `getId()`. La LoD suggère soit `game.isCreatedBy(user)` comme méthode de domaine sur `Game`, soit `game.getCreatorId()` comme propriété directe.
- **Correction suggérée :** Ajouter `boolean game.isCreatedBy(User user)` ou `UUID game.getCreatorId()` — cette approche flatterait aussi le modèle et éviterait le chargement EAGER de l'entité `User` pour juste comparer les IDs.

---

### DEM-011 — Model Game.java : `participant.getUser().getId()`

- **Fichier :** `src/main/java/com/fortnite/pronos/model/Game.java:398`
- **Code actuel :**
  ```java
  return creator.getId().equals(participant.getUser().getId());
  ```
- **Problème :** Dans une méthode du modèle `Game` lui-même, on navigue à travers `participant.getUser()` pour extraire `getId()`. Cela crée un couplage entre `Game` et `User` via `GameParticipant`, alors que `GameParticipant` devrait encapuler cette comparaison.
- **Correction suggérée :** Ajouter `participant.isUser(User user)` ou `participant.hasUserId(UUID userId)` sur `GameParticipant`.

---

### DEM-012 — ApiController : `candidate.getOwner().getId()` dans un filtre stream

- **Fichier :** `src/main/java/com/fortnite/pronos/controller/ApiController.java:187`
- **Code actuel :**
  ```java
  .filter(candidate -> candidate.getOwner().getId().equals(currentUser.getId()))
  ```
- **Problème :** Un contrôleur navigue à travers `Team.getOwner().getId()` dans une lambda stream. Les contrôleurs ne doivent pas contenir de logique métier de filtrage sur des propriétés imbriquées.
- **Correction suggérée :** Déplacer ce filtre dans un service `TeamService.findTeamByOwner(UUID userId, List<Team> teams)`, ou exposer `team.isOwnedBy(UUID userId)`.

---

### DEM-013 — AdminAlertService : `metrics.getHttp().getErrorRate()` et `metrics.getJvm().getHeapUsagePercent()`

- **Fichier :** `src/main/java/com/fortnite/pronos/service/admin/AdminAlertService.java:63,80,97,116,120`
- **Code actuel :**
  ```java
  double errorRate = metrics != null && metrics.getHttp() != null ? metrics.getHttp().getErrorRate() : 0;
  double heapUsage = metrics != null && metrics.getJvm() != null ? metrics.getJvm().getHeapUsagePercent() : 0;
  double diskUsage = health != null && health.getDisk() != null ? health.getDisk().getUsagePercent() : 0;
  int maxConnections = health.getDatabasePool().getMaxConnections();
  double usagePercent = health.getDatabasePool().getActiveConnections() * 100.0 / maxConnections;
  ```
- **Problème :** `AdminAlertService` navigue à deux niveaux dans les DTOs `SystemMetricsDto` et `SystemHealthDto` pour extraire des valeurs primitives. Si ces DTOs sont des agrégats de données (VO), les méthodes de navigation devraient être encapsulées dans des méthodes `metrics.getHttpErrorRate()`, `metrics.getJvmHeapUsagePercent()`, `health.getDiskUsagePercent()`, `health.getDatabasePoolUsagePercent()`.
- **Correction suggérée :** Ajouter des accesseurs délégataires sur `SystemMetricsDto` et `SystemHealthDto`, ou des méthodes de valeur directes : `health.getDatabasePoolUsagePercent(int maxConnections)`. Le calcul `activeConnections * 100 / maxConnections` est une logique métier qui appartient au DTO lui-même.

---

### DEM-014 — Template team-detail.html : `teamPlayer.player.region` et `.tranche`

- **Fichier :** `frontend/src/app/features/teams/team-detail/team-detail.html:74-86`
- **Code actuel :**
  ```html
  <span class="player-name">{{ teamPlayer.player.nickname }}</span>
  <span class="region-tag" [style.background]="getRegionColor(teamPlayer.player.region)">
    {{ teamPlayer.player.region }}
  </span>
  <span class="tranche-tag">T{{ teamPlayer.player.tranche }}</span>
  <span class="points">{{ formatPoints(teamPlayer.player.points || 0) }}</span>
  ```
- **Problème :** Le template accède systématiquement à `teamPlayer.player.X` — 2 niveaux de navigation répétés 5+ fois dans la même boucle `*ngFor`. Le composant est couplé à la structure interne de `TeamPlayer` (qui contient un `Player`).
- **Correction suggérée :** Utiliser une sous-composante `<app-team-player-card [teamPlayer]="teamPlayer">` qui encapsule le rendu du `Player`, ou exposer dans le composant parent une méthode `getPlayerInfo(teamPlayer: TeamPlayer): PlayerInfo` retournant un objet plat.

---

### DEM-015 — UserGamesStore : `stateSubject.value.games.length` et `.find()`

- **Fichier :** `frontend/src/app/core/services/user-games.store.ts:55,62,154`
- **Code actuel :**
  ```typescript
  return this.stateSubject.value.games.length;
  return this.stateSubject.value.games.length > 0;
  return this.stateSubject.value.games.find(g => g.id === gameId);
  ```
- **Problème :** Le store accède à `stateSubject.value.games` (3 niveaux) dans plusieurs méthodes. Bien que `stateSubject` soit un membre privé du store lui-même (donc légitimement accessible), la chaîne `.value.games` expose la structure interne du state. Si le shape de `StoreState` change, toutes ces méthodes sont affectées.
- **Correction suggérée :** Extraire une méthode privée `getCurrentGames(): Game[]` qui encapsule `this.stateSubject.value.games`, puis l'appeler dans `getGameCount()`, `hasGames()`, et `findGame()`. Réduction du couplage structurel interne.

---

### DEM-016 — DraftDto.java : navigation via `draft.getGame()` dans DTO mapper

- **Fichier :** `src/main/java/com/fortnite/pronos/dto/DraftDto.java:39-40`
  (voir aussi DEM-002 — inclus ici pour complétion du tableau de sévérité MOYENNE)
- Niveau HAUTE mais listée ci-dessus. Voir DEM-002.

---

### DEM-017 — Template admin-dashboard : `health.databasePool.activeConnections` en logique inline

- **Fichier :** `frontend/src/app/features/admin/admin-dashboard/admin-dashboard.component.html:71-79`
- **Code actuel :**
  ```html
  *ngIf="health.databasePool.activeConnections >= 0"
  {{ health.databasePool.activeConnections }}/{{ health.databasePool.maxConnections }}
  [value]="health.databasePool.maxConnections > 0
    ? (health.databasePool.activeConnections / health.databasePool.maxConnections) * 100
    : 0"
  ```
- **Problème :** Le template fait un calcul arithmétique (ratio de connexions) en accédant à 2 niveaux de profondeur dans un objet. La logique métier (`activeConnections / maxConnections * 100`) est dans le template HTML.
- **Correction suggérée :** Exposer dans le composant TypeScript une méthode `getDatabasePoolUsagePercent(): number` qui encapsule ce calcul, et utiliser `health.databasePool` via un getter du composant.

---

## Violations BASSES / Faux positifs détaillés

### DEM-B01 — Mappers JPA : `entity.getGame().getId()`, `entity.getCreator().getId()`

- **Fichiers :**
  - `adapter/out/persistence/draft/DraftEntityMapper.java:19`
  - `adapter/out/persistence/game/GameEntityMapper.java:207,217,285`
  - `adapter/out/persistence/team/TeamEntityMapper.java:37,39,56`
- **Contexte :** Ces mappers ont pour responsabilité explicite de convertir les graphes d'entités JPA (avec leurs relations `@ManyToOne`) en modèles domaine. Naviguer dans les relations imbriquées est la seule façon d'extraire les IDs des entités liées sans charger inutilement des entités séparées.
- **Décision :** FAUX POSITIF dans ce contexte. Les mappers sont les seuls autorisés à naviguer dans les internals JPA. Tolérable à condition de rester dans la couche adapter/persistence uniquement.

---

### DEM-B02 — SecurityContextHolder.getContext().getAuthentication()

- **Fichiers :**
  - `config/JwtAuthenticationFilter.java:58`
  - `config/TestFallbackAuthenticationFilter.java:42`
  - `service/FlexibleAuthenticationService.java:42,106`
  - `service/UserContextService.java:92,176`
  - `service/UserResolver.java:111`
- **Contexte :** C'est le pattern standard Spring Security pour accéder au contexte d'authentification. L'API Spring Security impose cette chaîne — il n'existe pas d'alternative plus courte fournie par le framework.
- **Décision :** PATTERN FRAMEWORK OBLIGATOIRE. Acceptable. La mitigation existante (`UserContextService` centralise ces appels) est déjà une bonne pratique.

---

### DEM-B03 — PerformanceMonitor : `memoryBean.getHeapMemoryUsage().getUsed()`

- **Fichiers :** `config/PerformanceMonitor.java:77,78,122,123,155,156`
- **Contexte :** `MemoryMXBean.getHeapMemoryUsage()` retourne un objet `MemoryUsage` — c'est l'API JMX standard Java. Il n'y a pas d'autre façon d'accéder à ces métriques. Ce pattern est documenté comme idiomatique par la JVM.
- **Décision :** PATTERN JMX OBLIGATOIRE. Acceptable. La sévérité est réduite par le fait que `getHeapMemoryUsage()` est appelé plusieurs fois dans la même méthode — une variable locale `MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage()` éviterait les appels multiples et réduirait la verbosité.

---

### DEM-B04 — DatabaseAutoConfiguration : `connection.getMetaData().getDatabaseProductName()`

- **Fichier :** `config/DatabaseAutoConfiguration.java:42-43`
- **Contexte :** API JDBC standard — `connection.getMetaData()` est l'unique point d'accès aux métadonnées de connexion. Pattern imposé par le driver JDBC.
- **Décision :** FAUX POSITIF. Pattern JDBC obligatoire.

---

### DEM-B05 — FortnitePronosException : `getCause().getClass().getSimpleName()`

- **Fichier :** `core/error/FortnitePronosException.java:245`
- **Contexte :** Méthode de diagnostic `buildDetailedReport()` dans une exception. Le seul but est d'afficher le type de la cause racine dans un log. `getCause()` retourne un `Throwable` et `getClass().getSimpleName()` est idiomatique pour obtenir le nom simple.
- **Décision :** BASSE sévérité. Tolérable dans une méthode de reporting d'erreur. Pourrait être extrait en méthode utilitaire `ExceptionUtils.causeTypeName(Throwable)`.

---

### DEM-B06 — AdminSystemMetricsService : `ManagementFactory.getRuntimeMXBean().getUptime()`

- **Fichier :** `service/admin/AdminSystemMetricsService.java:33`
- **Contexte :** API JMX standard. `ManagementFactory` est un point d'accès statique — c'est le pattern idiomatique Java pour accéder aux métriques JVM.
- **Décision :** FAUX POSITIF. Pattern JMX obligatoire.

---

## Patterns légitimes identifiés (ne pas corriger)

Les patterns suivants ont été identifiés comme **conformes à la LoD** ou relevant de **patterns framework/bibliothèque** reconnus :

| Pattern | Raison |
|---|---|
| `Optional.of(x).map(f).orElse(def)` | Fluent API fonctionnelle — chaque maillon transforme, pas navigue |
| `stream().filter().map().collect()` | Stream API Java — pipeline déclaratif, pas navigation d'objet |
| `.builder().field(x).build()` | Builder pattern — construction linéaire sans navigation |
| `SecurityContextHolder.getContext().getAuthentication()` | API Spring Security obligatoire |
| `memoryBean.getHeapMemoryUsage().getUsed()` | API JMX Java obligatoire |
| `connection.getMetaData().getDatabaseProductName()` | API JDBC obligatoire |
| `ManagementFactory.getRuntimeMXBean().getUptime()` | API JMX Java obligatoire |
| `route.snapshot.paramMap.get('id')` | API Angular Router obligatoire |
| `this.nameControl.value.trim()` | Angular ReactiveForm — `.value` est la propriété standard du contrôle |
| `this.basicInfoForm.value.name` | Angular ReactiveForm — `.value` est le seul accès possible aux valeurs du formulaire |
| `entity.getJavaType().getAnnotation(...)` | JPA Metamodel API — accès réflexif obligatoire dans les mappers admin |
| `entityManager.getMetamodel().getEntities()` | JPA API standard — unique point d'accès au métamodèle |

---

## Récapitulatif par fichier

| Fichier | Violations | Sévérité max |
|---|---|---|
| `dto/DraftPickDto.java` | DEM-001 | HAUTE |
| `dto/DraftDto.java` | DEM-002 | HAUTE |
| `application/facade/DraftDomainFacade.java` | DEM-003 | HAUTE |
| `dto/team/TeamDto.java` | DEM-004 | HAUTE |
| `trade-detail.component.html` | DEM-005 | HAUTE |
| `service/leaderboard/PronostiqueurLeaderboardService.java` | DEM-006 | HAUTE |
| `service/leaderboard/TeamLeaderboardService.java` | DEM-007 | MOYENNE |
| `service/leaderboard/PlayerLeaderboardService.java` | DEM-008 | MOYENNE |
| `service/admin/AdminGameSupervisionService.java` | DEM-009 | MOYENNE |
| `core/domain/GameDomainService.java` | DEM-010 | MOYENNE |
| `model/Game.java` | DEM-011 | MOYENNE |
| `controller/ApiController.java` | DEM-012 | MOYENNE |
| `service/admin/AdminAlertService.java` | DEM-013 | MOYENNE |
| `features/teams/team-detail/team-detail.html` | DEM-014 | MOYENNE |
| `core/services/user-games.store.ts` | DEM-015 | MOYENNE |
| `admin-dashboard.component.html` | DEM-017 | MOYENNE |
| Mappers JPA (3 fichiers) | DEM-B01 | BASSE (faux positif) |
| `config/PerformanceMonitor.java` | DEM-B03 | BASSE |
| `core/error/FortnitePronosException.java` | DEM-B05 | BASSE |

---

## Recommandations prioritaires

### Priorité 1 — Corriger immédiatement (HAUTE)

1. **DEM-001 `DraftPickDto`** : Ajouter des méthodes délégataires sur `DraftPick` : `getPickerUserId()`, `getPickerUsername()`, `getParticipantCount()`. Cela supprime les 3 violations en une seule modification de l'entité.

2. **DEM-004 `TeamDto`** : Extraire le calcul du score total dans un `TeamScoreCalculator` (service ou helper statique). La DTO devient passive et ne navigue plus dans les internals.

3. **DEM-005 template `trade-detail`** : Créer une sous-composante `<app-player-stats-card [player]="trade.playerOut">` qui encapsule le rendu de `playerOut.stats.X`.

### Priorité 2 — Refactoring de fond (MOYENNE)

4. **DEM-007/DEM-008/DEM-009 — Pattern `owner` dans les leaderboards** : Créer un accesseur `team.getOwnerId()` / `team.getOwnerUsername()` ou un VO `TeamOwnerInfo`. Cela résout 6 violations en une seule modification de `Team`.

5. **DEM-010/DEM-011 — Domaine `Game`** : Ajouter `game.isCreatedBy(User user)` et `participant.hasUserId(UUID)` comme méthodes de domaine expressives. Renforce l'ubiquitous language et supprime les navigations.

6. **DEM-013 `AdminAlertService`** : Ajouter `metrics.getHttpErrorRate()`, `metrics.getJvmHeapUsagePercent()`, `health.getDiskUsagePercent()`, `health.getDatabasePoolUsagePercent()` sur les DTOs système. Les calculs arithmétiques (`activeConnections / maxConnections`) appartiennent aux value objects, pas aux services clients.

### Priorité 3 — Améliorations ergonomiques (BASSE)

7. **DEM-B03 `PerformanceMonitor`** : Extraire `MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage()` en variable locale dans les méthodes `monitorPerformance()` et `detailedPerformanceReport()` pour éviter l'appel multiple et améliorer la lisibilité.

8. **DEM-015 `UserGamesStore`** : Extraire `private getCurrentGames()` pour centraliser `stateSubject.value.games`.

---

## Tickets suggérés

| ID | Description | Priorité | Effort |
|---|---|---|---|
| TECH-022-A | Ajouter méthodes délégataires sur `DraftPick` (pickerId, pickerName, participantCount) | P1 | 1h |
| TECH-022-B | Extraire `TeamScoreCalculator` depuis `TeamDto` | P1 | 2h |
| TECH-022-C | Sous-composante `PlayerStatsCard` pour `trade-detail` template | P1 | 1h |
| TECH-022-D | Ajouter `getOwnerId()` / `getOwnerUsername()` sur entité `Team` | P2 | 1h |
| TECH-022-E | Ajouter `game.isCreatedBy(User)` et `participant.hasUserId(UUID)` | P2 | 1h |
| TECH-022-F | Enrichir DTOs `SystemMetricsDto` / `SystemHealthDto` avec accesseurs calculés | P2 | 2h |
