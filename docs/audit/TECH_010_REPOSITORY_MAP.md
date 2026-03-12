# TECH-010 — Cartographie du Repository

> Audité le 2026-03-04
> Prérequis pour : TECH-011, TECH-012, TECH-013, TECH-014, TECH-015, TECH-019, TECH-020, TECH-021, TECH-022

---

## 1. Métriques globales

| Métrique | Valeur | Notes |
|---|---|---|
| **Fichiers Java (main)** | 431 | Controllers, Services, Domain, DTOs, Adapters |
| **Fichiers Java (test)** | 251 | Unit + Integration tests |
| **Fichiers TypeScript (src)** | 316 | Angular 19+ standalone components |
| **Migrations SQL** | 41 | V1–V41, progression linéaire |
| **Ports de domaine** | 29 | Repository interfaces |
| **Adaptateurs de persistance** | 13 | + 10 entity mappers |
| **Legacy JPA repositories** | 16 | Architecture hybride |
| **Couverture lignes** | 86.89% | Branches 73.36%, Functions 84.64% |

---

## 2. Backend — Structure des packages

### 2.1 Controllers (34 classes)

| Groupe | Classes | Taille notable |
|---|---|---|
| Game | GameController, GameDetailController, GameQueryController, GameStatisticsController, GameRealtimeController | GameController: **537 lignes** |
| Draft | DraftController, SnakeDraftController, DraftSimultaneousController, DraftParticipantTradeController, DraftAuditController, SwapSoloController | DraftController: **657 lignes** (corruption UTF-8) |
| Admin | AdminDashboardController, AdminPlayerPipelineController, AdminDraftRosterController, AdminGameSupervisionController, AdminScrapeController, AdminAuditController, AdminDatabaseController, ErrorJournalController | — |
| Player/Team | PlayerController, TeamController, LeaderboardController, GameLeaderboardController | — |
| Auth/Account | AuthController, AccountController, UserController | — |
| Autre | TradeController, ScoreController, MetaController, VisitTrackingController, HomeController, PrIngestionController, ApiController, GameIncidentController | — |

### 2.2 Services (116 classes — 14 sous-packages)

| Package | Classes | Anomalies |
|---|---|---|
| `service/` (racine) | ~34 classes | TradingService: 459 lignes |
| `service/admin/` | 22 classes | ErrorJournalService, AdminAuditLogService, etc. |
| `service/draft/` | 8 classes | DraftService: **509 lignes** |
| `service/game/` | 4 classes | GameCreationService, GameDraftService, etc. |
| `service/leaderboard/` | 6 classes | — |
| `service/trade/` | 2 classes | — |
| `service/ingestion/` | 8 classes | PrIngestionOrchestrationService, etc. |
| `service/scoring/` | 3 classes | — |
| `service/seed/`, `auth/`, `cache/`, `catalogue/`, `supabase/` | ~19 classes | CsvDataLoaderService: 418 lignes |

### 2.3 Domaine (57 classes — architecture hexagonale)

**Modèles purs :**
| Package | Classes |
|---|---|
| `domain/game/model/` | Game (**551 lignes**), GameParticipant, GameStatus, GameRegionRule, DraftMode, PlayerRegion |
| `domain/draft/model/` | Draft, DraftStatus, DraftAsyncWindow, DraftAsyncSelection, DraftAsyncWindowStatus, DraftRegionCursor, SnakeTurn, DraftParticipantTrade, DraftParticipantTradeStatus, DraftSwapAuditEntry |
| `domain/player/model/` | Player, RankSnapshot |
| `domain/player/identity/model/` | PlayerIdentityEntry, IdentityStatus, MetadataCorrection, RegionalStatRow |
| `domain/player/alias/model/` | PlayerAliasEntry |
| `domain/team/model/` | Team, TeamMember, TeamScoreDelta |
| `domain/trade/model/` | Trade, TradeStatus |

**Ports de domaine (29) :** `domain/port/out/`
DraftRepositoryPort, DraftDomainRepositoryPort, DraftPickRepositoryPort, DraftAsyncRepositoryPort, DraftRegionCursorRepositoryPort, DraftParticipantTradeRepositoryPort, DraftSwapAuditRepositoryPort, GameRepositoryPort, GameDomainRepositoryPort, GameParticipantRepositoryPort, GameRegionRuleRepositoryPort, PlayerRepositoryPort, PlayerDomainRepositoryPort, PlayerIdentityRepositoryPort, PlayerAliasRepositoryPort, RankSnapshotRepositoryPort, TeamRepositoryPort, TeamDomainRepositoryPort, TeamPlayerRepositoryPort, TeamScoreDeltaRepositoryPort, TradeRepositoryPort, TradeDomainRepositoryPort, UserRepositoryPort, ScoreRepositoryPort, PrSnapshotQueryPort, InvitationCodeRepositoryPort, ResolutionPort, EpicIdValidatorPort

### 2.4 Adaptateurs de persistance (13 + 10 mappers)

**Location :** `adapter/out/persistence/`

| Domaine | Adaptateurs | Mappers |
|---|---|---|
| Game | GameRepositoryAdapter | GameEntityMapper (382 lignes) |
| Draft | DraftRepositoryAdapter, DraftPickRepositoryAdapter, DraftAsyncRepositoryAdapter, DraftRegionCursorRepositoryAdapter, DraftParticipantTradeRepositoryAdapter, DraftSwapAuditRepositoryAdapter | DraftEntityMapper, DraftAsyncEntityMapper, DraftRegionCursorEntityMapper |
| Player | PlayerRepositoryAdapter, PlayerIdentityRepositoryAdapter, RankSnapshotRepositoryAdapter, PlayerAliasRepositoryAdapter | PlayerEntityMapper, PlayerIdentityEntityMapper, RankSnapshotEntityMapper |
| Team | TeamRepositoryAdapter, TeamScoreDeltaRepositoryAdapter | TeamEntityMapper |
| Trade | TradeRepositoryAdapter | — |
| Other | StubResolutionAdapter (resolution/), MockEpicIdValidatorAdapter (epicid/) | — |

### 2.5 Legacy (architecture hybride — dette technique)

**Legacy JPA entities (23) :** `model/` — en cours de migration

| Entité | Doublon domaine | État |
|---|---|---|
| model/Game.java (511 lignes) | domain/game/model/Game.java | DOUBLON CRITIQUE |
| model/Draft.java | domain/draft/model/Draft.java | Doublon |
| model/Player.java | domain/player/model/Player.java | Doublon |
| model/Team.java | domain/team/model/Team.java | Doublon |
| model/Trade.java | domain/trade/model/Trade.java | Doublon |
| model/GameParticipant.java | domain/game/model/GameParticipant.java | Doublon |
| model/User.java | — | Pas encore migré |
| model/Score.java | — | Pas encore migré |
| model/DraftPick.java | — | Pas encore migré |
| model/PrSnapshot.java | — | Pas encore migré |
| model/IngestionRun.java | — | Pas encore migré |
| ... | | |

**Legacy JPA repositories (16) :** `repository/`
GameRepository, DraftRepository, DraftPickRepository, PlayerRepository, TeamRepository, TradeRepository, GameParticipantRepository, GameRegionRuleRepository, TeamPlayerRepository, ScoreRepository, PrSnapshotRepository, UserRepository, IngestionRunRepository, + 3 nouveaux JPA repos (DraftParticipantTradeJpaRepository, DraftSwapAuditJpaRepository, TeamScoreDeltaJpaRepository)

> **137 fichiers Java** importent encore `com.fortnite.pronos.model.*` (legacy entities) — dette de migration.

### 2.6 DTOs (75 classes)

| Package | Classes |
|---|---|
| `dto/` (racine) | ~41 classes (GameDto, DraftDto, TradeDto, LeaderboardDto, etc.) |
| `dto/admin/` | 18 classes (PlayerIdentityEntryResponse, AdminAuditEntryDto, etc.) |
| `dto/auth/`, `dto/common/`, `dto/leaderboard/`, `dto/mapper/`, `dto/player/`, `dto/team/` | ~16 classes |

### 2.7 Gestion des exceptions (22 classes)

Architecture à 3 niveaux : `GlobalExceptionHandler` (@Order 2) + `DomainExceptionHandler` (@Order 1) + `AlreadyInGameFallbackHelper`.

Exceptions métier : BusinessException (base), + 21 exceptions domaine-spécifiques.

### 2.8 Application layer

`application/usecase/` : 10 use cases (GameCreationUseCase, TradeQueryUseCase, etc.)
`application/facade/` : 2 facades (GameDomainFacade, DraftDomainFacade)

---

## 3. Frontend — Structure des modules

### 3.1 Core (60 fichiers)

| Sous-package | Contenu |
|---|---|
| `core/services/` | leaderboard.service.ts (**487 lignes**), translation.service.ts, websocket.service.ts (313 lignes), user-games.store.ts, auth-switch.service.ts, etc. |
| `core/guards/` | auth.guard.ts, admin.guard.ts, game-selection.guard.ts |
| `core/repositories/` | leaderboard.repository.ts, dashboard.repository.ts |

### 3.2 Shared (68 fichiers)

| Sous-package | Contenu |
|---|---|
| `shared/components/` | main-layout (**523 lignes**), player-card, player-search-filter, snake-order-bar, sparkline-chart, coin-flip-animation, confirm-dialog, etc. |
| `shared/services/` | notification.service.ts (**430 lignes**), accessibility-announcer, focus-management, premium-interactions, effects/ (ui-effects, animation, particle, button) |

### 3.3 Features (192 fichiers — 12 modules)

| Feature | Fichiers | Anomalies |
|---|---|---|
| `admin/` | 30 | — |
| `draft/` | 30 | draft.interface.ts 376 lignes, draft.constants.ts 328 lignes |
| `game/` | 43 | game-detail.component.ts 418 lignes |
| `trades/` | 34 | trading-dashboard (**492**), trade-proposal (**491**), trade-form (**489**), trade-details (**450**), trading.service.ts (**460**) — **5 fichiers dépassant 400 lignes** |
| `dashboard/` | 14 | dashboard.component.ts **489 lignes**, dashboard-data.service.ts 408 lignes |
| `leaderboard/` | 12 | leaderboard-api.mapper.ts 373 lignes |
| `catalogue/` | 8 | — |
| `auth/` | ~8 | login.component.ts **602 lignes** |
| `teams/`, `profile/`, `settings/`, `other` | ~23 | — |

---

## 4. Fichiers dépassant 500 lignes (à refactoriser — CLAUDE.md §Contraintes)

### 4.1 Backend

| Fichier | Lignes | Action |
|---|---|---|
| `controller/DraftController.java` | 657 | Extraire DraftPickController, DraftStateController + réparer UTF-8 |
| `domain/game/model/Game.java` | 551 | Extraire GameLifecycleOps, GameParticipantOps, GameDraftOps |
| `controller/GameController.java` | 537 | Déjà partiellement splité — compléter extraction |
| `service/draft/DraftService.java` | 509 | Déjà à 509 — extraire DraftPickService, DraftStateService |
| `model/Game.java` (legacy) | 511 | Dette de migration — à supprimer quand migration complète |

### 4.2 Frontend

| Fichier | Lignes | Action |
|---|---|---|
| `auth/login.component.ts` | 602 | Extraire LoginFormService, LoginPasswordValidator |
| `shared/components/main-layout` | 523 | Extraire MainLayoutNavigationComponent |
| `leaderboard.service.ts` | 487 | Extraire PlayerLeaderboardService, TeamLeaderboardService côté frontend |
| `trades/trading-dashboard` | 492 | Extraire TradeListSection, TradeActionSection |
| `trades/trade-proposal` | 491 | Extraire ProposalFormComponent |
| `dashboard/dashboard.component.ts` | 489 | Extraire DashboardChartsComponent, DashboardStatsComponent |
| `trades/trade-form` | 489 | Extraire TradePlayerPickerComponent |
| `trades/trading.service.ts` | 460 | Extraire TradeMutationService, TradeReadService |
| `trades/trade-details` | 450 | Extraire TradeTimelineComponent |

---

## 5. Statut de migration hexagonale

| Domaine | Ports | Adapters | Legacy repo toujours utilisé |
|---|---|---|---|
| Game | ✅ GameRepositoryPort, GameDomainRepositoryPort, GameParticipantRepositoryPort, GameRegionRuleRepositoryPort | ✅ GameRepositoryAdapter | ⚠️ GameRepository (legacy) |
| Draft | ✅ 7 ports | ✅ 6 adaptateurs | ⚠️ DraftRepository, DraftPickRepository |
| Player | ✅ 4 ports | ✅ 4 adaptateurs | ⚠️ PlayerRepository |
| Team | ✅ TeamRepositoryPort, TeamDomainRepositoryPort, TeamPlayerRepositoryPort, TeamScoreDeltaRepositoryPort | ✅ 2 adaptateurs | ⚠️ TeamRepository |
| Trade | ✅ TradeRepositoryPort, TradeDomainRepositoryPort | ✅ TradeRepositoryAdapter | ⚠️ TradeRepository |
| User | ❌ Pas de port domaine | ❌ Pas d'adaptateur | UserRepository (legacy seul) |
| Score | ❌ Pas de port domaine | ❌ Pas d'adaptateur | ScoreRepository (legacy seul) |
| Ingestion | Partiel (PrSnapshotQueryPort, ResolutionPort) | Partiel | PrSnapshotRepository (legacy) |

**Résumé :** 5/8 domaines migrés (Game, Draft, Player, Team, Trade). User/Score/Ingestion = dette restante.

---

## 6. Distribution des tests backend

| Catégorie | Fichiers |
|---|---|
| Tests de controllers | 31 |
| Tests de services | 107 |
| Tests de config/architecture | 34 |
| Tests de domaine | ~40 |
| Tests d'intégration | ~39 |
| **Total** | **251** |

---

## 7. Chaîne de dépendances des audits (prérequis = ce document)

Ce document est le prérequis des audits suivants :

| Ticket | Objet | Dépend de |
|---|---|---|
| TECH-011 | Inventaire exhaustif fichiers versionnés | TECH-010 |
| TECH-012 | Détection code mort / fichiers orphelins | TECH-010 |
| TECH-013 | Vérification contraintes taille classes | TECH-010 (§4) |
| TECH-014 | Audit SOLID (SRP/OCP/LSP/ISP/DIP) | TECH-010 |
| TECH-015 | Audit architecture hexagonale | TECH-010 (§5) |
| TECH-019 | Audit DRY / duplication | TECH-010 |
| TECH-020 | Analyse stack technique | TECH-010 |
| TECH-021 | Audit conventions / style / lint | TECH-010 |
| TECH-022 | Audit Loi de Demeter | TECH-010 |
| TECH-023 | Synthèse globale + roadmap | TECH-010, TECH-016, TECH-017, TECH-018, TECH-020 |

---

## 8. Points d'attention prioritaires

1. **DraftController 657 lignes + corruption UTF-8** — fichier le plus risqué du backend
2. **137 fichiers importent `model.*`** — dette de migration hexagonale majeure
3. **model/Game.java doublon** — confusion entre domain/game/model/Game.java (pur) et model/Game.java (JPA)
4. **Feature trades** : 5 fichiers >400 lignes dans un seul module
5. **login.component.ts 602 lignes** — violation CLAUDE.md la plus critique côté frontend
