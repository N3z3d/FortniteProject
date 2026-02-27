---
stepsCompleted: ['step-01-init', 'step-02-discovery', 'step-02b-vision', 'step-02c-executive-summary', 'step-03-success', 'step-04-journeys', 'step-05-domain', 'step-06-innovation', 'step-07-project-type', 'step-08-scoping', 'step-09-functional', 'step-10-nonfunctional', 'step-11-polish', 'step-12-complete']
status: 'COMPLETE'
completedAt: '2026-02-22'
inputDocuments:
  - '_bmad-output/brainstorming/brainstorming-session-2026-02-21.md'
  - 'docs/FORTNITE_API_RESEARCH.md'
  - '_bmad-output/project-context.md'
workflowType: 'prd'
briefCount: 0
researchCount: 1
brainstormingCount: 1
projectDocsCount: 1
openQuestions:
  - 'App name: no catchy name yet, to define before launch'
  - 'POC BLOCKER: need Fortnite-API.com API key to validate FT pseudo -> Epic Account ID before pipeline build'
classification:
  projectType: 'web_app + data_pipeline (brownfield feature addition)'
  domain: 'Fantasy Sports / Gaming Analytics'
  complexity: 'high'
  projectContext: 'brownfield'
  regions: ['Asia', 'Brazil', 'Europe', 'MiddleEast', 'NAC', 'NAW', 'Oceania', 'Total']
  keyDecisions:
    - 'Pipeline 2 stages: raw scrape table + clean resolved player profile'
    - 'Resolution queue: async table-based (resolution_queue) + scheduled worker'
    - 'Primary region: auto-assigned by majority points over 1 year + admin override'
    - 'Catalog: top X per region (configurable threshold) â€” user-facing for all roles'
    - 'Points: delta PR over configurable competition period (not all-time)'
    - 'Draft modes: 2 options per game â€” snake draft (turn-by-turn, snake order) or simultaneous (all pick at once, duplicate resolution animation)'
    - 'Draft tranche rule: optional constraint on BOTH draft modes. Each pick slot has a minimum rank floor (N picks x tranche size). Can pick equal or worse rank, never better than floor. Disableable per game (free pick).'
    - 'Snake draft order: random at game start'
    - 'Simultaneous draft: each round = 1 choice per participant simultaneously. Duplicates resolved randomly. Loser re-selects missing slot but may change other choices too. Loop until all teams complete.'
    - 'Swap solo rule: unilateral. Replace own player with a FREE (unassigned) player, same region, strictly worse rank. Cannot target a player already on another team.'
    - 'Trade mutuel rule: 1-for-1, requires acceptance by both parties. Cross-region allowed (EU player for NAC player: valid). Available at any time during competition.'
    - 'Admin draft override: admin can assign/remove/add players to any team, bypassing tranche rules'
    - 'Admin manages pipeline data (UNRESOLVED, corrections) not catalog content'
    - 'Alias history: 2 sources stored in MVP (FortniteTracker pseudo + in-game Epic pseudo), searchable in Growth'
    - 'PR evolution curves: 3-month / 6-month trends (no tournament linking in v1)'
    - 'Infrastructure: Docker local -> Supabase prod, JDBC direct, hexagonal adapter'
    - 'Real-time v1: batch update post-scraping. WebSocket (STOMP.js existing) deferred to Growth'
---

# Product Requirements Document â€” FortniteProject
**Author:** Thibaut
**Date:** 2026-02-21

---

## Executive Summary

FortniteProject est une plateforme de fantasy sports dÃ©diÃ©e Ã  l'esport Fortnite â€” la premiÃ¨re du genre. Les utilisateurs composent des Ã©quipes personnalisÃ©es de joueurs professionnels rÃ©els, les engagent dans des ligues privÃ©es entre amis, et se comparent sur la base des Power Rankings officiels issus de FortniteTracker. Le moteur Ã©motionnel est multiple : avoir un enjeu personnel sur ses joueurs prÃ©fÃ©rÃ©s, rÃ©aliser les meilleurs Ã©changes stratÃ©giques, et dÃ©nicher les futures pÃ©pites avant tout le monde.

**ProblÃ¨me rÃ©solu :** Aucune plateforme dÃ©diÃ©e n'existe pour ce cas d'usage. Les alternatives actuelles (Excel manuel, spreadsheets partagÃ©s) sont fragiles, non scalables, impossibles Ã  partager, et excluent les nouveaux participants. Le projet existe depuis plusieurs annÃ©es entre amis sous forme d'Excel â€” l'app remplace ce workflow entiÃ¨rement avec une expÃ©rience fluide, automatisÃ©e et ouverte Ã  tous.

**Utilisateurs cibles :**
- Fans de Fortnite compÃ©titif souhaitant s'engager activement avec la scÃ¨ne pro
- Groupes d'amis voulant crÃ©er leurs propres ligues privÃ©es configurables
- Ã€ terme : joueurs invitÃ©s par leur entourage, puis audience payante

**ModÃ¨le :** Freemium â€” accÃ¨s gratuit pour maximiser l'adoption, monÃ©tisation diffÃ©rÃ©e.

### Ce qui rend ce produit unique

Aucun concurrent direct pour Fortnite. Le produit rÃ©pond Ã  un besoin rÃ©el non adressÃ©, avec une approche multi-tenant (chaque groupe joue dans sa propre bulle indÃ©pendante), des rÃ¨gles de draft configurables par partie, et des donnÃ©es joueurs rÃ©elles synchronisÃ©es automatiquement depuis les compÃ©titions officielles.

Une landing page publique (future feature) permettra de dÃ©couvrir le produit avec des donnÃ©es rÃ©elles avant toute inscription â€” supprimant la friction d'onboarding actuelle.

L'infrastructure de donnÃ©es â€” pipeline de scraping, rÃ©solution d'identitÃ© (FT pseudo â†’ Epic Account ID), catalogue joueurs avec historique PR et courbes d'Ã©volution â€” est entiÃ¨rement nouvelle et constitue le socle technique diffÃ©renciant. Sans donnÃ©es rÃ©elles, le jeu ne fonctionne pas.

## Project Classification

| Attribut | Valeur |
|---|---|
| **Type** | Web App + Data Pipeline (ajout brownfield) |
| **Domaine** | Fantasy Sports / Gaming Analytics |
| **ComplexitÃ©** | Ã‰levÃ©e â€” rÃ©solution d'identitÃ©, pipeline asynchrone, 8 rÃ©gions, hexagonal |
| **Contexte** | Brownfield â€” Spring Boot 3.3 / Angular 20, architecture hexagonale en migration |

---

## Success Criteria

### SuccÃ¨s utilisateur

Un utilisateur rÃ©ussit quand il peut, sans friction : crÃ©er un compte, rejoindre une partie, drafter ses joueurs pros favoris dans une interface fluide et belle. Le moment clÃ© est le **draft** â€” suspense, donnÃ©es visibles, expÃ©rience immersive.

La rÃ©tention est assurÃ©e par le **suivi continu** : revoir chaque matin si ses joueurs ont progressÃ©, surveiller son classement, et avoir assez de donnÃ©es pour dÃ©cider si un Ã©change vaut le coup.

### SuccÃ¨s business

| Niveau | Seuil | Signification |
|---|---|---|
| **Lancement rÃ©ussi** | 20 utilisateurs actifs rÃ©els | Le produit fonctionne, des gens jouent vraiment |
| **Bon lancement** | 50â€“100 utilisateurs actifs | Bouche-Ã -oreille, adoption multi-groupes |
| **Exceptionnel** | Premiers utilisateurs payants | Validation de la valeur perÃ§ue |

### SuccÃ¨s technique

| CritÃ¨re | Objectif |
|---|---|
| **Taille du catalogue** | 1 000 joueurs par rÃ©gion Ã— 7 rÃ©gions + ~7 000â€“10 000 en Total |
| **FraÃ®cheur des donnÃ©es** | Scraping 1 fois/jour entre 5h et 8h |
| **Taux UNRESOLVED** | 0% cible â€” tout UNRESOLVED = anomalie, alerte immÃ©diate |
| **DisponibilitÃ©** | App accessible en permanence, donnÃ©es stables entre deux scrapers |

### Outcomes mesurables

- Draft complet rÃ©alisable en < 5 minutes pour un nouvel utilisateur
- DonnÃ©es du catalogue Ã  jour chaque matin aprÃ¨s scraping nocturne
- ZÃ©ro joueur UNRESOLVED sans alerte admin dÃ©clenchÃ©e dans les 24h
- Un groupe d'amis peut crÃ©er et gÃ©rer une partie de A Ã  Z sans intervention de Thibaut

## Product Scope

### MVP â€” Minimum Viable Product

- Pipeline de donnÃ©es : scraping quotidien (5hâ€“8h), 1 000 joueurs/rÃ©gion, rÃ©solution d'identitÃ© fiable, stockage historique des aliases
- Catalogue joueurs (accessible Ã  tous) : profil par joueur (pseudo, rÃ©gion principale, PR par rÃ©gion, snapshots horodatÃ©s)
- App fonctionnelle : connexion, rejoindre/crÃ©er une partie, 2 modes de draft (serpent avec contrainte de tranches optionnelle / simultanÃ© avec rÃ©solution de doublons), swap solo (rang pire, mÃªme rÃ©gion) et trade mutuel entre participants (sans restriction de rang)
- Leaderboard temps rÃ©el basÃ© sur delta PR de la pÃ©riode de compÃ©tition configurable
- Admin dashboard pipeline : statut scraping, alertes UNRESOLVED, rÃ©solution manuelle 1-clic
- Suppression de compte utilisateur (libÃ¨re ses joueurs dans les parties en cours)

### Growth (Post-MVP)

- Courbes d'Ã©volution PR (tendances 3 mois / 6 mois) dans le catalogue
- Recherche par ancien pseudo (historique des aliases visible)
- Invitation de nouveaux joueurs dans une partie en cours
- Mise Ã  jour leaderboard en quasi-temps rÃ©el via WebSocket (STOMP.js existant)
- Statistiques avancÃ©es par joueur (Ã©volution, comparaison entre joueurs)

### Vision (Futur)

- Landing page publique avec donnÃ©es rÃ©elles (onboarding sans friction)
- MonÃ©tisation freemium : analyse prÃ©dictive de tendances joueurs (catalogue premium)
- Adversaires IA avec personnalitÃ©s distinctes â€” feature payante
- Parties rejoignables par des inconnus / communautÃ© plus large
- Association donnÃ©es/tournois FNCS
- Nom d'app percutant Ã  dÃ©finir avant le lancement public

---

## User Journeys

### Journey 1 â€” Lucas, le joueur casual (chemin idÃ©al)

**DÃ©couverte :** Teddy lui envoie un lien d'invitation. Lucas crÃ©e un compte en 30 secondes, rejoint la partie.

**Le draft :** L'app lui indique les rÃ¨gles (5 joueurs EU, taille de tranche : 7). Pick 1 â†’ plancher rank 1, il prend Queasy (rank 3). Pick 2 â†’ plancher rank 8. Pick 5 â†’ plancher rank 29, il choisit un inconnu rank 47 â€” son pari sur la pÃ©pite.

**La rÃ©tention :** Chaque matin il voit que son Ã©quipe progresse. Il consulte le catalogue pour envisager un Ã©change.

> **Capabilities :** tranches configurables, catalogue accessible pendant draft, leaderboard delta PR.

---

### Journey 2 â€” Karim, le passionnÃ© esport (Ã©change stratÃ©gique)

Son joueur rank 18 EU sous-performe. **Swap solo** : il cherche un joueur EU libre de rang pire (rank 19+). Il trouve rank 31 en progression â€” swap validÃ©. Ce joueur explose. Karim prend la tÃªte. Ensuite il propose un **trade mutuel** Ã  un ami : son joueur NAC rank 5 contre un joueur EU rank 12 â€” cross-rÃ©gion, trade acceptÃ©.

> **Capabilities :** swap solo (rang pire + mÃªme rÃ©gion + joueur libre), trade mutuel (1v1, cross-rÃ©gion, acceptation requise), validation backend temps rÃ©el.

---

### Journey 3 â€” Thibaut, l'admin (surveillance pipeline)

Alerte mail "3 UNRESOLVED depuis 48h". Dashboard : rÃ©sout 2 en 1 clic, marque le 3Ã¨me. VÃ©rifie rapport scraping (8 rÃ©gions OK, 7 234 joueurs, 0 erreur). Ferme en 10 minutes.

> **Capabilities :** alertes mail escalantes, dashboard pipeline, rÃ©solution 1-clic, rapport scraping.

---

### Journey 4 â€” Thomas, l'invitÃ© novice

Ne connaÃ®t aucun pro. Clique sur **"Recommander"** Ã  chaque slot â€” l'app propose le meilleur joueur au plancher. Ã‰quipe en 2 minutes. Revient 2 semaines plus tard en faisant ses propres choix.

> **Capabilities :** bouton "Recommander", recherche par nom dans catalogue.

---

### Journey Requirements Summary

| Capability | Journeys |
|---|---|
| Catalogue accessible Ã  tous (lecture) | Lucas, Karim, Thomas |
| SystÃ¨me de tranches (plancher par slot, configurable, dÃ©sactivable) | Lucas, Karim, Thomas |
| Mode draft serpent (tour par tour, ordre serpent, tranches optionnelles) | Lucas, Karim, Thomas |
| Mode draft simultanÃ© (soumission simultanÃ©e, animation + rÃ©solution doublons) | Lucas, Karim, Thomas |
| Bouton "Recommander" | Thomas |
| Swap solo (rank pire, mÃªme rÃ©gion) | Karim |
| Trade mutuel entre participants (sans restriction de rang) | Karim |
| Leaderboard delta PR sur pÃ©riode configurable | Lucas, Karim |
| Courbes Ã©volution PR â€” Growth | Karim |
| Dashboard pipeline + alertes mail | Thibaut |
| RÃ©solution UNRESOLVED 1-clic | Thibaut |
| Admin override draft (assign/retrait/ajout joueurs) | Thibaut |
| Lien d'invitation + onboarding rapide | Lucas, Thomas |
| Suppression compte + libÃ©ration joueurs | Tous |

---

## Domain-Specific Requirements

### ConformitÃ© & DonnÃ©es utilisateur

- **Suppression de compte (MVP)** : supprime donnÃ©es personnelles + libÃ¨re les joueurs dans les parties en cours.
- **Minimisation des donnÃ©es** : seules les donnÃ©es nÃ©cessaires au jeu sont stockÃ©es.

### Contraintes techniques

- **Pipeline rÃ©silient** : source de scraping et source de rÃ©solution derriÃ¨re des adapters hexagonaux swappables.
- **DonnÃ©es non destructives** : snapshots PR append-only.
- **Alertes opÃ©rationnelles** : tout UNRESOLVED â†’ alerte dans les 24h.

### Ã‰quitÃ© et contestation

- **Bouton "Signaler un problÃ¨me"** : contacte l'admin directement. Thibaut traite manuellement.

---

## Innovation & Novel Patterns

### Zones d'innovation

**1. CrÃ©ation de marchÃ©** â€” aucun concurrent direct pour Fortnite esport fantasy.

**2. Pipeline rÃ©solution d'identitÃ©** â€” FT pseudo â†’ Epic Account ID reconstruit par dÃ©duction, Ã©tat UNRESOLVED gÃ©rable, adapter swappable.

**3. MÃ©canique de draft par tranches** â€” plancher par slot : peut prendre pire, jamais mieux. Absent des fantasy sports classiques.

**4. Base de donnÃ©es historiques unique** â€” 1 000+ joueurs/rÃ©gion avec historique PR complet.

### Validation

- **POC critique** : valider FT pseudo â†’ Epic Account ID sur 10 joueurs avant de construire le pipeline complet.
- **Fallback** : si > 10% UNRESOLVED â†’ rÃ©solution manuelle admin en mode principal temporaire.

### Risques

| Risque | Mitigation |
|---|---|
| POC rÃ©solution Ã©choue | Mode dÃ©gradÃ© rÃ©solution manuelle |
| FT change format HTML | Adapter hexagonal swappable |
| MarchÃ© Fortnite dÃ©cline | Architecture rÃ©utilisable pour d'autres jeux |

---

## Web App + Data Pipeline â€” SpÃ©cifications Techniques

### Architecture technique

| Composant | DÃ©cision |
|---|---|
| **Frontend** | SPA Angular 20 standalone â€” existant |
| **Backend** | Spring Boot 3.3 REST API â€” existant, nouveaux endpoints ajoutÃ©s |
| **Pipeline scraping** | `@Scheduled` Spring Boot (5hâ€“8h), outils externes swappables |
| **Queue rÃ©solution** | Table PostgreSQL `resolution_queue` + worker schedulÃ© |
| **Base de donnÃ©es** | Docker PostgreSQL local â†’ Supabase prod (JDBC direct) |
| **Auth** | JWT existant (pas de changement) |
| **Real-time v1** | Batch update post-scraping â€” pas de WebSocket en MVP |
| **Real-time futur** | STOMP.js + SockJS dÃ©jÃ  prÃ©sents, activables en Growth |

### Support navigateurs & responsive

- **Cible principale** : navigateurs modernes desktop **et** mobile (Chrome, Firefox, Safari, Edge)
- **Responsive** : exigence MVP — qualité d'usage équivalente desktop/mobile sur les parcours critiques (draft, catalogue, pipeline admin)
- **SEO** : non requis pour l'app authentifiÃ©e â€” Ã  considÃ©rer pour la future landing page

### ModÃ¨le de permissions

| RÃ´le | AccÃ¨s |
|---|---|
| **Admin** | Dashboard pipeline, rÃ©solution UNRESOLVED, correction donnÃ©es joueurs, toutes les parties |
| **CrÃ©ateur de partie** | CrÃ©e/configure une partie, invite des joueurs, accÃ¨s catalogue complet |
| **Joueur** | Draft depuis catalogue, Ã©changes, leaderboard, consultation catalogue (tous les joueurs + stats) |

### Endpoints clÃ©s Ã  crÃ©er

- `GET /api/players?region=EU&limit=1000` â€” catalogue joueurs par rÃ©gion
- `GET /api/players/{id}` â€” profil joueur (PR par rÃ©gion, snapshots)
- `GET /api/games/{id}/leaderboard` â€” classement delta PR de la pÃ©riode
- `POST /api/games/{id}/draft/pick` â€” soumettre un pick (validation plancher tranches)
- `POST /api/games/{id}/draft/simultaneous` â€” soumettre les prÃ©fÃ©rences (mode simultanÃ©)
- `POST /api/games/{id}/swap` â€” swap solo (validation rang pire + mÃªme rÃ©gion)
- `POST /api/games/{id}/trade` â€” proposer un trade mutuel entre participants
- `PUT /api/games/{id}/trade/{tradeId}/accept` â€” accepter un trade
- `POST /admin/games/{id}/teams/{teamId}/players` â€” assigner un joueur (bypass tranches)
- `DELETE /admin/games/{id}/teams/{teamId}/players/{playerId}` â€” retirer un joueur
- `GET /admin/pipeline/status` â€” statut scraping + UNRESOLVED
- `POST /admin/players/{id}/resolve` â€” rÃ©solution manuelle Epic ID

### Performance

- Catalogue servi depuis cache Spring (CacheConfig existant)
- Scraping nocturne dÃ©couplÃ© â€” app disponible avec donnÃ©es J-1 si le scraper plante
- ZÃ©ro downtime pendant le scraping

---

## Project Scoping & Phased Development

### StratÃ©gie MVP

**Approche :** MVP "Experience" â€” remplacer l'Excel et prouver que l'expÃ©rience est suffisamment bonne pour que des gens reviennent jouer.

**Ressources :** 1 dÃ©veloppeur, stack existante Spring Boot + Angular, architecture hexagonale en place.

### Phase 0 â€” POC (avant tout dÃ©veloppement)

> âš ï¸ **Bloquant** : valider que le lien FT pseudo â†’ Epic Account ID fonctionne.

**Action requise :** crÃ©er un compte Fortnite-API.com (plan gratuit disponible), tester la recherche par pseudo sur 10 joueurs pros EU connus, vÃ©rifier que l'Epic Account ID retournÃ© est stable et correct.

- âœ… Si le POC passe â†’ construire le pipeline complet
- âš ï¸ Si le POC Ã©choue â†’ activer la rÃ©solution manuelle admin comme mode principal, continuer le dÃ©veloppement en parallÃ¨le

### Phase 1 â€” MVP

**Journeys supportÃ©s :** Lucas (draft complet), Thibaut (admin pipeline), Thomas (onboarding novice)

**Must-have :**
| Feature | Justification |
|---|---|
| Pipeline scraping + rÃ©solution d'identitÃ© | Sans donnÃ©es rÃ©elles, rien ne fonctionne |
| Catalogue joueurs (lecture tous rÃ´les) | CÅ“ur de l'expÃ©rience draft |
| Draft : 2 modes (serpent avec tranches optionnelles / simultanÃ© avec rÃ©solution doublons) | MÃ©canique principale du jeu |
| Leaderboard delta PR sur pÃ©riode | Raison de revenir chaque jour |
| Swap solo + trade mutuel validÃ©s backend | IntÃ©gritÃ© du jeu |
| Bouton "Recommander" | Onboarding novice |
| Dashboard admin pipeline + alertes | OpÃ©rationnel sans intervention DB |
| Suppression de compte | ConformitÃ© minimale |

### Phase 2 â€” Growth

Courbes PR, recherche par alias, invitation en cours de partie, WebSocket leaderboard, stats avancÃ©es

### Phase 3 â€” Vision

Landing page, monÃ©tisation, adversaires IA, communautÃ© publique, intÃ©gration FNCS

### Mitigation des risques

| Risque | Impact | Mitigation |
|---|---|---|
| POC rÃ©solution FTâ†’Epic Ã©choue | ðŸ”´ Critique | RÃ©solution manuelle admin en mode principal |
| FT bloque le scraper | ðŸ”´ Critique | Outils externes swappables, rate limiting |
| Scope creep pendant le dev | ðŸŸ¡ Moyen | Respecter strictement le pÃ©rimÃ¨tre Phase 1 |
| DonnÃ©es pÃ©rimÃ©es si scraper plante | ðŸŸ¡ Moyen | App reste disponible avec donnÃ©es J-1 + alerte admin |

---

## Functional Requirements

> Format : `[Acteur] peut [capacitÃ©] [contexte/contrainte]`
> Phase : toutes les exigences ci-dessous sont **MVP** sauf mention explicite.

### Domaine 1 â€” Pipeline de donnÃ©es

| # | Exigence fonctionnelle |
|---|---|
| FR-01 | Le systÃ¨me peut scraper les leaderboards FortniteTracker pour 8 rÃ©gions (Asia, Brazil, Europe, MiddleEast, NAC, NAW, Oceania, Total) via job `@Scheduled` (5hâ€“8h), ~1 000 joueurs/rÃ©gion |
| FR-02 | Le systÃ¨me peut stocker les donnÃ©es brutes de scraping dans une table staging avant rÃ©solution |
| FR-03 | Le systÃ¨me peut rÃ©soudre un pseudo FortniteTracker â†’ Epic Account ID via adapter hexagonal swappable (ex : Fortnite-API.com) |
| FR-04 | Le systÃ¨me peut marquer un joueur `UNRESOLVED` si la rÃ©solution Ã©choue, sans bloquer le reste du pipeline |
| FR-05 | Le systÃ¨me peut stocker l'historique des pseudos depuis 2 sources distinctes : (1) FortniteTracker (display name + pseudo URL) et (2) pseudo in-game Fortnite Epic â€” avec horodatage pour chaque changement |
| FR-06 | Le systÃ¨me peut stocker les snapshots PR (append-only) par joueur Ã— rÃ©gion Ã— timestamp |
| FR-07 | Le systÃ¨me peut assigner automatiquement une rÃ©gion principale (majoritÃ© des points sur 12 mois) + admin override |
| FR-08 | Le systÃ¨me peut exÃ©cuter un job quotidien lÃ©ger de dÃ©tection de doublons potentiels (mÃªmes points, mÃªme rÃ©gion, pseudos proches) |
| FR-09 | Le systÃ¨me peut dÃ©clencher une alerte admin automatique si un joueur reste `UNRESOLVED` > 24h |

### Domaine 2 â€” Catalogue joueurs

| # | Exigence fonctionnelle |
|---|---|
| FR-10 | Tout utilisateur connectÃ© peut consulter le catalogue filtrÃ© par rÃ©gion (lecture seule, accessible Ã  tous les rÃ´les) |
| FR-11 | Tout utilisateur peut voir le profil dÃ©taillÃ© d'un joueur : pseudo actuel, rÃ©gion principale, PR par rÃ©gion, snapshots horodatÃ©s |
| FR-12 | Tout utilisateur peut rechercher un joueur par pseudo dans le catalogue |
| FR-13 | Le catalogue affiche jusqu'Ã  1 000 joueurs par rÃ©gion, servi depuis le cache Spring (CacheConfig existant) |
| FR-14 | Le catalogue reste accessible pendant le draft (consultation + recherche en temps rÃ©el) |

### Domaine 3 â€” Gestion de parties

| # | Exigence fonctionnelle |
|---|---|
| FR-15 | Un joueur authentifiÃ© peut crÃ©er une partie en configurant : rÃ©gion, nombre de joueurs par Ã©quipe, taille de tranche, pÃ©riode de compÃ©tition, mode de draft |
| FR-16 | Un crÃ©ateur peut dÃ©sactiver le systÃ¨me de tranches pour une partie (mode choix libre â€” aucun plancher de rang) |
| FR-17 | Un crÃ©ateur peut inviter des participants via lien ou code d'invitation |
| FR-18 | Un participant peut rejoindre une partie via lien/code d'invitation |
| FR-19 | Un utilisateur peut supprimer son compte (libÃ¨re ses joueurs dans toutes les parties en cours) |
| FR-20 | Un participant peut signaler un problÃ¨me Ã  l'admin via bouton dÃ©diÃ© |

### Domaine 4 â€” Draft

| # | Exigence fonctionnelle |
|---|---|
| FR-21 | En mode draft serpent, l'ordre de passage est dÃ©terminÃ© alÃ©atoirement au dÃ©marrage ; les participants choisissent Ã  tour de rÃ´le dans un ordre serpent (Aâ†’Bâ†’Câ†’â€¦â†’Câ†’Bâ†’Aâ†’â€¦) jusqu'Ã  complÃ©tion de toutes les Ã©quipes |
| FR-22 | Si les tranches sont activÃ©es (modes serpent ET simultanÃ©), chaque slot a un plancher = `(slot-1) Ã— taille_tranche + 1` ; le participant peut choisir un joueur de rang Ã©gal ou pire au plancher, jamais meilleur |
| FR-23 | Si les tranches sont activÃ©es, le participant peut cliquer "Recommander" pour obtenir automatiquement le meilleur joueur disponible au plancher du slot courant |
| FR-24 | Le backend valide chaque pick contre les rÃ¨gles de tranche actives (les deux modes) et rejette tout pick hors plancher avec message d'erreur explicite |
| FR-25 | En mode draft simultanÃ©, chaque round = tous les participants soumettent **1 choix** en mÃªme temps ; si aucun doublon, le joueur est attribuÃ© et on passe au slot suivant |
| FR-26 | En mode draft simultanÃ©, une animation rÃ©vÃ¨le les sÃ©lections de tous les participants Ã  chaque round ; en cas de doublon, le systÃ¨me attribue le joueur alÃ©atoirement Ã  un seul participant via animation |
| FR-27 | En mode draft simultanÃ©, le participant qui perd un doublon re-sÃ©lectionne son slot manquant ; il peut modifier ses autres choix non encore verrouillÃ©s ; la boucle continue jusqu'Ã  rÃ©solution complÃ¨te de toutes les Ã©quipes |
| FR-28 | Un joueur est exclusif Ã  une Ã©quipe dans une partie donnÃ©e ; il peut Ãªtre draftÃ© dans plusieurs parties simultanÃ©ment |
| FR-29 | L'admin peut assigner manuellement un joueur Ã  n'importe quelle Ã©quipe d'une partie, en bypass des rÃ¨gles de tranche |
| FR-30 | L'admin peut retirer un joueur d'une Ã©quipe dans n'importe quelle partie |
| FR-31 | L'admin peut ajouter un joueur Ã  une Ã©quipe dans n'importe quelle partie |

### Domaine 5 â€” Ã‰changes & Swaps

| # | Exigence fonctionnelle |
|---|---|
| FR-32 | Un participant peut proposer un swap solo Ã  tout moment : remplacer l'un de ses joueurs par un joueur **libre** (non assignÃ© dans cette partie), de rang strictement pire, dans la **mÃªme rÃ©gion** ; impossible de cibler un joueur dÃ©jÃ  dans l'Ã©quipe d'un autre participant |
| FR-33 | Le backend valide le swap solo : joueur cible libre + mÃªme rÃ©gion + rang strictement pire â€” tout swap invalide est rejetÃ© avec message explicite |
| FR-34 | Un participant peut proposer un trade mutuel Ã  tout moment : **1 joueur contre 1 joueur** avec un autre participant, **sans restriction de rÃ©gion** (Ã©change EU contre NAC : valide) ni de rang |
| FR-35 | Un trade requiert l'acceptation explicite du participant adverse avant d'Ãªtre exÃ©cutÃ© |
| FR-36 | Tout swap/trade est enregistrÃ© avec horodatage pour traÃ§abilitÃ© et contestation |

### Domaine 6 â€” Leaderboard & Scoring

| # | Exigence fonctionnelle |
|---|---|
| FR-37 | Le systÃ¨me calcule le score de chaque Ã©quipe = delta PR (valeur fin âˆ’ valeur dÃ©but de pÃ©riode configurÃ©e) |
| FR-38 | Le leaderboard affiche les Ã©quipes classÃ©es par delta PR dÃ©croissant, visible par tous les participants de la partie |
| FR-39 | L'admin ou le crÃ©ateur peut configurer la pÃ©riode de compÃ©tition (date dÃ©but / date fin) |
| FR-40 | Le leaderboard est mis Ã  jour quotidiennement aprÃ¨s le scraping nocturne (batch, pas temps-rÃ©el en v1) |

### Domaine 7 â€” Administration pipeline

| # | Exigence fonctionnelle |
|---|---|
| FR-41 | L'admin peut consulter le tableau de bord pipeline : statut scraping par rÃ©gion, nb joueurs, derniÃ¨re exÃ©cution, nb UNRESOLVED |
| FR-42 | L'admin peut rÃ©soudre manuellement l'Epic Account ID d'un joueur UNRESOLVED en 1 clic |
| FR-43 | L'admin peut corriger les donnÃ©es d'un joueur (pseudo, rÃ©gion principale) |
| FR-44 | L'admin peut override la rÃ©gion principale d'un joueur (remplace l'assignation automatique) |
| FR-45 | Le systÃ¨me envoie des alertes mail escalantes si des joueurs UNRESOLVED persistent > 24h |
| FR-46 | L'admin peut consulter les logs de scraping dÃ©taillÃ©s : succÃ¨s/erreurs par rÃ©gion, nb joueurs, timestamp |
| FR-47 | L'admin peut accÃ©der en supervision Ã  toutes les parties en cours (vue globale) |

---

## Non-Functional Requirements

### Performance

| # | Exigence | CritÃ¨re mesurable |
|---|---|---|
| NFR-P01 | Les pages catalogue se chargent depuis le cache | < 2 secondes (cache chaud) |
| NFR-P02 | Les endpoints de draft/swap rÃ©pondent | < 500 ms |
| NFR-P03 | Le scraping nocturne de 8 rÃ©gions (~7 000 joueurs) se termine | < 3 heures (fenÃªtre 5hâ€“8h respectÃ©e) |
| NFR-P04 | Le leaderboard d'une partie se charge | < 2 secondes pour 20 participants |
| NFR-P05 | Le cache catalogue est rÃ©chauffÃ© automatiquement post-scraping avant 8h | Warmup exÃ©cutÃ© avant ouverture du trafic â€” aucun utilisateur ne subit le cache froid |

### SÃ©curitÃ©

| # | Exigence | CritÃ¨re mesurable |
|---|---|---|
| NFR-S01 | Toutes les communications sont chiffrÃ©es | HTTPS obligatoire en prod â€” 0 communication HTTP clair |
| NFR-S02 | Authentification JWT (existant) | Tokens signÃ©s, durÃ©e de vie limitÃ©e, invalidation Ã  la dÃ©connexion |
| NFR-S03 | Les endpoints `/admin/**` sont protÃ©gÃ©s par rÃ´le ADMIN | 403 pour tout autre rÃ´le |
| NFR-S04 | La suppression de compte efface toutes les donnÃ©es personnelles identifiables | ConformitÃ© RGPD minimale â€” 0 trace PII aprÃ¨s suppression |
| NFR-S05 | Les mots de passe sont hashÃ©s (bcrypt â€” existant) | 0 mot de passe en clair en base |
| NFR-S06 | Les actions d'administration de parties (assign/retrait/ajout joueur â€” FR-29/30/31) sont rÃ©servÃ©es au rÃ´le ADMIN uniquement | 403 si rÃ´le â‰  ADMIN â€” un crÃ©ateur de partie ne peut pas les exÃ©cuter |

### FiabilitÃ©

| # | Exigence | CritÃ¨re mesurable |
|---|---|---|
| NFR-R01 | L'application reste disponible si le scraping Ã©choue | DonnÃ©es J-1 en fallback â€” 0 downtime liÃ© au pipeline |
| NFR-R02 | Tout Ã©chec de scraping dÃ©clenche une alerte admin | DÃ©lai max : 24h aprÃ¨s l'Ã©chec |
| NFR-R03 | Le pipeline de rÃ©solution est non-bloquant | Un joueur UNRESOLVED ne bloque pas le reste du batch |
| NFR-R04 | Les snapshots PR sont append-only | Aucune donnÃ©e historique ne peut Ãªtre Ã©crasÃ©e ou supprimÃ©e |
| NFR-R05 | Les donnÃ©es affichÃ©es portent un horodatage visible ("mis Ã  jour il y a Xh") | Alerte admin dÃ©clenchÃ©e si > 48h sans scraping rÃ©ussi |

### ScalabilitÃ©

| # | Exigence | CritÃ¨re mesurable |
|---|---|---|
| NFR-SC01 | L'application supporte la cible utilisateur MVP, y compris les pics draft | 100 utilisateurs actifs simultanÃ©s + 20 soumissions simultanÃ©es en mode draft paramÃ©trÃ©, sans dÃ©gradation notable |
| NFR-SC02 | Le catalogue est servi sans requÃªte DB par appel | 100% des appels catalogue rÃ©pondent depuis le cache en prod |
| NFR-SC03 | L'ajout d'une 9Ã¨me rÃ©gion est possible sans refactoring majeur | Changement limitÃ© Ã  un adapter hexagonal â€” 0 modification domaine |

### IntÃ©gration

| # | Exigence | CritÃ¨re mesurable |
|---|---|---|
| NFR-I01 | L'adapter de scraping (FortniteTracker) est swappable | Remplacement = 1 fichier adapter, 0 modification domaine |
| NFR-I02 | L'adapter de rÃ©solution (Fortnite-API.com) est swappable | Remplacement = 1 fichier adapter, 0 modification domaine |
| NFR-I03 | Migration Docker â†’ Supabase prod sans modification code | Changement de datasource URL uniquement |
| NFR-I04 | Tout appel API externe (scraping + rÃ©solution) a un timeout configurÃ© | DÃ©faut 10s, configurable â€” dÃ©passement = UNRESOLVED + log, jamais de hang infini |

### MaintenabilitÃ©

| # | Exigence | CritÃ¨re mesurable |
|---|---|---|
| NFR-M01 | Tout nouveau code respecte l'architecture hexagonale existante | ArchUnit en place â€” 0 violation autorisÃ©e |
| NFR-M02 | Taille des classes et mÃ©thodes | Max 500 lignes/classe, max 50 lignes/mÃ©thode (enforced) |
| NFR-M03 | Couverture de tests sur tout nouveau code | â‰¥ 85% lignes â€” threshold enforced dans le build CI, pas seulement visÃ© |

