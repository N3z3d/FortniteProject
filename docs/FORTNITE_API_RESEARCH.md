# Recherche API Fortnite - Donnees disponibles et recommandation

> **Ticket**: JIRA-FEAT-003
> **Date**: 2026-02-08
> **Objectif**: Identifier les APIs Fortnite disponibles pour alimenter le catalogue de joueurs du projet Fortnite Pronos.

---

## Table des matieres

1. [Resume executif](#1-resume-executif)
2. [APIs analysees](#2-apis-analysees)
3. [Comparatif detaille](#3-comparatif-detaille)
4. [Donnees recuperables par API](#4-donnees-recuperables-par-api)
5. [Recommandation](#5-recommandation)
6. [Plan d'integration](#6-plan-dintegration)
7. [Risques et limitations](#7-risques-et-limitations)

---

## 1. Resume executif

**Constat** : Epic Games ne fournit pas d'API REST publique officielle pour les statistiques joueurs Fortnite. L'ecosysteme repose sur :
- Des **APIs communautaires** qui wrappent les endpoints internes d'Epic Games (Fortnite-API.com, FortniteAPI.io, MyFortniteStats).
- Des **APIs de tracking** qui aggregent les donnees (FortniteTracker / Tracker.gg).
- Des **endpoints internes Epic** non documentes officiellement mais reverses par la communaute.

**Recommandation** : Utiliser **Fortnite-API.com** comme API principale (gratuite, stable, API key simple) + **FortniteTracker** en complement pour les classements competitifs.

---

## 2. APIs analysees

### 2.1 Fortnite-API.com

| Critere | Detail |
|---------|--------|
| **URL** | https://fortnite-api.com |
| **Type** | API communautaire (wrapper endpoints Epic) |
| **Auth** | API key gratuite (inscription via Discord) |
| **Rate limit** | Non documente publiquement, usage raisonnable |
| **Statut** | Active, maintenue regulierement |
| **Langues** | Reponses multi-langues (en, fr, de, es, pt, etc.) |

**Endpoints disponibles** :

| Endpoint | Methode | Description |
|----------|---------|-------------|
| `/v2/stats/br/v2` | GET | Stats Battle Royale par nom ou account ID |
| `/v2/cosmetics/br` | GET | Tous les cosmetiques (skins, emotes, pickaxes, etc.) |
| `/v2/cosmetics/br/{id}` | GET | Cosmetique par ID |
| `/v2/cosmetics/br/new` | GET | Nouveaux cosmetiques |
| `/v2/cosmetics/br/search` | GET | Recherche de cosmetiques |
| `/v2/shop/br` | GET | Boutique du jour (MAJ auto 00:00 UTC) |
| `/v2/shop/br/combined` | GET | Boutique combinee |
| `/v2/news` | GET | News BR + STW + Creative |
| `/v2/news/br` | GET | News Battle Royale |
| `/v2/playlists` | GET | Tous les modes de jeu |
| `/v2/playlists/{id}` | GET | Mode de jeu par ID |
| `/v2/map` | GET | Carte actuelle avec POIs |
| `/v2/banners` | GET | Tous les bannieres |
| `/v2/banners/colors` | GET | Couleurs de bannieres |
| `/v2/aes` | GET | Cles AES actuelles |
| `/v2/creatorcode` | GET | Recherche code createur |

**Donnees Stats joueur** (endpoint `/v2/stats/br/v2`) :

```
Parametres :
  - name (string) : nom du joueur
  - accountId (string) : ID Epic du joueur
  - timeWindow (string) : "season" ou "lifetime"
  - image (string) : "all", "keyboardMouse", "gamepad", "touch" (genere une image)

Reponse :
{
  "status": 200,
  "data": {
    "account": {
      "id": "account-id-uuid",
      "name": "PlayerName"
    },
    "battlePass": {
      "level": 100,
      "progress": 0
    },
    "image": "https://...",  // si ?image=all
    "stats": {
      "all": {                   // Tous les inputs combines
        "overall": {
          "score": 123456,
          "scorePerMin": 45.2,
          "scorePerMatch": 312.5,
          "wins": 150,
          "top3": 300,
          "top5": 450,
          "top6": 500,
          "top10": 700,
          "top12": 800,
          "top25": 1200,
          "kills": 5000,
          "killsPerMin": 1.2,
          "killsPerMatch": 4.5,
          "deaths": 3000,
          "kd": 1.67,
          "matches": 4000,
          "winRate": 3.75,
          "minutesPlayed": 50000,
          "playersOutlived": 120000,
          "lastModified": "2026-01-15T12:00:00Z"
        },
        "solo": { ... },        // Memes champs par mode
        "duo": { ... },
        "squad": { ... },
        "ltm": { ... }          // Modes a duree limitee
      },
      "keyboardMouse": { ... }, // Memes sous-categories
      "gamepad": { ... },
      "touch": { ... }
    }
  }
}
```

### 2.2 FortniteAPI.io

| Critere | Detail |
|---------|--------|
| **URL** | https://fortniteapi.io |
| **Type** | API communautaire |
| **Auth** | API key gratuite (10 000 req/jour) ou Premium (1M req/jour, 10$/mois) |
| **Rate limit** | 10 000/jour (gratuit), 1 000 000/jour (premium) |
| **Statut** | **FERME LE 31 MARS 2026** |
| **Documentation** | https://fortniteapi.io/docs/ (Swagger) |

**Endpoints disponibles** :

| Endpoint | Methode | Description |
|----------|---------|-------------|
| `/v1/stats` | GET | Stats joueur (par account ID) |
| `/v2/items/list` | GET | Tous les cosmetiques |
| `/v2/items/upcoming` | GET | Cosmetiques a venir |
| `/v1/shop` | GET | Boutique du jour |
| `/v3/challenges` | GET | Challenges avec recompenses |
| `/v1/events/list` | GET | Tournois/evenements par region |
| `/v1/events/window` | GET | Details session tournoi |
| `/v1/events/cumulative` | GET | Classement cumulatif tournoi (beta) |
| `/v1/events/replay` | GET | Replays tournoi (premium) |
| `/v1/seasons/list` | GET | Liste des saisons |
| `/v2/game/maps` | GET | Cartes (actuelle + historique) |
| `/v1/loot/list` | GET | Armes et stats |
| `/v1/vehicles` | GET | Vehicules |
| `/v1/creative/island` | GET | Iles Creative (par code) |
| `/v1/creative/featured` | GET | Iles mises en avant |

**Donnees tournois** (unique a cette API) :

```
GET /v1/events/list?region=EU&lang=fr

Reponse :
{
  "result": true,
  "events": [
    {
      "id": "epicgames_S34_FNCS...",
      "name": "FNCS Major 1",
      "beginTime": "2026-01-15T18:00:00Z",
      "endTime": "2026-01-15T21:00:00Z",
      "region": "EU",
      "platform": "PC",
      "sessions": [
        {
          "id": "session-uuid",
          "beginTime": "...",
          "endTime": "...",
          "payout": { ... },
          "rules": { "teamSize": 3, "matchLimit": 6 }
        }
      ]
    }
  ]
}
```

> **ATTENTION** : Cette API ferme le 31 mars 2026. Ne pas baser l'architecture dessus.

### 2.3 MyFortniteStats.com

| Critere | Detail |
|---------|--------|
| **URL** | https://myfortnitestats.com/api |
| **Type** | API communautaire specialisee stats |
| **Auth** | API key (inscription via Epic Games login) |
| **Rate limit** | Non documente |
| **Statut** | Active |

**Endpoints disponibles** :

| Endpoint | Methode | Description |
|----------|---------|-------------|
| `/api/search?username={name}&platform={platform}` | GET | Recherche joueur par nom |
| `/api/stats/{accountId}` | GET | Stats lifetime |
| `/api/all-data/{accountId}` | GET | Profil complet (stats + ranked + metadata) |
| `/api/lookup/{displayName}?platform={platform}` | GET | Resolution nom -> account ID |
| `/api/ranked-progress/{accountId}` | GET | Progression ranked (BR, Zero Build, Reload) |
| `/api/ranked-search?username={name}` | GET | Recherche + ranked en une requete |
| `/api/ranked-leaderboard?mode={mode}&limit={limit}&offset={offset}` | GET | Classement ranked global |
| `/api/top-ranked-players?limit={limit}` | GET | Top joueurs Unreal |
| `/api/leaderboard?stat={stat}&gamemode={mode}&page={page}` | GET | Classement par stat |
| `/api/top-stats-players?stat={stat}&limit={limit}` | GET | Top joueurs par stat |

**Points forts** : Endpoints ranked detailles, leaderboards multiples.

### 2.4 FortniteTracker / Tracker.gg

| Critere | Detail |
|---------|--------|
| **URL** | https://fortnitetracker.com |
| **API** | https://tracker.gg/developers |
| **Type** | Plateforme de tracking + API |
| **Auth** | API key gratuite (inscription) |
| **Rate limit** | Limites augmentables gratuitement sur demande |
| **Statut** | Active, reference communautaire |

**Endpoints disponibles** :

| Endpoint | Methode | Description |
|----------|---------|-------------|
| `/profile/{platform}/{epicNickname}` | GET | Profil joueur + stats lifetime |
| `/leaderboard` | GET | Classement global |
| **Power Rankings API** | | |
| `/fortnitepr/player/{accountId}` | GET | Power Ranking joueur par region |

**Donnees Power Rankings** :

```
GET /fortnitepr/player/{accountId}

Reponse :
{
  "accountId": "...",
  "displayName": "PlayerName",
  "region": "EU",
  "powerRanking": {
    "rank": 42,
    "points": 15234,
    "events": [
      {
        "eventId": "FNCS_Major_1",
        "placement": 5,
        "points": 3200,
        "date": "2026-01-15"
      }
    ]
  }
}
```

### 2.5 Endpoints internes Epic Games (non officiels)

| Critere | Detail |
|---------|--------|
| **Documentation** | https://github.com/LeleDerGrasshalmi/FortniteEndpointsDocumentation |
| **Type** | Endpoints reverses, non documentes officiellement |
| **Auth** | OAuth2 (client_credentials ou authorization_code) |
| **Statut** | Fonctionnels mais peuvent changer sans preavis |

**Services principaux** :

| Service | Base URL | Description |
|---------|----------|-------------|
| Account Service | `account-public-service-prod.ol.epicgames.com` | Auth OAuth2, lookup comptes |
| Stats Proxy | `fortnite-public-service-prod11.ol.epicgames.com` | Stats joueurs (statsv2) |
| Events Service | `events-public-service-prod.ol.epicgames.com` | Tournois, classements |
| Friends Service | `friends-public-service-prod.ol.epicgames.com` | Liste d'amis |
| Party Service | `party-service-prod.ol.epicgames.com` | Groupes/equipes |

**Endpoint Stats (statsv2)** :

```
POST /fortnite/api/statsv2/query
Authorization: Bearer {access_token}

Body: {
  "appId": "Fortnite",
  "startDate": 0,
  "endDate": 9999999999,
  "owners": ["account-id-1", "account-id-2"]
}

Reponse : statistiques brutes avec champs :
  - br_placetop1_*     (wins)
  - br_kills_*         (kills)
  - br_matchesplayed_* (matches)
  - br_placetop10_*    (top 10)
  - br_placetop25_*    (top 25)
  - br_score_*         (score)
  - br_minutesplayed_* (temps de jeu)
  - br_playersoutlived_* (joueurs survecus)

Suffixes : _keyboardmouse, _gamepad, _touch
```

> **RISQUE** : Ces endpoints peuvent changer sans preavis. A utiliser en dernier recours ou via un wrapper (Fortnite-API.com).

---

## 3. Comparatif detaille

| Critere | Fortnite-API.com | FortniteAPI.io | MyFortniteStats | FortniteTracker | Epic (interne) |
|---------|:---:|:---:|:---:|:---:|:---:|
| **Stats joueur** | Oui | Oui | Oui | Oui | Oui |
| **Recherche par nom** | Oui | Non (account ID) | Oui | Oui | Oui |
| **Stats ranked** | Non | Non | Oui | Non | Partiel |
| **Tournois/FNCS** | Non | Oui | Non | Power Rankings | Oui |
| **Cosmetiques** | Oui (complet) | Oui | Non | Non | Non |
| **Boutique** | Oui | Oui | Non | Non | Non |
| **Carte/POIs** | Oui | Oui | Non | Non | Non |
| **Image stats auto** | Oui | Non | Non | Non | Non |
| **Multi-langue** | Oui | Oui | Non | Non | Non |
| **Gratuit** | Oui | Oui (10K/j) | Oui | Oui | Oui |
| **Perennite** | Bonne | **Ferme 03/2026** | Incertaine | Bonne | Incertaine |
| **Auth simple** | API key header | API key header | API key header | API key header | OAuth2 complexe |
| **Rate limit** | Raisonnable | 10K-1M/jour | ? | Augmentable | Strict |

---

## 4. Donnees recuperables par API

### 4.1 Donnees joueur (toutes APIs)

| Donnee | Fortnite-API.com | MyFortniteStats | FortniteTracker |
|--------|:---:|:---:|:---:|
| Nom d'affichage (Display Name) | Oui | Oui | Oui |
| Account ID Epic | Oui | Oui | Oui |
| Plateforme (PC/PS/Xbox) | Oui | Oui | Oui |
| Niveau Battle Pass | Oui | Non | Non |

### 4.2 Statistiques Battle Royale

| Statistique | Champ API |
|-------------|-----------|
| Victoires (wins) | `wins` / `placetop1` |
| Kills | `kills` |
| Parties jouees | `matches` / `matchesplayed` |
| K/D ratio | `kd` (calcule) |
| Win rate | `winRate` (calcule) |
| Score | `score` |
| Score/min | `scorePerMin` |
| Score/match | `scorePerMatch` |
| Kills/min | `killsPerMin` |
| Kills/match | `killsPerMatch` |
| Deces | `deaths` |
| Top 3/5/6/10/12/25 | `top3`, `top5`, `top6`, `top10`, `top12`, `top25` |
| Minutes jouees | `minutesPlayed` |
| Joueurs survecus | `playersOutlived` |
| Derniere MAJ | `lastModified` |

**Ventilation par** :
- **Mode** : Solo, Duo, Squad, LTM
- **Input** : Clavier/Souris, Manette, Tactile
- **Periode** : Lifetime, Saison en cours

### 4.3 Statistiques Ranked (MyFortniteStats uniquement)

| Donnee | Description |
|--------|-------------|
| Division actuelle | Bronze, Silver, Gold, Platinum, Diamond, Elite, Champion, Unreal |
| Progression | Points dans la division |
| Modes ranked | BR, Zero Build, Reload, Pimlico |
| Classement global | Position dans le leaderboard |

### 4.4 Donnees competitives / Tournois

| Donnee | FortniteAPI.io | FortniteTracker |
|--------|:---:|:---:|
| Liste des tournois | Oui | Oui |
| Nom du tournoi | Oui | Oui |
| Region | Oui | Oui |
| Dates (debut/fin) | Oui | Oui |
| Regles (team size, match limit) | Oui | Non |
| Cashprize/payout | Oui | Non |
| Resultats/classement | Oui (beta) | Power Rankings |
| Replays | Oui (premium) | Non |
| Power Ranking joueur | Non | Oui |
| Historique placements | Non | Oui |

### 4.5 Donnees cosmetiques (Fortnite-API.com)

| Donnee | Description |
|--------|-------------|
| ID cosmetique | Identifiant unique |
| Nom | Multi-langue |
| Description | Multi-langue |
| Type | Skin, Emote, Pickaxe, Back Bling, Glider, Wrap, etc. |
| Rarete | Common, Uncommon, Rare, Epic, Legendary, Mythic |
| Set | Collection/serie |
| Introduction | Chapitre + saison d'introduction |
| Images | Icon, featured, background, etc. |
| Variantes | Styles alternatifs |
| Tags | Categorisation |

---

## 5. Recommandation

### Architecture recommandee : Fortnite-API.com (principal) + FortniteTracker (complement)

```
                    +-------------------+
                    |   Notre Backend   |
                    |  (Spring Boot)    |
                    +--------+----------+
                             |
              +--------------+--------------+
              |                             |
    +---------v----------+       +----------v---------+
    | Fortnite-API.com   |       | FortniteTracker    |
    | (stats + cosmetics)|       | (Power Rankings)   |
    +--------------------+       +--------------------+
              |                             |
              +-------------+---------------+
                            |
                   +--------v--------+
                   | Epic Games      |
                   | (source donnees)|
                   +-----------------+
```

### Justification

| Critere | Choix |
|---------|-------|
| **Stats joueur** | Fortnite-API.com - recherche par nom, reponse complete, image auto |
| **Cosmetiques** | Fortnite-API.com - catalogue exhaustif, multi-langue |
| **Competitions** | FortniteTracker Power Rankings API - reference competitif |
| **Ranked** | MyFortniteStats en option si besoin ranked detaille |
| **Perennite** | Fortnite-API.com est stable ; FortniteAPI.io ferme en mars 2026 |
| **Simplicite** | API key simple vs OAuth2 complexe (endpoints Epic internes) |
| **Cout** | Gratuit pour les deux |

### Pourquoi PAS FortniteAPI.io ?
- **Ferme le 31 mars 2026**. Impossible de baser l'architecture dessus.
- Donnees tournois uniques mais temporaires.

### Pourquoi PAS les endpoints Epic internes ?
- Auth OAuth2 complexe (client credentials + access token).
- Non documentes officiellement, peuvent changer sans preavis.
- Les APIs communautaires wrappent deja ces endpoints de facon stable.

---

## 6. Plan d'integration

### Phase 1 : Service d'integration Fortnite-API.com

```java
// Port (domain)
public interface FortniteApiPort {
    Optional<FortnitePlayerStats> fetchPlayerStats(String playerName);
    Optional<FortnitePlayerStats> fetchPlayerStatsById(String accountId);
    List<FortniteCosmetic> fetchAllCosmetics();
}

// Adapter (infrastructure)
@Component
public class FortniteApiAdapter implements FortniteApiPort {
    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://fortnite-api.com/v2";
    // API key en header : Authorization: {apiKey}
}
```

### Phase 2 : Modele domain Player enrichi

```java
public final class Player {
    private final String id;              // ID interne
    private final String epicAccountId;   // ID Epic Games
    private final String displayName;     // Nom Fortnite
    private final String platform;        // PC, PSN, XBL
    private final String region;          // EU, NAE, NAW, etc.
    private final PlayerStats lifetimeStats;
    private final PlayerStats seasonStats;
    private final Instant lastSyncedAt;
}

public final class PlayerStats {
    private final int wins;
    private final int kills;
    private final int matches;
    private final double kd;
    private final double winRate;
    private final int top10;
    private final int top25;
    private final int minutesPlayed;
}
```

### Phase 3 : Synchronisation

```
1. Import initial : recherche joueurs pro par nom -> stockage BDD
2. Sync periodique : @Scheduled toutes les 6h -> MAJ stats depuis API
3. Import a la demande : bouton "Importer depuis Fortnite" dans le frontend
```

### Cles API necessaires

| API | Comment obtenir |
|-----|-----------------|
| Fortnite-API.com | Inscription via Discord sur https://dash.fortnite-api.com |
| FortniteTracker | Inscription sur https://fortnitetracker.com/site-api |

---

## 7. Risques et limitations

| Risque | Impact | Mitigation |
|--------|--------|------------|
| Fermeture API communautaire | Perte d'acces aux donnees | Architecture hexagonale (port/adapter) pour changer de provider facilement |
| Rate limiting | Blocage temporaire | Cache local (Redis ou in-memory) + TTL 1h sur les stats |
| Joueur introuvable | Nom incorrect ou compte prive | Fallback : creation manuelle + message explicite |
| Changement format reponse | Parsing echoue | Tests d'integration + monitoring des appels |
| Stats privees (Epic setting) | Pas de donnees pour certains joueurs | Gerer le cas "stats privees" dans le domain model |
| Latence API externe | UX degradee | Appels async + cache + indicateur de chargement |
| Pas de donnees historiques competitions via Fortnite-API.com | Catalogue incomplet pour le competitif | Complement FortniteTracker Power Rankings |

---

## Annexe : URLs de reference

- Fortnite-API.com : https://fortnite-api.com
- Fortnite-API Dashboard : https://dash.fortnite-api.com
- FortniteAPI.io (ferme 03/2026) : https://fortniteapi.io
- FortniteAPI.io Docs : https://fortniteapi.io/docs/
- MyFortniteStats API : https://myfortnitestats.com/api
- FortniteTracker : https://fortnitetracker.com
- Tracker.gg Developers : https://tracker.gg/developers
- Epic Endpoints (unofficial) : https://github.com/LeleDerGrasshalmi/FortniteEndpointsDocumentation
- Epic Auth Docs : https://dev.epicgames.com/docs/web-api-ref/authentication
