---
stepsCompleted: [1, 2]
session_topic: 'Système d agrégation d identités et données joueurs Fortnite — Power Rankings, pseudos multi-sources, Epic ID — Supabase + historique quotidien'
session_goals: 'Vision long terme robuste, résolution identité unique sans doublons, architecture scalable et changeable si APIs évoluent, historique des données'
selected_approach: 'ai-recommended'
techniques_used: ['First Principles Thinking', 'Assumption Reversal', 'Six Thinking Hats']
ideas_generated: []
---

# Session Brainstorming — Agrégation Joueurs Fortnite
**Date :** 2026-02-21
**Facilitatrice :** Mary 📊 Business Analyst

## Vue d'ensemble

**Sujet :** Construire un système d'agrégation d'identités et de données joueurs Fortnite — Power Rankings, pseudos, Epic ID — stocké dans Supabase avec historique quotidien.

**Défi central :** Résolution d'identité — un joueur réel = plusieurs représentations possibles (pseudo FT ≠ pseudo Epic ≠ alias custom ≠ ancien pseudo) → un profil unifié sans doublons.

**Techniques sélectionnées (AI-Recommended) :**
- **Phase 1 — First Principles Thinking** : trouver les vérités absolues du modèle de données
- **Phase 2 — Assumption Reversal** : challenger toutes les hypothèses du système
- **Phase 3 — Six Thinking Hats** : validation multi-perspective avant implémentation

---

## Phase 1 — First Principles Thinking

### Vérités absolues établies

| # | Vérité | Implication architecturale |
|---|---|---|
| V1 | Epic Account ID = identifiant permanent (100%) | Clé primaire universelle du système |
| V2 | PR lifetime ne fait que monter | Stocker des snapshots horodatés, jamais écraser |
| V3 | La "baisse" PR = artefact de filtre temporel | Le frontend filtre, la DB stocke tout brut |
| V4 | FT affiche 2 pseudos : display name + in-game pseudo (URL) | Potentiel point d'entrée pour la résolution |
| V5 | Top ~1000 par région, scraping paginé | Architecture batch, pas temps-réel |
| V6 | Changements de pseudo : quotidiens (plusieurs/jour sur 1000 joueurs) | Résolution automatique OBLIGATOIRE |

### Inconnues critiques (à valider en POC)

| # | Inconnue | Risque |
|---|---|---|
| I1 | FT expose-t-il le pseudo in-game de façon fiable dans le HTML ? | Pipeline de résolution cassé dès le départ |
| I2 | Fortnite-API.com : recherche par nom → Epic ID fonctionne-t-elle ? | Lien FT→Epic impossible à établir |
| I3 | Que contient exactement le HTML d'une page leaderboard FT ? | On scrappe dans le vide |

### Vérité fondamentale

> **Un "joueur" dans notre système N'EST PAS un pseudo. C'est un Epic Account ID.**
> Tout le reste — pseudos, display names, aliases, points — sont des attributs temporels rattachés à cet ID.

---

## Phase 2 — Assumption Reversal

### Insights clés

| Hypothèse inversée | Insight architectural |
|---|---|
| FT = source de joueurs → **FT = source de points seulement** | La liste des joueurs vit dans notre BDD. FT sert uniquement aux points quotidiens. |
| Résolution au scraping → **stockage brut d'abord, résolution après** | Pipeline 2 temps : scrape→store brut, puis résolution asynchrone. Données jamais perdues. |
| Pseudo = joueur → **pseudo = attribut temporel** | Table `pseudo_history` obligatoire. Le pseudo seul ne peut jamais être une clé de jointure. |
| Scraping quotidien → **scraping event-driven** | Les PR changent après chaque tournoi (irrégulier). Scraping quotidien OK comme filet, mais déclencher aussi post-tournoi. |
| Échec résolution = erreur → **échec résolution = état UNRESOLVED** | Joueur non-résolu = état `UNRESOLVED` acceptable. Pipeline non bloqué. Résolution en background. |
| Supabase = destination finale → **Supabase = couche portable** | Schema sans dépendances Supabase-spécifiques. Exportable si besoin. |

### Décisions confirmées
- ✅ PR changent après tournoi → scraping quotidien + trigger post-tournoi
- ✅ État `UNRESOLVED` accepté dans l'app

---

## Phase 3 — Six Thinking Hats

### ⚪ Blanc — Faits
- Epic Account ID = clé permanente
- FT leaderboard = paginé, top ~1000/région
- PR = cumulatif, change après tournoi (irrégulier)
- Pseudos changent quotidiennement (plusieurs/jour)
- Lien FT pseudo → Epic Account ID = **non testé** ⚠️

### 🔴 Rouge — Instincts
- Peur : FT change son HTML → scraper silencieusement cassé (silent failure)
- Peur : réconciliation manuelle sur 50 joueurs non résolus
- Excitation : BDD historique 2 ans de pros EU = unique au monde
- Intuition : tester sur 10 joueurs d'abord. Si ça marche → scale.

### ⚫ Noir — Risques critiques
| Risque | Impact |
|---|---|
| FT bloque le scraper (IP ban, CAPTCHA) | 🔴 Critique |
| FT change son HTML → silent failure | 🔴 Critique |
| Pseudo changé → Fortnite-API.com ne trouve pas → UNRESOLVED | 🟡 Moyen (géré) |
| Doublon créé avant résolution Epic ID | 🔴 Critique |
| Fortnite-API.com devient payant ou ferme | 🔴 Critique |

### 🟡 Jaune — Meilleur cas
- Profil unique par joueur : historique complet pseudos + points semaine/semaine
- Scraper silencieux : nuit + post-tournoi, zéro intervention manuelle
- Swap de source de données = changer un seul adapter (hexagonal)
- Dans 2 ans : BDD unique, personne d'autre ne l'a

### 🟢 Vert — Idées créatives
- **Webhook post-tournoi** : scraper le calendrier FNCS, déclencher 2h après chaque event
- **Fingerprint joueur** : métriques stables (K/D, nb tournois) pour reconnaître un joueur même si pseudo + profil FT ont changé
- **Résolution collaborative** : si résolution auto échoue → notification admin → résolution 1 clic
- **Confidence score** : score 0-100% sur la résolution d'identité. < 80% → badge "à vérifier"
- **Duplicate detector** : job quotidien qui cherche les doublons potentiels (mêmes points, même région, pseudos proches)

### 🔵 Bleu — Process recommandé
```
PHASE 0 — POC : valider les inconnues critiques
  → FT scraping : récupère-t-on le pseudo in-game ?
  → Fortnite-API.com : pseudo → Epic Account ID ?

PHASE 1 — Modèle de données
  → player (epic_account_id PK, confidence_score, status)
  → player_alias (epic_account_id FK, pseudo, source, valid_from, valid_to)
  → pr_snapshot (epic_account_id FK, region, points, scraped_at)
  → scraping_log (source, status, errors, timestamp)

PHASE 2 — Pipeline de scraping
  → Scraper FT leaderboard (par région, paginé)
  → Stocker brut → résolution asynchrone → Epic ID
  → Gestion UNRESOLVED + retry automatique

PHASE 3 — Automatisation
  → Cron quotidien (nuit)
  → Trigger post-tournoi (calendrier FNCS)
  → Duplicate detector quotidien
  → Alertes si scraping silencieusement cassé
```
