# Sprint 4 Rétrospective — "Validation, Câblage & Architecture"

**Date :** 2026-03-08
**Facilité par :** Bob (Scrum Master)
**Participants :** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev)
**Objectif Sprint 4 :** Staging public accessible + scraping Fortnite validé + pipeline CI/CD complet

---

## 1. Résumé du Sprint

### Métriques de livraison

| Métrique | Valeur |
|---|---|
| Stories livrées | 3 / 9 (33% — 6 mises en veille car dépendantes de l'hébergement) |
| Stories deferred-future | 5 (hébergement, secrets, DB prod, staging, CI/CD complet, WS auth) |
| Tests backend | 2372 run, 15F+1E (tous pre-existing, inchangé) |
| Tests frontend | 2206 run, 2185 passing (21 Zone.js pre-existing, inchangé) |
| Arch tests (hexagonale) | 40/40 ✅ |
| Incidents de production | 0 |

### Stories livrées

| Story | Description |
|---|---|
| sprint4-api-fortnite-wiring-check | FortniteApiAdapter câblé, 24 tests (unit + intégration + Spring wiring), URL encoding corrigé, graceful degradation |
| sprint4-e2e-seed-data | Seed Flyway isolé dev/prod, 4 users + 25 players, 8/8 smoke tests verts |
| sprint4-a6-jpa-legacy-migration-5-services | 5 services admin migrés JPA → domain ports, IngestionRunRepositoryPort créé, 40/40 arch tests verts |

### Items mis en veille (deferred-future)

Décision prise en retro : l'hébergement externe n'est pas une priorité. Docker local = cible opérationnelle. Ces items sont valides et conservés mais ne seront pas re-proposés avant que le besoin de staging public se fasse sentir.

- `sprint4-decision-hebergement` → deferred-future
- `sprint4-config-secrets-prod` → deferred-future
- `sprint4-db-prod-provisioning` → deferred-future
- `sprint4-a10-staging-deployment` → deferred-future
- `sprint4-a3b-cicd-pipeline-complet` → deferred-future
- `sprint4-sec-r2-websocket-auth` → deferred-future

---

## 2. Ce qui a bien marché

**Charlie (Senior Dev) :** "La migration A6 est un succès propre — 5 services legacy, zéro régression, 40/40 arch tests verts. Le pattern `IngestionRunRepositoryPort` avec la `default` method sur le JPA repo est élégant et réutilisable."

**Dana (QA Engineer) :** "Le câblage Fortnite API avec 24 tests (15 unit + 6 intégration + 3 Spring wiring) — c'est du travail de fond solide. Et le `UriComponentsBuilder` pour l'URL encoding était un bug caché qui aurait fait des dégâts en production avec des pseudos contenant des espaces ou accents."

**Alice (Product Owner) :** "La seed E2E qui isole correctement dev/prod via Flyway locations séparées — bonne pratique bien appliquée. Et la règle 'ne pas re-proposer l'hébergement' a été respectée cette fois."

**Elena (Junior Dev) :** "`findByUsernameIgnoreCase` dans `CustomUserDetailsService` — un bug silencieux de sensibilité à la casse sur le username qui aurait été un vrai casse-tête à déboguer en production."

### Victoires clés
- Migration hexagonale A6 : propre, complète, 0 régression
- API Fortnite : câblée, testée, robuste (timeout, degradation, URL encoding)
- Seed E2E : isolation dev/prod, idempotente, 8/8 smoke verts
- Hébergement : proposé une fois, mis en veille sans re-proposition

---

## 3. Ce qui n'a pas bien marché / Challenges

**Thibaut (Project Lead) :** "Je suis pas encore satisfait. Je vois aucun catalogue, ni page admin. Je savais pas où aller. J'ai essayé `/admin/dashboard` et j'ai eu une 404."

**Charlie (Senior Dev) :** "Le code existe — `/catalogue`, `/admin/dashboard`, `/admin/pipeline`... Mais il n'y a pas de menu de navigation global qui les expose. Et le SpaController ne capture pas correctement `/admin/dashboard` en accès direct (route à 2 segments)."

**Alice (Product Owner) :** "C'est le problème classique : les features existent dans le code mais l'utilisateur ne peut pas les découvrir. Une app sans navigation, c'est une app inutilisable même si le code est parfait."

**Dana (QA Engineer) :** "Et on a 0 tests E2E sur admin et catalogue. L'angle mort était total — on a couvert login + games + draft, jamais les sections métier principales."

**Elena (Junior Dev) :** "Le code review de sprint4-e2e-seed-data a trouvé 4 findings HIGH dont un architectural majeur : le login frontend est un mock — aucun JWT réel n'est émis. Les tests full-flow qui prétendent s'authentifier ne testent pas la vraie authentification."

### Challenges clés
- App non-navigable : catalogue et admin introuvables sans connaître l'URL directe
- Bug 404 SpaController sur `/admin/dashboard` (route à 2 segments non catchée)
- 0 tests E2E sur admin et catalogue — angle mort complet
- Login mock sans JWT réel — gap architectural documenté mais non résolu
- 3/7 actions Sprint 3 en attente (toutes dépendantes hébergement → légitimement en veille)

---

## 4. Suivi des actions Sprint 3

| # | Action promise | Statut | Notes |
|---|---|---|---|
| A1 | Seed data E2E pour tests warm start | ✅ Fait | sprint4-e2e-seed-data livré |
| A2 | Pré-qualifier 4 stretch en stories Sprint 4 | ✅ Fait | Tous dans sprint-status |
| A3 | Décision hébergement une seule fois | ✅ Respecté | Proposé une fois, deferred-future |
| A4 | CI/CD A3b docker push + deploy | ⏳ deferred-future | Dépend hébergement |
| A5 | Secrets config prod | ⏳ deferred-future | Dépend hébergement |
| A6 | API Fortnite wiring check | ✅ Fait | sprint4-api-fortnite-wiring-check livré |
| A7 | A3b pipeline complet | ⏳ deferred-future | Dépend hébergement |

**Score : 4/7 complétés — 3/7 légitimement en veille (bloqués sur hébergement).**

---

## 5. Leçons apprises

### L1 — Une feature qui n'est pas accessible n'existe pas
**Problème :** Catalogue et admin panel implémentés mais inaccessibles sans connaître l'URL. Bug 404 sur l'accès direct.
**Leçon :** Chaque nouvelle section doit être intégrée dans une navigation globale *au moment de son implémentation*, pas après. Le DoD doit inclure : "la feature est accessible depuis la navigation principale".

### L2 — Les tests E2E doivent couvrir toutes les sections, pas seulement le happy path joueur
**Problème :** 0 tests E2E sur `/admin/**` et `/catalogue`. Le flow joueur (login→games→draft) était couvert mais jamais les sections métier admin et catalogue.
**Leçon :** À chaque nouvelle section implémentée, créer au minimum un test E2E smoke. Ne pas accumuler de dette E2E section par section.

### L3 — Le login mock est un gap architectural à adresser avant la prod
**Problème :** `UserContextService.getAvailableProfiles()` est hardcodé, le login ne génère pas de JWT réel. Les tests qui "s'authentifient" ne testent pas la vraie authentification.
**Leçon :** Ce gap doit être documenté clairement dans le project-context et adressé avant toute exposition publique.

### L4 — Le `default` method sur JPA repository = pattern propre pour hexagonal
**Breaktrough :** `IngestionRunRepositoryPort.findRecentLogs(int limit)` implémenté via `default` method dans `IngestionRunRepository`. Garde Spring Data dans l'adapter, expose un contrat domain-friendly.
**Leçon :** Pattern réutilisable pour les futures migrations JPA → domain ports.

---

## 6. Questions ouvertes pour Sprint 5

### Question UX admin — à décider avec tous les agents
**Thibaut :** "Pour les comptes admin, il faudrait un bouton supplémentaire pour accéder au panneau admin. Mais je pense qu'il faudrait discuter avec tous les agents pour savoir ce qu'on fait et y réfléchir ensemble."

→ **Decision pending** : Party Mode recommandé pour décider de l'approche UX (bouton dédié admin / menu contextuel / sidebar / autre). Ne pas décider unilatéralement.

---

## 7. Action Items Sprint 5

═══════════════════════════════════════════════════════════
📝 ACTION ITEMS SPRINT 5 (issus de la retro Sprint 4)
═══════════════════════════════════════════════════════════

### P0 — Bugs bloquants

| # | Action | Détail |
|---|---|---|
| B1 | Fix SpaController 404 | `/admin/dashboard` retourne 404 en accès direct (route 2 segments non capturée). Corriger le pattern RequestMapping dans SpaController. |

### P0 — E2E en masse (priorité absolue Sprint 5)

| # | Action | Détail |
|---|---|---|
| E1 | E2E Admin Panel | Couvrir `/admin/dashboard`, `/admin/pipeline`, `/admin/users`, `/admin/database`, `/admin/logs`. Smoke + navigation + CRUD basique. |
| E2 | E2E Catalogue | Couvrir `/catalogue` : liste joueurs, recherche, filtres région/tranche, comparaison, sparkline. C'est le cœur du produit — agrégation de toutes les infos API joueurs. |

### P1 — Navigation & Accessibilité

| # | Action | Détail |
|---|---|---|
| N1 | Navbar globale | Menu de navigation principal exposant toutes les sections : Jeux / Catalogue / Admin. À définir en party mode avec les agents. |
| N2 | Accès admin UX | Décider (party mode) : bouton dédié admin / menu contextuel / sidebar. Ne pas implémenter sans décision collective. |

### P2 — Architecture

| # | Action | Détail |
|---|---|---|
| A1 | UserContextService JWT | Adresser le gap login mock → JWT réel. P2 pour Sprint 5, P0 avant toute exposition publique. |

### ⛔ Ne pas re-proposer
- Hébergement externe / staging → `deferred-future` — ne pas re-proposer

═══════════════════════════════════════════════════════════

---

## 8. Évaluation Sprint 4

| Dimension | Score | Notes |
|---|---|---|
| Livraison | 6/10 | 3/9 stories (33%), mais les 6 bloquées étaient légitimement en veille |
| Qualité technique | 9/10 | Migration A6 exemplaire, 24 tests Fortnite API, 0 régression |
| Tests E2E | 3/10 | 0 couverture admin et catalogue — angle mort majeur |
| Accessibilité features | 2/10 | Features implémentées non-navigables, bug 404 admin |
| Process | 8/10 | Règle hébergement respectée, retro productive, feedback terrain direct |
| **Global** | **5.6/10** | Technique solide mais UX et E2E en dette critique |

---

## 9. Preview Sprint 5

**Bob (Scrum Master) :** "Sprint 5 doit répondre à une question simple : est-ce que Thibaut peut utiliser son app ?"

**Priorités naturelles :**
1. **Fix SpaController** — débloquer l'accès aux routes admin
2. **Party Mode UX** — décider navbar + accès admin avec tous les agents
3. **E2E masse** — admin + catalogue, c'est la priorité absolue
4. **Catalogue** — valider l'agrégation API joueurs end-to-end

**Ce qu'on ne fait pas en Sprint 5 :**
- Hébergement externe → deferred-future
- Staging → deferred-future
- CI/CD deploy → deferred-future

---

*Rétrospective générée le 2026-03-08. Prochaine action : Sprint Planning Sprint 5 via `/bmad-bmm-sprint-planning` — ou Party Mode d'abord pour décider l'UX admin.*
