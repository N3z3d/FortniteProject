# Sprint 3 Retrospective — "Rendre le projet déployé et maintenable"

**Date:** 2026-03-06
**Facilitated by:** Bob (Scrum Master)
**Attendees:** Thibaut (Project Lead), Alice (Product Owner), Charlie (Senior Dev), Dana (QA Engineer), Elena (Junior Dev)
**Sprint Goal:** Rendre le projet déployé et maintenable (Docker local stable → staging → CI/CD vert)

---

## 1. Sprint Summary

### Delivery Metrics

| Metric | Value |
|---|---|
| Stories Done | 10 / 18 (56% — 4 deferred + 4 backlog stretch) |
| Stories Deferred Sprint 4 | 4 (hébergement, secrets, DB prod, staging) |
| Stories Stretch Backlog | 4 (API wiring check, CI/CD complet, WS auth, JPA migration) |
| Parallel agents used | Yes — 4 agents simultaneously (first time at this scale) |
| Backend tests | 2355 run, 15F+1E (all pre-existing) |
| Frontend tests | 2206 run, 2185 passing (21 Zone.js pre-existing) |
| Technical debt items addressed | 4 (DRY, Demeter, magic numbers, Dockerfile DoD) |
| Production incidents | 0 |

### Stories Completed

| Story | Description |
|---|---|
| sprint3-docker-local-stable | Docker local stack fonctionnel (http://localhost:8080) |
| sprint3-sec-r1-rate-limiting-login | Bucket4j 8.10.1, 5req/60s par IP, 5 tests |
| sprint3-a1-fix-zonejs-debounce-tests | 47→21 failures Zone.js (44 tests convertis) |
| sprint3-a2-upgrade-spring-boot-34 | Spring Boot 3.3.0→3.4.5 (Hibernate 6.6, Security 6.4) |
| sprint3-a3a-cicd-pipeline-minimal | .github/workflows/ci.yml (3 jobs: backend+frontend+docker) |
| sprint3-a4-dry-violations-top10 | MappingUtils + EntityReferenceFactory + ExceptionResponseBuilder + SecurityTestBeansConfig |
| sprint3-a5-demeter-violations-top12 | DEM-001/003/004/006/007/008 — score 5.5→7/10 |
| sprint3-a7-dockerfile-dod-update | Critère Dockerfile ajouté dans DoD project-context.md |
| sprint3-a9-sonarqube-magic-numbers | 35+ constantes nommées (backend + frontend) |
| sprint3-e2e-playwright-functional | 4 fichiers spec, 17 tests + full-game-flow.spec.ts (10 tests lifecycle complet) |

---

## 2. What Went Well

**Bob (Scrum Master):** "Commençons par les succès. Qu'est-ce qui a bien fonctionné ?"

**Charlie (Senior Dev):** "Les agents parallèles ont été une révélation. On a livré SEC-R1, CI/CD, Spring Boot upgrade et DRY en une seule session avec 4 agents simultanés. C'est quelque chose qu'on doit systématiser."

**Alice (Product Owner):** "Le Docker local est enfin stable. http://localhost:8080 fonctionne avec le SPA Angular + l'API Spring Boot dans un seul conteneur. C'est la fondation qu'on attendait depuis le début."

**Dana (QA Engineer):** "Le pipeline CI/CD minimal (A3a) est maintenant en place. Chaque push déclenche backend + frontend + docker build. C'est le filet de sécurité qu'il nous manquait."

**Elena (Junior Dev):** "L'upgrade Spring Boot 3.4.5 était redouté mais bien géré. On a documenté les 4 breaking changes (Hibernate 6.6, Security 6.4, SpaController, UUID transients) — c'est du savoir réutilisable."

**Bob (Scrum Master):** "Les E2E Playwright : on est passé de 8 smoke tests à 27 tests fonctionnels incluant le full game flow complet (login → create → invite → join → draft → pick → roster). Un vrai asset."

### Key Wins Summary
- Agents parallèles : 4 stories livrées simultanément en une session
- Docker local : stack fonctionnel end-to-end
- CI/CD minimal : pipeline GitHub Actions opérationnel
- Spring Boot 3.4.5 : migration réussie, breaking changes documentés
- E2E coverage : 27 tests Playwright (smoke + functional + full-game-flow)
- Qualité : DRY +, Demeter 5.5→7/10, 35+ magic numbers résolus

---

## 3. What Didn't Go Well / Challenges

**Bob (Scrum Master):** "Maintenant les difficultés. Qu'est-ce qui nous a freiné ?"

**Elena (Junior Dev):** "La décision d'hébergement a été soulevée trois fois. A chaque fois Thibaut a dit qu'il ne voulait pas la prendre maintenant. On a perdu du temps à reformuler la même question."

**Bob (Scrum Master):** "C'est un signal important. Si l'utilisateur dit non deux fois, la troisième fois n'est pas la bonne approche. On doit documenter les décisions reportées et ne pas les re-proposer dans le même sprint."

**Charlie (Senior Dev):** "Les breaking changes Spring Boot 3.4 ont créé des effets de bord inattendus : StaleObjectStateException Hibernate (UUIDs pre-assigned), dual SecurityFilterChain, SpaController interceptant /api/. Chacun était résolvable mais prenait du temps à diagnostiquer."

**Alice (Product Owner):** "4 items restent en backlog stretch (API wiring check, CI/CD complet, WS auth, JPA migration). Ce sont des stretch goals légitimes, mais on devrait les pré-qualifier pour Sprint 4 plutôt que les laisser flotter."

**Dana (QA Engineer):** "La couverture E2E est bonne en termes de structure, mais les tests FULL-FLOW-06 à 08 (start draft, pick, roster) utilisent `test.fixme()` car ils dépendent d'une DB peuplée. On a des tests structurellement corrects mais non-exécutables en CI à froid."

---

## 4. Lessons Learned

### L1 — Respecter les décisions "Non" de l'utilisateur
**Problème:** La décision d'hébergement a été proposée 3 fois malgré le refus répété.
**Leçon:** Quand l'utilisateur dit "non" ou "reporte", marquer l'item `deferred-sprintN` dans sprint-status.yaml et ne plus le re-proposer dans le même sprint. Documenter la raison dans un commentaire YAML.
**Action:** Règle ajoutée dans project-context.md — voir section DoD.

### L2 — Documenter les breaking changes au moment où ils surviennent
**Problème:** Spring Boot 3.4 a eu 4 breaking changes ; chacun a nécessité du diagnostic.
**Leçon:** Créer une section "Breaking Changes Rencontrés" dans chaque story d'upgrade. Les mémoriser dans MEMORY.md immédiatement (pas après).
**Action:** MEMORY.md contient maintenant les 4 patterns Spring Boot 3.4. A réutiliser si upgrade 3.5 arrive.

### L3 — Les agents parallèles nécessitent une séparation nette des périmètres
**Problème:** Agents parallèles ont bien fonctionné car les domaines étaient indépendants (SEC-R1 / CI / Spring / DRY). Si deux agents touchent le même fichier, conflit possible.
**Leçon:** Avant de lancer des agents en parallèle, vérifier qu'aucun fichier partagé n'est modifié par 2 agents simultanément. Documenter les zones exclusives par agent.

### L4 — Les E2E tests "dépendants de données" doivent être flaggés dès la création
**Problème:** Tests FULL-FLOW-06/07/08 utilisent `test.fixme()` car la DB est vide.
**Leçon:** Créer deux catégories de E2E : (a) tests "cold start" (fonctionnent sur DB vide), (b) tests "warm start" (nécessitent seed data). Tagguer avec `@tags: ['cold']` ou `@tags: ['warm']` dans les métadonnées Playwright.

### L5 — CI/CD minimal livré : ne pas attendre le "complet" pour commit
**Breaktrough:** A3a (pipeline minimal) livré rapidement. A3b (pipeline complet) reporté stretch.
**Leçon:** "Minimum Viable Pipeline" est suffisant pour débloquer la valeur. Le complet viendra en itérant. Pattern confirmé : livrer en paliers fonctionne mieux que viser le parfait du premier coup.

---

## 5. Action Items for Sprint 4

| # | Action | Owner | Priority |
|---|---|---|---|
| A1 | Seed data E2E : créer un script de seed DB pour les tests "warm start" (full-game-flow) | Dev | P1 |
| A2 | Pré-qualifier les 4 items stretch en stories prêtes-pour-dev Sprint 4 | SM | P1 |
| A3 | Décision hébergement Sprint 4 : proposer une seule fois avec choix binaire (Railway oui/non) et respecter la décision | SM | P2 |
| A4 | CI/CD A3b : docker push + deploy staging (dépend décision hébergement) | Dev | P2 |
| A5 | Secrets config prod (JWT_SECRET, DB_PASSWORD, FORTNITE_API_KEY) via hébergeur | Dev | P2 |
| A6 | API Fortnite wiring check : valider que FortniteApiAdapter + clé .env fonctionnent en Docker local | Dev | P1 |
| A7 | A3b pipeline complet : ne démarrer qu'après décision hébergement | Dev | P3 |

---

## 6. Sprint 3 Score

| Dimension | Score | Notes |
|---|---|---|
| Delivery | 7/10 | 10/18 done (56%), mais 4 deferred intentionnels + 4 stretch légitimes |
| Quality | 8/10 | Tests baseline maintenu, DRY+Demeter+coverage améliorés |
| Process | 7/10 | Agents parallèles excellent, hébergement re-proposé 3x (-)  |
| Technical | 8/10 | Spring Boot 3.4 migrée, Docker stable, CI/CD en place |
| E2E Coverage | 7/10 | 27 tests bons, mais 3 fixme dépendants de seed data |
| **Global** | **7.4/10** | Sprint solide, fondations deployability en place |

---

## 7. Next Sprint Preview — Sprint 4

**Bob (Scrum Master):** "Sprint 4 devrait capitaliser sur les fondations de Sprint 3. Les priorités naturelles sont :"

1. **Décision hébergement** (une seule fois, binaire) — débloque staging + secrets + DB prod
2. **API Fortnite wiring check** — valider le scraping end-to-end en Docker
3. **E2E seed data** — rendre les full-game-flow tests exécutables en CI
4. **CI/CD A3b** — après hébergement choisi
5. **JPA migration A6** — tech debt critique, 5 services legacy à migrer vers domain ports

**Charlie (Senior Dev):** "J'ajouterais aussi SEC-R2 WebSocket auth — si on expose staging publiquement, le WS doit être authentifié."

**Alice (Product Owner):** "Agreed. Et on devrait formaliser le processus de sprint planning Sprint 4 avec BMAD après cette retro."

---

## 8. Team Agreements for Sprint 4

1. Quand l'utilisateur dit "non" ou "reporte" : `deferred-sprintN` dans YAML, ne plus re-proposer dans le même sprint
2. Agents parallèles : vérifier l'indépendance des périmètres avant lancement
3. E2E tests : distinguer cold/warm dès la création
4. Breaking changes : documenter dans MEMORY.md immédiatement quand découverts
5. Pipeline : livrer en paliers, ne pas bloquer sur le "complet"

---

*Rétrospective générée le 2026-03-06. Prochaine action : Sprint Planning Sprint 4 via `bmad-bmm-sprint-planning`.*
