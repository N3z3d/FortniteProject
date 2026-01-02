# Database Design - Fortnite Pronos Fantasy League

## Overview

Ce document decrit le schema de base de donnees pour l'application Fortnite Pronos Fantasy League.

**Objectif produit** : Construire une app de fantasy league Fortnite avec :
- Catalogue joueurs (top X par region + global, ingestion quotidienne)
- Games (ligues) independantes creees par les utilisateurs
- Draft (modes: Live Snake / Live Timer / Async avec resolution conflits)
- Teams par participant avec historique complet
- Trades 1v1 apres la draft (sans regles/quotas)
- Scoring : a la date D, score = somme des PR points des joueurs actuellement dans l'equipe

**Bases supportees** :
- PostgreSQL (production/dev)
- H2 (test)

---

## Entity Relationship Diagram (ERD)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              JOUEURS & PROFIL                                    │
└─────────────────────────────────────────────────────────────────────────────────┘

+------------------+       +-------------------+       +---------------------+
|     PLAYERS      |       |  PLAYER_ALIASES   |       | PLAYER_SOCIAL_LINKS |
+------------------+       +-------------------+       +---------------------+
| id (UUID) PK     |<------| player_id FK      |       | id (UUID) PK        |
| fortnite_id UK   |       | nickname_raw      |       | player_id FK ------>|
| tracker_id UK    |       | source (ENUM)     |       | platform (ENUM)     |
| admin_display_   |       | since             |       | handle              |
|   name UK NULL   |       | until             |       | url                 |
| status (ENUM)    |       +-------------------+       | added_at            |
| last_seen_at     |                                   +---------------------+
| created_at       |
| updated_at       |       +---------------------+     +---------------------+
+------------------+       | PLAYER_ORG_HISTORY  |     | PLAYER_INPUT_HISTORY|
        |                  +---------------------+     +---------------------+
        |                  | id (UUID) PK        |     | id (UUID) PK        |
        |                  | player_id FK ------>|     | player_id FK ------>|
        |                  | org_name            |     | input_method (ENUM) |
        |                  | since / until       |     | since / until       |
        |                  +---------------------+     +---------------------+
        |
        |                  +----------------------+    +------------------------+
        |                  | PLAYER_COUNTRY_      |    | PLAYER_DISPLAY_NAME_   |
        |                  |   HISTORY            |    |   HISTORY (audit)      |
        |                  +----------------------+    +------------------------+
        |                  | id (UUID) PK         |    | id (UUID) PK           |
        +----------------->| player_id FK         |    | player_id FK --------->|
                           | country_code CHAR(2) |    | old/new_display_name   |
                           | since / until        |    | changed_at / changed_by|
                           +----------------------+    +------------------------+


┌─────────────────────────────────────────────────────────────────────────────────┐
│                           PR SNAPSHOTS (quotidiens)                              │
└─────────────────────────────────────────────────────────────────────────────────┘

+------------------+       +---------------------+     +---------------------+
|   PR_SNAPSHOTS   |       |   INGESTION_RUNS    |     | PR_INGESTION_TARGETS|
+------------------+       +---------------------+     +---------------------+
| player_id FK     |------>| id (UUID) PK        |     | id (UUID) PK        |
| region (ENUM)    |       | source (ENUM)       |     | region (ENUM)       |
| snapshot_date    |       | started_at          |     | top_n INT           |
| points INT       |       | finished_at         |     | enabled BOOLEAN     |
| rank INT         |       | status (ENUM)       |     | effective_from DATE |
| collected_at     |       | total_rows_written  |     | effective_to DATE   |
| run_id FK -------|------>| error_message       |     +---------------------+
+------------------+       +---------------------+
  UK(player_id, region, snapshot_date)


┌─────────────────────────────────────────────────────────────────────────────────┐
│                         FANTASY LEAGUE (Games, Teams, Draft)                     │
└─────────────────────────────────────────────────────────────────────────────────┘

+------------------+       +---------------------+       +------------------+
|      USERS       |       |       GAMES         |       |      TEAMS       |
+------------------+       +---------------------+       +------------------+
| id (UUID) PK     |<---+  | id (UUID) PK        |<------| id (UUID) PK     |
| username UK      |    |  | creator_id FK ------+       | game_id FK       |
| email UK         |    |  | name                |       | owner_id FK ---->|
| password         |    |  | description         |       | name             |
| role (ENUM)      |    |  | status (ENUM)       |       +------------------+
| current_season   |    |  | invitation_code UK  |               |
+------------------+    |  | draft_type (ENUM)   |               |
        |               |  | draft_timer_seconds |               v
        |               |  | async_window_hours  |       +----------------------+
        |               |  | created_at          |       | TEAM_ROSTER_HISTORY  |
        |               |  | rules_json JSONB    |       +----------------------+
        |               |  +---------------------+       | id (UUID) PK         |
        |               |          |                     | game_id FK           |
        |               |          |1                    | team_id FK           |
        |               |          v N                   | player_id FK ------->|
        |               |  +---------------------+       | region_slot (ENUM)   |
        |               |  | GAME_PARTICIPANTS   |       | since / until        |
        |               |  +---------------------+       | acquisition_type     |
        |               +--| user_id FK          |       | tranche_at_acquisition|
        |                  | game_id FK          |       | acquisition_ref_id   |
        +----------------->| joined_at           |       +----------------------+
                           | draft_order         |         UK(game_id, player_id) WHERE until IS NULL
                           +---------------------+


┌─────────────────────────────────────────────────────────────────────────────────┐
│                                    DRAFT                                         │
└─────────────────────────────────────────────────────────────────────────────────┘

+------------------+       +-------------------+       +------------------------+
|     DRAFTS       |       |   DRAFT_PICKS     |       | DRAFT_ASYNC_WINDOWS    |
+------------------+       +-------------------+       +------------------------+
| id (UUID) PK     |<------| id (UUID) PK      |       | id (UUID) PK           |
| game_id FK UK    |       | draft_id FK       |       | draft_id FK ---------->|
| status (ENUM)    |       | participant_id FK |       | window_no INT          |
| current_round    |       | player_id FK      |       | region_slot (ENUM)     |
| current_pick     |       | region_slot (ENUM)|       | opens_at / closes_at   |
| created_at       |       | round INT         |       | status (ENUM)          |
| started_at       |       | pick_number INT   |       +------------------------+
| finished_at      |       | tranche_at_pick   |
| updated_at       |       | picked_at         |       +------------------------+
+------------------+       | pick_source (ENUM)|       | DRAFT_ASYNC_SELECTIONS |
                           +-------------------+       +------------------------+
                             UK(draft_id, player_id)   | id (UUID) PK           |
                                                       | draft_id FK            |
                                                       | participant_id FK      |
                                                       | window_no / region_slot|
                                                       | player_id FK           |
                                                       | submitted_at           |
                                                       +------------------------+


┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   TRADES                                         │
└─────────────────────────────────────────────────────────────────────────────────┘

+------------------+
|     TRADES       |
+------------------+
| id (UUID) PK     |
| game_id FK       |
| from_team_id FK  |
| to_team_id FK    |
| player_out_id FK |
| player_in_id FK  |
| status (ENUM)    |
| proposed_at      |
| responded_at     |
+------------------+
```

---

## Enums

```sql
-- Regions pour PR snapshots (inclut GLOBAL)
CREATE TYPE pr_region AS ENUM ('EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME', 'GLOBAL');

-- Regions pour draft (GLOBAL non draftable)
CREATE TYPE draft_region_slot AS ENUM ('EU', 'NAC', 'NAW', 'BR', 'ASIA', 'OCE', 'ME');

-- Types de draft
CREATE TYPE draft_type AS ENUM ('LIVE_SNAKE', 'LIVE_LINEAR_TIMER', 'ASYNC_RANDOM_RESOLVE');

-- Status des games
CREATE TYPE game_status AS ENUM ('CREATING', 'DRAFTING', 'ACTIVE', 'FINISHED', 'CANCELLED');

-- Status des drafts
CREATE TYPE draft_status AS ENUM ('PENDING', 'RUNNING', 'PAUSED', 'FINISHED', 'CANCELLED');

-- Status des trades
CREATE TYPE trade_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED');

-- Types d'acquisition dans roster
CREATE TYPE acquisition_type AS ENUM ('DRAFT', 'TRADE', 'JOIN_LATE', 'ADMIN');

-- Status joueur (manuel)
CREATE TYPE player_status AS ENUM ('ACTIVE', 'INACTIVE');

-- Source des alias
CREATE TYPE alias_source AS ENUM ('OFFICIAL', 'TRACKER');

-- Source des picks
CREATE TYPE pick_source AS ENUM ('USER', 'AUTO', 'ASYNC_RESOLVE');

-- Input method
CREATE TYPE input_method AS ENUM ('KBM', 'CONTROLLER', 'HYBRID', 'UNKNOWN');

-- Social platforms
CREATE TYPE social_platform AS ENUM ('TWITCH', 'YOUTUBE', 'X', 'INSTAGRAM', 'TIKTOK', 'DISCORD');
```

---

## Table Details

### PLAYERS
Joueurs Fortnite professionnels (identite stable).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, auto-gen | Identifiant unique interne |
| fortnite_id | VARCHAR(255) | UNIQUE, NOT NULL | ID officiel Epic/Fortnite (stable malgre changements pseudo) |
| tracker_id | VARCHAR(255) | UNIQUE, NOT NULL | ID stable FortniteTracker |
| admin_display_name | VARCHAR(255) | UNIQUE, NULL | Surcouche affichage admin (prioritaire) |
| status | player_status | NOT NULL, DEFAULT 'ACTIVE' | Status manuel (ACTIVE/INACTIVE) |
| last_seen_at | TIMESTAMP | NULL | Derniere apparition dans un top ingere |
| created_at | TIMESTAMP | NOT NULL | Date creation |
| updated_at | TIMESTAMP | NOT NULL | Derniere modification locale |

**Note importante** : La region n'est PAS dans cette table car un joueur peut avoir des PR multi-regions + global.

### PLAYER_ALIASES
Historique des pseudos (avec since/until).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| player_id | UUID | FK -> PLAYERS | Joueur |
| nickname_raw | VARCHAR(255) | NOT NULL | Pseudo brut (aucun traitement) |
| source | alias_source | NOT NULL | OFFICIAL ou TRACKER |
| since | TIMESTAMP | NOT NULL | Date debut |
| until | TIMESTAMP | NULL | Date fin (NULL si actif) |

**Contrainte** : Un seul alias actif par player/source.

### PLAYER_SOCIAL_LINKS
Liens reseaux sociaux des joueurs.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| player_id | UUID | FK -> PLAYERS | Joueur |
| platform | social_platform | NOT NULL | Plateforme |
| handle | VARCHAR(255) | NOT NULL | Identifiant sur la plateforme |
| url | VARCHAR(500) | NULL | URL complete |
| added_at | TIMESTAMP | NOT NULL | Date ajout |
| added_by | UUID | NULL | Admin/user qui a ajoute |

**Contrainte** : UNIQUE(player_id, platform, handle)

### PLAYER_ORG_HISTORY
Historique des organisations/equipes esport.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| player_id | UUID | FK -> PLAYERS | Joueur |
| org_name | VARCHAR(255) | NOT NULL | Nom org (ou "FreeAgent") |
| since | TIMESTAMP | NOT NULL | Date debut |
| until | TIMESTAMP | NULL | Date fin |

### PLAYER_INPUT_HISTORY
Historique des methodes d'input (clavier/manette).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| player_id | UUID | FK -> PLAYERS | Joueur |
| input_method | input_method | NOT NULL | KBM, CONTROLLER, HYBRID, UNKNOWN |
| since | TIMESTAMP | NOT NULL | Date debut |
| until | TIMESTAMP | NULL | Date fin |

### PLAYER_COUNTRY_HISTORY
Historique des nationalites.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| player_id | UUID | FK -> PLAYERS | Joueur |
| country_code | CHAR(2) | NOT NULL | Code ISO pays |
| since | TIMESTAMP | NOT NULL | Date debut |
| until | TIMESTAMP | NULL | Date fin |

### PLAYER_DISPLAY_NAME_HISTORY
Audit des changements de display name admin.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| player_id | UUID | FK -> PLAYERS | Joueur |
| old_display_name | VARCHAR(255) | NULL | Ancien nom |
| new_display_name | VARCHAR(255) | NULL | Nouveau nom |
| changed_at | TIMESTAMP | NOT NULL | Date changement |
| changed_by | UUID | NULL | Admin qui a modifie |

### PR_SNAPSHOTS
Points/Ranking quotidiens par region (multi-regions + global).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| player_id | UUID | FK -> PLAYERS | Joueur |
| region | pr_region | NOT NULL | EU, NAC, NAW, BR, ASIA, OCE, ME, GLOBAL |
| snapshot_date | DATE | NOT NULL | Date du snapshot |
| points | INTEGER | NOT NULL | PR points |
| rank | INTEGER | NOT NULL | Classement |
| collected_at | TIMESTAMP | NOT NULL | Timestamp collecte |
| run_id | UUID | FK -> INGESTION_RUNS, NULL | Run d'ingestion |

**Contrainte** : UNIQUE(player_id, region, snapshot_date)

### INGESTION_RUNS
Tracabilite des imports de donnees.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| source | VARCHAR(50) | NOT NULL | Ex: FORTNITE_TRACKER |
| started_at | TIMESTAMP | NOT NULL | Debut |
| finished_at | TIMESTAMP | NULL | Fin |
| status | VARCHAR(20) | NOT NULL | SUCCESS, PARTIAL, FAILED |
| total_rows_written | INTEGER | NULL | Lignes ecrites |
| error_message | TEXT | NULL | Message erreur |

### PR_INGESTION_TARGETS
Configuration des tops a ingerer par region.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| region | pr_region | NOT NULL | Region |
| top_n | INTEGER | NOT NULL | Nombre de joueurs a recuperer |
| enabled | BOOLEAN | NOT NULL | Actif |
| effective_from | DATE | NOT NULL | Date debut validite |
| effective_to | DATE | NULL | Date fin validite |

### USERS
Utilisateurs de l'application (pronostiqueurs).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| username | VARCHAR(50) | UNIQUE, NOT NULL | Nom d'utilisateur |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Email |
| password | VARCHAR(255) | NOT NULL | Mot de passe hashe (BCrypt) |
| role | VARCHAR(20) | NOT NULL | USER, ADMIN, SPECTATOR |
| current_season | INTEGER | NOT NULL | Saison courante (ex: 2025) |

### GAMES
Parties de fantasy league.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| creator_id | UUID | FK -> USERS | Createur |
| name | VARCHAR(255) | NOT NULL | Nom de la game |
| description | VARCHAR(500) | NULL | Description |
| status | game_status | NOT NULL | CREATING, DRAFTING, ACTIVE, FINISHED, CANCELLED |
| invitation_code | VARCHAR(10) | UNIQUE | Code d'invitation |
| draft_type | draft_type | NOT NULL | LIVE_SNAKE, LIVE_LINEAR_TIMER, ASYNC_RANDOM_RESOLVE |
| draft_timer_seconds | INTEGER | NULL | Timer par pick (mode live) |
| async_window_hours | INTEGER | NULL | Duree fenetre (mode async) |
| created_at | TIMESTAMP | NOT NULL | Date creation |
| rules_json | JSONB | NULL | Regles flexibles sans migration |

### GAME_PARTICIPANTS
Participants aux games.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| game_id | UUID | FK -> GAMES | Game |
| user_id | UUID | FK -> USERS | Utilisateur |
| joined_at | TIMESTAMP | NOT NULL | Date d'adhesion |
| draft_order | INTEGER | NULL | Ordre de draft |

**Contraintes** :
- UNIQUE(game_id, user_id)
- UNIQUE(game_id, draft_order) si draft_order utilise

### TEAMS
Equipes des pronostiqueurs.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| game_id | UUID | FK -> GAMES | Game associee |
| owner_id | UUID | FK -> USERS | Proprietaire |
| name | VARCHAR(255) | NOT NULL | Nom de l'equipe |

### TEAM_ROSTER_HISTORY
Historique du roster (since/until pour chaque joueur).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| game_id | UUID | FK -> GAMES | Game (pour contraintes) |
| team_id | UUID | FK -> TEAMS | Equipe |
| player_id | UUID | FK -> PLAYERS | Joueur |
| region_slot | draft_region_slot | NOT NULL | Slot region (EU, NAC, etc. - pas GLOBAL) |
| since | TIMESTAMP | NOT NULL | Date d'ajout |
| until | TIMESTAMP | NULL | Date de retrait (NULL si actif) |
| acquisition_type | acquisition_type | NOT NULL | DRAFT, TRADE, JOIN_LATE, ADMIN |
| tranche_at_acquisition | INTEGER | NULL | Tranche au moment de l'acquisition |
| acquisition_ref_id | UUID | NULL | Reference (draft_pick_id ou trade_id) |

**Contrainte critique** :
```sql
-- Un joueur actif dans une seule team par game
CREATE UNIQUE INDEX uq_game_player_active
ON team_roster_history(game_id, player_id)
WHERE until IS NULL;
```

### DRAFTS
Sessions de draft.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| game_id | UUID | FK -> GAMES, UNIQUE | Game associee |
| status | draft_status | NOT NULL | PENDING, RUNNING, PAUSED, FINISHED, CANCELLED |
| current_round | INTEGER | NOT NULL | Round actuel |
| current_pick | INTEGER | NOT NULL | Pick actuel |
| created_at | TIMESTAMP | NOT NULL | Date creation |
| started_at | TIMESTAMP | NULL | Date debut |
| finished_at | TIMESTAMP | NULL | Date fin |
| updated_at | TIMESTAMP | NOT NULL | Derniere mise a jour |

### DRAFT_PICKS
Picks effectues pendant le draft.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| draft_id | UUID | FK -> DRAFTS | Draft |
| participant_id | UUID | FK -> GAME_PARTICIPANTS | Participant |
| player_id | UUID | FK -> PLAYERS | Joueur selectionne |
| region_slot | draft_region_slot | NOT NULL | Slot region du pick |
| round | INTEGER | NOT NULL | Numero du round |
| pick_number | INTEGER | NOT NULL | Numero du pick |
| tranche_at_pick | INTEGER | NULL | Tranche au moment du pick |
| picked_at | TIMESTAMP | NOT NULL | Date/heure du pick |
| pick_source | pick_source | NOT NULL | USER, AUTO, ASYNC_RESOLVE |

**Contrainte** : UNIQUE(draft_id, player_id) - pas de double pick

### DRAFT_ASYNC_WINDOWS
Fenetres pour le mode draft async.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| draft_id | UUID | FK -> DRAFTS | Draft |
| window_no | INTEGER | NOT NULL | Numero de fenetre |
| region_slot | draft_region_slot | NOT NULL | Slot region |
| opens_at | TIMESTAMP | NOT NULL | Ouverture |
| closes_at | TIMESTAMP | NOT NULL | Fermeture |
| status | VARCHAR(20) | NOT NULL | OPEN, CLOSED, RESOLVED |

### DRAFT_ASYNC_SELECTIONS
Selections soumises en mode async.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| draft_id | UUID | FK -> DRAFTS | Draft |
| participant_id | UUID | FK -> GAME_PARTICIPANTS | Participant |
| window_no | INTEGER | NOT NULL | Numero de fenetre |
| region_slot | draft_region_slot | NOT NULL | Slot region |
| player_id | UUID | FK -> PLAYERS | Joueur souhaite |
| submitted_at | TIMESTAMP | NOT NULL | Date soumission |

**Contrainte** : UNIQUE(draft_id, participant_id, window_no, region_slot, player_id)

### TRADES
Echanges de joueurs 1v1.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| game_id | UUID | FK -> GAMES | Game |
| from_team_id | UUID | FK -> TEAMS | Equipe initiatrice |
| to_team_id | UUID | FK -> TEAMS | Equipe destinataire |
| player_out_id | UUID | FK -> PLAYERS | Joueur sortant |
| player_in_id | UUID | FK -> PLAYERS | Joueur entrant |
| status | trade_status | NOT NULL | PENDING, ACCEPTED, REJECTED, CANCELLED |
| proposed_at | TIMESTAMP | NOT NULL | Date proposition |
| responded_at | TIMESTAMP | NULL | Date reponse |

**Regles metier** :
- Trades uniquement 1v1
- Uniquement apres la draft
- Pas de contre-offres (oui/non)
- Pas de deadline
- Pas de validation tranche/quota apres draft

---

## Indexes

```sql
-- Players
CREATE INDEX idx_players_fortnite_id ON players(fortnite_id);
CREATE INDEX idx_players_tracker_id ON players(tracker_id);
CREATE INDEX idx_players_status ON players(status);
CREATE INDEX idx_players_last_seen ON players(last_seen_at);

-- PR Snapshots (requetes frequentes)
CREATE INDEX idx_pr_snapshots_region_date_rank ON pr_snapshots(region, snapshot_date, rank);
CREATE INDEX idx_pr_snapshots_player_region_date ON pr_snapshots(player_id, region, snapshot_date);

-- Games
CREATE INDEX idx_games_status ON games(status);
CREATE INDEX idx_games_creator ON games(creator_id);

-- Teams
CREATE INDEX idx_teams_game ON teams(game_id);
CREATE INDEX idx_teams_owner ON teams(owner_id);

-- Roster History
CREATE INDEX idx_roster_team ON team_roster_history(team_id);
CREATE INDEX idx_roster_player ON team_roster_history(player_id);
CREATE INDEX idx_roster_game ON team_roster_history(game_id);
CREATE INDEX idx_roster_active ON team_roster_history(team_id) WHERE until IS NULL;

-- Trades
CREATE INDEX idx_trades_game ON trades(game_id);
CREATE INDEX idx_trades_status ON trades(status);
```

---

## Data Flow

### 1. Ingestion quotidienne PR
```
Config PR_INGESTION_TARGETS (top_n par region)
    |
    v
FortniteTracker API --> INGESTION_RUNS (trace)
    |
    v
Upsert PLAYERS (par fortnite_id/tracker_id)
    |
    v
Insert PR_SNAPSHOTS (points, rank, date)
    |
    v
Update players.last_seen_at
```

### 2. Creation de Game
```
User -> createGame()
    |
    +-> GAMES (status=CREATING, draft_type, rules_json)
    |
    +-> GAME_PARTICIPANTS (createur)
```

### 3. Draft
```
Game.startDraft() -> DRAFTS (status=RUNNING)
    |
    +-- Mode Live Snake/Timer:
    |       draft_picks tour par tour
    |
    +-- Mode Async:
            DRAFT_ASYNC_WINDOWS (fenetres)
            DRAFT_ASYNC_SELECTIONS (soumissions)
            Resolution conflits aleatoire
            -> draft_picks
    |
    v
Fin draft: TEAMS + TEAM_ROSTER_HISTORY (acquisition_type=DRAFT)
```

### 4. Trades
```
Proposition -> TRADES (status=PENDING)
    |
    v (si ACCEPTED)
TEAM_ROSTER_HISTORY:
    - until=now() pour joueurs sortants
    - nouvelles lignes (since=now(), until=NULL, acquisition_type=TRADE)
```

### 5. Scoring (team score a date D)
```
Pour une team a la date D:
    1. Recuperer joueurs actifs: since <= D AND (until IS NULL OR until > D)
    2. Pour chaque joueur, recuperer pr_snapshots du jour D sur sa region_slot
    3. Score team = SUM(points)

Note: Les joueurs vires (until <= D) ne comptent plus.
```

---

## Regles Metier Importantes

1. **Disponibilite joueur** : un joueur est "pris" dans une game si `team_roster_history` contient une ligne active (`until IS NULL`) pour ce player dans cette game.

2. **Region_slot fixee au draft** : pas de GLOBAL draftable.

3. **Trades sans regles** : pas de validation tranche/quota apres draft.

4. **Historique partout** : since/until pour alias/org/input/country/roster.

5. **Status ACTIVE/INACTIVE** : manuel (admin).

6. **Pas de bench** : "bench" = joueur vire (on met `until`), on garde l'historique.

---

## Risque a Anticiper

**"Top X" cree des trous de donnees** : Si on ingere seulement "top X", un joueur dans une team peut sortir du top X → plus de snapshots → score fige/incorrect.

**Recommandation** : Ingestion quotidienne = top X + watchlist = tous les joueurs presents dans des games actives (lookup par ID).

---

## Politique Snapshot Manquant

A trancher:
- **Carry-forward** : utiliser le dernier snapshot avant la date D
- **Zero** : 0 points si pas de snapshot
- **Missing** : indiquer donnee manquante

---

## Migration Strategy

- Flyway/Liquibase pour versioning des schemas
- H2 en mode PostgreSQL pour compatibilite test/prod
- DDL auto avec Hibernate en dev, validate en prod
