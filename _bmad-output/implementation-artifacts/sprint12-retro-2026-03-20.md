# Sprint 12 Retrospective — "Stabilisation — l'app ne plante plus"

**Date :** 2026-03-20
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev)
**Objectif Sprint 12 :** "Zéro crash sur disconnect/draft, delete/archive fonctionnels, navigation propre, logout propre"
**Session de test manuel :** Thibaut + Teddy — réalisée le 2026-03-20 avant clôture rétro

---

## 1. Résumé du Sprint

### Métriques de livraison

| Métrique | Valeur |
|---|---|
| Stories livrées | 12 / 13 (92%) |
| Story déférée | `sprint12-sprint11-deferred-items` — pipeline email (intentionnel) |
| Phase 0 — Crashes P0 | 4/4 ✅ |
| Phase 1 — UX/Error | 3/3 ✅ |
| Phase 2 — Bonus | 1/2 ✅ (deferred items → backlog stable) |
| Phase 3 — Validation | 4/4 ✅ |
| Tests backend | 2 383 run, 0 nouvelles régressions |
| Tests frontend Vitest | 2 254 run / 1 720 passing (534 pre-existing inchangés) |
| E2E couverts | login/logout store cleanup, delete/archive 409, draft 2 joueurs multi-context |

### Stories livrées

| Story | Description | Status |
|---|---|---|
| `sprint12-ws-lifecycle-fix` | RC-1+RC-5 : takeUntilDestroyed + store.reset() au logout | ✅ done |
| `sprint12-delete-archive-fix` | RC-2 : DataIntegrityViolation → 409 CONFLICT | ✅ done |
| `sprint12-draft-timer-server-sync` | RC-3 : expiresAt dans STOMP PICK_PROMPT | ✅ done |
| `sprint12-loading-states-fix` | RC-4 : isLoading signal + état tripartite | ✅ done |
| `sprint12-error-boundary` | GlobalErrorHandler → ErrorBoundaryComponent | ✅ done |
| `sprint12-create-game-fixes` | isCreating signal + idempotence backend 409 | ✅ done |
| `sprint12-nav-cleanup` | Header simplifié : Logo \| Catalogue \| 👤 Profil | ✅ done |
| `sprint12-candeactivate-draft-guard` | CanDeactivateFn Angular 19 + ConfirmLeaveDialog | ✅ done |
| `sprint12-docker-validation` | 10 ACs validés en conditions Docker réelles | ✅ done |
| `sprint12-e2e-login-logout` | AUTH-LL-01..04 — store cleanup RC-5 | ✅ done |
| `sprint12-e2e-delete-archive` | DA-01..04 — 409 CONFLICT RC-2 | ✅ done |
| `sprint12-e2e-draft-two-players` | DRAFT-2P-01..04 — multi-context 2 browsers | ✅ done |
| `sprint12-sprint11-deferred-items` | Email monitoring + cooldown + WS-01 CI guard | ⏸ backlog |

---

## 2. Suivi des actions Sprint 10 (dernier sprint avec rétro)

> Sprint 11 n'a pas eu de rétro — gap d'apprentissage confirmé.

| # | Action promise (Sprint 10) | Statut | Notes |
|---|---|---|---|
| A1 | Email monitoring pipeline (câbler EmailAlertService sur regionFailures) | ❌ Non fait | 3ème déféré consécutif — décision consciente Sprint 12 |
| A2 | Email alerts cooldown 24h (lastAlertSentAt) | ❌ Non fait | Même déféré |
| A3 | WS-01 skip en CI (test.skip process.env.CI) | ❌ Non fait | Même déféré |

**Score : 0/3 — items jamais critiques assez pour forcer leur priorité.**

Pattern identifié : ces 3 items sont en backlog depuis Sprint 10. Ils ne sont douloureux que si le cron pipeline est activé en prod. Tant que `INGESTION_PR_SCHEDULED_ENABLED=false`, le risque reste théorique.

---

## 3. Ce qui a bien marché

**Alice (Product Owner):** "92% sur un sprint de stabilisation pure. Les 4 P0 sont fermés — les crashes qui bloquaient les vrais joueurs. Chaque fix avait un Root Cause identifier (RC-1 à RC-5) : on a arrêté de patcher les symptômes."

**Charlie (Senior Dev):** "La discipline de nommer les root causes a changé la qualité des fixes. RC-3 (timer désynchronisé) par exemple — au lieu de bricoler le client, on a fait le bon choix : ajouter `expiresAt` dans le STOMP event côté backend. Solution durable."

**Dana (QA Engineer):** "Les 3 specs E2E sont un vrai filet de sécurité. Pour la première fois, le logout/login store cleanup est testé bout-en-bout. Le fix RC-5 ne peut plus régresser silencieusement."

**Elena (Junior Dev):** "Le `canDeactivateDraftGuard` est élégant — CanDeactivateFn Angular 19, 21 lignes, pas une classe. Exactement le genre de code qu'on devrait écrire partout."

**Thibaut (Project Lead):** "J'ai adoré vous voir tous travailler. La discipline et la rigueur du sprint sont visibles."

### Victoires clés
- 4/4 crashes P0 fermés avec Root Cause nommé
- Code review adversariale a trouvé et corrigé 5 issues réelles avant merge
- Docker validation a confirmé les 10 ACs en conditions réelles
- Pattern canDeactivateFn Angular 19 documenté et testé
- Sprint status mis à `done` en début de rétro (leçon L6 Sprint 10 enfin appliquée)

---

## 4. Challenges identifiés

### Challenge #1 — 534 tests frontend pre-existing (dette visible)
- 534 `fakeAsync(async () => {})` incompatibles Vitest/Zone.js depuis la migration Sprint 7
- Le signal test est noyé — une vraie régression de 10 tests est invisible dans les 534
- Pattern de fix documenté (Pattern A + B) mais jamais appliqué à grande échelle
- Décision : story dédiée Sprint 13

### Challenge #2 — Sprint 11 sans rétro + 3 actions Sprint 10 jamais exécutées
- 3ème sprint consécutif de déférage pour email monitoring/cooldown/WS-01 CI guard
- Sprint 11 passé sans rétro → apprentissage cycle sauté
- Ces items restent backlog stable tant que le cron n'est pas activé en prod

### Challenge #3 — Gap entre E2E "passing" et usage réel
- Les E2E draft testent les picks **via API** (pas via l'UI navigateur)
- Un utilisateur réel passe par WebSocket + rendu + synchronisation d'état : chemin totalement différent
- Session de test manuel a révélé 6 bugs critiques non couverts par les E2E
- Leçon : les E2E agent-générés ont tendance à être over-guarded (test.skip) et under-assertive

### Challenge #4 — Absence de logs structurés
- Diagnostic des bugs = navigation à l'aveugle
- "Erreur serveur" sans message précis = impossible à debugger sans accès direct aux logs serveur
- Ralentit tous les cycles de debug de tous les sprints futurs

---

## 5. Bugs remontés — Session de test Thibaut + Teddy (2026-03-20)

### 🔴 Critiques — Bloquent le flux principal

| ID | Description | Composant probable |
|---|---|---|
| BUG-01 | "Erreur serveur" après "Démarrer la draft" — côté Thibaut ET Teddy | GameController / DraftController |
| BUG-02 | Timer bloque à 0s — rien ne se passe (pas d'autopick, pas de transition) | DraftTimerComponent / SnakeDraftService |
| BUG-03 | **Région incorrecte** — Asie demandée, joueur Océanie (Gazer) proposé | DraftTrancheService / filtrage région |
| BUG-04 | À partir du 3ème joueur : sélection impossible | SnakeDraftService / DraftPickOrchestrator |
| BUG-05 | Quitter la draft → impossible de revenir → "Erreur serveur" | canDeactivate / SnakeDraftController |
| BUG-06 | State désync entre Thibaut et Teddy (Teddy voit "à Thibaut de jouer" après son pick) | WebSocket STOMP / DraftEventMessage |

### 🟡 Importants — Dégradent l'expérience

| ID | Description |
|---|---|
| BUG-07 | Bouton "Catalogue" visible sur "Mes parties" — contexte inattendu |
| BUG-08 | Filtre région catalogue → "Aucun joueur trouvé" (bug requête/filtrage) |
| BUG-09 | Après refresh : "initialisation" + "connexion instable" — état UI incompréhensible |
| BUG-10 | Route `/snake` non accessible depuis l'UI — Teddy a dû saisir l'URL manuellement |
| BUG-11 | Type de draft non affiché à l'écran (snake ? simultané ?) |
| BUG-12 | Admin panel : nombreuses erreurs visibles |
| BUG-13 | **Régression visuelle page classement** — dégradation par rapport à l'ancienne version |

### 🟢 Observations / Manques

| ID | Description |
|---|---|
| OBS-01 | Customisation du timing de draft non visible/accessible |
| OBS-02 | Absence de logs structurés — visibilité quasi nulle |
| OBS-03 | 773 joueurs : origine et données non documentées |
| OBS-04 | Catalogue visible hors partie — pertinence à questionner |

---

## 6. Leçons apprises

### L1 — Tests agent-générés = over-guarded + under-assertive [NOUVEAU]
**Contexte :** Code review Sprint 12 a trouvé : ConfirmLeaveDialog vérifie l'existence des boutons mais ne les clique pas, AUTH-LL-02 vérifie body visible mais pas le redirect, DRAFT-2P-04 accepte DRAFTING comme état final.
**Leçon :** Les tests générés par agent optimisent pour "ne pas échouer" plutôt que pour "valider le comportement". Code review obligatoire sur tous les tests avant `done`.
**How to apply :** Checklist code review tests : (1) est-ce qu'on clique/interagit, pas juste qu'on vérifie la présence ? (2) l'assertion finale est-elle l'état exact attendu ou un fallback large ?

### L2 — Tester l'app manuellement après sprint de stabilisation [NOUVEAU]
**Contexte :** Sprint 12 a corrigé 7 bugs. Session de test manuel post-sprint a révélé 6 nouveaux bugs critiques non couverts par les E2E.
**Leçon :** Un sprint de stabilisation sans session de test manuel réel est incomplet. Les E2E API-level ne substituent pas un vrai utilisateur qui navigue.
**How to apply :** DoD sprint de stabilisation : inclure une session de test manuel avant de fermer le sprint.

### L3 — Sprint sans rétro = apprentissage perdu [CONFIRME]
**Contexte :** Sprint 11 n'a pas eu de rétro. Les 3 actions Sprint 10 non exécutées auraient pu être revisitées et soit escaladées soit formellement dépriorisées.
**Leçon :** Même une courte rétro vaut mieux que pas de rétro. Le déférage répété sans décision explicite crée une dette de process.

### L4 — Logs structurés = prérequis au diagnostic [NOUVEAU]
**Contexte :** Thibaut a vu "Erreur serveur" sans message précis. Sans logs structurés, impossible de diagnostiquer BUG-01 à distance.
**Leçon :** Les logs structurés ne sont pas du polish — ils sont du prérequis opérationnel. Chaque sprint passé sans eux ralentit tous les diagnostics futurs.

---

## 7. Action Items Sprint 13

### Priorité absolue (P0)
1. **[A1] Logs structurés backend** — SLF4J JSON layout + corrélation request ID + niveaux par package. Sans ça, BUG-01 à BUG-06 sont impossibles à diagnostiquer proprement.
2. **[A2] Fix BUG-01..06** — les 6 bugs critiques draft remontés en session de test
3. **[A3] Fix 534 tests frontend Zone.js/fakeAsync** — story dédiée agent isolé en worktree

### Priorité haute (P1)
4. **[A4] Fix BUG-07..13** — bugs importants UX/visuel (dont régression classement)
5. **[A5] Documentation joueurs** — fichier Markdown récapitulatif + diagramme Mermaid architecture

### Backlog stable (ne pas re-proposer avant activation cron)
- Email monitoring pipeline + cooldown 24h + WS-01 CI guard

---

## 8. Scoring préliminaire Sprint 13

| Item | Valeur | Risque si non fait | Effort | **Score** |
|---|---|---|---|---|
| Logs structurés backend | 5 | 5 | 2 | **12** |
| Fix BUG-01..06 (draft critiques) | 5 | 5 | 4 | **14** |
| Fix 534 tests frontend | 4 | 3 | 3 | **10** |
| Fix BUG-07..13 (UX/visuel) | 3 | 2 | 3 | **8** |
| Docs joueurs + Mermaid archi | 2 | 1 | 1 | **4** |

**Recommandation Sprint 13 :** Bugs draft critiques (score 14) + logs structurés (score 12) en P0. Fix 534 tests (score 10) en P1. Docs en bonus.

---

## 9. Readiness Assessment

| Dimension | Status |
|---|---|
| Tests backend | ✅ 2 383 run, 0 régression |
| Tests frontend | ⚠️ 534 pre-existing (masquent le signal) |
| E2E | ⚠️ Couvrent API-level, pas le flux navigateur réel |
| App testée manuellement | ✅ Session Thibaut + Teddy 2026-03-20 |
| Bugs critiques connus | 🔴 6 P0 identifiés |
| Logs / observabilité | 🔴 Absents — diagnostic aveugle |
| Déploiement prod | ⏸ Docker local uniquement |

---

## 10. Clôture

Bob (Scrum Master): "Sprint 12 a livré ce qu'il promettait : zéro crash sur les P0. La session de test manuel a été précieuse — elle donne à Sprint 13 une base de bugs réels, pas d'hypothèses. C'est du travail honnête."

Alice (Product Owner): "Et les docs joueurs + Mermaid en Sprint 13 vont aider tout le monde à mieux comprendre le domaine."

Charlie (Senior Dev): "Les logs d'abord. Sans eux, on fixe les bugs en aveugle."

Dana (QA Engineer): "Je retiens la leçon L1 — les tests agent-générés doivent tous passer par code review. C'est non négociable maintenant."

**Sprint 12 : DONE ✅**
**Sprint 13 : À planifier après cette rétro**
