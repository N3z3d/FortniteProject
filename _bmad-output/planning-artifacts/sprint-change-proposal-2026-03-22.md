# Sprint Change Proposal — 2026-03-22
**Statut :** En attente d'approbation
**Scope :** Majeur — recentrage produit + stabilisation draft
**Généré par :** Correct Course workflow (BMAD) — 15 méthodes d'élicitation avancée

---

## Section 1 — Résumé du problème

La session de test Docker réel du 2026-03-22 (post-Sprint 13) a révélé que plusieurs fonctionnalités critiques ne fonctionnent pas en conditions réelles, malgré leur statut `done` dans sprint-status.yaml.

**Deux bugs d'infrastructure corrigés ce soir :**
- `SpaController` interceptait `/assets/**` → i18n cassée (toutes les traductions en clés) depuis Sprint 3
- `SpaController` interceptait `/ws/**` → WebSocket/SockJS mort depuis Sprint 3 → 271 fausses erreurs dans le journal admin

**Bugs restants (non couverts par les fixes ce soir) :**
- Draft crash systématique au tour 2 en mode multi-région
- Timer non synchronisé entre les deux comptes joueurs
- Filtre région ignoré dans la liste de sélection (joueurs hors-région sélectionnables)
- Notification "C'est ton tour" absente ou insuffisante pour le joueur en attente
- Leaderboard visuellement insuffisant post-draft
- Données hardcodées dans le tableau de bord jeu ("147 joueurs draftés")

**Cause racine systémique :** La Definition of Done ne requérait pas de test en conditions Docker réelles avec de vrais utilisateurs. Les E2E tests bypassent l'UI et soumettent les picks via API HTTP directe — ils ne valident pas les flux temps-réel WebSocket/STOMP et ne détectent pas ces régressions.

**Constat de fond :** Le PRD déclare le WebSocket *"deferred to Growth"* alors que le draft temps-réel en dépend entièrement. Ce conflit doit être corrigé dans le PRD.

---

## Section 2 — Analyse d'impact

### Impact sur les Epics

| Epic | Statut déclaré | Réalité terrain | Impact |
|------|----------------|-----------------|--------|
| Epic 1 — Pipeline données | done | ✅ Fonctionnel | Aucun |
| Epic 2 — Catalogue | done | ⚠️ Fonctionne, stats absentes UI | Mineur |
| Epic 3 — Créer/rejoindre | done | ✅ Fonctionnel | Aucun |
| Epic 4 — Draft | done | ❌ Crash tour 2, timer désync, filtre région | **Critique** |
| Epic 5 — Trades/swaps | done | ✅ Probablement fonctionnel (non testé) | Aucun |
| Epic 6 — Leaderboard | done | ⚠️ Calcul OK, design insuffisant | Moyen |
| Epic 7 — Admin | done | ⚠️ Données hardcodées, pas de valeur réelle actuellement | Mineur |

### Impact sur les artefacts

**PRD :**
- Ligne à corriger : *"WebSocket (STOMP.js existant) deferred to Growth"* → WebSocket est MVP obligatoire, le draft temps-réel en dépend
- Ajout : Definition of Done doit inclure "testé en Docker avec 2 vrais utilisateurs" pour toute story draft

**Architecture :**
- Aucun conflit de fond — WebSocket STOMP déjà en place, architecture hexagonale saine
- Ajout : `SpaController` doit exclure `/ws/**` et `/assets/**` (déjà corrigé)

**Tests E2E :**
- Écart de stratégie : les E2E actuels bypassent l'UI et se marquent `skipped` plutôt qu'ÉCHEC quand le setup échoue
- Décision : hybride — garder E2E API comme contrats backend + ajouter 1 E2E UI par parcours critique

**Draft simultané :**
- Implémenté mais jamais testé en Docker réel
- Décision : garder dans le code, masquer visuellement dans l'UI jusqu'à validation Docker

---

## Section 3 — Approche recommandée

**Option choisie : Option 1 (ajustement direct) + correction partielle PRD (Option 3)**

**Rationale :**
- L'architecture est saine, il n'y a rien à défaire
- Sprint 14 = stabiliser les fondations du draft jusqu'à ce qu'une vraie partie soit jouable de A à Z
- Le test de succès de Sprint 14 n'est pas "N stories done" mais **"Thibaut et Teddy jouent une partie complète sans frustration"**
- Tout ce qui ne sert pas cet objectif passe en Sprint 15

**Vision 6 mois :**
> Si Sprint 14 livre un draft fonctionnel, la première vraie partie peut être jouée en avril 2026. En mai : invitation de 2 amis supplémentaires. En juin : scores mis à jour automatiquement depuis FortniteTracker. En septembre : 6 joueurs réguliers. Sans Sprint 14 solide, le produit reste un prototype non utilisé.

**Décisions techniques structurantes :**

| Décision | Choix retenu |
|----------|-------------|
| Timer | 100% serveur — client affiche depuis `expiresAt`, ne gère plus l'état |
| Draft simultané | Masqué dans l'UI jusqu'à test Docker validé |
| E2E | Hybride : E2E API (contrats) + 1 E2E UI draft (2 contextes, clic réel) |
| DoD draft | Inclure test Docker 2 comptes réels avant de marquer done |
| Durée timer | 3 minutes par défaut (configurable, 30s pour les tests) |

---

## Section 4 — Stories Sprint 14

### P0 — Bloquants (sans ces fixes, impossible de jouer)

**sprint14-fix-draft-crash-tour2**
> Fix du crash Angular au tour 2 en mode multi-région. Cause probable : `draftEvents$` Subject émet null ou termine après le premier pick STOMP. Valider avec logs navigateur sur Docker, corriger le parsing du STOMP event `PICK_MADE`, ajouter test unitaire + test Docker 2 comptes.

**sprint14-fix-timer-server-sync**
> Timer 100% serveur. Le composant `DraftTimerComponent` doit calculer le temps restant depuis `expiresAt` (ISO-8601 fourni par le backend) et ne jamais repartir de 60s au refresh. Inclure : timer synchronisé sur les 2 écrans, resync après reconnexion WebSocket.

**sprint14-fix-region-filter-draft**
> Filtre région automatique dans la liste de sélection du draft serpent. Quand le curseur est sur la région ASIE, seuls les joueurs de région ASIE sont affichés — pas besoin de filtre manuel. Le pick auto respecte aussi la région. Inclure validation backend si le filtre est contourné.

### P1 — Expérience joueur

**sprint14-ux-notification-tour**
> Bannière "TON TOUR — Xm Ys" en couleur vive (gaming primary), visible immédiatement quand c'est au tour du joueur. Pour l'adversaire : "Tour de [Nom] — Xm Ys". Animation sur le changement de tour. Inclure notification au lancement du draft côté spectateur (Teddy voit "Draft commencé — en attente du pick de Thibaut").

**sprint14-fix-leaderboard-design**
> Refonte visuelle du leaderboard game (`/games/:id/leaderboard`) pour atteindre le niveau "gaming theme" prévu dans la spec UX : fond sombre, couleurs médaille or/argent/bronze, font Rajdhani pour les titres, deltas PR positifs/négatifs en vert/rouge. Référence : `simple-leaderboard.component.scss`.

**sprint14-fix-participants-count**
> Fix "2/5 participants" — la valeur `maxParticipants` configurée à la création (2) n'est pas respectée dans l'affichage. Vérifier le mapping DTO `GameDto.maxParticipants` → interface Angular `Game`.

**sprint14-desactiver-draft-simultane-ui**
> Masquer le mode "Simultané" dans le formulaire de création de partie tant qu'il n'a pas été validé en Docker. Conserver tout le code backend — uniquement désactivation visuelle (`[disabled]` ou `*ngIf`). Effort estimé : 30 minutes.

### P2 — Infrastructure qualité

**sprint14-e2e-ui-draft-complet**
> 1 test Playwright E2E UI qui ouvre 2 vrais contextes navigateur, joue un draft complet via clic réel sur les boutons, et vérifie l'état final sur les 2 pages. Remplace les 4 tests DRAFT-2P qui se marquent `skipped`. Intégrer dans CI avec retry=1.

### Corrections PRD (hors stories)

1. Retirer *"WebSocket (STOMP.js existant) deferred to Growth"* — remplacer par *"WebSocket STOMP requis en MVP pour le draft temps-réel"*
2. Ajouter dans la section DoD : *"Toute story touchant le draft ou le WebSocket doit être testée avec 2 comptes réels en Docker avant d'être marquée done"*

---

## Section 5 — Handoff et critères de succès

**Scope de ce changement : Majeur**
Implique une correction PRD, une redéfinition de la DoD, et une réorganisation des priorités Sprint 14.

**Handoff :**
- **Développement (Charlie/Dev agent)** : 8 stories Sprint 14 ci-dessus
- **Product Owner (Alice)** : Corriger le PRD sur WebSocket + DoD
- **Scrum Master (Bob)** : Mettre à jour sprint-status.yaml + s'assurer que sprint14 commence par les P0

**Critère de succès Sprint 14 :**
> Thibaut et Teddy ouvrent l'app, créent une partie, font un draft complet (tous les slots remplis, ordre serpent respecté, timer synchronisé, région respectée) sans bug, et voient le leaderboard avec leurs équipes.

**Ce qui reste pour Sprint 15 :**
- Catalogue avec stats joueurs (ranking, points, région)
- Admin panel cleanup (supprimer données hardcodées, dashboard utile)
- Draft simultané (activer après validation Docker)
- Chaos monkey scenarios (reconnexion, picks invalides, edge cases)
- Optimisations performance et UX supplémentaires

---

*Document généré le 2026-03-22 via BMAD Correct Course workflow.*
*15 méthodes d'élicitation avancée appliquées : Pre-mortem, 5 Whys, User Personas, War Room, First Principles, Stakeholder Round Table, Comparative Matrix, Pre-mortem 6 mois, Debate Club, What If, Good Cop Bad Cop, Reverse Engineering, Chaos Monkey, ADR, SCAMPER.*
