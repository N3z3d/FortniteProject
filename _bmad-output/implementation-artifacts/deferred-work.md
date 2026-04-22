# Deferred Work

## Deferred from: code review of sprint19-fix-resolution-adapter-config (2026-04-22)

- D1 - `fortnite-api` peut demarrer sans `FORTNITE_API_KEY`, puis retourner silencieusement `not found` au premier lookup [FortniteApiAdapter.java:35] - dette de configuration pre-existante; la story courante cible uniquement la validation de `resolution.adapter`. Tracking cree : `sprint19-fix-fortnite-api-key-config`.

## Deferred from: code review of sprint19-fix-join-redirect (2026-04-20)

- W1 — Double navigate pré-existant dans `JoinGameComponent` : snackbar action (5s) + setTimeout (1s) peuvent appeler `router.navigate(['/games', id])` deux fois si l'utilisateur clique "View" avant 1s — inoffensif mais code smell [join-game.component.ts:71-81]

## Deferred from: code review of sprint19-fix-draft-button-guard (2026-04-20)

- W1 — Cache store périmé peut autoriser l'accès au draft si le statut a changé côté serveur dans la fenêtre 30s — pré-existant, même pattern dans `simultaneousModeGuard` [draft-status.guard.ts]
- W2 — Double appel HTTP sur la route `/simultaneous` : `draftStatusGuard` puis `simultaneousModeGuard` appellent chacun `getGameById` sur cache miss car aucun ne peuple le store [game-routing.module.ts]
- W3 — Statuts legacy `DRAFT`/`RECRUITING` présents dans le type `GameStatus` mais non couverts par les tests du guard — régression silencieuse si backend émet `DRAFT` sur un jeu actif [draft-status.guard.ts]
- W4 — Race condition navigation WS pendant l'appel HTTP du guard : pas de `takeUntil`, le `catchError` peut resolver après une navigation concurrente [draft-status.guard.ts]
- W5 — `findGameById` dans le store fait une comparaison stricte `===` sans normalisation de casse — URL tapée manuellement avec casse différente tombe en fallback HTTP [user-games.store.ts]
- W6 — Routes draft définies à plat (pas en arbre parent/enfants) : une nouvelle route `:id/draft/xxx` ajoutée sans `canActivate` sera non gardée par défaut [game-routing.module.ts]

## Deferred from: code review of sprint19-fix-catalogue-demo-banner (2026-04-19)

- W1 — Violation SRP : `ScrapeLogService` injecté dans `PlayerController` — couplage infrastructure scraping dans un contrôleur joueurs, devrait passer par un use case dédié [PlayerController.java] — décision de spec, refactoring futur
- W2 — Race condition UI OnPush : `getDataStatus()` et `getPlayers()` concurrents dans `ngOnInit` — bannière peut pop-in après rendu des joueurs sur connexion lente [player-catalogue-page.component.ts] — UX hors scope
- W3 — DB hit à chaque appel endpoint public : aucun cache (`@Cacheable` / `Cache-Control`) sur `/catalogue/data-status` — charge sous trafic bot [PlayerController.java] — optimisation hors scope
- W4 — `getDataStatus()` re-déclenché à chaque navigation : aucun `shareReplay(1)` ni garde single-shot [player-catalogue.service.ts + player-catalogue-page.component.ts] — optimisation mineure
- W5 — `RateLimitingFilter` global peut throttler IPs NAT partagées sur ce nouvel endpoint public [SecurityConfig.java] — préoccupation pré-existante

## Deferred from: code review of sprint19-fix-invitation-code-security (2026-04-19)

- W1 — Race TOCTOU : participants non chargés → `isGameHost()` fallback `creatorName` uniquement — fenêtre d'affichage transitoire pour un non-hôte [game-detail.component.ts] — pre-existing
- W2 — `canRegenerateCode()` implémentation partagée avec `canRenameGame()` — couplage sémantique, un futur changement sur l'un casse l'autre silencieusement [game-detail-permissions.service.ts:116-118] — pre-existing
- W3 — Jeu legacy `FINISHED` + `creatorId` absent : hôte peut cliquer régénérer mais backend rejettera — pas de garde sur le statut [game-detail-permissions.service.ts] — pre-existing
- W4 — `canRegenerateCode()` appelé 3× par cycle CD sans `OnPush` [game-detail.component.html:273,297,311] — pre-existing, tracé JIRA-AUDIT-011

## Deferred from: code review of sprint18-retry-429-pipeline (2026-04-17)

- D1 — Erreurs non-429 (ex: 500) émettent `null` comme "not found" — indiscernables d'un résultat vide légitime [pipeline.service.ts] — comportement pré-existant

## Deferred from: code review of sprint19-fix-draft-notification (2026-04-18)

- D1 — `unsubscribeAll()` ne remet pas `activeGameEventsGameId` à null [websocket.service.ts:302] — hardening défensif non critique, disconnect() efface déjà en amont
- D2 — Perte définitive de subscription game events après maxReconnectAttempts (disconnect() efface activeGameEventsGameId) [websocket.service.ts] — comportement du système de reconnect existant, hors scope
- D3 — `gameNotifications$` est un Subject plain — race étroite théorique si DRAFT_STARTED arrive avant que subscribeToDraftStarted() soit abonné [websocket.service.ts] — quasi impossible en pratique
- D4 — `GameDetailComponent` ne désabonne jamais explicitement sa subscription STOMP game events à ngOnDestroy [game-detail.component.ts] — pre-existing, mitigé par le modèle one-active-subscription du service
- D5 — `GameHomeComponent` ne subscribe pas si le jeu est déjà en statut DRAFTING au moment du refresh de page [game-home.component.ts:251] — scenario distinct (user revient mid-draft), hors scope story RC-2
- D2 — Callback `onRetry` paramètre du service crée un couplage service/vue — alternative BehaviorSubject plus complexe, pattern acceptable [pipeline.service.ts]
- D3 — Pas de feedback après épuisement rate-limit (null silencieux, spinner s'arrête sans message) — pattern UX pré-existant [admin-pipeline-table.component.ts]
- D4 — AC-5 `suggestLoading` validé superficiellement via Subject, pas avec vrais timers fakeAsync — amélioration test non bloquante [admin-pipeline-table.component.spec.ts]

## Deferred from: code review of sprint18-adapter-info-endpoint (2026-04-17)

- E1 — Appelants de `resolveFortniteId` pas encore migrés (`ResolutionQueueService`, `PrIngestionRowProcessor`) — cleanup à faire après décision D2 sur le refactoring de `ResolutionPort`
- E2 — `ResolutionQueueService.tryResolveEntry()` ne vérifie pas `epicAccountId == null` avant `entry.resolve()` — risque de persister un Epic ID null comme RESOLVED [ResolutionQueueService.java:76]
- E3 — `RESOLUTION_ADAPTER=""` → `NoSuchBeanDefinitionException` au démarrage — gap de configuration pré-existant, aucun test de démarrage partiel
- E4 — Flash UX : `getRegionalStatus()` se résout après `forkJoin`, table régionale apparaît après disparition du spinner [admin-pipeline-page.component.ts]
- E5 — Dev Notes de la story déclarent "pas de nouvelle dépendance" mais `resolutionPort` est ajouté (5 deps total, CouplingTest OK — pas bloquant)

## Deferred from: code review of sprint16-da06-leave-e2e (2026-04-16)

- Force-refresh bypass du guard `state.loading` peut provoquer des requêtes HTTP concurrentes [user-games.store.ts:88] — tradeoff architectural correct pour fixer la regression DA-06, à monitorer si des doubles-requêtes apparaissent en production

## Deferred from: code review of sprint16-pipeline-autoload (2026-04-13)

- `@Deprecated default` dans `ResolutionPort` interface domaine — dette design intentionnelle pour rétrocompatibilité transitoire, à supprimer quand `FortniteApiResolutionAdapter` sera migré vers `resolvePlayer()`
- `PlayerIdentityPipelineService` à 5 dépendances injected (limite CouplingTest : 7) — surveiller lors des prochains sprints pour ne pas dépasser le seuil

## Deferred from: code review of sprint14-fix-leaderboard-design (2026-04-11)

- `Orbitron` reste dans d'autres règles de `simple-leaderboard.component.scss` (`.podium .points`, `.table-container`, rank column, `.points-value`, `h3`) — cohérence typographique partielle, hors scope AC-2
- `+12%` hardcodé dans le bloc stat-trend du dashboard — valeur fictive co-localisée avec le fix `proPlayersCount`, trompeuse quand le count est 0
- FOUT/layout-shift Rajdhani — problème infrastructure pré-existant lié au chargement non-bloquant des polices gaming

## Deferred from: code review of sprint14-ux-notification-tour (2026-04-10)

- Vibration ne respecte pas `prefersReducedMotion` — appel `navigator.vibrate()` sans vérification de l'accessibilité motion; à ajouter dans une prochaine story UX
- Duplicate `this.draft = state` dans `applyDraftState()` — assignation double pré-existante, aucun impact fonctionnel [snake-draft-page.component.ts]
- Race condition phase/draft : `phase` mis à jour dans le handler WS, `draft` mis à jour via REST — label peut ne pas s'afficher pendant ~100ms sur connexion lente [snake-draft-page.component.html:40]
- Manque test négatif : `.waiting-turn-label` absent quand `phase !== 'waiting'` [snake-draft-page.component.spec.ts]
- `component.isMyTurn = false` direct dans les tests — pattern pré-existant dans le fichier spec, brittle mais cohérent avec les autres tests

## Deferred from: code review of sprint14-fix-participants-count (2026-04-08)

- `canAcceptParticipants()` utilise `participants.size()` au lieu de `getTotalParticipantCount()` — incohérence pré-existante [Game.java:247]
- JPA entity `model/Game.java` rejette toujours le créateur dans `isInvalidNewParticipant` — divergence pré-existante entre domain model et entity [model/Game.java:330]
- `fromEntityGame`/`countEntityParticipants` n'a aucun appelant en production — undercount potentiel pour legacy data sans impact réel

## Deferred from: code review of sprint19-fix-deprecated-resolution (2026-04-21)

- D1 - `ResolutionQueueService` n'a pas de trigger runtime de production [ResolutionQueueService.java:44] - dette d'architecture pre-existante; la story de cleanup interdit d'ajouter endpoint/config/flux Spring.
- D2 - `tryResolveEntry()` conserve un catch large autour de `compute`, `entry.resolve` et `identityRepository.save` [ResolutionQueueService.java:69] - pattern pre-existant; a traiter dans un durcissement transactionnel separe.

## Deferred from: code review de sprint19-fix-leaderboard-regression (2026-04-18)

- **W1** — `h1` Orbitron potentiellement non chargé dans index.html → fallback monospace sur le titre principal du leaderboard (`simple-leaderboard.component.scss` l.31). Spec interdit de modifier.
- **W2** — Rajdhani chargé non-bloquant (`media=print onload`) → CLS/FOUF visible au premier rendu du podium. Amélioration : `<link rel="preload" as="font">` pour Rajdhani woff2.
- **W3** — Rajdhani chargé uniquement en poids 600/700. D'autres poids (300/400/500) se retrouvent en synthèse browser si utilisés.
- **W4** — `.podium-item:hover` écrase le `transform: scale(1.15)` du `.first` item → jump visuel au hover. Scoper les transforms hover par variant (`.first:hover`, `.second:hover`, `.third:hover`).
- **W5** — `position: fixed` sur `.leaderboard-container::before` déborde hors du composant si intégré dans un layout parent.
- **W6** — `simple-leaderboard.component.spec.ts` n'a que 2 tests sans assertion DOM — les changements HTML sont invisibles au test suite.
