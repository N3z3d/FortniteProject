# Fortnite API — Capacités & Mapping Projet

> Analysé le 2026-03-04
> Clé configurée dans `.env` (gitignorée) — variable `FORTNITE_API_KEY`
> Base URL : `https://fortnite-api.com`

---

## 1. Vue d'ensemble

Le projet utilise **deux sources de données Fortnite** complémentaires :

| Source | Ce qu'elle fournit | Accès |
|---|---|---|
| **fortnite-api.com** (clé API) | Stats BR casualles d'un joueur nommé | REST API gratuite (clé gratuite) |
| **FortniteTracker scraper** (Google Sheets) | Power Rankings compétitifs par région | HTML scraping (ScrapFly/ScraperAPI/Scrape.do) |

Ces deux sources sont **non substituables** — elles couvrent des dimensions différentes du jeu.

---

## 2. fortnite-api.com — Endpoint principal

### `GET /v2/stats/br/v2`

Paramètres :

| Paramètre | Type | Description |
|---|---|---|
| `name` | string | Pseudo Epic exact (ex: `Bugha`) |
| `accountId` | string | Epic ID 32 hex chars (ex: `33f85e8ed7124d15ae29cfaf53340239`) |
| `timeWindow` | `lifetime` ou `season` | Défaut : `lifetime` |
| `image` | string | (optionnel) type d'image à inclure |

Header requis : `Authorization: {API_KEY}`

### Structure de réponse

```json
{
  "status": 200,
  "data": {
    "account": {
      "id": "33f85e8ed7124d15ae29cfaf53340239",
      "name": "Bugha"
    },
    "battlePass": {
      "level": 20,
      "progress": 76
    },
    "stats": {
      "all": { ... },
      "keyboardMouse": { ... },
      "gamepad": { ... },
      "touch": { ... }
    }
  }
}
```

### Dimensions des stats

**Par type d'input** (`stats.*`) :

| Clé | Description |
|---|---|
| `all` | Toutes plateformes confondues |
| `keyboardMouse` | Clavier + souris (PC) |
| `gamepad` | Manette (console) |
| `touch` | Écran tactile (mobile) |

**Par mode de jeu** (`stats.all.*`) :

| Clé | Description |
|---|---|
| `overall` | Toutes modes confondus |
| `solo` | Solo |
| `duo` | Duo |
| `squad` | Squad |
| `ltm` | Modes temporaires (LTM) |

### Champs disponibles par mode (exemple : `stats.all.overall`)

| Champ | Type | Exemple (Bugha lifetime) | Description |
|---|---|---|---|
| `score` | number | `1 871 653` | Score total accumulé |
| `scorePerMin` | float | `15.08` | Score moyen par minute |
| `scorePerMatch` | float | `210.04` | Score moyen par partie |
| `wins` | number | `373` | Victoires royales |
| `top3` | number | `212` | Top 3 finishes |
| `top5` | number | `308` | Top 5 finishes |
| `top6` | number | `322` | Top 6 finishes |
| `top10` | number | `147` | Top 10 finishes |
| `top12` | number | `624` | Top 12 finishes |
| `top25` | number | `285` | Top 25 finishes |
| `kills` | number | `28 502` | Éliminations totales |
| `killsPerMin` | float | `0.23` | Éliminations/minute |
| `killsPerMatch` | float | `3.199` | Éliminations/partie |
| `deaths` | number | `8 538` | Décès totaux |
| `kd` | float | `3.338` | Ratio Kills/Deaths |
| `matches` | number | `8 911` | Parties jouées |
| `winRate` | float | `4.186` | % de victoires |
| `minutesPlayed` | number | `124 111` | Temps de jeu total (min) |
| `playersOutlived` | number | `334 400` | Joueurs éliminés avant soi |
| `lastModified` | ISO 8601 | `2025-12-22T05:33:17Z` | Dernière mise à jour |

### Cas d'erreur

| HTTP | Corps | Signification |
|---|---|---|
| `404` | `{"status":404,"error":"the requested account does not exist"}` | Pseudo inexistant ou compte privé |
| `401` | `{"status":401,"error":"unauthorized"}` | Clé API manquante/invalide |
| `429` | — | Rate limit dépassé |

---

## 3. Lien entre Epic ID et FortniteTracker

### Comment récupérer l'Epic ID d'un joueur

Quand on navigue sur le profil FortniteTracker d'un joueur, l'URL contient le pseudo et l'ID Epic est visible dans le code source de la page :

```
URL:  https://fortnitetracker.com/profile/all/M8%20ak1./events?id=077d69a8-f1ea-43e4-bf8f-1273dc0b5aa5
```

```js
// Dans le source HTML de la page :
let platformInfo = {
    platformId: 8,
    platformSlug: 'epic',
    platformUserHandle: 'M8 ak1.',
    platformUserId: '077d69a8-f1ea-43e4-bf8f-1273dc0b5aa5',   // ← Epic ID avec tirets
    platformUserIdentifier: 'M8 ak1.'
};
```

### Format de l'Epic ID

- **FortniteTracker** : UUID avec tirets — `077d69a8-f1ea-43e4-bf8f-1273dc0b5aa5` (36 chars)
- **fortnite-api.com** : Hex sans tirets — `077d69a8f1ea43e4bf8f1273dc0b5aa5` (32 chars)
- Conversion : retirer les `-` → même identifiant

**Conséquence** : en enrichissant le scraper pour extraire l'Epic ID depuis FortniteTracker, on peut ensuite appeler fortnite-api.com avec `?accountId=` pour éviter les ambiguïtés de pseudo (joueurs avec pseudo similaires).

---

## 4. Ce que fortnite-api.com NE fournit PAS

| Donnée | Disponible ? | Alternative |
|---|---|---|
| Power Rankings / PR points | ❌ Non | Scraper FortniteTracker |
| Classement compétitif régional | ❌ Non | Scraper FortniteTracker |
| Affiliation équipe (ex: M8, NRG…) | ❌ Non | Scraper FortniteTracker |
| Résultats de tournois | ❌ Non | Scraper FortniteTracker (events) |
| Leaderboard global Battle Royale | ❌ Endpoint retourne vide | N/A |
| Avatar / photo de profil | ❌ Non retourné | N/A |
| Cosmetics skins possédés | ❌ Endpoint déprécié (HTTP 410) | N/A |

---

## 5. Ce que le Scraper FortniteTracker fournit (rappel)

Le script Google Sheets scrape `fortnitetracker.com/events/powerrankings` :

| Champ | Exemple |
|---|---|
| `rank` | `1` (position dans le leaderboard PR) |
| `player` | `Bugha` (pseudo actuel) |
| `team` | `SEN` (équipe esport) |
| `points` | `20400` (PR points compétitifs) |

**Régions disponibles** :

| Région | Pages | Joueurs max |
|---|---|---|
| GLOBAL | 21 | 2 100 |
| NAW (North America West) | 3 | 300 |
| NAE (North America East / NAC) | 3 | 300 |
| EU (Europe) | 3 | 300 |
| BR (Brazil) | 3 | 300 |
| ASIA | 3 | 300 |
| ME (Middle East) | 3 | 300 |
| OCE (Oceania) | 3 | 300 |

**Total** : jusqu'à ~4 200 joueurs indexés (avec recouvrements inter-régions).

---

## 6. Mapping vers les besoins du projet (FEAT-004)

### Cas d'usage FEAT-004 : Catalogue de joueurs pour le draft

| Besoin | Source recommandée | Champs utilisés |
|---|---|---|
| Liste des joueurs drafables avec rang PR | **Scraper FT** | `rank`, `player`, `points`, `team` |
| Région principale du joueur | **Scraper FT** | source (GLOBAL, EU, NAW…) |
| Stats de profil pour enrichissement | **fortnite-api.com** | `kd`, `winRate`, `wins`, `matches` |
| Epic ID pour résolution sans ambiguïté | **Scraper FT** (source HTML) ou **fortnite-api.com** (`account.id`) |
| Variation de PR (delta quotidien) | **Scraper FT** (snapshots successifs) | `points` J vs J-1 |

### Architecture de collecte recommandée

```
[Scraper Google Sheets] ──→ [Staging DB]
                                │
                    ┌───────────┴────────────┐
                    │                        │
              Données PR              fortnite-api.com
         (rank, team, points)      (kd, wins, winRate…)
                    │                        │
                    └───────────┬────────────┘
                                │
                    [PlayerIdentityEntry]
                    (pseudo + Epic ID + PR + stats)
```

### Appel API recommandé pour FEAT-004

```http
GET /v2/stats/br/v2?name={pseudo}&timeWindow=lifetime
Authorization: {FORTNITE_API_KEY}
```

Champs à stocker pour l'enrichissement joueur :
- `account.id` → Epic ID (clé de résolution)
- `stats.all.overall.kd` → KD ratio
- `stats.all.overall.winRate` → % victoires
- `stats.all.overall.wins` → victoires totales
- `stats.all.overall.kills` → éliminations
- `stats.all.overall.matches` → expérience de jeu
- `stats.all.overall.lastModified` → date fraîcheur

---

## 7. Contraintes & limitations

| Contrainte | Valeur |
|---|---|
| Quota API | Gratuit (sans limite documentée explicite) |
| Visibilité compte | Le joueur doit avoir son profil **public** sur Epic |
| Fraîcheur données | `lastModified` indique la dernière mise à jour Epic |
| Latence | ~300–500ms par requête (Cloudflare CDN) |
| Langue des erreurs | Anglais uniquement |
| Format du pseudo | Sensible à la casse et aux accents (URL-encode requis) |

---

## 8. Conclusion

**fortnite-api.com avec la clé API** est utile pour **enrichir les fiches joueurs** (stats BR, KD, winRate, Epic ID) mais **ne remplace pas le scraper FortniteTracker** qui reste la seule source des Power Rankings compétitifs nécessaires au système de draft.

La combinaison optimale pour FEAT-004 :
1. **Scraper FT** → population initiale du catalogue (rank, PR points, team, région)
2. **FortniteTracker HTML** → extraction de l'Epic ID depuis le profil
3. **fortnite-api.com** → enrichissement optionnel des stats casual (KD, wins, winRate)
